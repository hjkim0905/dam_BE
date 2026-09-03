package app.dam.room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByUserIdAndLeftAtIsNull(Long userId);

    Optional<Membership> findByRoomIdAndUserId(Long roomId, Long userId);

    List<Membership> findByRoomIdAndLeftAtIsNull(Long roomId);

    List<Membership> findByUserId(Long userId);
}
