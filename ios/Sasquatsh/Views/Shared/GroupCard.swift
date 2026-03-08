import SwiftUI

struct GroupCard: View {
    let group: GroupSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                if let logoUrl = group.logoUrl, let url = URL(string: logoUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        groupIcon
                    }
                    .frame(width: 44, height: 44)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                } else {
                    groupIcon
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(group.name)
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)

                    if let location = locationText {
                        Text(location)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }

                Spacer()

                if let role = group.userRole {
                    BadgeView(text: role.displayName, color: .md3PrimaryContainer)
                }
            }

            if let description = group.description, !description.isEmpty {
                Text(description)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .lineLimit(2)
            }

            HStack(spacing: 16) {
                Label {
                    Text("\(group.memberCount) members")
                        .font(.md3BodySmall)
                } icon: {
                    Image(systemName: "person.3")
                        .foregroundStyle(Color.md3Primary)
                }

                BadgeView(text: group.groupType.displayName, color: .md3TertiaryContainer)

                Spacer()

                Image(systemName: policyIcon)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text(group.joinPolicy.displayName)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
    }

    private var groupIcon: some View {
        RoundedRectangle(cornerRadius: MD3Shape.small)
            .fill(Color.md3PrimaryContainer)
            .frame(width: 44, height: 44)
            .overlay {
                Image(systemName: "person.3.fill")
                    .foregroundStyle(Color.md3OnPrimaryContainer)
            }
    }

    private var locationText: String? {
        if let city = group.locationCity, let state = group.locationState {
            return "\(city), \(state)"
        }
        return nil
    }

    private var policyIcon: String {
        switch group.joinPolicy {
        case .open: return "door.left.hand.open"
        case .request: return "hand.raised"
        case .invite_only: return "lock"
        }
    }
}
