package dev.chell.launcher

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The wallpaper only shows through if the theme sets both halves of the
 * recipe, and neither half is visible from the layout alone.
 */
@RunWith(RobolectricTestRunner::class)
class WallpaperTest {

    @Test
    fun `the window is flagged to show the wallpaper`() {
        val activity = HomeScreen.launch().get()

        val flags = activity.window.attributes.flags
        assertTrue(
            "FLAG_SHOW_WALLPAPER is not set",
            flags and WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER != 0,
        )
    }

    @Test
    fun `the window background is transparent`() {
        val activity = HomeScreen.launch().get()

        val background = activity.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val color = background.getColor(0, -1)
        background.recycle()

        assertEquals(0, color)
    }
}
