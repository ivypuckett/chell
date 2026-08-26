package dev.chell.launcher

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.AppInfo
import kotlin.math.abs

/**
 * Long-press dragging for a grid of app cells.
 *
 * A long press already meant "open the action menu", so this starts the drag
 * by hand from that same press ([beginDrag]) with the helper's own detector
 * off: two detectors on one gesture would race over the same ~500ms. A press
 * that ends without travelling still means the menu, reported through
 * [onPress].
 *
 * [onEdgeHold] is optional and reports that a dragged cell has been held
 * against one of the four edges, which is how a drag leaves the container it
 * started in: sideways to the neighbouring page, or up and down between the
 * drawer and the favourites row. What an edge means is the caller's business.
 */
class GridDragger(
    private val view: RecyclerView,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onPress: (AppInfo, View) -> Unit,
    private val onEdgeHold: ((index: Int, edge: Edge) -> Unit)? = null,
    directions: Int = SIDEWAYS,
) {

    /**
     * Whether the finger has travelled far enough for the gesture to count as
     * a drag. Not the same as "something was swapped": a drag that ends
     * between two cells swaps nothing, and popping the action menu open at the
     * end of it would be a surprise.
     */
    private var dragged = false

    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

    private val handler = Handler(Looper.getMainLooper())

    /** The edge a cell is currently being held against, if any. */
    private var pendingEdge: Edge? = null

    private val callback = object : ItemTouchHelper.SimpleCallback(
        directions,
        0, // A grid of apps is not a list to swipe things out of.
    ) {
        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recycler: RecyclerView,
            holder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            move(holder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }

        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onChildDraw(
            canvas: Canvas,
            recycler: RecyclerView,
            holder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            if (isCurrentlyActive && (abs(dX) > touchSlop || abs(dY) > touchSlop)) dragged = true
            if (isCurrentlyActive) watchEdges(holder, dX, dY)
            super.onChildDraw(canvas, recycler, holder, dX, dY, actionState, isCurrentlyActive)
        }

        /**
         * A press that ends where it started was not a drag at all, so it means
         * what a long press has always meant here: open the action menu.
         */
        override fun clearView(recycler: RecyclerView, holder: RecyclerView.ViewHolder) {
            super.clearView(recycler, holder)
            cancelEdgeHold()
            if (dragged) return
            val adapter = recycler.adapter as? AppGridAdapter ?: return
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPress(adapter.appAt(position), holder.itemView)
            }
        }
    }

    private val touchHelper = ItemTouchHelper(callback).apply { attachToRecyclerView(view) }

    /**
     * Moves a cell and reports it. Cells move as the drag goes rather than on
     * drop, so a drag interrupted by the launcher going away is not lost.
     */
    fun move(from: Int, to: Int) {
        val adapter = view.adapter as? AppGridAdapter ?: return
        if (from == to || from !in 0 until adapter.itemCount || to !in 0 until adapter.itemCount) {
            return
        }
        adapter.move(from, to)
        onMove(from, to)
    }

    /** Turns the long press on a cell into a drag rather than a menu. */
    fun beginDrag(app: AppInfo, itemView: View) {
        val holder = view.findContainingViewHolder(itemView)
        if (holder == null) {
            onPress(app, itemView)
            return
        }
        dragged = false
        touchHelper.startDrag(holder)
    }

    /**
     * Fires [onEdgeHold] once a cell has been held against an edge long enough
     * to read as intent rather than as overshoot on the way to the last cell.
     */
    private fun watchEdges(holder: RecyclerView.ViewHolder, dX: Float, dY: Float) {
        val report = onEdgeHold ?: return
        // Only once the gesture is a drag. A cell that starts life against an
        // edge -- the first or last on the page -- is already inside the
        // margin, so picking it up and pausing would otherwise carry it off
        // the page without the finger having moved at all.
        if (!dragged) return
        // Only the axis the finger is actually travelling along. Every cell in
        // the bottom row sits inside the bottom margin for the whole of a
        // sideways drag, and reading both axes at once would hand it to the
        // favourites row instead of moving it.
        val edge =
            if (abs(dX) >= abs(dY)) horizontalEdge(holder, dX) else verticalEdge(holder, dY)
        if (edge == pendingEdge) return
        cancelEdgeHold()
        pendingEdge = edge
        if (edge == null) return
        // The cell keeps moving while the timer runs, so its position is read
        // when the timer fires, not when it is scheduled. Reading it here
        // carried whichever app had since taken the cell's place.
        handler.postDelayed(
            {
                val index = holder.bindingAdapterPosition
                if (index != RecyclerView.NO_POSITION) report(index, edge)
            },
            EDGE_HOLD_MILLIS,
        )
    }

    private fun horizontalEdge(holder: RecyclerView.ViewHolder, dX: Float): Edge? {
        val margin = holder.itemView.width / 2f
        return when {
            holder.itemView.left + dX < margin -> Edge.LEFT
            holder.itemView.right + dX > view.width - margin -> Edge.RIGHT
            else -> null
        }
    }

    private fun verticalEdge(holder: RecyclerView.ViewHolder, dY: Float): Edge? {
        val margin = holder.itemView.height / 2f
        return when {
            holder.itemView.top + dY < margin -> Edge.TOP
            holder.itemView.bottom + dY > view.height - margin -> Edge.BOTTOM
            else -> null
        }
    }

    private fun cancelEdgeHold() {
        handler.removeCallbacksAndMessages(null)
        pendingEdge = null
    }

    /** The side of its container a dragged cell is being held against. */
    enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    companion object {
        /** A single row: there is nowhere above or below to move a cell to. */
        const val SIDEWAYS = ItemTouchHelper.START or ItemTouchHelper.END

        /**
         * A row a cell can also be lifted up out of. There is still nowhere
         * above to move it to; allowing the direction is what makes
         * [ItemTouchHelper] report the upward travel at all, and a lift that
         * ends in nothing simply drops the cell back where it was.
         */
        const val SIDEWAYS_OR_OUT = SIDEWAYS or ItemTouchHelper.UP

        /** A grid, where a cell moves in both axes. */
        const val GRID = SIDEWAYS or ItemTouchHelper.UP or ItemTouchHelper.DOWN

        /** Long enough that reaching for the last cell does not flip the page. */
        private const val EDGE_HOLD_MILLIS = 600L
    }
}
