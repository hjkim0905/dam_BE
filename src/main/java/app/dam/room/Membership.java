package app.dam.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    protected Membership() {
    }

    public static Membership join(Long roomId, Long userId) {
        Membership membership = new Membership();
        membership.roomId = roomId;
        membership.userId = userId;
        membership.joinedAt = LocalDateTime.now();
        return membership;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    /**
     * 돌아올 땐 새 행을 만들지 않고 이 행을 되살린다. 새로 만들면 UNIQUE 에 걸리고,
     * 걸리지 않게 풀면 같은 방에 두 번 속한 상태가 된다.
     */
    public void rejoin() {
        this.leftAt = null;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
