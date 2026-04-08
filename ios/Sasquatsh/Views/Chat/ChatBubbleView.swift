import SwiftUI

struct ChatBubbleView: View {
    let message: ChatMessage
    let isOwnMessage: Bool
    var onDelete: (() -> Void)?
    var onReport: (() -> Void)?

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if !isOwnMessage {
                UserAvatarView(
                    url: message.user?.avatarUrl,
                    name: message.user?.displayName,
                    size: 28
                )
            }

            VStack(alignment: isOwnMessage ? .trailing : .leading, spacing: 2) {
                if !isOwnMessage {
                    Text(message.user?.displayName ?? "Unknown")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                Text(message.content)
                    .font(.md3BodyMedium)
                    .foregroundStyle(isOwnMessage ? Color.md3OnPrimaryContainer : Color.md3OnSurface)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isOwnMessage ? Color.md3PrimaryContainer : Color.md3SurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    .contextMenu {
                        if isOwnMessage {
                            Button(role: .destructive) {
                                onDelete?()
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        } else {
                            Button {
                                onReport?()
                            } label: {
                                Label("Report", systemImage: "exclamationmark.triangle")
                            }
                        }
                    }

                Text(formattedTime)
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            if isOwnMessage {
                Spacer(minLength: 40)
            }
        }
        .frame(maxWidth: .infinity, alignment: isOwnMessage ? .trailing : .leading)
    }

    private var formattedTime: String {
        guard let date = message.createdAt.toDate else { return "" }
        let calendar = Calendar.current
        let formatter = DateFormatter()

        if calendar.isDateInToday(date) {
            formatter.dateFormat = "h:mm a"
        } else if calendar.isDateInYesterday(date) {
            formatter.dateFormat = "'Yesterday' h:mm a"
        } else {
            formatter.dateFormat = "MMM d, h:mm a"
        }

        return formatter.string(from: date)
    }
}
