package app.dam.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * 세션 토큰은 우리가 발급하고 우리가 검증한다. 서버가 하나뿐이라 대칭키로 충분하다.
 * 검증하는 쪽이 늘어나면 그때 비대칭으로 바꾼다.
 */
@Configuration
public class JwtConfig {

    private final SecretKeySpec key;

    JwtConfig(DamProperties properties) {
        this.key = new SecretKeySpec(
                properties.session().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder sessionTokenEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder sessionTokenDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
