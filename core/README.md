# :core

Pure Kotlin domain logic — USB communication, network transfer, file splitting, RCM
payload injection. **No UI dependency of any kind.** This is what makes the
native-UI-per-platform architecture possible: every app in this repo (`androidApp`,
`macosApp`, `linuxApp`, `windowsApp`) links against this same module and gets identical
protocol behavior.

## Targets

| Target | Source set | Notes |
|---|---|---|
| Android | `androidMain` | Consumed directly by `androidApp`. |
| JVM (desktop) | `desktopMain` | Consumed directly by `linuxApp` and `windowsApp/backend`. |
| `macosArm64` (Kotlin/Native) | `macosArm64Main` | Consumed by `macosApp` via a linked `.framework`. |

## Package layout

```
domain/
  usb/         UsbDevice, UsbConnection, UsbController — plain interfaces (not expect/actual;
               each platform provides its own concrete implementing class instead, see below).
  protocols/   Goldleaf, Tinfoil (Awoo/Sphaira), Pfs0, Crc32c — the actual transfer protocols.
               These only ever talk to UsbConnection, so they're 100% shared, unmodified,
               across every platform including the Kotlin/Native macOS target.
  net/         NetworkServer — expect/actual (JVM: java.net sockets, macOS: Ktor-network).
  file/        FileSplitter — split/merge for FAT32-compatible file chunking, plus the
               getFileSplitter() expect/actual factory.
  rcm/         RcmPayloadBuilder — Fusée Gelée payload construction (pure Kotlin, no
               platform dependency at all).
  models/      UnifiedFile, LogPrinter, MsgType, FileStatus — shared value types/interfaces.
  util/        IoDispatcher — expect/actual, since Dispatchers.IO isn't available on
               Kotlin/Native (macOS actual falls back to Dispatchers.Default).

platform/
  PathUtils.kt         expect/actual getDefaultDownloadsPath().
  file/FilePicker.kt   Interface + Android/Desktop actuals. No macOS actual — the SwiftUI
                       app calls NSOpenPanel itself and constructs a MacUnifiedFile from
                       the result; native file-picker UX belongs in the native UI layer,
                       not here.
```

## Adding a new platform target

Since `UsbDevice`/`UsbConnection`/`UsbController` are plain interfaces rather than
`expect`/`actual`, a new platform doesn't need to touch any shared code — it just needs a
new class implementing those three interfaces (see `MacosUsbController`/`Device`/
`Connection` for the Kotlin/Native + libusb-cinterop example, or `DesktopUsbController`
for the JVM + usb4java example). Everything in `domain/protocols/` will work against it
unmodified.

For `NetworkServer`, `FileSplitter`'s factory, `PathUtils`, and `IoDispatcher`, which
*are* `expect`/`actual`, add a new actual for the new source set.

## macOS-specific notes

The `macosArm64Main` actuals use:
- **libusb via cinterop** (`src/nativeInterop/cinterop/libusb.def`) for USB — requires
  full Xcode (not just Command Line Tools) to generate, since Kotlin/Native's interop
  tool shells out to `xcrun`/`xcodebuild` even for a locally-defined `.def` file.
- **Ktor-network** for the socket server, since it publishes real klibs for
  `macosArm64`/`macosX64` and avoids hand-rolled POSIX socket cinterop.
- **Plain POSIX file I/O** (`fopen`/`fread`/`fwrite`/`opendir`/`readdir`) for file
  splitting and `UnifiedFile`, via Kotlin/Native's bundled `platform.posix` — no custom
  cinterop needed for these.

Build the framework with:
```bash
./gradlew :core:linkDebugFrameworkMacosArm64
```
This produces a single-arch `macosArm64` `.framework` (no XCFramework/multi-arch merge —
that step also needs `xcodebuild`, and isn't set up here since only Apple Silicon is
targeted for local development so far).
