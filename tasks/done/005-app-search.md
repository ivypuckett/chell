# App Search

## Goal
Let the user find an app by typing instead of paging through the whole grid.

## Acceptance Criteria
- A search field sits above the app grid and filters it as the user types
- Matching is case-insensitive and ranks better matches first:
  label prefix, then word-start, then substring; ties break alphabetically
- A blank query shows every app, in the drawer's normal alphabetical order
- A query matching nothing shows a "No matches" message, not a blank screen
- Back clears a non-empty query rather than doing nothing
- Matching and ranking live in `core` with unit tests
- `./gradlew build` passes, lint included

## Implementation Notes
- `AppSearch` lives in `core`: matching and ranking are pure string work, so
  they are cheap to test without an emulator.
- Ranking is a three-way `Match` enum (prefix, word-start, substring) sorted by
  ordinal, then by lowercased label. Package names are not matched -- labels are
  what the user sees.
- `AppDrawer` no longer sorts. It used to sort alphabetically, which silently
  discarded the search ranking; ordering is now the caller's job and the drawer
  only paginates. A device check caught this after the unit tests had missed it.
- `windowSoftInputMode="stateAlwaysHidden"` keeps the keyboard down when Home is
  pressed; without it the new `EditText` takes focus and covers the grid.
- The query is cleared in `onStop` as well as by Back, so returning from an app
  shows the whole drawer rather than a stale filter.
- The Robolectric tests measure and lay out the root view themselves: nothing
  lays a window out under Robolectric, and the first load waits on
  `pager.doOnLayout`. Without that the tests only passed by ordering luck.

## Status
Done
