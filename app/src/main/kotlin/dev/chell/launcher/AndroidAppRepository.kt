package dev.chell.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dev.chell.launcher.core.AppInfo
import dev.chell.launcher.core.AppRepository

/**
 * Reads the set of launchable apps from the platform [PackageManager].
 *
 * Package visibility on API 30+ is granted by the `<queries>` element in the
 * manifest, which is narrower than the QUERY_ALL_PACKAGES permission.
 */
class AndroidAppRepository(context: Context) : AppRepository {

    private val packageManager: PackageManager = context.packageManager
    private val ownPackageName: String = context.packageName

    override fun installedApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, 0)
            // An app may expose several launcher activities; one entry each.
            .distinctBy { it.activityInfo.packageName }
            // Chell declares CATEGORY_LAUNCHER so other launchers can start it,
            // but it should not list itself in its own drawer.
            .filterNot { it.activityInfo.packageName == ownPackageName }
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                )
            }
    }

    fun icon(packageName: String): Drawable? =
        runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()

    /** Returns the intent that starts [packageName], or null if it has none. */
    fun launchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)
}
