package app.dam.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientZoneTest {

    @Test
    @DisplayName("앱이 보낸 시간대를 그대로 쓴다")
    void 앱이_보낸_시간대를_쓴다() {
        assertThat(ClientZone.of("Australia/Sydney")).isEqualTo(ZoneId.of("Australia/Sydney"));
        assertThat(ClientZone.of("America/Los_Angeles")).isEqualTo(ZoneId.of("America/Los_Angeles"));
    }

    @Test
    @DisplayName("헤더가 없으면 서울로 본다")
    void 헤더가_없으면_서울() {
        assertThat(ClientZone.of(null)).isEqualTo(ClientZone.DEFAULT);
        assertThat(ClientZone.of("")).isEqualTo(ClientZone.DEFAULT);
        assertThat(ClientZone.of("   ")).isEqualTo(ClientZone.DEFAULT);
    }

    @Test
    @DisplayName("못 읽는 값이 와도 기록을 막지 않는다")
    void 못_읽는_값은_서울로() {
        // 시간대를 못 읽었다고 사람이 오늘을 못 담게 되어서는 안 된다.
        assertThat(ClientZone.of("Mars/Olympus")).isEqualTo(ClientZone.DEFAULT);
        assertThat(ClientZone.of("../../etc/passwd")).isEqualTo(ClientZone.DEFAULT);
    }

    @Test
    @DisplayName("한국보다 앞선 곳도 오늘을 담을 수 있다")
    void 한국보다_앞선_곳() {
        // 시드니가 자정을 넘긴 순간 서울은 아직 어제다. 서버 시간대로 재면
        // 그 한 시간 동안 시드니 사람은 오늘을 담지 못한다.
        assertThat(ClientZone.of("Pacific/Auckland").getRules()).isNotNull();
    }
}
