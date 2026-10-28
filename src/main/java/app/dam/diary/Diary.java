package app.dam.diary;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "diaries")
public class Diary {

    /* 담. 이 시작한 해보다 앞선 책은 만들 일이 없다. 너무 오래된 해를 막는 하한일 뿐이다. */
    private static final int FIRST_YEAR = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(nullable = false)
    private int year;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cover_color", nullable = false, length = 7)
    private String coverColor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Diary() {
    }

    private Diary(Long userId, Long roomId, int year, String coverColor) {
        this.userId = userId;
        this.roomId = roomId;
        this.year = year;
        this.coverColor = coverColor;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static Diary solo(Long userId, int year, String coverColor) {
        return new Diary(userId, null, year, coverColor);
    }

    public static Diary shared(Long roomId, int year, String coverColor) {
        return new Diary(null, roomId, year, coverColor);
    }

    /** 내년 책까지는 미리 만들 수 있다. 연말에 다음 해를 준비하는 사람이 있다. */
    public static void checkYear(int year, LocalDate today) {
        if (year < FIRST_YEAR || year > today.getYear() + 1) {
            throw ApiException.of(ErrorCode.DIARY_YEAR_INVALID);
        }
    }

    /**
     * 방의 책은 지금 그 방에 있는 사람만 본다. 방을 나가면 함께 꾸미던 책도 놓고 간다.
     * 혼자 만든 책은 방에 들어가도 계속 내 것이다.
     */
    public boolean canBeSeenBy(Long viewerId, Long viewerRoomId) {
        if (roomId != null) return Objects.equals(roomId, viewerRoomId);
        return Objects.equals(userId, viewerId);
    }

    public void recolor(String coverColor) {
        this.coverColor = coverColor;
        this.updatedAt = LocalDateTime.now();
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getCoverColor() {
        return coverColor;
    }
}
