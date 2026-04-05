# App Drawer Search / Filter

## Goal
Add a `filter(query: String): AppDrawer` method to `AppDrawer` so the UI can show
only apps whose label contains the query string (case-insensitive).

## Acceptance Criteria
- `filter("")` returns an `AppDrawer` equivalent to the original (all apps, same pageSize)
- `filter("ca")` returns only apps whose label contains "ca" (case-insensitive), sorted,
  with the same pageSize
- Result is a new `AppDrawer`; the original is unchanged
- Unit tests in `core` cover all criteria and pass: `./gradlew :core:test`

## Implementation Notes
- Lives entirely in `core/` — no Android dependencies
- Single method on `AppDrawer`; reuses existing constructor/sort logic
- Keep `AppDrawer` under 50 lines

## Status
Done
