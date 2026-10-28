-- 다꾸 책. 방에 있으면 방의 책이고, 혼자면 내 책이다. 둘 중 하나만 채운다.
-- 혼자일 때 만든 책은 방에 들어가도 내 것으로 남는다.
CREATE TABLE diaries (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NULL,
    room_id     BIGINT   NULL,
    year        INT      NOT NULL,
    cover_color CHAR(7)  NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    -- 주인 하나에 한 해 한 권. room_id 가 NULL 이면 유니크 키가 걸리지 않으므로
    -- 주인을 한 칸으로 합쳐 건다.
    owner_key   VARCHAR(24) AS (COALESCE(CONCAT('r', room_id), CONCAT('u', user_id))) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_diaries_owner_year (owner_key, year),
    KEY ix_diaries_room (room_id),
    CONSTRAINT ck_diaries_one_owner CHECK ((user_id IS NULL) <> (room_id IS NULL)),
    -- 탈퇴하면 혼자 쓰던 책은 같이 사라진다(MeService). 방의 책은 남은 사람에게 남는다.
    -- owner_key 와 CHECK 가 user_id 를 읽어서 MySQL 이 여기에 CASCADE 를 허락하지 않는다.
    CONSTRAINT fk_diaries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_diaries_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE diary_pages (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    diary_id   BIGINT   NOT NULL,
    position   INT      NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY ix_diary_pages_diary_position (diary_id, position),
    CONSTRAINT fk_diary_pages_diary FOREIGN KEY (diary_id) REFERENCES diaries (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 페이지에 붙인 것 하나하나. 둘이 같은 책을 꾸미므로 페이지를 통째로 덮어쓰지 않고
-- 붙인 것마다 버전을 둔다. 서로 다른 것을 만지면 부딪히지 않는다.
CREATE TABLE diary_items (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    page_id    BIGINT       NOT NULL,
    kind       VARCHAR(12)  NOT NULL,
    entry_id   BIGINT       NULL,
    sticker    VARCHAR(32)  NULL,
    frame      VARCHAR(16)  NULL,
    text       VARCHAR(80)  NULL,
    x          DOUBLE       NOT NULL,
    y          DOUBLE       NOT NULL,
    scale      DOUBLE       NOT NULL,
    rotation   DOUBLE       NOT NULL,
    layer      INT          NOT NULL,
    version    BIGINT       NOT NULL,
    created_by BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY ix_diary_items_page (page_id),
    CONSTRAINT fk_diary_items_page FOREIGN KEY (page_id) REFERENCES diary_pages (id) ON DELETE CASCADE,
    -- 붙인 사진의 기록이 지워지면 사진도 떨어진다. 지운 사진이 책에 남아 있으면 안 된다.
    CONSTRAINT fk_diary_items_entry FOREIGN KEY (entry_id) REFERENCES entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_diary_items_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
