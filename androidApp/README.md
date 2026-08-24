# :androidApp

Thin Android application shell. `MainActivity.kt` does nothing but host `composeApp`'s
`App()` composable — all screens, view models, and theming live in `:composeApp`; all
domain logic lives in `:core`. This module exists only because Android needs its own
`AndroidManifest.xml`, launcher icons, and an `Activity` entry point.

## Build & run

```bash
./gradlew :androidApp:installDebug
```

Requires an Android SDK configured (`local.properties` with `sdk.dir`, or `ANDROID_HOME`
set) — not available in every dev environment (e.g. this repo has been built without one
present at times; `:core`/`:composeApp`'s Android targets simply can't compile until it's
set up).
