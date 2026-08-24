# macosApp

Native macOS UI: a **SwiftUI** app (Swift Package, not a Gradle module) linking directly
against a Kotlin/Native `.framework` built from `:core`. Real `NavigationSplitView`
sidebar, real macOS menu bar and Preferences scene (⌘,), real system materials/typography
— none of it is Compose.

## Status

**Built, run, and verified.** Screenshotted a real native window with working sidebar
navigation, native `Picker`s (rendering as real `NSPopUpButton`s), and native controls
throughout.

## Architecture

```
Swift (this package)  <--calls directly-->  core.framework (Kotlin/Native, macosArm64)
     NstkMacApp                                   Goldleaf, Tinfoil, RcmPayloadBuilder,
     ContentView (NavigationSplitView)             MacosUsbController, NetworkServer,
     UploadView / RcmView /                        MacosFileSplitter, MacUnifiedFile
     SplitMergeView / SettingsView
```

- Kotlin/Native auto-generates an Objective-C header for `:core`'s public API, which Swift
  imports directly (`import core`) — no hand-written bridging layer. Class names lose
  their `Core` prefix on the Swift side (e.g. `CoreMacUnifiedFile` in the header becomes
  `MacUnifiedFile` in Swift, per the `__attribute__((swift_name(...)))` annotations
  Kotlin/Native emits).
- `suspend fun`s become Swift `async throws` functions automatically via the
  completion-handler bridging convention; `Result<T>`-returning suspend functions (like
  `RcmPayloadBuilder.buildPayload`) come across as `Any`/`id` on the Swift side and need an
  explicit cast.
- No `FilePicker` implementation exists on the Kotlin side for macOS — this app calls
  `NSOpenPanel` directly and constructs a `MacUnifiedFile` from the chosen path, then calls
  straight into `:core`'s protocol classes.
- `LogStore`/`BridgedLogPrinter` (`LogStore.swift`) bridge Kotlin's `LogPrinter` interface
  to a SwiftUI `ObservableObject` for the log views.

## Build & run

```bash
./gradlew :core:linkDebugFrameworkMacosArm64
cd macosApp
swift build
swift run NstkMac
```

`Package.swift` links against the single-arch `macosArm64` debug framework directly (via
linker/compiler `-F`/`-framework` flags, not an `.xcframework`/`binaryTarget`) — this is
Apple-Silicon-only for now and intentionally skips the XCFramework merge step, which
requires full Xcode (`xcodebuild -create-xcframework`) rather than just Command Line
Tools. See `:core`'s README for the cinterop/Xcode requirement in more detail.

## Known gaps

- Only `macosArm64` — no Intel Mac (`macosX64`) support yet, and no universal/XCFramework
  build for distribution.
- Not code-signed or notarized — fine for local dev, not for distribution.
- Feature scope is intentionally streamlined relative to `:composeApp`'s screens (e.g.
  Settings here is a handful of `@AppStorage`-backed toggles, not a full 1:1 port) — the
  goal was proving the architecture end-to-end, not exhaustive parity.
