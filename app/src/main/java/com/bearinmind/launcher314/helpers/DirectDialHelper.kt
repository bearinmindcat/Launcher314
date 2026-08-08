package com.bearinmind.launcher314.helpers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.ContactsContract
import com.bearinmind.launcher314.data.HomeScreenApp
import com.bearinmind.launcher314.data.loadHomeScreenData
import com.bearinmind.launcher314.data.saveBitmapToFile
import com.bearinmind.launcher314.data.saveHomeScreenData
import java.io.File

/** Direct dial 1x1 — rides the "shortcut_" infra (icon PNG + .meta + HomeScreenApp) with an ACTION_CALL tel: intent. */
object DirectDialHelper {

    private const val ICON_SIZE = 192

    /** Creates the shortcut from a picked Phone data row; returns the contact name, null on failure. */
    fun createFromPhonePick(context: Context, phoneRowUri: Uri): String? {
        var name: String? = null
        var number: String? = null
        var photoUri: String? = null
        try {
            context.contentResolver.query(
                phoneRowUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    number = c.getString(0)
                    name = c.getString(1)
                    photoUri = c.getString(2)
                }
            }
        } catch (_: Exception) {
            return null
        }
        val num = number?.takeIf { it.isNotBlank() } ?: return null
        val displayName = name?.takeIf { it.isNotBlank() } ?: num

        val shortcutId = "shortcut_dial_${System.currentTimeMillis()}"
        val iconsDir = File(context.filesDir, "shortcut_icons")
        if (!iconsDir.exists()) iconsDir.mkdirs()

        val icon = loadContactPhoto(context, photoUri) ?: letterTile(displayName)
        saveBitmapToFile(icon, File(iconsDir, "$shortcutId.png"))
        icon.recycle()

        val callIntent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", num, null))
        val dialerPkg = context.packageManager
            .resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName ?: ""
        File(iconsDir, "$shortcutId.meta")
            .writeText("$displayName\n${callIntent.toUri(Intent.URI_INTENT_SCHEME)}\n$dialerPkg")

        if (!placeOnHomeScreen(context, shortcutId)) {
            File(iconsDir, "$shortcutId.png").delete()
            File(iconsDir, "$shortcutId.meta").delete()
            return null
        }
        return displayName
    }

    /** First empty cell, preferring the page the user is currently viewing. */
    private fun placeOnHomeScreen(context: Context, shortcutId: String): Boolean {
        val data = loadHomeScreenData(context)
        val prefs = context.applicationContext
            .getSharedPreferences("app_drawer_settings", Context.MODE_PRIVATE)
        val gridColumns = prefs.getInt("home_grid_columns", 4)
        val gridRows = prefs.getInt("home_grid_rows", 5)
        val totalCells = gridColumns * gridRows
        val currentPage = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            .getInt("launcher_current_page", 0)
        val placedWidgets = com.bearinmind.launcher314.ui.widgets.WidgetManager.loadPlacedWidgets(context)

        val pageOrder = listOf(currentPage) + (0..10).filter { it != currentPage }
        for (page in pageOrder) {
            val occupied = mutableSetOf<Int>()
            data.apps.filter { it.page == page }.forEach { occupied.add(it.position) }
            data.folders.filter { it.page == page }.forEach { occupied.add(it.position) }
            placedWidgets.filter { it.page == page }.forEach { w ->
                for (r in w.startRow until (w.startRow + w.rowSpan)) {
                    for (col in w.startColumn until (w.startColumn + w.columnSpan)) {
                        occupied.add(r * gridColumns + col)
                    }
                }
            }
            val empty = (0 until totalCells).firstOrNull { it !in occupied } ?: continue
            saveHomeScreenData(context, data.copy(
                apps = data.apps + HomeScreenApp(shortcutId, empty, page)
            ))
            return true
        }
        return false
    }

    private fun loadContactPhoto(context: Context, photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        return try {
            context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { input ->
                BitmapFactory.decodeStream(input)
            }?.let { src ->
                // Center-crop square then scale — the launcher's icon pipeline shapes it.
                val side = minOf(src.width, src.height)
                val cropped = Bitmap.createBitmap(
                    src, (src.width - side) / 2, (src.height - side) / 2, side, side
                )
                val scaled = Bitmap.createScaledBitmap(cropped, ICON_SIZE, ICON_SIZE, true)
                if (cropped !== src) src.recycle()
                if (scaled !== cropped) cropped.recycle()
                scaled
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Colored tile with the contact's initial — hue derived from the name so it's stable. */
    private fun letterTile(name: String): Bitmap {
        val bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val hue = ((name.hashCode().toLong() and 0xFFFFFFFFL) % 360L).toFloat()
        canvas.drawColor(Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.62f)))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = ICON_SIZE * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val initial = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#"
        val y = ICON_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initial, ICON_SIZE / 2f, y, paint)
        return bmp
    }
}
