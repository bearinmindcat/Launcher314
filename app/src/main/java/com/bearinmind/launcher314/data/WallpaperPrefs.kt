package com.bearinmind.launcher314.data

import android.content.Context
import java.io.File

private const val WP_PREFS_NAME = "wallpaper_settings"
private const val WP_KEY_MODE = "wallpaper_mode"                 // "system" | "custom"
private const val WP_KEY_CUSTOM_PATH = "wallpaper_custom_path"   // absolute path in app storage
private const val WP_KEY_DIM = "wallpaper_dim"                   // 0-100 darkening percent
private const val WP_KEY_BLUR = "wallpaper_blur"                 // 0-100 blur amount
private const val WP_KEY_CACHE_VERSION = "wallpaper_cache_version" // bumps when file rewritten

const val WALLPAPER_MODE_SYSTEM = "system"
const val WALLPAPER_MODE_CUSTOM = "custom"

/** Step 2 mode — user edited and committed an actual Android system wallpaper via WallpaperManager. */
const val WALLPAPER_MODE_DEVICE = "device"

// Device wallpaper keys (Step 2)
private const val WP_KEY_DEVICE_SOURCE_PATH = "wallpaper_device_source_path"
private const val WP_KEY_DEVICE_EDIT_SCALE = "wallpaper_device_edit_scale"           // float, stored x1000
private const val WP_KEY_DEVICE_EDIT_OFFSET_X = "wallpaper_device_edit_offset_x"     // float, stored x10
private const val WP_KEY_DEVICE_EDIT_OFFSET_Y = "wallpaper_device_edit_offset_y"     // float, stored x10
private const val WP_KEY_DEVICE_EDIT_ROTATION = "wallpaper_device_edit_rotation"     // 0 / 90 / 180 / 270
private const val WP_KEY_DEVICE_EDIT_FLIP_H = "wallpaper_device_edit_flip_h"         // bool
private const val WP_KEY_DEVICE_EDIT_FLIP_V = "wallpaper_device_edit_flip_v"         // bool
// v2 keys: scale unified to -100..+100 with 0 as the neutral default. Old v1
// keys (0..100 with 50 neutral) are intentionally NOT read so prior saves
// don't surface as pre-activated effects at the new neutral (0).
private const val WP_KEY_DEVICE_EDIT_BRIGHTNESS = "wallpaper_device_edit_brightness_v2"
private const val WP_KEY_DEVICE_EDIT_CONTRAST = "wallpaper_device_edit_contrast_v2"
private const val WP_KEY_DEVICE_EDIT_SATURATION = "wallpaper_device_edit_saturation_v2"
private const val WP_KEY_DEVICE_EDIT_BLUR = "wallpaper_device_edit_blur"             // int 0..100
private const val WP_KEY_DEVICE_EDIT_VIGNETTE = "wallpaper_device_edit_vignette"     // int 0..100
private const val WP_KEY_DEVICE_EDIT_FILTER = "wallpaper_device_edit_filter"         // "none" | "grayscale" | "sepia" | "invert"
private const val WP_KEY_DEVICE_EDIT_CROP_L = "wallpaper_device_edit_crop_l"         // float 0..1 stored x10000
private const val WP_KEY_DEVICE_EDIT_CROP_T = "wallpaper_device_edit_crop_t"
private const val WP_KEY_DEVICE_EDIT_CROP_R = "wallpaper_device_edit_crop_r"
private const val WP_KEY_DEVICE_EDIT_CROP_B = "wallpaper_device_edit_crop_b"
private const val WP_KEY_DEVICE_EDIT_EXPOSURE = "wallpaper_device_edit_exposure_v2"
private const val WP_KEY_DEVICE_EDIT_HIGHLIGHTS = "wallpaper_device_edit_highlights_v2"
private const val WP_KEY_DEVICE_EDIT_SHADOWS = "wallpaper_device_edit_shadows_v2"
private const val WP_KEY_DEVICE_EDIT_TINT = "wallpaper_device_edit_tint_v2"
private const val WP_KEY_DEVICE_EDIT_TEMPERATURE = "wallpaper_device_edit_temperature_v2"
private const val WP_KEY_DEVICE_EDIT_SHARPNESS = "wallpaper_device_edit_sharpness_v2"

