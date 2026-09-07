package app.dam.room;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class RoomDto {

    private RoomDto() {
    }

    public record JoinRequest(@NotBlank String code) {
    }

    public record InviteIssued(String code, LocalDateTime expiresAt) {
    }

    public record Partner(Long id, String name) {
    }

    public record Joined(Long roomId, Partner partner, LocalDate joinedAt) {
    }
}
