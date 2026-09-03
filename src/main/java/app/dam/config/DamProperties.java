package app.dam.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dam")
public record DamProperties(Session session, Apple apple, Storage storage) {

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
