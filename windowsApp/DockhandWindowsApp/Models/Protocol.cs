using System.Text.Json.Serialization;

namespace DockhandWindowsApp.Models;

// Mirrors windowsApp/backend/src/main/kotlin/com/mrnrod45/dockhand/windows/Protocol.kt

public record Request(
    [property: JsonPropertyName("id")] int Id,
    [property: JsonPropertyName("method")] string Method,
    [property: JsonPropertyName("params")] object Params
);

public record Response(
    [property: JsonPropertyName("id")] int Id,
    [property: JsonPropertyName("result")] System.Text.Json.JsonElement? Result,
    [property: JsonPropertyName("error")] string? Error
);

public record Event(
    [property: JsonPropertyName("event")] string EventName,
    [property: JsonPropertyName("message")] string Message
);

public record UsbDeviceInfo(
    [property: JsonPropertyName("name")] string Name,
    [property: JsonPropertyName("vendorId")] int VendorId,
    [property: JsonPropertyName("productId")] int ProductId
);
