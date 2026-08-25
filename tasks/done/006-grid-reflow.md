# Grid Reflow on Viewport Change

## Goal
Re-derive the grid when the pager's height changes, instead of computing it
once at first layout. Left over from task 005: with `adjustResize`, the
keyboard opening over the search field shrinks the pager, and the rows stayed
sized for a viewport that was no longer there.

## Acceptance Criteria
- Pagination is recomputed when the pager's measured size changes the grid
- Recomputing does not reenter the layout pass or loop
- An unchanged grid does no work
- `./gradlew build` passes, lint included

## Implementation Notes
- `MainActivity` keeps `currentMetrics` (the grid the pager is laid out for)
  and re-renders from an `OnLayoutChangeListener` only when `gridMetrics()`
  differs. The equality check is what stops the loop: after `showApps`,
  `currentMetrics` matches, so later layout passes are no-ops.
- The re-render is handed to `pager.post`; swapping the adapter inside the
  layout pass would reenter `RecyclerView`'s own layout.
- The arithmetic itself is `GridMetrics.fit`, already unit tested in `core`.

## Testing gap
The trigger is not covered by an automated test, and was not exercised
end to end:

- Robolectric pins the window to the emulated display size. Laying the root
  or the pager out by hand is undone by the next traversal, and a
  `setQualifiers` config change does not relayout an existing window. Tests
  written against it passed without the feature, so they were removed rather
  than kept as false assurance.
- The `chell-test` AVD's IME is a floating toolbar: `mInputShown=true` but the
  app window stays 1080x2400, so the keyboard never resizes the pager.

What is verified: the arithmetic (`GridMetricsTest`), and that the listener
causes no loop, flicker, or regression in normal use on the emulator. A device
with a conventional IME would exercise the rest.

## Status
Done
