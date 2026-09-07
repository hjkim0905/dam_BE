package app.dam.security;

import app.dam.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 온보딩 전에는 온보딩과 내 정보 조회만 열어둔다. 각 컨트롤러에서 검사하면
 * 새 엔드포인트를 추가할 때마다 빠뜨릴 수 있다.
 */
public class OnboardingFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of("/me/onboarding", "/me");

    private final ApiErrorWriter errors;

    public OnboardingFilter(ApiErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean blocked = authentication != null
                && authentication.getPrincipal() instanceof SessionUser user
                && !user.onboarded()
                && !ALLOWED.contains(path);

        if (blocked) {
            errors.write(response, ErrorCode.ONBOARDING_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
