package app.dam.entry;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 맺은 날 당일에 이미 혼자 담아둔 것을 방으로 들인다. 담는 순간의 방을 박는
     * 방식이라, 아침에 담고 저녁에 맺으면 같은 날인데도 서로 안 보인다.
     *
     * 그전에 혼자 담던 것은 건드리지 않는다. 다른 방에 속한 것도 그대로 둔다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Entry e
               set e.roomId = :roomId
             where e.userId in :userIds
               and e.entryDate = :on
               and e.roomId is null
            """)
    int adoptInto(@Param("roomId") Long roomId,
                  @Param("userIds") Collection<Long> userIds,
                  @Param("on") LocalDate on);
}
