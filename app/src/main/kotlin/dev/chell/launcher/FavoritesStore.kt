package dev.chell.launcher

import android.content.Context
import dev.chell.launcher.core.Favorites

/**
 * Persists [Favorites] as an ordered list of package names.
 *
 * Stored as one delimited string rather than a string set, because a set has
 * no order and the order is the whole point.
 */
class FavoritesStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): Favorites {
        val stored = preferences.getString(KEY_PACKAGES, null).orEmpty()
        return Favorites(stored.split(DELIMITER).filter { it.isNotEmpty() })
    }

    fun save(favorites: Favorites) {
        preferences.edit()
            .putString(KEY_PACKAGES, favorites.packageNames.joinToString(DELIMITER))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "chell"
        const val KEY_PACKAGES = "favorites"

        /** Not a legal character in a package name, so it cannot appear in one. */
        const val DELIMITER = "\n"
    }
}
