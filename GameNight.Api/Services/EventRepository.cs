using System.Data;
using GameNight.Api.Entities;
using MySqlConnector;

namespace GameNight.Api.Services;

public class EventRepository : IEventRepository
{
    private readonly string _connectionString;
    private readonly ILogger<EventRepository> _logger;

    public EventRepository(IConfiguration configuration, ILogger<EventRepository> logger)
    {
        _connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? throw new InvalidOperationException("Connection string 'DefaultConnection' not found.");
        _logger = logger;
    }

    public async Task<IReadOnlyList<Event>> GetPublicEventsAsync(EventSearchFilter? filter, CancellationToken cancellationToken)
    {
        var sql = @"
SELECT e.*, u.id AS host_id, u.display_name AS host_name, u.avatar_url AS host_avatar,
       (SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.id AND er.status = 'confirmed') AS confirmed_count
FROM events e
JOIN users u ON u.id = e.host_user_id
WHERE e.is_public = TRUE AND e.status = 'published' AND e.event_date >= CURDATE()";

        var parameters = new List<MySqlParameter>();

        if (filter != null)
        {
            if (!string.IsNullOrWhiteSpace(filter.City))
            {
                sql += " AND LOWER(e.city) LIKE @City";
                parameters.Add(new("@City", DbType.String) { Value = $"%{filter.City.ToLowerInvariant()}%" });
            }

            if (!string.IsNullOrWhiteSpace(filter.State))
            {
                sql += " AND LOWER(e.state) LIKE @State";
                parameters.Add(new("@State", DbType.String) { Value = $"%{filter.State.ToLowerInvariant()}%" });
            }

            if (!string.IsNullOrWhiteSpace(filter.SearchText))
            {
                sql += " AND (LOWER(e.title) LIKE @Search OR LOWER(e.game_title) LIKE @Search OR LOWER(e.description) LIKE @Search)";
                parameters.Add(new("@Search", DbType.String) { Value = $"%{filter.SearchText.ToLowerInvariant()}%" });
            }

            if (filter.GameCategory.HasValue)
            {
                sql += " AND e.game_category = @GameCategory";
                parameters.Add(new("@GameCategory", DbType.String) { Value = GameCategoryToDbString(filter.GameCategory.Value) });
            }

            if (filter.DifficultyLevel.HasValue)
            {
                sql += " AND e.difficulty_level = @DifficultyLevel";
                parameters.Add(new("@DifficultyLevel", DbType.String) { Value = filter.DifficultyLevel.Value.ToString().ToLowerInvariant() });
            }

            if (filter.DateFrom.HasValue)
            {
                sql += " AND e.event_date >= @DateFrom";
                parameters.Add(new("@DateFrom", DbType.Date) { Value = filter.DateFrom.Value.ToDateTime(TimeOnly.MinValue) });
            }

            if (filter.DateTo.HasValue)
            {
                sql += " AND e.event_date <= @DateTo";
                parameters.Add(new("@DateTo", DbType.Date) { Value = filter.DateTo.Value.ToDateTime(TimeOnly.MinValue) });
            }
        }

        sql += " ORDER BY e.event_date ASC, e.start_time ASC LIMIT 100";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection);
        command.Parameters.AddRange(parameters.ToArray());

