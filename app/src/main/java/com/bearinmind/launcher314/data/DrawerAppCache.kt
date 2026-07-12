package com.bearinmind.launcher314.data

import android.content.Context
import com.bearinmind.launcher314.helpers.ProfileType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cross-open + cross-process cache of the drawer's app list.
 *
 * The drawer composable (AppDrawerScreen) is disposed every time the drawer
 * closes, so its `allApps` state used to reset to empty on every open — forcing
 * a full LauncherApps re-enumeration (+ getPackageInfo + APK size per app) and
 * a loading spinner each time. On a cold start after the process was memory-
 * killed (e.g. returning from a fullscreen YouTube video on a 6GB phone) the
 * spinner lasted ~1s.
 *
 * This cache lets the drawer paint instantly:
 *  - `memory`  survives the drawer being disposed on close (lost on process death).
 *  - the JSON file survives process death (the YouTube-kill case).
 *
 * The cached list is only ever a HINT — getInstalledApps() still runs in the
 * background on open and overwrites the cache with fresh truth.
 */
object DrawerAppCache {
    @Volatile
    private var memory: List<AppInfo>? = null

    private const val FILE = "drawer_app_cache.json"

    @Serializable
    private data class CachedApp(
        val name: String,
        val packageName: String,
        val iconPath: String,
        val installTime: Long,
        val lastUpdateTime: Long,
        val sizeBytes: Long,
        val userSerial: Long?,
        // ProfileType is re-derived on the next live enumeration; stored by
        // name so a cold start still shows the right profile grouping.
        val profileType: String
    )

    /** In-memory cache, if populated (survives drawer close, not process death). */
    fun memoryApps(): List<AppInfo>? = memory

    /**
     * Read the on-disk cache (cold start) and seed `memory`. Returns null when
     * there's no usable cache yet. Cheap JSON read — call off the main thread.
     */
    fun diskApps(context: Context): List<AppInfo>? {
        memory?.let { return it }
        return try {
            val f = File(context.filesDir, FILE)
            if (!f.exists()) return null
            val cached = Json.decodeFromString<List<CachedApp>>(f.readText())
            val apps = cached.map {
                AppInfo(
                    name = it.name,
                    packageName = it.packageName,
                    iconPath = it.iconPath,
                    installTime = it.installTime,
                    lastUpdateTime = it.lastUpdateTime,
                    sizeBytes = it.sizeBytes,
                    userSerial = it.userSerial,
                    profileType = runCatching { ProfileType.valueOf(it.profileType) }
                        .getOrDefault(ProfileType.PERSONAL)
                )
            }
            memory = apps
            apps.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /** Warm `memory` from disk at process start so the first open is instant. */
    fun warm(context: Context) {
        if (memory == null) diskApps(context)
    }

    /** Update both tiers after a fresh enumeration. No write if unchanged. */
    fun update(context: Context, apps: List<AppInfo>) {
        if (memory == apps) return
        memory = apps
        try {
            val dto = apps.map {
                CachedApp(
                    it.name, it.packageName, it.iconPath, it.installTime,
                    it.lastUpdateTime, it.sizeBytes, it.userSerial, it.profileType.name
                )
            }
            File(context.filesDir, FILE).writeText(Json.encodeToString(dto))
        } catch (_: Exception) {
        }
    }
}
