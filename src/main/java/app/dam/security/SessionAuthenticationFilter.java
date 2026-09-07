package app.dam.security;

import app.dam.user.User;
import app.dam.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 토큰에는 사용자 id 만 담는다. 이름과 온보딩 여부는 바뀌는 값이라 매 요청 DB 에서 읽는다.
 * 토큰에 넣어두면 닉네임을 바꿔도 토큰이 만료될 때까지 옛 값이 따라다닌다.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder decoder;
    private final UserRepository users;

    public SessionAuthenticationFilter(JwtDecoder sessionTokenDecoder, UserRepository users) {
        this.decoder = sessionTokenDecoder;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            users.findById(Long.valueOf(jwt.getSubject())).ifPresent(this::hold);
        } catch (JwtException | NumberFormatException e) {
            // 인증 실패는 여기서 판정하지 않는다. 컨텍스트를 비워두면 진입점이 401 을 만든다.
            SecurityContextHolder.clearContext();
        }
    }

    private void hold(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new SessionUser(user.getId(), user.isOnboarded()), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