        var events = new List<Event>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            events.Add(MapEvent(reader));
        }

        return events;
    }

    private static string GameCategoryToDbString(GameCategory category) => category switch
    {
        GameCategory.DeckBuilding => "deck_building",
        GameCategory.WorkerPlacement => "worker_placement",
        GameCategory.AreaControl => "area_control",
        GameCategory.RolePlaying => "role_playing",
        _ => category.ToString().ToLowerInvariant()
    };

    public async Task<IReadOnlyList<Event>> GetUserEventsAsync(Guid userId, CancellationToken cancellationToken)
    {
        const string sql = @"
SELECT e.*, u.id AS host_id, u.display_name AS host_name, u.avatar_url AS host_avatar,
       (SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.id AND er.status = 'confirmed') AS confirmed_count
FROM events e
JOIN users u ON u.id = e.host_user_id
JOIN event_registrations r ON r.event_id = e.id
WHERE r.user_id = @UserId AND r.status IN ('pending', 'confirmed')
ORDER BY e.event_date ASC, e.start_time ASC;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@UserId", DbType.Guid) { Value = userId } }
        };

        var events = new List<Event>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            events.Add(MapEvent(reader));
        }

        return events;
    }

    public async Task<IReadOnlyList<Event>> GetHostedEventsAsync(Guid hostUserId, CancellationToken cancellationToken)
    {
        const string sql = @"
SELECT e.*, u.id AS host_id, u.display_name AS host_name, u.avatar_url AS host_avatar,
       (SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.id AND er.status = 'confirmed') AS confirmed_count
FROM events e
JOIN users u ON u.id = e.host_user_id
WHERE e.host_user_id = @HostUserId
ORDER BY e.event_date DESC, e.start_time ASC;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@HostUserId", DbType.Guid) { Value = hostUserId } }
        };

        var events = new List<Event>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            events.Add(MapEvent(reader));
        }

        return events;
    }

    public async Task<Event?> GetEventAsync(Guid id, CancellationToken cancellationToken)
    {
        const string eventSql = @"
SELECT e.*, u.id AS host_id, u.display_name AS host_name, u.avatar_url AS host_avatar,
       (SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.id AND er.status = 'confirmed') AS confirmed_count
FROM events e
JOIN users u ON u.id = e.host_user_id
WHERE e.id = @Id;";

        const string registrationsSql = @"
SELECT r.*, u.id AS user_id, u.display_name, u.avatar_url
FROM event_registrations r
JOIN users u ON u.id = r.user_id
WHERE r.event_id = @Id
ORDER BY r.registered_at ASC;";

        const string itemsSql = @"
SELECT i.*, u.display_name AS claimed_by_name
FROM event_items i
LEFT JOIN users u ON u.id = i.claimed_by_user_id
WHERE i.event_id = @Id
ORDER BY i.item_category, i.item_name;";

        await using var connection = await OpenConnectionAsync(cancellationToken);

        // Get event
        Event? evt = null;
        await using (var command = new MySqlCommand(eventSql, connection))
        {
            command.Parameters.Add(new("@Id", DbType.Guid) { Value = id });
            await using var reader = await command.ExecuteReaderAsync(cancellationToken);
            if (await reader.ReadAsync(cancellationToken))
            {
                evt = MapEvent(reader);
            }
        }

        if (evt is null) return null;

        // Get registrations
        await using (var command = new MySqlCommand(registrationsSql, connection))
        {
            command.Parameters.Add(new("@Id", DbType.Guid) { Value = id });
            await using var reader = await command.ExecuteReaderAsync(cancellationToken);
            while (await reader.ReadAsync(cancellationToken))
            {
                evt.Registrations.Add(MapRegistration(reader));
            }
        }

        // Get items
        await using (var command = new MySqlCommand(itemsSql, connection))
        {
            command.Parameters.Add(new("@Id", DbType.Guid) { Value = id });
            await using var reader = await command.ExecuteReaderAsync(cancellationToken);
            while (await reader.ReadAsync(cancellationToken))
            {
                evt.Items.Add(MapItem(reader));
            }
        }

        return evt;
    }

    public async Task<Event> CreateEventAsync(Event evt, CancellationToken cancellationToken)
    {
        const string sql = @"
INSERT INTO events (id, host_user_id, title, description, game_title, game_category, event_date, start_time,
    duration_minutes, setup_minutes, address_line1, city, state, postal_code, location_details,
    difficulty_level, max_players, is_public, is_charity_event, status, created_at, updated_at)
VALUES (@Id, @HostUserId, @Title, @Description, @GameTitle, @GameCategory, @EventDate, @StartTime,
    @DurationMinutes, @SetupMinutes, @AddressLine1, @City, @State, @PostalCode, @LocationDetails,
    @DifficultyLevel, @MaxPlayers, @IsPublic, @IsCharityEvent, @Status, @CreatedAt, @UpdatedAt);";

        evt.Id = evt.Id == Guid.Empty ? Guid.NewGuid() : evt.Id;
        evt.CreatedAt = DateTime.UtcNow;
        evt.UpdatedAt = DateTime.UtcNow;

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@Id", DbType.Guid) { Value = evt.Id },
                new("@HostUserId", DbType.Guid) { Value = evt.HostUserId },
                new("@Title", DbType.String) { Value = evt.Title },
                new("@Description", DbType.String) { Value = (object?)evt.Description ?? DBNull.Value },
                new("@GameTitle", DbType.String) { Value = (object?)evt.GameTitle ?? DBNull.Value },
                new("@GameCategory", DbType.String) { Value = evt.GameCategory.HasValue ? GameCategoryToDbString(evt.GameCategory.Value) : DBNull.Value },
                new("@EventDate", DbType.Date) { Value = evt.EventDate.ToDateTime(TimeOnly.MinValue) },
                new("@StartTime", DbType.Time) { Value = evt.StartTime.ToTimeSpan() },
                new("@DurationMinutes", DbType.Int32) { Value = evt.DurationMinutes },
                new("@SetupMinutes", DbType.Int32) { Value = evt.SetupMinutes },
                new("@AddressLine1", DbType.String) { Value = (object?)evt.AddressLine1 ?? DBNull.Value },
                new("@City", DbType.String) { Value = (object?)evt.City ?? DBNull.Value },
                new("@State", DbType.String) { Value = (object?)evt.State ?? DBNull.Value },
                new("@PostalCode", DbType.String) { Value = (object?)evt.PostalCode ?? DBNull.Value },
                new("@LocationDetails", DbType.String) { Value = (object?)evt.LocationDetails ?? DBNull.Value },
                new("@DifficultyLevel", DbType.String) { Value = evt.DifficultyLevel?.ToString().ToLowerInvariant() ?? (object)DBNull.Value },
                new("@MaxPlayers", DbType.Int32) { Value = evt.MaxPlayers },
                new("@IsPublic", DbType.Boolean) { Value = evt.IsPublic },
                new("@IsCharityEvent", DbType.Boolean) { Value = evt.IsCharityEvent },
                new("@Status", DbType.String) { Value = evt.Status.ToString().ToLowerInvariant() },
                new("@CreatedAt", DbType.DateTime2) { Value = evt.CreatedAt },
                new("@UpdatedAt", DbType.DateTime2) { Value = evt.UpdatedAt },
            }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        return evt;
    }

    public async Task<EventResult> UpdateEventAsync(Event evt, Guid requestingUserId, CancellationToken cancellationToken)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);

        // Check ownership
        var existingEvent = await LoadEventForUpdate(connection, null, evt.Id, cancellationToken);
        if (existingEvent is null) return EventResult.NotFound();
        if (existingEvent.HostUserId != requestingUserId) return EventResult.NotAuthorized();

        const string sql = @"
