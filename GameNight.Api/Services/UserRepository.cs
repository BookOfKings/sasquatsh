using System.Data;
using GameNight.Api.Entities;
using MySqlConnector;

namespace GameNight.Api.Services;

public class UserRepository : IUserRepository
{
    private readonly string _connectionString;
    private readonly ILogger<UserRepository> _logger;

    public UserRepository(IConfiguration configuration, ILogger<UserRepository> logger)
    {
        _connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? throw new InvalidOperationException("The MySQL connection string 'DefaultConnection' was not found.");
        _logger = logger;
    }

    public async Task<User?> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        const string sql = @"
SELECT id, firebase_uid, email, display_name, avatar_url, subscription_tier,
       subscription_expires_at, created_at, updated_at
FROM users
WHERE id = @Id;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@Id", DbType.Guid) { Value = id } }
        };

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (await reader.ReadAsync(cancellationToken))
        {
            return MapUser(reader);
        }

        return null;
    }

    public async Task<User?> GetByFirebaseUidAsync(string firebaseUid, CancellationToken cancellationToken)
    {
        const string sql = @"
SELECT id, firebase_uid, email, display_name, avatar_url, subscription_tier,
       subscription_expires_at, created_at, updated_at
FROM users
WHERE firebase_uid = @FirebaseUid;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@FirebaseUid", DbType.String) { Value = firebaseUid } }
        };

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (await reader.ReadAsync(cancellationToken))
        {
            return MapUser(reader);
        }

        return null;
    }

    public async Task<User?> GetByEmailAsync(string email, CancellationToken cancellationToken)
    {
        const string sql = @"
SELECT id, firebase_uid, email, display_name, avatar_url, subscription_tier,
       subscription_expires_at, created_at, updated_at
FROM users
WHERE email = @Email;";

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters = { new("@Email", DbType.String) { Value = email.ToLowerInvariant() } }
        };

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (await reader.ReadAsync(cancellationToken))
        {
            return MapUser(reader);
        }

        return null;
    }

    public async Task<User> CreateAsync(User user, CancellationToken cancellationToken)
    {
        const string sql = @"
INSERT INTO users (id, firebase_uid, email, display_name, avatar_url, subscription_tier, created_at, updated_at)
VALUES (@Id, @FirebaseUid, @Email, @DisplayName, @AvatarUrl, @SubscriptionTier, @CreatedAt, @UpdatedAt);";

        user.Id = user.Id == Guid.Empty ? Guid.NewGuid() : user.Id;
        user.CreatedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@Id", DbType.Guid) { Value = user.Id },
                new("@FirebaseUid", DbType.String) { Value = user.FirebaseUid },
                new("@Email", DbType.String) { Value = user.Email.ToLowerInvariant() },
                new("@DisplayName", DbType.String) { Value = (object?)user.DisplayName ?? DBNull.Value },
                new("@AvatarUrl", DbType.String) { Value = (object?)user.AvatarUrl ?? DBNull.Value },
                new("@SubscriptionTier", DbType.String) { Value = user.SubscriptionTier.ToString().ToLowerInvariant() },
                new("@CreatedAt", DbType.DateTime2) { Value = user.CreatedAt },
                new("@UpdatedAt", DbType.DateTime2) { Value = user.UpdatedAt },
            }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        _logger.LogInformation("Created user {UserId} with email {Email}", user.Id, user.Email);
        return user;
    }

    public async Task<User> UpdateAsync(User user, CancellationToken cancellationToken)
    {
        const string sql = @"
UPDATE users
SET display_name = @DisplayName,
    avatar_url = @AvatarUrl,
    subscription_tier = @SubscriptionTier,
    subscription_expires_at = @SubscriptionExpiresAt,
    updated_at = @UpdatedAt
WHERE id = @Id;";

        user.UpdatedAt = DateTime.UtcNow;

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = new MySqlCommand(sql, connection)
        {
            Parameters =
            {
                new("@Id", DbType.Guid) { Value = user.Id },
                new("@DisplayName", DbType.String) { Value = (object?)user.DisplayName ?? DBNull.Value },
                new("@AvatarUrl", DbType.String) { Value = (object?)user.AvatarUrl ?? DBNull.Value },
                new("@SubscriptionTier", DbType.String) { Value = user.SubscriptionTier.ToString().ToLowerInvariant() },
                new("@SubscriptionExpiresAt", DbType.DateTime2) { Value = (object?)user.SubscriptionExpiresAt ?? DBNull.Value },
                new("@UpdatedAt", DbType.DateTime2) { Value = user.UpdatedAt },
            }
        };

        await command.ExecuteNonQueryAsync(cancellationToken);
        return user;
    }

    public async Task<User> GetOrCreateByFirebaseUidAsync(
        string firebaseUid,
        string email,
        string? displayName,
        string? avatarUrl,
        CancellationToken cancellationToken)
    {
        var existingUser = await GetByFirebaseUidAsync(firebaseUid, cancellationToken);
        if (existingUser != null)
        {
            // Update display name and avatar if they changed
            if (existingUser.DisplayName != displayName || existingUser.AvatarUrl != avatarUrl)
            {
                existingUser.DisplayName = displayName;
                existingUser.AvatarUrl = avatarUrl;
                await UpdateAsync(existingUser, cancellationToken);
            }
            return existingUser;
        }

        var newUser = new User
        {
            FirebaseUid = firebaseUid,
            Email = email,
            DisplayName = displayName,
            AvatarUrl = avatarUrl,
            SubscriptionTier = SubscriptionTier.Free
        };

        return await CreateAsync(newUser, cancellationToken);
    }

    private async Task<MySqlConnection> OpenConnectionAsync(CancellationToken cancellationToken)
    {
        var connection = new MySqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);
        return connection;
    }

    private static User MapUser(MySqlDataReader reader)
    {
        var tierString = reader.GetString(reader.GetOrdinal("subscription_tier"));
        var tier = tierString switch
        {
            "pro" => SubscriptionTier.Pro,
            "premium" => SubscriptionTier.Premium,
            _ => SubscriptionTier.Free
        };

        return new User
        {
            Id = reader.GetGuid(reader.GetOrdinal("id")),
            FirebaseUid = reader.GetString(reader.GetOrdinal("firebase_uid")),
            Email = reader.GetString(reader.GetOrdinal("email")),
            DisplayName = reader.IsDBNull(reader.GetOrdinal("display_name"))
                ? null
                : reader.GetString(reader.GetOrdinal("display_name")),
            AvatarUrl = reader.IsDBNull(reader.GetOrdinal("avatar_url"))
                ? null
                : reader.GetString(reader.GetOrdinal("avatar_url")),
            SubscriptionTier = tier,
            SubscriptionExpiresAt = reader.IsDBNull(reader.GetOrdinal("subscription_expires_at"))
                ? null
                : reader.GetDateTime(reader.GetOrdinal("subscription_expires_at")),
            CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at")),
            UpdatedAt = reader.GetDateTime(reader.GetOrdinal("updated_at")),
        };
    }
}
