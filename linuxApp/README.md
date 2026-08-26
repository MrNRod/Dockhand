# linuxApp

Native Linux UI using [java-gi](https://github.com/jwharm/java-gi) (GTK4 bindings for the
JVM via the Foreign Function & Memory API — stable since JDK 22, no `--enable-preview`
needed). Reuses `:core`'s existing desktop JVM actuals (`DesktopUsbController`,
`DesktopFileSplitter`, the desktop `NetworkServer`) directly and unmodified — this module
only supplies a GTK4 UI layer in place of Compose Desktop.

## Status

**Built, run, and verified on real Ubuntu 24.04 (ARM64)** — a genuine GTK4 window with
working navigation, native file dialogs, and a custom icon+label sidebar. Also packaged
and installed as a real `.deb` via `jpackage` (see below), with a correctly matched dock
icon.

(It can't run on macOS at all, for reference — Homebrew's `gtk4` formula has no `quartz`
GDK backend, so `gtk_application_startup` crashes immediately with a native `NSException`
before ever reaching this module's own code — the exact same wall hit independently with
the Rust/`gtk4-rs` prototype in `~/Projects/Rust/nstk-native`. It only runs on real Linux.)

## Building and running

```sh
./gradlew :linuxApp:run
```

On this dev machine (macOS + Homebrew GTK4), the build script sets `DYLD_LIBRARY_PATH`
automatically so the libraries can be *found* — that's macOS-only plumbing and a no-op on
real Linux, where these libs are already on the standard system search path.

On a Linux VM, you'll need GTK4 installed (e.g. `apt install libgtk-4-1` on
Debian/Ubuntu, or `dnf install gtk4` on Fedora) and a JDK 22+ on `PATH`.

## Packaging

```sh
./gradlew :linuxApp:jpackageDeb   # needs dpkg-deb (present by default on Debian/Ubuntu)
./gradlew :linuxApp:jpackageRpm   # needs rpmbuild (`apt install rpm` on Debian/Ubuntu)
```

Output lands in `build/jpackage/`. `jpackage` also generates the `.desktop` file and
installs the icon — GTK4 removed per-window icon APIs entirely, so this is the *only* way
the app gets a taskbar/launcher icon on Linux at all, not just a packaging nicety. A
custom `.desktop` template lives at `packaging/linux/NS-ToolKit.desktop`, overriding
jpackage's auto-generated one to add `StartupWMClass` — without it, GNOME Shell's dock
can't match the running window back to its icon (it falls back to matching by `Exec` name,
which doesn't line up with the app's actual GApplication ID), even though the static
app-grid icon is fine either way since that's read straight from the `.desktop` file.

## Architecture notes

- No FFI/binding-generation step at all — `:core`'s `jvm("desktop")` target is a normal
  Gradle dependency, called directly like any other JVM library.
- GTK4 has no built-in reactive UI framework the way SwiftUI does, so `AppState` uses a
  simple observer pattern (`Listener.onChanged()`) and each page manually rebuilds the
  parts of its widget tree that changed.
- File pickers use GTK4's modern async `FileDialog` API (4.10+) rather than the older
  `FileChooserNative`, matching current GNOME HIG guidance.
- The Split/Merge mode selector uses two `ToggleButton`s in a `Box` with the `linked` CSS
  class — GTK's idiomatic way to get a native segmented-control look.
