package app.dam.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InviteTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 3, 12, 0);

    @Test
    void 만료_시각이_지나면_만료다() {
        Invite invite = Invite.issue("H7K2QM", 1L, 2L, NOW.minusSeconds(1));

        assertThat(invite.isExpired(NOW)).isTrue();
    }

    @Test
    void 만료_전이면_살아_있다() {
        Invite invite = Invite.issue("H7K2QM", 1L, 2L, NOW.plusHours(24));

        assertThat(invite.isExpired(NOW)).isFalse();
        assertThat(invite.isUsed()).isFalse();
    }
}
