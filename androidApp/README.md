# :androidApp

The Android application: UI, navigation, view models, and theming, in one plain
`com.android.application` module. All domain logic (USB, network transfer, file
splitting, RCM payload injection) lives in `:core`, which this module depends on
directly.

This used to be split across two modules — a thin `androidApp` shell hosting a
Kotlin Multiplatform `composeApp` module for the actual UI. Nothing else ever consumed
`composeApp` (the desktop apps each have their own native UI, see the root README), so
the multiplatform layer was pure indirection. The two were folded into this one module;
`composeApp` no longer exists.

## Architecture

```
MainActivity                    Activity entry point: builds AndroidUsbController /
                                 AndroidFilePicker, wires the USB-attach intent, calls
                                 enableEdgeToEdge(), hosts App() via setContent { }.
  └─ App()                      Applies AppTheme, then AppNavigation.
       └─ AppNavigation()       NavHost + bottom nav bar (AppLayout), four routes:
            ├─ UploadScreen         Upload/RCM/Split-Merge/Settings.
            ├─ RcmScreen
            ├─ SplitMergeScreen
            └─ SettingsScreen
```

- `ui/theme/` — `Theme.kt` (`AppTheme`) picks light/dark from the system and colour
  from the platform's dynamic-colour API (`dynamicLightColorScheme`/
  `dynamicDarkColorScheme`, Android 12+). `ui/theme/oneui/OneUi.kt` detects Samsung One
  UI via the system feature Samsung declares on every One UI build (public
  `PackageManager.hasSystemFeature` API, with a hardware+framework-class fallback) —
  when detected, the app re-layers the platform's own colour scheme the way One UI
  stacks its surfaces (`OneUiColorScheme.kt`) and switches shapes/type/spacing
  (`OneUiTokens.kt`, `AppStyle.kt`) to match. Nothing here talks to a private Samsung
  API; the palette itself always comes from the standard Android dynamic-colour system,
  which already reflects Samsung's own palette on a Galaxy device.
- `ui/components/AdaptiveComponents.kt` — shared composables (top bar, settings rows,
  action buttons) that render Material or One UI depending on which `AppStyle` is
  active, so screens don't branch on device type themselves.
- `ui/components/AppLayout.kt` — the bottom navigation bar + content host.
- `ui/navigation/AppNavigation.kt` — defines the four screens (`Screen` enum, icons
  from `res/drawable/`) and the `NavHost` wiring.
- `ui/screens/`, `ui/viewmodels/` — one screen + one `ViewModel` per destination.

The Compose implementation itself comes from the JetBrains Compose Multiplatform
library artifacts (`org.jetbrains.compose.*` in the version catalog) rather than
`androidx.compose.*` directly, a holdover from when this module's UI was multiplatform
— there is no Kotlin Multiplatform target here any more, just a single Android
application consuming those libraries on one platform.

## Build & run

```bash
./gradlew :androidApp:installDebug
```

Requires an Android SDK configured (`local.properties` with `sdk.dir`, or `ANDROID_HOME`
set), with **API 37** available (`compileSdk`, since the AGP 9.4 upgrade) — Compose's
newer `androidx.compose.animation` artifacts require compiling against it even though
`minSdk`/`targetSdk` stay lower.
