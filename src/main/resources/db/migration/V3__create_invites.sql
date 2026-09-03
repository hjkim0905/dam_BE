CREATE TABLE invites (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    code       CHAR(6)  NOT NULL,
    room_id    BIGINT   NOT NULL,
    created_by BIGINT   NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at    DATETIME NULL,
    used_by    BIGINT   NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invites_code (code),
    KEY ix_invites_room_used (room_id, used_at),
    CONSTRAINT fk_invites_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_invites_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_invites_used_by FOREIGN KEY (used_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
