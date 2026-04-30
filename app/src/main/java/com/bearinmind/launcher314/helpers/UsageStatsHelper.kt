package com.bearinmind.launcher314.helpers

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Backs the Recent Apps overlay (issue #40).
 *
 * Wraps Android's [UsageStatsManager] for "what apps has the user been using
 * lately" queries. The required permission ([PACKAGE_USAGE_STATS]) is a
 * "special" permission — there's no runtime dialog. The user has to navigate
 * Settings → Apps → Special access → Usage access manually. We expose a
 * [hasUsageAccess] check + an [openUsageAccessSettings] helper that fires the
 * standard intent so the overlay can show a "Grant access" button.
 */
object UsageStatsHelper {

    enum class Sort { Recency, Frequency }

    data class RecentAppEntry(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?,
        val lastTimeUsedMs: Long,
        val totalTimeForegroundMs: Long
    )

    /**
     * True if our process holds GET_USAGE_STATS at the AppOps layer. We check
     * via AppOpsManager rather than `checkSelfPermission` because the
     * special-permission grant only updates the AppOps state.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = try {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } catch (_: Throwable) {
            // Older devices: fall back to deprecated checkOpNoThrow.
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Fire the system Settings activity for the user to grant Usage Access. */
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Some devices don't expose the screen; bail silently.
        }
    }

    /**
     * Query recently-used apps from the system. Returns at most [limit]
     * entries, sorted per [sort], skipping our own package and packages
     * without a launcher intent. Window is the last 7 days.
     */
    fun queryRecentApps(
        context: Context,
        sort: Sort,
        limit: Int
    ): List<RecentAppEntry> {
        if (!hasUsageAccess(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val end = System.currentTimeMillis()
        val begin = end - 7L * 24L * 60L * 60L * 1000L  // 7 days
        val raw = try {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end)
        } catch (_: Throwable) {
            return emptyList()
        } ?: return emptyList()

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchablePackages: Set<String> = pm.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toHashSet()

        // Coalesce per package — UsageStatsManager can return multiple buckets per
        // package within the window; take the latest lastTimeUsed and sum the
        // totalTimeInForeground.
        val byPackage = HashMap<String, Pair<Long, Long>>() // pkg -> (lastTimeUsed, totalForegroundMs)
        for (us in raw) {
            val pkg = us.packageName ?: continue
            if (pkg == context.packageName) continue
            if (pkg !in launchablePackages) continue
            val prev = byPackage[pkg]
            val newLast = if (prev == null) us.lastTimeUsed else maxOf(prev.first, us.lastTimeUsed)
            val newSum = if (prev == null) us.totalTimeInForeground else prev.second + us.totalTimeInForeground
            byPackage[pkg] = newLast to newSum
        }

        val entries = byPackage.entries.mapNotNull { (pkg, pair) ->
            val (last, total) = pair
            if (last <= 0L) return@mapNotNull null
            val label = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                catch (_: Exception) { pkg }
            val icon = try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }
            RecentAppEntry(
                packageName = pkg,
                label = label,
                icon = icon,
                lastTimeUsedMs = last,
                totalTimeForegroundMs = total
            )
        }

        val sorted = when (sort) {
            Sort.Recency -> entries.sortedByDescending { it.lastTimeUsedMs }
            Sort.Frequency -> entries.sortedByDescending { it.totalTimeForegroundMs }
        }
        return sorted.take(limit)
    }
}
