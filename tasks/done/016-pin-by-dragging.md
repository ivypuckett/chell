# Pin by Dragging

## Goal
Pinning is menu-only: the sole way an app reaches the favourites row is
`action_favorite` in the long-press menu. Tasks 013-015 made both containers
draggable, so the direct gesture should work too -- drag an app from the
drawer onto the favourites row to pin it, and drag it back out to unpin it.

## Acceptance Criteria
- Holding a dragged drawer cell against the bottom edge pins that app
- Holding a dragged favourites cell against the top edge unpins it
- Reordering within either container is unchanged, and so is the sideways
  edge hold that carries an app to the next page
- An app already pinned is not pinned twice
- `./gradlew build` passes, lint included

## Design decisions
- Reuse the edge hold from task 014 rather than inventing a second gesture.
  `GridDragger.watchEdges` already turns "held against an edge for 600ms"
  into a callback; this adds the other axis to the same check, so the two
  ways a cell leaves its container stay one mechanism with one timer.
- `onEdgeHold`'s `direction: Int` becomes a named `Edge` (`LEFT`, `RIGHT`,
  `UP`, `DOWN`). Four cases in two axes is past what a signed int reads
  clearly as, and `MainActivity` has to branch on which it got anyway --
  sideways means `carryToPage`, down means pin.
- The drag ends at the hand-off, exactly as `carryToPage` already does. Each
  container is its own `RecyclerView` and `ItemTouchHelper` cannot drag a cell
  from one into another; pretending otherwise is a much larger change than
  this feature is worth.
- The favourites row gains `UP` in its movement flags (`SIDEWAYS_OR_OUT`).
  It was left open whether `onChildDraw` reports a `dY` the callback did not
  permit; it does not. `ItemTouchHelper.updateDxDy` clamps travel in every
  direction the flags omit, so with `SIDEWAYS` alone the row's `dY` is
  always 0 and an upward lift is invisible to the callback. Allowing `UP`
  costs nothing else: a one-row grid has no cell above to swap with, so
  `onMove` is never offered a vertical target.

## Implementation Notes
- `watchEdges` reads one axis per frame -- whichever the finger is actually
  travelling along (`abs(dX) >= abs(dY)`). Reading both would pin a cell
  dragged sideways along the bottom row, which sits inside the bottom margin
  for the whole of that drag, and would unpin any favourite nudged upwards
  while being reordered. The favourites row is the sharp case: it is one cell
  tall, so its cells are inside the *top* margin at rest.
- `MainActivity.leaveDrawer` is public for the same reason `moveApp` and
  `carryToPage` are -- the pager adapter hands it to each page's dragger, and
  it is the seam the Robolectric tests drive.
- Pinning an app that is already pinned is refused rather than allowed to
  fall out of `Favorites.pin`'s `distinct()`. It would not duplicate the app,
  but it would move it to the front of the row, which is not what dragging it
  onto a row it is already in should do.
- An app dragged to the bottom edge also keeps the drawer position it was
  dragged to -- it has visibly travelled to the end of the grid, and the
  reorder happens cell by cell on the way down. Left as is: it is honest
  about where the finger went, and undoing it would mean unwinding moves
  already saved.

## Testing note
The two pure decisions -- what an edge means, and refusing a second pin --
are covered by `FavoritesRowTest` through `leaveDrawer`. The gesture plumbing
either side of that (which axis is read, the hold timer, the movement flags)
is `ItemTouchHelper` geometry and was verified on the emulator.

## Verified
`./gradlew build` passes, lint included; `FavoritesRowTest` runs 10 tests, 0
failures. On the emulator, driven with `adb shell input motionevent` so the
long press and the hold are real:
- Camera dragged from the middle of the drawer to the bottom edge and held:
  `favorites` became `camera2, chrome, deskclock, messaging`.
- The same cell dragged up out of the row and held: `favorites` back to
  `chrome, deskclock, messaging`.
- Sideways drag within the row still reorders (`chrome, deskclock` swapped)
  and unpins nothing.
- In landscape, where a page is a single row -- so every cell is inside both
  the top and the bottom margin -- an app dragged to the right edge and held
  still carried to page 2 (index 0 to index 9) and was not pinned.

## Status
Done
