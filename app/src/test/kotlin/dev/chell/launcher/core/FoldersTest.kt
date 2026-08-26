package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FoldersTest {

    private val alpha = AppInfo("com.a", "Alpha")
    private val beta = AppInfo("com.b", "Beta")
    private val gamma = AppInfo("com.c", "Gamma")
    private val delta = AppInfo("com.d", "Delta")

    private val apps = listOf(alpha, beta, gamma, delta)

    /** A readable rendering of the cells: an app is its label, a folder its members'. */
    private fun cells(folders: Folders, of: List<AppInfo> = apps) =
        folders.arrange(of).map { item ->
            when (item) {
                is DrawerItem.App -> item.app.label
                is DrawerItem.Folder -> item.apps.joinToString("+") { it.label }
            }
        }

    @Test
    fun noFoldersLeavesEveryAppAsItsOwnCell() {
        assertEquals(listOf("Alpha", "Beta", "Gamma", "Delta"), cells(Folders()))
    }

    @Test
    fun combiningTwoAppsMakesOneCellWhereTheFirstOfThemWas() {
        val folders = Folders().combine(source = "com.c", target = "com.a")

        // The folder takes Alpha's slot, and Gamma is no longer a cell of its own.
        assertEquals(listOf("Alpha+Gamma", "Beta", "Delta"), cells(folders))
    }

    @Test
    fun theFolderSitsWhereItsFirstMemberSitsNotWhereItWasMade() {
        // Beta is dropped onto Delta, so the folder is Delta's -- but Beta comes
        // first in the flat order, so that is where the cell appears.
        val folders = Folders().combine(source = "com.b", target = "com.d")

        assertEquals(listOf("Alpha", "Beta+Delta", "Gamma"), cells(folders))
    }

    @Test
    fun droppingOntoAFolderMemberJoinsThatFolder() {
        val folders = Folders()
            .combine(source = "com.b", target = "com.a")
            .combine(source = "com.c", target = "com.b")

        assertEquals(listOf("Alpha+Beta+Gamma", "Delta"), cells(folders))
        assertEquals(1, folders.assignments.values.distinct().size)
    }

    @Test
    fun combiningIntoASecondFolderLeavesTheFirst() {
        val folders = Folders()
            .combine(source = "com.b", target = "com.a")
            .combine(source = "com.d", target = "com.c")
            // Beta leaves Alpha's folder, which then has one member and dissolves.
            .combine(source = "com.b", target = "com.c")

        assertEquals(listOf("Alpha", "Beta+Gamma+Delta"), cells(folders))
        assertNull(folders.folderOf("com.a"))
    }

    @Test
    fun combiningAnAppWithItselfChangesNothing() {
        val folders = Folders().combine(source = "com.a", target = "com.a")

        assertTrue(folders.assignments.isEmpty())
    }

    @Test
    fun combiningTwoMembersOfTheSameFolderChangesNothing() {
        val folders = Folders().combine(source = "com.b", target = "com.a")

        assertEquals(folders.assignments, folders.combine("com.a", "com.b").assignments)
    }

    @Test
    fun removingAMemberTakesItOutOfTheFolder() {
        val folders = Folders()
            .combine(source = "com.b", target = "com.a")
            .combine(source = "com.c", target = "com.a")

        assertEquals(listOf("Alpha+Beta+Gamma", "Delta"), cells(folders))
        assertEquals(listOf("Alpha+Gamma", "Beta", "Delta"), cells(folders.remove("com.b")))
    }

    @Test
    fun aFolderWithOneMemberLeftDissolves() {
        val folders = Folders().combine(source = "com.b", target = "com.a")

        val dissolved = folders.remove("com.b")

        assertEquals(listOf("Alpha", "Beta", "Gamma", "Delta"), cells(dissolved))
        assertTrue(dissolved.assignments.isEmpty())
        assertNull(dissolved.folderOf("com.a"))
    }

    @Test
    fun aFolderOfOneCannotBeBuiltDirectly() {
        // The invariant is the constructor's, not just combine's and remove's.
        assertTrue(Folders(mapOf("com.a" to "f1")).assignments.isEmpty())
    }

    @Test
    fun aGroupShowsAsAnAppWhileOnlyOneMemberIsInstalled() {
        val folders = Folders().combine(source = "com.b", target = "com.a")

        // Beta is uninstalled. Alpha is a plain cell again, not a folder of one.
        assertEquals(listOf("Alpha", "Gamma", "Delta"), cells(folders, apps - beta))
    }

    @Test
    fun anUninstalledMemberKeepsItsPlaceInTheFolder() {
        val folders = Folders().combine(source = "com.b", target = "com.a")

        // The assignment survives the app going away, so reinstalling restores
        // the folder rather than leaving Beta loose at the end of the drawer.
        assertEquals(listOf("Alpha", "Gamma", "Delta"), cells(folders, apps - beta))
        assertEquals(listOf("Alpha+Beta", "Gamma", "Delta"), cells(folders, apps))
    }

    @Test
    fun membersReportsWhatIsInAFolder() {
        val folders = Folders().combine(source = "com.b", target = "com.a")
        val id = folders.folderOf("com.a")!!

        assertEquals(setOf("com.a", "com.b"), folders.members(id))
    }

    @Test
    fun aSecondFolderGetsAnIdOfItsOwn() {
        val folders = Folders()
            .combine(source = "com.b", target = "com.a")
            .combine(source = "com.d", target = "com.c")

        assertEquals(2, folders.assignments.values.distinct().size)
    }
}
