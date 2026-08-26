package dev.chell.launcher.core

interface AppRepository {
    fun installedApps(): List<AppInfo>
}
