namespace GameNight.Api.Entities;

public class EventItem
{
    public Guid Id { get; set; }
    public Guid EventId { get; set; }
    public string ItemName { get; set; } = string.Empty;
    public ItemCategory ItemCategory { get; set; } = ItemCategory.Other;
    public int QuantityNeeded { get; set; } = 1;
    public Guid? ClaimedByUserId { get; set; }
    public DateTime? ClaimedAt { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties (populated by repository)
    public User? ClaimedByUser { get; set; }
}

public enum ItemCategory
{
    Food,
    Drinks,
    Supplies,
    Other
}
