# Split Up MainActivity

## Goal
`MainActivity` had grown to 283 lines against the project's 400-line ceiling,
owning the pager, search, the page indicator, the favorites row, and the
long-press menu. Get the two self-contained pieces out before the next feature
lands on top of them.

## Acceptance Criteria
- The page indicator and the favorites row live in their own classes
- No user-visible change; the existing tests pass unmodified
- `./gradlew build` passes, lint included

## Design decisions
- **`FavoritesRow` owns the pinned set and its store**, not just the view. The
  activity was holding `favorites`, `favoritesStore`, and a `columns` argument
  it had to re-derive from `currentMetrics` on every pin -- moving the state to
  the row means pinning re-renders without the caller knowing what is on screen.
- The row remembers the apps and column count from the last `show`, which is
  what removes that argument from `pin`/`unpin`.
- `pinToFavorites`/`unpinFromFavorites` stay on `MainActivity` as one-line
  delegates: they are what `FavoritesRowTest` drives, and a refactor should not
  need the tests rewritten to prove it kept the behaviour.

## Implementation Notes
- `MainActivity` 283 -> 229 lines; `PageIndicator` 43, `FavoritesRow` 60.
- `PageIndicator` reads the dot dimensions once at construction rather than on
  every rebuild.

## Verified
`./gradlew build` passes with lint. Every existing `:app` test passes unchanged,
which is the point -- no test was touched in this task.

## Status
Done
