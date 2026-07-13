package com.bearinmind.launcher314

import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.lastOpenedKey
import com.bearinmind.launcher314.helpers.DrawerSearchMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke tests for the type-to-find app-drawer matcher. Verifies the behaviors
 * users expect (initials, word boundaries, streaks, accents, package fallback,
 * typo tolerance) without asserting exact score magnitudes.
 */
class DrawerSearchMatcherTest {

    private fun app(name: String, pkg: String = "pkg." + name.lowercase().replace(" ", "")) =
        AppInfo(name = name, packageName = pkg, iconPath = "")

    @Test fun initials_yt_findsYouTube() {
        assertTrue(DrawerSearchMatcher.scoreMatch("YouTube", "yt") > 0)
    }

    @Test fun initials_gm_findsGmail() {
        assertTrue(DrawerSearchMatcher.scoreMatch("Gmail", "gm") > 0)
    }

    @Test fun boundary_hyphen_findsFDroid() {
        assertTrue(DrawerSearchMatcher.scoreMatch("F-Droid", "fd") > 0)
    }

    @Test fun boundary_space_findsGoogleMaps() {
        assertTrue(DrawerSearchMatcher.scoreMatch("Google Maps", "maps") > 0)
    }

    @Test fun streak_findsChrome() {
        assertTrue(DrawerSearchMatcher.scoreMatch("Chrome", "chr") > 0)
    }

    @Test fun trailingCharsAllowed_findsYouTube() {
        // Query is a subsequence; label may have trailing chars.
        assertTrue(DrawerSearchMatcher.scoreMatch("YouTube", "youtub") > 0)
    }

    @Test fun noMatch_returnsZero() {
        assertEquals(0, DrawerSearchMatcher.scoreMatch("Chrome", "zzz"))
    }

    @Test fun queryLongerThanLabel_returnsZero() {
        assertEquals(0, DrawerSearchMatcher.scoreMatch("Yo", "youtube"))
    }

    @Test fun pureStreakBeatsBoundaryJump() {
        // gm on "Gmail" is a pure streak (0 penalty); yt on "YouTube" pays a
        // word-boundary jump (1 penalty). So gm should score at least as high.
        val gm = DrawerSearchMatcher.scoreMatch("Gmail", "gm")
        val yt = DrawerSearchMatcher.scoreMatch("YouTube", "yt")
        assertTrue(gm >= yt)
    }

    @Test fun accentFolding_pokeFindsPokemon() {
        assertTrue(DrawerSearchMatcher.scoreMatch("Pokémon", "poke") > 0)
    }

    @Test fun typoFallback_rescuesMissButRanksBelowReal() {
        val real = DrawerSearchMatcher.scoreMatch("Chrome", "chrome")
        val typo = DrawerSearchMatcher.scoreMatch("Chrome", "chrpme") // p is QWERTY-neighbor of o
        assertTrue("typo should still match", typo > 0)
        assertTrue("typo must rank strictly below a real match", typo < real)
    }

    @Test fun typoFallback_notFiredForShortQueries() {
        // Query length < 3: no fallback, an outright miss stays 0.
        assertEquals(0, DrawerSearchMatcher.scoreMatch("Chrome", "cx"))
    }

    @Test fun searchApps_ranksBestMatchFirst() {
        val apps = listOf(app("Yatzy"), app("YouTube"), app("Yelp"))
        val res = DrawerSearchMatcher.searchApps(apps, "yt")
        assertTrue(res.isNotEmpty())
        assertEquals("YouTube", res.first().name)
    }

    @Test fun searchApps_packageFallbackViaDot() {
        val apps = listOf(
            app("Chrome", "com.android.chrome"),
            app("Gmail", "com.google.android.gm"),
            app("Photos", "com.google.android.apps.photos"),
        )
        val res = DrawerSearchMatcher.searchApps(apps, "com.google")
        assertTrue(res.any { it.name == "Gmail" })
        assertTrue(res.any { it.name == "Photos" })
    }

    @Test fun searchApps_blankQueryReturnsEmpty() {
        assertTrue(DrawerSearchMatcher.searchApps(listOf(app("Chrome")), "   ").isEmpty())
    }

    @Test fun searchApps_whitespaceInQueryIsStripped() {
        val res = DrawerSearchMatcher.searchApps(listOf(app("Google Maps")), "g m")
        assertTrue(res.any { it.name == "Google Maps" })
    }

    // ---- Recency ranking (issue #64) ----

    @Test fun recency_breaksTiesWithinSameScore() {
        val notes = app("Notes")     // "note" is a streak match -> MAX_VALUE
        val notepad = app("Notepad") // "note" is a streak match -> MAX_VALUE (ties Notes)
        val opened = mapOf(lastOpenedKey(notes.packageName, notes.userSerial) to 9_999_999_999L)
        val res = DrawerSearchMatcher.searchApps(listOf(notepad, notes), "note", lastOpened = opened)
        assertEquals("Notes", res.first().name) // recently-opened wins the tie
    }

    @Test fun recency_matchQualityStillWins() {
        val gmail = app("Gmail")          // "gm" streak -> MAX_VALUE
        val gameMgr = app("Game Manager") // "gm" -> G streak, M word-boundary -> MAX_VALUE-1
        val opened = mapOf(lastOpenedKey(gameMgr.packageName, gameMgr.userSerial) to 9_999_999_999L)
        val res = DrawerSearchMatcher.searchApps(listOf(gameMgr, gmail), "gm", lastOpened = opened)
        assertEquals("Gmail", res.first().name) // better match beats recency across tiers
    }

    @Test fun recency_ignoredWhenMapEmpty() {
        // No recency data -> falls back to alphabetical tiebreak on equal scores.
        val res = DrawerSearchMatcher.searchApps(listOf(app("Notepad"), app("Notes")), "note")
        assertEquals("Notepad", res.first().name) // "Notepad" < "Notes" alphabetically
    }
}
