package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.Favorites

/**
 * The row of pinned apps under the drawer.
 *
 * Owns the pinned set and its persistence, so pinning from anywhere re-renders
 * the row without the caller having to know what it is currently showing --
 * the last apps and column count are remembered from the previous [show].
 */
class FavoritesRow(
    private val view: RecyclerView,
    private val store: FavoritesStore,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit,
) {

    private var favorites: Favorites = store.load()

    private var apps: List<AppInfo> = emptyList()
    private var columns: Int = 0

    fun isPinned(packageName: String): Boolean = favorites.isPinned(packageName)

    /**
     * Fills the row with as many pinned apps as fit across it. The row is a
     * window onto the front of the list, so pinning never has to be refused;
     * an empty row is hidden rather than left as a gap.
     */
    fun show(apps: List<AppInfo>, columns: Int) {
        this.apps = apps
        this.columns = columns

        val pinned = favorites.resolve(apps, limit = columns)
        if (pinned.isEmpty()) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE
        view.layoutManager = GridLayoutManager(view.context, columns)
        view.adapter = AppGridAdapter(pinned, iconFor, onClick, onLongClick)
    }

    fun pin(packageName: String) = update(favorites.pin(packageName))

    fun unpin(packageName: String) = update(favorites.unpin(packageName))

    private fun update(updated: Favorites) {
        favorites = updated
        store.save(favorites)
        show(apps, columns)
    }
}
