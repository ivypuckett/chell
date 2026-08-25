package dev.chell.launcher

import android.content.Context

/**
 * Persists an ordered list of package names.
 *
 * Stored as one delimited string rather than a string set, because a set has
 * no order and the order is the whole point -- of the favourites row and of
 * the drawer arrangement alike.
 */
class PackageListStore(context: Context, private val key: String) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val stored = preferences.getString(key, null).orEmpty()
        return stored.split(DELIMITER).filter { it.isNotEmpty() }
    }

    fun save(packageNames: List<String>) {
        preferences.edit().putString(key, packageNames.joinToString(DELIMITER)).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "chell"

        /** Not a legal character in a package name, so it cannot appear in one. */
        const val DELIMITER = "\n"
    }
}
