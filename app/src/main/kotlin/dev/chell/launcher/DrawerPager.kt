package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.AppOrder
import dev.chell.launcher.core.AppSearch
import dev.chell.launcher.core.DrawerItem
import dev.chell.launcher.core.Folders
import dev.chell.launcher.core.GridMetrics

/**
 * The paged app grid: what it is showing, the arrangement a drag rewrites, and
 * where a cell held against an edge or over another cell goes.
 *
 * Split out of [MainActivity], which was holding this, the search field, the
 * favourites row and the action menu at once. The activity keeps the things a
 * drag ends up talking to -- pinning, launching, the menu, opening a folder --
 * and hands them in as callbacks, so this class never needs to know what else
 * is on screen.
 *
 * The order and the folders are two structures over the same packages. Neither
 * knows about the other: [AppOrder] is the flat list of positions, [Folders]
 * says which packages share a cell, and [DrawerItem] is what falls out when the
 * second is applied to the first.
 */
class DrawerPager(
    private val pager: ViewPager2,
    private val emptyMessage: TextView,
    private val pageIndicator: PageIndicator,
    private val orderStore: PackageListStore,
    private val folderStore: FolderStore,
    private val iconFor: (String) -> Drawable?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit,
    private val onFolderClick: (DrawerItem.Folder, View) -> Unit,
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
    var shownItems: List<DrawerItem> = emptyList()
        private set

    /** The apps behind [shownItems], flattened; folders contribute their members. */
    private var shownApps: List<AppInfo> = emptyList()

    private var appOrder: AppOrder = AppOrder(orderStore.load())
    private var folders: Folders = folderStore.load()

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
        // A query lists apps, never folders: someone looking for an app by name
        // should not have to remember which folder they filed it in.
        val searching = query.isNotBlank()
        val matched = AppSearch.search(allApps, query)
        val apps = if (searching) matched else appOrder.apply(matched)
        shownApps = apps
        val items = if (searching) apps.map { DrawerItem.App(it) } else folders.arrange(apps)
        shownItems = items

        if (items.isEmpty()) {
            pager.visibility = View.GONE
            emptyMessage.setText(if (allApps.isEmpty()) R.string.no_apps else R.string.no_matches)
            emptyMessage.visibility = View.VISIBLE
            pageIndicator.setPageCount(0)
            return
        }
        pager.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        val drawer = AppDrawer(items, pageSize = metrics.pageSize)

        val targetPage = if (keepPage) pager.currentItem.coerceAtMost(drawer.pageCount - 1) else 0
        pager.adapter = DrawerPagerAdapter(
            drawer = drawer,
            columns = metrics.columns,
            pageSize = metrics.pageSize,
            iconFor = iconFor,
            onClick = ::openCell,
            onLongClick = ::pressCell,
            onMove = if (searching) null else ::moveApp,
            onEdgeHold = if (searching) null else ::leaveDrawer,
            onCombine = if (searching) null else ::combineCells,
        )
        pager.setCurrentItem(targetPage, false)

        // setCurrentItem does not fire onPageSelected when the page is already
        // the current one, so the dots are built for the page just chosen.
        pageIndicator.setPageCount(drawer.pageCount)
        pageIndicator.markCurrent(targetPage)
    }

    /** Tapping a cell launches an app, or opens a folder onto its contents. */
    private fun openCell(item: DrawerItem, anchor: View) {
        when (item) {
            is DrawerItem.App -> onClick(item.app)
            is DrawerItem.Folder -> onFolderClick(item, anchor)
        }
    }

    /**
     * A long press that did not become a drag. The action menu is about one
     * app -- pinning it, uninstalling it -- and none of it means anything for a
     * folder, which is opened by tapping it instead.
     */
    private fun pressCell(item: DrawerItem, anchor: View) {
        if (item is DrawerItem.App) onLongClick(item.app, anchor)
    }

    /**
     * Records a drag in the drawer. Deliberately does not re-render: the cell
     * has already moved on screen, and rebuilding the pager under a finger
     * that is still down would cancel the drag it is reporting.
     */
    fun moveApp(from: Int, to: Int) {
        appOrder = appOrder.move(shownItems, from, to)
        orderStore.save(appOrder.packageNames)
        val moved = shownItems.toMutableList()
        if (from in moved.indices && to in moved.indices && from != to) {
            moved.add(to, moved.removeAt(from))
        }
        shownItems = moved
        shownApps = appOrder.apply(shownApps)
    }

    /**
     * Puts the cell held over another one into a folder with it. Held over a
     * folder, the app joins it -- which is the only way a folder gets a third
     * member.
     *
     * Only an app is ever the one moving. A folder dragged onto something is a
     * reorder: merging two folders, or nesting one, is a second helping of the
     * same modelling question and is not worth answering until it is asked.
     */
    fun combineCells(index: Int, target: Int) {
        val source = shownItems.getOrNull(index) as? DrawerItem.App ?: return
        // Combining with any member joins that member's folder, so a folder
        // cell is addressed through the first app in it.
        val onto = shownItems.getOrNull(target)?.packageNames?.firstOrNull() ?: return
        folders = folders.combine(source.app.packageName, onto)
        folderStore.save(folders)
        rerender()
    }

    /** Takes [packageName] out of the folder holding it, back into the drawer. */
    fun removeFromFolder(packageName: String) {
        folders = folders.remove(packageName)
        folderStore.save(folders)
        rerender()
    }

    /** The folder [id] as it currently stands, or null once it has dissolved. */
    fun folder(id: String): DrawerItem.Folder? =
        shownItems.filterIsInstance<DrawerItem.Folder>().firstOrNull { it.id == id }

    /**
     * What holding a drawer cell against an edge means: sideways carries it to
     * the neighbouring page, downwards hands it out of the drawer. There is
     * nothing above the drawer to hand an app to.
     */
    fun leaveDrawer(index: Int, edge: GridDragger.Edge) {
        val item = shownItems.getOrNull(index) ?: return
        when (edge) {
            GridDragger.Edge.LEFT -> carryToPage(index, -1)
            GridDragger.Edge.RIGHT -> carryToPage(index, 1)
            // A folder cannot be pinned: the row resolves package names, and a
            // folder there would be the same modelling question a second time.
            GridDragger.Edge.BOTTOM -> if (item is DrawerItem.App) onDropOut(item.app)
            GridDragger.Edge.TOP -> Unit
        }
    }

    /**
     * Sends the cell held against an edge to the neighbouring page.
     *
     * The drag ends here rather than continuing on the new page: each page is
     * its own RecyclerView, and a drag cannot be handed from one to another.
     * The cell lands at the near edge of the page it was carried to, which is
     * where it was headed.
     */
    fun carryToPage(index: Int, direction: Int) {
        val pageSize = currentMetrics?.pageSize ?: return
        val target = pager.currentItem + direction
        if (target < 0 || target >= (pager.adapter?.itemCount ?: 0)) return

        val landing = if (direction > 0) target * pageSize else target * pageSize + pageSize - 1
        moveApp(index, landing.coerceIn(0, shownItems.size - 1))
        rerender()
        pager.setCurrentItem(target, true)
    }
}
