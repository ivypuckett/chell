# Home Screen UI

## Goal
Polish `MainActivity` into a proper home screen: wallpaper background, a dock
strip at the bottom, and a gesture/button to open the app drawer.

## Acceptance Criteria
- Home screen shows the system wallpaper as the background
  (`WallpaperManager.getInstance(this).drawable`)
- A single "Apps" FAB or up-swipe gesture opens `AppDrawerActivity`
- The layout is a `ConstraintLayout` with a transparent status bar
- `onBackPressed` is a no-op (launcher behaviour — already implemented)

## Implementation Notes
- Set `android:windowBackground` to `@android:color/transparent` and
  `android:windowShowWallpaper` to `true` in the activity theme
- Use `WindowCompat.setDecorFitsSystemWindows(window, false)` for edge-to-edge
- FAB at bottom-center with label "Apps"; tapping starts `AppDrawerActivity`
- Optional: `GestureDetector` for upward fling as an alternative to the FAB
- Depends on task 005

## Status
New
