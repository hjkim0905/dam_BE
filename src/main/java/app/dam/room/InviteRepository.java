package app.dam.room;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Invite> findFirstByRoomIdAndUsedAtIsNullAndExpiresAtAfterOrderByIdDesc(
            Long roomId, LocalDateTime now);

    List<Invite> findByCreatedByOrUsedBy(Long createdBy, Long usedBy);

    List<Invite> findByRoomIdAndUsedAtIsNull(Long roomId);

    /**
     * 조회하고 나서 찍으면 둘이 동시에 넣었을 때 둘 다 통과한다. 조건을 갱신문 안에
     * 넣고 바뀐 행이 하나일 때만 진행한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Invite i
               set i.usedAt = :now, i.usedBy = :userId
             where i.code = :code and i.usedAt is null
            """)
    int markUsed(@Param("code") String code,
                 @Param("userId") Long userId,
                 @Param("now") LocalDateTime now);
}
