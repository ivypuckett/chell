package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesTest {

    private val apps = listOf(
        AppInfo("com.a", "Alpha"),
        AppInfo("com.b", "Beta"),
        AppInfo("com.c", "Gamma"),
    )

    @Test
    fun startsEmpty() {
        assertEquals(emptyList<String>(), Favorites().packageNames)
    }

    @Test
    fun pinningAddsToTheFront() {
        val favorites = Favorites().pin("com.a").pin("com.b")

        assertEquals(listOf("com.b", "com.a"), favorites.packageNames)
    }

    @Test
    fun pinningSomethingAlreadyPinnedMovesItToTheFrontWithoutDuplicating() {
        val favorites = Favorites().pin("com.a").pin("com.b").pin("com.a")

        assertEquals(listOf("com.a", "com.b"), favorites.packageNames)
    }

    @Test
    fun unpinningRemovesIt() {
        val favorites = Favorites().pin("com.a").pin("com.b").unpin("com.a")

        assertEquals(listOf("com.b"), favorites.packageNames)
    }

    @Test
    fun unpinningSomethingNotPinnedChangesNothing() {
        val favorites = Favorites().pin("com.a").unpin("com.zzz")

        assertEquals(listOf("com.a"), favorites.packageNames)
    }

    @Test
    fun reportsWhatIsPinned() {
        val favorites = Favorites().pin("com.a")

        assertTrue(favorites.isPinned("com.a"))
        assertFalse(favorites.isPinned("com.b"))
    }

    @Test
    fun resolvesToAppsInFavoriteOrder() {
        val favorites = Favorites().pin("com.c").pin("com.a")

        assertEquals(listOf("Alpha", "Gamma"), favorites.resolve(apps).map { it.label })
    }

    @Test
    fun resolveSkipsAppsThatAreNoLongerInstalled() {
        val favorites = Favorites().pin("com.gone").pin("com.a")

        assertEquals(listOf("Alpha"), favorites.resolve(apps).map { it.label })
    }

    @Test
    fun resolveIsCappedAtTheRowWidth() {
        val favorites = Favorites().pin("com.a").pin("com.b").pin("com.c")

        assertEquals(2, favorites.resolve(apps, limit = 2).size)
    }

    @Test
    fun aNonPositiveLimitResolvesToNothing() {
        val favorites = Favorites().pin("com.a")

        assertEquals(emptyList<AppInfo>(), favorites.resolve(apps, limit = 0))
    }
}
