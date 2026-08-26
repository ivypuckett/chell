package dev.chell.launcher

import android.content.Context
import dev.chell.launcher.core.Folders

/**
 * Persists [Folders] as one "folder id, package name" entry per member.
 *
 * A flat list of entries rather than anything nested, so it goes through the
 * same [PackageListStore] the order and the favourites use. The entry order is
 * not meaningful here -- where a folder's members sit is the drawer order's
 * business, and this only records which cell they share.
 */
class FolderStore(context: Context) {

    private val store = PackageListStore(context, KEY_FOLDERS)

    fun load(): Folders = Folders(
        store.load().mapNotNull { entry ->
            val split = entry.indexOf(SEPARATOR)
            if (split <= 0 || split == entry.lastIndex) null
            else entry.substring(split + 1) to entry.substring(0, split)
        }.toMap(),
    )

    fun save(folders: Folders) = store.save(
        folders.assignments.map { (packageName, id) -> "$id$SEPARATOR$packageName" },
    )

    private companion object {
        const val KEY_FOLDERS = "folders"

        /** Not legal in a package name or in a folder id, so it splits cleanly. */
        const val SEPARATOR = ' '
    }
}
