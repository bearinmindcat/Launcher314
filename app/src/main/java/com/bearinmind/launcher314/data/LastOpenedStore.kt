package com.bearinmind.launcher314.data

import android.content.Context

/**
 * Per-app "last opened" timestamps, used to rank drawer search results by
 * recency (issue #64 — the most-recently-used matching app comes first).
 *
 * We hook our OWN launch path (launchApp), so we get a perfect signal without
 * the special-access PACKAGE_USAGE_STATS permission (which needs a separate
 * user grant and triggers Play policy review). Keyed per (package, user) so
 * work/cloned profiles are tracked separately.
 */
private const val LAST_OPENED_PREFS = "drawer_last_opened"
private const val LAUNCH_COUNT_PREFS = "drawer_launch_counts"

/** Stable key for a launchable component; personal profile (null serial) -> 0. */
fun lastOpenedKey(packageName: String, userSerial: Long?): String =
    packageName + "#" + (userSerial ?: 0L)

/**
 * Record that an app was just opened: bumps its last-opened timestamp (for the
 * "recently used first" search order, #64) AND its launch count (for the
 * frecency-ranked Suggested apps card). Fire-and-forget; cheap.
 */
fun recordAppOpened(context: Context, packageName: String, userSerial: Long?) {
    try {
        val key = lastOpenedKey(packageName, userSerial)
        val app = context.applicationContext
        app.getSharedPreferences(LAST_OPENED_PREFS, Context.MODE_PRIVATE)
            .edit().putLong(key, System.currentTimeMillis()).apply()
        val counts = app.getSharedPreferences(LAUNCH_COUNT_PREFS, Context.MODE_PRIVATE)
        counts.edit().putInt(key, counts.getInt(key, 0) + 1).apply()
    } catch (_: Exception) {
    }
}

/** Forget a package's usage (all profiles) — call on uninstall so dead apps
 *  don't linger in recency/frecency. */
fun forgetAppUsage(context: Context, packageName: String) {
    try {
        val app = context.applicationContext
        for (name in listOf(LAST_OPENED_PREFS, LAUNCH_COUNT_PREFS)) {
            val prefs = app.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            prefs.all.keys.filter { it.substringBeforeLast('#') == packageName }
                .forEach { editor.remove(it) }
            editor.apply()
        }
    } catch (_: Exception) {
    }
}

/** All last-opened timestamps, keyed by [lastOpenedKey]. */
fun getLastOpenedMap(context: Context): Map<String, Long> {
    return try {
        val prefs = context.applicationContext
            .getSharedPreferences(LAST_OPENED_PREFS, Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        prefs.all.filterValues { it is Long } as Map<String, Long>
    } catch (_: Exception) {
        emptyMap()
    }
}

/** All launch counts, keyed by [lastOpenedKey]. */
fun getLaunchCountMap(context: Context): Map<String, Int> {
    return try {
        val prefs = context.applicationContext
            .getSharedPreferences(LAUNCH_COUNT_PREFS, Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        prefs.all.filterValues { it is Int } as Map<String, Int>
    } catch (_: Exception) {
        emptyMap()
    }
}

/**
 * Frecency score for the Suggested apps card: launch count decayed by how long
 * ago the app was last used. `0.95^ageDays` gives a ~13.5-day half-life — recent
 * habits dominate, but a frequently-used app stays up for a couple of weeks.
 * Never-opened apps score 0.
 */
fun frecencyScore(count: Int, lastOpenedMs: Long, nowMs: Long): Double {
    if (count <= 0 || lastOpenedMs <= 0L) return 0.0
    val ageDays = ((nowMs - lastOpenedMs).coerceAtLeast(0L)) / 86_400_000.0
    return count * Math.pow(0.95, ageDays)
}
