import Foundation
import os

actor RoundCounterPersistence {
    static let shared = RoundCounterPersistence()
    private let logger = Logger(subsystem: "com.sasquatsh", category: "RoundCounter")

    private var fileURL: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("round_counter_state.json")
    }

    func save(_ state: RoundCounterState) {
        do {
            let data = try JSONEncoder().encode(state)
            try data.write(to: fileURL, options: .atomic)
        } catch {
            logger.error("Failed to save round counter: \(error.localizedDescription)")
        }
    }

    func load() -> RoundCounterState? {
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return nil }
        do {
            let data = try Data(contentsOf: fileURL)
            return try JSONDecoder().decode(RoundCounterState.self, from: data)
        } catch {
            logger.error("Failed to load round counter: \(error.localizedDescription)")
            return nil
        }
    }

    func clear() {
        try? FileManager.default.removeItem(at: fileURL)
    }
}
