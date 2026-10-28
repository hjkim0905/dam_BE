package app.dam.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import org.junit.jupiter.api.Test;

class DiaryItemTest {

    private static final Placement CENTER = new Placement(0.5, 0.5, 1, 0);

    @Test
    void 페이지_밖으로_멀리_끌어낸_것은_가장자리에_걸쳐_둔다() {
        Placement placed = new Placement(3, -2, 1, 0).normalized();

        assertThat(placed.x()).isEqualTo(Placement.MAX_POSITION);
        assertThat(placed.y()).isEqualTo(Placement.MIN_POSITION);
    }

    @Test
    void 너무_작거나_크게_키운_것은_한도에_맞춘다() {
        assertThat(new Placement(0.5, 0.5, 0.01, 0).normalized().scale()).isEqualTo(Placement.MIN_SCALE);
        assertThat(new Placement(0.5, 0.5, 40, 0).normalized().scale()).isEqualTo(Placement.MAX_SCALE);
    }

    @Test
    void 여러_바퀴_돌린_각도는_한_바퀴_안으로_접는다() {
        double turned = new Placement(0.5, 0.5, 1, Math.PI * 5 + 0.25).normalized().rotation();

        assertThat(turned).isCloseTo(-Math.PI + 0.25, within(1e-9));
    }

    @Test
    void 숫자가_아닌_값은_받지_않는다() {
        assertThatThrownBy(() -> new Placement(Double.NaN, 0.5, 1, 0).normalized())
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new Placement(0.5, 0.5, Double.POSITIVE_INFINITY, 0).normalized())
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 글씨는_앞뒤_공백을_떼고_비어_있으면_받지_않는다() {
        DiaryItem item = DiaryItem.text(1L, "  담다디  ", CENTER, 0, 9L);

        assertThat(item.getText()).isEqualTo("담다디");
        assertThatThrownBy(() -> DiaryItem.text(1L, "   ", CENTER, 0, 9L)).isInstanceOf(ApiException.class);
    }

    @Test
    void 스티커_이름은_정해진_모양만_받는다() {
        DiaryItem.sticker(1L, "heart-pink", CENTER, 0, 9L);

        assertThatThrownBy(() -> DiaryItem.sticker(1L, "../etc/passwd", CENTER, 0, 9L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 다른_사람이_먼저_옮겼으면_덮어쓰지_않는다() {
        DiaryItem item = DiaryItem.sticker(1L, "star", CENTER, 0, 9L);

        assertThatThrownBy(() -> item.place(new Placement(0.2, 0.2, 1, 0), 1, 3L))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ErrorCode.DIARY_ITEM_CONFLICT);
    }

    @Test
    void 같은_버전이면_옮긴다() {
        DiaryItem item = DiaryItem.sticker(1L, "star", CENTER, 0, 9L);

        item.place(new Placement(0.2, 0.3, 1.5, 0.4), 4, item.getVersion());

        assertThat(item.getX()).isEqualTo(0.2);
        assertThat(item.getY()).isEqualTo(0.3);
        assertThat(item.getScale()).isEqualTo(1.5);
        assertThat(item.getRotation()).isEqualTo(0.4);
        assertThat(item.getLayer()).isEqualTo(4);
    }
}
