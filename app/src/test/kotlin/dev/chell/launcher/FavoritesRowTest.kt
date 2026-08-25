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

    private fun store() =
        FavoritesStore(ApplicationProvider.getApplicationContext<Context>())

    private fun row(activity: MainActivity): RecyclerView =
        activity.findViewById(R.id.favorites_row)

    private fun rowLabels(activity: MainActivity): List<String> {
        val adapter = row(activity).adapter as? AppGridAdapter ?: return emptyList()
        return (0 until adapter.itemCount).map { adapter.appAt(it).label }
    }
}
