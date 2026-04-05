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
}
