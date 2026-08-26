# :composeApp

Compose Multiplatform UI (Material 3) — screens, view models, navigation, and theming.
Consumed only by `androidApp`, where Compose is genuinely native (Android's own modern
UI toolkit, not a cross-platform renderer). The JVM `desktop` target this module used to
also support was removed once `desktopApp` was retired in favor of the native
`macosApp`/`linuxApp`/`windowsApp` apps — see the root README's architecture section.

This module has **no domain logic of its own** — it depends on `:core` (`api(project(":core"))`
in `build.gradle.kts`) for everything USB/network/file-related and only contains
presentation-layer code.

## Package layout

```
ui/
  screens/            UploadScreen, RcmScreen, SplitMergeScreen, SettingsScreen — the
                       common (shared) implementations.
  screens/platform/    expect/actual PlatformUploadScreen etc. — only an androidMain
                       actual now, delegating straight to the common screens.
  viewmodels/          UploadViewModel, RcmViewModel, SplitMergeViewModel, SettingsViewModel.
                       Hold UI state as StateFlow/mutableStateOf, call into :core's classes
                       directly (UsbController, protocols, FileSplitter, NetworkServer).
  navigation/          AppNavigation.kt — the NavHost + Screen enum (Upload/Rcm/SplitMerge/
                       Settings), and PlatformAppLayout (expect/actual, androidMain only —
                       a bottom NavigationBar).
  components/          PlatformAppLayout, PlatformDraggableArea — expect/actual per-platform
                       chrome helpers.
  theme/               Color.kt, Theme.kt (expect/actual), Type.kt, ThemeConfig.kt.
```

## Why this module exists separately from `:core`

Compose Multiplatform doesn't publish artifacts for `macosArm64`/Kotlin-Native targets the
way it does for `androidTarget()`/JVM — so `:core` (which does have a `macosArm64` target
for `macosApp`) can't have Compose as a dependency at all. Splitting the pure logic
(`:core`) from the Compose UI (`:composeApp`) is what makes both possible in the same
repo. See the root README's architecture diagram for the full picture.
