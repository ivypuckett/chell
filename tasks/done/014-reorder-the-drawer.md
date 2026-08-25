# Drag to Reorder the Drawer

## Goal
The drawer was alphabetical and fixed. Let a drag say where an app goes, the
way task 013 did for the favourites row.

## Acceptance Criteria
- Long-pressing an app in the grid and dragging it moves it
- Holding a dragged app against an edge sends it to the neighbouring page
- The arrangement is saved and survives a restart and a rotation
- A newly installed app appears at the end, disturbing nothing
- Long-pressing without dragging still opens the action menu
- Search results stay ranked by relevance and are not draggable
- The ordering rule lives in `core`, with unit tests
- `./gradlew build` passes, lint included

## Design decisions
- **One packed list, not a per-page seating plan.** The grid reflows when the
  screen rotates or the keyboard opens, and positions tied to a page and a cell
  would not survive that. `AppOrder` is a list of package names; a reflow only
  re-paginates it. Verified by rotating with an arrangement in place.
- **A move records the whole arrangement, not just the app that moved.** Once
  one app has been placed by hand, the alphabetical order the rest were in is
  a decision too, and a later install must not be allowed to shuffle them.
- **New apps go last**, which falls out of `apply` appending everything the
  order does not know. An empty order therefore leaves the drawer alphabetical,
  exactly as it was before anything was dragged.
- **Search is not draggable.** Results are ranked by relevance, so a position
  in them means nothing to store. `DrawerPagerAdapter` takes a null `onMove`
  while a query is live, which is also what puts the long press back to
  opening the menu.
- **`moveApp` deliberately does not re-render.** The cell has already moved on
  screen, and rebuilding the pager under a finger that is still down would
  cancel the drag being reported.

## Implementation Notes
- `GridDragger` is the gesture, shared with the favourites row, which shrank
  by half when it moved there. Same rules as task 013: the helper's own
  long-press detector stays off, the drag is started by hand, and a press that
  ends without travelling opens the menu.
- Each page is its own `RecyclerView`, so the positions a drag reports are
  positions on that page; `DrawerPagerAdapter` translates them to positions in
  the whole drawer, which is all `AppOrder` understands.
- `PackageListStore` now holds the "an ordered list of package names" logic
  that `FavoritesStore` had, since the arrangement needs exactly the same
  thing. `FavoritesStore` is a thin wrapper over it.

## Known limitation
**A cross-page drag does not continue on the new page.** Each page is a
separate `RecyclerView`, and `ItemTouchHelper` cannot hand a drag from one to
another; doing it properly needs a custom drag layer, or collapsing the pager
into a single horizontally scrolling grid with a custom snap helper and an
index permutation. So holding at an edge flips the page and drops the app at
the near edge of it -- where it was headed -- and the drag ends there. To place
it precisely, grab it again on the new page.

## Verified
`./gradlew build` passes, lint included. `AppOrderTest` covers the ordering
rule; `DrawerOrderTest` covers the drawer, persistence, and that search is
unaffected.

On the emulator:
- dragging Calendar right within a page put it third, matching the stored order
- in landscape (two pages), holding it against the right edge carried it to
  index 9 -- the first slot of page 2 -- and flipped the pager there
- rotating back to portrait re-paginated 9 columns to 4 with the arrangement
  intact, which is the case the packed list exists for
- picking up an edge cell and holding still opened the menu and carried
  nothing

Two bugs were found here rather than by a test, both fixed:
- the edge-hold timer read the cell's position when it was *scheduled*, but
  the cell keeps moving during those 600ms, so it carried whichever app had
  since taken that place. It now reads the position when the timer fires.
- a cell that starts against an edge -- the first or last on a page -- is
  already inside the margin, so picking it up and pausing carried it off the
  page with no movement at all. The timer now only runs once the gesture has
  become a drag.

## Status
Done
