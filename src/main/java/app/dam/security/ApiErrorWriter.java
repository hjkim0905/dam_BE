package app.dam.security;

import app.dam.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * 필터에서 나가는 에러도 컨트롤러와 같은 모양이어야 한다. 여기서만 다르면
 * 프론트가 401 하나 때문에 별도 분기를 갖게 된다.
 */
@Component
public class ApiErrorWriter {

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 값이 둘 다 우리가 정한 상수라 직렬화기를 끌어올 이유가 없다.
        response.getWriter().write(
                "{\"code\":\"%s\",\"message\":\"%s\"}".formatted(code.name(), code.message()));
    }
}
