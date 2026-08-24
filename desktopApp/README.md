# :desktopApp

Compose Desktop (JVM) shell around `:composeApp`. **Currently targets Windows and Linux
only** — macOS packaging was removed once `macosApp` (native SwiftUI) took over that
platform; see the root README's architecture section.

This is meant to be a **temporary fallback**: it stays in place for Windows/Linux until
`windowsApp` and `linuxApp` (their native replacements) are each confirmed working in a
real VM, at which point this module gets scoped down or retired per platform.

## Build & run

```bash
./gradlew :desktopApp:run
```

## Packaging

```bash
./gradlew :desktopApp:packageMsi   # Windows
./gradlew :desktopApp:packageDeb   # Linux (Debian/Ubuntu)
./gradlew :desktopApp:packageRpm   # Linux (Fedora/RHEL)
```

Output lands under `desktopApp/build/compose/binaries/main/`. Icons for these installers
live in `src/jvmMain/resources/` (`icon.ico` for Windows, `icon.png` for Linux — there's
no `icon.icns` anymore since this module no longer packages for macOS).

## What's in here

Just `main.kt` — OS detection for dark-mode polling (`isSystemDarkTheme()`, still branches
on Linux/Windows for `gsettings`/registry checks) and the `Window { }` + `application { }`
Compose Desktop boilerplate. No screens or view models live here; those are all in
`:composeApp`.
