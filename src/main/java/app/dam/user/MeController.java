package app.dam.user;

import app.dam.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeController {

    private final MeService me;

    MeController(MeService me) {
        this.me = me;
    }

    @GetMapping
    MeDto.Profile profile() {
        return me.profile(CurrentUser.id());
    }

    @PostMapping("/onboarding")
    MeDto.Profile onboarding(@Valid @RequestBody MeDto.Onboarding request) {
        return me.completeOnboarding(CurrentUser.id(), request);
    }

    @PatchMapping
    MeDto.Profile rename(@Valid @RequestBody MeDto.Rename request) {
        return me.rename(CurrentUser.id(), request.name());
    }

    /**
     * 사진 삭제를 트랜잭션 밖으로 빼려고 두 단계로 나눈다. 서비스 안에서 같이
     * 지우면 스토리지가 흔들릴 때 계정 삭제까지 되돌아간다.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw() {
        List<String> photoKeys = me.withdraw(CurrentUser.id());
        me.erasePhotos(photoKeys);
    }
}
