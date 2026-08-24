# linuxApp

Native Linux UI using [java-gi](https://github.com/jwharm/java-gi) (GTK4 bindings for the
JVM via the Foreign Function & Memory API — stable since JDK 22, no `--enable-preview`
needed). Reuses `:core`'s existing desktop JVM actuals (`DesktopUsbController`,
`DesktopFileSplitter`, the desktop `NetworkServer`) directly and unmodified — this module
only supplies a GTK4 UI layer in place of Compose Desktop.

## Status

- **Compiles cleanly** against `:core`'s `jvm("desktop")` target.
- **Could not be visually verified on macOS.** Running it here gets all the way through
  loading `libgtk-4`/`libgobject-2.0`/`libgio-2.0` via Panama and into
  `gtk_application_startup`, then crashes with a native `NSException` — Homebrew's `gtk4`
  formula has no `quartz` GDK backend, so it can't actually open a window on macOS at all
  (the exact same wall hit independently with the Rust/`gtk4-rs` prototype in
  `~/Projects/Rust/nstk-native`). Since the crash happens deep inside GTK's own native
  code, past all of this module's Kotlin code, that's strong evidence the Kotlin/java-gi
  code itself is correct — it just needs a real Linux X11/Wayland session to render into.
- **Needs testing in a real Linux VM** to confirm the actual window/widgets render as
  intended.

## Building and running

```sh
./gradlew :linuxApp:run
```

On this dev machine (macOS + Homebrew GTK4), the build script sets `DYLD_LIBRARY_PATH`
automatically so the libraries can be *found* — that's macOS-only plumbing and a no-op on
real Linux, where these libs are already on the standard system search path.

On a Linux VM, you'll need GTK4 installed (e.g. `apt install libgtk-4-1` on
Debian/Ubuntu, or `dnf install gtk4` on Fedora) and a JDK 22+ on `PATH`.

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
