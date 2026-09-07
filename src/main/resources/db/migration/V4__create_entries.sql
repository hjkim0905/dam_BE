CREATE TABLE entries (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    room_id    BIGINT       NULL,
    entry_date DATE         NOT NULL,
    color      CHAR(7)      NOT NULL,
    photo_key  VARCHAR(255) NOT NULL,
    memo       VARCHAR(255) NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entries_user_date (user_id, entry_date),
    KEY ix_entries_room_date (room_id, entry_date),
    CONSTRAINT fk_entries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_entries_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
