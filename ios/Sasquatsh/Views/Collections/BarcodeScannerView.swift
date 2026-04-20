import SwiftUI
import AVFoundation

// MARK: - Camera Scanner (UIKit bridge)

class BarcodeCameraUIView: UIView {
    var onBarcodeScanned: ((String) -> Void)?
    private var session: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var lastScanned: String?
    private var metadataDelegate: MetadataDelegate?

    func startCamera() {
        guard session == nil else {
            // Already set up, just restart if needed
            if let session, !session.isRunning {
                DispatchQueue.global(qos: .userInitiated).async {
                    session.startRunning()
                }
            }
            return
        }

        let session = AVCaptureSession()
        self.session = session

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else { return }

        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        session.addOutput(output)

        let delegate = MetadataDelegate { [weak self] code in
            guard let self, code != self.lastScanned else { return }
            self.lastScanned = code
            AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
            self.onBarcodeScanned?(code)
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
                self?.lastScanned = nil
            }
        }
        self.metadataDelegate = delegate
        output.setMetadataObjectsDelegate(delegate, queue: .main)
        output.metadataObjectTypes = [.ean8, .ean13, .upce]

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = bounds
        layer.insertSublayer(preview, at: 0)
        self.previewLayer = preview

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }
    }

    func stopCamera() {
        session?.stopRunning()
        previewLayer?.removeFromSuperlayer()
        previewLayer = nil
        session = nil
        metadataDelegate = nil
        lastScanned = nil
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    private class MetadataDelegate: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        let handler: (String) -> Void
        init(handler: @escaping (String) -> Void) { self.handler = handler }
        func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
            if let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject, let val = obj.stringValue {
                handler(val)
            }
        }
    }
}

struct BarcodeCameraView: UIViewRepresentable {
    var onBarcodeScanned: (String) -> Void

    func makeUIView(context: Context) -> BarcodeCameraUIView {
        let view = BarcodeCameraUIView()
        view.backgroundColor = .black
        view.onBarcodeScanned = onBarcodeScanned
        view.startCamera()
        return view
    }

    func updateUIView(_ uiView: BarcodeCameraUIView, context: Context) {
        uiView.onBarcodeScanned = onBarcodeScanned
        uiView.startCamera()
    }

    static func dismantleUIView(_ uiView: BarcodeCameraUIView, coordinator: ()) {
        uiView.stopCamera()
    }
}

// MARK: - Barcode Scanner Sheet

