package app.dam.auth;

import jakarta.validation.constraints.NotBlank;

public final class AuthDto {

    private AuthDto() {
    }

    public record AppleLogin(@NotBlank String identityToken,
                             String authorizationCode,
                             String fullName) {
    }

    public record Session(String accessToken, long expiresIn, Me user) {
    }

    public record Me(Long id, String name, boolean onboarded) {
    }
}
