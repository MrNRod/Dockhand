# NS-ToolKit (NSTK)

**NS-ToolKit** is a modern, cross-platform fork and merger of [NS-USBloader](https://github.com/developersu/ns-usbloader) and [NS-USBloader-mobile](https://github.com/developersu/ns-usbloader-mobile).

The core USB/network/file logic (Goldleaf, Tinfoil/Awoo/Sphaira protocols, RCM payload
injection, split/merge) is written once in Kotlin Multiplatform and shared across every
platform. The UI, however, is **native per platform** — Compose on Android, SwiftUI on
macOS, GTK4 on Linux, WinUI 3 on Windows — rather than one Compose UI stretched across
everything.

## Architecture

```
:core            Pure Kotlin logic — USB, network, file splitting, RCM. No UI dependency
                 at all. Targets: Android, JVM (desktop), macosArm64 (Kotlin/Native).
                 Consumed directly by every app below.                    → core/README.md

:composeApp      Compose Multiplatform UI (Material 3), consumed by androidApp and
                 desktopApp. Android is genuinely native here — Compose is Android's own
                 modern UI toolkit, not a cross-platform renderer, for that target.
                                                                       → composeApp/README.md

:androidApp      Thin Android application shell wrapping :composeApp.     → androidApp/README.md
:desktopApp      Compose Desktop shell wrapping :composeApp — currently serves
                 Windows + Linux (see note below on macosApp/linuxApp/windowsApp).
                                                                        → desktopApp/README.md

macosApp/        Native SwiftUI app (Swift Package, not a Gradle module) linking directly
                 against a Kotlin/Native framework built from :core. Real NavigationSplitView
                 sidebar, real macOS menu bar, real system materials — no Compose involved.
                                                                          → macosApp/README.md

linuxApp/        Native GTK4 UI (Kotlin/JVM via java-gi, the Foreign Function & Memory
                 API bindings for GTK4) calling :core's existing desktop JVM code directly.
                                                                          → linuxApp/README.md

windowsApp/      Native WinUI 3 (C#) frontend + a small JVM backend (windowsApp/backend,
                 also built on :core) that the frontend spawns as a subprocess and talks
                 to over a local JSON-RPC socket.                      → windowsApp/README.md
```

### Status of the native apps

- **macosApp**: built, run, and verified — a real native window with working navigation.
- **linuxApp**: compiles cleanly against `:core`; not yet visually confirmed in a real
  Linux environment (see `linuxApp/README.md` for why and what to check).
- **windowsApp**: the JVM backend is built and tested end-to-end; the WinUI frontend is
  unverified — it needs an actual Windows machine or VM to build and run at all (see
  `windowsApp/README.md`).

`desktopApp` (Compose) is being kept as the Windows/Linux fallback until `linuxApp` and
`windowsApp` are each confirmed working, at which point it'll be scoped down or retired
per platform.

## Features

-   **USB & Network Install**: Goldleaf, Awoo, Tinfoil, and Sphaira protocols.
-   **RCM Payload Injection**: Fusée Gelée exploit support, native on every platform.
-   **Split/Merge Tool**: Split files larger than 4GB for FAT32 SD cards and merge them
    back (compatible with legacy split files).
-   **Native file pickers** on every platform (`NSOpenPanel`, GTK4 `FileDialog`, WinRT
    `FileOpenPicker`, Android's Storage Access Framework).

## Prerequisites

-   **JDK 25** (Required for compilation).
-   **Xcode** (for `macosApp` — cinterop against custom C libraries like libusb needs the
    full Xcode toolchain, not just Command Line Tools).
-   **GTK4** (for `linuxApp` — e.g. `apt install libgtk-4-1` on Debian/Ubuntu).
-   **.NET 8 SDK + Windows App SDK** (for `windowsApp`'s WinUI frontend — Windows only).
-   **Android Studio** (Ladybug or newer) or **IntelliJ IDEA** (2024.3+).

## Build & Run

**Android**:
```bash
./gradlew :androidApp:installDebug
```

**Desktop (Windows/Linux, Compose)**:
```bash
./gradlew :desktopApp:run
```

**macOS (native SwiftUI)**:
```bash
./gradlew :core:linkDebugFrameworkMacosArm64
cd macosApp && swift run NstkMac
```

**Linux (native GTK4)**:
```bash
./gradlew :linuxApp:run
```

**Windows (native WinUI)** — see `windowsApp/README.md` for the full two-part build
(JVM backend + C# frontend).

### Generating Installers

The project is configured to generate native installers using `jpackage` for the Compose
Desktop app, which now targets **Windows and Linux only** (`desktopApp` no longer builds
a macOS package — macOS ships as the native SwiftUI app in `macosApp/` instead). Run the
packaging command on the corresponding OS.

**Windows**:
```bash
./gradlew :desktopApp:packageMsi
```

**Linux**:
```bash
./gradlew :desktopApp:packageDeb
./gradlew :desktopApp:packageRpm
```

### CI/CD Pipeline
This project uses **GitLab CI** to automate the build process.
-   Pipelines are triggered by **Git Tags** (e.g., `v1.0.0`).
-   Generates all desktop installers and Android APKs as job artifacts.

## Credits

This project respects and builds upon the incredible work of **developersu**, the original creator of NS-USBloader and NS-USBloader-mobile.

-   Original Desktop App: [ns-usbloader](https://github.com/developersu/ns-usbloader)
-   Original Mobile App: [ns-usbloader-mobile](https://github.com/developersu/ns-usbloader-mobile)

**NS-ToolKit** is a comprehensive refactor to Kotlin Multiplatform, aiming to maintain the spirit of the original tools while modernizing the tech stack for easier maintenance and cross-platform feature parity.

The project namespace has been refactored to `com.mrnrod45.nstk` to reflect this fork.

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

You are free to use, modify, and distribute this software under the terms of the GPLv3. See the [LICENSE](LICENSE) file for details.

---
*Forked and maintained by mrnrod45.*
