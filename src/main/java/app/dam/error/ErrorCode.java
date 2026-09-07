package app.dam.error;

import org.springframework.http.HttpStatus;

/** 프론트가 그대로 보여줘도 되는 문장이어야 한다. */
public enum ErrorCode {

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"),
    UPDATE_REQUIRED(HttpStatus.UPGRADE_REQUIRED, "앱을 업데이트해 주세요"),
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "온보딩을 먼저 마쳐 주세요"),
    CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "약관에 동의해야 시작할 수 있어요"),
    NOT_MY_ENTRY(HttpStatus.FORBIDDEN, "내가 담은 것만 지울 수 있어요"),
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "그 기록을 찾을 수 없어요"),
    FUTURE_DATE(HttpStatus.BAD_REQUEST, "아직 오지 않은 날은 담을 수 없어요"),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "그런 초대코드가 없어요"),
    INVITE_EXPIRED(HttpStatus.GONE, "초대코드가 만료됐어요"),
    INVITE_USED(HttpStatus.GONE, "이미 사용된 초대코드예요"),
    INVITE_SELF(HttpStatus.BAD_REQUEST, "내가 만든 코드는 쓸 수 없어요"),
    ALREADY_IN_ROOM(HttpStatus.CONFLICT, "이미 방에 있어요"),
    NOT_IN_ROOM(HttpStatus.CONFLICT, "속한 방이 없어요"),
    ROOM_FULL(HttpStatus.CONFLICT, "방이 이미 둘로 찼어요"),
    PHOTO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "사진이 너무 커요"),
    PHOTO_TYPE_UNSUPPORTED(HttpStatus.BAD_REQUEST, "이 형식의 사진은 올릴 수 없어요"),
    APPLE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "애플 로그인을 확인하지 못했어요"),
    APPLE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "애플 로그인이 아직 준비되지 않았어요"),
    STORAGE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "사진 저장소가 아직 준비되지 않았어요"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청을 이해하지 못했어요");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
