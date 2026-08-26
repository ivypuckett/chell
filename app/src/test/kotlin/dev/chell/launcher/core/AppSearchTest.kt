package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSearchTest {

    private val apps = listOf(
        AppInfo("com.google.chrome", "Chrome"),
        AppInfo("com.google.calendar", "Calendar"),
        AppInfo("com.example.reader", "Comic Reader"),
        AppInfo("com.example.arch", "Archive"),
        AppInfo("com.example.cam", "camera"),
        AppInfo("com.example.chat", "Discord"),
        AppInfo("com.example.shop", "Store"),
    )

    private fun search(query: String) = AppSearch.search(apps, query).map { it.label }

    @Test
    fun blankQueryReturnsEveryAppAlphabetically() {
        assertEquals(
            listOf("Archive", "Calendar", "camera", "Chrome", "Comic Reader", "Discord", "Store"),
            search(""),
        )
    }

    @Test
    fun whitespaceOnlyQueryIsTreatedAsBlank() {
        assertEquals(7, search("   ").size)
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertEquals(listOf("Chrome"), search("CHROME"))
    }

    @Test
    fun nonMatchingAppsAreExcluded() {
        assertEquals(listOf("Calendar", "camera"), search("ca"))
    }

    @Test
    fun prefixMatchesRankAboveSubstringMatches() {
        // "Comic Reader" starts with "co"; "Discord" only contains it.
        assertEquals(listOf("Comic Reader", "Discord"), search("co"))
    }

    @Test
    fun wordStartMatchesRankAboveSubstringMatches() {
        // "Comic Reader" has a word starting with "re"; "Store" merely contains it.
        assertEquals(listOf("Comic Reader", "Store"), search("re"))
    }

    @Test
    fun prefixMatchesRankAboveWordStartMatches() {
        assertEquals(listOf("Reader Pro", "Comic Reader"), AppSearch.search(
            listOf(AppInfo("com.a", "Comic Reader"), AppInfo("com.b", "Reader Pro")),
            "reader",
        ).map { it.label })
    }

    @Test
    fun tiesBreakAlphabeticallyIgnoringCase() {
        assertEquals(listOf("Calendar", "camera"), search("cA"))
    }

    @Test
    fun queryIsTrimmedBeforeMatching() {
        assertEquals(listOf("Chrome"), search("  chrome "))
    }

    @Test
    fun noMatchesReturnsEmptyList() {
        assertEquals(emptyList<String>(), search("zzz"))
    }
}
