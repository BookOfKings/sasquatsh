-- Migration: Add users table for authentication
-- Date: 2026-02-18

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) NOT NULL,
    firebase_uid VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NULL,
    avatar_url VARCHAR(500) NULL,
    subscription_tier ENUM('free', 'pro', 'premium') NOT NULL DEFAULT 'free',
    subscription_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY ux_users_firebase_uid (firebase_uid),
    UNIQUE KEY ux_users_email (email),
    INDEX ix_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
