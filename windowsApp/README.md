# windowsApp

Native Windows UI: a **WinUI 3 (C#)** frontend talking to a **JVM backend** (`backend/`,
reusing `:core`'s existing desktop JVM actuals — `DesktopUsbController`,
`DesktopFileSplitter`, the desktop `NetworkServer` — unmodified) over a local loopback
JSON-RPC protocol. This split exists because there's no mature way to call Kotlin/JVM
code directly from C#/WinUI; spawning the shared logic as a subprocess and talking to it
over a socket is the standard, well-understood pattern for pairing a native UI with a JVM
engine when true language interop isn't available.

## Status

**Built, run, and verified on Windows 11 (ARM64)** — the WinUI 3 frontend builds via
Visual Studio, spawns the JVM backend, and talks to it correctly over the loopback
JSON-RPC socket. Settings (theme, ROM folder/XCI toggles) persist and are wired into real
upload/file-picker behavior; the titlebar icon, Mica backdrop, and dark/light theming all
match native Windows 11 chrome.

- **`backend/`** is pure JVM, no OS-specific dependency — verified standalone too: starts,
  prints its listening port, accepts a connection, and correctly handles `listDevices`,
  `findRcmDevice`, and unknown-method error cases over the real JSON-RPC protocol.

## Architecture

```
NstkWindowsApp (WinUI 3, C#)  <--loopback TCP, newline-delimited JSON-->  backend (JVM, Kotlin)
        |                                                                          |
   NavigationView shell                                                    RequestHandler
   Upload / Payload /                                                      -> :core's
   Split & Merge / Settings                                                   DesktopUsbController
                                                                               DesktopFileSplitter
                                                                               NetworkServer
                                                                               Goldleaf / Tinfoil
                                                                               RcmPayloadBuilder
```

- The backend binds an OS-assigned loopback port, prints `PORT <n>` as its first stdout
  line, then serves requests until the one client connection closes (at which point it
  exits — it's meant to live for exactly one app session).
- Protocol: `{"id", "method", "params"}` requests, `{"id", "result"|"error"}` responses,
  plus one-way `{"event", "message"}` pushes for streaming log lines during
  upload/inject/convert operations. See `backend/.../Protocol.kt` and
  `NstkWindowsApp/Models/Protocol.cs` — the two are hand-kept in sync (no shared schema
  generation for a protocol this small).

## Building and testing in a Windows VM

1. **Build the backend jar** (do this on any machine with a JDK — the output is portable):
   ```sh
   ./gradlew :windowsApp:backend:fatJar
   ```
   Copy the resulting `windowsApp/backend/build/libs/nstk-windows-backend.jar` into
   `windowsApp/NstkWindowsApp/backend/` (create that folder) before running the C# app —
   see the `<None Include>` in `NstkWindowsApp.csproj`.

2. **On the Windows VM**, install:
   - .NET 8 SDK
   - Windows App SDK workload (`dotnet workload install ...` or via Visual Studio)
   - A JDK 17+ on `PATH` (the backend jar needs `java` to be invokable — this is what
     `BackendClient.StartAsync` shells out to)

3. Build and run:
   ```powershell
   cd windowsApp\NstkWindowsApp
   dotnet run
   ```

## Known gaps to check first when testing

- `BackendClient.StartAsync` assumes `java` is on `PATH` — if the VM doesn't have one, the
  Process.Start call will throw before ever reaching the "no output" error message.
- The app is unpackaged (`WindowsPackageType=None`) for simplicity — no MSIX manifest, no
  Start Menu registration. Fine for VM smoke-testing; would need packaging work for real
  distribution.
- File pickers (`FileOpenPicker`/`FolderPicker`) require `InitializeWithWindow` on desktop
  WinUI 3 apps, which is wired up via `App.MainWindowInstance` — if picker calls throw
  immediately, that's the first place to check.
