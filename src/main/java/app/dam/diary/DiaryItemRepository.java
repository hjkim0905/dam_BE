package app.dam.diary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryItemRepository extends JpaRepository<DiaryItem, Long> {

    List<DiaryItem> findByPageIdInOrderByLayerAscIdAsc(Collection<Long> pageIds);

    long countByPageId(Long pageId);

    Optional<DiaryItem> findFirstByPageIdOrderByLayerDesc(Long pageId);
}
