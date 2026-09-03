package app.dam.entry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VisibilityTest {

    private static final Long ME = 1L;
    private static final Long PARTNER = 2L;

    @Test
    void 함께_보기는_방에_남은_모두를_본다() {
        assertThat(Visibility.whose(List.of(ME, PARTNER), ME, EntryDto.View.BOTH))
                .containsExactly(ME, PARTNER);
    }

    @Test
    void 상대것_보기는_나를_뺀다() {
        assertThat(Visibility.whose(List.of(ME, PARTNER), ME, EntryDto.View.THEIRS))
                .containsExactly(PARTNER);
    }

    @Test
    void 상대가_나가면_상대것은_비어_있다() {
        assertThat(Visibility.whose(List.of(ME), ME, EntryDto.View.THEIRS)).isEmpty();
    }

    @Test
    void 상대가_나가면_함께_보기에도_내_것만_남는다() {
        // 기록의 room_id 는 그대로 두어야 다시 맺었을 때 돌아온다.
        // 그래서 사라지게 하는 일은 조회 쪽이 맡는다.
        assertThat(Visibility.whose(List.of(ME), ME, EntryDto.View.BOTH)).containsExactly(ME);
    }
}
