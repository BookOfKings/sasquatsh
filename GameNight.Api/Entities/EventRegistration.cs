namespace GameNight.Api.Entities;

public class EventRegistration
{
    public Guid Id { get; set; }
    public Guid EventId { get; set; }
    public Guid UserId { get; set; }
    public RegistrationStatus Status { get; set; } = RegistrationStatus.Pending;
    public DateTime RegisteredAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties (populated by repository)
    public User? User { get; set; }
    public Event? Event { get; set; }
}

public enum RegistrationStatus
{
    Pending,
    Confirmed,
    Declined,
    Cancelled,
    Waitlist
}
