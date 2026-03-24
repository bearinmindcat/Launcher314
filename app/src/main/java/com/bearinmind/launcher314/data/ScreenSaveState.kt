package com.bearinmind.launcher314.data

import android.content.Context
import kotlin.math.roundToInt

// SharedPreferences keys and defaults fo   r drawer grid settings
private const val PREFS_NAME = "app_drawer_settings"
private const val KEY_GRID_SIZE = "grid_size"
private const val KEY_ICON_SIZE = "icon_size"
private const val KEY_SIZE_LINKED = "size_linked"
private const val KEY_SCROLLBAR_WIDTH = "scrollbar_width"
private const val KEY_SCROLLBAR_HEIGHT = "scrollbar_height"
private const val KEY_SCROLLBAR_COLOR = "scrollbar_color"
private const val KEY_SCROLLBAR_INTENSITY = "scrollbar_intensity"
private const val KEY_ICON_SIZE_PERCENT = "drawer_icon_size_percent"
private const val KEY_SCROLLBAR_WIDTH_PERCENT = "scrollbar_width_percent"
private const val KEY_SCROLLBAR_HEIGHT_PERCENT = "scrollbar_height_percent"
private const val KEY_DRAWER_GRID_ROWS = "drawer_grid_rows"
private const val KEY_DRAWER_PAGED_MODE = "drawer_paged_mode"
private const val KEY_ICON_TEXT_SIZE_PERCENT = "icon_text_size_percent"
private const val KEY_SELECTED_FONT = "selected_font_id"
private const val KEY_IMPORTED_FONTS = "imported_font_paths"
private const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
private const val DEFAULT_SELECTED_ICON_PACK = ""  // Empty = system icons
private const val KEY_GLOBAL_ICON_SHAPE = "global_icon_shape"
private const val KEY_GLOBAL_ICON_BG_COLOR = "global_icon_bg_color"
private const val KEY_GLOBAL_ICON_BG_INTENSITY = "global_icon_bg_intensity"

private const val DEFAULT_GRID_SIZE = 4
private const val DEFAULT_ICON_SIZE = 48
private const val DEFAULT_LINKED = true
private const val DEFAULT_SCROLLBAR_WIDTH = 8
private const val DEFAULT_SCROLLBAR_HEIGHT = 140
private const val DEFAULT_ICON_SIZE_PERCENT = 100
private const val DEFAULT_SCROLLBAR_WIDTH_PERCENT = 100
private const val DEFAULT_SCROLLBAR_HEIGHT_PERCENT = 100
private const val DEFAULT_SCROLLBAR_COLOR = 0xFFFFFFFF.toInt()  // White
private const val DEFAULT_SCROLLBAR_INTENSITY = 100  // 100% = original color
private const val DEFAULT_DRAWER_GRID_ROWS = 6
private const val DEFAULT_DRAWER_PAGED_MODE = false
private const val DEFAULT_ICON_TEXT_SIZE_PERCENT = 100
private const val DEFAULT_SELECTED_FONT = "default"
private const val KEY_SETTINGS_TAB = "settings_selected_tab"

/**
 * Get the saved grid size (number of columns) from SharedPreferences.
 * Returns DEFAULT_GRID_SIZE (4) if not set.
 */
fun getGridSize(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_GRID_SIZE, DEFAULT_GRID_SIZE)
}

/**
 * Save the grid size (number of columns) to SharedPreferences.
 */
fun setGridSize(context: Context, size: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_GRID_SIZE, size).apply()
}

/**
 * Get the saved icon size from SharedPreferences.
 * Returns DEFAULT_ICON_SIZE (48) if not set.
 */
fun getIconSize(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_ICON_SIZE, DEFAULT_ICON_SIZE)
}

/**
 * Save the icon size to SharedPreferences.
 */
fun setIconSize(context: Context, size: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_ICON_SIZE, size).apply()
}

/**
 * Get whether icon size and grid size are linked from SharedPreferences.
 * Returns DEFAULT_LINKED (true) if not set.
 */
fun getSizeLinked(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_SIZE_LINKED, DEFAULT_LINKED)
}

/**
 * Save whether icon size and grid size are linked to SharedPreferences.
 */
fun setSizeLinked(context: Context, linked: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_SIZE_LINKED, linked).apply()
}

// ============================================================================
// ICON SIZE PERCENTAGE (replaces dp-based icon size for proportional scaling)
// ============================================================================

fun getDrawerIconSizePercent(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getInt(KEY_ICON_SIZE_PERCENT, DEFAULT_ICON_SIZE_PERCENT)
    // Auto-migrate from old 30-80 scale (direct % of cell width) to new 50-150 scale
    if (value < 50) {
        val migrated = (value / 0.55f).roundToInt().coerceIn(50, 125)
        prefs.edit().putInt(KEY_ICON_SIZE_PERCENT, migrated).apply()
        return migrated
    }
    return value
}

