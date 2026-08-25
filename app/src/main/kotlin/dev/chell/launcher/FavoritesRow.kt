package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
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

    /**
     * Whether the finger has travelled far enough for the gesture to count as
     * a drag. Not the same as "something was swapped": a drag that ends
     * between two cells swaps nothing, and popping the action menu open at the
     * end of it would be a surprise.
     */
    private var dragged = false

    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

    private val dragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.START or ItemTouchHelper.END,
        0, // The row is not a list to swipe things out of.
    ) {
        // A long press is how the action menu opens, so the drag is started by
        // hand from that same press rather than by the helper's own detector.
        // Two detectors racing over one gesture is what this avoids.
        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recycler: RecyclerView,
            holder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            dragged = true
            move(holder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }

        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onChildDraw(
            canvas: android.graphics.Canvas,
            recycler: RecyclerView,
            holder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            if (isCurrentlyActive && kotlin.math.abs(dX) > touchSlop) dragged = true
            super.onChildDraw(canvas, recycler, holder, dX, dY, actionState, isCurrentlyActive)
        }

        /**
         * A press that ends where it started was not a drag at all, so it means
         * what a long press has always meant here: open the action menu.
         */
        override fun clearView(recycler: RecyclerView, holder: RecyclerView.ViewHolder) {
            super.clearView(recycler, holder)
            if (dragged) return
            val adapter = recycler.adapter as? AppGridAdapter ?: return
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onLongClick(adapter.appAt(position), holder.itemView)
            }
        }
    }

    private val touchHelper = ItemTouchHelper(dragCallback).apply { attachToRecyclerView(view) }

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
        view.adapter = AppGridAdapter(pinned, iconFor, onClick, ::beginDrag)
    }

    fun pin(packageName: String) = update(favorites.pin(packageName))

    fun unpin(packageName: String) = update(favorites.unpin(packageName))

    /**
     * Moves a cell and saves the new order. Called for each step of a drag, so
     * a drag interrupted by the launcher going away is not lost.
     */
    fun move(from: Int, to: Int) {
        val adapter = view.adapter as? AppGridAdapter ?: return
        if (from == to || from !in 0 until adapter.itemCount || to !in 0 until adapter.itemCount) {
            return
        }
        adapter.move(from, to)
        favorites = favorites.reorder(adapter.order())
        store.save(favorites)
    }

    /** Turns the long press on a cell into a drag rather than a menu. */
    private fun beginDrag(app: AppInfo, itemView: View) {
        val holder = view.findContainingViewHolder(itemView)
        if (holder == null) {
            onLongClick(app, itemView)
            return
        }
        dragged = false
        touchHelper.startDrag(holder)
    }

    private fun update(updated: Favorites) {
        favorites = updated
        store.save(favorites)
        show(apps, columns)
    }
}
