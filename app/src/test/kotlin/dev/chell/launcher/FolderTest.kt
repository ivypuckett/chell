package dev.chell.launcher

import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import dev.chell.launcher.core.DrawerItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Folders as the drawer sees them. The gesture that makes one -- holding a
 * dragged cell over another -- is [GridDragger] geometry and is verified on the
 * emulator; what it reports is [MainActivity.combineCells], which is what these
 * drive.
 */
@RunWith(RobolectricTestRunner::class)
class FolderTest {

    @Before
    fun setUp() {
        HomeScreen.addLauncherActivity("com.example.alpha", "Alpha")
        HomeScreen.addLauncherActivity("com.example.beta", "Beta")
        HomeScreen.addLauncherActivity("com.example.gamma", "Gamma")
    }

    @Test
    fun `holding one cell over another combines them into a folder`() {
        val activity = HomeScreen.launch().get()

        // Gamma held over Alpha.
        activity.combineCells(index = 2, target = 0)

        assertEquals(listOf("Alpha+Gamma", "Beta"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `a folder is still there after a restart`() {
        HomeScreen.launch().get().combineCells(index = 1, target = 0)

        assertEquals(
            listOf("Alpha+Beta", "Gamma"),
            HomeScreen.shownLabels(HomeScreen.launch().get()),
        )
    }

    @Test
    fun `holding a third app over the folder joins it`() {
        val activity = HomeScreen.launch().get()

        activity.combineCells(index = 1, target = 0)
        // The folder is cell 0 now and Gamma is cell 1.
        activity.combineCells(index = 1, target = 0)

        assertEquals(listOf("Alpha+Beta+Gamma"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `dragging a folder onto an app reorders instead of nesting`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)

        // The folder is cell 0; holding it over Gamma must not swallow Gamma.
        activity.combineCells(index = 0, target = 1)

        assertEquals(listOf("Alpha+Beta", "Gamma"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `taking an app out of a folder puts it back in the drawer`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 2, target = 0)

        activity.removeFromFolder("com.example.gamma")

        assertEquals(listOf("Alpha", "Beta", "Gamma"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `a folder with one app left dissolves back into that app`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)

        // Removing one of two leaves a folder of one, which is not a folder.
        activity.removeFromFolder("com.example.beta")

        assertEquals(listOf("Alpha", "Beta", "Gamma"), HomeScreen.shownLabels(activity))
        assertTrue(activity.shownItems().all { it is DrawerItem.App })
    }

    @Test
    fun `a folder moves as one cell and its members travel with it`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)

        // Cells are now [Alpha+Beta, Gamma]; send the folder to the end.
        activity.moveApp(from = 0, to = 1)

        assertEquals(
            listOf("Gamma", "Alpha+Beta"),
            HomeScreen.shownLabels(HomeScreen.launch().get()),
        )
    }

    @Test
    fun `searching lists apps and never folders`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)

        activity.findViewById<EditText>(R.id.search_field).setText("a")

        // Every label contains "a"; a folder member has to be findable by name
        // without the reader remembering which folder it is in.
        assertEquals(listOf("Alpha", "Beta", "Gamma"), HomeScreen.shownLabels(activity))
    }

    @Test
    fun `a folder cannot be pinned to the favourites row`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)

        // The gesture that pins an app: hold it against the bottom edge.
        activity.leaveDrawer(index = 0, edge = GridDragger.Edge.BOTTOM)

        val row = activity.findViewById<RecyclerView>(R.id.favorites_row)
        assertEquals(0, row.adapter?.itemCount ?: 0)
    }

    @Test
    fun `an app is still pinnable once it is out of a folder`() {
        val activity = HomeScreen.launch().get()
        activity.combineCells(index = 1, target = 0)
        activity.removeFromFolder("com.example.beta")

        activity.leaveDrawer(index = 1, edge = GridDragger.Edge.BOTTOM)

        val row = activity.findViewById<RecyclerView>(R.id.favorites_row)
        assertEquals(1, row.adapter?.itemCount)
    }
}