struct BarcodeScannerSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    var initialOwnedBggIds: Set<Int> = []
    var onGameFound: (AddCollectionGameInput) -> Void
    @State private var addedBggIds: Set<Int> = []

    private var ownedBggIds: Set<Int> {
        initialOwnedBggIds.union(addedBggIds)
    }

    @State private var scannedCode: String?
    @State private var manualCode = ""
    @State private var isLooking = false
    @State private var result: UpcLookupResult?
    @State private var error: String?
    @State private var useManualEntry = false
    @State private var scanSessionId = UUID()
    @State private var showManualBggSearch = false
    @State private var bggSearchQuery = ""
    @State private var bggSearchResults: [BggSearchResult] = []
    @State private var isSearchingBgg = false

    private var currentUpc: String {
        result?.upc ?? scannedCode ?? manualCode
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let result {
                    let status = result.bggInfoStatus ?? ""
                    let matches = result.bggInfo ?? []

                    if status == "verified" && !matches.isEmpty {
                        // Use Case 1: Verified match
                        verifiedView(match: matches[0], upc: currentUpc)
                    } else if !matches.isEmpty {
                        // Use Case 2: Suggestions — user picks the right one
                        suggestionsView(matches: matches, upc: currentUpc)
                    } else if showManualBggSearch {
                        // Use Case 3: No data — manual BGG search
                        manualBggSearchView
                    } else {
                        // Use Case 3: No data — prompt
                        noDataView
                    }
                } else if isLooking {
                    Spacer()
                    D20ProgressView(size: 40, message: "Looking up barcode...")
                    Spacer()
                } else if useManualEntry {
                    manualEntryView
                } else {
                    // Camera scanner
                    ZStack {
                        BarcodeCameraView { code in
                            scannedCode = code
                            lookupBarcode(code)
                        }
                        .id(scanSessionId)

                        // Overlay with scanning guide
                        VStack {
                            Spacer()

                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.white, lineWidth: 2)
                                .frame(width: 280, height: 140)
                                .background(Color.clear)

                            Spacer()

                            VStack(spacing: 12) {
                                if let code = scannedCode {
                                    Text("Scanned: \(code)")
                                        .font(.md3LabelLarge)
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 6)
                                        .background(Color.black.opacity(0.6))
                                        .clipShape(Capsule())
                                }

                                Text("Point camera at the barcode")
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Capsule())

                                Button {
                                    useManualEntry = true
                                } label: {
                                    Text("Enter code manually")
                                        .font(.md3LabelLarge)
                                        .foregroundStyle(.white.opacity(0.8))
                                }
                            }
                            .padding(.bottom, 40)
                        }
                    }
                    .ignoresSafeArea()
                }

                if let error {
                    VStack(spacing: 8) {
                        Text(error)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Error)
                            .multilineTextAlignment(.center)
                        Button("Try Again") {
                            self.error = nil
                            self.result = nil
                            self.scannedCode = nil
                            self.scanSessionId = UUID()
                        }
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3Primary)
                    }
                    .padding()
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Scan Barcode")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    // MARK: - Manual Entry

    private var manualEntryView: some View {
        VStack(spacing: 20) {
            Spacer()

            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 50))
                .foregroundStyle(Color.md3Primary.opacity(0.4))

            Text("Enter UPC/EAN barcode")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            HStack {
                TextField("e.g. 681706711003", text: $manualCode)
                    .keyboardType(.numberPad)
                    .padding(12)
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))

                Button {
                    lookupBarcode(manualCode)
                } label: {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 18))
                        .frame(width: 44, height: 44)
                        .background(Color.md3Primary)
                        .foregroundStyle(Color.md3OnPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                }
                .disabled(manualCode.count < 8)
            }
            .padding(.horizontal, 24)

            Button {
                useManualEntry = false
                result = nil
                error = nil
            } label: {
                Text("Use camera instead")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3Primary)
            }

            Spacer()
        }
    }

    // MARK: - Scan Another Header

    private var scanAnotherHeader: some View {
        HStack {
            Spacer()
            Button {
                resetToScanner()
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "barcode.viewfinder")
                    Text("Scan Another")
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
        .padding(.vertical, 8)
    }

    // MARK: - Use Case 1: Verified Match

    private func verifiedView(match: UpcBggInfo, upc: String) -> some View {
        VStack(spacing: 0) {
            scanAnotherHeader

            ScrollView {
                VStack(spacing: 16) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 40))
                        .foregroundStyle(Color.md3Primary)

                    Text("Verified Match!")
                        .font(.md3HeadlineSmall)
                        .foregroundStyle(Color.md3OnSurface)

                    gameCard(match: match, upc: upc)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
        }
    }

    // MARK: - Use Case 2: Suggestions

    private func suggestionsView(matches: [UpcBggInfo], upc: String) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text("Which game is this?")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                scanAnotherHeader
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 10) {
                    Text("Select the correct match to help improve results for everyone")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .padding(.horizontal, 16)

                    ForEach(matches) { match in
                        gameCard(match: match, upc: upc)
                    }

                    // None of these? Search manually
                    Button {
                        showManualBggSearch = true
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "magnifyingglass")
                            Text("None of these — search BGG")
                        }
                        .outlinedButtonStyle()
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                }
                .padding(.bottom, 16)
            }
        }
    }

    // MARK: - Use Case 3: No Data

    private var noDataView: some View {
        VStack(spacing: 20) {
            scanAnotherHeader

            Spacer()

            Image(systemName: "questionmark.circle")
                .font(.system(size: 48))
                .foregroundStyle(Color.md3Tertiary)

            Text("No match found")
                .font(.md3HeadlineSmall)
                .foregroundStyle(Color.md3OnSurface)

            Text("This barcode isn't in the database yet.\nSearch BGG to find the game and help others!")
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button {
                showManualBggSearch = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "magnifyingglass")
                    Text("Search BoardGameGeek")
                }
                .primaryButtonStyle()
            }
            .padding(.horizontal, 24)

            Spacer()
        }
    }

    // MARK: - Manual BGG Search (Use Case 3 follow-up)

    private var manualBggSearchView: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Find the game on BGG")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                scanAnotherHeader
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Search bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                TextField("Search by game name...", text: $bggSearchQuery)
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
                VStack(spacing: 8) {
                    ForEach(bggSearchResults) { result in
                        let isOwned = ownedBggIds.contains(result.bggId)
                        HStack(spacing: 12) {
                            if let urlStr = result.thumbnailUrl, let url = URL(string: urlStr) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.md3SurfaceVariant
                                }
                                .frame(width: 50, height: 50)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(result.name)
                                    .font(.md3TitleMedium)
                                    .foregroundStyle(Color.md3OnSurface)
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
                                    selectFromBggSearch(result)
                                } label: {
                                    Text("This one")
                                        .font(.md3LabelLarge)
                                        .padding(.horizontal, 12)
                                        .frame(height: 34)
                                        .background(Color.md3Primary)
                                        .foregroundStyle(Color.md3OnPrimary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                        .padding(10)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
        .onChange(of: bggSearchQuery) { _, value in
            guard !value.isEmpty else {
                bggSearchResults = []
                return
            }
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                guard bggSearchQuery == value else { return }
                searchBgg()
            }
        }
    }

    // MARK: - Reusable Game Card

    private func gameCard(match: UpcBggInfo, upc: String) -> some View {
        let isOwned = ownedBggIds.contains(match.id)

        return HStack(spacing: 12) {
            if let urlStr = match.thumbnailUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(match.name)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)

                if let year = match.published {
                    Text(year)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                if let confidence = match.confidence {
                    HStack(spacing: 4) {
                        Image(systemName: confidence > 0.7 ? "checkmark.seal.fill" : "questionmark.circle")
                            .font(.system(size: 11))
                        Text("\(Int(confidence * 100))% match")
                            .font(.md3BodySmall)
                    }
                    .foregroundStyle(confidence > 0.7 ? Color.md3Primary : Color.md3Tertiary)
                }
            }

            Spacer()

            if isOwned {
                Label("Owned", systemImage: "checkmark.circle.fill")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3Primary)
            } else {
                Button {
                    addToCollection(match: match, upc: upc)
                } label: {
                    Text("Add")
                        .font(.md3LabelLarge)
                        .padding(.horizontal, 16)
                        .frame(height: 36)
                        .background(Color.md3Primary)
                        .foregroundStyle(Color.md3OnPrimary)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(12)
        .background(isOwned ? Color.md3PrimaryContainer.opacity(0.2) : Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .padding(.horizontal, 16)
    }

    // MARK: - Actions

    private func resetToScanner() {
        result = nil
        scannedCode = nil
        error = nil
        useManualEntry = false
        showManualBggSearch = false
        bggSearchQuery = ""
        bggSearchResults = []
        scanSessionId = UUID()
    }

    private func lookupBarcode(_ code: String) {
        let clean = code.replacingOccurrences(of: "[^0-9]", with: "", options: .regularExpression)
        guard clean.count >= 8 else {
            error = "Barcode must be at least 8 digits"
            return
        }

        isLooking = true
        error = nil
        showManualBggSearch = false
        Task {
            do {
                result = try await services.gameUpc.lookupUpc(upc: clean, search: nil)
            } catch {
                self.error = "Lookup failed: \(error.localizedDescription)"
            }
            isLooking = false
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

    private func selectFromBggSearch(_ result: BggSearchResult) {
        // Add to collection
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
        onGameFound(input)
        addedBggIds.insert(result.bggId)

        // POST the match back to GameUPC to contribute to the database
        Task {
            try? await services.gameUpc.voteMatch(upc: currentUpc, bggId: result.bggId)
        }
    }

    private func addToCollection(match: UpcBggInfo, upc: String) {
        let input = AddCollectionGameInput(
            bggId: match.id,
            name: match.name,
            thumbnailUrl: match.thumbnailUrl,
            imageUrl: match.imageUrl,
            minPlayers: nil,
            maxPlayers: nil,
            playingTime: nil,
            yearPublished: match.published.flatMap { Int($0) },
            bggRank: nil,
            averageRating: nil
        )
        onGameFound(input)
        addedBggIds.insert(match.id)

        // Vote to confirm the match
        Task {
            try? await services.gameUpc.voteMatch(upc: upc, bggId: match.id)
        }
    }
}
