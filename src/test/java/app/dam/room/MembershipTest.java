package app.dam.room;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MembershipTest {

    @Test
    void 나가면_나간_시각이_찍힌다() {
        Membership membership = Membership.join(1L, 2L);
        membership.leave();

        assertThat(membership.getRoomId()).isEqualTo(1L);
        assertThat(membership.getUserId()).isEqualTo(2L);
    }

    @Test
    void 돌아오면_같은_행이_되살아난다() {
        Membership membership = Membership.join(1L, 2L);
        var firstJoin = membership.getJoinedAt();
        membership.leave();

        membership.rejoin();

        assertThat(membership.getJoinedAt()).isAfterOrEqualTo(firstJoin);
    }
}
