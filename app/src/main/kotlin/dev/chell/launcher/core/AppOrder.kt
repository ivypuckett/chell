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
     * Moves the app at [from] to [to], both indexes into [apply]'s result.
     *
     * The whole arrangement is recorded, not just the app that moved: once one
     * app has been placed by hand, the alphabetical order the rest were in is
     * a decision too, and a later install must not be able to shuffle them.
     *
     * An app that is not installed drops out of the arrangement here, because
     * the drawer holds every installed app and there is nothing to anchor it
     * between. Reinstalling puts it at the end, the same as any new app.
     */
    fun move(apps: List<AppInfo>, from: Int, to: Int): AppOrder {
        val arranged = apply(apps).map { it.packageName }.toMutableList()
        if (from !in arranged.indices || to !in arranged.indices || from == to) return this
        arranged.add(to, arranged.removeAt(from))
        return AppOrder(arranged)
    }
}
