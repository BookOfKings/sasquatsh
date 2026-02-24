-- Events table
CREATE TABLE IF NOT EXISTS events (
    id CHAR(36) NOT NULL,
    host_user_id CHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NULL,
    game_title VARCHAR(160) NULL,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 120,
    setup_minutes INT NOT NULL DEFAULT 15,
    address_line1 VARCHAR(200) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(50) NULL,
    postal_code VARCHAR(20) NULL,
    location_details VARCHAR(200) NULL,
    difficulty_level ENUM('beginner', 'intermediate', 'advanced') NULL,
    max_players INT NOT NULL DEFAULT 4,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    is_charity_event BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('draft', 'published', 'cancelled', 'completed') NOT NULL DEFAULT 'draft',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_events_host FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX ix_events_date (event_date),
    INDEX ix_events_status (status),
    INDEX ix_events_host (host_user_id)
);

-- Event registrations table
CREATE TABLE IF NOT EXISTS event_registrations (
    id CHAR(36) NOT NULL,
    event_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    status ENUM('pending', 'confirmed', 'declined', 'cancelled', 'waitlist') NOT NULL DEFAULT 'pending',
    registered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY ux_event_user (event_id, user_id),
    INDEX ix_registrations_user (user_id)
);

-- Event items (things to bring)
CREATE TABLE IF NOT EXISTS event_items (
    id CHAR(36) NOT NULL,
    event_id CHAR(36) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_category ENUM('food', 'drinks', 'supplies', 'other') NOT NULL DEFAULT 'other',
    quantity_needed INT NOT NULL DEFAULT 1,
    claimed_by_user_id CHAR(36) NULL,
    claimed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_items_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_claimed_by FOREIGN KEY (claimed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX ix_items_event (event_id)
);
