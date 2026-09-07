package app.dam.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dam")
public record DamProperties(Session session, Apple apple, Storage storage, Client client) {

    /**
     * 이 버전보다 낮은 앱은 받지 않는다. 앱을 새로 내지 않고 여기만 고쳐서
     * 강제할 수 있어야 하므로 코드가 아니라 설정에 둔다.
     */
    public record Client(String minVersion) {
    }

    public record Session(String secret, Duration ttl) {
    }

    public record Apple(String bundleId, String teamId, String keyId, String privateKey) {

        public boolean configured() {
            return notBlank(bundleId) && notBlank(teamId) && notBlank(keyId) && notBlank(privateKey);
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }

    public record Storage(
            String bucket,
            String region,
            String endpoint,
            String accessKey,
            String secretKey,
            Duration uploadTtl,
            Duration readTtl,
            long maxBytes
    ) {
    }
}
