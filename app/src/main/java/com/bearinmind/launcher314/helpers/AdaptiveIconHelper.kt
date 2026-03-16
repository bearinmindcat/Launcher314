package com.bearinmind.launcher314.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import com.bearinmind.launcher314.data.drawableToBitmap
import com.bearinmind.launcher314.data.saveBitmapToFile
import java.io.File

private const val ICON_SIZE = 192

fun getGlobalShapedDir(context: Context): File {
    val dir = File(context.filesDir, "global_shaped_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun clearGlobalShapedIcons(context: Context) {
    val dir = File(context.filesDir, "global_shaped_icons")
    if (dir.exists()) dir.deleteRecursively()
}

fun getOrGenerateGlobalShapedIcon(context: Context, packageName: String, shapeName: String): String {
    val dir = getGlobalShapedDir(context)
    val file = File(dir, "$packageName.png")
    if (file.exists()) return file.absolutePath
    return generateShapedIcon(context, packageName, shapeName, file)
}

fun getShapedExpDir(context: Context): File {
    val dir = File(context.filesDir, "shaped_exp_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun deleteShapedIcon(context: Context, packageName: String) {
    val file = File(getShapedExpDir(context), "$packageName.png")
    if (file.exists()) file.delete()
}

fun generateShapedIcon(context: Context, packageName: String, shapeName: String): String {
    val outFile = File(getShapedExpDir(context), "$packageName.png")
    return generateShapedIcon(context, packageName, shapeName, outFile)
}

private fun generateShapedIcon(context: Context, packageName: String, shapeName: String, outFile: File, bgTintColor: Int? = null, bgTintAlpha: Float = 1f): String {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    val result = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val shapePath = createShapePath(shapeName, ICON_SIZE.toFloat())
    canvas.clipPath(shapePath)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
        // Draw bg+fg separately to avoid the system's built-in icon mask
        val bg = drawable.background
        val fg = drawable.foreground
        if (bgTintColor != null) {
            val alphaInt = (bgTintAlpha * 255).toInt()
            bg?.colorFilter = android.graphics.PorterDuffColorFilter(
                (bgTintColor and 0x00FFFFFF) or (alphaInt shl 24),
                android.graphics.PorterDuff.Mode.SRC_ATOP
            )
        }
        // Expand both layers to 1.5x (replicates AdaptiveIconDrawable's internal
        // layer expansion). Adaptive icon layers are 108dp but only the inner 72dp
        // (66.7%) is visible. At 1.5x, the fg content fills our canvas correctly.
        val layerSize = (ICON_SIZE * 1.5f).toInt()
        val offset = (ICON_SIZE - layerSize) / 2
        bg?.setBounds(offset, offset, offset + layerSize, offset + layerSize)
        fg?.setBounds(offset, offset, offset + layerSize, offset + layerSize)
        bg?.draw(canvas)
        fg?.draw(canvas)
        bg?.colorFilter = null
    } else {
        // Non-adaptive icon: extract bg color, fill shape, draw icon centered
        val iconBitmap = drawableToBitmap(drawable)
        val bgColor = extractDominantColor(iconBitmap)
        iconBitmap.recycle()

        val bgPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawPath(shapePath, bgPaint)

        // Draw the icon directly at 75% size, centered — setBounds normalizes
        // any icon to exact pixel bounds regardless of native size
        val padding = (ICON_SIZE * 0.125f).toInt()
        drawable.setBounds(padding, padding, ICON_SIZE - padding, ICON_SIZE - padding)
        drawable.draw(canvas)
    }

    saveBitmapToFile(result, outFile)
    result.recycle()
    return outFile.absolutePath
}

fun getShapedBgTintedDir(context: Context): File {
    val dir = File(context.filesDir, "shaped_bg_tinted_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun deleteShapedBgTintedIcon(context: Context, packageName: String) {
    val file = File(getShapedBgTintedDir(context), "$packageName.png")
    if (file.exists()) file.delete()
}

/**
 * Generate icon with BOTH shape mask AND background-only tint.
 * Applies tint to the bg layer of AdaptiveIconDrawable, then clips to shape.
 */
fun generateShapedBgTintedIcon(context: Context, packageName: String, shapeName: String, tintColor: Int, tintAlpha: Float): String {
    val outFile = File(getShapedBgTintedDir(context), "$packageName.png")
    return generateShapedIcon(context, packageName, shapeName, outFile, bgTintColor = tintColor, bgTintAlpha = tintAlpha)
}


private fun createShapePath(shapeName: String, size: Float): Path {
    return when (shapeName) {
        IconShapes.CIRCLE -> Path().apply {
            addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        }
        IconShapes.ROUNDED_SQUARE -> Path().apply {
            val radius = size * 0.08f
            addRoundRect(RectF(0f, 0f, size, size), radius, radius, Path.Direction.CW)
        }
        IconShapes.SQUIRCLE -> buildBezierPath(size, 0.2f)
        IconShapes.TEARDROP -> createTeardropPath(size)
        IconShapes.SAMMY -> buildBezierPath(size, 0.4431717f, 0.14010102f)
        IconShapes.CUPERTINO -> createCupertinoPath(size)
        else -> Path().apply {
            addRect(RectF(0f, 0f, size, size), Path.Direction.CW)
        }
    }
}

// Builds a shape using cubic Bezier corners (matches Lawnchair/Neo Launcher approach)
private fun buildBezierPath(size: Float, cdX: Float, cdY: Float = cdX): Path {
    val hw = size / 2f
    val path = Path()
    // Start at top center
    path.moveTo(hw, 0f)
    // Top-right corner
    path.cubicTo(hw + hw * (1f - cdX), 0f, size, hw - hw * (1f - cdY), size, hw)
    // Bottom-right corner
    path.cubicTo(size, hw + hw * (1f - cdY), hw + hw * (1f - cdX), size, hw, size)
    // Bottom-left corner
    path.cubicTo(hw - hw * (1f - cdX), size, 0f, hw + hw * (1f - cdY), 0f, hw)
    // Top-left corner
    path.cubicTo(0f, hw - hw * (1f - cdY), hw - hw * (1f - cdX), 0f, hw, 0f)
    path.close()
    return path
}

private fun createTeardropPath(size: Float): Path {
    val path = Path()
    val r = size * 0.35f
    path.moveTo(size, 0f)
    path.lineTo(size, size - r)
    path.arcTo(RectF(size - 2 * r, size - 2 * r, size, size), 0f, 90f)
    path.lineTo(r, size)
    path.arcTo(RectF(0f, size - 2 * r, 2 * r, size), 90f, 90f)
    path.lineTo(0f, r)
    path.arcTo(RectF(0f, 0f, 2 * r, 2 * r), 180f, 90f)
    path.close()
    return path
}

// iOS Cupertino — pre-computed Bezier points from Lawnchair source
private fun createCupertinoPath(size: Float): Path {
    val s = size
    val path = Path()
    path.moveTo(s * 0.5f, 0f)
    path.cubicTo(s * 0.6039f, 0f, s * 0.712f, 0f, s * 0.83f, s * 0.0342f)
    path.cubicTo(s * 0.83f, s * 0.0342f, s * 0.9658f, s * 0.17f, s * 1f, s * 0.17f)
    path.cubicTo(s * 1f, s * 0.288f, s * 1f, s * 0.3961f, s * 1f, s * 0.5f)
    path.cubicTo(s * 1f, s * 0.6039f, s * 1f, s * 0.712f, s * 0.9658f, s * 0.83f)
    path.cubicTo(s * 0.9658f, s * 0.83f, s * 0.83f, s * 0.9658f, s * 0.83f, s * 0.9658f)
    path.cubicTo(s * 0.712f, s * 1f, s * 0.6039f, s * 1f, s * 0.5f, s * 1f)
    path.cubicTo(s * 0.3961f, s * 1f, s * 0.288f, s * 1f, s * 0.17f, s * 0.9658f)
    path.cubicTo(s * 0.17f, s * 0.9658f, s * 0.0342f, s * 0.83f, s * 0.0342f, s * 0.83f)
    path.cubicTo(0f, s * 0.712f, 0f, s * 0.6039f, 0f, s * 0.5f)
    path.cubicTo(0f, s * 0.3961f, 0f, s * 0.288f, s * 0.0342f, s * 0.17f)
    path.cubicTo(s * 0.0342f, s * 0.17f, s * 0.17f, s * 0.0342f, s * 0.17f, s * 0.0342f)
    path.cubicTo(s * 0.288f, 0f, s * 0.3961f, 0f, s * 0.5f, 0f)
    path.close()
    return path
}

fun getForegroundIconsDir(context: Context): File {
    val dir = File(context.filesDir, "foreground_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

/**
 * Extract ONLY the foreground layer of an adaptive icon, drawn on a transparent background.
 * Returns null for legacy (non-adaptive) icons where layers can't be separated.
 */
fun getOrGenerateForegroundIcon(context: Context, packageName: String): String? {
    val dir = getForegroundIconsDir(context)
    val file = File(dir, "$packageName.png")
    if (file.exists()) return file.absolutePath

    val drawable = context.packageManager.getApplicationIcon(packageName)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || drawable !is AdaptiveIconDrawable) {
        return null
    }

    val fg = drawable.foreground ?: return null
    val result = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    fg.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
    fg.draw(canvas)

    saveBitmapToFile(result, file)
    result.recycle()
    return file.absolutePath
}

fun getBgColorShapedDir(context: Context): File {
    val dir = File(context.filesDir, "bg_color_shaped_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun clearBgColorShapedIcons(context: Context) {
    val dir = File(context.filesDir, "bg_color_shaped_icons")
    if (dir.exists()) dir.deleteRecursively()
}

fun getOrGenerateBgColorShapedIcon(context: Context, packageName: String, shapeName: String, bgColor: Int): String {
    val dir = getBgColorShapedDir(context)
    val file = File(dir, "$packageName.png")
    if (file.exists()) return file.absolutePath
    return generateBgColorShapedIcon(context, packageName, shapeName, bgColor)
}

/**
 * Generate shaped icon with user-chosen solid background color replacing the icon's
 * original background layer. Only the foreground layer of adaptive icons is preserved.
 * Must separate layers manually since the native bg is replaced by user color.
 */
private fun generateBgColorShapedIcon(context: Context, packageName: String, shapeName: String, bgColor: Int): String {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    val result = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val shapePath = createShapePath(shapeName, ICON_SIZE.toFloat())
    canvas.clipPath(shapePath)

    // Fill the shape with the user's chosen background color
    val bgPaint = Paint().apply {
        color = bgColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawPath(shapePath, bgPaint)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
        // Draw only the foreground layer at 1.5x (bg replaced by user color)
        val fg = drawable.foreground
        val layerSize = (ICON_SIZE * 1.5f).toInt()
        val offset = (ICON_SIZE - layerSize) / 2
        fg?.setBounds(offset, offset, offset + layerSize, offset + layerSize)
        fg?.draw(canvas)
    } else {
        // Legacy icon: draw icon centered on colored background
        val padding = (ICON_SIZE * 0.125f).toInt()
        drawable.setBounds(padding, padding, ICON_SIZE - padding, ICON_SIZE - padding)
        drawable.draw(canvas)
    }

    val outFile = File(getBgColorShapedDir(context), "$packageName.png")
    saveBitmapToFile(result, outFile)
    result.recycle()
    return outFile.absolutePath
}

fun getBgTintedDir(context: Context): File {
    val dir = File(context.filesDir, "bg_tinted_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun deleteBgTintedIcon(context: Context, packageName: String) {
    val file = File(getBgTintedDir(context), "$packageName.png")
    if (file.exists()) file.delete()
}

/**
 * Generate an icon with tint applied only to the background layer of an AdaptiveIconDrawable.
 * The foreground is drawn untinted on top. For non-adaptive icons, falls back to full tint.
 */
fun generateBgTintedIcon(context: Context, packageName: String, tintColor: Int, tintAlpha: Float): String {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    val result = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
        // Apply tint to background layer, then draw the full AdaptiveIconDrawable
        // so the system's default icon mask is preserved
        val bg = drawable.background
        val alphaInt = (tintAlpha * 255).toInt()
        bg?.colorFilter = android.graphics.PorterDuffColorFilter(
            (tintColor and 0x00FFFFFF) or (alphaInt shl 24),
            android.graphics.PorterDuff.Mode.SRC_ATOP
        )

        drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
        drawable.draw(canvas)

        // Clear the filter so we don't affect the original drawable
        bg?.colorFilter = null
    } else {
        // Legacy icon: just draw normally (can't separate layers)
        val iconBitmap = drawableToBitmap(drawable)
        val scaled = Bitmap.createScaledBitmap(iconBitmap, ICON_SIZE, ICON_SIZE, true)
        canvas.drawBitmap(scaled, 0f, 0f, null)
        scaled.recycle()
        iconBitmap.recycle()
    }

    val outFile = File(getBgTintedDir(context), "$packageName.png")
    saveBitmapToFile(result, outFile)
    result.recycle()
    return outFile.absolutePath
}

private fun extractDominantColor(bitmap: Bitmap): Int {
    val w = bitmap.width
    val h = bitmap.height
    val colorCounts = mutableMapOf<Int, Int>()

    // Count all fully opaque pixels only (alpha == 255)
    // This skips Samsung's anti-aliased squircle mask border entirely
    val step = maxOf(w / 32, 1)
    for (x in 0 until w step step) {
        for (y in 0 until h step step) {
            val pixel = bitmap.getPixel(x, y)
            if (((pixel shr 24) and 0xFF) == 0xFF) {
                // Quantize to reduce noise — group similar colors together
                val quantized = (pixel and 0xFFF0F0F0.toInt()) or 0xFF000000.toInt()
                colorCounts[quantized] = (colorCounts[quantized] ?: 0) + 1
            }
        }
    }

    // Most common fully-opaque color = background (covers more area than any foreground color)
    return colorCounts.maxByOrNull { it.value }?.key ?: 0xFF424242.toInt()
}

