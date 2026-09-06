package app.dam.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.dam.config.DamProperties;
import app.dam.error.ApiException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * client_secret 은 고정 문자열이 아니라 p8 키로 매번 서명해 만드는 JWT 다.
 * 실제 로그인 때만 도는 경로라, 여기서 막아두지 않으면 배포 뒤에야 깨진 걸 안다.
 */
class AppleClientTest {

    private static final String BUNDLE_ID = "com.hjkim.dam";
    private static final String TEAM_ID = "RL59ND3RNM";
    private static final String KEY_ID = "ABCDE12345";

    private static KeyPair p256() throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static AppleClient clientWith(String privateKey) {
        return new AppleClient(new DamProperties(
                new DamProperties.Session("secret", Duration.ofDays(30)),
                new DamProperties.Apple(BUNDLE_ID, TEAM_ID, KEY_ID, privateKey),
                null));
    }

    private static String pkcs8(KeyPair keys) {
        return Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
    }

    @Test
    void 애플이_읽을_수_있는_ES256_토큰을_만든다() throws Exception {
        KeyPair keys = p256();

        SignedJWT jwt = SignedJWT.parse(clientWith(pkcs8(keys)).clientSecret());

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
        assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keys.getPublic()))).isTrue();
    }

    @Test
    void 애플이_보는_자리에_맞는_값을_담는다() throws Exception {
        SignedJWT jwt = SignedJWT.parse(clientWith(pkcs8(p256())).clientSecret());
        var claims = jwt.getJWTClaimsSet();

        // 발급자는 팀, 주체는 앱이다. 둘을 바꿔 넣으면 애플이 조용히 거절한다.
        assertThat(claims.getIssuer()).isEqualTo(TEAM_ID);
        assertThat(claims.getSubject()).isEqualTo(BUNDLE_ID);
        assertThat(claims.getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(KEY_ID);
        assertThat(claims.getExpirationTime()).isAfter(claims.getIssueTime());
    }

    @Test
    void 머리말과_줄바꿈이_섞여_있어도_읽는다() throws Exception {
        KeyPair keys = p256();
        String base64 = pkcs8(keys);
        // .p8 파일을 통째로 붙여넣은 모양. 환경변수로 넣다 보면 흔하다.
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + base64.replaceAll("(.{64})", "$1\n")
                + "\n-----END PRIVATE KEY-----\n";

        SignedJWT jwt = SignedJWT.parse(clientWith(pem).clientSecret());

        assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keys.getPublic()))).isTrue();
    }

    @Test
    void 설정이_비어_있으면_준비되지_않았다고_답한다() {
        assertThatThrownBy(() -> clientWith("").clientSecret())
                .isInstanceOf(ApiException.class);
    }
}
