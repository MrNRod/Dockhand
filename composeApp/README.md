# :composeApp

Compose Multiplatform UI (Material 3) — screens, view models, navigation, and theming.
Consumed by `androidApp` (where Compose is genuinely native) and `desktopApp` (where it's
a cross-platform renderer, currently covering Windows + Linux — see the root README for
the plan to eventually replace that with `linuxApp`/`windowsApp`).

This module has **no domain logic of its own** — it depends on `:core` (`api(project(":core"))`
in `build.gradle.kts`) for everything USB/network/file-related and only contains
presentation-layer code.

## Package layout

```
ui/
  screens/            UploadScreen, RcmScreen, SplitMergeScreen, SettingsScreen — the
                       common (shared) implementations.
  screens/platform/    expect/actual PlatformUploadScreen etc. Currently both the
                       androidMain and desktopMain actuals just delegate straight to the
                       common screens (desktopMain used to branch on macOS here before
                       macosApp existed — that branch is gone now).
  viewmodels/          UploadViewModel, RcmViewModel, SplitMergeViewModel, SettingsViewModel.
                       Hold UI state as StateFlow/mutableStateOf, call into :core's classes
                       directly (UsbController, protocols, FileSplitter, NetworkServer).
  navigation/          AppNavigation.kt — the NavHost + Screen enum (Upload/Rcm/SplitMerge/
                       Settings), and PlatformAppLayout (expect/actual — Android gets a
                       bottom NavigationBar, Desktop gets an OS-aware sidebar/rail, see
                       ui/layout/DesktopLayouts.kt for the Windows/Linux-specific styling).
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
