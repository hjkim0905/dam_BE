package app.dam.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String provider;

    @Column(name = "apple_sub")
    private String appleSub;

    @Column(length = 12)
    private String name;

    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @Column(name = "apple_refresh_token", length = 512)
    private String appleRefreshToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    private User(String provider, String appleSub) {
        this.provider = provider;
        this.appleSub = appleSub;
        this.createdAt = LocalDateTime.now();
    }

    public static User apple(String appleSub) {
        return new User("apple", appleSub);
    }

    /**
     * 온보딩 전에도 세션은 있어야 해서 이름과 동의 시각이 널인 채로 행이 먼저 생긴다.
     * 동의 시각이 찍혔는지가 온보딩을 마쳤는지와 같은 말이다.
     */
    public boolean isOnboarded() {
        return termsAgreedAt != null && privacyAgreedAt != null && name != null;
    }

    public void completeOnboarding(String name) {
        this.name = name;
        LocalDateTime now = LocalDateTime.now();
        this.termsAgreedAt = now;
        this.privacyAgreedAt = now;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void holdAppleRefreshToken(String token) {
        this.appleRefreshToken = token;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAppleRefreshToken() {
        return appleRefreshToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
