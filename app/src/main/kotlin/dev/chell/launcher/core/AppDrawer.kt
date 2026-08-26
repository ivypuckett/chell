package dev.chell.launcher.core

/**
 * Splits [apps] into fixed-size pages, in the order given.
 *
 * Ordering belongs to the caller: [AppSearch] ranks by relevance, which is not
 * alphabetical, and re-sorting here would throw that ranking away.
 */
class AppDrawer(private val apps: List<AppInfo>, private val pageSize: Int) {

    init {
        require(pageSize > 0) { "pageSize must be positive" }
    }

    val pageCount: Int = if (apps.isEmpty()) 0 else (apps.size + pageSize - 1) / pageSize

    fun page(index: Int): List<AppInfo> {
        if (index < 0 || index >= pageCount) throw IndexOutOfBoundsException("index $index out of range [0, $pageCount)")
        val from = index * pageSize
        val to = minOf(from + pageSize, apps.size)
        return apps.subList(from, to)
    }
}
