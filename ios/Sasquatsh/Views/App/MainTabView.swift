import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0

    private let diceIcon = MDCIcon.uiImage(MDCIcon.Dice(), evenOdd: true)
    private let groupsIcon = MDCIcon.uiImage(MDCIcon.AccountMultiple())
    private let needPlayersIcon = MDCIcon.uiImage(MDCIcon.AccountSearch())
    private let dashboardIcon = MDCIcon.uiImage(MDCIcon.Dashboard())
    private let profileIcon = MDCIcon.uiImage(MDCIcon.Account())

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab(value: 0) {
                NavigationStack {
                    EventListView()
                }
            } label: {
                Label { Text("Games") } icon: { Image(uiImage: diceIcon) }
            }

            Tab(value: 1) {
                NavigationStack {
                    GroupListView()
                }
            } label: {
                Label { Text("Groups") } icon: { Image(uiImage: groupsIcon) }
            }

            Tab(value: 2) {
                NavigationStack {
                    PlayerRequestListView()
                }
            } label: {
                Label { Text("Need Players") } icon: { Image(uiImage: needPlayersIcon) }
            }

            Tab(value: 3) {
                NavigationStack {
                    DashboardView()
                }
            } label: {
                Label { Text("Dashboard") } icon: { Image(uiImage: dashboardIcon) }
            }

            Tab(value: 4) {
                NavigationStack {
                    ProfileView()
                }
            } label: {
                Label { Text("Profile") } icon: { Image(uiImage: profileIcon) }
            }
        }
        .tint(Color.md3Primary)
    }
}
