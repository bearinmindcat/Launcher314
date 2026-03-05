package com.bearinmind.launcher314.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Triggers the system uninstall dialog for the specified package.
 * Uses ACTION_DELETE which is the standard approach across all Android versions.
 */
fun uninstallApp(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Unable to uninstall app", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens the system app info/settings screen for the specified package.
 */
fun openAppInfo(context: Context, packageName: String) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Opens the system wallpaper picker.
 */
fun openWallpaperPicker(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
    } catch (e: Exception) {
        // Fallback to system wallpaper settings
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Handle error silently
        }
    }
}

/**
 * Opens the widget picker.
 */
fun openWidgetPicker(context: Context) {
    try {
        val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e3: Exception) {
                // Handle error silently
            }
        }
    }
}
