package dev.chell.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidAppRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AndroidAppRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = AndroidAppRepository(context)
    }

    @Test
    fun `maps launcher activities to AppInfo`() {
        addLauncherActivity("com.example.alpha", "Alpha")
        addLauncherActivity("com.example.beta", "Beta")

        val apps = repository.installedApps()

        assertEquals(
            setOf("com.example.alpha", "com.example.beta"),
            apps.map { it.packageName }.toSet(),
        )
        assertEquals("Alpha", apps.single { it.packageName == "com.example.alpha" }.label)
    }

    @Test
    fun `de-duplicates apps exposing several launcher activities`() {
        addLauncherActivity("com.example.multi", "Multi", activity = "Main")
        addLauncherActivity("com.example.multi", "Multi", activity = "Other")

        val apps = repository.installedApps()

        assertEquals(1, apps.count { it.packageName == "com.example.multi" })
    }

    @Test
    fun `excludes Chell from its own drawer`() {
        // Chell declares CATEGORY_LAUNCHER so other launchers can start it.
        addLauncherActivity(context.packageName, "Chell")

        val apps = repository.installedApps()

        assertFalse(apps.any { it.packageName == context.packageName })
    }

    @Test
    fun `launchIntent is null for a package with no launcher entry`() {
        assertNull(repository.launchIntent("com.example.absent"))
    }

    private fun addLauncherActivity(
        packageName: String,
        label: String,
        activity: String = "MainActivity",
    ) {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                this.name = "$packageName.$activity"
                this.applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                }
            }
            nonLocalizedLabel = label
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(context.packageManager)
            .addResolveInfoForIntent(launcherIntent, resolveInfo)
    }
}
