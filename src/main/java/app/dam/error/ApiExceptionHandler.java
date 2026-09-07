package app.dam.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handle(ApiException e) {
        return ResponseEntity.status(e.code().status()).body(ErrorResponse.of(e.code()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        log.debug("잘못된 요청", e);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    /**
     * 없는 경로나 안 받는 메서드처럼 스프링이 스스로 상태코드를 아는 예외가 있다.
     * 그 인터페이스는 Throwable 이 아니라 애너테이션으로 못 잡아서 여기서 가른다.
     * 안 그러면 404 와 405 가 전부 500 으로 나간다.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse known) {
            var status = known.getStatusCode();
            log.debug("처리할 수 없는 요청", e);
            return ResponseEntity.status(status).body(new ErrorResponse(
                    status.toString().replaceAll("^\\d+\\W*", "").replaceAll("\\W+", "_").toUpperCase(),
                    "요청을 처리할 수 없어요"));
        }
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "잠시 뒤에 다시 시도해 주세요"));
    }
}