const val WP_FILTER_NONE = "none"
const val WP_FILTER_GRAYSCALE = "grayscale"      // labelled "Mono" in the UI — uses the enhanced BT.709-weighted matrix
const val WP_FILTER_SEPIA = "sepia"
const val WP_FILTER_INVERT = "invert"             // retained for saved-state compat; not exposed in the pager
const val WP_FILTER_WARM = "warm"
const val WP_FILTER_COOL = "cool"
const val WP_FILTER_VIVID = "vivid"
const val WP_FILTER_FADED = "faded"
const val WP_FILTER_POLAROID = "polaroid"
const val WP_FILTER_KODACHROME = "kodachrome"
const val WP_FILTER_VINTAGE = "vintage"
const val WP_FILTER_TEAL_ORANGE = "teal_orange"
const val WP_FILTER_NIGHT = "night"

data class DeviceWallpaperEdit(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Int = 0,      // 0, 90, 180, 270
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    // Crop rectangle in normalized image coordinates (0..1). Default = full image.
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    // All color/tonal effects use a unified -100..+100 range with 0 as the
    // neutral / default value. Negative reduces the effect, positive enhances
    // it. Color-matrix math uses `value / 100f` as the bipolar strength
    // (-1..+1) so 0 = identity, +100 = max positive, -100 = max negative.
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val exposure: Int = 0,
    val highlights: Int = 0,
    val shadows: Int = 0,
    val tint: Int = 0,                  // - = green, + = magenta
    val temperature: Int = 0,           // - = cool, + = warm
    val sharpness: Int = 0,             // - = softer, + = sharper
    // --- Retained for backwards compat with older saved state ---
    val blur: Int = 0,                  // 0..100
    val vignette: Int = 0,              // 0..100
    val filter: String = WP_FILTER_NONE
)

fun getWallpaperMode(context: Context): String {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(WP_KEY_MODE, WALLPAPER_MODE_SYSTEM) ?: WALLPAPER_MODE_SYSTEM
}

fun setWallpaperMode(context: Context, mode: String) {
    context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(WP_KEY_MODE, mode).apply()
}

fun getCustomWallpaperPath(context: Context): String? {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    val path = prefs.getString(WP_KEY_CUSTOM_PATH, null) ?: return null
    return if (File(path).exists()) path else null
}

fun setCustomWallpaperPath(context: Context, path: String?) {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        if (path == null) remove(WP_KEY_CUSTOM_PATH) else putString(WP_KEY_CUSTOM_PATH, path)
        apply()
    }
}

fun getWallpaperDim(context: Context): Int {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(WP_KEY_DIM, 0).coerceIn(0, 100)
}

fun setWallpaperDim(context: Context, value: Int) {
    context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putInt(WP_KEY_DIM, value.coerceIn(0, 100)).apply()
}

fun getWallpaperBlur(context: Context): Int {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(WP_KEY_BLUR, 0).coerceIn(0, 100)
}

fun setWallpaperBlur(context: Context, value: Int) {
    context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putInt(WP_KEY_BLUR, value.coerceIn(0, 100)).apply()
}

fun getWallpaperCacheVersion(context: Context): Int {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(WP_KEY_CACHE_VERSION, 0)
}

fun bumpWallpaperCacheVersion(context: Context) {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    val v = prefs.getInt(WP_KEY_CACHE_VERSION, 0) + 1
    prefs.edit().putInt(WP_KEY_CACHE_VERSION, v).apply()
}

/** Returns the app-private file where custom wallpapers are persisted. */
fun getCustomWallpaperFile(context: Context): File {
    val dir = File(context.filesDir, "wallpapers")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "custom_wallpaper.jpg")
}

// ========== Device wallpaper (Step 2) ==========

fun getDeviceWallpaperSourceFile(context: Context): File {
    val dir = File(context.filesDir, "wallpapers")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "device_wallpaper_source.jpg")
}

fun getDeviceWallpaperSourcePath(context: Context): String? {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    val path = prefs.getString(WP_KEY_DEVICE_SOURCE_PATH, null) ?: return null
    return if (File(path).exists()) path else null
}

fun setDeviceWallpaperSourcePath(context: Context, path: String?) {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        if (path == null) remove(WP_KEY_DEVICE_SOURCE_PATH) else putString(WP_KEY_DEVICE_SOURCE_PATH, path)
        apply()
    }
}

