package dev.chell.launcher.core

/**
 * The pinned apps, newest first.
 *
 * Pinning puts an app at the front so a just-pinned app is always inside the
 * visible window, and nothing has to be rejected when the row is full: the row
 * is a view onto the front of this list, not the whole of it.
 *
 * Instances are immutable; [pin] and [unpin] return a new one.
 */
class Favorites(packageNames: List<String> = emptyList()) {

    val packageNames: List<String> = packageNames.distinct()

    fun pin(packageName: String): Favorites =
        Favorites(listOf(packageName) + packageNames)

    fun unpin(packageName: String): Favorites =
        Favorites(packageNames.filterNot { it == packageName })

    fun isPinned(packageName: String): Boolean = packageName in packageNames

    /**
     * The same pinned apps, with [visibleOrder] rearranged into the slots those
     * packages already occupy.
     *
     * The row only ever shows a window onto the front of the list, and
     * [resolve] skips packages that are not installed, so a position in the row
     * is not a position in this list. Rewriting slots rather than moving
     * entries means a drag reorders exactly what the user can see: anything
     * uninstalled, or off the end of the row, keeps the place it had.
     *
     * Packages that are not pinned are ignored.
     */
    fun reorder(visibleOrder: List<String>): Favorites {
        val moving = visibleOrder.filter { it in packageNames }.distinct()
        val slots = packageNames.indices.filter { packageNames[it] in moving }
        if (slots.size != moving.size) return this
        val reordered = packageNames.toMutableList()
        slots.forEachIndexed { i, slot -> reordered[slot] = moving[i] }
        return Favorites(reordered)
    }

    /**
     * The pinned apps as [AppInfo], in pinned order, at most [limit] of them.
     *
     * Packages that are no longer installed are skipped rather than dropped
     * from the list: an app that comes back keeps its place.
     */
    fun resolve(apps: List<AppInfo>, limit: Int = Int.MAX_VALUE): List<AppInfo> {
        if (limit <= 0) return emptyList()
        val byPackage = apps.associateBy { it.packageName }
        return packageNames.mapNotNull { byPackage[it] }.take(limit)
    }
}
