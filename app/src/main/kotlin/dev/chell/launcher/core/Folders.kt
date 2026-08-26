package dev.chell.launcher.core

/**
 * Which apps have been grouped into folders.
 *
 * A projection over [AppOrder], not a replacement for it. The order stays one
 * flat list of package names, because the grid reflows on rotation and on the
 * keyboard opening and anything tied to a page and a cell would not survive
 * that. Membership is held here, keyed by package, and [arrange] derives the
 * cells by collapsing each group at the position of its first member in that
 * flat list.
 *
 * The price of two structures is that they have to agree about a package. What
 * keeps them agreeing is that this one never invents a position: a folder sits
 * where its first member sits, and a member dragged away simply stops being a
 * member. Nothing here has to be rewritten when the order changes.
 *
 * A folder always has at least two members: the constructor drops assignments
 * that would leave a folder with fewer, so "a folder with one app left
 * dissolves back into that app" is a property of the type rather than a case
 * every caller has to remember.
 *
 * Instances are immutable; [combine] and [remove] return a new one.
 */
class Folders(assignments: Map<String, String> = emptyMap()) {

    /** Package name to the id of the folder holding it. */
    val assignments: Map<String, String> =
        assignments.filterValues { id -> assignments.count { it.value == id } > 1 }

    fun folderOf(packageName: String): String? = assignments[packageName]

    /** The packages assigned to [folderId]. [arrange] is what puts them in order. */
    fun members(folderId: String): Set<String> =
        assignments.filterValues { it == folderId }.keys

    /**
     * [apps] as the cells the drawer shows: every ungrouped app where it
     * already was, and one folder cell per group at the position of its first
     * member.
     *
     * A group with only one of its members installed is shown as that app
     * rather than as a folder of one. The assignment is left alone, so a folder
     * whose member is uninstalled and then reinstalled comes back -- the same
     * bargain [Favorites.resolve] makes for a pinned app that goes away.
     */
    fun arrange(apps: List<AppInfo>): List<DrawerItem> {
        if (assignments.isEmpty()) return apps.map { DrawerItem.App(it) }

        val grouped = apps
            .filter { it.packageName in assignments }
            .groupBy { assignments.getValue(it.packageName) }

        val emitted = mutableSetOf<String>()
        return apps.mapNotNull { app ->
            val id = assignments[app.packageName] ?: return@mapNotNull DrawerItem.App(app)
            if (!emitted.add(id)) return@mapNotNull null
            val members = grouped.getValue(id)
            if (members.size > 1) DrawerItem.Folder(id, members) else DrawerItem.App(members.single())
        }
    }

    /**
     * Puts [source] in a folder with [target], which is what dropping one cell
     * onto another means.
     *
     * Dropping onto an app that is already in a folder joins that folder rather
     * than making a second one, so repeatedly dropping apps onto the same cell
     * fills it. A source that was in another folder leaves it, and that folder
     * dissolves if it is left with one member.
     */
    fun combine(source: String, target: String): Folders {
        if (source == target) return this
        val existing = assignments[target]
        if (existing != null && existing == assignments[source]) return this
        val folderId = existing ?: nextId()
        return Folders(assignments + (target to folderId) + (source to folderId))
    }

    /** Takes [packageName] out of whatever folder holds it. */
    fun remove(packageName: String): Folders = Folders(assignments - packageName)

    /**
     * An id no existing folder uses. Numbered rather than derived from the
     * members, because members come and go and an id must not.
     */
    private fun nextId(): String {
        val used = assignments.values.mapNotNull { it.removePrefix(PREFIX).toIntOrNull() }
        return PREFIX + ((used.maxOrNull() ?: 0) + 1)
    }

    companion object {
        private const val PREFIX = "f"
    }
}
