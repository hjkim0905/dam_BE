CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    provider            VARCHAR(16)  NOT NULL,
    apple_sub           VARCHAR(255) NULL,
    name                VARCHAR(12)  NULL,
    terms_agreed_at     DATETIME     NULL,
    privacy_agreed_at   DATETIME     NULL,
    apple_refresh_token VARCHAR(512) NULL,
    created_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_apple_sub (apple_sub)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
