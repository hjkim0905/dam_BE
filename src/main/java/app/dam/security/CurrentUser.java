package app.dam.security;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 컨트롤러가 매번 SecurityContext 를 헤집지 않도록 한 군데로 모은다. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static SessionUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser user)) {
            throw ApiException.of(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    public static Long id() {
        return get().id();
    }
}
