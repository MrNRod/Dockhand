# macosApp

Native macOS UI: a **SwiftUI** app linking directly against a Kotlin/Native `.framework` built from `:core`. Real `NavigationSplitView` sidebar, real macOS menu bar and Preferences scene (⌘,), real system materials/typography — none of it is Compose.

## Status

**Built, run, and verified.** Real native window with working sidebar navigation, native `Picker`s (rendering as real `NSPopUpButton`s), and native controls throughout.

## Architecture

```
Swift (this package)  <--calls directly-->  core.framework (Kotlin/Native, macosArm64)
     DockhandMacApp                                Goldleaf, Tinfoil, RcmPayloadBuilder,
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

## Build & Run

### Building & Running via Gradle

```bash
# Build the native .app bundle (macosApp/build/dist/Dockhand.app)
./gradlew :macosApp:assembleApp

# Build and launch the .app bundle
./gradlew :macosApp:run

# Build the .app bundle and packaged DMG installer
./gradlew :macosApp:packageDmg
```

### CLI / Swift Package Manager (Development)

```bash
./gradlew :core:linkDebugFrameworkMacosArm64
cd macosApp
swift build
swift run DockhandMac
```

`Package.swift` links against the single-arch `macosArm64` debug framework directly (via linker/compiler `-F`/`-framework` flags, not an `.xcframework`/`binaryTarget`) — this is Apple-Silicon-only for now and intentionally skips the XCFramework merge step, which requires full Xcode (`xcodebuild -create-xcframework`) rather than just Command Line Tools. See `:core`'s README for the cinterop/Xcode requirement in more detail. It reads `DOCKHAND_FRAMEWORK_DIR` (default `debugFramework`) to pick which build type to link against — `package.sh` sets it to `releaseFramework` for packaged builds.

## Packaging

```bash
# Builds .app bundle and DMG installer in macosApp/build/dist/
./macosApp/package.sh

# Or build the .app bundle only:
./macosApp/package.sh --app-only
```

Builds `:core`'s release framework, builds the Swift executable in release config, assembles a real `Dockhand.app` bundle (generating multi-resolution `AppIcon.icns` from `Resources/AppIcon.png` into `Contents/Resources/`, setting `CFBundleIconFile`/`CFBundleIconName`, embedding `core.framework` and Homebrew's `libusb-1.0.0.dylib` relinked to `@rpath` so the packaged app does not need Homebrew installed), ad-hoc code-signs it, and produces:
- `macosApp/build/dist/Dockhand.app`
- `macosApp/build/dist/Dockhand-<version>-macos-arm64.dmg`

See the root `.gitlab-ci.yml`'s `package-macos` job for how this runs in CI.

## Known gaps

- Only `macosArm64` — no Intel Mac (`macosX64`) support yet, and no universal/XCFramework build for distribution.
- Ad-hoc code-signed only (`codesign --sign -`, via `package.sh`) — not notarized, so Gatekeeper shows an "unidentified developer" warning on first launch (right-click → Open, or `xattr -cr Dockhand.app`, bypasses it). Proper Developer ID signing + notarization would need an Apple Developer Program membership.
- Feature scope is intentionally streamlined relative to `:androidApp`'s screens (e.g. Settings here is a handful of `@AppStorage`-backed toggles, not a full 1:1 port) — the goal was proving the architecture end-to-end, not exhaustive parity.
