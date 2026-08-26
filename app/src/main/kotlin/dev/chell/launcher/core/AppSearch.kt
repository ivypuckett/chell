package dev.chell.launcher.core

/**
 * Filters and ranks apps by a typed query.
 *
 * Kept out of the Android layer so the matching rules are unit testable;
 * callers pass in the app list they already hold.
 */
object AppSearch {

    /** Better matches sort first; [PREFIX] is the strongest. */
    private enum class Match { PREFIX, WORD_START, SUBSTRING }

    /**
     * Returns the apps matching [query], best match first, ties broken
     * alphabetically. A blank query matches everything, so the result is just
     * the alphabetical list [AppDrawer] would show anyway.
     */
    fun search(apps: List<AppInfo>, query: String): List<AppInfo> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return apps.sortedBy { it.label.lowercase() }
        return apps
            .mapNotNull { app -> match(app.label.lowercase(), needle)?.let { app to it } }
            .sortedWith(compareBy({ it.second }, { it.first.label.lowercase() }))
            .map { it.first }
    }

    private fun match(label: String, needle: String): Match? = when {
        label.startsWith(needle) -> Match.PREFIX
        startsAWord(label, needle) -> Match.WORD_START
        label.contains(needle) -> Match.SUBSTRING
        else -> null
    }

    /** True when [needle] begins a word of [label] other than the first. */
    private fun startsAWord(label: String, needle: String): Boolean =
        label.split(' ').drop(1).any { it.startsWith(needle) }
}
