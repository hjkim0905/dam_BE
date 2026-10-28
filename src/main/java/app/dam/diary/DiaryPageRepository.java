package app.dam.diary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryPageRepository extends JpaRepository<DiaryPage, Long> {

    List<DiaryPage> findByDiaryIdOrderByPositionAscIdAsc(Long diaryId);

    long countByDiaryId(Long diaryId);

    Optional<DiaryPage> findFirstByDiaryIdOrderByPositionDesc(Long diaryId);
}
