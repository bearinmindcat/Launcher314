package com.bearinmind.launcher314.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.bearinmind.launcher314.data.HomeScreenApp
import com.bearinmind.launcher314.data.loadHomeScreenData
import com.bearinmind.launcher314.data.saveHomeScreenData
import com.bearinmind.launcher314.data.saveBitmapToFile
import java.io.File

/**
 * Receives "Add to Home Screen" shortcuts from browsers (Firefox, etc.) and other apps.
 * Firefox checks for this receiver in the manifest before showing the "Add to Home Screen" menu option.
 */
class InstallShortcutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.android.launcher.action.INSTALL_SHORTCUT") return

        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
        val launchIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT) ?: return

        Log.d("InstallShortcut", "Received shortcut: name=$name intent=$launchIntent")

        // Save the icon
        val iconBitmap = getShortcutIcon(context, intent)
        val shortcutId = "shortcut_${System.currentTimeMillis()}"
        val iconsDir = File(context.filesDir, "shortcut_icons")
        if (!iconsDir.exists()) iconsDir.mkdirs()
        val iconFile = File(iconsDir, "$shortcutId.png")
        if (iconBitmap != null) {
            saveBitmapToFile(iconBitmap, iconFile)
            iconBitmap.recycle()
        }

        // Save shortcut metadata (name + intent URI) to a simple file
        val metaFile = File(iconsDir, "$shortcutId.meta")
        metaFile.writeText("$name\n${launchIntent.toUri(Intent.URI_INTENT_SCHEME)}")

        // Add to home screen at first available position on page 0
        val data = loadHomeScreenData(context)
        val gridColumns = context.applicationContext.getSharedPreferences("app_drawer_settings", Context.MODE_PRIVATE)
            .getInt("home_grid_columns", 4)
        val gridRows = context.applicationContext.getSharedPreferences("app_drawer_settings", Context.MODE_PRIVATE)
            .getInt("home_grid_rows", 5)
        val totalCells = gridColumns * gridRows

        // Find first truly empty cell across all pages
        val placedWidgets = com.bearinmind.launcher314.ui.widgets.WidgetManager.loadPlacedWidgets(context)
        var targetPage = 0
        var targetPosition = 0
        for (page in 0..10) {
            val occupiedByApps = data.apps.filter { it.page == page }.map { it.position }.toSet()
            val occupiedByFolders = data.folders.filter { it.page == page }.map { it.position }.toSet()
            val occupiedByWidgets = mutableSetOf<Int>()
            placedWidgets.filter { it.page == page }.forEach { widget ->
                for (r in widget.startRow until (widget.startRow + widget.rowSpan)) {
                    for (c in widget.startColumn until (widget.startColumn + widget.columnSpan)) {
                        occupiedByWidgets.add(r * gridColumns + c)
                    }
                }
            }
            val allOccupied = occupiedByApps + occupiedByFolders + occupiedByWidgets
            val empty = (0 until totalCells).firstOrNull { it !in allOccupied }
            if (empty != null) {
                targetPage = page
                targetPosition = empty
                break
            }
        }

        val newApp = HomeScreenApp(
            packageName = shortcutId,
            position = targetPosition,
            page = targetPage
        )

        val updatedData = data.copy(apps = data.apps + newApp)
        saveHomeScreenData(context, updatedData)

        Log.d("InstallShortcut", "Added shortcut '$name' at page $targetPage position $targetPosition with id $shortcutId")
    }

    private fun getShortcutIcon(context: Context, intent: Intent): Bitmap? {
        // Try direct bitmap extra first
        val iconBitmap = intent.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
        if (iconBitmap != null) return iconBitmap

        // Try icon resource
        val iconResource = intent.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
        if (iconResource != null) {
            try {
                val resources = context.packageManager.getResourcesForApplication(iconResource.packageName)
                val id = resources.getIdentifier(iconResource.resourceName, null, null)
                if (id != 0) {
                    val drawable = resources.getDrawable(id, null)
                    if (drawable is BitmapDrawable) return drawable.bitmap
                }
            } catch (_: Exception) {}
        }

        return null
    }
}
