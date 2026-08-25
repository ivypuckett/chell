package dev.chell.launcher

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import dev.chell.launcher.core.AppDrawer
import dev.chell.launcher.core.AppInfo

class MainActivity : ComponentActivity() {

    private lateinit var repository: AndroidAppRepository
    private val iconCache = mutableMapOf<String, Drawable?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Launchers sit at the root of the task, so back must not navigate away.
        // Replaces the old onBackPressed() override, which is no longer invoked
        // for predictive back gestures.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        repository = AndroidAppRepository(this)
        showApps()
    }

    private fun showApps() {
        val pager: ViewPager2 = findViewById(R.id.drawer_pager)
        val empty: TextView = findViewById(R.id.empty_message)

        val apps = repository.installedApps()
        if (apps.isEmpty()) {
            pager.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }

        val drawer = AppDrawer(apps, pageSize = COLUMNS * ROWS)
        pager.adapter = DrawerPagerAdapter(
            drawer = drawer,
            columns = COLUMNS,
            iconFor = ::cachedIcon,
            onClick = ::launch,
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

    private companion object {
        const val COLUMNS = 4
        const val ROWS = 5
    }
}
