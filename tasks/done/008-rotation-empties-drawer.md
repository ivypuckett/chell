# Fix: Rotating Emptied the Drawer

## Goal
Rotating the device left Chell showing "No apps found" until it was force
stopped. A regression from task 005.

## Cause
`MainActivity` waited for `pager.doOnLayout` before its first load. Recreating
the activity restores the search field's saved state, which runs the text
watcher before the first layout; that render found `allApps` still empty, so it
hid the pager. A hidden view is never laid out, so the layout the initial load
was waiting on never arrived and the drawer stayed empty for good.

## Fix
Size and load from `grid_container`, the frame holding the pager and the empty
message. It is never hidden, so the load cannot be starved by the empty state
it is meant to replace.

## Acceptance Criteria
- The drawer survives recreation, with a test that fails without the fix
- `./gradlew build` passes, lint included

## Implementation Notes
- `MainActivityRecreateTest` covers it two ways: a real `controller.recreate()`,
  and forcing an empty render before the first layout.
- Found by rotating the emulator while checking the page indicator, not by the
  test suite. Bisecting against 10f61ce confirmed 005 introduced it.

## Status
Done
