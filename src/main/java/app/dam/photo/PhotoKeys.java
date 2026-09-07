package app.dam.photo;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class PhotoKeys {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/heic", "heic",
            "image/webp", "webp");

    private PhotoKeys() {
    }

    public static boolean supports(String contentType) {
        return contentType != null && EXTENSIONS.containsKey(contentType.toLowerCase());
    }

    /**
     * 유저 id 로 앞을 나눠 한 접두사 아래 파일이 무한히 쌓이지 않게 한다.
     * 날짜까지 넣어두면 나중에 오래된 것을 다룰 때 키만 보고 고를 수 있다.
     */
    public static String next(Long userId, String contentType, LocalDate today) {
        String extension = EXTENSIONS.get(contentType.toLowerCase());
        return "u%d/%04d/%02d/%s.%s".formatted(
                userId, today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""), extension);
    }
}
