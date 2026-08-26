package dev.chell.launcher

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderIconTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `the members are tiled onto the plate`() {
        val icon = FolderIcon.of(context, listOf(ColorDrawable(), ColorDrawable())) as LayerDrawable

        // The plate plus one layer per member.
        assertEquals(3, icon.numberOfLayers)
    }

    @Test
    fun `only the first few members are shown`() {
        val icons = List(6) { ColorDrawable() }

        val icon = FolderIcon.of(context, icons) as LayerDrawable

        assertTrue("a folder icon must stay legible at cell size", icon.numberOfLayers <= 5)
    }

    @Test
    fun `the icons handed in are left alone`() {
        // The launcher caches icons and binds the same instance to a cell
        // elsewhere. Resizing one into a tile used to resize it there too,
        // which showed as a folder drawn as one member blown up to fill it.
        val shared = ColorDrawable().apply { bounds = Rect(0, 0, 100, 100) }

        val icon = FolderIcon.of(context, listOf(shared, ColorDrawable())) as LayerDrawable

        assertEquals(Rect(0, 0, 100, 100), shared.bounds)
        assertNotSame(shared, icon.getDrawable(1))
    }
}
