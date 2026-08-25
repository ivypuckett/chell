package dev.chell.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.AppSearch
import dev.chell.launcher.core.GridMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var repository: AndroidAppRepository
    private lateinit var pager: ViewPager2
    private lateinit var emptyMessage: TextView
    private lateinit var searchField: EditText

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

        pager = findViewById(R.id.drawer_pager)
        emptyMessage = findViewById(R.id.empty_message)
        searchField = findViewById(R.id.search_field)
        repository = AndroidAppRepository(this)

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

        // The grid is sized from the pager's measured bounds, so the first load
        // waits for layout.
        pager.doOnLayout { loadApps() }
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

    /**
     * Renders [allApps] filtered by the current query. [keepPage] holds the
     * reader's place across a reload; a changed query resets to page 0.
     */
    private fun showApps(keepPage: Boolean = true) {
        val apps = AppSearch.search(allApps, searchField.text.toString())
        if (apps.isEmpty()) {
            pager.visibility = View.GONE
            emptyMessage.setText(if (allApps.isEmpty()) R.string.no_apps else R.string.no_matches)
            emptyMessage.visibility = View.VISIBLE
            return
        }
        pager.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        val metrics = gridMetrics()
        val drawer = AppDrawer(apps, pageSize = metrics.pageSize)

        val targetPage = if (keepPage) pager.currentItem.coerceAtMost(drawer.pageCount - 1) else 0
        pager.adapter = DrawerPagerAdapter(
            drawer = drawer,
            columns = metrics.columns,
            iconFor = ::cachedIcon,
            onClick = ::launch,
        )
        pager.setCurrentItem(targetPage, false)
    }

    /** Derives the grid from the pager's measured size and the cell dimensions. */
    private fun gridMetrics(): GridMetrics {
        // The padding lives on each page (page_apps.xml), not on the pager.
        val pagePadding = resources.getDimensionPixelSize(R.dimen.app_page_vertical_padding)
        return GridMetrics.fit(
            availableWidth = pager.width,
            availableHeight = pager.height - 2 * pagePadding,
            cellWidth = resources.getDimensionPixelSize(R.dimen.app_cell_width),
            cellHeight = resources.getDimensionPixelSize(R.dimen.app_cell_height),
        )
    }

    private fun cachedIcon(packageName: String): Drawable? =
        iconCache.getOrPut(packageName) { repository.icon(packageName) }

    private fun launch(app: AppInfo) {
        val intent = repository.launchIntent(app.packageName)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.cannot_launch, app.label), Toast.LENGTH_SHORT)
                .show()
            return
        }
        startActivity(intent)
    }
}
