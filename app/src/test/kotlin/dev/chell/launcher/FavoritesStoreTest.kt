package dev.chell.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.chell.launcher.core.Favorites
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesStoreTest {

    private lateinit var store: FavoritesStore

    @Before
    fun setUp() {
        store = FavoritesStore(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `nothing saved reads as no favorites`() {
        assertEquals(emptyList<String>(), store.load().packageNames)
    }

    @Test
    fun `saved favorites come back in order`() {
        store.save(Favorites().pin("com.b").pin("com.a"))

        assertEquals(listOf("com.a", "com.b"), store.load().packageNames)
    }

    @Test
    fun `saving an empty set clears what was there`() {
        store.save(Favorites().pin("com.a"))
        store.save(Favorites())

        assertEquals(emptyList<String>(), store.load().packageNames)
    }

    @Test
    fun `a second store sees the first one's writes`() {
        store.save(Favorites().pin("com.a"))

        val reopened = FavoritesStore(ApplicationProvider.getApplicationContext<Context>())

        assertEquals(listOf("com.a"), reopened.load().packageNames)
    }
}
