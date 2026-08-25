# App Drawer UI

## Goal
Wire `AndroidAppRepository → AppDrawer → UI` so Chell lists the installed
apps and launches them, replacing the empty black screen.

## Acceptance Criteria
- `AndroidAppRepository` implements `core`'s `AppRepository` via `PackageManager`
- Apps with a LAUNCHER activity are listed, de-duplicated by package
- Chell does not appear in its own drawer
- Apps are shown in a horizontally paged grid backed by `AppDrawer.page()`
- Tapping an app launches it; an app with no launch intent shows a message
- An empty app list shows "No apps found" rather than a blank screen
- `./gradlew build` passes, lint included

## Implementation Notes
- Package visibility on API 30+ comes from a `<queries>` element in the
  manifest, not `QUERY_ALL_PACKAGES` (which Play restricts).
- Grid is 4x5; `pageSize` is `COLUMNS * ROWS`.
- Icons are loaded through `PackageManager` and cached per package.

## Status
Done
