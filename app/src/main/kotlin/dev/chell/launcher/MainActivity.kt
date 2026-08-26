package dev.chell.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.GridMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var repository: AndroidAppRepository
    private lateinit var searchField: EditText
    private lateinit var favoritesRow: FavoritesRow
    private lateinit var drawer: DrawerPager

    /**
     * Holds the pager and the empty message. Sizing and the initial load hang
     * off this rather than off the pager: the pager is hidden whenever there is
     * nothing to show, and a hidden view is never laid out, so waiting on it
     * could wait forever.
     */
    private lateinit var gridContainer: FrameLayout

    private val iconCache = mutableMapOf<String, Drawable?>()

    /** Every installed app; what the grid shows is this filtered by the query. */
    private var allApps: List<AppInfo> = emptyList()

    /** Apps come and go while the launcher is on screen; reload when they do. */
    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            iconCache.clear()
            loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchField = findViewById(R.id.search_field)
        gridContainer = findViewById(R.id.grid_container)
        repository = AndroidAppRepository(this)
        favoritesRow = FavoritesRow(
            view = findViewById(R.id.favorites_row),
            store = FavoritesStore(this),
            iconFor = ::cachedIcon,
            onClick = ::launch,
            onLongClick = ::showAppActions,
        )
        drawer = DrawerPager(
            pager = findViewById(R.id.drawer_pager),
            emptyMessage = findViewById(R.id.empty_message),
            pageIndicator = PageIndicator(findViewById(R.id.page_indicator)),
            orderStore = PackageListStore(this, KEY_ORDER),
            iconFor = ::cachedIcon,
            onClick = ::launch,
            onLongClick = ::showAppActions,
            onDropOut = ::pinDroppedApp,
            rerender = ::showApps,
        )

        // A new query starts at the first page of its own results.
        searchField.doAfterTextChanged { showApps(keepPage = false) }

        // Launchers sit at the root of the task, so back must not navigate away.
        // Replaces the old onBackPressed() override, which is no longer invoked
        // for predictive back gestures.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                searchField.text.clear()
            }
        })

        // The grid is sized from the container's measured bounds, so the first
        // load waits for layout.
        gridContainer.doOnLayout { loadApps() }

        // The pager shrinks when the keyboard opens over the search field, which
        // fits fewer rows. Re-page whenever that changes the grid; re-rendering
        // in place would reenter the layout pass, so hand it to the next frame.
        gridContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (allApps.isNotEmpty() && gridMetrics() != drawer.currentMetrics) {
                gridContainer.post { if (gridMetrics() != drawer.currentMetrics) showApps() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageChangeReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(packageChangeReceiver)
        // Coming back to the launcher should show the whole drawer, not
        // whatever was typed before the last app was opened.
        searchField.text.clear()
    }

    private fun loadApps() {
        // Querying every installed package and loading its label hits the
        // PackageManager once per app, which is far too slow for the main
        // thread on a device with a realistic number of apps.
        lifecycleScope.launch {
            allApps = withContext(Dispatchers.IO) { repository.installedApps() }
            showApps()
        }
    }

    private fun showApps(keepPage: Boolean = true) {
        val metrics = gridMetrics()
        favoritesRow.show(allApps, metrics.columns)
        drawer.show(allApps, searchField.text.toString(), metrics, keepPage)
    }

    /** Derives the grid from the container's measured size and the cell dimensions. */
    private fun gridMetrics(): GridMetrics {
        // The padding lives on each page (page_apps.xml), not on the pager.
        val pagePadding = resources.getDimensionPixelSize(R.dimen.app_page_vertical_padding)
        return GridMetrics.fit(
            availableWidth = gridContainer.width,
            availableHeight = gridContainer.height - 2 * pagePadding,
            cellWidth = resources.getDimensionPixelSize(R.dimen.app_cell_width),
            cellHeight = resources.getDimensionPixelSize(R.dimen.app_cell_height),
        )
    }

    private fun cachedIcon(packageName: String): Drawable? =
        iconCache.getOrPut(packageName) { repository.icon(packageName) }

    /** A cell dragged out of the bottom of the drawer is being pinned. */
    private fun pinDroppedApp(app: AppInfo) {
        if (!favoritesRow.isPinned(app.packageName)) pinToFavorites(app.packageName)
    }

    // The drag entry points stay here as one-line delegates: they are what the
    // tests drive, and moving the pager out should not need them rewritten.

    fun moveApp(from: Int, to: Int) = drawer.moveApp(from, to)

    fun leaveDrawer(index: Int, edge: GridDragger.Edge) = drawer.leaveDrawer(index, edge)

    fun pinToFavorites(packageName: String) = favoritesRow.pin(packageName)

    fun unpinFromFavorites(packageName: String) = favoritesRow.unpin(packageName)

    fun moveFavorite(from: Int, to: Int) = favoritesRow.move(from, to)

    /** The menu a long press opens, anchored to the cell that was pressed. */
    private fun showAppActions(app: AppInfo, anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.inflate(R.menu.app_actions)
        val pinned = favoritesRow.isPinned(app.packageName)
        menu.menu.findItem(R.id.action_favorite)
            .setTitle(if (pinned) R.string.action_unpin else R.string.action_pin)
        // System apps cannot be removed, so do not offer to.
        menu.menu.findItem(R.id.action_uninstall).isVisible =
            !repository.isSystemApp(app.packageName)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_favorite ->
                    if (pinned) unpinFromFavorites(app.packageName)
                    else pinToFavorites(app.packageName)
                R.id.action_app_info -> startActivity(repository.appInfoIntent(app.packageName))
                R.id.action_uninstall -> startActivity(repository.uninstallIntent(app.packageName))
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        menu.show()
    }

    private fun launch(app: AppInfo) {
        val intent = repository.launchIntent(app.packageName)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.cannot_launch, app.label), Toast.LENGTH_SHORT)
                .show()
            return
        }
        startActivity(intent)
    }

    private companion object {
        const val KEY_ORDER = "order"
    }
}
