CREATE TABLE rooms (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE memberships (
    id        BIGINT   NOT NULL AUTO_INCREMENT,
    room_id   BIGINT   NOT NULL,
    user_id   BIGINT   NOT NULL,
    joined_at DATETIME NOT NULL,
    left_at   DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_memberships_room_user (room_id, user_id),
    KEY ix_memberships_user_left (user_id, left_at),
    CONSTRAINT fk_memberships_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
