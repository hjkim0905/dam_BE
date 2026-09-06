package app.dam.auth;

import app.dam.config.DamProperties;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 앱이 애플에서 받아온 토큰을 서버가 확인하는 자리다. 서버가 사용자를 애플로
 * 보내는 흐름이 아니라서 OAuth2 Client 가 아니라 검증만 필요하다.
 */
@Component
public class AppleClient {

    private static final Logger log = LoggerFactory.getLogger(AppleClient.class);

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String JWKS = ISSUER + "/auth/keys";
    private static final String TOKEN = ISSUER + "/auth/token";
    private static final String REVOKE = ISSUER + "/auth/revoke";

    private final DamProperties.Apple apple;
    private final RestClient http = RestClient.create();
    private final JwtDecoder decoder;

    AppleClient(DamProperties properties) {
        this.apple = properties.apple();
        this.decoder = apple.configured() ? buildDecoder(apple.bundleId()) : null;
    }

    private static JwtDecoder buildDecoder(String bundleId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWKS).build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud", audiences -> audiences != null && audiences.contains(bundleId));
        decoder.setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(ISSUER), audience));
        return decoder;
    }

    public boolean configured() {
        return apple.configured();
    }

    /** identityToken 의 서명과 iss, aud, exp 를 확인하고 sub 을 돌려준다. */
    public String verify(String identityToken) {
        requireConfigured();
        try {
            return decoder.decode(identityToken).getSubject();
        } catch (JwtException e) {
            log.info("애플 토큰 검증 실패", e);
            throw ApiException.of(ErrorCode.APPLE_TOKEN_INVALID);
        }
    }

    /**
     * refresh token 은 탈퇴할 때 폐기하려고 받아둔다. 교환에 실패해도 로그인 자체는
     * 진행한다. 여기서 막으면 애플이 잠깐 흔들릴 때 아무도 로그인하지 못한다.
     */
    public String exchangeForRefreshToken(String authorizationCode) {
        requireConfigured();
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", apple.bundleId());
            form.add("client_secret", clientSecret());
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");

            Map<?, ?> body = http.post().uri(TOKEN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return body == null ? null : (String) body.get("refresh_token");
        } catch (RuntimeException e) {
            log.warn("애플 토큰 교환에 실패했다. 로그인은 그대로 진행한다", e);
            return null;
        }
    }

    public void revoke(String refreshToken) {
        if (refreshToken == null || !apple.configured()) {
            return;
        }
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", apple.bundleId());
            form.add("client_secret", clientSecret());
            form.add("token", refreshToken);
            form.add("token_type_hint", "refresh_token");

            http.post().uri(REVOKE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            // 폐기에 실패했다고 탈퇴를 막으면 사용자가 계정을 못 지운다.
            log.warn("애플 토큰 폐기에 실패했다", e);
        }
    }

    /**
     * client_secret 은 고정 문자열이 아니라 p8 키로 매번 서명해 만드는 JWT 다.
     * 애플이 최대 6개월까지 허용하지만 짧게 잡아 매 호출 새로 만든다.
     */
    String clientSecret() {
        requireConfigured();
        try {
            Instant now = Instant.now();
            var claims = new JWTClaimsSet.Builder()
                    .issuer(apple.teamId())
                    .subject(apple.bundleId())
                    .audience(ISSUER)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(300)))
                    .build();
            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(apple.keyId())
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(privateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("애플 client_secret 을 만들지 못했다", e);
        }
    }

    private ECPrivateKey privateKey() {
        try {
            // 머리말을 떼고 base64 가 아닌 글자를 모두 버린다. 줄바꿈이 진짜 개행이든
            // 문자 그대로의 \n 이든, 환경변수에 어떻게 들어와도 같은 값이 된다.
            String pem = apple.privateKey()
                    .replaceAll("-----[A-Z ]+-----", "")
                    .replaceAll("[^A-Za-z0-9+/=]", "");
            var spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem));
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("APPLE_PRIVATE_KEY 를 읽지 못했다", e);
        }
    }

    private void requireConfigured() {
        if (!apple.configured()) {
            throw ApiException.of(ErrorCode.APPLE_NOT_CONFIGURED);
        }
    }
}
