package app.dam.entry;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    Optional<Entry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);

    List<Entry> findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(
            Long userId, LocalDate from, LocalDate to);

    /**
     * 방으로만 거르면 나간 사람의 기록이 남은 사람 화면에 계속 보인다. 기록에 박힌
     * room_id 는 지우지 않아야 다시 맺었을 때 돌아오므로, 지금 방에 있는 사람으로
     * 한 번 더 좁힌다.
     */
    List<Entry> findByRoomIdAndUserIdInAndEntryDateBetweenOrderByEntryDateAsc(
            Long roomId, Collection<Long> userIds, LocalDate from, LocalDate to);

    List<Entry> findByUserId(Long userId);

    long countByUserId(Long userId);

    Optional<Entry> findFirstByUserIdOrderByEntryDateAsc(Long userId);
}
