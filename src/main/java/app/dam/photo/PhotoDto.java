package app.dam.photo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public final class PhotoDto {

    private PhotoDto() {
    }

    public record UploadRequest(@NotBlank String contentType, @Positive long contentLength) {
    }

    public record UploadTicket(String photoKey, String uploadUrl, long expiresIn) {
    }
}
