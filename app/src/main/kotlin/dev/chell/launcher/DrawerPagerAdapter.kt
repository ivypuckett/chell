package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo

/**
 * Presents each [AppDrawer] page as a horizontally swipeable grid.
 *
 * Each page is its own [RecyclerView] with its own dragger, so the positions a
 * drag reports are positions on that page. They are translated to positions in
 * the whole drawer here, because the order the launcher stores is one packed
 * list and knows nothing about pages.
 *
 * [onMove] being null is what makes the drawer read-only: search results are
 * ranked by relevance, and dragging within a ranking would mean nothing.
 */
class DrawerPagerAdapter(
    val drawer: AppDrawer,
    private val columns: Int,
    private val pageSize: Int,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit,
    private val onMove: ((from: Int, to: Int) -> Unit)? = null,
    private val onEdgeHold: ((index: Int, edge: GridDragger.Edge) -> Unit)? = null,
) : RecyclerView.Adapter<DrawerPagerAdapter.PageViewHolder>() {

    class PageViewHolder(val grid: RecyclerView) : RecyclerView.ViewHolder(grid) {
        var dragger: GridDragger? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val grid = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_apps, parent, false) as RecyclerView
        grid.layoutManager = GridLayoutManager(parent.context, columns)
        val holder = PageViewHolder(grid)
        if (onMove != null) {
            holder.dragger = GridDragger(
                view = grid,
                onMove = { from, to -> onMove.invoke(holder.global(from), holder.global(to)) },
                onPress = onLongClick,
                onEdgeHold = onEdgeHold?.let { report ->
                    { index, edge -> report(holder.global(index), edge) }
                },
                directions = GridDragger.GRID,
            )
        }
        return holder
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val press = holder.dragger?.let { it::beginDrag } ?: onLongClick
        holder.grid.adapter =
            AppGridAdapter(drawer.page(position), iconFor, onClick, press)
    }

    override fun getItemCount(): Int = drawer.pageCount

    /** A position on this page as a position in the whole drawer. */
    private fun PageViewHolder.global(position: Int): Int =
        bindingAdapterPosition * pageSize + position
}
