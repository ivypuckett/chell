# Drag Vertically in the Drawer

## Goal
Task 014 shipped the drawer drag with `START or END` movement flags, so an app
could only be moved along its own row. In a grid it should move in both axes.

## Acceptance Criteria
- An app can be dragged up and down as well as sideways
- The favourites row still moves sideways only
- `./gradlew build` passes, lint included

## Design decisions
- The movement flags become a `GridDragger` parameter with two named values,
  `SIDEWAYS` and `GRID`, rather than being hard-coded. The row and the grid
  genuinely differ -- a single row has nowhere above or below to move a cell
  to -- and naming that reads better at the call site than an int of ORed
  constants.
- `SIDEWAYS` stays the default, so the row keeps its behaviour by saying
  nothing.

## Implementation Notes
- The "has this gesture become a drag" check tested `abs(dX)` only, which
  would have ignored a purely vertical drag and popped the action menu open at
  the end of it. It now takes either axis.
- Nothing in `core` changed: `AppOrder` indexes one packed list, and moving a
  cell down a row is just a larger step along it.

## Testing note
Not covered by a unit test. What changed is the movement flags handed to
`ItemTouchHelper` and the axis of a slop check; the resulting index move goes
through `AppOrder.move`, which `AppOrderTest` already covers. Verified on the
emulator instead.

## Verified
`./gradlew build` passes, lint included. On the emulator, dragging Camera from
the first cell straight down two rows moved it from index 0 to index 8 -- the
first cell of row 3 in a four-column grid -- and the stored order matched.

## Status
Done
