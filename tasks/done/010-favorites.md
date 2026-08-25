# Favorites Row

## Goal
Give Chell a home: a row of pinned apps that is always there, so the launcher
opens to something useful rather than straight into the full drawer.

## Acceptance Criteria
- A row of favorites sits below the grid, above the page indicator
- Long-pressing an app offers "Pin to favorites" / "Unpin"
- Favorites survive a restart
- Tapping and long-pressing a favorite behave as they do in the drawer
- An app that is uninstalled disappears from the row
- No favorites means no row at all
- The ordering and membership rules live in `core`, with unit tests
- `./gradlew build` passes, lint included

## Design decisions
- The newest favorite goes to the **front**, and the row shows as many as fit
  across one grid row. That way a just-pinned app is always visible, and there
  is no "favorites are full" error to explain. Nothing is dropped from storage;
  the row is just a window onto the front of the list.
- Pinning an app that is already pinned moves it to the front rather than
  duplicating it.

## Implementation Notes
- `Favorites` in `core` is immutable: `pin`/`unpin` return a new instance, and
  `resolve` maps package names to `AppInfo` against the installed list, so an
  uninstalled app is skipped without being forgotten -- reinstall it and it
  keeps its place.
- `FavoritesStore` persists the order as one newline-delimited string. A
  `StringSet` would have been the obvious `SharedPreferences` type but has no
  order, and the order is the whole point.
- The row reuses `AppGridAdapter`, so tap, long press, and the icon cache all
  behave exactly as they do in the drawer.
- The row is a sibling of `grid_container`, so showing it shrinks the grid and
  the reflow listener from task 006 re-pages on its own.

## Verified
On the emulator: pinning two apps put the newest first, the row survived a
force stop, and the menu reads "Unpin" for an app already pinned.

## Status
Done
