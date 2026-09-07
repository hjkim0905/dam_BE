package app.dam.auth;

import app.dam.config.DamProperties;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenService {

    private final JwtEncoder encoder;
    private final DamProperties.Session session;

    SessionTokenService(JwtEncoder sessionTokenEncoder, DamProperties properties) {
        this.encoder = sessionTokenEncoder;
        this.session = properties.session();
    }

    public String issue(Long userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("dam")
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(session.ttl()))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return session.ttl().toSeconds();
    }
}
