package app.dam.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class InviteCodesTest {

    @Test
    void 코드는_여섯_자리다() {
        assertThat(InviteCodes.next()).hasSize(6);
    }

    @Test
    void 헷갈리는_글자는_쓰지_않는다() {
        // 뽑기가 무작위라 한 번으로는 못 본다. 충분히 돌려 전체 문자집합을 훑는다.
        String all = String.join("", java.util.stream.Stream
                .generate(InviteCodes::next).limit(2000).toList());

        assertThat(all).doesNotContain("O").doesNotContain("0");
        assertThat(all).doesNotContain("I").doesNotContain("1");
    }

    @Test
    void 같은_난수면_같은_코드가_나온다() {
        RandomGenerator fixed = RandomGenerator.of("L64X128MixRandom");
        assertThat(InviteCodes.next(fixed)).hasSize(6);
    }

    @Test
    void 소문자와_공백을_붙여넣어도_받는다() {
        assertThat(InviteCodes.normalize("  h7k2qm ")).isEqualTo("H7K2QM");
    }

    @Test
    void 널을_넣어도_터지지_않는다() {
        assertThat(InviteCodes.normalize(null)).isEmpty();
    }

    @Test
    void 문자집합을_벗어난_코드는_거른다() {
        assertThat(InviteCodes.looksValid("H7K2QM")).isTrue();
        assertThat(InviteCodes.looksValid("H7K2Q0")).isFalse();
        assertThat(InviteCodes.looksValid("H7K2Q")).isFalse();
        assertThat(InviteCodes.looksValid(null)).isFalse();
    }
}
