# App Drawer Domain Model

## Goal
Add a pure-JVM `AppDrawer` class to `core` that represents a sorted, paginated
view of installed apps — the domain backbone of the app drawer UI.

## Acceptance Criteria
- `AppDrawer` takes a `List<AppInfo>` and a `pageSize: Int`
- Apps are sorted case-insensitively by label
- `pageCount` returns the number of pages
- `page(index)` returns the apps on that page (last page may be shorter)
- `page(index)` throws `IndexOutOfBoundsException` for invalid indices
- Unit tests in `core` cover all criteria and pass on JVM: `./gradlew :core:test`

## Implementation Notes
- Lives in `core/` — no Android dependencies
- `app/` will later wire `AndroidAppRepository → AppDrawer → UI`
- Keep `AppDrawer` under 50 lines

## Status
Done
