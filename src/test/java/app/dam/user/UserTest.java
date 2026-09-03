package app.dam.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 갓_만든_유저는_온보딩_전이다() {
        assertThat(User.apple("sub-1").isOnboarded()).isFalse();
    }

    @Test
    void 온보딩을_마치면_이름과_동의_시각이_함께_생긴다() {
        User user = User.apple("sub-1");

        user.completeOnboarding("지호");

        assertThat(user.isOnboarded()).isTrue();
        assertThat(user.getName()).isEqualTo("지호");
    }

    @Test
    void 이름만_바꿔도_온보딩_상태는_그대로다() {
        User user = User.apple("sub-1");
        user.completeOnboarding("지호");

        user.rename("민서");

        assertThat(user.getName()).isEqualTo("민서");
        assertThat(user.isOnboarded()).isTrue();
    }
}
