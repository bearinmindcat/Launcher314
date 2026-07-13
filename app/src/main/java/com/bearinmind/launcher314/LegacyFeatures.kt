package com.bearinmind.launcher314

/**
 * Graveyard for retired features, kept for reference only. Nothing in this file
 * is wired up anywhere — it compiles to an empty object and does nothing at
 * runtime. The original code is preserved below as comments, together with
 * where it used to hook in, so it can be resurrected or studied later.
 */
object LegacyFeatures {

    // =====================================================================
    // Fuzzy app-drawer search ("search fuzziness")
    // Retired 2026-07-13.
    //
    // What it did: an opt-in word-boundary-aware subsequence matcher for the
    // app drawer's type-to-find search — "yt" -> YouTube, "fd" -> F-Droid, etc.
    // — with accent folding, a QWERTY typo fallback, and a "Search Fuzziness"
    // strictness slider. Superseded by the classic case-insensitive substring
    // search (name.contains), which is what the drawer now always uses. The
    // "Recently used first when searching" (#64) order still applies on top.
    //
    // The matcher itself (helpers/DrawerSearchMatcher.kt) and its unit tests
    // (test/.../DrawerSearchMatcherTest.kt) are LEFT IN PLACE — dormant but
    // fully working — so the feature can be re-enabled by restoring the two
    // hook points below. The prefs (isFuzzySearchEnabled / setFuzzySearchEnabled,
    // getDrawerSearchFuzziness / setDrawerSearchFuzziness in ScreenSaveState.kt)
    // and SliderConfigs.searchFuzziness also remain.
    //
    // Where it hooked in:
    //
    // 1) ui/settings/EditDrawerSettingsScreen.kt — the toggle + slider at the
    //    top of the Additional Drawer Settings screen (removed):
    //
    //    var fuzzyEnabled by remember { mutableStateOf(isFuzzySearchEnabled(context)) }
    //    var fuzziness by remember { mutableFloatStateOf(getDrawerSearchFuzziness(context).toFloat()) }
    //    ...
    //    SettingsToggleItem(
    //        title = "Fuzzy app search",
    //        subtitle = "Find apps by initials, e.g. \"yt\" -> YouTube",
    //        checked = fuzzyEnabled,
    //        onCheckedChange = { fuzzyEnabled = it; setFuzzySearchEnabled(context, it) }
    //    )
    //    if (fuzzyEnabled) {
    //        SettingSlider(fuzziness, SliderConfigs.searchFuzziness) {
    //            fuzziness = it; setDrawerSearchFuzziness(context, it.roundToInt())
    //        }
    //    }
    //
    // 2) ui/drawer/AppDrawerScreen.kt — the state, the ON_RESUME refresh, and
    //    the branch inside the `filteredApps` derivedStateOf (removed):
    //
    //    var searchFuzziness by remember { mutableIntStateOf(getDrawerSearchFuzziness(context)) }
    //    var fuzzySearchEnabled by remember { mutableStateOf(isFuzzySearchEnabled(context)) }
    //    // (and, in the ON_RESUME LifecycleEventObserver:)
    //    searchFuzziness = getDrawerSearchFuzziness(context)
    //    fuzzySearchEnabled = isFuzzySearchEnabled(context)
    //
    //    // ...inside filteredApps, choosing the search set:
    //    val searched = when {
    //        searchQuery.isBlank() -> tabFiltered
    //        fuzzySearchEnabled ->
    //            DrawerSearchMatcher.searchApps(
    //                tabFiltered, searchQuery, searchFuzziness,
    //                if (recentFirstSearch) lastOpenedMap else emptyMap()
    //            )
    //        else -> tabFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
    //    }
    //    when {
    //        // Fuzzy results are already relevance-ranked (recency baked in).
    //        searchQuery.isNotBlank() && fuzzySearchEnabled -> searched
    //        searchQuery.isNotBlank() && recentFirstSearch -> searched.sortedWith(...)
    //        else -> /* manual sort option */
    //    }
    //
    // To resurrect: re-add both hook points above. The matcher, prefs, tests,
    // and slider config are all still present.
    // =====================================================================
}
