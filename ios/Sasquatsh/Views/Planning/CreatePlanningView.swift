import SwiftUI

struct CreatePlanningView: View {
    let groupId: String
    let members: [GroupMember]
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = CreatePlanningViewModel()
    @State private var newDate = Date()

    var body: some View {
        NavigationStack {
            Form {
                Section("Session Info") {
                    TextField("Title", text: $vm.title)
                    TextField("Description", text: $vm.description, axis: .vertical)
                        .lineLimit(2...4)
                    DatePicker("Response Deadline", selection: $vm.responseDeadline, displayedComponents: .date)
                }

                Section("Proposed Dates") {
                    HStack {
                        DatePicker("Add Date", selection: $newDate, displayedComponents: .date)
                        Button {
                            vm.addDate(newDate)
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .foregroundStyle(Color.md3Primary)
                        }
                    }

                    ForEach(vm.proposedDates, id: \.self) { date in
                        HStack {
                            Text(date.displayDate)
                            Spacer()
                            Button {
                                vm.removeDate(date)
                            } label: {
                                Image(systemName: "minus.circle")
                                    .foregroundStyle(Color.md3Error)
                            }
                        }
                    }
                }

                Section("Invite Members") {
                    ForEach(members) { member in
                        HStack {
                            UserAvatarView(url: member.avatarUrl, name: member.displayName, size: 28)
                            Text(member.displayName ?? "Member")
                                .font(.md3BodyMedium)
                            Spacer()
                            if vm.selectedMemberIds.contains(member.userId) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(Color.md3Primary)
                            } else {
                                Image(systemName: "circle")
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            if vm.selectedMemberIds.contains(member.userId) {
                                vm.selectedMemberIds.remove(member.userId)
                            } else {
                                vm.selectedMemberIds.insert(member.userId)
                            }
                        }
                    }

                    Button("Select All") {
                        vm.selectedMemberIds = Set(members.map(\.userId))
                    }
                    .font(.md3LabelSmall)
                }

                if let error = vm.error {
                    Section {
                        Text(error).foregroundStyle(Color.md3Error)
                    }
                }
            }
            .navigationTitle("New Planning Session")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        Task {
                            if let _ = await vm.save(groupId: groupId) {
                                dismiss()
                            }
                        }
                    }
                    .disabled(!vm.isValid || vm.isLoading)
                }
            }
            .task { vm.configure(services: services) }
        }
    }
}
