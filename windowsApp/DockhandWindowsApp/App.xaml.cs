using Microsoft.UI.Xaml;
using DockhandWindowsApp.Services;

namespace DockhandWindowsApp;

public partial class App : Application
{
    /// <summary>Shared backend connection, available to all pages via ((App)Application.Current).Backend.</summary>
    public BackendClient Backend { get; } = new();

    /// <summary>
    /// The single main window. Pages need this to initialize WinRT pickers
    /// (FileOpenPicker etc.) with a window handle, which desktop WinUI 3 apps
    /// must do explicitly — there's no implicit "current window" like UWP had.
    /// </summary>
    public static Window MainWindowInstance { get; private set; } = null!;

    public App()
    {
        InitializeComponent();
    }

    protected override async void OnLaunched(LaunchActivatedEventArgs args)
    {
        MainWindowInstance = new MainWindow();
        MainWindowInstance.Activate();

        // The backend jar isn't built by this project — run
        // `gradlew :windowsApp:backend:fatJar` first and copy the output here,
        // or adjust this path. See windowsApp/README.md.
        // Note: "backend" subfolder matches the .csproj's
        // <None Include="backend\dockhand-windows-backend.jar"> copy destination.
        var jarPath = System.IO.Path.Combine(AppContext.BaseDirectory, "backend", "dockhand-windows-backend.jar");
        try
        {
            await Backend.StartAsync(jarPath);
        }
        catch (Exception ex)
        {
            // In a real app, surface this in the UI (e.g. a dialog) rather than
            // just logging — left minimal here since this whole project is an
            // unverified scaffold pending testing on a real Windows machine.
            System.Diagnostics.Debug.WriteLine($"Failed to start backend: {ex.Message}");
        }
    }
}
