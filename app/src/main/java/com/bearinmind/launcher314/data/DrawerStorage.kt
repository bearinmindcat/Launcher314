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
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val iconsDir = File(context.cacheDir, "app_icons")
    if (!iconsDir.exists()) {
        iconsDir.mkdirs()
    }

    val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

    return resolveInfoList
        .mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val iconFile = File(iconsDir, "$packageName.png")

                if (!iconFile.exists()) {
                    val drawable = packageManager.getApplicationIcon(packageName)
                    val bitmap = drawableToBitmap(drawable)
                    saveBitmapToFile(bitmap, iconFile)
                    bitmap.recycle()
                }

                // Get package info for install/update times
                val packageInfo = packageManager.getPackageInfo(packageName, 0)

                // Get app size from APK file
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val apkSize = File(appInfo.sourceDir).length()

                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    iconPath = IconPackManager.resolveIconPath(context, packageName, iconFile.absolutePath),
                    installTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime,
                    sizeBytes = apkSize
                )
            } catch (e: Exception) {
                null
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.name.lowercase() }
}
