import SwiftUI

struct DateAvailabilityGrid: View {
    let dates: [PlanningDate]
    let invitees: [PlanningInvitee]

    var body: some View {
        GeometryReader { geo in
            let nameWidth: CGFloat = max(90, geo.size.width * 0.28)
            let dateColumnWidth = dates.isEmpty ? 60.0 : max(50, (geo.size.width - nameWidth) / CGFloat(dates.count))

            ScrollView(.horizontal, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    // Header row: dates
                    HStack(spacing: 0) {
                        Text("")
                            .frame(width: nameWidth, alignment: .leading)

                        ForEach(dates) { date in
                            VStack(spacing: 2) {
                                Text(shortDate(date.proposedDate))
                                    .font(.md3LabelSmall)
                                    .fontWeight(.medium)
                                if let time = date.startTime, !time.isEmpty {
                                    Text(time)
                                        .font(.system(size: 9))
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                            .frame(width: dateColumnWidth)
                            .multilineTextAlignment(.center)
                        }
                    }
                    .padding(.vertical, 8)
                    .background(Color.md3PrimaryContainer.opacity(0.5))

                    Divider()

                    // Invitee rows
                    ForEach(invitees) { invitee in
                        HStack(spacing: 0) {
                            HStack(spacing: 6) {
                                UserAvatarView(
                                    url: invitee.user?.avatarUrl,
                                    name: invitee.user?.displayName,
                                    size: 24,
                                    userId: invitee.userId
                                )
                                Text(invitee.user?.displayName ?? "Player")
                                    .font(.md3BodySmall)
                                    .lineLimit(1)
                            }
                            .frame(width: nameWidth, alignment: .leading)

                            ForEach(dates) { date in
                                let vote = date.votes?.first { $0.userId == invitee.userId }
                                cellView(invitee: invitee, vote: vote)
                                    .frame(width: dateColumnWidth, height: 40)
                            }
                        }
                        Divider()
                    }

                    // Total row
                    HStack(spacing: 0) {
                        Text("Available")
                            .font(.md3LabelSmall)
                            .fontWeight(.semibold)
                            .frame(width: nameWidth, alignment: .leading)

                        ForEach(dates) { date in
                            let count = date.availableCount ?? countAvailable(date)
                            let total = invitees.count
                            Text("\(count)/\(total)")
                                .font(.md3LabelMedium)
                                .fontWeight(.bold)
                                .foregroundStyle(count > 0 ? Color.md3Primary : Color.md3OnSurfaceVariant)
                                .frame(width: dateColumnWidth)
                        }
                    }
                    .padding(.vertical, 10)
                    .background(Color.md3SurfaceVariant.opacity(0.3))
                }
                .frame(minWidth: geo.size.width)
            }
        }
        .frame(height: CGFloat(invitees.count + 2) * 44)
    }

    private func cellView(invitee: PlanningInvitee, vote: DateVote?) -> some View {
        Group {
            if invitee.cannotAttendAny ?? false {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.md3Error.opacity(0.7))
            } else if !invitee.hasResponded {
                Image(systemName: "questionmark.circle")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.4))
            } else if let vote {
                Image(systemName: vote.isAvailable ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .font(.system(size: 18))
                    .foregroundStyle(vote.isAvailable ? .green : Color.md3Error.opacity(0.7))
            } else {
                Image(systemName: "minus.circle")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.3))
            }
        }
    }

    private func shortDate(_ dateStr: String) -> String {
        guard let date = dateStr.toDate else { return dateStr }
        let f = DateFormatter()
        f.dateFormat = "EEE\nMMM d"
        return f.string(from: date)
    }

    private func countAvailable(_ date: PlanningDate) -> Int {
        date.votes?.filter { $0.isAvailable }.count ?? 0
    }
}
