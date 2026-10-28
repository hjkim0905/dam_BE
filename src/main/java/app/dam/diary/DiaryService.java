package app.dam.diary;

import app.dam.entry.Entry;
import app.dam.entry.EntryRepository;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import app.dam.photo.PhotoStorage;
import app.dam.room.RoomService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryService {

    /* 한 권에 이만큼이면 한 해를 꾸미고도 남는다. 끝없이 늘리는 요청을 막는 한도다. */
    private static final int MAX_PAGES = 200;
    private static final int MAX_ITEMS = 60;
    /* 해마다 다른 색으로 꽂혀야 책장에서 연도가 갈린다. 사용자가 바꿀 수 있다. */
    private static final List<String> COVERS = List.of("#F6C1CC", "#9CC3EF", "#9FD3B5", "#F6CF7A", "#C9A7E8", "#F5B48A");

    private final DiaryRepository diaries;
    private final DiaryPageRepository pages;
    private final DiaryItemRepository items;
    private final EntryRepository entries;
    private final RoomService rooms;
    private final PhotoStorage photos;

    DiaryService(DiaryRepository diaries, DiaryPageRepository pages, DiaryItemRepository items,
                 EntryRepository entries, RoomService rooms, PhotoStorage photos) {
        this.diaries = diaries;
        this.pages = pages;
        this.items = items;
        this.entries = entries;
        this.rooms = rooms;
        this.photos = photos;
    }

    /** 혼자 만든 책과 지금 방의 책을 최근 해부터 꽂는다. */
    @Transactional(readOnly = true)
    public DiaryDto.Shelf shelf(Long userId) {
        Long roomId = rooms.currentRoomId(userId);
        Stream<Diary> shared = roomId == null ? Stream.empty() : diaries.findByRoomId(roomId).stream();
        List<DiaryDto.Book> books = Stream.concat(shared, diaries.findByUserIdAndRoomIdIsNull(userId).stream())
                .sorted(Comparator.comparingInt(Diary::getYear).reversed())
                .map(this::present)
                .toList();
        return new DiaryDto.Shelf(books);
    }

    /**
     * 그 해의 책을 펼친다. 없으면 만든다. 방에 있으면 방의 책이다. 둘이 같은 순간에 처음
     * 펼치면 한쪽은 유니크 키에 걸리는데, 그때는 상대가 만든 책을 그대로 준다.
     */
    @Transactional
    public DiaryDto.Book open(Long userId, int year, ZoneId zone) {
        Diary.checkYear(year, LocalDate.now(zone));
        Long roomId = rooms.currentRoomId(userId);
        return present(find(userId, roomId, year).orElseGet(() -> create(userId, roomId, year)));
    }

    private java.util.Optional<Diary> find(Long userId, Long roomId, int year) {
        return roomId == null
                ? diaries.findByUserIdAndRoomIdIsNullAndYear(userId, year)
                : diaries.findByRoomIdAndYear(roomId, year);
    }

    private Diary create(Long userId, Long roomId, int year) {
        String cover = COVERS.get(Math.floorMod(year, COVERS.size()));
        Diary diary = roomId == null ? Diary.solo(userId, year, cover) : Diary.shared(roomId, year, cover);
        try {
            return diaries.saveAndFlush(diary);
        } catch (DataIntegrityViolationException raced) {
            return find(userId, roomId, year).orElseThrow(() -> raced);
        }
    }

    @Transactional
    public DiaryDto.Book recolor(Long userId, Long diaryId, String coverColor) {
        Diary diary = visible(userId, diaryId);
        diary.recolor(coverColor.toUpperCase());
        return present(diary);
    }

    @Transactional(readOnly = true)
    public DiaryDto.Pages pages(Long userId, Long diaryId) {
        Diary diary = visible(userId, diaryId);
        List<DiaryPage> found = pages.findByDiaryIdOrderByPositionAscIdAsc(diaryId);
        Map<Long, List<DiaryItem>> byPage = items.findByPageIdInOrderByLayerAscIdAsc(
                found.stream().map(DiaryPage::getId).toList()).stream()
                .collect(Collectors.groupingBy(DiaryItem::getPageId));
        Map<Long, Entry> photosById = entries.findAllById(byPage.values().stream().flatMap(List::stream)
                        .map(DiaryItem::getEntryId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Entry::getId, Function.identity()));
        List<DiaryDto.Page> presented = found.stream()
                .map(page -> new DiaryDto.Page(page.getId(), page.getPosition(),
                        byPage.getOrDefault(page.getId(), List.of()).stream()
                                .map(item -> present(item, photosById.get(item.getEntryId())))
                                .toList()))
                .toList();
        return new DiaryDto.Pages(present(diary), presented);
    }

    @Transactional
    public DiaryDto.Page addPage(Long userId, Long diaryId) {
        Diary diary = visible(userId, diaryId);
        if (pages.countByDiaryId(diaryId) >= MAX_PAGES) throw ApiException.of(ErrorCode.DIARY_FULL);
        int next = pages.findFirstByDiaryIdOrderByPositionDesc(diaryId).map(DiaryPage::getPosition).orElse(-1) + 1;
        DiaryPage page = pages.save(DiaryPage.append(diaryId, next));
        diary.touch();
        return new DiaryDto.Page(page.getId(), page.getPosition(), List.of());
    }

    @Transactional
    public void removePage(Long userId, Long pageId) {
        DiaryPage page = pages.findById(pageId).orElseThrow(() -> ApiException.of(ErrorCode.DIARY_PAGE_NOT_FOUND));
        visible(userId, page.getDiaryId()).touch();
        pages.delete(page);
    }

    @Transactional
    public DiaryDto.Item addItem(Long userId, Long pageId, DiaryDto.Add request) {
        DiaryPage page = pages.findById(pageId).orElseThrow(() -> ApiException.of(ErrorCode.DIARY_PAGE_NOT_FOUND));
        Diary diary = visible(userId, page.getDiaryId());
        if (items.countByPageId(pageId) >= MAX_ITEMS) throw ApiException.of(ErrorCode.DIARY_PAGE_FULL);
        int top = items.findFirstByPageIdOrderByLayerDesc(pageId).map(DiaryItem::getLayer).orElse(-1) + 1;

        Entry photo = null;
        DiaryItem item;
        switch (request.kind()) {
            case PHOTO -> {
                photo = attachable(userId, diary, request.entryId());
                String frame = request.frame() == null ? "polaroid" : request.frame();
                item = DiaryItem.photo(pageId, photo.getId(), frame, request.placement(), top, userId);
            }
            case STICKER -> item = DiaryItem.sticker(pageId, request.sticker(), request.placement(), top, userId);
            case TEXT -> item = DiaryItem.text(pageId, request.text(), request.placement(), top, userId);
            default -> throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        }
        page.touch();
        return present(items.saveAndFlush(item), photo);
    }

    @Transactional
    public DiaryDto.Item moveItem(Long userId, Long itemId, DiaryDto.Move request) {
        DiaryItem item = editable(userId, itemId);
        if (request.text() != null) item.rewrite(request.text(), request.version());
        item.place(request.placement(), request.layer(), request.version());
        Entry photo = item.getEntryId() == null ? null : entries.findById(item.getEntryId()).orElse(null);
        return present(items.saveAndFlush(item), photo);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        items.delete(editable(userId, itemId));
    }

    /** 탈퇴할 때 부른다. 혼자 쓰던 책만 지운다. 페이지와 붙인 것은 DB 가 따라 지운다. */
    @Transactional
    public void forget(Long userId) {
        diaries.deleteAll(diaries.findByUserIdAndRoomIdIsNull(userId));
        diaries.flush();
    }

    private DiaryItem editable(Long userId, Long itemId) {
        DiaryItem item = items.findById(itemId).orElseThrow(() -> ApiException.of(ErrorCode.DIARY_ITEM_NOT_FOUND));
        DiaryPage page = pages.findById(item.getPageId())
                .orElseThrow(() -> ApiException.of(ErrorCode.DIARY_ITEM_NOT_FOUND));
        visible(userId, page.getDiaryId());
        return item;
    }

    /** 볼 수 없는 책은 없는 책처럼 답한다. 남의 책이 있다는 사실도 흘리지 않는다. */
    private Diary visible(Long userId, Long diaryId) {
        Long roomId = rooms.currentRoomId(userId);
        return diaries.findById(diaryId)
                .filter(diary -> diary.canBeSeenBy(userId, roomId))
                .orElseThrow(() -> ApiException.of(ErrorCode.DIARY_NOT_FOUND));
    }

    /**
     * 혼자 쓰는 책에는 내 기록만, 방의 책에는 그 방에 남긴 둘의 기록까지 붙인다.
     * 방을 나간 뒤에 남긴 상대의 기록은 이 방 것이 아니라서 붙일 수 없다.
     */
    private Entry attachable(Long userId, Diary diary, Long entryId) {
        if (entryId == null) throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        Entry entry = entries.findById(entryId).orElseThrow(() -> ApiException.of(ErrorCode.DIARY_PHOTO_HIDDEN));
        boolean mine = entry.getUserId().equals(userId);
        boolean ours = diary.getRoomId() != null && diary.getRoomId().equals(entry.getRoomId());
        if (!(diary.getRoomId() == null ? mine : ours || mine)) throw ApiException.of(ErrorCode.DIARY_PHOTO_HIDDEN);
        return entry;
    }

    private DiaryDto.Book present(Diary diary) {
        return new DiaryDto.Book(diary.getId(), diary.getYear(), diary.getCoverColor(),
                diary.getRoomId() != null, pages.countByDiaryId(diary.getId()));
    }

    private DiaryDto.Item present(DiaryItem item, Entry photo) {
        return new DiaryDto.Item(item.getId(), item.getKind(), item.getEntryId(),
                photo == null ? null : photos.readUrl(photo.getPhotoKey()),
                photo == null ? null : photo.getEntryDate(),
                photo == null ? null : photo.getColor(),
                item.getSticker(), item.getFrame(), item.getText(),
                item.getX(), item.getY(), item.getScale(), item.getRotation(), item.getLayer(),
                item.getVersion(), item.getCreatedBy());
    }
}
