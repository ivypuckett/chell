# Long-press Actions

## Goal
Long-pressing an app should offer the two things a launcher is expected to do
with it: open its system settings page, and uninstall it. Tapping is currently
the only gesture the drawer supports.

## Acceptance Criteria
- Long-pressing an app cell opens a menu anchored to it
- "App info" opens the system app details screen for that package
- "Uninstall" starts the system uninstall flow
- Uninstall is not offered for system apps, which cannot be removed
- A long press does not also launch the app
- `./gradlew build` passes, lint included

## Implementation Notes
- A `PopupMenu` anchored to the pressed cell, inflated from
  `res/menu/app_actions.xml`. Uninstall is hidden rather than disabled when
  `isSystemApp` is true.
- `AppGridAdapter`'s long-click listener returns true so the press is consumed;
  otherwise the click listener fires on release and launches the app.
- `isSystemApp` checks `FLAG_SYSTEM` or `FLAG_UPDATED_SYSTEM_APP`, and reports
  false for an unknown package rather than throwing.
- The Robolectric tests install a `PackageInfo` as well as adding a
  `ResolveInfo`: resolving a launcher activity does not make the package
  installed, and `isSystemApp` reads the installed `ApplicationInfo`.

## Verified
Long press opens the menu on the emulator, and "App info" lands on
`com.android.settings/.spa.SpaActivity`. The uninstall flow was not exercised
end to end: every app on the `chell-test` AVD is a system app, so the item is
correctly never offered there. Its intent is unit tested.

## Status
Done
