package dev.chell.launcher

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageIndicatorTest {

    @Test
    fun `shows one dot per page`() {
        val activity = manyApps()

        assertEquals(HomeScreen.pageCount(activity), indicator(activity).childCount)
        assertTrue("expected more than one page", HomeScreen.pageCount(activity) > 1)
    }

    @Test
    fun `marks the page the pager is on`() {
        val activity = manyApps()

        HomeScreen.goToPage(activity, 1)

        assertEquals(1, selectedDot(activity))
    }

    @Test
    fun `a single page shows no indicator`() {
        HomeScreen.addLauncherActivity("com.example.only", "Only")
        val activity = HomeScreen.launch().get()

        assertEquals(View.GONE, indicator(activity).visibility)
    }

    @Test
    fun `re-paging after a search rebuilds the dots`() {
        val activity = manyApps()

        activity.findViewById<EditText>(R.id.search_field).setText("App 1")

        assertEquals(HomeScreen.pageCount(activity), indicator(activity).childCount)
        assertEquals(0, selectedDot(activity))
    }

    private fun manyApps(): MainActivity {
        repeat(40) { HomeScreen.addLauncherActivity("com.example.app$it", "App %02d".format(it)) }
        return HomeScreen.launch().get()
    }

    private fun indicator(activity: MainActivity): ViewGroup =
        activity.findViewById(R.id.page_indicator)

    /** Index of the dot marked as current, or -1 if none is. */
    private fun selectedDot(activity: MainActivity): Int {
        val dots = indicator(activity)
        return (0 until dots.childCount).firstOrNull { dots.getChildAt(it).isSelected } ?: -1
    }
}
