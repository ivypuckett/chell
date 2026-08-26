package dev.chell.launcher.core

/**
 * Splits [items] into fixed-size pages, in the order given.
 *
 * Ordering belongs to the caller: [AppSearch] ranks by relevance, which is not
 * alphabetical, and re-sorting here would throw that ranking away.
 *
 * Generic because the drawer pages [DrawerItem] cells while search results are
 * plain [AppInfo]; pagination is the same arithmetic either way.
 */
class AppDrawer<T>(private val items: List<T>, private val pageSize: Int) {

    init {
        require(pageSize > 0) { "pageSize must be positive" }
    }

    val pageCount: Int = if (items.isEmpty()) 0 else (items.size + pageSize - 1) / pageSize

    fun page(index: Int): List<T> {
        if (index < 0 || index >= pageCount) throw IndexOutOfBoundsException("index $index out of range [0, $pageCount)")
        val from = index * pageSize
        val to = minOf(from + pageSize, items.size)
        return items.subList(from, to)
    }
}
