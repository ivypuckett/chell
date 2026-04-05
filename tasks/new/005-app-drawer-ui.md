# App Drawer UI

## Goal
Build the app-drawer screen: a scrollable grid of app icons + labels.
Tapping an icon launches the app; a search bar at the top filters the list.

## Acceptance Criteria
- `AppDrawerActivity` (or `AppDrawerFragment`) shows a `RecyclerView` grid
  (3 columns) of all installed apps
- Each cell shows the app icon (from `PackageManager`) and label
- Tapping a cell fires the launcher intent for that app
- A search `EditText` at the top filters the grid in real time using
  `AppDrawer.filter(query)`
- Back/dismiss returns to the home screen

## Implementation Notes
- `RecyclerView` + `GridLayoutManager(context, 3)`
- Adapter binds `AppInfo` list; icon loaded with
  `packageManager.getApplicationIcon(packageName)`
- Wire: `AndroidAppRepository → AppDrawer → Adapter`
- `AppDrawerActivity` declared in `AndroidManifest.xml`
- `MainActivity` opens `AppDrawerActivity` on upward swipe or button tap
- Keep each file under 400 lines; split adapter into its own file if needed
- Depends on tasks 003 and 004

## Status
New
