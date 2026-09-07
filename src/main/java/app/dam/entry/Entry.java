package app.dam.entry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "photo_key", nullable = false)
    private String photoKey;

    @Column(length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Entry() {
    }

    public static Entry keep(Long userId, Long roomId, LocalDate date,
                             String color, String photoKey, String memo) {
        Entry entry = new Entry();
        entry.userId = userId;
        entry.roomId = roomId;
        entry.entryDate = date;
        entry.color = color;
        entry.photoKey = photoKey;
        entry.memo = memo;
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = entry.createdAt;
        return entry;
    }

    /**
     * 다시 담으면 새 행이 아니라 이 행을 고친다. 방은 담는 시점의 것으로 다시 박는다.
     * 혼자 담아둔 날을 방에 들어온 뒤 고쳐 담으면 그때부터 함께 보이는 것이 맞다.
     */
    public String replaceWith(Long roomId, String color, String photoKey, String memo) {
        String previousPhotoKey = this.photoKey;
        this.roomId = roomId;
        this.color = color;
        this.photoKey = photoKey;
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
        return previousPhotoKey;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getColor() {
        return color;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    public String getMemo() {
        return memo;
    }
}
