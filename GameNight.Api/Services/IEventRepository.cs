using GameNight.Api.Entities;

namespace GameNight.Api.Services;

public interface IEventRepository
{
    Task<IReadOnlyList<Event>> GetPublicEventsAsync(EventSearchFilter? filter, CancellationToken cancellationToken);
    Task<IReadOnlyList<Event>> GetUserEventsAsync(Guid userId, CancellationToken cancellationToken);
    Task<IReadOnlyList<Event>> GetHostedEventsAsync(Guid hostUserId, CancellationToken cancellationToken);
    Task<Event?> GetEventAsync(Guid id, CancellationToken cancellationToken);
    Task<Event> CreateEventAsync(Event evt, CancellationToken cancellationToken);
    Task<EventResult> UpdateEventAsync(Event evt, Guid requestingUserId, CancellationToken cancellationToken);
    Task<EventResult> DeleteEventAsync(Guid id, Guid requestingUserId, CancellationToken cancellationToken);
    Task<RegistrationResult> RegisterForEventAsync(Guid eventId, Guid userId, CancellationToken cancellationToken);
    Task<RegistrationResult> CancelRegistrationAsync(Guid eventId, Guid userId, CancellationToken cancellationToken);
    Task<EventItem> AddItemAsync(Guid eventId, EventItem item, CancellationToken cancellationToken);
    Task<EventResult> ClaimItemAsync(Guid eventId, Guid itemId, Guid userId, CancellationToken cancellationToken);
    Task<EventResult> UnclaimItemAsync(Guid eventId, Guid itemId, Guid userId, CancellationToken cancellationToken);
}

public class EventResult
{
    public bool Success { get; private set; }
    public string? ErrorMessage { get; private set; }
    public EventResultCode Code { get; private set; }

    public static EventResult SuccessResult() => new() { Success = true, Code = EventResultCode.Success };
    public static EventResult NotFound() => new() { Success = false, Code = EventResultCode.NotFound, ErrorMessage = "Event not found" };
    public static EventResult NotAuthorized() => new() { Success = false, Code = EventResultCode.NotAuthorized, ErrorMessage = "You are not authorized to modify this event" };
    public static EventResult Failure(string message) => new() { Success = false, Code = EventResultCode.Failure, ErrorMessage = message };
}

public class RegistrationResult
{
    public bool Success { get; private set; }
    public string? ErrorMessage { get; private set; }
    public RegistrationResultCode Code { get; private set; }
    public EventRegistration? Registration { get; private set; }

    public static RegistrationResult SuccessResult(EventRegistration registration) => new() { Success = true, Code = RegistrationResultCode.Success, Registration = registration };
    public static RegistrationResult EventNotFound() => new() { Success = false, Code = RegistrationResultCode.EventNotFound, ErrorMessage = "Event not found" };
    public static RegistrationResult EventFull() => new() { Success = false, Code = RegistrationResultCode.EventFull, ErrorMessage = "This event is full" };
    public static RegistrationResult AlreadyRegistered() => new() { Success = false, Code = RegistrationResultCode.AlreadyRegistered, ErrorMessage = "You are already registered for this event" };
    public static RegistrationResult NotRegistered() => new() { Success = false, Code = RegistrationResultCode.NotRegistered, ErrorMessage = "You are not registered for this event" };
    public static RegistrationResult Failure(string message) => new() { Success = false, Code = RegistrationResultCode.Failure, ErrorMessage = message };
}

public enum EventResultCode
{
    Success,
    NotFound,
    NotAuthorized,
    Failure
}

public enum RegistrationResultCode
{
    Success,
    EventNotFound,
    EventFull,
    AlreadyRegistered,
    NotRegistered,
    Failure
}

public class EventSearchFilter
{
    public string? City { get; set; }
    public string? State { get; set; }
    public string? SearchText { get; set; }
    public GameCategory? GameCategory { get; set; }
    public DifficultyLevel? DifficultyLevel { get; set; }
    public DateOnly? DateFrom { get; set; }
    public DateOnly? DateTo { get; set; }
}
