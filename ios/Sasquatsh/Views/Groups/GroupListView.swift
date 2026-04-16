import SwiftUI

struct GroupListView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = GroupListViewModel()
    @State private var showCreateGroup = false
    @State private var showFilters = false
    @State private var showUpgradePrompt = false

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                SearchBarView(text: $vm.searchText, placeholder: "Search groups...")
                    .padding(.horizontal)
                    .onChange(of: vm.searchText) { _, _ in
                        Task { await vm.loadGroups() }
                    }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Button {
                            showFilters = true
                        } label: {
                            Label("Filters", systemImage: "line.3.horizontal.decrease.circle")
                                .font(.md3LabelMedium)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.md3SurfaceContainerHigh)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                .overlay(
                                    RoundedRectangle(cornerRadius: MD3Shape.small)
                                        .stroke(Color.md3OutlineVariant, lineWidth: 1)
                                )
                        }

                        if vm.selectedType != nil {
                            Button {
                                vm.clearFilters()
                                Task { await vm.loadGroups() }
                            } label: {
                                Label("Clear", systemImage: "xmark")
                                    .font(.md3LabelMedium)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.md3SurfaceContainerHigh)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: MD3Shape.small)
                                            .stroke(Color.md3OutlineVariant, lineWidth: 1)
                                    )
                            }
                        }
                    }
                    .padding(.horizontal)
                }

                if let error = vm.error {
                    ErrorBannerView(message: error) { vm.error = nil }
                }

                if vm.isLoading && vm.groups.isEmpty {
                    LoadingView()
                } else if vm.groups.isEmpty {
                    EmptyStateView(
                        icon: "person.3",
                        title: "No Groups Found",
                        message: "Create a group to start planning game nights together",
                        buttonTitle: "Create Group",
                        action: {
                            let tier = authVM.user?.subscriptionTier ?? .free
                            if TierConfig.canCreateGroup(tier, currentCount: vm.groups.count) {
                                showCreateGroup = true
                            } else {
                                showUpgradePrompt = true
                            }
                        }
                    )
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(vm.groups) { group in
                            NavigationLink(value: group.id) {
                                GroupCard(group: group)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(for: String.self) { groupId in
            GroupDetailView(groupId: groupId)
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    let tier = authVM.user?.subscriptionTier ?? .free
                    if TierConfig.canCreateGroup(tier, currentCount: vm.groups.count) {
                        showCreateGroup = true
                    } else {
                        showUpgradePrompt = true
                    }
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(Color.md3Primary)
                }
            }
        }
        .sheet(isPresented: $showCreateGroup) {
            CreateGroupView()
        }
        .sheet(isPresented: $showUpgradePrompt) {
            UpgradePromptView(
                limitType: .groups,
                currentTier: authVM.user?.subscriptionTier ?? .free
            )
        }
        .sheet(isPresented: $showFilters) {
            NavigationStack {
                Form {
                    Section("Group Type") {
                        Picker("Type", selection: $vm.selectedType) {
                            Text("Any").tag(GroupType?.none)
                            ForEach(GroupType.allCases) { type in
                                Text(type.displayName).tag(GroupType?.some(type))
                            }
                        }
                    }
                }
                .navigationTitle("Filters")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Apply") {
                            showFilters = false
                            Task { await vm.loadGroups() }
                        }
                    }
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showFilters = false }
                    }
                }
            }
            .presentationDetents([.medium])
        }
        .refreshable { await vm.loadGroups() }
        .task {
            vm.configure(services: services)
            await vm.loadGroups()
        }
    }
}
