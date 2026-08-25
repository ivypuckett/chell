package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AppOrderTest {

    private val apps = listOf(
        AppInfo("com.a", "Alpha"),
        AppInfo("com.b", "Beta"),
        AppInfo("com.c", "Gamma"),
    )

    private fun labels(apps: List<AppInfo>) = apps.map { it.label }

    @Test
    fun anEmptyOrderLeavesAppsAlone() {
        assertEquals(apps, AppOrder().apply(apps))
    }

    @Test
    fun applyArrangesTheAppsItKnows() {
        val order = AppOrder(listOf("com.c", "com.a", "com.b"))

        assertEquals(listOf("Gamma", "Alpha", "Beta"), labels(order.apply(apps)))
    }

    @Test
    fun anAppTheOrderDoesNotKnowGoesLast() {
        val order = AppOrder(listOf("com.c", "com.a"))

        // com.b is the new install, and lands after the arrangement.
        assertEquals(listOf("Gamma", "Alpha", "Beta"), labels(order.apply(apps)))
    }

    @Test
    fun applySkipsAnAppThatIsNotInstalled() {
        val order = AppOrder(listOf("com.gone", "com.b", "com.a", "com.c"))

        assertEquals(listOf("Beta", "Alpha", "Gamma"), labels(order.apply(apps)))
    }

    @Test
    fun moveRecordsTheWholeArrangement() {
        // Nothing was arranged before, so moving one app freezes the rest.
        val moved = AppOrder().move(apps, from = 0, to = 2)

        assertEquals(listOf("com.b", "com.c", "com.a"), moved.packageNames)
        assertEquals(listOf("Beta", "Gamma", "Alpha"), labels(moved.apply(apps)))
    }

    @Test
    fun moveWorksOnTheArrangedOrderNotTheAlphabeticalOne() {
        val order = AppOrder(listOf("com.c", "com.b", "com.a"))

        // Index 0 is Gamma once arranged, not Alpha.
        val moved = order.move(apps, from = 0, to = 1)

        assertEquals(listOf("com.b", "com.c", "com.a"), moved.packageNames)
    }

    @Test
    fun moveIgnoresIndexesOutsideTheDrawer() {
        val order = AppOrder(listOf("com.a", "com.b", "com.c"))

        assertEquals(order.packageNames, order.move(apps, from = 0, to = 9).packageNames)
        assertEquals(order.packageNames, order.move(apps, from = -1, to = 1).packageNames)
        assertEquals(order.packageNames, order.move(apps, from = 1, to = 1).packageNames)
    }

    @Test
    fun aNewInstallCannotShuffleAnArrangement() {
        val arranged = AppOrder().move(apps, from = 2, to = 0)
        val withNewApp = apps + AppInfo("com.new", "Newcomer")

        assertEquals(
            listOf("Gamma", "Alpha", "Beta", "Newcomer"),
            labels(arranged.apply(withNewApp)),
        )
    }
}