UPDATE events SET
    title = @Title, description = @Description, game_title = @GameTitle, game_category = @GameCategory,
    event_date = @EventDate, start_time = @StartTime, duration_minutes = @DurationMinutes,
    setup_minutes = @SetupMinutes, address_line1 = @AddressLine1, city = @City,
    state = @State, postal_code = @PostalCode, location_details = @LocationDetails,
    difficulty_level = @DifficultyLevel, max_players = @MaxPlayers, is_public = @IsPublic,
    is_charity_event = @IsCharityEvent, status = @Status, updated_at = @UpdatedAt
WHERE id = @Id;";

        evt.UpdatedAt = DateTime.UtcNow;

        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@Id", DbType.Guid) { Value = evt.Id },
                new("@Title", DbType.String) { Value = evt.Title },
                new("@Description", DbType.String) { Value = (object?)evt.Description ?? DBNull.Value },
                new("@GameTitle", DbType.String) { Value = (object?)evt.GameTitle ?? DBNull.Value },
                new("@GameCategory", DbType.String) { Value = evt.GameCategory.HasValue ? GameCategoryToDbString(evt.GameCategory.Value) : DBNull.Value },
                new("@EventDate", DbType.Date) { Value = evt.EventDate.ToDateTime(TimeOnly.MinValue) },
                new("@StartTime", DbType.Time) { Value = evt.StartTime.ToTimeSpan() },
                new("@DurationMinutes", DbType.Int32) { Value = evt.DurationMinutes },
                new("@SetupMinutes", DbType.Int32) { Value = evt.SetupMinutes },
                new("@AddressLine1", DbType.String) { Value = (object?)evt.AddressLine1 ?? DBNull.Value },
                new("@City", DbType.String) { Value = (object?)evt.City ?? DBNull.Value },
                new("@State", DbType.String) { Value = (object?)evt.State ?? DBNull.Value },
                new("@PostalCode", DbType.String) { Value = (object?)evt.PostalCode ?? DBNull.Value },
                new("@LocationDetails", DbType.String) { Value = (object?)evt.LocationDetails ?? DBNull.Value },
                new("@DifficultyLevel", DbType.String) { Value = evt.DifficultyLevel?.ToString().ToLowerInvariant() ?? (object)DBNull.Value },
                new("@MaxPlayers", DbType.Int32) { Value = evt.MaxPlayers },
                new("@IsPublic", DbType.Boolean) { Value = evt.IsPublic },
                new("@IsCharityEvent", DbType.Boolean) { Value = evt.IsCharityEvent },
                new("@Status", DbType.String) { Value = evt.Status.ToString().ToLowerInvariant() },
                new("@UpdatedAt", DbType.DateTime2) { Value = evt.UpdatedAt },
            }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        return EventResult.SuccessResult();
    }

    public async Task<EventResult> DeleteEventAsync(Guid id, Guid requestingUserId, CancellationToken cancellationToken)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);

        var existingEvent = await LoadEventForUpdate(connection, null, id, cancellationToken);
        if (existingEvent is null) return EventResult.NotFound();
        if (existingEvent.HostUserId != requestingUserId) return EventResult.NotAuthorized();

        const string sql = "DELETE FROM events WHERE id = @Id";
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@Id", DbType.Guid) { Value = id } }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        return EventResult.SuccessResult();
    }

    public async Task<RegistrationResult> RegisterForEventAsync(Guid eventId, Guid userId, CancellationToken cancellationToken)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);

        try
        {
            var evt = await LoadEventForUpdate(connection, transaction, eventId, cancellationToken);
            if (evt is null)
            {
                await transaction.RollbackAsync(cancellationToken);
                return RegistrationResult.EventNotFound();
            }

            // Check if already registered
            var existingReg = await GetExistingRegistration(connection, transaction, eventId, userId, cancellationToken);
            if (existingReg is not null)
            {
                await transaction.RollbackAsync(cancellationToken);
                return RegistrationResult.AlreadyRegistered();
            }

            // Check capacity
            var confirmedCount = await CountConfirmedRegistrations(connection, transaction, eventId, cancellationToken);
            if (confirmedCount >= evt.MaxPlayers)
            {
                await transaction.RollbackAsync(cancellationToken);
                return RegistrationResult.EventFull();
            }

            var registration = new EventRegistration
            {
                Id = Guid.NewGuid(),
                EventId = eventId,
                UserId = userId,
                Status = RegistrationStatus.Confirmed,
                RegisteredAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            const string sql = @"
INSERT INTO event_registrations (id, event_id, user_id, status, registered_at, updated_at)
VALUES (@Id, @EventId, @UserId, @Status, @RegisteredAt, @UpdatedAt);";

            await using var command = new MySqlCommand(sql, connection, (MySqlTransaction)transaction)
            {
                Parameters =
                {
                    new("@Id", DbType.Guid) { Value = registration.Id },
                    new("@EventId", DbType.Guid) { Value = registration.EventId },
                    new("@UserId", DbType.Guid) { Value = registration.UserId },
                    new("@Status", DbType.String) { Value = registration.Status.ToString().ToLowerInvariant() },
                    new("@RegisteredAt", DbType.DateTime2) { Value = registration.RegisteredAt },
                    new("@UpdatedAt", DbType.DateTime2) { Value = registration.UpdatedAt },
                }
            };

            await command.ExecuteNonQueryAsync(cancellationToken);
            await transaction.CommitAsync(cancellationToken);
            return RegistrationResult.SuccessResult(registration);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to register for event {EventId}", eventId);
            try { await transaction.RollbackAsync(cancellationToken); } catch { }
            return RegistrationResult.Failure("Unable to register at this time");
        }
    }

    public async Task<RegistrationResult> CancelRegistrationAsync(Guid eventId, Guid userId, CancellationToken cancellationToken)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);

        const string sql = "DELETE FROM event_registrations WHERE event_id = @EventId AND user_id = @UserId";
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@EventId", DbType.Guid) { Value = eventId },
                new("@UserId", DbType.Guid) { Value = userId },
            }
        };

        var rows = await command.ExecuteNonQueryAsync(cancellationToken);
        if (rows == 0) return RegistrationResult.NotRegistered();

        return RegistrationResult.SuccessResult(new EventRegistration { EventId = eventId, UserId = userId });
    }

    public async Task<EventItem> AddItemAsync(Guid eventId, EventItem item, CancellationToken cancellationToken)
    {
        const string sql = @"
INSERT INTO event_items (id, event_id, item_name, item_category, quantity_needed, created_at)
VALUES (@Id, @EventId, @ItemName, @ItemCategory, @QuantityNeeded, @CreatedAt);";

        item.Id = item.Id == Guid.Empty ? Guid.NewGuid() : item.Id;
        item.EventId = eventId;
        item.CreatedAt = DateTime.UtcNow;

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@Id", DbType.Guid) { Value = item.Id },
                new("@EventId", DbType.Guid) { Value = item.EventId },
                new("@ItemName", DbType.String) { Value = item.ItemName },
                new("@ItemCategory", DbType.String) { Value = item.ItemCategory.ToString().ToLowerInvariant() },
                new("@QuantityNeeded", DbType.Int32) { Value = item.QuantityNeeded },
                new("@CreatedAt", DbType.DateTime2) { Value = item.CreatedAt },
            }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        return item;
    }

    public async Task<EventResult> ClaimItemAsync(Guid eventId, Guid itemId, Guid userId, CancellationToken cancellationToken)
    {
        const string sql = @"
UPDATE event_items
SET claimed_by_user_id = @UserId, claimed_at = @ClaimedAt
WHERE id = @ItemId AND event_id = @EventId AND claimed_by_user_id IS NULL;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@ItemId", DbType.Guid) { Value = itemId },
                new("@EventId", DbType.Guid) { Value = eventId },
                new("@UserId", DbType.Guid) { Value = userId },
                new("@ClaimedAt", DbType.DateTime2) { Value = DateTime.UtcNow },
            }
        };

        var rows = await command.ExecuteNonQueryAsync(cancellationToken);
        if (rows == 0) return EventResult.Failure("Item not found or already claimed");

        return EventResult.SuccessResult();
    }

    public async Task<EventResult> UnclaimItemAsync(Guid eventId, Guid itemId, Guid userId, CancellationToken cancellationToken)
    {
        const string sql = @"
UPDATE event_items
SET claimed_by_user_id = NULL, claimed_at = NULL
WHERE id = @ItemId AND event_id = @EventId AND claimed_by_user_id = @UserId;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@ItemId", DbType.Guid) { Value = itemId },
                new("@EventId", DbType.Guid) { Value = eventId },
                new("@UserId", DbType.Guid) { Value = userId },
            }
        };

        var rows = await command.ExecuteNonQueryAsync(cancellationToken);
        if (rows == 0) return EventResult.Failure("Item not found or not claimed by you");

        return EventResult.SuccessResult();
    }

    private async Task<MySqlConnection> OpenConnectionAsync(CancellationToken cancellationToken)
    {
        var connection = new MySqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);
        return connection;
    }

    private static Event MapEvent(MySqlDataReader reader)
    {
        var evt = new Event
        {
            Id = reader.GetGuid(reader.GetOrdinal("id")),
            HostUserId = reader.GetGuid(reader.GetOrdinal("host_user_id")),
            Title = reader.GetString(reader.GetOrdinal("title")),
            Description = reader.IsDBNull(reader.GetOrdinal("description")) ? null : reader.GetString(reader.GetOrdinal("description")),
            GameTitle = reader.IsDBNull(reader.GetOrdinal("game_title")) ? null : reader.GetString(reader.GetOrdinal("game_title")),
            EventDate = DateOnly.FromDateTime(reader.GetDateTime(reader.GetOrdinal("event_date"))),
            StartTime = TimeOnly.FromTimeSpan(reader.GetTimeSpan(reader.GetOrdinal("start_time"))),
            DurationMinutes = reader.GetInt32(reader.GetOrdinal("duration_minutes")),
            SetupMinutes = reader.GetInt32(reader.GetOrdinal("setup_minutes")),
            AddressLine1 = reader.IsDBNull(reader.GetOrdinal("address_line1")) ? null : reader.GetString(reader.GetOrdinal("address_line1")),
            City = reader.IsDBNull(reader.GetOrdinal("city")) ? null : reader.GetString(reader.GetOrdinal("city")),
            State = reader.IsDBNull(reader.GetOrdinal("state")) ? null : reader.GetString(reader.GetOrdinal("state")),
            PostalCode = reader.IsDBNull(reader.GetOrdinal("postal_code")) ? null : reader.GetString(reader.GetOrdinal("postal_code")),
            LocationDetails = reader.IsDBNull(reader.GetOrdinal("location_details")) ? null : reader.GetString(reader.GetOrdinal("location_details")),
            MaxPlayers = reader.GetInt32(reader.GetOrdinal("max_players")),
            IsPublic = reader.GetBoolean(reader.GetOrdinal("is_public")),
            IsCharityEvent = reader.GetBoolean(reader.GetOrdinal("is_charity_event")),
            CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at")),
            UpdatedAt = reader.GetDateTime(reader.GetOrdinal("updated_at")),
        };

        // Parse enums
        var difficultyStr = reader.IsDBNull(reader.GetOrdinal("difficulty_level")) ? null : reader.GetString(reader.GetOrdinal("difficulty_level"));
        if (!string.IsNullOrEmpty(difficultyStr) && Enum.TryParse<DifficultyLevel>(difficultyStr, true, out var difficulty))
        {
            evt.DifficultyLevel = difficulty;
        }

        var gameCategoryStr = reader.IsDBNull(reader.GetOrdinal("game_category")) ? null : reader.GetString(reader.GetOrdinal("game_category"));
        if (!string.IsNullOrEmpty(gameCategoryStr))
        {
            // Convert snake_case to PascalCase for enum parsing
            var normalized = gameCategoryStr.Replace("_", "");
            if (Enum.TryParse<GameCategory>(normalized, true, out var gameCategory))
            {
                evt.GameCategory = gameCategory;
            }
        }

        var statusStr = reader.GetString(reader.GetOrdinal("status"));
        if (Enum.TryParse<EventStatus>(statusStr, true, out var status))
        {
            evt.Status = status;
        }

        // Map host user if present
        if (HasColumn(reader, "host_name"))
        {
            evt.Host = new User
            {
                Id = reader.GetGuid(reader.GetOrdinal("host_id")),
                DisplayName = reader.IsDBNull(reader.GetOrdinal("host_name")) ? null : reader.GetString(reader.GetOrdinal("host_name")),
                AvatarUrl = reader.IsDBNull(reader.GetOrdinal("host_avatar")) ? null : reader.GetString(reader.GetOrdinal("host_avatar")),
            };
        }

        return evt;
    }

    private static EventRegistration MapRegistration(MySqlDataReader reader)
    {
        var reg = new EventRegistration
        {
            Id = reader.GetGuid(reader.GetOrdinal("id")),
            EventId = reader.GetGuid(reader.GetOrdinal("event_id")),
            UserId = reader.GetGuid(reader.GetOrdinal("user_id")),
            RegisteredAt = reader.GetDateTime(reader.GetOrdinal("registered_at")),
            UpdatedAt = reader.GetDateTime(reader.GetOrdinal("updated_at")),
        };

        var statusStr = reader.GetString(reader.GetOrdinal("status"));
        if (Enum.TryParse<RegistrationStatus>(statusStr, true, out var status))
        {
            reg.Status = status;
        }

        if (HasColumn(reader, "display_name"))
        {
            reg.User = new User
            {
                Id = reader.GetGuid(reader.GetOrdinal("user_id")),
                DisplayName = reader.IsDBNull(reader.GetOrdinal("display_name")) ? null : reader.GetString(reader.GetOrdinal("display_name")),
                AvatarUrl = reader.IsDBNull(reader.GetOrdinal("avatar_url")) ? null : reader.GetString(reader.GetOrdinal("avatar_url")),
            };
        }

        return reg;
    }

    private static EventItem MapItem(MySqlDataReader reader)
    {
        var item = new EventItem
        {
            Id = reader.GetGuid(reader.GetOrdinal("id")),
            EventId = reader.GetGuid(reader.GetOrdinal("event_id")),
            ItemName = reader.GetString(reader.GetOrdinal("item_name")),
            QuantityNeeded = reader.GetInt32(reader.GetOrdinal("quantity_needed")),
            ClaimedByUserId = reader.IsDBNull(reader.GetOrdinal("claimed_by_user_id")) ? null : reader.GetGuid(reader.GetOrdinal("claimed_by_user_id")),
            ClaimedAt = reader.IsDBNull(reader.GetOrdinal("claimed_at")) ? null : reader.GetDateTime(reader.GetOrdinal("claimed_at")),
            CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at")),
        };

        var categoryStr = reader.GetString(reader.GetOrdinal("item_category"));
        if (Enum.TryParse<ItemCategory>(categoryStr, true, out var category))
        {
            item.ItemCategory = category;
        }

        if (HasColumn(reader, "claimed_by_name") && !reader.IsDBNull(reader.GetOrdinal("claimed_by_name")))
        {
            item.ClaimedByUser = new User
            {
                Id = item.ClaimedByUserId ?? Guid.Empty,
                DisplayName = reader.GetString(reader.GetOrdinal("claimed_by_name")),
            };
        }

        return item;
    }

    private static bool HasColumn(MySqlDataReader reader, string columnName)
    {
        for (int i = 0; i < reader.FieldCount; i++)
        {
            if (reader.GetName(i).Equals(columnName, StringComparison.OrdinalIgnoreCase))
                return true;
        }
        return false;
    }

    private static async Task<Event?> LoadEventForUpdate(MySqlConnection connection, MySqlTransaction? transaction, Guid id, CancellationToken cancellationToken)
    {
        var sql = "SELECT * FROM events WHERE id = @Id" + (transaction is not null ? " FOR UPDATE" : "");
        await using var command = transaction is not null
            ? new MySqlCommand(sql, connection, transaction)
            : new MySqlCommand(sql, connection);
        command.Parameters.Add(new("@Id", DbType.Guid) { Value = id });

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (await reader.ReadAsync(cancellationToken))
        {
            return new Event
            {
                Id = reader.GetGuid(reader.GetOrdinal("id")),
                HostUserId = reader.GetGuid(reader.GetOrdinal("host_user_id")),
                MaxPlayers = reader.GetInt32(reader.GetOrdinal("max_players")),
            };
        }
        return null;
    }

    private static async Task<EventRegistration?> GetExistingRegistration(MySqlConnection connection, MySqlTransaction transaction, Guid eventId, Guid userId, CancellationToken cancellationToken)
    {
        const string sql = "SELECT id FROM event_registrations WHERE event_id = @EventId AND user_id = @UserId LIMIT 1";
        await using var command = new MySqlCommand(sql, connection, transaction)
        {
            Parameters =
            {
                new("@EventId", DbType.Guid) { Value = eventId },
                new("@UserId", DbType.Guid) { Value = userId },
            }
        };

        var result = await command.ExecuteScalarAsync(cancellationToken);
        if (result is null) return null;

        return new EventRegistration { Id = (Guid)result, EventId = eventId, UserId = userId };
    }

    private static async Task<int> CountConfirmedRegistrations(MySqlConnection connection, MySqlTransaction transaction, Guid eventId, CancellationToken cancellationToken)
    {
        const string sql = "SELECT COUNT(*) FROM event_registrations WHERE event_id = @EventId AND status = 'confirmed'";
        await using var command = new MySqlCommand(sql, connection, transaction)
        {
            Parameters = { new("@EventId", DbType.Guid) { Value = eventId } }
        };

        var result = await command.ExecuteScalarAsync(cancellationToken);
        return Convert.ToInt32(result, System.Globalization.CultureInfo.InvariantCulture);
    }
}
