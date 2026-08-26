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
   - Optionally, a JDK 17+ on `PATH` — only needed for this `dotnet run` local-dev path.
     `BackendClient.ResolveJavaExecutable` prefers a bundled `runtime\bin\java.exe`
     (produced by `gradlew :windowsApp:backend:jlinkRuntime`, see below) and only falls
     back to `java` on `PATH` if that folder isn't present.

3. Build and run:
   ```powershell
   cd windowsApp\NstkWindowsApp
   dotnet run
   ```

## Building the MSI installer

```powershell
dotnet tool restore
./gradlew.bat :windowsApp:backend:fatJar :windowsApp:backend:jlinkRuntime
New-Item -ItemType Directory -Force -Path windowsApp\NstkWindowsApp\backend
Copy-Item windowsApp\backend\build\libs\nstk-windows-backend.jar windowsApp\NstkWindowsApp\backend\
New-Item -ItemType Directory -Force -Path windowsApp\NstkWindowsApp\runtime
Copy-Item -Recurse -Force windowsApp\backend\build\runtime\* windowsApp\NstkWindowsApp\runtime\
dotnet publish windowsApp\NstkWindowsApp\NstkWindowsApp.csproj -c Release -p:Platform=x64 -r win-x64 --self-contained true -o windowsApp\NstkWindowsApp\publish
dotnet wix build windowsApp\packaging\windows\Product.wxs -d ProductVersion=1.0.0 -d PublishDir=windowsApp\NstkWindowsApp\publish -arch x64 -out nstk-windows.msi
```

This bundles a minimal `jlink`-built JRE alongside the backend jar and WinUI frontend, so
the installed app needs nothing preinstalled. See the root `.gitlab-ci.yml`'s
`package-windows` job for the exact sequence this is run with in CI (including version
wiring from `gradle.properties`' `app.version`).

## Known gaps to check first when testing

- The app is unpackaged in the `dotnet run`/`dotnet build` sense (`WindowsPackageType=None`,
  no MSIX manifest) — that's still true for local dev, but the MSI built above **does**
  register a proper Start Menu shortcut and Programs-and-Features uninstall entry; it's
  just not an MSIX package.
- File pickers (`FileOpenPicker`/`FolderPicker`) require `InitializeWithWindow` on desktop
  WinUI 3 apps, which is wired up via `App.MainWindowInstance` — if picker calls throw
  immediately, that's the first place to check.
