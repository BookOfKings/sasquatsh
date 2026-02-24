using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Apis.Auth.OAuth2;

namespace GameNight.Api.Services;

public class FirebaseAuthService : IFirebaseAuthService
{
    private readonly ILogger<FirebaseAuthService> _logger;
    private readonly bool _isInitialized;

    public FirebaseAuthService(ILogger<FirebaseAuthService> logger, IWebHostEnvironment env)
    {
        _logger = logger;

        // Initialize Firebase Admin SDK if not already initialized
        if (FirebaseApp.DefaultInstance == null)
        {
            try
            {
                FirebaseApp.Create(new AppOptions
                {
                    Credential = GoogleCredential.GetApplicationDefault()
                });
                _isInitialized = true;
                _logger.LogInformation("Firebase Admin SDK initialized successfully");
            }
            catch (Exception ex)
            {
                _isInitialized = false;
                if (env.IsDevelopment())
                {
                    _logger.LogWarning("Firebase Admin SDK not initialized - running in development mode without Firebase validation. Error: {Message}", ex.Message);
                }
                else
                {
                    throw; // Re-throw in production
                }
            }
        }
        else
        {
            _isInitialized = true;
        }
    }

    public async Task<FirebaseTokenResult> ValidateTokenAsync(string idToken, CancellationToken cancellationToken)
    {
        // If Firebase isn't initialized (dev mode), decode JWT without validation
        if (!_isInitialized)
        {
            return DecodeTokenWithoutValidation(idToken);
        }

        try
        {
            var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(idToken, cancellationToken);

            var uid = decodedToken.Uid;
            var email = decodedToken.Claims.TryGetValue("email", out var emailClaim)
                ? emailClaim?.ToString()
                : null;
            var name = decodedToken.Claims.TryGetValue("name", out var nameClaim)
                ? nameClaim?.ToString()
                : null;
            var picture = decodedToken.Claims.TryGetValue("picture", out var pictureClaim)
                ? pictureClaim?.ToString()
                : null;

            if (string.IsNullOrEmpty(email))
            {
                _logger.LogWarning("Firebase token for uid {Uid} does not contain email claim", uid);
                return FirebaseTokenResult.Failed("Token does not contain email");
            }

            return FirebaseTokenResult.Succeeded(uid, email, name, picture);
        }
        catch (FirebaseAuthException ex)
        {
            _logger.LogWarning(ex, "Firebase token validation failed: {Message}", ex.Message);
            return FirebaseTokenResult.Failed("Invalid or expired token");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error during Firebase token validation");
            return FirebaseTokenResult.Failed("Authentication error");
        }
    }

    /// <summary>
    /// Decodes a Firebase JWT token without signature validation (DEV ONLY).
    /// This allows local development without Firebase Admin credentials.
    /// </summary>
    private FirebaseTokenResult DecodeTokenWithoutValidation(string idToken)
    {
        try
        {
            // JWT is base64url encoded: header.payload.signature
            var parts = idToken.Split('.');
            if (parts.Length != 3)
            {
                return FirebaseTokenResult.Failed("Invalid token format");
            }

            // Decode the payload (second part)
            var payload = parts[1];
            // Add padding if needed for base64
            payload = payload.PadRight(payload.Length + (4 - payload.Length % 4) % 4, '=');
            payload = payload.Replace('-', '+').Replace('_', '/');

            var jsonBytes = Convert.FromBase64String(payload);
            var json = System.Text.Encoding.UTF8.GetString(jsonBytes);
            var claims = System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, System.Text.Json.JsonElement>>(json);

            if (claims == null)
            {
                return FirebaseTokenResult.Failed("Could not parse token claims");
            }

            var uid = claims.TryGetValue("user_id", out var uidElement) ? uidElement.GetString()
                    : claims.TryGetValue("sub", out var subElement) ? subElement.GetString()
                    : null;
            var email = claims.TryGetValue("email", out var emailElement) ? emailElement.GetString() : null;
            var name = claims.TryGetValue("name", out var nameElement) ? nameElement.GetString() : null;
            var picture = claims.TryGetValue("picture", out var pictureElement) ? pictureElement.GetString() : null;

            if (string.IsNullOrEmpty(uid) || string.IsNullOrEmpty(email))
            {
                _logger.LogWarning("Token missing required claims (uid or email)");
                return FirebaseTokenResult.Failed("Token missing required claims");
            }

            _logger.LogDebug("DEV MODE: Decoded token for {Email} without validation", email);
            return FirebaseTokenResult.Succeeded(uid, email, name, picture);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to decode token in dev mode");
            return FirebaseTokenResult.Failed("Invalid token");
        }
    }
}
