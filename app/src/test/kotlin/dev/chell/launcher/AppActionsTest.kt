package dev.chell.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import dev.chell.launcher.core.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppActionsTest {

    private lateinit var context: Context
    private lateinit var repository: AndroidAppRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = AndroidAppRepository(context)
    }

    @Test
    fun `app info intent opens the details screen for the package`() {
        val intent = repository.appInfoIntent("com.example.alpha")

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:com.example.alpha", intent.data.toString())
    }

    @Test
    fun `uninstall intent targets the package`() {
        val intent = repository.uninstallIntent("com.example.alpha")

        assertEquals(Intent.ACTION_DELETE, intent.action)
        assertEquals("package:com.example.alpha", intent.data.toString())
    }

    @Test
    fun `a system app is reported as one`() {
        HomeScreen.addSystemApp("com.example.system", "System")

        assertTrue(repository.isSystemApp("com.example.system"))
    }

    @Test
    fun `an ordinary app is not`() {
        HomeScreen.addLauncherActivity("com.example.plain", "Plain")

        assertFalse(repository.isSystemApp("com.example.plain"))
    }

    @Test
    fun `an unknown package is not treated as a system app`() {
        assertFalse(repository.isSystemApp("com.example.missing"))
    }

    @Test
    fun `long-pressing a cell reports the app and does not launch it`() {
        var longPressed: AppInfo? = null
        var launched: AppInfo? = null
        val app = AppInfo("com.example.alpha", "Alpha")
        val adapter = AppGridAdapter(
            apps = listOf(app),
            iconFor = { null },
            onClick = { launched = it },
            onLongClick = { pressed, _ -> longPressed = pressed },
        )
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val handled = holder.itemView.performLongClick()

        assertTrue("the long press must be consumed", handled)
        assertEquals(app, longPressed)
        assertNull("a long press must not launch the app", launched)
    }
}
