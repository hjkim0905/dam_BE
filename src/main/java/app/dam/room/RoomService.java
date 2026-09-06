package app.dam.room;

import app.dam.entry.EntryRepository;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import app.dam.user.User;
import app.dam.user.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    /** 담은 둘이 채우는 서비스라 정원이 둘이다. */
    static final int CAPACITY = 2;
    static final Duration INVITE_TTL = Duration.ofHours(24);
    private static final int CODE_ATTEMPTS = 10;

    private final RoomRepository rooms;
    private final MembershipRepository memberships;
    private final InviteRepository invites;
    private final UserRepository users;
    private final EntryRepository entries;

    RoomService(RoomRepository rooms, MembershipRepository memberships,
                InviteRepository invites, UserRepository users, EntryRepository entries) {
        this.rooms = rooms;
        this.memberships = memberships;
        this.invites = invites;
        this.users = users;
        this.entries = entries;
    }

    public Optional<Membership> currentMembership(Long userId) {
        return memberships.findByUserIdAndLeftAtIsNull(userId);
    }

    public Long currentRoomId(Long userId) {
        return currentMembership(userId).map(Membership::getRoomId).orElse(null);
    }

    /** 지금 그 방에 남아 있는 사람들. 나간 사람은 빠진다. */
    public List<Long> currentMemberIds(Long roomId) {
        return memberships.findByRoomIdAndLeftAtIsNull(roomId).stream()
                .map(Membership::getUserId)
                .toList();
    }

    public Optional<User> partnerOf(Long roomId, Long userId) {
        return memberships.findByRoomIdAndLeftAtIsNull(roomId).stream()
                .map(Membership::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .flatMap(users::findById);
    }

    /**
     * 방이 없으면 먼저 만든다. 이미 살아 있는 코드가 있으면 그걸 그대로 돌려준다.
     * 누를 때마다 새로 뽑으면 이미 상대에게 보낸 코드가 죽는다.
     */
    @Transactional
    public RoomDto.InviteIssued issueInvite(Long userId) {
        Membership membership = currentMembership(userId)
                .orElseGet(() -> openRoom(userId));

        if (memberships.findByRoomIdAndLeftAtIsNull(membership.getRoomId()).size() >= CAPACITY) {
            throw ApiException.of(ErrorCode.ROOM_FULL);
        }

        LocalDateTime now = LocalDateTime.now();
        Invite invite = invites
                .findFirstByRoomIdAndUsedAtIsNullAndExpiresAtAfterOrderByIdDesc(
                        membership.getRoomId(), now)
                .orElseGet(() -> invites.save(Invite.issue(
                        uniqueCode(), membership.getRoomId(), userId, now.plus(INVITE_TTL))));

        return new RoomDto.InviteIssued(invite.getCode(), invite.getExpiresAt());
    }

    @Transactional
    public RoomDto.Joined join(Long userId, String rawCode) {
        leaveEmptyRoom(userId);

        String code = InviteCodes.normalize(rawCode);
        if (!InviteCodes.looksValid(code)) {
            throw ApiException.of(ErrorCode.INVITE_NOT_FOUND);
        }

        Invite invite = invites.findByCode(code)
                .orElseThrow(() -> ApiException.of(ErrorCode.INVITE_NOT_FOUND));
        if (invite.isUsed()) {
            throw ApiException.of(ErrorCode.INVITE_USED);
        }
        if (invite.isExpired(LocalDateTime.now())) {
            throw ApiException.of(ErrorCode.INVITE_EXPIRED);
        }
        if (invite.getCreatedBy().equals(userId)) {
            throw ApiException.of(ErrorCode.INVITE_SELF);
        }
        if (memberships.findByRoomIdAndLeftAtIsNull(invite.getRoomId()).size() >= CAPACITY) {
            throw ApiException.of(ErrorCode.ROOM_FULL);
        }

        // 둘이 같은 코드를 동시에 넣으면 여기서 하나만 살아남는다.
        if (invites.markUsed(code, userId, LocalDateTime.now()) != 1) {
            throw ApiException.of(ErrorCode.INVITE_USED);
        }

        Membership membership = memberships
                .findByRoomIdAndUserId(invite.getRoomId(), userId)
                .map(existing -> {
                    existing.rejoin();
                    return existing;
                })
                .orElseGet(() -> memberships.save(
                        Membership.join(invite.getRoomId(), userId)));

        // 맺은 날 당일 것은 양쪽 모두 방으로 들인다. 한쪽만 들이면 같은 날인데
        // 내 것만 보이고 상대 것은 안 보이는 상태가 된다.
        entries.adoptInto(
                invite.getRoomId(), currentMemberIds(invite.getRoomId()), LocalDate.now());

        User partner = partnerOf(invite.getRoomId(), userId).orElse(null);
        return new RoomDto.Joined(
                invite.getRoomId(),
                partner == null ? null : new RoomDto.Partner(partner.getId(), partner.getName()),
                membership.getJoinedAt().toLocalDate());
    }

    /**
     * 초대코드를 만들면 방이 먼저 생긴다. 그래서 둘이 각자 코드를 만들어 두면
     * 서로의 코드를 쓸 수 없는 막다른 길이 된다. 혼자 있는 방이면 비켜 준다.
     *
     * 남겨 둔 내 코드도 같이 없앤다. 안 그러면 그 코드로 들어온 사람이 아무도
     * 없는 방에 혼자 서게 된다.
     */
    private void leaveEmptyRoom(Long userId) {
        Membership mine = currentMembership(userId).orElse(null);
        if (mine == null) {
            return;
        }
        if (memberships.findByRoomIdAndLeftAtIsNull(mine.getRoomId()).size() > 1) {
            throw ApiException.of(ErrorCode.ALREADY_IN_ROOM);
        }

        invites.deleteAll(invites.findByRoomIdAndUsedAtIsNull(mine.getRoomId()));
        mine.leave();
        memberships.flush();
    }

    /**
     * 기록은 건드리지 않는다. 조회가 지금 속한 방을 기준으로 돌아서 서로의 것만
     * 안 보이게 되고, 다시 맺으면 그대로 돌아온다.
     */
    @Transactional
    public void leave(Long userId) {
        Membership membership = currentMembership(userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_IN_ROOM));
        membership.leave();
    }

    /** 탈퇴할 때 이 사람의 자취를 방에서 지운다. 방 자체는 남는다. */
    @Transactional
    public void forget(Long userId) {
        invites.deleteAll(invites.findByCreatedByOrUsedBy(userId, userId));
        memberships.deleteAll(memberships.findByUserId(userId));
        // users 를 지우기 전에 자식을 실제로 내보내야 FK 가 걸리지 않는다.
        memberships.flush();
        invites.flush();
    }

    private Membership openRoom(Long userId) {
        Room room = rooms.save(Room.create());
        return memberships.save(Membership.join(room.getId(), userId));
    }

    /**
     * 미리 조회해서 비었는지 확인하는 방식은 동시에 들어오면 뚫린다. 넣어 보고
     * UNIQUE 에 걸리면 다시 뽑는다. 32^6 이라 실제로 부딪히는 일은 드물다.
     */
    private String uniqueCode() {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            String code = InviteCodes.next();
            if (!invites.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("초대코드를 뽑지 못했다");
    }
}
