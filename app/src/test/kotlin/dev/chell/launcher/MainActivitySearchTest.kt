package dev.chell.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivitySearchTest {

    @Before
    fun setUp() {
        addLauncherActivity("com.example.alpha", "Alpha")
        addLauncherActivity("com.example.beta", "Beta")
        addLauncherActivity("com.example.gamma", "Gamma")
    }

    @Test
    fun `shows every app when the query is empty`() {
        val activity = launchHome()
        assertEquals(listOf("Alpha", "Beta", "Gamma"), shownLabels(activity))
    }

    @Test
    fun `typing filters the grid to matching apps`() {
        val activity = launchHome()

        activity.findViewById<EditText>(R.id.search_field).setText("be")

        assertEquals(listOf("Beta"), shownLabels(activity))
    }

    @Test
    fun `the grid keeps the ranking order, not the alphabet`() {
        addLauncherActivity("com.example.mail", "Gmail")
        addLauncherActivity("com.example.maps", "Maps")
        val activity = launchHome()

        activity.findViewById<EditText>(R.id.search_field).setText("ma")

        // "Maps" starts with the query; "Gamma" and "Gmail" merely contain it,
        // so they follow, alphabetically between themselves.
        assertEquals(listOf("Maps", "Gamma", "Gmail"), shownLabels(activity))
    }

    @Test
    fun `a query matching nothing shows the empty message`() {
        val activity = launchHome()

        activity.findViewById<EditText>(R.id.search_field).setText("zzz")

        val message = activity.findViewById<TextView>(R.id.empty_message)
        assertEquals(View.VISIBLE, message.visibility)
        assertEquals(activity.getString(R.string.no_matches), message.text)
    }

    @Test
    fun `back clears a non-empty query`() {
        val activity = launchHome()
        val field = activity.findViewById<EditText>(R.id.search_field)
        field.setText("be")

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(field.text.isEmpty())
        assertEquals(listOf("Alpha", "Beta", "Gamma"), shownLabels(activity))
    }

    @Test
    fun `leaving the foreground clears the query`() {
        val controller = startHome()
        val activity = controller.get()
        activity.findViewById<EditText>(R.id.search_field).setText("be")

        controller.pause().stop()

        assertTrue(activity.findViewById<EditText>(R.id.search_field).text.isEmpty())
        controller.destroy()
    }

    private fun launchHome(): MainActivity = startHome().get()

    /**
     * Robolectric never lays a window out on its own, and the first app load is
     * deferred until the pager has been measured, so drive both explicitly.
     */
    private fun startHome(): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val root = controller.get().findViewById<View>(R.id.home_root)
        root.measure(
            MeasureSpec.makeMeasureSpec(WIDTH_PX, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(HEIGHT_PX, MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, WIDTH_PX, HEIGHT_PX)
        shadowOf(Looper.getMainLooper()).idle()
        return controller
    }

    /** The labels the drawer would lay out, across every page. */
    private fun shownLabels(activity: MainActivity): List<String> {
        val pager = activity.findViewById<ViewPager2>(R.id.drawer_pager)
        val adapter = pager.adapter as? DrawerPagerAdapter ?: return emptyList()
        val drawer = adapter.drawer
        return (0 until drawer.pageCount).flatMap { drawer.page(it) }.map { it.label }
    }

    private companion object {
        const val WIDTH_PX = 1080
        const val HEIGHT_PX = 1920
    }

    private fun addLauncherActivity(packageName: String, label: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.Main"
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
                nonLocalizedLabel = label
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }
}
