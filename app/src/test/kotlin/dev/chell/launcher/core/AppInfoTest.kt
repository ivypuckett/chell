package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppInfoTest {

    @Test
    fun equalWhenPackageNameAndLabelMatch() {
        val a = AppInfo("com.example.app", "Example")
        val b = AppInfo("com.example.app", "Example")
        assertEquals(a, b)
    }

    @Test
    fun notEqualWhenPackageNameDiffers() {
        val a = AppInfo("com.foo", "App")
        val b = AppInfo("com.bar", "App")
        assertNotEquals(a, b)
    }

    @Test
    fun sortedByLabelAlphabetically() {
        val apps = listOf(
            AppInfo("com.z", "Zebra"),
            AppInfo("com.a", "Apple"),
            AppInfo("com.m", "Mango"),
        )
        val sorted = apps.sortedBy { it.label }
        assertEquals(listOf("Apple", "Mango", "Zebra"), sorted.map { it.label })
    }

    @Test
    fun sortedByLabelIsCaseInsensitive() {
        val apps = listOf(
            AppInfo("com.b", "banana"),
            AppInfo("com.a", "Apple"),
        )
        val sorted = apps.sortedBy { it.label.lowercase() }
        assertEquals("Apple", sorted[0].label)
    }
}
