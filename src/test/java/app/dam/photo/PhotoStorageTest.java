package app.dam.photo;

import static org.assertj.core.api.Assertions.assertThat;

import app.dam.config.DamProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PhotoStorageTest {

    private static PhotoStorage storage(Duration readTtl) {
        return new PhotoStorage(new DamProperties(
                null,
                null,
                new DamProperties.Storage(
                        "dam",
                        "ap-tokyo-1",
                        "https://example.compat.objectstorage.ap-tokyo-1.oraclecloud.com",
                        "access",
                        "c2VjcmV0LWtleS1mb3ItdGVzdA==",
                        Duration.ofMinutes(5),
                        readTtl,
                        10_000_000L)));
    }

    @Test
    void 같은_사진의_주소는_그대로다() {
        // 주소가 매번 달라지면 브라우저가 캐시를 못 찾아 달력을 열 때마다 다시 받는다.
        var photos = storage(Duration.ofDays(7));

        assertThat(photos.readUrl("u1/2026/08/a.jpg")).isEqualTo(photos.readUrl("u1/2026/08/a.jpg"));
    }

    @Test
    void 다른_사진은_다른_주소를_받는다() {
        var photos = storage(Duration.ofDays(7));

        assertThat(photos.readUrl("u1/2026/08/a.jpg"))
                .isNotEqualTo(photos.readUrl("u1/2026/08/b.jpg"));
    }

    @Test
    void 수명이_다하면_다시_서명한다() throws InterruptedException {
        // 돌려쓰는 창은 수명의 절반이라 1초짜리면 0.5초다. 그 뒤엔 새 서명이 나와야 한다.
        var photos = storage(Duration.ofSeconds(1));
        String first = photos.readUrl("u1/2026/08/a.jpg");

        Thread.sleep(600);

        assertThat(photos.readUrl("u1/2026/08/a.jpg")).isNotEqualTo(first);
    }

    @Test
    void 지운_사진의_주소는_남지_않는다() {
        var photos = storage(Duration.ofDays(7));
        String before = photos.readUrl("u1/2026/08/a.jpg");

        photos.delete("u1/2026/08/a.jpg");

        assertThat(photos.readUrl("u1/2026/08/a.jpg")).isNotEqualTo(before);
    }
}
