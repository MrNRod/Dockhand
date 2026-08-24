import Foundation
import core

@MainActor
final class LogStore: ObservableObject {
    @Published var text: String = ""

    func append(_ line: String) {
        text += line + "\n"
    }

    func clear() {
        text = ""
    }
}

/// Bridges Kotlin's `LogPrinter` interface to a Swift-side observable log buffer.
final class BridgedLogPrinter: NSObject, LogPrinter {
    private let store: LogStore

    init(store: LogStore) {
        self.store = store
    }

    func print(message: String, type: MsgType) {
        let line = "[\(type.name)] \(message)"
        Task { @MainActor in store.append(line) }
    }

    func updateProgress(value: Double) {}

    func update(file: any UnifiedFile, status: FileStatus) {}

    func update(files: [String: any UnifiedFile], status: FileStatus) {}

    func close() {}
}
