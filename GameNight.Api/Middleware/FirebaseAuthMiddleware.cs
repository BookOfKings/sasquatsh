using System.Security.Claims;
using GameNight.Api.Services;

namespace GameNight.Api.Middleware;

public class FirebaseAuthMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<FirebaseAuthMiddleware> _logger;

    public FirebaseAuthMiddleware(RequestDelegate next, ILogger<FirebaseAuthMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(
        HttpContext context,
        IFirebaseAuthService firebaseAuth,
        IUserRepository userRepository)
    {
        var authHeader = context.Request.Headers.Authorization.FirstOrDefault();

        if (!string.IsNullOrEmpty(authHeader) && authHeader.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
        {
            var token = authHeader["Bearer ".Length..].Trim();

            if (!string.IsNullOrEmpty(token))
            {
                var result = await firebaseAuth.ValidateTokenAsync(token, context.RequestAborted);

                if (result.Success && !string.IsNullOrEmpty(result.FirebaseUid) && !string.IsNullOrEmpty(result.Email))
                {
                    // Get or create user in database
                    var user = await userRepository.GetOrCreateByFirebaseUidAsync(
                        result.FirebaseUid,
                        result.Email,
                        result.DisplayName,
                        result.AvatarUrl,
                        context.RequestAborted);

                    // Build claims identity
                    var claims = new List<Claim>
                    {
                        new(ClaimTypes.NameIdentifier, user.Id.ToString()),
                        new(ClaimTypes.Email, user.Email),
                        new("firebase_uid", user.FirebaseUid),
                        new("subscription_tier", user.SubscriptionTier.ToString().ToLowerInvariant())
                    };

                    if (!string.IsNullOrEmpty(user.DisplayName))
                    {
                        claims.Add(new Claim(ClaimTypes.Name, user.DisplayName));
                    }

                    context.User = new ClaimsPrincipal(new ClaimsIdentity(claims, "Firebase"));
                    _logger.LogDebug("Authenticated user {UserId} via Firebase", user.Id);
                }
            }
        }

        await _next(context);
    }
}

public static class FirebaseAuthMiddlewareExtensions
{
    public static IApplicationBuilder UseFirebaseAuth(this IApplicationBuilder builder)
    {
        return builder.UseMiddleware<FirebaseAuthMiddleware>();
    }
}
