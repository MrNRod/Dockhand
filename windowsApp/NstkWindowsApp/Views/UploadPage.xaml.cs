using System.Collections.ObjectModel;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage.Pickers;
using WinRT.Interop;
using NstkWindowsApp.Models;

namespace NstkWindowsApp.Views;

public sealed partial class UploadPage : Page
{
    public record FileEntry(string Name, string Path);

    private readonly ObservableCollection<FileEntry> _files = new();
    private string _protocol = "Goldleaf";
    private string _transport = "USB";
    private bool _isUploading;

    public UploadPage()
    {
        InitializeComponent();
        FilesList.ItemsSource = _files;

        var app = (App)Application.Current;
        app.Backend.OnEvent += OnBackendEvent;
    }

    private void OnBackendEvent(Models.Event e)
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            if (e.EventName == "log")
            {
                LogText.Text += e.Message + "\n";
            }
            else if (e.EventName == "uploadDone")
            {
                _isUploading = false;
                UpdateUploadButtonState();
            }
        });
    }

    private void ProtocolCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        _protocol = (string)((ComboBoxItem)ProtocolCombo.SelectedItem).Tag;
        // ComboBox fires SelectionChanged as soon as its IsSelected="True" item is
        // parsed, which happens before InitializeComponent() has assigned fields for
        // elements declared later in the XAML tree (like TransportCombo).
        if (TransportCombo == null) return;
        var transportEnabled = _protocol == "Awoo";
        TransportCombo.IsEnabled = transportEnabled;
        if (!transportEnabled)
        {
            TransportCombo.SelectedIndex = 0; // USB
        }
    }

    private void TransportCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (TransportCombo.SelectedItem is not ComboBoxItem item) return;
        _transport = (string)item.Tag;
        // Same early-fire issue as ProtocolCombo_SelectionChanged: IpAddressBox and
        // UploadButton are declared later in the XAML tree and aren't assigned yet
        // the first time this fires during parsing.
        if (IpAddressBox == null || UploadButton == null) return;
        IpAddressBox.Visibility = _transport == "NET" ? Visibility.Visible : Visibility.Collapsed;
        UploadButton.Content = _transport == "USB" ? "Upload to Switch" : "Upload over Network";
    }

    private async void AddFiles_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FileOpenPicker { ViewMode = PickerViewMode.List };
        picker.FileTypeFilter.Add("*");
        // Desktop (unpackaged) WinUI 3 apps must associate pickers with a window handle.
        InitializeWithWindow.Initialize(picker, WindowNative.GetWindowHandle(App.MainWindowInstance));

        var files = await picker.PickMultipleFilesAsync();
        foreach (var file in files)
        {
            _files.Add(new FileEntry(file.Name, file.Path));
        }
        UpdateUploadButtonState();
    }

    private void RemoveFile_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as Button)?.Tag is FileEntry entry)
        {
            _files.Remove(entry);
            UpdateUploadButtonState();
        }
    }

    private void UpdateUploadButtonState()
    {
        UploadButton.IsEnabled = _files.Count > 0 && !_isUploading;
    }

    private async void Upload_Click(object sender, RoutedEventArgs e)
    {
        if (_files.Count == 0 || _isUploading) return;
        _isUploading = true;
        UpdateUploadButtonState();
        LogText.Text = "";

        var app = (App)Application.Current;
        var parameters = new
        {
            protocol = _protocol,
            transport = _transport,
            ip = IpAddressBox.Text,
            files = _files.Select(f => f.Path).ToArray()
        };

        try
        {
            await app.Backend.CallAsync("startUpload", parameters);
        }
        catch (Exception ex)
        {
            LogText.Text += $"[FAIL] {ex.Message}\n";
            _isUploading = false;
            UpdateUploadButtonState();
        }
    }
}
