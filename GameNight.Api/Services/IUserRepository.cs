using GameNight.Api.Entities;

namespace GameNight.Api.Services;

public interface IUserRepository
{
    Task<User?> GetByIdAsync(Guid id, CancellationToken cancellationToken);
    Task<User?> GetByFirebaseUidAsync(string firebaseUid, CancellationToken cancellationToken);
    Task<User?> GetByEmailAsync(string email, CancellationToken cancellationToken);
    Task<User> CreateAsync(User user, CancellationToken cancellationToken);
    Task<User> UpdateAsync(User user, CancellationToken cancellationToken);
    Task<User> GetOrCreateByFirebaseUidAsync(string firebaseUid, string email, string? displayName, string? avatarUrl, CancellationToken cancellationToken);
}
