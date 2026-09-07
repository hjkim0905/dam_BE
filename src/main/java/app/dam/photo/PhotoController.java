package app.dam.photo;

import app.dam.config.DamProperties;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import app.dam.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/photos")
public class PhotoController {

    private final PhotoStorage storage;
    private final DamProperties.Storage settings;

    PhotoController(PhotoStorage storage, DamProperties properties) {
        this.storage = storage;
        this.settings = properties.storage();
    }

    /**
     * 서버는 서명만 해주고 사진 바이트는 앱이 스토리지로 직접 올린다.
     * 크기는 서명 전에 본다. 한 번 내준 뒤에는 막을 길이 없다.
     */
    @PostMapping("/upload-url")
    PhotoDto.UploadTicket uploadUrl(@Valid @RequestBody PhotoDto.UploadRequest request) {
        if (!PhotoKeys.supports(request.contentType())) {
            throw ApiException.of(ErrorCode.PHOTO_TYPE_UNSUPPORTED);
        }
        if (request.contentLength() > settings.maxBytes()) {
            throw ApiException.of(ErrorCode.PHOTO_TOO_LARGE);
        }

        String key = PhotoKeys.next(CurrentUser.id(), request.contentType(), LocalDate.now());
        return new PhotoDto.UploadTicket(
                key, storage.uploadUrl(key, request.contentType()), settings.uploadTtl().toSeconds());
    }
}
