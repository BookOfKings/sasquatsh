import SwiftUI

struct RaffleEntryBreakdownView: View {
    let entries: [(type: RaffleEntryType, count: Int)]
    let totalEntries: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(entries, id: \.type.id) { entry in
                HStack(spacing: 8) {
                    Image(systemName: entry.type.iconName)
                        .foregroundStyle(Color.md3Primary)
                        .frame(width: 20)
                    Text(entry.type.displayName)
                        .font(.md3BodyMedium)
                    Spacer()
                    Text("\(entry.count)")
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3Primary)
                }
            }

            if !entries.isEmpty {
                Divider()
                HStack {
                    Text("Total Entries")
                        .font(.md3TitleSmall)
                    Spacer()
                    Text("\(totalEntries)")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3Primary)
                }
            }
        }
    }
}
