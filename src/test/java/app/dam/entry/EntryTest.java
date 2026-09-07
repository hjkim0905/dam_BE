package app.dam.entry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EntryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 3);

    @Test
    void 다시_담으면_이전_사진_키를_돌려준다() {
        Entry entry = Entry.keep(1L, null, DAY, "#AAAAAA", "old.jpg", null);

        String previous = entry.replaceWith(null, "#BBBBBB", "new.jpg", "메모");

        assertThat(previous).isEqualTo("old.jpg");
        assertThat(entry.getPhotoKey()).isEqualTo("new.jpg");
        assertThat(entry.getColor()).isEqualTo("#BBBBBB");
        assertThat(entry.getMemo()).isEqualTo("메모");
    }

    @Test
    void 혼자_담아둔_날을_방에_들어온_뒤_고치면_그_방_것이_된다() {
        Entry entry = Entry.keep(1L, null, DAY, "#AAAAAA", "a.jpg", null);

        entry.replaceWith(7L, "#BBBBBB", "b.jpg", null);

        // room_id 를 다시 박지 않으면 고쳐 담아도 상대에게 안 보인다.
        assertThat(entry.getEntryDate()).isEqualTo(DAY);
        assertThat(entry.getUserId()).isEqualTo(1L);
    }
}
