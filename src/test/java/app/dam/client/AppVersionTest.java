package app.dam.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppVersionTest {

    @Test
    void 자리마다_숫자로_견준다() {
        // 문자열로 비교하면 "1.10.0" 이 "1.9.0" 보다 작다고 나온다.
        assertThat(AppVersion.isOlderThan("1.9.0", "1.10.0")).isTrue();
        assertThat(AppVersion.isOlderThan("1.10.0", "1.9.0")).isFalse();
    }

    @Test
    void 같은_버전은_낡지_않았다() {
        assertThat(AppVersion.isOlderThan("1.2.3", "1.2.3")).isFalse();
    }

    @Test
    void 자리_수가_달라도_견준다() {
        assertThat(AppVersion.isOlderThan("1.2", "1.2.1")).isTrue();
        assertThat(AppVersion.isOlderThan("1.2.0", "1.2")).isFalse();
        assertThat(AppVersion.isOlderThan("2", "1.9.9")).isFalse();
    }

    @Test
    void 읽을_수_없는_자리는_0_으로_본다() {
        assertThat(AppVersion.isOlderThan("1.0.0", "nope")).isFalse();
        assertThat(AppVersion.isOlderThan(null, "1.0.0")).isTrue();
        assertThat(AppVersion.isOlderThan("", "1.0.0")).isTrue();
    }
}
