package app.dam.entry;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class EntryDto {

    private EntryDto() {
    }

    public record Keep(
            @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color,
            @NotBlank @Size(max = 255) String photoKey,
            @Size(max = 255) String memo) {
    }

    public record Author(Long id, String name) {
    }

    public record Kept(
            Long id,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            String color,
            String photoUrl,
            String memo,
            Author author) {
    }

    public record Page(List<Kept> entries) {
    }

    /** 내것, 함께, 상대것. 프론트의 필터와 같은 말이다. */
    public enum View {
        MINE, BOTH, THEIRS
    }
}
