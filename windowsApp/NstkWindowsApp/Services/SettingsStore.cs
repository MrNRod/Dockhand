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
    private record StoredSettings(
        string Theme,
        bool UseRomFolder = false,
        bool AllowXci = true,
        bool AutoCheckUpdates = true);

    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "NstkWindowsApp", "settings.json");

    private static StoredSettings Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return new StoredSettings("System");
            var json = File.ReadAllText(FilePath);
            return JsonSerializer.Deserialize<StoredSettings>(json) ?? new StoredSettings("System");
        }
        catch
        {
            return new StoredSettings("System");
        }
    }

    private static void Save(StoredSettings settings)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(settings));
        }
        catch
        {
            // Best-effort; a failed save just means the choice won't persist.
        }
    }

    /// <summary>"System", "Light", or "Dark". Defaults to "System".</summary>
    public static string LoadTheme() => Load().Theme;
    public static void SaveTheme(string theme) => Save(Load() with { Theme = theme });

    public static bool LoadUseRomFolder() => Load().UseRomFolder;
    public static void SaveUseRomFolder(bool value) => Save(Load() with { UseRomFolder = value });

    public static bool LoadAllowXci() => Load().AllowXci;
    public static void SaveAllowXci(bool value) => Save(Load() with { AllowXci = value });

    public static bool LoadAutoCheckUpdates() => Load().AutoCheckUpdates;
    public static void SaveAutoCheckUpdates(bool value) => Save(Load() with { AutoCheckUpdates = value });
}
