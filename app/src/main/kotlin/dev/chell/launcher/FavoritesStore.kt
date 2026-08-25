package dev.chell.launcher

import android.content.Context
import dev.chell.launcher.core.Favorites

/** Persists [Favorites] as an ordered list of package names. */
class FavoritesStore(context: Context) {

    private val store = PackageListStore(context, KEY_PACKAGES)

    fun load(): Favorites = Favorites(store.load())

    fun save(favorites: Favorites) = store.save(favorites.packageNames)

    private companion object {
        const val KEY_PACKAGES = "favorites"
    }
}
