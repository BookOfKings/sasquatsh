CREATE TABLE IF NOT EXISTS board_games (
    id CHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(400) NULL,
    host VARCHAR(80) NULL,
    location VARCHAR(120) NULL,
    max_seats INT NOT NULL DEFAULT 4,
    start_time TIME NOT NULL DEFAULT '09:00:00',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservations (
    id CHAR(36) NOT NULL,
    board_game_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    reserved_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_game FOREIGN KEY (board_game_id) REFERENCES board_games (id) ON DELETE CASCADE,
    CONSTRAINT ux_reservation_email UNIQUE (board_game_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
