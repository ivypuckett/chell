# Android App Repository

## Goal
Implement `AppRepository` in `app/` using Android's `PackageManager` so the
launcher can load the list of installed, launchable apps.

## Acceptance Criteria
- `AndroidAppRepository(context: Context)` implements `AppRepository`
- `installedApps()` queries `PackageManager` for all apps that have a
  `CATEGORY_LAUNCHER` intent, returning an `AppInfo` per result
- Labels come from `PackageManager.getApplicationLabel`
- Returns a stable `List<AppInfo>` (order doesn't matter — `AppDrawer` sorts)
- No Android instrumentation tests required; manual smoke-test on device/emulator

## Implementation Notes
- Lives in `app/src/main/kotlin/dev/chell/launcher/`
- Requires `Context`; inject via constructor (no singleton/static state)
- Query pattern:
  ```kotlin
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  packageManager.queryIntentActivities(intent, 0)
  ```
- Depends on task 003 being done (uses `AppInfo` from `:core`)

## Status
New