fun setDrawerIconSizePercent(context: Context, percent: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_ICON_SIZE_PERCENT, percent).apply()
}

// ============================================================================
// SCROLLBAR SETTINGS (percentage-based for proportional scaling)
// ============================================================================

fun getScrollbarWidthPercent(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getInt(KEY_SCROLLBAR_WIDTH_PERCENT, DEFAULT_SCROLLBAR_WIDTH_PERCENT)
    // Auto-migrate from old 1-5 scale (direct % of screen width) to new 50-150 scale
    if (value < 50) {
        val migrated = (value / 0.02f).roundToInt().coerceIn(50, 150)
        prefs.edit().putInt(KEY_SCROLLBAR_WIDTH_PERCENT, migrated).apply()
        return migrated
    }
    return value
}

fun setScrollbarWidthPercent(context: Context, percent: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_WIDTH_PERCENT, percent).apply()
}

fun getScrollbarHeightPercent(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getInt(KEY_SCROLLBAR_HEIGHT_PERCENT, DEFAULT_SCROLLBAR_HEIGHT_PERCENT)
    // Auto-migrate from old 5-30 scale (direct % of screen height) to new 50-150 scale
    if (value < 50) {
        val migrated = (value / 0.20f).roundToInt().coerceIn(50, 150)
        prefs.edit().putInt(KEY_SCROLLBAR_HEIGHT_PERCENT, migrated).apply()
        return migrated
    }
    return value
}

fun setScrollbarHeightPercent(context: Context, percent: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_HEIGHT_PERCENT, percent).apply()
}

// ============================================================================
// SCROLLBAR SETTINGS (legacy dp-based - kept for backward compatibility)

/**
 * Get the scrollbar width from SharedPreferences.
 */
fun getScrollbarWidth(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SCROLLBAR_WIDTH, DEFAULT_SCROLLBAR_WIDTH)
}

/**
 * Save the scrollbar width to SharedPreferences.
 */
fun setScrollbarWidth(context: Context, width: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_WIDTH, width).apply()
}

/**
 * Get the scrollbar height from SharedPreferences.
 */
fun getScrollbarHeight(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SCROLLBAR_HEIGHT, DEFAULT_SCROLLBAR_HEIGHT)
}

/**
 * Save the scrollbar height to SharedPreferences.
 */
fun setScrollbarHeight(context: Context, height: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_HEIGHT, height).apply()
}

/**
 * Get the scrollbar color from SharedPreferences.
 */
fun getScrollbarColor(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SCROLLBAR_COLOR, DEFAULT_SCROLLBAR_COLOR)
}

/**
 * Save the scrollbar color to SharedPreferences.
 */
fun setScrollbarColor(context: Context, color: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_COLOR, color).apply()
}

/**
 * Get the scrollbar color intensity from SharedPreferences.
 * Returns DEFAULT_SCROLLBAR_INTENSITY (100) if not set.
 * Range: 50 (darker) to 150 (lighter)
 */
fun getScrollbarIntensity(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SCROLLBAR_INTENSITY, DEFAULT_SCROLLBAR_INTENSITY)
}

/**
 * Save the scrollbar color intensity to SharedPreferences.
 */
fun setScrollbarIntensity(context: Context, intensity: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SCROLLBAR_INTENSITY, intensity).apply()
}

// ============================================================================
// DRAWER PAGED MODE SETTINGS
// ============================================================================

fun getDrawerGridRows(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_DRAWER_GRID_ROWS, DEFAULT_DRAWER_GRID_ROWS)
}

fun setDrawerGridRows(context: Context, rows: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_DRAWER_GRID_ROWS, rows).apply()
}

fun getDrawerPagedMode(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_DRAWER_PAGED_MODE, DEFAULT_DRAWER_PAGED_MODE)
}

fun setDrawerPagedMode(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_DRAWER_PAGED_MODE, enabled).apply()
}

// ============================================================================
// ICON TEXT SIZE PERCENTAGE (shared between home screen and app drawer)
// ============================================================================

fun getIconTextSizePercent(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_ICON_TEXT_SIZE_PERCENT, DEFAULT_ICON_TEXT_SIZE_PERCENT)
}

fun setIconTextSizePercent(context: Context, percent: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_ICON_TEXT_SIZE_PERCENT, percent).apply()
}

// ============================================================================
// GLOBAL ICON SHAPE (EXP method applied to all icons)
// ============================================================================

fun getGlobalIconShape(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getString(KEY_GLOBAL_ICON_SHAPE, null)
    return if (value.isNullOrEmpty()) null else value
}

fun setGlobalIconShape(context: Context, shape: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (shape == null) {
        prefs.edit().remove(KEY_GLOBAL_ICON_SHAPE).apply()
    } else {
        prefs.edit().putString(KEY_GLOBAL_ICON_SHAPE, shape).apply()
    }
}

