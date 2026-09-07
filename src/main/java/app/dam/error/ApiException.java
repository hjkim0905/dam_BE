package app.dam.error;

public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static ApiException of(ErrorCode code) {
        return new ApiException(code);
    }
}
