package app.dam.diary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "diary_pages")
public class DiaryPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DiaryPage() {
    }

    public static DiaryPage append(Long diaryId, int position) {
        DiaryPage page = new DiaryPage();
        page.diaryId = diaryId;
        page.position = position;
        page.createdAt = LocalDateTime.now();
        page.updatedAt = page.createdAt;
        return page;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public int getPosition() {
        return position;
    }
}
