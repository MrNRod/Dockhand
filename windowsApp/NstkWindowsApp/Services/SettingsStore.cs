using System.Text.Json;

namespace NstkWindowsApp.Services;

/// <summary>
/// Tiny local-file settings persistence. This app is unpackaged
/// (WindowsPackageType=None), so Windows.Storage.ApplicationData isn't
/// available — it requires package identity. A plain JSON file under
/// %LOCALAPPDATA% is the simplest substitute.
/// </summary>
public static class SettingsStore
{
    private record StoredSettings(string Theme);

    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "NstkWindowsApp", "settings.json");

    /// <summary>"System", "Light", or "Dark". Defaults to "System".</summary>
    public static string LoadTheme()
    {
        try
        {
            if (!File.Exists(FilePath)) return "System";
            var json = File.ReadAllText(FilePath);
            var settings = JsonSerializer.Deserialize<StoredSettings>(json);
            return settings?.Theme ?? "System";
        }
        catch
        {
            return "System";
        }
    }

    public static void SaveTheme(string theme)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(new StoredSettings(theme)));
        }
        catch
        {
            // Best-effort; a failed save just means the choice won't persist.
        }
    }
}
