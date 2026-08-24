using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using NstkWindowsApp.Services;

namespace NstkWindowsApp.Views;

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
    }

    private void ThemeRadio_Checked(object sender, RoutedEventArgs e)
    {
        var theme = (string)((RadioButton)sender).Tag;
        SettingsStore.SaveTheme(theme);
        (App.MainWindowInstance as MainWindow)?.ApplyTheme(theme);
    }
}
