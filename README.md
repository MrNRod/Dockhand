# Dockhand

Unified multiplatform frontend and payload delivery tool for Nintendo Switch.

The core logic (USB protocol, Goldleaf/Tinfoil clients, file splitting, RCM payload injection) lives in a single shared Kotlin Multiplatform library (`:core`).

Each supported desktop operating system gets a **first-class platform-native UI** — SwiftUI on macOS, GTK4 on Linux, WinUI 3 on Windows — rather than one Compose UI stretched across environments where it doesn't quite fit. Android uses Compose Multiplatform.

## Modules

```
dockhand/
├── core/                Shared KMP library (pure business logic, zero UI dependencies
│                        at all. Targets: Android, JVM (desktop), macosArm64 (Kotlin/Native).
│                        → core/README.md
├── composeApp/          Shared Compose Multiplatform UI components (currently used by
│                        androidApp).
│                        → composeApp/README.md
├── androidApp/          Android application module (Kotlin + Jetpack/Compose Multiplatform).
│                        → androidApp/README.md
├── macosApp/            Native SwiftUI app (Swift Package + Gradle wrapper) linking directly
│                        against core.framework (Kotlin/Native). Real NavigationSplitView
│                        sidebar, real macOS menu bar, real system materials — no Compose involved.
│                        → macosApp/README.md
├── linuxApp/            Native Linux app (GTK4 via java-gi FFM bindings, Kotlin/JVM)
│                        linking against :core. Real AdwHeaderBar, native GNOME file
│                        dialogs, system theme sync — no Compose involved.
│                        → linuxApp/README.md
└── windowsApp/          Native Windows app: WinUI 3 frontend (C# / Windows App SDK) +
                         thin JVM backend subprocess linking against :core over JSON-RPC.
                         Mica material, Windows 11 controls, NavigationView sidebar —
                         no Compose involved.
                         → windowsApp/README.md
```

## Status

All four frontends are implemented and wired to `:core`:
- **androidApp**: working, running on Android devices.
- **macosApp**: built, run, and verified — a real native window with working navigation.
- **linuxApp**: built and running with native GTK4 UI.
- **windowsApp**: WinUI 3 frontend running against the JVM backend subprocess.

There is no Compose Desktop fallback anymore — each of macOS/Linux/Windows now has a dedicated native UI layer consuming `:core` via the most idiomatic mechanism for that platform.

## Getting Started

### Prerequisites
-   **JDK 25** (recommended: Eclipse Temurin 25 or OpenJDK 25 via SDKMAN/Homebrew). Gradle toolchains will attempt to auto-provision JDK 25 if `org.gradle.java.installations.auto-download=true` is set.
-   **Android SDK** (for `:androidApp` and `:composeApp`).
-   **Xcode** (for `macosApp` — cinterop against custom C libraries like libusb needs the macOS SDK headers Xcode provides, not just Command Line Tools).
-   **GTK4 runtime** (for `linuxApp` — `apt install libgtk-4-1` on Debian/Ubuntu, `pacman -S gtk4` on Arch, `brew install gtk4` on macOS for local smoke tests).
-   **.NET 8 SDK + Windows App SDK** (for `windowsApp` — building the WinUI 3 frontend requires Windows).

### Build & Run Commands

**Android**:
```bash
./gradlew :androidApp:installDebug
```

**macOS (native SwiftUI)**:
```bash
# Build & run native .app bundle
./gradlew :macosApp:run

# Or run via Swift Package Manager during development:
./gradlew :core:linkDebugFrameworkMacosArm64
cd macosApp && swift run DockhandMac
```

**Linux (native GTK4)**:
```bash
./gradlew :linuxApp:run
```

**Windows (native WinUI)** — see `windowsApp/README.md` for the full two-part build (JVM backend + C# frontend).

### Generating Installers

**Linux** — a real `.deb`/`.rpm` via `jpackage` (bundled with the JDK, no extra plugin):
```bash
./gradlew :linuxApp:jpackageDeb
./gradlew :linuxApp:jpackageRpm
```
Must run on the target OS — `jpackageDeb` needs `dpkg-deb` (present by default on Debian/Ubuntu), `jpackageRpm` needs `rpmbuild` (`apt install rpm` on Debian/Ubuntu).

**Windows** — a real `.msi` via WiX Toolset, with a bundled minimal JRE (`jlink`) so end users don't need Java preinstalled:
```powershell
dotnet tool restore
./gradlew.bat :windowsApp:backend:fatJar :windowsApp:backend:jlinkRuntime
# copy backend/build/libs and backend/build/runtime into DockhandWindowsApp/backend and
# DockhandWindowsApp/runtime — see windowsApp/README.md for the full sequence
dotnet publish windowsApp/DockhandWindowsApp/DockhandWindowsApp.csproj -c Release -p:Platform=x64 -r win-x64 --self-contained true -o windowsApp/DockhandWindowsApp/publish
dotnet wix build windowsApp/packaging/windows/Product.wxs -d ProductVersion=1.0.0 -d PublishDir=windowsApp/DockhandWindowsApp/publish -arch x64 -out dockhand-windows.msi
```

**macOS** — a real `.app` bundle (`Dockhand.app`) + `.dmg` installer, ad-hoc code-signed with native icon:
```bash
# Via Gradle:
./gradlew :macosApp:assembleApp   # Generates macosApp/build/dist/Dockhand.app
./gradlew :macosApp:packageDmg    # Generates macosApp/build/dist/Dockhand-1.0.0-macos-arm64.dmg

# Or via standalone packaging script:
./macosApp/package.sh
```

### CI/CD Pipeline
This project uses **GitLab CI** to automate the build process.
- Pipelines are triggered by **Git Tags** (e.g., `v1.0.0`).
- Generates installers/APKs as job artifacts.

## Credits

This project respects and builds upon the incredible work of **developersu**, the original creator of NS-USBloader and NS-USBloader-mobile.
