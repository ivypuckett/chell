package dev.chell.launcher

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Rotating recreates the activity, and restoring the search field's state runs
 * the text watcher before the first layout. That render found no apps yet and
 * hid the pager, which stopped the layout the initial load was waiting on.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityRecreateTest {

    @Before
    fun setUp() {
        HomeScreen.addLauncherActivity("com.example.alpha", "Alpha")
        HomeScreen.addLauncherActivity("com.example.beta", "Beta")
    }

    @Test
    fun `the grid survives being recreated`() {
        val controller = HomeScreen.launch()

        controller.recreate()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf("Alpha", "Beta"), HomeScreen.shownLabels(controller.get()))
    }

    @Test
    fun `an empty render before the first layout does not stop the load`() {
        // No layout has happened yet, so nothing has been loaded: the render
        // this forces is the one that used to hide the pager for good.
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().findViewById<android.widget.EditText>(R.id.search_field).setText("")

        HomeScreen.resize(controller.get(), HomeScreen.HEIGHT_PX)

        assertEquals(listOf("Alpha", "Beta"), HomeScreen.shownLabels(controller.get()))
    }
}
