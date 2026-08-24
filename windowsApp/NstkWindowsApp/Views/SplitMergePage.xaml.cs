using System.Collections.ObjectModel;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage.Pickers;
using WinRT.Interop;

namespace NstkWindowsApp.Views;

public sealed partial class SplitMergePage : Page
{
    private readonly ObservableCollection<string> _paths = new();
    private bool _isSplitMode = true;
    private bool _isProcessing;
    private string _outputDir = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile) + "\\Downloads";

    public SplitMergePage()
    {
        InitializeComponent();
        PathsList.ItemsSource = _paths;
        OutputDirText.Text = _outputDir;
        ((App)Application.Current).Backend.OnEvent += OnBackendEvent;
    }

    private void OnBackendEvent(Models.Event e)
    {
        if (e.EventName != "convertDone") return;
        DispatcherQueue.TryEnqueue(() =>
        {
            _isProcessing = false;
            StatusText.Text = e.Message;
            UpdateButtonState();
        });
    }

    private void SplitToggle_Click(object sender, RoutedEventArgs e)
    {
        _isSplitMode = true;
        MergeToggle.IsChecked = false;
        SelectFilesButton.Content = "Select File…";
        ActionButton.Content = "Split";
    }

    private void MergeToggle_Click(object sender, RoutedEventArgs e)
    {
        _isSplitMode = false;
        SplitToggle.IsChecked = false;
        SelectFilesButton.Content = "Add Files…";
        ActionButton.Content = "Merge";
    }

    private async void SelectFiles_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FileOpenPicker { ViewMode = PickerViewMode.List };
        picker.FileTypeFilter.Add("*");
        InitializeWithWindow.Initialize(picker, WindowNative.GetWindowHandle(App.MainWindowInstance));

        if (_isSplitMode)
        {
            var file = await picker.PickSingleFileAsync();
            if (file is null) return;
            _paths.Clear();
            _paths.Add(file.Path);
        }
        else
        {
            var files = await picker.PickMultipleFilesAsync();
            foreach (var file in files)
            {
                if (!_paths.Contains(file.Path)) _paths.Add(file.Path);
            }
        }
        UpdateButtonState();
    }

    private void Clear_Click(object sender, RoutedEventArgs e)
    {
        _paths.Clear();
        UpdateButtonState();
    }

    private async void ChangeOutputDir_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FolderPicker();
        picker.FileTypeFilter.Add("*");
        InitializeWithWindow.Initialize(picker, WindowNative.GetWindowHandle(App.MainWindowInstance));

        var folder = await picker.PickSingleFolderAsync();
        if (folder is null) return;
        _outputDir = folder.Path;
        OutputDirText.Text = _outputDir;
    }

    private void UpdateButtonState()
    {
        ActionButton.IsEnabled = _paths.Count > 0 && !_isProcessing;
    }

    private async void Action_Click(object sender, RoutedEventArgs e)
    {
        if (_paths.Count == 0 || _isProcessing) return;
        _isProcessing = true;
        StatusText.Text = _isSplitMode ? "Splitting..." : "Merging...";
        ActionButton.Content = "Processing…";
        UpdateButtonState();

        var parameters = new
        {
            isSplit = _isSplitMode,
            paths = _paths.ToArray(),
            outputDir = _outputDir
        };

        try
        {
            await ((App)Application.Current).Backend.CallAsync("convert", parameters);
        }
        catch (Exception ex)
        {
            _isProcessing = false;
            StatusText.Text = $"Error: {ex.Message}";
            UpdateButtonState();
        }
    }
}
