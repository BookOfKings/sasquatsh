import SwiftUI
import PhotosUI

struct ShelfScannerSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    var ownedBggIds: Set<Int> = []
    var onGamesAdded: ([AddCollectionGameInput]) -> Void

    @State private var selectedPhoto: PhotosPickerItem?
    @State private var capturedImage: UIImage?
    @State private var isScanning = false
    @State private var scanResult: ShelfScanResult?
    @State private var quota: ShelfScanQuota?
    @State private var error: String?
    @State private var selectedGameIds: Set<String> = []
    @State private var showCamera = false
    @State private var addedBggIds: Set<Int> = []

    // Manual BGG search for unmatched titles
    @State private var searchingTitle: String?
    @State private var bggSearchQuery = ""
    @State private var bggSearchResults: [BggSearchResult] = []
    @State private var isSearchingBgg = false

    private var allOwnedIds: Set<Int> {
        ownedBggIds.union(addedBggIds)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let scanResult {
                    resultsView(scanResult)
                } else if isScanning {
                    Spacer()
                    D20ProgressView(size: 50, message: "Scanning shelf...")
                    Text("Reading game titles with AI vision")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .padding(.top, 8)
                    Spacer()
                } else if let searchingTitle {
                    manualSearchView(for: searchingTitle)
                } else {
                    captureView
                }

                if let error {
                    Text(error)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3Error)
                        .padding()
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Shelf Scanner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .task {
                do {
                    quota = try await services.shelfScan.getRemainingScans()
                } catch {}
            }
            .onChange(of: selectedPhoto) { _, item in
                guard let item else { return }
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let image = UIImage(data: data) {
                        capturedImage = image
                        await scanShelf(image: image)
                    }
                }
            }
            .fullScreenCover(isPresented: $showCamera) {
                CameraCapture { image in
                    capturedImage = image
                    Task { await scanShelf(image: image) }
                }
            }
        }
    }

    // MARK: - Capture View

    private var captureView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "books.vertical.fill")
                .font(.system(size: 50))
                .foregroundStyle(Color.md3Primary.opacity(0.4))

            Text("Scan Your Game Shelf")
                .font(.md3HeadlineSmall)
                .foregroundStyle(Color.md3OnSurface)

            Text("Take a photo of your game shelf and we'll identify the titles using AI vision")
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if let quota {
                HStack(spacing: 4) {
                    Image(systemName: "camera.circle")
                        .font(.system(size: 14))
                    if quota.limit.isUnlimited {
                        Text("Unlimited scans")
                            .font(.md3BodySmall)
                    } else {
                        Text("\(quota.remaining) scan\(quota.remaining == 1 ? "" : "s") remaining this month")
                            .font(.md3BodySmall)
                    }
                }
                .foregroundStyle(quota.limit.isUnlimited || quota.remaining > 0 ? Color.md3OnSurfaceVariant : Color.md3Error)
            }

            Spacer()

            VStack(spacing: 10) {
                Button {
                    showCamera = true
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "camera.fill")
                        Text("Take Photo")
                    }
                    .primaryButtonStyle()
                }

                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    HStack(spacing: 8) {
                        Image(systemName: "photo.on.rectangle")
                        Text("Choose from Library")
                    }
                    .secondaryButtonStyle()
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
    }

    // MARK: - Results View

    private func resultsView(_ result: ShelfScanResult) -> some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Found \(result.totalDetected ?? result.games.count) titles")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                    Text("\(result.matched ?? 0) matched to BGG")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
                Button {
                    scanResult = nil
                    capturedImage = nil
                    selectedPhoto = nil
                    error = nil
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "camera.fill")
                        Text("Scan Again")
                    }
                    .font(.md3LabelLarge)
                    .padding(.horizontal, 14)
                    .frame(height: 32)
                    .background(Color.md3SecondaryContainer)
                    .foregroundStyle(Color.md3OnSecondaryContainer)
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            ScrollView {
                VStack(spacing: 8) {
                    // Matched games
                    ForEach(result.games) { game in
                        if game.bggId != nil {
                            matchedGameRow(game)
                        }
                    }

                    // Unmatched titles
                    let unmatched = result.games.filter { $0.bggId == nil }
                    if !unmatched.isEmpty {
                        Text("Unmatched Titles")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.top, 8)

                        ForEach(unmatched) { game in
                            unmatchedGameRow(game)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }

            // Add all matched button
            let matchedUnowned = result.games.filter { $0.bggId != nil && !allOwnedIds.contains($0.bggId!) }
            if !matchedUnowned.isEmpty {
                Button {
                    addAllMatched(matchedUnowned)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "plus.circle")
                        Text("Add All \(matchedUnowned.count) Matched Games")
                    }
                    .primaryButtonStyle()
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.md3SurfaceContainer)
            }
        }
    }

    private func matchedGameRow(_ game: ShelfScanGame) -> some View {
        let bggId = game.bggId!
        let isOwned = allOwnedIds.contains(bggId)

        return HStack(spacing: 12) {
            if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 50, height: 50)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            }

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(game.name ?? game.detectedTitle)
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)

                    if let conf = game.confidence {
                        Image(systemName: conf == "high" ? "checkmark.seal.fill" : "questionmark.circle")
                            .font(.system(size: 12))
                            .foregroundStyle(conf == "high" ? Color.md3Primary : Color.md3Tertiary)
                    }
                }

                HStack(spacing: 8) {
                    if let year = game.yearPublished {
                        Text(String(year))
                            .font(.md3BodySmall)
                    }
                    if let min = game.minPlayers, let max = game.maxPlayers {
                        Text(min == max ? "\(min)p" : "\(min)-\(max)p")
                            .font(.md3BodySmall)
                    }
                }
                .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            Spacer()

            if isOwned {
                Label("Owned", systemImage: "checkmark.circle.fill")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3Primary)
            } else {
                Button {
                    addSingleGame(game)
                } label: {
                    Text("Add")
                        .font(.md3LabelLarge)
                        .padding(.horizontal, 14)
                        .frame(height: 34)
                        .background(Color.md3Primary)
                        .foregroundStyle(Color.md3OnPrimary)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(10)
        .background(isOwned ? Color.md3PrimaryContainer.opacity(0.2) : Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
    }

    private func unmatchedGameRow(_ game: ShelfScanGame) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "questionmark.circle")
                .font(.system(size: 20))
                .foregroundStyle(Color.md3Tertiary)
                .frame(width: 50)

            Text(game.detectedTitle)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurface)
                .lineLimit(1)

            Spacer()

            Button {
                bggSearchQuery = game.detectedTitle
                searchingTitle = game.detectedTitle
            } label: {
                Text("Search")
                    .font(.md3LabelLarge)
                    .padding(.horizontal, 12)
                    .frame(height: 34)
                    .background(Color.md3SecondaryContainer)
                    .foregroundStyle(Color.md3OnSecondaryContainer)
                    .clipShape(Capsule())
            }
        }
        .padding(10)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
    }

    // MARK: - Manual BGG Search for Unmatched

    private func manualSearchView(for title: String) -> some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    searchingTitle = nil
                    bggSearchResults = []
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.md3Primary)
                }
                Text("Search for: \(title)")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                TextField("Search BGG...", text: $bggSearchQuery)
                    .onSubmit { searchBgg() }
                if isSearchingBgg {
                    ProgressView().scaleEffect(0.8)
                }
            }
            .padding(10)
            .background(Color.md3Surface)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .padding(.horizontal, 16)

            ScrollView {
                VStack(spacing: 6) {
                    ForEach(bggSearchResults) { result in
                        let isOwned = allOwnedIds.contains(result.bggId)
                        HStack(spacing: 12) {
                            if let urlStr = result.thumbnailUrl, let url = URL(string: urlStr) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.md3SurfaceVariant
                                }
                                .frame(width: 44, height: 44)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(result.name)
                                    .font(.md3BodyMedium)
                                    .lineLimit(1)
                                if let year = result.yearPublished {
                                    Text(String(year))
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }

                            Spacer()

                            if isOwned {
                                Label("Owned", systemImage: "checkmark.circle.fill")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3Primary)
                            } else {
                                Button {
                                    addFromSearch(result)
                                } label: {
                                    Text("Add")
                                        .font(.md3LabelLarge)
                                        .padding(.horizontal, 14)
                                        .frame(height: 34)
                                        .background(Color.md3Primary)
                                        .foregroundStyle(Color.md3OnPrimary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                        .padding(8)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
        .onAppear { searchBgg() }
        .onChange(of: bggSearchQuery) { _, value in
            guard !value.isEmpty else { return }
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                guard bggSearchQuery == value else { return }
                searchBgg()
            }
        }
    }

    // MARK: - Actions

    private func scanShelf(image: UIImage) async {
        // Resize to max 1024px and compress to keep under Supabase 2MB body limit
        let resized = resizeImage(image, maxDimension: 1024)
        guard let data = resized.jpegData(compressionQuality: 0.6) else {
            error = "Failed to process image"
            return
        }

        isScanning = true
        error = nil
        do {
            scanResult = try await services.shelfScan.scanImage(imageData: data)
            // Refresh quota
            quota = try? await services.shelfScan.getRemainingScans()
        } catch {
            self.error = error.localizedDescription
        }
        isScanning = false
    }

    private func resizeImage(_ image: UIImage, maxDimension: CGFloat) -> UIImage {
        let size = image.size
        guard max(size.width, size.height) > maxDimension else { return image }
        let scale = maxDimension / max(size.width, size.height)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }

    private func searchBgg() {
        guard !bggSearchQuery.isEmpty else { return }
        isSearchingBgg = true
        Task {
            do {
                bggSearchResults = try await services.bgg.searchGames(query: bggSearchQuery)
            } catch {
                self.error = error.localizedDescription
            }
            isSearchingBgg = false
        }
    }

    private func addSingleGame(_ game: ShelfScanGame) {
        guard let bggId = game.bggId else { return }
        let input = AddCollectionGameInput(
            bggId: bggId,
            name: game.name ?? game.detectedTitle,
            thumbnailUrl: game.thumbnailUrl,
            imageUrl: nil,
            minPlayers: game.minPlayers,
            maxPlayers: game.maxPlayers,
            playingTime: game.playingTime,
            yearPublished: game.yearPublished,
            bggRank: nil,
            averageRating: nil
        )
        onGamesAdded([input])
        addedBggIds.insert(bggId)
    }

    private func addAllMatched(_ games: [ShelfScanGame]) {
        let inputs = games.compactMap { game -> AddCollectionGameInput? in
            guard let bggId = game.bggId else { return nil }
            addedBggIds.insert(bggId)
            return AddCollectionGameInput(
                bggId: bggId,
                name: game.name ?? game.detectedTitle,
                thumbnailUrl: game.thumbnailUrl,
                imageUrl: nil,
                minPlayers: game.minPlayers,
                maxPlayers: game.maxPlayers,
                playingTime: game.playingTime,
                yearPublished: game.yearPublished,
                bggRank: nil,
                averageRating: nil
            )
        }
        onGamesAdded(inputs)
    }

    private func addFromSearch(_ result: BggSearchResult) {
        let input = AddCollectionGameInput(
            bggId: result.bggId,
            name: result.name,
            thumbnailUrl: result.thumbnailUrl,
            imageUrl: nil,
            minPlayers: nil,
            maxPlayers: nil,
            playingTime: nil,
            yearPublished: result.yearPublished,
            bggRank: nil,
            averageRating: nil
        )
        onGamesAdded([input])
        addedBggIds.insert(result.bggId)
        searchingTitle = nil
        bggSearchResults = []
    }
}

// MARK: - Camera Capture

struct CameraCapture: UIViewControllerRepresentable {
    var onCapture: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onCapture: onCapture, dismiss: dismiss)
    }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onCapture: (UIImage) -> Void
        let dismiss: DismissAction

        init(onCapture: @escaping (UIImage) -> Void, dismiss: DismissAction) {
            self.onCapture = onCapture
            self.dismiss = dismiss
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                onCapture(image)
            }
            dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            dismiss()
        }
    }
}
