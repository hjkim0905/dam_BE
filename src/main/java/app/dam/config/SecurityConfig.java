package app.dam.config;

import app.dam.error.ErrorCode;
import app.dam.security.ApiErrorWriter;
import app.dam.security.AppVersionFilter;
import app.dam.security.OnboardingFilter;
import app.dam.security.SessionAuthenticationFilter;
import app.dam.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(DamProperties.class)
public class SecurityConfig {

    /**
     * 필터를 빈으로 두지 않고 여기서 만든다. Filter 타입 빈은 부트가 서블릿 체인에도
     * 자동 등록해서 시큐리티 체인 밖에서 한 번 더 돈다.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtDecoder sessionTokenDecoder,
                                    UserRepository users,
                                    ApiErrorWriter errors,
                                    DamProperties properties,
                                    @Qualifier("corsConfigurationSource")
                                    CorsConfigurationSource cors) throws Exception {
        var session = new SessionAuthenticationFilter(sessionTokenDecoder, users);
        var onboarding = new OnboardingFilter(errors);
        var version = new AppVersionFilter(properties.client().minVersion(), errors);

        return http
                // 토큰으로만 인증한다. 쿠키를 안 쓰니 CSRF 로 태울 자격증명 자체가 없다.
                .csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(cors))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                errors.write(response, ErrorCode.UNAUTHENTICATED)))
                // 버전은 인증보다 먼저 본다. 낡은 앱은 로그인 여부와 상관없이 막아야 한다.
                // 기준점은 표준 필터라야 한다. 우리가 만든 필터를 가리키면
                // "등록된 순서가 없다"며 뜨지 않는다.
                .addFilterBefore(version, SecurityContextHolderFilter.class)
                .addFilterBefore(session, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(onboarding, SessionAuthenticationFilter.class)
                .build();
    }

    /**
     * 웹뷰가 띄우는 Next.js 번들이 다른 오리진에서 호출한다. 자격증명은 헤더로만
     * 오가고 쿠키를 쓰지 않아서 allowCredentials 는 꺼둔다.
     */
    // HandlerMappingIntrospector 도 CorsConfigurationSource 라서 이름으로 집어야 한다.
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${dam.cors.origins:*}") List<String> origins) {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
