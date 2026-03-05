package com.bearinmind.launcher314.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

// Storage functions for home screen data

fun loadHomeScreenData(context: Context): HomeScreenData {
    return try {
        val file = File(context.filesDir, "home_screen_data.json")
        if (file.exists()) {
            Json.decodeFromString<HomeScreenData>(file.readText())
        } else {
            HomeScreenData()
        }
    } catch (e: Exception) {
        HomeScreenData()
    }
}

fun saveHomeScreenData(context: Context, data: HomeScreenData) {
    try {
        val file = File(context.filesDir, "home_screen_data.json")
        file.writeText(Json.encodeToString(data))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadAvailableApps(context: Context): List<HomeAppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val iconsDir = File(context.cacheDir, "app_icons")
    if (!iconsDir.exists()) {
        iconsDir.mkdirs()
    }

    val resolveInfoList = packageManager.queryIntentActivities(intent, 0)

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

                HomeAppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    iconPath = iconFile.absolutePath
                )
            } catch (e: Exception) {
                null
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.name.lowercase() }
}

// Shared bitmap utility functions (used by home, drawer, and preview)

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}
