package app.dam.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.dam.error.ApiException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DiaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Test
    void 혼자_만든_책은_나만_본다() {
        Diary diary = Diary.solo(1L, 2026, "#F6C1CC");

        assertThat(diary.canBeSeenBy(1L, null)).isTrue();
        assertThat(diary.canBeSeenBy(1L, 7L)).isTrue();
        assertThat(diary.canBeSeenBy(2L, null)).isFalse();
    }

    @Test
    void 방의_책은_지금_그_방에_있는_사람만_본다() {
        Diary diary = Diary.shared(7L, 2026, "#F6C1CC");

        assertThat(diary.canBeSeenBy(1L, 7L)).isTrue();
        assertThat(diary.canBeSeenBy(2L, 7L)).isTrue();
        // 방을 나가면 함께 꾸미던 책도 더는 보이지 않는다.
        assertThat(diary.canBeSeenBy(1L, null)).isFalse();
        assertThat(diary.canBeSeenBy(1L, 8L)).isFalse();
    }

    @Test
    void 아직_오지_않은_해의_다음_해까지만_만들_수_있다() {
        Diary.checkYear(2027, TODAY);
        Diary.checkYear(2020, TODAY);

        assertThatThrownBy(() -> Diary.checkYear(2028, TODAY)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Diary.checkYear(1999, TODAY)).isInstanceOf(ApiException.class);
    }

    @Test
    void 표지_색을_바꾼다() {
        Diary diary = Diary.solo(1L, 2026, "#F6C1CC");

        diary.recolor("#9CC3EF");

        assertThat(diary.getCoverColor()).isEqualTo("#9CC3EF");
    }
}
