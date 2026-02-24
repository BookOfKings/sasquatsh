using Npgsql;

var connectionString = "Host=ep-raspy-wildflower-ain3l9jz-pooler.c-4.us-east-1.aws.neon.tech;Database=neondb;Username=neondb_owner;Password=npg_FlRQPCj5ymL4;SSL Mode=Require";

var migrations = new (string Name, string Sql)[]
{
    ("Create users table", @"
        CREATE TABLE IF NOT EXISTS users (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            firebase_uid VARCHAR(128) NOT NULL,
            email VARCHAR(255) NOT NULL,
            display_name VARCHAR(120) NULL,
            avatar_url VARCHAR(500) NULL,
            subscription_tier VARCHAR(20) NOT NULL DEFAULT 'free',
            subscription_expires_at TIMESTAMP NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT ux_users_firebase_uid UNIQUE (firebase_uid),
            CONSTRAINT ux_users_email UNIQUE (email),
            CONSTRAINT chk_subscription_tier CHECK (subscription_tier IN ('free', 'pro', 'premium'))
        )"),

    ("Create events table", @"
        CREATE TABLE IF NOT EXISTS events (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            host_user_id UUID NOT NULL,
            title VARCHAR(160) NOT NULL,
            description VARCHAR(2000) NULL,
            game_title VARCHAR(160) NULL,
            game_category VARCHAR(50) NULL,
            event_date DATE NOT NULL,
            start_time TIME NOT NULL,
            duration_minutes INT NOT NULL DEFAULT 120,
            setup_minutes INT NOT NULL DEFAULT 15,
            address_line1 VARCHAR(200) NULL,
            city VARCHAR(100) NULL,
            state VARCHAR(50) NULL,
            postal_code VARCHAR(20) NULL,
            location_details VARCHAR(200) NULL,
            difficulty_level VARCHAR(20) NULL,
            max_players INT NOT NULL DEFAULT 4,
            is_public BOOLEAN NOT NULL DEFAULT TRUE,
            is_charity_event BOOLEAN NOT NULL DEFAULT FALSE,
            status VARCHAR(20) NOT NULL DEFAULT 'draft',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_events_host FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT chk_difficulty_level CHECK (difficulty_level IS NULL OR difficulty_level IN ('beginner', 'intermediate', 'advanced')),
            CONSTRAINT chk_status CHECK (status IN ('draft', 'published', 'cancelled', 'completed'))
        )"),

    ("Create events indexes", @"
        CREATE INDEX IF NOT EXISTS ix_events_date ON events (event_date);
        CREATE INDEX IF NOT EXISTS ix_events_city ON events (city);
        CREATE INDEX IF NOT EXISTS ix_events_state ON events (state)"),

    ("Create event_registrations table", @"
        CREATE TABLE IF NOT EXISTS event_registrations (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            event_id UUID NOT NULL,
            user_id UUID NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'pending',
            registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
            CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT ux_event_user UNIQUE (event_id, user_id),
            CONSTRAINT chk_registration_status CHECK (status IN ('pending', 'confirmed', 'declined', 'cancelled', 'waitlist'))
        )"),

    ("Create event_items table", @"
        CREATE TABLE IF NOT EXISTS event_items (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            event_id UUID NOT NULL,
            item_name VARCHAR(100) NOT NULL,
            item_category VARCHAR(20) NOT NULL DEFAULT 'other',
            quantity_needed INT NOT NULL DEFAULT 1,
            claimed_by_user_id UUID NULL,
            claimed_at TIMESTAMP NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_items_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
            CONSTRAINT fk_items_claimed_by FOREIGN KEY (claimed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
            CONSTRAINT chk_item_category CHECK (item_category IN ('food', 'drinks', 'supplies', 'other'))
        )")
};

Console.WriteLine("GameNight Database Migration Runner (PostgreSQL/Neon)");
Console.WriteLine("=====================================================\n");

try
{
    await using var connection = new NpgsqlConnection(connectionString);
    Console.WriteLine("Connecting to Neon PostgreSQL...");
    await connection.OpenAsync();
    Console.WriteLine("Connected!\n");

    foreach (var (name, sql) in migrations)
    {
        try
        {
            Console.Write($"Running: {name}... ");
            await using var cmd = new NpgsqlCommand(sql, connection);
            await cmd.ExecuteNonQueryAsync();
            Console.WriteLine("OK");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"FAILED\n  Error: {ex.Message}");
        }
    }

    Console.WriteLine("\nMigrations complete!");
}
catch (Exception ex)
{
    Console.WriteLine($"Connection failed: {ex.Message}");
}
