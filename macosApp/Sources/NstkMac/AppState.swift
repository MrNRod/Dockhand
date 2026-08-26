import Foundation
import core

struct FileEntry: Identifiable {
    let id = UUID()
    let file: MacUnifiedFile

    var name: String { file.name }
    var path: String { file.path }
}

@MainActor
final class AppState: ObservableObject {
    // Upload screen
    @Published var selectedProtocol: String = "Goldleaf"
    @Published var transport: String = "USB"
    @Published var ipAddress: String = "192.168.1.1"
    @Published var files: [FileEntry] = []
    @Published var isUploading = false
    let uploadLog = LogStore()

    // RCM screen
    @Published var selectedPayload: FileEntry?
    @Published var isInjecting = false
    let rcmLog = LogStore()

    // Split & Merge screen
    @Published var isSplitMode = true
    @Published var selectedPaths: [FileEntry] = []
    @Published var outputDir: String = NSHomeDirectory() + "/Downloads"
    @Published var isProcessing = false
    @Published var statusMessage: String = ""

    let usbController = MacosUsbController()
    let fileSplitter = FileSplitterFactory_macosKt.getFileSplitter()

    var isTransportEnabled: Bool { selectedProtocol == "Awoo" }

    func setProtocol(_ protocolName: String) {
        selectedProtocol = protocolName
        if protocolName == "Goldleaf" || protocolName == "Sphaira" {
            transport = "USB"
        }
    }

    func addFiles(_ urls: [URL]) {
        files.append(contentsOf: urls.map { FileEntry(file: MacUnifiedFile(filePath: $0.path)) })
    }

    func removeFile(_ entry: FileEntry) {
        files.removeAll { $0.id == entry.id }
    }

    func startUpload() {
        guard !files.isEmpty, !isUploading else { return }
        isUploading = true
        uploadLog.clear()
        let logPrinter = BridgedLogPrinter(store: uploadLog)
        let fileMap = Dictionary(uniqueKeysWithValues: files.map { ($0.name, $0.file as (any UnifiedFile)) })
        let protocolName = selectedProtocol
        let transportMode = transport
        let switchIp = ipAddress

        Task {
            defer { Task { @MainActor in self.isUploading = false } }

            if transportMode == "NET" {
                let server = NetworkServer()
                server.start(
                    files: Array(fileMap.values),
                    hostIp: "",
                    port: 6042,
                    hostExtra: "",
                    noRequestsServe: false,
                    switchIp: switchIp
                ) { message in
                    Task { @MainActor in self.uploadLog.append(message) }
                }
                return
            }

            let devices = usbController.listDevices()
            guard let device = devices.first, let connection = device.open() else {
                await MainActor.run { self.uploadLog.append("[FAIL] No USB device found or failed to open connection.") }
                return
            }
            defer { connection.close() }

            guard connection.claimInterface(interfaceNumber: connection.activeInterfaceIndex) else {
                await MainActor.run { self.uploadLog.append("[FAIL] Failed to claim USB interface \(connection.activeInterfaceIndex).") }
                return
            }

            do {
                if protocolName == "Goldleaf" {
                    try await Goldleaf(connection: connection, logPrinter: logPrinter).start(files: fileMap)
                } else {
                    try await Tinfoil(connection: connection, logPrinter: logPrinter)
                        .start(files: fileMap, isSphaira: protocolName == "Sphaira")
                }
            } catch {
                await MainActor.run { self.uploadLog.append("[FAIL] Upload error: \(error.localizedDescription)") }
            }
        }
    }

    func injectPayload() {
        guard let payload = selectedPayload, !isInjecting else { return }
        isInjecting = true
        rcmLog.clear()

        Task {
            defer { Task { @MainActor in self.isInjecting = false } }
            do {
                rcmLog.append("Preparing payload...")
                let payloadBytes = try await RcmPayloadBuilder().buildPayload(payloadFile: payload.file)
                guard let device = usbController.findRcmDevice() else {
                    await MainActor.run { self.rcmLog.append("[FAIL] No Switch found in RCM mode.") }
                    return
                }
                rcmLog.append("Found RCM device. Injecting...")
                _ = try await usbController.injectPayload(device: device, payload: payloadBytes as! KotlinByteArray)
                await MainActor.run { self.rcmLog.append("[PASS] Payload injected! The device should boot now.") }
            } catch {
                await MainActor.run { self.rcmLog.append("[FAIL] \(error.localizedDescription)") }
            }
        }
    }

    func startConversion() {
        guard !selectedPaths.isEmpty, !isProcessing else { return }
        isProcessing = true
        statusMessage = isSplitMode ? "Splitting..." : "Merging..."
        let splitting = isSplitMode
        let outDir = outputDir
        let items = selectedPaths

        Task {
            var success = 0
            var failed = 0
            for entry in items {
                do {
                    let ok: Bool
                    if splitting {
                        ok = try await fileSplitter.splitFile(sourceFile: entry.file, outputDir: outDir).boolValue
                    } else {
                        ok = try await fileSplitter.mergeFiles(firstFile: entry.file, outputDir: outDir).boolValue
                    }
                    if ok { success += 1 } else { failed += 1 }
                } catch {
                    failed += 1
                }
            }
            await MainActor.run {
                self.isProcessing = false
                self.statusMessage = failed == 0
                    ? "Success! Processed \(success) files."
                    : "Done. Success: \(success), Failed: \(failed)"
            }
        }
    }
}
