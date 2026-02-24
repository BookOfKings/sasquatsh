namespace GameNight.Api.Entities;

public class Event
{
    public Guid Id { get; set; }
    public Guid HostUserId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? GameTitle { get; set; }
    public GameCategory? GameCategory { get; set; }
    public DateOnly EventDate { get; set; }
    public TimeOnly StartTime { get; set; }
    public int DurationMinutes { get; set; } = 120;
    public int SetupMinutes { get; set; } = 15;
    public string? AddressLine1 { get; set; }
    public string? City { get; set; }
    public string? State { get; set; }
    public string? PostalCode { get; set; }
    public string? LocationDetails { get; set; }
    public DifficultyLevel? DifficultyLevel { get; set; }
    public int MaxPlayers { get; set; } = 4;
    public bool IsPublic { get; set; } = true;
    public bool IsCharityEvent { get; set; }
    public EventStatus Status { get; set; } = EventStatus.Draft;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties (populated by repository)
    public User? Host { get; set; }
    public List<EventRegistration> Registrations { get; set; } = new();
    public List<EventItem> Items { get; set; } = new();
}

public enum DifficultyLevel
{
    Beginner,
    Intermediate,
    Advanced
}

public enum EventStatus
{
    Draft,
    Published,
    Cancelled,
    Completed
}

public enum GameCategory
{
    Strategy,
    Party,
    Cooperative,
    DeckBuilding,
    WorkerPlacement,
    AreaControl,
    Dice,
    Trivia,
    RolePlaying,
    Miniatures,
    Card,
    Family,
    Abstract,
    Other
}