fun getGlobalIconBgColor(context: Context): Int? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return if (prefs.contains(KEY_GLOBAL_ICON_BG_COLOR)) prefs.getInt(KEY_GLOBAL_ICON_BG_COLOR, 0) else null
}

fun setGlobalIconBgColor(context: Context, color: Int?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (color == null) {
        prefs.edit().remove(KEY_GLOBAL_ICON_BG_COLOR).apply()
    } else {
        prefs.edit().putInt(KEY_GLOBAL_ICON_BG_COLOR, color).apply()
    }
}

fun getGlobalIconBgIntensity(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_GLOBAL_ICON_BG_INTENSITY, 100)
}

fun setGlobalIconBgIntensity(context: Context, intensity: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_GLOBAL_ICON_BG_INTENSITY, intensity).apply()
}

// ============================================================================
// AUTO OPEN KEYBOARD
// ============================================================================

private const val KEY_AUTO_OPEN_KEYBOARD = "auto_open_keyboard"

fun getAutoOpenKeyboard(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_AUTO_OPEN_KEYBOARD, false)
}

fun setAutoOpenKeyboard(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_AUTO_OPEN_KEYBOARD, enabled).apply()
}

// ============================================================================
// REVERSE DRAWER SEARCH BAR
// ============================================================================

private const val KEY_REVERSE_SEARCH_BAR = "reverse_drawer_search_bar"

fun getReverseDrawerSearchBar(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_REVERSE_SEARCH_BAR, false)
}

fun setReverseDrawerSearchBar(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_REVERSE_SEARCH_BAR, enabled).apply()
}

// ============================================================================
// DOUBLE-TAP TO LOCK SCREEN
// ============================================================================

private const val KEY_DOUBLE_TAP_LOCK = "double_tap_lock_enabled"

fun getDoubleTapLockEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_DOUBLE_TAP_LOCK, false)
}

fun setDoubleTapLockEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_DOUBLE_TAP_LOCK, enabled).apply()
}

// ============================================================================
// FONT SELECTION (shared between home screen and app drawer)
// ============================================================================

fun getSelectedFont(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_SELECTED_FONT, DEFAULT_SELECTED_FONT) ?: DEFAULT_SELECTED_FONT
}

fun setSelectedFont(context: Context, fontId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SELECTED_FONT, fontId).apply()
}

fun getImportedFontPaths(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_IMPORTED_FONTS, emptySet()) ?: emptySet()
}

fun addImportedFontPath(context: Context, path: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getStringSet(KEY_IMPORTED_FONTS, emptySet())?.toMutableSet() ?: mutableSetOf()
    current.add(path)
    prefs.edit().putStringSet(KEY_IMPORTED_FONTS, current).apply()
}

fun removeImportedFontPath(context: Context, path: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getStringSet(KEY_IMPORTED_FONTS, emptySet())?.toMutableSet() ?: mutableSetOf()
    current.remove(path)
    prefs.edit().putStringSet(KEY_IMPORTED_FONTS, current).apply()
}

// ============================================================================
// ICON PACK SELECTION
// ============================================================================

fun getSelectedIconPack(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_SELECTED_ICON_PACK, DEFAULT_SELECTED_ICON_PACK) ?: DEFAULT_SELECTED_ICON_PACK
}

fun setSelectedIconPack(context: Context, iconPackPackage: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SELECTED_ICON_PACK, iconPackPackage).apply()
}

// ============================================================================
// SETTINGS TAB SELECTION
// ============================================================================

fun getSettingsSelectedTab(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SETTINGS_TAB, 0)
}

fun setSettingsSelectedTab(context: Context, tab: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SETTINGS_TAB, tab).apply()
}

// ============================================================================
// WIDGET ROUNDED CORNERS
// ============================================================================

private const val KEY_WIDGET_ROUNDED_CORNERS_ENABLED = "widget_rounded_corners_enabled"
private const val KEY_WIDGET_CORNER_RADIUS = "widget_corner_radius_percent"
private const val DEFAULT_WIDGET_ROUNDED_CORNERS_ENABLED = true
private const val DEFAULT_WIDGET_CORNER_RADIUS_PERCENT = 50  // 50% = 16dp out of 32dp max

fun getWidgetRoundedCornersEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_WIDGET_ROUNDED_CORNERS_ENABLED, DEFAULT_WIDGET_ROUNDED_CORNERS_ENABLED)
}

fun setWidgetRoundedCornersEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_WIDGET_ROUNDED_CORNERS_ENABLED, enabled).apply()
}

fun getWidgetCornerRadiusPercent(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_WIDGET_CORNER_RADIUS, DEFAULT_WIDGET_CORNER_RADIUS_PERCENT)
}

fun setWidgetCornerRadiusPercent(context: Context, percent: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_WIDGET_CORNER_RADIUS, percent).apply()
}

/** Max corner radius in dp (100% maps to this value) */
const val WIDGET_MAX_CORNER_RADIUS_DP = 32f
