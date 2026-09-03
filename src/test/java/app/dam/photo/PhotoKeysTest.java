package app.dam.photo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PhotoKeysTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 3);

    @Test
    void 키는_유저와_연월로_나뉜다() {
        assertThat(PhotoKeys.next(7L, "image/jpeg", DAY)).startsWith("u7/2026/09/");
    }

    @Test
    void 확장자는_콘텐츠_타입을_따른다() {
        assertThat(PhotoKeys.next(1L, "image/png", DAY)).endsWith(".png");
        assertThat(PhotoKeys.next(1L, "image/heic", DAY)).endsWith(".heic");
    }

    @Test
    void 같은_조건이라도_키는_겹치지_않는다() {
        assertThat(PhotoKeys.next(1L, "image/jpeg", DAY))
                .isNotEqualTo(PhotoKeys.next(1L, "image/jpeg", DAY));
    }

    @Test
    void 다루지_않는_형식은_거른다() {
        assertThat(PhotoKeys.supports("image/jpeg")).isTrue();
        assertThat(PhotoKeys.supports("IMAGE/JPEG")).isTrue();
        assertThat(PhotoKeys.supports("image/gif")).isFalse();
        assertThat(PhotoKeys.supports(null)).isFalse();
    }
}
