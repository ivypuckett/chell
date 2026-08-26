package dev.chell.launcher.core

/**
 * The order the drawer arranges apps in, once the user has moved one.
 *
 * A single packed list, not a per-page seating plan: the grid reflows when the
 * screen rotates or the keyboard opens, and positions tied to a page and a
 * cell would not survive that. Positions here are indexes into one list, so a
 * reflow only re-paginates it.
 *
 * Instances are immutable; [move] returns a new one.
 */
class AppOrder(packageNames: List<String> = emptyList()) {

    val packageNames: List<String> = packageNames.distinct()

    /**
     * [apps] arranged by this order: everything arranged, in that order,
     * followed by everything else in the order it arrived.
     *
     * The tail is what puts a newly installed app at the end rather than in
     * the middle of an arrangement. Callers pass apps alphabetically, so an
     * empty order leaves the drawer alphabetical, which is what it was before
     * anything was dragged.
     */
    fun apply(apps: List<AppInfo>): List<AppInfo> {
        if (packageNames.isEmpty()) return apps
        val byPackage = apps.associateBy { it.packageName }
        val arranged = packageNames.mapNotNull { byPackage[it] }
        val known = packageNames.toSet()
        return arranged + apps.filterNot { it.packageName in known }
    }

    /**
     * Moves the cell at [from] to [to], both indexes into [cells].
     *
     * Cells, not apps: a folder is one cell holding several packages, and they
     * travel together. Writing every cell's packages back out in the cells' new
     * order is what keeps this flat list agreeing with [Folders] without either
     * of them knowing about the other.
     *
     * The whole arrangement is recorded, not just the cell that moved: once one
     * app has been placed by hand, the alphabetical order the rest were in is
     * a decision too, and a later install must not be able to shuffle them.
     *
     * An app that is not installed drops out of the arrangement here, because
     * the drawer holds every installed app and there is nothing to anchor it
     * between. Reinstalling puts it at the end, the same as any new app.
     */
    fun move(cells: List<DrawerItem>, from: Int, to: Int): AppOrder {
        if (from !in cells.indices || to !in cells.indices || from == to) return this
        val moved = cells.toMutableList()
        moved.add(to, moved.removeAt(from))
        return AppOrder(moved.flatMap { it.packageNames })
    }
}
