using System.Security.Claims;
using GameNight.Api.DTOs.Auth;
using GameNight.Api.Extensions;
using GameNight.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GameNight.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IUserRepository _userRepository;
    private readonly ILogger<AuthController> _logger;

    public AuthController(IUserRepository userRepository, ILogger<AuthController> logger)
    {
        _userRepository = userRepository;
        _logger = logger;
    }

    /// <summary>
    /// Get the current authenticated user's profile.
    /// Called after Firebase authentication to sync/create user in our database.
    /// </summary>
    [HttpGet("me")]
    [Authorize]
    public async Task<ActionResult<UserDto>> GetCurrentUser(CancellationToken cancellationToken)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
        {
            return Unauthorized();
        }

        var user = await _userRepository.GetByIdAsync(userId, cancellationToken);
        if (user == null)
        {
            return NotFound();
        }

        return Ok(user.ToDto());
    }

    /// <summary>
    /// Update the current user's profile.
    /// </summary>
    [HttpPut("me")]
    [Authorize]
    public async Task<ActionResult<UserDto>> UpdateCurrentUser(
        [FromBody] UpdateUserDto dto,
        CancellationToken cancellationToken)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
        {
            return Unauthorized();
        }

        var user = await _userRepository.GetByIdAsync(userId, cancellationToken);
        if (user == null)
        {
            return NotFound();
        }

        if (!string.IsNullOrEmpty(dto.DisplayName))
        {
            user.DisplayName = dto.DisplayName.Trim();
        }

        await _userRepository.UpdateAsync(user, cancellationToken);
        _logger.LogInformation("User {UserId} updated their profile", userId);

        return Ok(user.ToDto());
    }
}

public class UpdateUserDto
{
    public string? DisplayName { get; set; }
}
