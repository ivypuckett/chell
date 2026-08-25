package dev.chell.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Shared plumbing for the [MainActivity] tests.
 *
 * Robolectric never lays a window out on its own, and the launcher defers its
 * first app load until the pager has been measured, so the tests have to drive
 * layout themselves.
 */
object HomeScreen {

    const val WIDTH_PX = 1080
    const val HEIGHT_PX = 1920

    fun launch(height: Int = HEIGHT_PX): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        resize(controller.get(), height)
        return controller
    }

    /** Re-measures and re-lays out the whole window at [height]. */
    fun resize(activity: MainActivity, height: Int) {
        val root = activity.findViewById<View>(R.id.home_root)
        root.measure(
            MeasureSpec.makeMeasureSpec(WIDTH_PX, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, WIDTH_PX, height)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** The apps the drawer would lay out, in order, across every page. */
    fun shownLabels(activity: MainActivity): List<String> {
        val pager = activity.findViewById<ViewPager2>(R.id.drawer_pager)
        val adapter = pager.adapter as? DrawerPagerAdapter ?: return emptyList()
        val drawer = adapter.drawer
        return (0 until drawer.pageCount).flatMap { drawer.page(it) }.map { it.label }
    }

    /** Swipes to [index] and lets the page-change callbacks run. */
    fun goToPage(activity: MainActivity, index: Int) {
        val pager = activity.findViewById<ViewPager2>(R.id.drawer_pager)
        pager.setCurrentItem(index, false)
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun pageCount(activity: MainActivity): Int {
        val pager = activity.findViewById<ViewPager2>(R.id.drawer_pager)
        return (pager.adapter as? DrawerPagerAdapter)?.drawer?.pageCount ?: 0
    }

    /** Registers a launcher activity whose package is flagged as system. */
    fun addSystemApp(packageName: String, label: String) {
        addLauncherActivity(packageName, label, ApplicationInfo.FLAG_SYSTEM)
    }

    fun addLauncherActivity(packageName: String, label: String, flags: Int = 0) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.Main"
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    this.flags = flags
                }
                nonLocalizedLabel = label
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        // Resolving the activity is not the same as the package being installed;
        // isSystemApp reads the installed ApplicationInfo.
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    this.flags = flags
                }
            },
        )
    }
}
