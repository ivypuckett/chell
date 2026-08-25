# Show the Wallpaper

## Goal
Chell drew a solid black rectangle where every other launcher shows the user's
wallpaper. Let it through.

## Acceptance Criteria
- The home screen shows the current wallpaper behind the grid
- App labels, the search hint, and the page dots stay readable over a pale
  wallpaper as well as a dark one
- `./gradlew build` passes, lint included

## Design decisions
- A **scrim over the whole root** rather than a per-view background or a
  gradient behind the grid. One 35%-black layer covers the search field, the
  labels, the favorites row, and the dots at once, and it keeps working when
  something new is added to the layout.
- 35% is deliberately light. A heavier scrim would guarantee contrast but
  would undo the point of the change -- at that opacity the wallpaper reads as
  a tint, not a picture.
- Label shadows carry the rest of the contrast, so the scrim did not have to
  be darkened for the worst case.

## Implementation Notes
- Showing the wallpaper takes two theme attributes, not one:
  `android:windowShowWallpaper` sets `FLAG_SHOW_WALLPAPER` on the window, but
  an opaque `windowBackground` still covers it, so the background has to go
  transparent as well. Either one alone is a no-op.
- `WallpaperTest` asserts both halves. Neither is visible from the layout, and
  a silent revert of either would look fine in every other test.

## Verified
`./gradlew build` passes. On the emulator the wallpaper shows through, and the
labels in the pale band at the top of the image are as legible as the ones over
the dark half.

## Status
Done
