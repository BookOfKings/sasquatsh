import SwiftUI

struct UserAvatarView: View {
    let url: String?
    let name: String?
    var size: CGFloat = 40
    var userId: String? = nil
    var isAdmin: Bool = false
    var isFoundingMember: Bool = false

    @State private var showProfile = false

    var body: some View {
        let avatar = avatarContent
            .overlay(alignment: .bottomTrailing) {
                if isAdmin {
                    Circle()
                        .fill(Color.md3Error)
                        .frame(width: size * 0.3, height: size * 0.3)
                        .overlay {
                            Image(systemName: "star.fill")
                                .font(.system(size: size * 0.15))
                                .foregroundStyle(.white)
                        }
                } else if isFoundingMember {
                    Circle()
                        .fill(Color.md3Tertiary)
                        .frame(width: size * 0.3, height: size * 0.3)
                        .overlay {
                            Image(systemName: "star.fill")
                                .font(.system(size: size * 0.15))
                                .foregroundStyle(.white)
                        }
                }
            }

        if let userId {
            Button {
                showProfile = true
            } label: {
                avatar
            }
            .sheet(isPresented: $showProfile) {
                UserProfileSheet(userId: userId)
                    .presentationDetents([.medium, .large])
            }
        } else {
            avatar
        }
    }

    private var avatarContent: some View {
        Group {
            if let url, let imageURL = URL(string: url) {
                AsyncImage(url: imageURL) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        placeholderView
                    default:
                        ProgressView()
                            .frame(width: size, height: size)
                    }
                }
                .frame(width: size, height: size)
                .clipShape(Circle())
            } else {
                placeholderView
            }
        }
    }

    private var placeholderView: some View {
        Circle()
            .fill(Color.md3PrimaryContainer)
            .frame(width: size, height: size)
            .overlay {
                Text(initials)
                    .font(.system(size: size * 0.4, weight: .semibold))
                    .foregroundStyle(Color.md3OnPrimaryContainer)
            }
    }

    private var initials: String {
        guard let name, !name.isEmpty else { return "?" }
        let parts = name.split(separator: " ")
        if parts.count >= 2 {
            return String(parts[0].prefix(1) + parts[1].prefix(1)).uppercased()
        }
        return String(name.prefix(2)).uppercased()
    }
}
