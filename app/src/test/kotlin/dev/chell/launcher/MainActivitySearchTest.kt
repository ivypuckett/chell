package dev.chell.launcher

import android.view.View
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

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

    private fun startHome(): ActivityController<MainActivity> = HomeScreen.launch()

    private fun shownLabels(activity: MainActivity) = HomeScreen.shownLabels(activity)

    private fun addLauncherActivity(packageName: String, label: String) =
        HomeScreen.addLauncherActivity(packageName, label)
}
