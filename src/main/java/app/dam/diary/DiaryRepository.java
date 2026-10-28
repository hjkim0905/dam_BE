package app.dam.diary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByUserIdAndRoomIdIsNull(Long userId);

    List<Diary> findByRoomId(Long roomId);

    Optional<Diary> findByUserIdAndRoomIdIsNullAndYear(Long userId, int year);

    Optional<Diary> findByRoomIdAndYear(Long roomId, int year);
}
