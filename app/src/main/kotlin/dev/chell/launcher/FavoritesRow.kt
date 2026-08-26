package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.DrawerItem
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

    // The row is one page wide, so its sideways edges lead nowhere; the top
    // is the way out of it.
    private val dragger = GridDragger(
        view = view,
        onMove = ::saveOrder,
        onPress = { item, anchor -> (item as? DrawerItem.App)?.let { onLongClick(it.app, anchor) } },
        onEdgeHold = ::liftOut,
        directions = GridDragger.SIDEWAYS_OR_OUT,
    )

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
        // The row holds apps only: a folder there would mean resolving package
        // names to something that is not one, which is the whole modelling
        // question again. They are wrapped as cells because that is what the
        // grid adapter binds.
        view.adapter = AppGridAdapter(
            items = pinned.map { DrawerItem.App(it) },
            iconFor = iconFor,
            onClick = { item, _ -> (item as? DrawerItem.App)?.let { onClick(it.app) } },
            onLongClick = dragger::beginDrag,
        )
    }

    fun pin(packageName: String) = update(favorites.pin(packageName))

    fun unpin(packageName: String) = update(favorites.unpin(packageName))

    fun move(from: Int, to: Int) = dragger.move(from, to)

    /**
     * Unpins the cell held above the row: dragging an app out of the row is the
     * gesture for taking it out, the counterpart of dropping one in.
     */
    private fun liftOut(index: Int, edge: GridDragger.Edge) {
        if (edge != GridDragger.Edge.TOP) return
        val adapter = view.adapter as? AppGridAdapter ?: return
        if (index !in 0 until adapter.itemCount) return
        unpin(adapter.itemAt(index).packageNames.first())
    }

    /** Records the order the cells are now in, after one of them has moved. */
    private fun saveOrder(from: Int, to: Int) {
        val adapter = view.adapter as? AppGridAdapter ?: return
        favorites = favorites.reorder(adapter.order())
        store.save(favorites)
    }

    private fun update(updated: Favorites) {
        favorites = updated
        store.save(favorites)
        show(apps, columns)
    }
}
