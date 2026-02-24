namespace GameNight.Api.DTOs.Events;

public record EventDto(
    Guid Id,
    Guid HostUserId,
    string Title,
    string? Description,
    string? GameTitle,
    string? GameCategory,
    string EventDate,
    string StartTime,
    int DurationMinutes,
    int SetupMinutes,
    string? AddressLine1,
    string? City,
    string? State,
    string? PostalCode,
    string? LocationDetails,
    string? DifficultyLevel,
    int MaxPlayers,
    int ConfirmedCount,
    bool IsPublic,
    bool IsCharityEvent,
    string Status,
    UserSummaryDto? Host,
    List<RegistrationDto>? Registrations,
    List<EventItemDto>? Items,
    DateTime CreatedAt
);

public record EventSummaryDto(
    Guid Id,
    string Title,
    string? GameTitle,
    string? GameCategory,
    string EventDate,
    string StartTime,
    int DurationMinutes,
    string? City,
    string? State,
    string? DifficultyLevel,
    int MaxPlayers,
    int ConfirmedCount,
    bool IsPublic,
    bool IsCharityEvent,
    string Status,
    UserSummaryDto? Host
);

public record CreateEventDto(
    string Title,
    string? Description,
    string? GameTitle,
    string? GameCategory = null,
    string EventDate = "",
    string StartTime = "",
    int DurationMinutes = 120,
    int SetupMinutes = 15,
    string? AddressLine1 = null,
    string? City = null,
    string? State = null,
    string? PostalCode = null,
    string? LocationDetails = null,
    string? DifficultyLevel = null,
    int MaxPlayers = 4,
    bool IsPublic = true,
    bool IsCharityEvent = false,
    string Status = "draft"
);

public record UpdateEventDto(
    string Title,
    string? Description,
    string? GameTitle,
    string? GameCategory,
    string EventDate,
    string StartTime,
    int DurationMinutes,
    int SetupMinutes,
    string? AddressLine1,
    string? City,
    string? State,
    string? PostalCode,
    string? LocationDetails,
    string? DifficultyLevel,
    int MaxPlayers,
    bool IsPublic,
    bool IsCharityEvent,
    string Status
);

public record RegistrationDto(
    Guid Id,
    Guid UserId,
    string Status,
    UserSummaryDto? User,
    DateTime RegisteredAt
);

public record EventItemDto(
    Guid Id,
    string ItemName,
    string ItemCategory,
    int QuantityNeeded,
    Guid? ClaimedByUserId,
    string? ClaimedByName,
    DateTime? ClaimedAt
);

public record CreateEventItemDto(
    string ItemName,
    string ItemCategory = "other",
    int QuantityNeeded = 1
);

public record UserSummaryDto(
    Guid Id,
    string? DisplayName,
    string? AvatarUrl
);
