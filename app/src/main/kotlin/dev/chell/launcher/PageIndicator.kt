package dev.chell.launcher

import android.view.View
import android.widget.LinearLayout

/**
 * The row of dots under the drawer, one per page.
 *
 * Owns the dot views so the activity only has to say how many pages there are
 * and which one is showing.
 */
class PageIndicator(private val container: LinearLayout) {

    private val size =
        container.resources.getDimensionPixelSize(R.dimen.page_dot_size)
    private val spacing =
        container.resources.getDimensionPixelSize(R.dimen.page_dot_spacing)

    /** Rebuilds the dots. A single page needs no indicator at all. */
    fun setPageCount(pageCount: Int) {
        container.removeAllViews()
        if (pageCount <= 1) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        repeat(pageCount) {
            val dot = View(container.context)
            dot.setBackgroundResource(R.drawable.page_dot)
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = spacing
                marginEnd = spacing
            }
            container.addView(dot)
        }
    }

    fun markCurrent(position: Int) {
        for (i in 0 until container.childCount) {
            container.getChildAt(i).isSelected = i == position
        }
    }
}
