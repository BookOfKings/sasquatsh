import SwiftUI
import PhotosUI

struct EditGroupView: View {
    let group: GameGroup
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = CreateEditGroupViewModel()
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var showRemoveLogoConfirm = false

    var body: some View {
        NavigationStack {
            Form {
                // Logo section
                Section("Group Logo") {
                    HStack {
                        Spacer()
                        VStack(spacing: 10) {
                            // Current/preview logo
                            if let url = vm.currentLogoUrl, let imageURL = URL(string: url) {
                                AsyncImage(url: imageURL) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    logoPlaceholder
                                }
                                .frame(width: 80, height: 80)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            } else {
                                logoPlaceholder
                            }

                            if vm.isUploadingLogo {
                                ProgressView("Uploading...")
                                    .controlSize(.small)
                            } else {
                                HStack(spacing: 12) {
                                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                        Text(vm.currentLogoUrl != nil ? "Change" : "Upload")
                                            .font(.md3LabelMedium)
                                            .foregroundStyle(Color.md3Primary)
                                    }

                                    if vm.currentLogoUrl != nil {
                                        Button("Remove") {
                                            showRemoveLogoConfirm = true
                                        }
                                        .font(.md3LabelMedium)
                                        .foregroundStyle(Color.md3Error)
                                    }
                                }
                            }
                        }
                        Spacer()
                    }
                }

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
            .onChange(of: selectedPhoto) { _, newItem in
                guard let newItem else { return }
                Task {
                    // Load as UIImage via transferable Data, with fallback
                    var imageData: Data?
                    if let data = try? await newItem.loadTransferable(type: Data.self),
                       let uiImage = UIImage(data: data) {
                        imageData = compressImage(uiImage, maxSize: 400, quality: 0.7)
                    }
                    if let imageData {
                        await vm.uploadLogo(imageData: imageData)
                    } else {
                        vm.error = "Could not load the selected image"
                    }
                }
            }
            .alert("Remove Logo", isPresented: $showRemoveLogoConfirm) {
                Button("Remove", role: .destructive) {
                    Task { await vm.removeLogo() }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Are you sure you want to remove the group logo?")
            }
            .task {
                vm.configure(services: services)
                vm.loadForEdit(group: group)
            }
        }
    }

    private var logoPlaceholder: some View {
        RoundedRectangle(cornerRadius: 14)
            .fill(Color.md3PrimaryContainer)
            .frame(width: 80, height: 80)
            .overlay {
                Image(systemName: "person.3.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.md3OnPrimaryContainer)
            }
    }

    private func compressImage(_ image: UIImage, maxSize: CGFloat, quality: CGFloat) -> Data {
        let size = image.size
        let scale = min(maxSize / max(size.width, size.height), 1.0)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: newSize)
        let resized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
        return resized.jpegData(compressionQuality: quality) ?? Data()
    }
}
