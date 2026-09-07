package app.dam.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_by")
    private Long usedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Invite() {
    }

    public static Invite issue(String code, Long roomId, Long createdBy, LocalDateTime expiresAt) {
        Invite invite = new Invite();
        invite.code = code;
        invite.roomId = roomId;
        invite.createdBy = createdBy;
        invite.expiresAt = expiresAt;
        invite.createdAt = LocalDateTime.now();
        return invite;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public String getCode() {
        return code;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
