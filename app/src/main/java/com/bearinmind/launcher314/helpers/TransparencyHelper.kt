package com.bearinmind.launcher314.helpers

import android.content.Context
import androidx.compose.ui.graphics.Color

// SharedPreferences for drawer transparency
private const val PREFS_NAME = "app_drawer_settings"
private const val KEY_DRAWER_TRANSPARENCY = "drawer_transparency"

// Default: 0% transparency (fully opaque black background)
private const val DEFAULT_DRAWER_TRANSPARENCY = 0

/**
 * Get the drawer transparency from SharedPreferences.
 * Returns value from 0 to 100:
 * - 0% = fully opaque (solid black background, can't see through)
 * - 100% = fully transparent (see through to home screen)
 */
fun getDrawerTransparency(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_DRAWER_TRANSPARENCY, DEFAULT_DRAWER_TRANSPARENCY)
}

/**
 * Save the drawer transparency to SharedPreferences.
 * Value should be from 0 to 100:
 * - 0% = fully opaque (solid black background)
 * - 100% = fully transparent (see through to home screen)
 */
fun setDrawerTransparency(context: Context, transparency: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_DRAWER_TRANSPARENCY, transparency.coerceIn(0, 100)).apply()
}

/**
 * Calculate the background color for the app drawer based on transparency setting.
 * Returns a Color with appropriate alpha value.
 *
 * @param transparency Value from 0 to 100
 * @return Color with alpha: 0% transparency = alpha 1.0 (opaque), 100% = alpha 0.0 (transparent)
 */
fun calculateDrawerBackgroundColor(transparency: Int): Color {
    val backgroundAlpha = (100 - transparency) / 100f
    return Color(0xFF121212).copy(alpha = backgroundAlpha)
}

/**
 * Calculate the background alpha for the app drawer based on transparency setting.
 *
 * @param transparency Value from 0 to 100
 * @return Alpha value: 0% transparency = 1.0 (opaque), 100% = 0.0 (transparent)
 */
fun calculateDrawerBackgroundAlpha(transparency: Int): Float {
    return (100 - transparency) / 100f
}


