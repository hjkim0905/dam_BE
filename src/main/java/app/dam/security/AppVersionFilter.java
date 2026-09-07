package app.dam.security;

import app.dam.client.AppVersion;
import app.dam.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 너무 낡은 앱은 아예 받지 않는다. 최소 버전만 내려 주고 앱이 스스로 판단하게 하면,
 * 그 화면을 지나친 앱은 API 를 그대로 쓸 수 있어서 강제가 되지 않는다.
 *
 * 헤더가 없는 요청은 통과시킨다. 브라우저에서 연 웹과 헬스체크가 그렇고,
 * 여기서 막으면 앱과 무관한 것들이 같이 죽는다.
 */
public class AppVersionFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-App-Version";

    private final String minVersion;
    private final ApiErrorWriter errors;

    public AppVersionFilter(String minVersion, ApiErrorWriter errors) {
        this.minVersion = minVersion;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String version = request.getHeader(HEADER);

        if (version != null && AppVersion.isOlderThan(version, minVersion)) {
            errors.write(response, ErrorCode.UPDATE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
