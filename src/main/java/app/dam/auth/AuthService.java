package app.dam.auth;

import app.dam.user.User;
import app.dam.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final SessionTokenService tokens;
    private final AppleClient apple;

    AuthService(UserRepository users, SessionTokenService tokens, AppleClient apple) {
        this.users = users;
        this.tokens = tokens;
        this.apple = apple;
    }

    @Transactional
    public AuthDto.Session loginWithApple(AuthDto.AppleLogin request) {
        String appleSub = apple.verify(request.identityToken());
        User user = users.findByAppleSub(appleSub)
                .orElseGet(() -> users.save(User.apple(appleSub)));

        if (request.authorizationCode() != null && !request.authorizationCode().isBlank()) {
            String refreshToken = apple.exchangeForRefreshToken(request.authorizationCode());
            if (refreshToken != null) {
                user.holdAppleRefreshToken(refreshToken);
            }
        }
        return session(user);
    }

    private AuthDto.Session session(User user) {
        return new AuthDto.Session(
                tokens.issue(user.getId()),
                tokens.ttlSeconds(),
                new AuthDto.Me(user.getId(), user.getName(), user.isOnboarded()));
    }
}
