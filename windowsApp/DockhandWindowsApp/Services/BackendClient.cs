using System.Collections.Concurrent;
using System.Diagnostics;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using DockhandWindowsApp.Models;

namespace DockhandWindowsApp.Services;

/// <summary>
/// Spawns the JVM backend (windowsApp/backend) as a subprocess, connects to the
/// loopback TCP port it prints on startup, and exposes a request/response API
/// plus a stream of pushed events (log lines, operation-complete notifications).
/// One instance per app lifetime — the backend process exits when this
/// connection closes.
/// </summary>
public sealed class BackendClient : IAsyncDisposable
{
    public event Action<Event>? OnEvent;

    private Process? _process;
    private TcpClient? _tcpClient;
    private StreamWriter? _writer;
    private StreamReader? _reader;
    private readonly ConcurrentDictionary<int, TaskCompletionSource<Response>> _pending = new();
    private int _nextId = 1;
    private Task? _readLoopTask;

    /// <summary>
    /// Prefers the jlink-bundled runtime shipped next to the app (see jlinkRuntime in
    /// windowsApp/backend/build.gradle.kts, packaged by the MSI installer) so installed
    /// users need nothing preinstalled; falls back to "java" on PATH for local dev
    /// builds that skip the jlink step.
    /// </summary>
    private static string ResolveJavaExecutable()
    {
        var bundled = Path.Combine(AppContext.BaseDirectory, "runtime", "bin", "java.exe");
        return File.Exists(bundled) ? bundled : "java";
    }

    public async Task StartAsync(string backendJarPath, CancellationToken cancellationToken = default)
    {
        var javaExe = ResolveJavaExecutable();
        _process = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = javaExe,
                Arguments = $"-jar \"{backendJarPath}\"",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            }
        };
        try
        {
            _process.Start();
        }
        catch (System.ComponentModel.Win32Exception ex)
        {
            var bundledPath = Path.Combine(AppContext.BaseDirectory, "runtime", "bin", "java.exe");
            throw new InvalidOperationException(
                $"Could not launch the Java backend (tried '{javaExe}'). Expected a bundled " +
                $"runtime at '{bundledPath}', or 'java' on PATH as a fallback. Run " +
                "`gradlew :windowsApp:backend:jlinkRuntime` and copy its output to " +
                "DockhandWindowsApp/runtime/, or install a JDK 17+.", ex);
        }

        // First line of stdout is "PORT <n>" — see Main.kt in the backend module.
        var firstLine = await _process.StandardOutput.ReadLineAsync(cancellationToken)
            ?? throw new InvalidOperationException("Backend produced no output — did it start correctly?");
        var parts = firstLine.Split(' ');
        if (parts.Length != 2 || parts[0] != "PORT" || !int.TryParse(parts[1], out var port))
        {
            throw new InvalidOperationException($"Unexpected backend startup line: '{firstLine}'");
        }

        _tcpClient = new TcpClient();
        await _tcpClient.ConnectAsync(System.Net.IPAddress.Loopback, port, cancellationToken);
        var stream = _tcpClient.GetStream();
        _writer = new StreamWriter(stream, Encoding.UTF8) { AutoFlush = true };
        _reader = new StreamReader(stream, Encoding.UTF8);

        _readLoopTask = Task.Run(() => ReadLoopAsync(cancellationToken), cancellationToken);
    }

    private async Task ReadLoopAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            var line = await _reader!.ReadLineAsync(cancellationToken);
            if (line is null) break; // backend closed the connection
            if (string.IsNullOrWhiteSpace(line)) continue;

            using var doc = JsonDocument.Parse(line);
            if (doc.RootElement.TryGetProperty("event", out _))
            {
                var evt = JsonSerializer.Deserialize<Event>(line)!;
                OnEvent?.Invoke(evt);
            }
            else
            {
                var response = JsonSerializer.Deserialize<Response>(line)!;
                if (_pending.TryRemove(response.Id, out var tcs))
                {
                    tcs.SetResult(response);
                }
            }
        }
    }

    public async Task<JsonElement?> CallAsync(string method, object parameters, CancellationToken cancellationToken = default)
    {
        var id = Interlocked.Increment(ref _nextId);
        var tcs = new TaskCompletionSource<Response>(TaskCreationOptions.RunContinuationsAsynchronously);
        _pending[id] = tcs;

        var request = new Request(id, method, parameters);
        var json = JsonSerializer.Serialize(request);
        await _writer!.WriteLineAsync(json.AsMemory(), cancellationToken);

        var response = await tcs.Task.WaitAsync(cancellationToken);
        if (response.Error is not null)
        {
            throw new InvalidOperationException($"Backend error ({method}): {response.Error}");
        }
        return response.Result;
    }

    public async ValueTask DisposeAsync()
    {
        _writer?.Dispose();
        _reader?.Dispose();
        _tcpClient?.Dispose();
        if (_process is { HasExited: false })
        {
            try { _process.Kill(); } catch { /* best effort */ }
        }
        if (_readLoopTask is not null)
        {
            try { await _readLoopTask; } catch { /* expected on cancellation */ }
        }
    }
}
