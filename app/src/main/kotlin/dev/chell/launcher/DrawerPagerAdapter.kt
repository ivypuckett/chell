package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo

/** Presents each [AppDrawer] page as a horizontally swipeable grid. */
class DrawerPagerAdapter(
    val drawer: AppDrawer,
    private val columns: Int,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, android.view.View) -> Unit,
) : RecyclerView.Adapter<DrawerPagerAdapter.PageViewHolder>() {

    class PageViewHolder(val grid: RecyclerView) : RecyclerView.ViewHolder(grid)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val grid = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_apps, parent, false) as RecyclerView
        grid.layoutManager = GridLayoutManager(parent.context, columns)
        return PageViewHolder(grid)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.grid.adapter =
            AppGridAdapter(drawer.page(position), iconFor, onClick, onLongClick)
    }

    override fun getItemCount(): Int = drawer.pageCount
}
