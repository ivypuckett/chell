package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.DrawerItem

/**
 * Binds the cells of a single drawer page into a grid.
 *
 * A cell is a [DrawerItem], not an [AppInfo]: a folder occupies one cell and
 * has to bind, be pressed and be dragged like any other. There is no second
 * view type -- a folder is the same layout with a composed icon -- so the
 * dragger's indexes stay indexes into one list.
 */
class AppGridAdapter(
    items: List<DrawerItem>,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (DrawerItem, View) -> Unit,
    private val onLongClick: (DrawerItem, View) -> Unit,
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    /** Mutable so a drag can reorder the cells in place. */
    private val items = items.toMutableList()

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val label: TextView = view.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is DrawerItem.App -> {
                holder.label.text = item.app.label
                holder.icon.setImageDrawable(iconFor(item.app.packageName))
            }
            is DrawerItem.Folder -> {
                holder.label.setText(R.string.folder)
                holder.icon.setImageDrawable(
                    FolderIcon.of(holder.itemView.context, item.packageNames.mapNotNull(iconFor)),
                )
            }
        }
        holder.itemView.setOnClickListener { view -> onClick(item, view) }
        holder.itemView.setOnLongClickListener { view ->
            onLongClick(item, view)
            // Consume it, or the click listener fires on release as well.
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun itemAt(position: Int): DrawerItem = items[position]

    /** The cells in the order they are in, which a drag rewrites as it goes. */
    fun cells(): List<DrawerItem> = items.toList()

    /** The order the cells are in, flattened. A folder contributes its members. */
    fun order(): List<String> = items.flatMap { it.packageNames }

    /** Moves a cell during a drag. The caller persists the result. */
    fun move(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
    }
}