fun getDeviceWallpaperEdit(context: Context): DeviceWallpaperEdit {
    val prefs = context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE)
    return DeviceWallpaperEdit(
        scale = prefs.getInt(WP_KEY_DEVICE_EDIT_SCALE, 1000) / 1000f,
        offsetX = prefs.getInt(WP_KEY_DEVICE_EDIT_OFFSET_X, 0) / 10f,
        offsetY = prefs.getInt(WP_KEY_DEVICE_EDIT_OFFSET_Y, 0) / 10f,
        rotationDegrees = prefs.getInt(WP_KEY_DEVICE_EDIT_ROTATION, 0),
        flipH = prefs.getBoolean(WP_KEY_DEVICE_EDIT_FLIP_H, false),
        flipV = prefs.getBoolean(WP_KEY_DEVICE_EDIT_FLIP_V, false),
        brightness = prefs.getInt(WP_KEY_DEVICE_EDIT_BRIGHTNESS, 0).coerceIn(-100, 100),
        contrast = prefs.getInt(WP_KEY_DEVICE_EDIT_CONTRAST, 0).coerceIn(-100, 100),
        saturation = prefs.getInt(WP_KEY_DEVICE_EDIT_SATURATION, 0).coerceIn(-100, 100),
        blur = prefs.getInt(WP_KEY_DEVICE_EDIT_BLUR, 0).coerceIn(0, 100),
        vignette = prefs.getInt(WP_KEY_DEVICE_EDIT_VIGNETTE, 0).coerceIn(0, 100),
        filter = prefs.getString(WP_KEY_DEVICE_EDIT_FILTER, WP_FILTER_NONE) ?: WP_FILTER_NONE,
        cropLeft = (prefs.getInt(WP_KEY_DEVICE_EDIT_CROP_L, 0) / 10000f).coerceIn(0f, 1f),
        cropTop = (prefs.getInt(WP_KEY_DEVICE_EDIT_CROP_T, 0) / 10000f).coerceIn(0f, 1f),
        cropRight = (prefs.getInt(WP_KEY_DEVICE_EDIT_CROP_R, 10000) / 10000f).coerceIn(0f, 1f),
        cropBottom = (prefs.getInt(WP_KEY_DEVICE_EDIT_CROP_B, 10000) / 10000f).coerceIn(0f, 1f),
        exposure = prefs.getInt(WP_KEY_DEVICE_EDIT_EXPOSURE, 0).coerceIn(-100, 100),
        highlights = prefs.getInt(WP_KEY_DEVICE_EDIT_HIGHLIGHTS, 0).coerceIn(-100, 100),
        shadows = prefs.getInt(WP_KEY_DEVICE_EDIT_SHADOWS, 0).coerceIn(-100, 100),
        tint = prefs.getInt(WP_KEY_DEVICE_EDIT_TINT, 0).coerceIn(-100, 100),
        temperature = prefs.getInt(WP_KEY_DEVICE_EDIT_TEMPERATURE, 0).coerceIn(-100, 100),
        sharpness = prefs.getInt(WP_KEY_DEVICE_EDIT_SHARPNESS, 0).coerceIn(-100, 100)
    )
}

fun setDeviceWallpaperEdit(context: Context, edit: DeviceWallpaperEdit) {
    context.getSharedPreferences(WP_PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putInt(WP_KEY_DEVICE_EDIT_SCALE, (edit.scale * 1000).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_OFFSET_X, (edit.offsetX * 10).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_OFFSET_Y, (edit.offsetY * 10).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_ROTATION, edit.rotationDegrees)
        .putBoolean(WP_KEY_DEVICE_EDIT_FLIP_H, edit.flipH)
        .putBoolean(WP_KEY_DEVICE_EDIT_FLIP_V, edit.flipV)
        .putInt(WP_KEY_DEVICE_EDIT_BRIGHTNESS, edit.brightness.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_CONTRAST, edit.contrast.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_SATURATION, edit.saturation.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_BLUR, edit.blur.coerceIn(0, 100))
        .putInt(WP_KEY_DEVICE_EDIT_VIGNETTE, edit.vignette.coerceIn(0, 100))
        .putString(WP_KEY_DEVICE_EDIT_FILTER, edit.filter)
        .putInt(WP_KEY_DEVICE_EDIT_CROP_L, (edit.cropLeft * 10000).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_CROP_T, (edit.cropTop * 10000).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_CROP_R, (edit.cropRight * 10000).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_CROP_B, (edit.cropBottom * 10000).toInt())
        .putInt(WP_KEY_DEVICE_EDIT_EXPOSURE, edit.exposure.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_HIGHLIGHTS, edit.highlights.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_SHADOWS, edit.shadows.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_TINT, edit.tint.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_TEMPERATURE, edit.temperature.coerceIn(-100, 100))
        .putInt(WP_KEY_DEVICE_EDIT_SHARPNESS, edit.sharpness.coerceIn(-100, 100))
        .apply()
}
