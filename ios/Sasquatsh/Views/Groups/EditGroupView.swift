import SwiftUI

struct EditGroupView: View {
    let group: GameGroup
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = CreateEditGroupViewModel()

    var body: some View {
        NavigationStack {
            Form {
                Section("Group Info") {
                    TextField("Group Name", text: $vm.name)
                    TextField("Description", text: $vm.description, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section("Type") {
                    Picker("Group Type", selection: $vm.groupType) {
                        ForEach(GroupType.allCases) { type in
                            Text(type.displayName).tag(type)
                        }
                    }
                }

                Section("Location") {
                    TextField("City", text: $vm.locationCity)
                    USStatePicker(selection: $vm.locationState)
                    HStack {
                        Text("Radius (miles)")
                        Spacer()
                        TextField("25", value: $vm.locationRadiusMiles, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }
                }

                Section("Join Policy") {
                    Picker("Join Policy", selection: $vm.joinPolicy) {
                        ForEach(JoinPolicy.allCases) { policy in
                            Text(policy.displayName).tag(policy)
                        }
                    }
                }

                if let error = vm.error {
                    Section {
                        Text(error).foregroundStyle(Color.md3Error)
                    }
                }
            }
            .navigationTitle("Edit Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            if let _ = await vm.save() {
                                dismiss()
                            }
                        }
                    }
                    .disabled(!vm.isValid || vm.isLoading)
                }
            }
            .task {
                vm.configure(services: services)
                vm.loadForEdit(group: group)
            }
        }
    }
}
