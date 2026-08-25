# App Drawer: Performance, Sizing, and Freshness

## Goal
Address the rough edges left by task 003: main-thread loading, a hard-coded
grid, a drawer that never refreshed, and untested Android-layer code.

## Acceptance Criteria
- App loading happens off the main thread
- Grid columns and rows are derived from the measured viewport, not constants
- The drawer refreshes when packages are installed, removed, or changed
- `AndroidAppRepository` has unit tests
- `./gradlew build` passes, lint included

## Implementation Notes
- `GridMetrics` lives in `core`, not `app`: the arithmetic is pure and so is
  cheap to test. It always returns at least a 1x1 grid, because `AppDrawer`
  rejects a non-positive page size.
- `app_page_vertical_padding` is shared between `page_apps.xml` and
  `MainActivity.gridMetrics()` so the layout and the arithmetic cannot drift.
- `item_app` has a fixed height equal to `app_cell_height` so the computed row
  count matches what is actually laid out.
- The package receiver is registered in `onStart` / unregistered in `onStop`,
  and clears the icon cache before reloading.
- `:app` tests use Robolectric; no emulator required.

## Status
Done
