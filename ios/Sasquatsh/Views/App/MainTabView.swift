import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0

    var body: some View {
        VStack(spacing: 0) {
            // Content
            Group {
                switch selectedTab {
                case 0:
                    NavigationStack { DashboardView() }
                case 1:
                    NavigationStack { EventListView() }
                case 2:
                    NavigationStack { GroupListView() }
                case 3:
                    NavigationStack { PlayerRequestListView() }
                case 4:
                    NavigationStack { ProfileView() }
                default:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // MD3 Navigation Bar
            MD3TabBar(selectedTab: $selectedTab)
        }
        .ignoresSafeArea(.keyboard)
    }
}

// MARK: - Material Design 3 Tab Bar

private struct MD3TabBar: View {
    @Binding var selectedTab: Int

    private let tabs: [(icon: UIImage, label: String)] = [
        (MDCIcon.uiImage(MDCIcon.Dashboard()), "Dashboard"),
        (MDCIcon.uiImage(MDCIcon.Dice(), evenOdd: true), "Games"),
        (MDCIcon.uiImage(MDCIcon.AccountMultiple()), "Groups"),
        (MDCIcon.uiImage(MDCIcon.AccountSearch()), "Need Players"),
        (MDCIcon.uiImage(MDCIcon.Account()), "Profile"),
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<tabs.count, id: \.self) { index in
                tabItem(index: index)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(Color.md3SurfaceContainer)
        .overlay(alignment: .top) {
            Divider()
        }
    }

    private func tabItem(index: Int) -> some View {
        let isSelected = selectedTab == index
        let tab = tabs[index]

        return Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedTab = index
            }
        } label: {
            VStack(spacing: 4) {
                ZStack {
                    // MD3 active indicator pill
                    if isSelected {
                        Capsule()
                            .fill(Color.md3SecondaryContainer)
                            .frame(width: 56, height: 28)
                    }

                    Image(uiImage: tab.icon)
                        .renderingMode(.template)
                        .frame(width: 24, height: 24)
                }
                .frame(height: 28)

                Text(tab.label)
                    .font(.system(size: 11, weight: isSelected ? .semibold : .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(isSelected ? Color.md3Primary : Color.md3OnSurfaceVariant)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
