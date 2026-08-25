package dev.chell.launcher

import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrawerOrderTest {

    @Before
    fun setUp() {
        HomeScreen.addLauncherActivity("com.example.alpha", "Alpha")
        HomeScreen.addLauncherActivity("com.example.beta", "Beta")
        HomeScreen.addLauncherActivity("com.example.gamma", "Gamma")
    }

    @Test
    fun `the drawer is alphabetical until something is moved`() {
        val activity = HomeScreen.launch().get()

        assertEquals(listOf("Alpha", "Beta", "Gamma"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `a moved app keeps its new place across a restart`() {
        val activity = HomeScreen.launch().get()

        activity.moveApp(from = 0, to = 2)

        // Deliberately not re-rendered under the finger, so the new order is
        // read back from a fresh launch rather than from this one.
        assertEquals(
            listOf("Beta", "Gamma", "Alpha"),
            HomeScreen.shownLabels(HomeScreen.launch().get()),
        )
    }

    @Test
    fun `a newly installed app lands at the end of an arrangement`() {
        HomeScreen.launch().get().moveApp(from = 2, to = 0)

        HomeScreen.addLauncherActivity("com.example.delta", "Delta")

        assertEquals(
            listOf("Gamma", "Alpha", "Beta", "Delta"),
            HomeScreen.shownLabels(HomeScreen.launch().get()),
        )
    }

    @Test
    fun `an app that is moved twice ends where it was last put`() {
        val activity = HomeScreen.launch().get()

        activity.moveApp(from = 0, to = 2)
        activity.moveApp(from = 2, to = 1)

        assertEquals(
            listOf("Beta", "Alpha", "Gamma"),
            HomeScreen.shownLabels(HomeScreen.launch().get()),
        )
    }

    @Test
    fun `searching ranks by relevance and leaves the arrangement alone`() {
        val activity = HomeScreen.launch().get()
        activity.moveApp(from = 2, to = 0)

        activity.findViewById<EditText>(R.id.search_field).setText("a")

        // Every label contains "a"; the ranking is the search's, not the
        // arrangement's.
        assertEquals(listOf("Alpha", "Beta", "Gamma"), HomeScreen.shownLabels(activity))
    }
}
