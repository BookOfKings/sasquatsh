using System.Security.Claims;
using GameNight.Api.DTOs.Events;
using GameNight.Api.Entities;
using GameNight.Api.Extensions;
using GameNight.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GameNight.Api.Controllers;

[ApiController]
[Route("api/events")]
public class EventsController : ControllerBase
{
    private readonly IEventRepository _eventRepository;
    private readonly ILogger<EventsController> _logger;

    public EventsController(IEventRepository eventRepository, ILogger<EventsController> logger)
    {
        _eventRepository = eventRepository;
        _logger = logger;
    }

    /// <summary>
    /// Get all public, published events with optional filters.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<List<EventSummaryDto>>> GetPublicEvents(
        [FromQuery] string? city,
        [FromQuery] string? state,
        [FromQuery] string? search,
        [FromQuery] string? gameCategory,
        [FromQuery] string? difficulty,
        [FromQuery] string? dateFrom,
        [FromQuery] string? dateTo,
        CancellationToken cancellationToken)
    {
        var filter = new EventSearchFilter
        {
            City = city,
            State = state,
            SearchText = search,
            GameCategory = ParseGameCategory(gameCategory),
            DifficultyLevel = ParseDifficultyLevel(difficulty),
            DateFrom = ParseDate(dateFrom),
            DateTo = ParseDate(dateTo)
        };

        var events = await _eventRepository.GetPublicEventsAsync(filter, cancellationToken);
        return Ok(events.Select(e => e.ToSummaryDto()).ToList());
    }

    private static GameCategory? ParseGameCategory(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        return Enum.TryParse<GameCategory>(value, true, out var cat) ? cat : null;
    }

    private static DifficultyLevel? ParseDifficultyLevel(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        return Enum.TryParse<DifficultyLevel>(value, true, out var level) ? level : null;
    }

    private static DateOnly? ParseDate(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        return DateOnly.TryParse(value, out var date) ? date : null;
    }

    /// <summary>
    /// Get events the current user is registered for.
    /// </summary>
    [HttpGet("my-events")]
    [Authorize]
    public async Task<ActionResult<List<EventSummaryDto>>> GetMyEvents(CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var events = await _eventRepository.GetUserEventsAsync(userId.Value, cancellationToken);
        return Ok(events.Select(e => e.ToSummaryDto()).ToList());
    }

    /// <summary>
    /// Get events hosted by the current user.
    /// </summary>
    [HttpGet("hosted")]
    [Authorize]
    public async Task<ActionResult<List<EventSummaryDto>>> GetHostedEvents(CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var events = await _eventRepository.GetHostedEventsAsync(userId.Value, cancellationToken);
        return Ok(events.Select(e => e.ToSummaryDto()).ToList());
    }

    /// <summary>
    /// Get a single event by ID with full details.
    /// </summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<EventDto>> GetEvent(Guid id, CancellationToken cancellationToken)
    {
        var evt = await _eventRepository.GetEventAsync(id, cancellationToken);
        if (evt is null) return NotFound();

        return Ok(evt.ToDto());
    }

    /// <summary>
    /// Create a new event.
    /// </summary>
    [HttpPost]
    [Authorize]
    public async Task<ActionResult<EventDto>> CreateEvent(
        [FromBody] CreateEventDto dto,
        CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var evt = dto.ToEntity(userId.Value);
        var created = await _eventRepository.CreateEventAsync(evt, cancellationToken);

        _logger.LogInformation("User {UserId} created event {EventId}: {Title}", userId, created.Id, created.Title);

        return CreatedAtAction(nameof(GetEvent), new { id = created.Id }, created.ToDto());
    }

    /// <summary>
    /// Update an existing event (host only).
    /// </summary>
    [HttpPut("{id:guid}")]
    [Authorize]
    public async Task<ActionResult<EventDto>> UpdateEvent(
        Guid id,
        [FromBody] UpdateEventDto dto,
        CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var evt = await _eventRepository.GetEventAsync(id, cancellationToken);
        if (evt is null) return NotFound();

        evt.UpdateFromDto(dto);
        var result = await _eventRepository.UpdateEventAsync(evt, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return result.Code switch
            {
                EventResultCode.NotFound => NotFound(),
                EventResultCode.NotAuthorized => Forbid(),
                _ => BadRequest(result.ErrorMessage)
            };
        }

        _logger.LogInformation("User {UserId} updated event {EventId}", userId, id);
        return Ok(evt.ToDto());
    }

    /// <summary>
    /// Delete an event (host only).
    /// </summary>
    [HttpDelete("{id:guid}")]
    [Authorize]
    public async Task<ActionResult> DeleteEvent(Guid id, CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var result = await _eventRepository.DeleteEventAsync(id, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return result.Code switch
            {
                EventResultCode.NotFound => NotFound(),
                EventResultCode.NotAuthorized => Forbid(),
                _ => BadRequest(result.ErrorMessage)
            };
        }

        _logger.LogInformation("User {UserId} deleted event {EventId}", userId, id);
        return NoContent();
    }

    /// <summary>
    /// Register for an event.
    /// </summary>
    [HttpPost("{id:guid}/register")]
    [Authorize]
    public async Task<ActionResult> RegisterForEvent(Guid id, CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var result = await _eventRepository.RegisterForEventAsync(id, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return result.Code switch
            {
                RegistrationResultCode.EventNotFound => NotFound(),
                RegistrationResultCode.EventFull => BadRequest(new { message = result.ErrorMessage }),
                RegistrationResultCode.AlreadyRegistered => BadRequest(new { message = result.ErrorMessage }),
                _ => BadRequest(new { message = result.ErrorMessage })
            };
        }

        _logger.LogInformation("User {UserId} registered for event {EventId}", userId, id);
        return Ok(new { message = "Successfully registered!" });
    }

    /// <summary>
    /// Cancel registration for an event.
    /// </summary>
    [HttpDelete("{id:guid}/register")]
    [Authorize]
    public async Task<ActionResult> CancelRegistration(Guid id, CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var result = await _eventRepository.CancelRegistrationAsync(id, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return result.Code switch
            {
                RegistrationResultCode.NotRegistered => BadRequest(new { message = result.ErrorMessage }),
                _ => BadRequest(new { message = result.ErrorMessage })
            };
        }

        _logger.LogInformation("User {UserId} cancelled registration for event {EventId}", userId, id);
        return NoContent();
    }

    /// <summary>
    /// Add an item to bring to an event.
    /// </summary>
    [HttpPost("{id:guid}/items")]
    [Authorize]
    public async Task<ActionResult<EventItemDto>> AddItem(
        Guid id,
        [FromBody] CreateEventItemDto dto,
        CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        // Verify event exists
        var evt = await _eventRepository.GetEventAsync(id, cancellationToken);
        if (evt is null) return NotFound();

        var item = dto.ToEntity();
        var created = await _eventRepository.AddItemAsync(id, item, cancellationToken);

        _logger.LogInformation("User {UserId} added item to event {EventId}: {ItemName}", userId, id, item.ItemName);
        return Created($"/api/events/{id}/items/{created.Id}", created.ToDto());
    }

    /// <summary>
    /// Claim an item to bring.
    /// </summary>
    [HttpPost("{eventId:guid}/items/{itemId:guid}/claim")]
    [Authorize]
    public async Task<ActionResult> ClaimItem(Guid eventId, Guid itemId, CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var result = await _eventRepository.ClaimItemAsync(eventId, itemId, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return BadRequest(new { message = result.ErrorMessage });
        }

        _logger.LogInformation("User {UserId} claimed item {ItemId} for event {EventId}", userId, itemId, eventId);
        return Ok(new { message = "Item claimed!" });
    }

    /// <summary>
    /// Unclaim an item.
    /// </summary>
    [HttpDelete("{eventId:guid}/items/{itemId:guid}/claim")]
    [Authorize]
    public async Task<ActionResult> UnclaimItem(Guid eventId, Guid itemId, CancellationToken cancellationToken)
    {
        var userId = GetUserId();
        if (userId is null) return Unauthorized();

        var result = await _eventRepository.UnclaimItemAsync(eventId, itemId, userId.Value, cancellationToken);

        if (!result.Success)
        {
            return BadRequest(new { message = result.ErrorMessage });
        }

        _logger.LogInformation("User {UserId} unclaimed item {ItemId} for event {EventId}", userId, itemId, eventId);
        return NoContent();
    }

    private Guid? GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
        {
            return null;
        }
        return userId;
    }
}
