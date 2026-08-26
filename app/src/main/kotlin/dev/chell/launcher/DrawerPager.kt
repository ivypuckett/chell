package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.AppOrder
import dev.chell.launcher.core.AppSearch
import dev.chell.launcher.core.GridMetrics

/**
 * The paged app grid: what it is showing, the arrangement a drag rewrites, and
 * where a cell held against an edge goes.
 *
 * Split out of [MainActivity], which was holding this, the search field, the
 * favourites row and the action menu at once. The activity keeps the things a
 * drag ends up talking to -- pinning, launching, the menu -- and hands them in
 * as callbacks, so this class never needs to know what else is on screen.
 */
class DrawerPager(
    private val pager: ViewPager2,
    private val emptyMessage: TextView,
    private val pageIndicator: PageIndicator,
    private val orderStore: PackageListStore,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit,
    /** A cell held against the bottom edge has been handed out of the drawer. */
    private val onDropOut: (AppInfo) -> Unit,
    /**
     * Re-runs the last [show]. Supplied by the owner rather than done here,
     * because re-rendering needs the app list and the query, which live
     * outside this class.
     */
    private val rerender: () -> Unit,
) {

    /** Exactly what the grid is showing, which is what a drag's indexes mean. */
    var shownApps: List<AppInfo> = emptyList()
        private set

    private var appOrder: AppOrder = AppOrder(orderStore.load())

    /** The grid the pager is currently laid out for; null until the first show. */
    var currentMetrics: GridMetrics? = null
        private set

    init {
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = pageIndicator.markCurrent(position)
        })
    }

    /**
     * Renders [allApps] filtered by [query]. [keepPage] holds the reader's
     * place across a reload; a changed query resets to page 0.
     */
    fun show(
        allApps: List<AppInfo>,
        query: String,
        metrics: GridMetrics,
        keepPage: Boolean = true,
    ) {
        currentMetrics = metrics

        // Search results are ranked by relevance, so the arrangement only
        // applies to the unfiltered drawer -- and only that is draggable.
        val searching = query.isNotBlank()
        val matched = AppSearch.search(allApps, query)
        val apps = if (searching) matched else appOrder.apply(matched)
        shownApps = apps

        if (apps.isEmpty()) {
            pager.visibility = View.GONE
            emptyMessage.setText(if (allApps.isEmpty()) R.string.no_apps else R.string.no_matches)
            emptyMessage.visibility = View.VISIBLE
            pageIndicator.setPageCount(0)
            return
        }
        pager.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        val drawer = AppDrawer(apps, pageSize = metrics.pageSize)

        val targetPage = if (keepPage) pager.currentItem.coerceAtMost(drawer.pageCount - 1) else 0
        pager.adapter = DrawerPagerAdapter(
            drawer = drawer,
            columns = metrics.columns,
            pageSize = metrics.pageSize,
            iconFor = iconFor,
            onClick = onClick,
            onLongClick = onLongClick,
            onMove = if (searching) null else ::moveApp,
            onEdgeHold = if (searching) null else ::leaveDrawer,
        )
        pager.setCurrentItem(targetPage, false)

        // setCurrentItem does not fire onPageSelected when the page is already
        // the current one, so the dots are built for the page just chosen.
        pageIndicator.setPageCount(drawer.pageCount)
        pageIndicator.markCurrent(targetPage)
    }

    /**
     * Records a drag in the drawer. Deliberately does not re-render: the cell
     * has already moved on screen, and rebuilding the pager under a finger
     * that is still down would cancel the drag it is reporting.
     */
    fun moveApp(from: Int, to: Int) {
        appOrder = appOrder.move(shownApps, from, to)
        orderStore.save(appOrder.packageNames)
        shownApps = appOrder.apply(shownApps)
    }

    /**
     * What holding a drawer cell against an edge means: sideways carries it to
     * the neighbouring page, downwards hands it out of the drawer. There is
     * nothing above the drawer to hand an app to.
     */
    fun leaveDrawer(index: Int, edge: GridDragger.Edge) {
        val app = shownApps.getOrNull(index) ?: return
        when (edge) {
            GridDragger.Edge.LEFT -> carryToPage(index, -1)
            GridDragger.Edge.RIGHT -> carryToPage(index, 1)
            GridDragger.Edge.BOTTOM -> onDropOut(app)
            GridDragger.Edge.TOP -> Unit
        }
    }

    /**
     * Sends the app held against an edge to the neighbouring page.
     *
     * The drag ends here rather than continuing on the new page: each page is
     * its own RecyclerView, and a drag cannot be handed from one to another.
     * The app lands at the near edge of the page it was carried to, which is
     * where it was headed.
     */
    fun carryToPage(index: Int, direction: Int) {
        val pageSize = currentMetrics?.pageSize ?: return
        val target = pager.currentItem + direction
        if (target < 0 || target >= (pager.adapter?.itemCount ?: 0)) return

        val landing = if (direction > 0) target * pageSize else target * pageSize + pageSize - 1
        moveApp(index, landing.coerceIn(0, shownApps.size - 1))
        rerender()
        pager.setCurrentItem(target, true)
    }
}
