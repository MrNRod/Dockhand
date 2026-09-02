using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage.Pickers;
using WinRT.Interop;

namespace DockhandWindowsApp.Views;

public sealed partial class PayloadPage : Page
{
    private string? _payloadPath;
    private bool _isInjecting;

    public PayloadPage()
    {
        InitializeComponent();
        ((App)Application.Current).Backend.OnEvent += OnBackendEvent;
    }

    private void OnBackendEvent(Models.Event e)
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            if (e.EventName == "log")
            {
                LogText.Text += e.Message + "\n";
            }
            else if (e.EventName == "injectDone")
            {
                _isInjecting = false;
                UpdateButtonState();
            }
        });
    }

    private async void SelectPayload_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FileOpenPicker { ViewMode = PickerViewMode.List };
        picker.FileTypeFilter.Add(".bin");
        InitializeWithWindow.Initialize(picker, WindowNative.GetWindowHandle(App.MainWindowInstance));

        var file = await picker.PickSingleFileAsync();
        if (file is null) return;

        _payloadPath = file.Path;
        PayloadNameText.Text = file.Name;
        UpdateButtonState();
    }

    private void UpdateButtonState()
    {
        InjectButton.IsEnabled = _payloadPath is not null && !_isInjecting;
        InjectButton.Content = _isInjecting ? "Injecting…" : "Inject Payload";
    }

    private async void Inject_Click(object sender, RoutedEventArgs e)
    {
        if (_payloadPath is null || _isInjecting) return;
        _isInjecting = true;
        UpdateButtonState();
        LogText.Text = "";

        try
        {
            await ((App)Application.Current).Backend.CallAsync("injectPayload", new { payloadPath = _payloadPath });
        }
        catch (Exception ex)
        {
            LogText.Text += $"[FAIL] {ex.Message}\n";
            _isInjecting = false;
            UpdateButtonState();
        }
    }
}
