package app.dam.user;

import app.dam.auth.AppleClient;
import app.dam.entry.EntryService;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import app.dam.photo.PhotoStorage;
import app.dam.room.Membership;
import app.dam.room.RoomService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeService {

    private final UserRepository users;
    private final EntryService entries;
    private final RoomService rooms;
    private final AppleClient apple;
    private final PhotoStorage photos;

    MeService(UserRepository users, EntryService entries, RoomService rooms,
              AppleClient apple, PhotoStorage photos) {
        this.users = users;
        this.entries = entries;
        this.rooms = rooms;
        this.apple = apple;
        this.photos = photos;
    }

    /** 마이페이지가 한 번에 그려지도록 통계와 방까지 한 응답에 담는다. */
    @Transactional(readOnly = true)
    public MeDto.Profile profile(Long userId) {
        User user = user(userId);
        MeDto.Room room = rooms.currentMembership(userId)
                .map(this::describe)
                .orElse(null);

        return new MeDto.Profile(
                user.getId(),
                user.getName(),
                user.isOnboarded(),
                entries.countOf(userId),
                entries.firstKeptDateOf(userId).orElse(null),
                room);
    }

    /**
     * 동의 시각은 서버가 찍는다. 앱이 보낸 시각을 믿으면 기기 시계가 틀어졌을 때
     * 언제 동의했는지를 증명할 수 없다.
     */
    @Transactional
    public MeDto.Profile completeOnboarding(Long userId, MeDto.Onboarding request) {
        if (!request.termsAgreed() || !request.privacyAgreed()) {
            throw ApiException.of(ErrorCode.CONSENT_REQUIRED);
        }
        user(userId).completeOnboarding(request.name().strip());
        return profile(userId);
    }

    @Transactional
    public MeDto.Profile rename(Long userId, String name) {
        user(userId).rename(name.strip());
        return profile(userId);
    }

    /**
     * 순서가 중요하다. 애플 토큰은 계정 행을 지우기 전에 폐기해야 남아 있고,
     * 사진은 트랜잭션 밖에서 지워야 스토리지 사정으로 탈퇴가 막히지 않는다.
     */
    @Transactional
    public List<String> withdraw(Long userId) {
        User user = user(userId);
        apple.revoke(user.getAppleRefreshToken());

        List<String> photoKeys = entries.removeAllOf(userId);
        rooms.forget(userId);
        users.delete(user);
        return photoKeys;
    }

    /** 실패해서 남은 파일은 고아 파일 청소가 거둔다. 탈퇴를 막지는 않는다. */
    public void erasePhotos(List<String> photoKeys) {
        photoKeys.forEach(photos::delete);
    }

    private MeDto.Room describe(Membership membership) {
        MeDto.Partner partner = rooms
                .partnerOf(membership.getRoomId(), membership.getUserId())
                .map(other -> new MeDto.Partner(other.getId(), other.getName()))
                .orElse(null);
        return new MeDto.Room(
                membership.getRoomId(), partner, membership.getJoinedAt().toLocalDate());
    }

    private User user(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHENTICATED));
    }
}
