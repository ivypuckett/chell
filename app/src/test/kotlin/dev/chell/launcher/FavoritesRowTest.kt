package dev.chell.launcher

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import dev.chell.launcher.core.Favorites
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesRowTest {

    @Before
    fun setUp() {
        HomeScreen.addLauncherActivity("com.example.alpha", "Alpha")
        HomeScreen.addLauncherActivity("com.example.beta", "Beta")
    }

    @Test
    fun `no favorites means no row`() {
        val activity = HomeScreen.launch().get()

        assertEquals(View.GONE, row(activity).visibility)
    }

    @Test
    fun `pinned apps appear in the row, newest first`() {
        store().save(Favorites().pin("com.example.beta").pin("com.example.alpha"))

        val activity = HomeScreen.launch().get()

        assertEquals(View.VISIBLE, row(activity).visibility)
        assertEquals(listOf("Alpha", "Beta"), rowLabels(activity))
    }

    @Test
    fun `a favorite that is no longer installed is skipped`() {
        store().save(Favorites().pin("com.example.gone").pin("com.example.alpha"))

        val activity = HomeScreen.launch().get()

        assertEquals(listOf("Alpha"), rowLabels(activity))
    }

    @Test
    fun `pinning updates the row and survives a restart`() {
        val activity = HomeScreen.launch().get()

        activity.pinToFavorites("com.example.beta")

        assertEquals(listOf("Beta"), rowLabels(activity))
        assertEquals(listOf("com.example.beta"), store().load().packageNames)
    }

    @Test
    fun `unpinning empties the row again`() {
        val activity = HomeScreen.launch().get()
        activity.pinToFavorites("com.example.beta")

        activity.unpinFromFavorites("com.example.beta")

        assertEquals(View.GONE, row(activity).visibility)
        assertEquals(emptyList<String>(), store().load().packageNames)
    }

    @Test
    fun `dragging a favorite reorders the row and saves the order`() {
        store().save(Favorites().pin("com.example.beta").pin("com.example.alpha"))
        val activity = HomeScreen.launch().get()
        assertEquals(listOf("Alpha", "Beta"), rowLabels(activity))

        activity.moveFavorite(0, 1)

        assertEquals(listOf("Beta", "Alpha"), rowLabels(activity))
        assertEquals(
            listOf("com.example.beta", "com.example.alpha"),
            store().load().packageNames,
        )
    }

    @Test
    fun `a dragged order survives a restart`() {
        store().save(Favorites().pin("com.example.beta").pin("com.example.alpha"))
        val activity = HomeScreen.launch().get()
        activity.moveFavorite(0, 1)

        assertEquals(listOf("Beta", "Alpha"), rowLabels(HomeScreen.launch().get()))
    }

    @Test
    fun `dragging leaves a favorite that is not installed where it was`() {
        // com.example.gone is pinned between the two, but never shown, so the
        // drag cannot have addressed it -- it keeps its slot.
        store().save(
            Favorites()
                .pin("com.example.beta")
                .pin("com.example.gone")
                .pin("com.example.alpha"),
        )
        val activity = HomeScreen.launch().get()

        activity.moveFavorite(0, 1)

        assertEquals(
            listOf("com.example.beta", "com.example.gone", "com.example.alpha"),
            store().load().packageNames,
        )
    }

    @Test
    fun `an app held against the bottom of the drawer is pinned`() {
        val activity = HomeScreen.launch().get()

        // Beta is the second cell of an alphabetical drawer.
        activity.leaveDrawer(index = 1, edge = GridDragger.Edge.BOTTOM)

        assertEquals(listOf("Beta"), rowLabels(activity))
        assertEquals(listOf("com.example.beta"), store().load().packageNames)
    }

    @Test
    fun `holding an app that is already pinned leaves the row alone`() {
        // Pinning puts an app at the front, so pinning Beta a second time
        // would shuffle the row rather than do nothing.
        store().save(Favorites().pin("com.example.beta").pin("com.example.alpha"))
        val activity = HomeScreen.launch().get()

        activity.leaveDrawer(index = 1, edge = GridDragger.Edge.BOTTOM)

        assertEquals(listOf("Alpha", "Beta"), rowLabels(activity))
    }

    private fun store() =
        FavoritesStore(ApplicationProvider.getApplicationContext<Context>())

    private fun row(activity: MainActivity): RecyclerView =
        activity.findViewById(R.id.favorites_row)

    private fun rowLabels(activity: MainActivity): List<String> {
        val adapter = row(activity).adapter as? AppGridAdapter ?: return emptyList()
        return (0 until adapter.itemCount).map { adapter.appAt(it).label }
    }
}
