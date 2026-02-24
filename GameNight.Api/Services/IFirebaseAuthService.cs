using GameNight.Api.Entities;

namespace GameNight.Api.Services;

public interface IFirebaseAuthService
{
    Task<FirebaseTokenResult> ValidateTokenAsync(string idToken, CancellationToken cancellationToken);
}

public record FirebaseTokenResult(
    bool Success,
    string? FirebaseUid,
    string? Email,
    string? DisplayName,
    string? AvatarUrl,
    string? ErrorMessage
)
{
    public static FirebaseTokenResult Failed(string message) => new(false, null, null, null, null, message);

    public static FirebaseTokenResult Succeeded(string uid, string email, string? displayName, string? avatarUrl)
        => new(true, uid, email, displayName, avatarUrl, null);
}
