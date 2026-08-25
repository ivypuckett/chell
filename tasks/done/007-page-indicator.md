# Page Indicator

## Goal
Show which drawer page you are on, and how many there are. Right now a paged
grid gives no sign that further pages exist.

## Acceptance Criteria
- One dot per page, below the grid
- The dot for the current page is visually distinct
- Swiping to another page moves the marked dot
- A single page shows no indicator at all
- The indicator follows a re-page (search, reflow), not just a swipe
- `./gradlew build` passes, lint included

## Implementation Notes
- The dots are plain `View`s with a state-list drawable (`page_dot.xml`): the
  current page is opaque, the rest dimmed, so no second colour is needed and
  `isSelected` alone drives the appearance.
- They are rebuilt in `showApps`, so search and reflow re-page them for free.
  `setCurrentItem` does not fire `onPageSelected` when the page is unchanged,
  which is why `markCurrentPage` is called explicitly after building them.
- Verified on the emulator by raising density (`wm density 640`) to force a
  second page: the marker follows a swipe. At normal density the AVD's 18 apps
  fit one page, where the indicator is correctly absent.

## Status
Done
