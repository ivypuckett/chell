package dev.chell.launcher.core

/**
 * One cell of the drawer: a single app, or a folder standing for several.
 *
 * The drawer deals in cells rather than apps because a folder occupies one cell
 * and travels as one thing when it is dragged. [packageNames] is what makes
 * that work without the grid knowing which kind it is holding: a cell's move is
 * recorded by writing out the packages of every cell in their new order.
 */
sealed interface DrawerItem {

    /** One package for an app, all of its members for a folder, in order. */
    val packageNames: List<String>

    data class App(val app: AppInfo) : DrawerItem {
        override val packageNames: List<String> get() = listOf(app.packageName)
    }

    /**
     * A folder has an id and members, and deliberately no name. Naming is a
     * separate concern: a folder is told apart by the icons in it, and renaming
     * is worth adding only if it is actually asked for.
     */
    data class Folder(val id: String, val apps: List<AppInfo>) : DrawerItem {
        override val packageNames: List<String> get() = apps.map { it.packageName }
    }
}
