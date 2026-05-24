package com.bearinmind.launcher314.data

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.bearinmind.launcher314.helpers.IconPackManager
import java.io.File

// Storage functions for drawer data

fun loadDrawerData(context: Context): DrawerData {
    return try {
        val file = File(context.filesDir, "drawer_data.json")
        if (file.exists()) {
            Json.decodeFromString<DrawerData>(file.readText())
        } else {
            DrawerData()
        }
    } catch (e: Exception) {
        DrawerData()
    }
}

fun saveDrawerData(context: Context, data: DrawerData) {
    try {
        val file = File(context.filesDir, "drawer_data.json")
        file.writeText(Json.encodeToString(data))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getInstalledApps(context: Context): List<AppInfo> {
    // LauncherApps-based enumeration: includes work-profile / managed /
    // cloned-profile apps. Each entry carries `userSerial` so launches go
    // to the right profile and badged icons are cached per (pkg, user).
    val packageManager = context.packageManager
    val activities = com.bearinmind.launcher314.helpers.LauncherAppsHelper.enumerateAllApps(context)

    return activities
        .mapNotNull { activity ->
            try {
                val packageName = activity.applicationInfo.packageName
                val userSerial = com.bearinmind.launcher314.helpers.LauncherAppsHelper.serialFor(context, activity.user)
                val profileType = com.bearinmind.launcher314.helpers.LauncherAppsHelper
                    .profileTypeFor(context, activity.user)
                val iconPath = com.bearinmind.launcher314.helpers.LauncherAppsHelper
                    .loadOrCacheBadgedIcon(context, activity, userSerial)

                // Install/update time + APK size — these are user-agnostic so
                // looking them up via PackageManager is still fine. Wrapped in
                // try/catch in case the package was uninstalled between the
                // LauncherApps query and this call.
                var installTime = 0L
                var updateTime = 0L
                var apkSize = 0L
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    installTime = packageInfo.firstInstallTime
                    updateTime = packageInfo.lastUpdateTime
                    apkSize = File(activity.applicationInfo.sourceDir).length()
                } catch (_: Throwable) { /* ignore — fields stay 0 */ }

                AppInfo(
                    name = activity.label.toString(),
                    packageName = packageName,
                    iconPath = IconPackManager.resolveIconPath(context, packageName, iconPath),
                    installTime = installTime,
                    lastUpdateTime = updateTime,
                    sizeBytes = apkSize,
                    userSerial = userSerial,
                    profileType = profileType
                )
            } catch (e: Exception) {
                null
            }
        }
        // De-dupe on (pkg, user) so the same app from the same profile only
        // appears once — but a work copy stays distinct from personal.
        .distinctBy { it.packageName to it.userSerial }
        .sortedBy { it.name.lowercase() }
}
