import SwiftUI

struct DateAvailabilityGrid: View {
    let dates: [PlanningDate]
    let invitees: [PlanningInvitee]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                // Header row: dates
                HStack(spacing: 0) {
                    Text("")
                        .frame(width: 100, alignment: .leading)

                    ForEach(dates) { date in
                        Text(shortDate(date.proposedDate))
                            .font(.md3LabelSmall)
                            .frame(width: 60)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(.vertical, 8)
                .background(Color.md3PrimaryContainer.opacity(0.5))

                Divider()

                // Invitee rows
                ForEach(invitees) { invitee in
                    HStack(spacing: 0) {
                        Text(invitee.user?.displayName ?? "Player")
                            .font(.md3BodySmall)
                            .lineLimit(1)
                            .frame(width: 100, alignment: .leading)

                        ForEach(dates) { date in
                            let vote = date.votes?.first { $0.userId == invitee.userId }
                            cellView(invitee: invitee, vote: vote)
                                .frame(width: 60, height: 36)
                        }
                    }
                    Divider()
                }

                // Total row
                HStack(spacing: 0) {
                    Text("Available")
                        .font(.md3LabelSmall)
                        .frame(width: 100, alignment: .leading)

                    ForEach(dates) { date in
                        Text("\(date.availableCount ?? countAvailable(date))")
                            .font(.md3LabelSmall)
                            .fontWeight(.bold)
                            .foregroundStyle(Color.md3Primary)
                            .frame(width: 60)
                    }
                }
                .padding(.vertical, 8)
                .background(Color.md3SurfaceVariant.opacity(0.3))
            }
        }
    }

    private func cellView(invitee: PlanningInvitee, vote: DateVote?) -> some View {
        Group {
            if invitee.cannotAttendAny {
                Image(systemName: "xmark")
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3Error)
            } else if !invitee.hasResponded {
                Image(systemName: "questionmark")
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else if let vote {
                Image(systemName: vote.isAvailable ? "checkmark" : "xmark")
                    .font(.md3LabelSmall)
                    .foregroundStyle(vote.isAvailable ? .green : Color.md3Error)
            } else {
                Image(systemName: "minus")
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
    }

    private func shortDate(_ dateStr: String) -> String {
        guard let date = dateStr.toDate else { return dateStr }
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f.string(from: date)
    }

    private func countAvailable(_ date: PlanningDate) -> Int {
        date.votes?.filter { $0.isAvailable }.count ?? 0
    }
}
