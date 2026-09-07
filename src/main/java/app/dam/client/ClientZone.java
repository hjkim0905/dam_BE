package app.dam.client;

import java.time.ZoneId;

/**
 * 오늘이 언제인지는 서버가 아니라 그 사람이 선 자리가 정한다.
 *
 * 서버 시간대로 재면 서버보다 앞선 곳에 있는 사람이 자정을 넘긴 뒤 한동안
 * "아직 오지 않은 날"이라는 말을 듣는다. 서울 기준이면 시드니가, UTC 기준이면
 * 한국이 그렇게 된다.
 */
public final class ClientZone {

    public static final ZoneId DEFAULT = ZoneId.of("Asia/Seoul");

    private ClientZone() {}

    /** 못 읽는 값이 와도 서울로 본다. 시간대를 몰랐다고 오늘을 막을 수는 없다. */
    public static ZoneId of(String header) {
        if (header == null || header.isBlank()) return DEFAULT;
        try {
            return ZoneId.of(header.trim());
        } catch (Exception e) {
            return DEFAULT;
        }
    }
}
