package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.DrawerItem

/**
 * An open folder: its members in a small grid, anchored to the cell.
 *
 * A popup rather than an expansion of the grid itself. Expanding inline looks
 * better but re-opens the reflow question the flat order exists to avoid,
 * whereas a popup is the same shape as the long-press action menu, which is
 * already anchored to a cell.
 *
 * Dragging a cell against the top of the popup takes it out of the folder,
 * which is the same gesture -- and the same [GridDragger] hold -- that lifts an
 * app out of the favourites row.
 */
class FolderPopup(
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onRemove: (String) -> Unit,
) {

    private var window: PopupWindow? = null

    /** The folder currently open, so a change can be re-read and re-shown. */
    private var openId: String? = null

    fun show(folder: DrawerItem.Folder, anchor: View) {
        dismiss()
        val context = anchor.context
        val grid = LayoutInflater.from(context)
            .inflate(R.layout.folder_popup, anchor.parent as? ViewGroup, false) as RecyclerView

        val columns = minOf(
            folder.apps.size,
            context.resources.getInteger(R.integer.folder_popup_columns),
        ).coerceAtLeast(1)
        grid.layoutManager = GridLayoutManager(context, columns)

        // The row is one popup wide, so its sideways edges lead nowhere; the
        // top is the way out of it.
        val dragger = GridDragger(
            view = grid,
            // Reordering inside a folder is not persisted: where a member sits
            // is the drawer order's business, and nothing has asked to arrange
            // the inside of a folder yet.
            onMove = { _, _ -> },
            onPress = { _, _ -> },
            onEdgeHold = { index, edge -> liftOut(grid, index, edge) },
            directions = GridDragger.SIDEWAYS_OR_OUT,
        )
        grid.adapter = AppGridAdapter(
            items = folder.apps.map { DrawerItem.App(it) },
            iconFor = iconFor,
            onClick = { item, _ -> clickMember(item) },
            onLongClick = dragger::beginDrag,
        )

        openId = folder.id
        window = PopupWindow(grid, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
            .apply {
                // Dismissing on an outside tap is what makes the popup feel like
                // a layer over the drawer rather than a screen of its own.
                setOnDismissListener { openId = null }
                showAsDropDown(anchor, 0, 0, Gravity.CENTER_HORIZONTAL)
            }
    }

    /** True while [id] is the folder on screen, so a change can re-open it. */
    fun isShowing(id: String): Boolean = openId == id

    fun dismiss() {
        window?.dismiss()
        window = null
        openId = null
    }

    private fun clickMember(item: DrawerItem) {
        val app = (item as? DrawerItem.App)?.app ?: return
        dismiss()
        onClick(app)
    }

    private fun liftOut(grid: RecyclerView, index: Int, edge: GridDragger.Edge) {
        if (edge != GridDragger.Edge.TOP) return
        val adapter = grid.adapter as? AppGridAdapter ?: return
        if (index !in 0 until adapter.itemCount) return
        val packageName = adapter.itemAt(index).packageNames.firstOrNull() ?: return
        dismiss()
        onRemove(packageName)
    }
}
