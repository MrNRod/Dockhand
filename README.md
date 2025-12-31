# NS-ToolKit (NSTK)

**NS-ToolKit** is a modern, cross-platform fork and merger of [NS-USBloader](https://github.com/developersu/ns-usbloader) and [NS-USBloader-mobile](https://github.com/developersu/ns-usbloader-mobile).

This project unifies the entire codebase into a single **Kotlin Multiplatform (KMP)** application, enabling a consistent experience across Desktop (Windows, macOS, Linux) and Android.

## Features

-   **Cross-Platform Core**: Re-engineered core logic (USB, Network, File I/O) in pure Kotlin suitable for all platforms.
-   **Modern UI**: Fully migrated to **Compose Multiplatform** with Material 3.
    -   Adaptive navigation (Rail for Desktop, Bar for Mobile).
    -   Dark Mode support (including native window/dialogs on macOS).
-   **Unified Architecture**:
    -   One codebase for both Desktop and Android.
    -   Shared ViewModels and Business Logic.
-   **Enhanced Functionality**:
    -   **USB & Network Install**: Supports GoldLeaf, Awoo, Tinfoil, and DBI protocols.
    -   **Native File Picker**: Improved filtering (Extension-based) and multi-file selection.
    -   **RCM Payload Injection**: Integrated Fusée Gelée exploit support for completely native payload injection on both Desktop and Android.
    -   **Split/Merge Tool**: Built-in tool to split files larger than 4GB for FAT32 SD cards and merge them back (fully compatible with legacy split files).
-   **Deployment**:
    -   **Installers**: Native installers for macOS (`.dmg`, `.pkg`), Windows (`.msi`), and Linux (`.deb`, `.rpm`).
    -   **Android**: Standard `.apk` distribution.
    -   **CI/CD**: Fully automated packaging pipeline via GitLab CI.

## Prerequisites

-   **JDK 25** (Required for compilation).
-   **Android Studio** (Ladybug or newer recommended) or **IntelliJ IDEA** (2024.3+).
-   **Git** (For version control).

## Build & Run

### Development
Run the application directly from your IDE or terminal:

**Desktop**:
```bash
./gradlew :desktopApp:run
```

**Android**:
```bash
./gradlew :androidApp:installDebug
```

### Generating Installers
The project is configured to generate native installers using `jpackage`. You must run the packaging command on the corresponding OS (e.g., build Windows MSI on Windows).

**macOS (Apple Silicon/Intel)**:
```bash
./gradlew :desktopApp:packageDmg
./gradlew :desktopApp:packagePkg
```
*Output: `desktopApp/build/compose/binaries/main/`*

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
