package app.dam.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class MeDto {

    private MeDto() {
    }

    public record Onboarding(
            @NotBlank @Size(max = 12) String name,
            @AssertTrue boolean termsAgreed,
            @AssertTrue boolean privacyAgreed) {
    }

    public record Rename(@NotBlank @Size(max = 12) String name) {
    }

    public record Partner(Long id, String name) {
    }

    public record Room(Long id, Partner partner,
                       @JsonFormat(pattern = "yyyy-MM-dd") LocalDate joinedAt) {
    }

    public record Profile(
            Long id,
            String name,
            boolean onboarded,
            long keptCount,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate firstKeptDate,
            Room room) {
    }
}
