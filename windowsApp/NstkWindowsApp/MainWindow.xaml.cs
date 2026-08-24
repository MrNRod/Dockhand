using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using NstkWindowsApp.Views;

namespace NstkWindowsApp;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        Title = "NS-ToolKit";
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
