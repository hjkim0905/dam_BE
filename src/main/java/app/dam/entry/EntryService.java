package app.dam.entry;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import app.dam.photo.PhotoStorage;
import app.dam.room.RoomService;
import app.dam.user.User;
import app.dam.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryService {

    private final EntryRepository entries;
    private final UserRepository users;
    private final RoomService rooms;
    private final PhotoStorage photos;

    EntryService(EntryRepository entries, UserRepository users,
                 RoomService rooms, PhotoStorage photos) {
        this.entries = entries;
        this.users = users;
        this.rooms = rooms;
        this.photos = photos;
    }

    /**
     * 방은 요청이 아니라 저장 시점에 서버가 보고 박는다. 앱이 보내게 하면 속하지
     * 않은 방에 기록을 넣을 수 있다.
     */
    @Transactional
    public EntryDto.Kept keep(Long userId, EntryDto.Keep request, ZoneId zone) {
        if (request.date().isAfter(LocalDate.now(zone))) {
            throw ApiException.of(ErrorCode.FUTURE_DATE);
        }

        Long roomId = rooms.currentRoomId(userId);
        Entry entry = entries.findByUserIdAndEntryDate(userId, request.date())
                .map(existing -> {
                    String oldKey = existing.replaceWith(
                            roomId, request.color(), request.photoKey(), request.memo());
                    if (!oldKey.equals(request.photoKey())) {
                        photos.delete(oldKey);
                    }
                    return existing;
                })
                .orElseGet(() -> entries.save(Entry.keep(
                        userId, roomId, request.date(),
                        request.color(), request.photoKey(), request.memo())));

        return present(entry, users.findById(userId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public EntryDto.Page list(Long userId, LocalDate from, LocalDate to, EntryDto.View view) {
        Long roomId = rooms.currentRoomId(userId);

        // 방이 없는데 함께나 상대것을 부르면 내것과 같은 결과를 준다. 프론트가 필터를
        // 숨기고 있을 뿐이라, 이걸 에러로 만들면 상태가 엇갈릴 때 화면이 깨진다.
        if (roomId == null || view == EntryDto.View.MINE) {
            return new EntryDto.Page(present(
                    entries.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(userId, from, to)));
        }

        List<Long> whose = Visibility.whose(rooms.currentMemberIds(roomId), userId, view);
        if (whose.isEmpty()) {
            return new EntryDto.Page(List.of());
        }

        return new EntryDto.Page(present(
                entries.findByRoomIdAndUserIdInAndEntryDateBetweenOrderByEntryDateAsc(
                        roomId, whose, from, to)));
    }

    @Transactional
    public void remove(Long userId, Long entryId) {
        Entry entry = entries.findById(entryId)
                .orElseThrow(() -> ApiException.of(ErrorCode.ENTRY_NOT_FOUND));
        if (!entry.getUserId().equals(userId)) {
            throw ApiException.of(ErrorCode.NOT_MY_ENTRY);
        }
        entries.delete(entry);
        photos.delete(entry.getPhotoKey());
    }

    /** 탈퇴할 때 부른다. 사진 키는 트랜잭션 밖에서 지우도록 돌려준다. */
    @Transactional
    public List<String> removeAllOf(Long userId) {
        List<Entry> mine = entries.findByUserId(userId);
        List<String> keys = mine.stream().map(Entry::getPhotoKey).toList();
        entries.deleteAll(mine);
        entries.flush();
        return keys;
    }

    public long countOf(Long userId) {
        return entries.countByUserId(userId);
    }

    public Optional<LocalDate> firstKeptDateOf(Long userId) {
        return entries.findFirstByUserIdOrderByEntryDateAsc(userId).map(Entry::getEntryDate);
    }

    private List<EntryDto.Kept> present(List<Entry> found) {
        Map<Long, User> authors = new HashMap<>();
        List<EntryDto.Kept> kept = new ArrayList<>(found.size());
        for (Entry entry : found) {
            User author = authors.computeIfAbsent(
                    entry.getUserId(), id -> users.findById(id).orElseThrow());
            kept.add(present(entry, author));
        }
        return kept;
    }

    private EntryDto.Kept present(Entry entry, User author) {
        return new EntryDto.Kept(
                entry.getId(),
                entry.getEntryDate(),
                entry.getColor(),
                photos.readUrl(entry.getPhotoKey()),
                entry.getMemo(),
                new EntryDto.Author(author.getId(), author.getName()));
    }
}
