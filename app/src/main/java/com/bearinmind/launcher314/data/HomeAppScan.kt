package com.bearinmind.launcher314.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserManager

/** Issue #127: keeps the home screen's app list current after uninstalls/disables, and drops apps that are gone for good from the saved layout. */
object HomeAppScan {
    /** Set on any package event: the next home refresh re-reads the installed apps. */
    @Volatile var dirty = false
    @Volatile private var seq = -1

    /** True when the installed-app list may be stale (a package event, or a change the listener missed). */
    fun needsRescan(context: Context): Boolean {
        if (dirty) return true
        if (Build.VERSION.SDK_INT < 26 || seq < 0) return false
        return try { context.packageManager.getChangedPackages(seq) != null } catch (_: Exception) { false }
    }

    /** Call right before reading the installed apps; hand the result to [scanned] afterwards. */
    fun beginScan(context: Context): Int {
        dirty = false
        if (Build.VERSION.SDK_INT < 26) return -1
        return try {
            val since = seq.coerceAtLeast(0)
            context.packageManager.getChangedPackages(since)?.sequenceNumber ?: since
        } catch (_: Exception) { -1 }
    }

    fun scanned(scanSeq: Int) { seq = scanSeq }

    /** Layout apps that are gone for good: not launchable, and disabled or uninstalled (unless a profile is paused, which hides its apps). */
    fun gonePackages(context: Context, data: HomeScreenData, launchable: Set<String>): Set<String> =
        goneAmong(context, buildSet {
            data.apps.forEach { add(it.packageName) }
            data.dockApps.forEach { add(it.packageName) }
            data.folders.forEach { addAll(it.appPackageNames) }
            data.dockFolders.forEach { addAll(it.appPackageNames) }
        }, launchable)

    /** Same rule as [gonePackages] for any list of stored entries (drawer folders use it too, issue #139). */
    fun goneAmong(context: Context, entries: Collection<String>, launchable: Set<String>): Set<String> {
        val candidates = entries.toSet()
            .filter { it.isNotEmpty() && !it.startsWith("shortcut_") && !isFolderEntry(it) && it !in launchable }
        if (candidates.isEmpty()) return emptySet()
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val profilePaused = try {
            um.userProfiles.any { it != Process.myUserHandle() && um.isQuietModeEnabled(it) }
        } catch (_: Exception) { true }
        val pm = context.packageManager
        return candidates.filter { pkg ->
            try { !pm.getApplicationInfo(pkg, 0).enabled }
            catch (_: PackageManager.NameNotFoundException) { !profilePaused }
            catch (_: Exception) { false }
        }.toSet()
    }

    /** Removes [gone] apps everywhere; a top-level folder left with one app dissolves into it (as "Remove from folder" does). Null when nothing changed. */
    fun pruneLayout(data: HomeScreenData, gone: Set<String>): HomeScreenData? {
        if (gone.isEmpty()) return null
        // Issue #139: drop the entry (not blank it) so the apps after it shift back instead of leaving a hole.
        fun strip(list: List<String>) = list.filter { it !in gone }.dropLastWhile { it.isEmpty() }
        val apps = data.apps.filter { it.packageName !in gone }.toMutableList()
        val dockApps = data.dockApps.filter { it.packageName !in gone }.toMutableList()
        val folders = data.folders.mapNotNull { f ->
            if (f.appPackageNames.none { it in gone }) return@mapNotNull f
            val kept = strip(f.appPackageNames)
            val live = kept.filter { it.isNotEmpty() }
            // Nested sub-folders (page < 0) never dissolve: they have no grid cell of their own.
            if (f.page >= 0 && live.size <= 1 && live.none { isFolderEntry(it) }) {
                live.firstOrNull()?.let { apps += HomeScreenApp(it, f.position, f.page) }
                null
            } else f.copy(appPackageNames = kept)
        }
        val dockFolders = data.dockFolders.mapNotNull { f ->
            if (f.appPackageNames.none { it in gone }) return@mapNotNull f
            val kept = strip(f.appPackageNames)
            val live = kept.filter { it.isNotEmpty() }
            if (live.size <= 1 && live.none { isFolderEntry(it) }) {
                live.firstOrNull()?.let { dockApps += DockApp(it, f.position, page = f.page) }
                null
            } else f.copy(appPackageNames = kept)
        }
        val out = HomeScreenData(apps, dockApps, folders, dockFolders)
        return if (out == data) null else out
    }

    /** Issue #139: drawer folders drop [gone] apps so the rest shift back (drawer folders never dissolve). Null when nothing changed. */
    fun pruneDrawerFolders(folders: List<AppFolder>, gone: Set<String>): List<AppFolder>? {
        if (gone.isEmpty() || folders.none { f -> f.appPackageNames.any { it in gone } }) return null
        return folders.map { f ->
            if (f.appPackageNames.none { it in gone }) f
            else f.copy(appPackageNames = f.appPackageNames.filter { it !in gone }.dropLastWhile { it.isEmpty() })
        }
    }
}
