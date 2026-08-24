using System.Runtime.InteropServices;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using NstkWindowsApp.Services;
using NstkWindowsApp.Views;
using WinRT.Interop;

namespace NstkWindowsApp;

public sealed partial class MainWindow : Window
{
    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attribute, ref int value, int valueSize);

    private const int DwmwaUseImmersiveDarkMode = 20;

    public MainWindow()
    {
        InitializeComponent();
        Title = "NS-ToolKit";

        // Mica is the native Windows 11 backdrop material — without it the client
        // area falls back to a flat, overly dark color instead of matching the OS.
        SystemBackdrop = new MicaBackdrop();

        ApplyTheme(SettingsStore.LoadTheme());
    }

    /// <summary>theme is "System", "Light", or "Dark".</summary>
    public void ApplyTheme(string theme)
    {
        var elementTheme = theme switch
        {
            "Light" => ElementTheme.Light,
            "Dark" => ElementTheme.Dark,
            _ => ElementTheme.Default // follows the OS setting
        };
        if (Content is FrameworkElement root)
        {
            root.RequestedTheme = elementTheme;

            // RequestedTheme only affects the XAML content — the native title bar
            // and caption buttons are drawn by DWM and need to be told separately.
            var isDark = root.ActualTheme == ElementTheme.Dark;
            var hwnd = WindowNative.GetWindowHandle(this);
            var value = isDark ? 1 : 0;
            DwmSetWindowAttribute(hwnd, DwmwaUseImmersiveDarkMode, ref value, sizeof(int));
        }
    }

    private void NavView_Loaded(object sender, RoutedEventArgs e)
    {
        NavView.SelectedItem = NavView.MenuItems[0];
        ContentFrame.Navigate(typeof(UploadPage));
    }

    private void NavView_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.IsSettingsSelected)
        {
            ContentFrame.Navigate(typeof(SettingsPage));
            return;
        }

        var tag = (args.SelectedItemContainer as NavigationViewItem)?.Tag as string;
        ContentFrame.Navigate(tag switch
        {
            "upload" => typeof(UploadPage),
            "payload" => typeof(PayloadPage),
            "splitmerge" => typeof(SplitMergePage),
            _ => typeof(UploadPage)
        });
    }
}
