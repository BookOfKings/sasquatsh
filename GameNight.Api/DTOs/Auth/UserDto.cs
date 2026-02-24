namespace GameNight.Api.DTOs.Auth;

public record UserDto(
    Guid Id,
    string Email,
    string? DisplayName,
    string? AvatarUrl,
    string SubscriptionTier,
    DateTime? SubscriptionExpiresAt,
    DateTime CreatedAt
);
