using System;
using System.Linq;
using GameNight.Api.DTOs.Auth;
using GameNight.Api.DTOs.Events;
using GameNight.Api.Entities;

namespace GameNight.Api.Extensions;

public static class MappingExtensions
{
    public static UserDto ToDto(this User user) =>
        new(
            user.Id,
            user.Email,
            user.DisplayName,
            user.AvatarUrl,
            user.SubscriptionTier.ToString().ToLowerInvariant(),
            user.SubscriptionExpiresAt,
            user.CreatedAt
        );

    public static EventDto ToDto(this Event evt) =>
        new(
            evt.Id,
            evt.HostUserId,
            evt.Title,
            evt.Description,
            evt.GameTitle,
            evt.GameCategory?.ToString().ToLowerInvariant(),
            evt.EventDate.ToString("yyyy-MM-dd"),
            evt.StartTime.ToString("HH:mm"),
            evt.DurationMinutes,
            evt.SetupMinutes,
            evt.AddressLine1,
            evt.City,
            evt.State,
            evt.PostalCode,
            evt.LocationDetails,
            evt.DifficultyLevel?.ToString().ToLowerInvariant(),
            evt.MaxPlayers,
            evt.Registrations?.Count(r => r.Status == RegistrationStatus.Confirmed) ?? 0,
            evt.IsPublic,
            evt.IsCharityEvent,
            evt.Status.ToString().ToLowerInvariant(),
            evt.Host?.ToSummaryDto(),
            evt.Registrations?.Select(r => r.ToDto()).ToList(),
            evt.Items?.Select(i => i.ToDto()).ToList(),
            evt.CreatedAt
        );

    public static EventSummaryDto ToSummaryDto(this Event evt) =>
        new(
            evt.Id,
            evt.Title,
            evt.GameTitle,
            evt.GameCategory?.ToString().ToLowerInvariant(),
            evt.EventDate.ToString("yyyy-MM-dd"),
            evt.StartTime.ToString("HH:mm"),
            evt.DurationMinutes,
            evt.City,
            evt.State,
            evt.DifficultyLevel?.ToString().ToLowerInvariant(),
            evt.MaxPlayers,
            evt.Registrations?.Count(r => r.Status == RegistrationStatus.Confirmed) ?? 0,
            evt.IsPublic,
            evt.IsCharityEvent,
            evt.Status.ToString().ToLowerInvariant(),
            evt.Host?.ToSummaryDto()
        );

    public static DTOs.Events.RegistrationDto ToDto(this EventRegistration reg) =>
        new(
            reg.Id,
            reg.UserId,
            reg.Status.ToString().ToLowerInvariant(),
            reg.User?.ToSummaryDto(),
            reg.RegisteredAt
        );

    public static EventItemDto ToDto(this EventItem item) =>
        new(
            item.Id,
            item.ItemName,
            item.ItemCategory.ToString().ToLowerInvariant(),
            item.QuantityNeeded,
            item.ClaimedByUserId,
            item.ClaimedByUser?.DisplayName,
            item.ClaimedAt
        );

    public static UserSummaryDto ToSummaryDto(this User user) =>
        new(user.Id, user.DisplayName, user.AvatarUrl);

    public static Event ToEntity(this CreateEventDto dto, Guid hostUserId)
    {
        return new Event
        {
            HostUserId = hostUserId,
            Title = dto.Title.Trim(),
            Description = string.IsNullOrWhiteSpace(dto.Description) ? null : dto.Description.Trim(),
            GameTitle = string.IsNullOrWhiteSpace(dto.GameTitle) ? null : dto.GameTitle.Trim(),
            GameCategory = ParseGameCategory(dto.GameCategory),
            EventDate = DateOnly.Parse(dto.EventDate),
            StartTime = TimeOnly.Parse(dto.StartTime),
            DurationMinutes = dto.DurationMinutes,
            SetupMinutes = dto.SetupMinutes,
            AddressLine1 = string.IsNullOrWhiteSpace(dto.AddressLine1) ? null : dto.AddressLine1.Trim(),
            City = string.IsNullOrWhiteSpace(dto.City) ? null : dto.City.Trim(),
            State = string.IsNullOrWhiteSpace(dto.State) ? null : dto.State.Trim(),
            PostalCode = string.IsNullOrWhiteSpace(dto.PostalCode) ? null : dto.PostalCode.Trim(),
            LocationDetails = string.IsNullOrWhiteSpace(dto.LocationDetails) ? null : dto.LocationDetails.Trim(),
            DifficultyLevel = ParseDifficultyLevel(dto.DifficultyLevel),
            MaxPlayers = dto.MaxPlayers,
            IsPublic = dto.IsPublic,
            IsCharityEvent = dto.IsCharityEvent,
            Status = ParseEventStatus(dto.Status),
        };
    }

    public static void UpdateFromDto(this Event evt, UpdateEventDto dto)
    {
        evt.Title = dto.Title.Trim();
        evt.Description = string.IsNullOrWhiteSpace(dto.Description) ? null : dto.Description.Trim();
        evt.GameTitle = string.IsNullOrWhiteSpace(dto.GameTitle) ? null : dto.GameTitle.Trim();
        evt.GameCategory = ParseGameCategory(dto.GameCategory);
        evt.EventDate = DateOnly.Parse(dto.EventDate);
        evt.StartTime = TimeOnly.Parse(dto.StartTime);
        evt.DurationMinutes = dto.DurationMinutes;
        evt.SetupMinutes = dto.SetupMinutes;
        evt.AddressLine1 = string.IsNullOrWhiteSpace(dto.AddressLine1) ? null : dto.AddressLine1.Trim();
        evt.City = string.IsNullOrWhiteSpace(dto.City) ? null : dto.City.Trim();
        evt.State = string.IsNullOrWhiteSpace(dto.State) ? null : dto.State.Trim();
        evt.PostalCode = string.IsNullOrWhiteSpace(dto.PostalCode) ? null : dto.PostalCode.Trim();
        evt.LocationDetails = string.IsNullOrWhiteSpace(dto.LocationDetails) ? null : dto.LocationDetails.Trim();
        evt.DifficultyLevel = ParseDifficultyLevel(dto.DifficultyLevel);
        evt.MaxPlayers = dto.MaxPlayers;
        evt.IsPublic = dto.IsPublic;
        evt.IsCharityEvent = dto.IsCharityEvent;
        evt.Status = ParseEventStatus(dto.Status);
    }

    public static EventItem ToEntity(this CreateEventItemDto dto) =>
        new()
        {
            ItemName = dto.ItemName.Trim(),
            ItemCategory = ParseItemCategory(dto.ItemCategory),
            QuantityNeeded = dto.QuantityNeeded,
        };

    private static DifficultyLevel? ParseDifficultyLevel(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        return Enum.TryParse<DifficultyLevel>(value, true, out var level) ? level : null;
    }

    private static GameCategory? ParseGameCategory(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        // Handle snake_case from database
        var normalized = value.Replace("_", "");
        return Enum.TryParse<GameCategory>(normalized, true, out var cat) ? cat : null;
    }

    private static EventStatus ParseEventStatus(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return EventStatus.Draft;
        return Enum.TryParse<EventStatus>(value, true, out var status) ? status : EventStatus.Draft;
    }

    private static ItemCategory ParseItemCategory(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return ItemCategory.Other;
        return Enum.TryParse<ItemCategory>(value, true, out var cat) ? cat : ItemCategory.Other;
    }
}
