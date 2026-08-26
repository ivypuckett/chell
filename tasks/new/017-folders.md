# Folders

## Goal
Tasks 013-016 made both containers draggable and gave a drag two ways to leave
the container it started in -- sideways to the next page, up or down between
the drawer and the favourites row. The gesture still missing is dropping one
cell *onto* another: hold a dragged app over another app and the two become a
folder, which opens to show what is inside it.

This is the largest change to the domain model so far and the design is not
settled. The options are laid out below rather than decided; pick one before
writing code.

## Acceptance Criteria
- Holding a dragged app over another app's cell combines the two into a folder
- A folder occupies one cell and shows its contents when tapped
- An app can be dragged out of an open folder back into the drawer
- A folder with one app left dissolves back into that app
- Reordering, the sideways edge hold, and pinning are all unchanged
- Search results are unaffected -- a query lists apps, never folders
- `./gradlew build` passes, lint included

## The model decision
`AppOrder` is deliberately one flat packed list of package names, because the
grid reflows on rotation and on the keyboard opening, and anything tied to a
page and a cell would not survive that (see its class comment). A folder is
the first thing in the drawer that is not an app, so something has to give.
Three ways, in the order I would defend them:

1. **Folders as a projection over the flat order.** `AppOrder` is untouched. A
   new `core` class holds the grouping -- package name to folder id -- and the
   displayed list is derived by collapsing each group into a single cell at the
   position of its first member. The flat list stays the single source of
   position, so reflow is still just re-pagination, and the stored order format
   does not change. Cost: two structures have to agree about a package, and
   "position of its first member" needs a rule for what happens when a member
   is dragged away.

2. **A sealed `DrawerItem` (App or Folder).** `AppDrawer.page` returns
   `List<DrawerItem>`, `AppGridAdapter` grows a second view type, and the order
   is an order over items. The most honest model and the one that reads best,
   but it touches every layer: the drawer, the adapter, the dragger's indexes,
   and `AppSearch`, which ranks `AppInfo` and should keep doing so.

3. **A sentinel entry in the flat list.** A folder is a reserved string in
   `AppOrder.packageNames` (`folder:1` or similar) with a side table of
   members. Cheapest diff, keeps `PackageListStore`'s format, and is the least
   honest -- `AppOrder` would hold strings that are not package names, which
   its own comment and `PackageListStore`'s both currently rule out.

Option 1 unless the folder cell turns out to need state that has nowhere to
live in it, in which case option 2 -- and option 2 is the one to jump to
rather than layering more onto option 1 later.

## Implementation Notes
- **The combine gesture fights `ItemTouchHelper`.** `onMove` swaps as soon as a
  dragged cell overlaps a target, so by the time a hold could be timed the two
  cells have already traded places. Suppressing the swap for the target being
  hovered is `canDropOver` / `chooseDropTarget`, not `onMove`. Expect this to
  be the awkward part; the 600ms hold timer itself already exists in
  `GridDragger.watchEdges` and should be reused, keyed on a target cell instead
  of an edge.
- The edge holds must keep working during the same drag. A cell dragged along
  the bottom row is over other cells for the whole of that travel, which is the
  same shape of problem `watchEdges` solved by reading one axis per frame --
  whichever the finger is actually travelling along. Combining should likewise
  require the finger to have *settled*, not merely to be passing over.
- Opening a folder: a popup anchored to the cell is the smaller change and
  matches how the action menu already works (`showAppActions`). Expanding the
  folder inline into the grid looks better and re-opens the reflow question.
  Start with the popup.
- Folder naming is a separate concern and is deliberately not in the acceptance
  criteria above. Auto-name from nothing, and add renaming later only if it is
  asked for -- the guide says add no feature beyond what is needed.
- `MainActivity` is at 298 lines against the 400-line cap and this will not fit
  inside it. Split it first (task 012 did this once already); the drag and
  edge-hold handling is the seam.
- Whether a folder can be pinned to the favourites row is open. The row
  resolves package names through `Favorites.resolve`, so a folder there means
  the same model question a second time. Suggested: no, not in this task.

## Testing note
The grouping model belongs in `core` where it is cheap to test -- combining,
dissolving at one member, and what happens to a group when a member is
uninstalled. The gesture plumbing (the hold timer, `canDropOver`) is
`ItemTouchHelper` geometry and will have to be verified on the emulator, the
same as tasks 015 and 016.

## Status
New
