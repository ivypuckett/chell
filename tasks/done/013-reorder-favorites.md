# Drag to Reorder Favorites

## Goal
Favorites were ordered by when they were pinned, so moving one meant unpinning
it and pinning it again. Let a drag say where an app goes.

## Acceptance Criteria
- Long-pressing a favorite and dragging it moves it within the row
- The new order is saved and survives a restart
- Long-pressing without dragging still opens the action menu
- A pinned app that is not installed keeps its place
- The reordering rule lives in `core`, with unit tests
- `./gradlew build` passes, lint included

## Design decisions
- **`Favorites.reorder` takes the visible order and rewrites slots**, rather
  than taking a from/to pair. A position in the row is not a position in the
  stored list: the row is a window onto the front of it, and `resolve` skips
  packages that are not installed. Rewriting the slots those visible packages
  already occupy means a drag reorders exactly what the user can see, and
  anything hidden -- uninstalled, or off the end of the row -- keeps its place.
- **The drag is started by hand from the cell's own long press**, with
  `isLongPressDragEnabled = false`. A long press already opened the action
  menu, and leaving `ItemTouchHelper`'s detector on would put two detectors on
  one gesture, racing over the same ~500ms.
- **A press that ends without travelling opens the menu**, from `clearView`.
  One gesture keeps both meanings, so nothing had to be added to the menu to
  make room for dragging.
- The order is saved on every step of the drag rather than on drop, so a drag
  interrupted by the launcher going away is not lost.

## Implementation Notes
- `dragged` is set from `onChildDraw` once the finger passes the touch slop,
  **not** from `onMove`. Keying it to swaps meant a drag that ended between two
  cells swapped nothing, counted as "not a drag", and popped the action menu
  open at the end of it -- which is not what the user just asked for. Caught on
  the emulator, not by a test.
- `moveFavorite` on `MainActivity` exists for the same reason
  `pinToFavorites` does: it is what the Robolectric tests drive, since the
  gesture itself is not reachable from them.

## Verified
`./gradlew build` passes, lint included. Core tests cover the slot-rewriting
rule; `FavoritesRowTest` covers the row and its persistence.

On the emulator, with Clock/Messages/Chrome pinned:
- dragging Clock from the first slot to the last left the row as Messages,
  Chrome, Clock, and the stored order matched
- a long press with no movement opened the menu, anchored to the cell, reading
  "Unpin" and offering no "Uninstall" for a system app
- a drag that ended between two cells changed nothing and opened no menu

One caveat found while testing with injected events: a press that lands inside
`ItemTouchHelper`'s ~250ms snap-back animation is swallowed, because the helper
is still intercepting. A real press arrives well after that, and the gesture
after it behaves normally.

## Status
Done
