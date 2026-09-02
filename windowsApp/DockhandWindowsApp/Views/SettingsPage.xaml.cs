using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using DockhandWindowsApp.Services;

namespace DockhandWindowsApp.Views;

public sealed partial class SettingsPage : Page
{
    public SettingsPage()
    {
        InitializeComponent();

        // Set after InitializeComponent() so all three RadioButtons already exist —
        // setting IsChecked declaratively in XAML instead would fire Checked while
        // InitializeComponent() is still running, before sibling fields are assigned.
        var savedTheme = SettingsStore.LoadTheme();
        var radio = savedTheme switch
        {
            "Light" => ThemeLightRadio,
            "Dark" => ThemeDarkRadio,
            _ => ThemeSystemRadio
        };
        radio.IsChecked = true;

        AutoCheckUpdatesToggle.IsOn = SettingsStore.LoadAutoCheckUpdates();
        UseRomFolderToggle.IsOn = SettingsStore.LoadUseRomFolder();
        AllowXciToggle.IsOn = SettingsStore.LoadAllowXci();
    }

    private void ThemeRadio_Checked(object sender, RoutedEventArgs e)
    {
        var theme = (string)((RadioButton)sender).Tag;
        SettingsStore.SaveTheme(theme);
        (App.MainWindowInstance as MainWindow)?.ApplyTheme(theme);
    }

    private void AutoCheckUpdatesToggle_Toggled(object sender, RoutedEventArgs e) =>
        SettingsStore.SaveAutoCheckUpdates(AutoCheckUpdatesToggle.IsOn);

    private void UseRomFolderToggle_Toggled(object sender, RoutedEventArgs e) =>
        SettingsStore.SaveUseRomFolder(UseRomFolderToggle.IsOn);

    private void AllowXciToggle_Toggled(object sender, RoutedEventArgs e) =>
        SettingsStore.SaveAllowXci(AllowXciToggle.IsOn);
}
