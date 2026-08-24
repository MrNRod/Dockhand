using System.Collections.Concurrent;
using System.Diagnostics;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using NstkWindowsApp.Models;

namespace NstkWindowsApp.Services;

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

    public async Task StartAsync(string backendJarPath, CancellationToken cancellationToken = default)
    {
        _process = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = "java",
                Arguments = $"-jar \"{backendJarPath}\"",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            }
        };
        _process.Start();

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
