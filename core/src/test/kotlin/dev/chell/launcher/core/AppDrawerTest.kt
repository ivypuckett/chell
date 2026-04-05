package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppDrawerTest {

    private val apps = listOf(
        AppInfo("com.z", "Zebra"),
        AppInfo("com.a", "Apple"),
        AppInfo("com.m", "Mango"),
        AppInfo("com.b", "Banana"),
        AppInfo("com.c", "cherry"),
    )

    @Test
    fun emptyListHasZeroPages() {
        val drawer = AppDrawer(emptyList(), pageSize = 5)
        assertEquals(0, drawer.pageCount)
    }

    @Test
    fun singlePageWhenAppsLessThanPageSize() {
        val drawer = AppDrawer(apps, pageSize = 10)
        assertEquals(1, drawer.pageCount)
    }

    @Test
    fun pageCountRoundedUp() {
        val drawer = AppDrawer(apps, pageSize = 2)
        assertEquals(3, drawer.pageCount)
    }

    @Test
    fun appsAreSortedCaseInsensitively() {
        val drawer = AppDrawer(apps, pageSize = 10)
        val labels = drawer.page(0).map { it.label }
        assertEquals(listOf("Apple", "Banana", "cherry", "Mango", "Zebra"), labels)
    }

    @Test
    fun firstPageHasCorrectItems() {
        val drawer = AppDrawer(apps, pageSize = 2)
        assertEquals(listOf("Apple", "Banana"), drawer.page(0).map { it.label })
    }

    @Test
    fun secondPageHasCorrectItems() {
        val drawer = AppDrawer(apps, pageSize = 2)
        assertEquals(listOf("cherry", "Mango"), drawer.page(1).map { it.label })
    }

    @Test
    fun lastPageMayShorter() {
        val drawer = AppDrawer(apps, pageSize = 2)
        assertEquals(listOf("Zebra"), drawer.page(2).map { it.label })
    }

    @Test
    fun negativeIndexThrows() {
        val drawer = AppDrawer(apps, pageSize = 5)
        assertFailsWith<IndexOutOfBoundsException> { drawer.page(-1) }
    }

    @Test
    fun outOfBoundsIndexThrows() {
        val drawer = AppDrawer(apps, pageSize = 5)
        assertFailsWith<IndexOutOfBoundsException> { drawer.page(1) }
    }

    @Test
    fun invalidPageSizeThrows() {
        assertFailsWith<IllegalArgumentException> { AppDrawer(apps, pageSize = 0) }
    }

    @Test
    fun filterEmptyQueryReturnsAll() {
        val drawer = AppDrawer(apps, pageSize = 10)
        val filtered = drawer.filter("")
        assertEquals(drawer.pageCount, filtered.pageCount)
        assertEquals(drawer.page(0).map { it.label }, filtered.page(0).map { it.label })
    }

    @Test
    fun filterMatchesCaseInsensitively() {
        val drawer = AppDrawer(apps, pageSize = 10)
        val filtered = drawer.filter("AN")
        // "Banana" and "Mango" contain "an" case-insensitively
        assertEquals(listOf("Banana", "Mango"), filtered.page(0).map { it.label })
    }

    @Test
    fun filterReturnsEmptyDrawerWhenNoMatch() {
        val drawer = AppDrawer(apps, pageSize = 10)
        val filtered = drawer.filter("xyz")
        assertEquals(0, filtered.pageCount)
    }

    @Test
    fun filterPreservesPageSize() {
        val drawer = AppDrawer(apps, pageSize = 2)
        val filtered = drawer.filter("a")
        // "Apple", "Banana", "Mango" contain "a" — 3 results, pageSize 2 → 2 pages
        assertEquals(2, filtered.pageCount)
    }

    @Test
    fun filterDoesNotMutateOriginal() {
        val drawer = AppDrawer(apps, pageSize = 10)
        drawer.filter("apple")
        assertEquals(5, drawer.page(0).size)
    }
}
