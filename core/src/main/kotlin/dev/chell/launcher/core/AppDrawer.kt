package dev.chell.launcher.core

class AppDrawer(apps: List<AppInfo>, private val pageSize: Int) {

    init {
        require(pageSize > 0) { "pageSize must be positive" }
    }

    private val sorted: List<AppInfo> = apps.sortedBy { it.label.lowercase() }

    val pageCount: Int = if (sorted.isEmpty()) 0 else (sorted.size + pageSize - 1) / pageSize

    fun page(index: Int): List<AppInfo> {
        if (index < 0 || index >= pageCount) throw IndexOutOfBoundsException("index $index out of range [0, $pageCount)")
        val from = index * pageSize
        val to = minOf(from + pageSize, sorted.size)
        return sorted.subList(from, to)
    }
}
