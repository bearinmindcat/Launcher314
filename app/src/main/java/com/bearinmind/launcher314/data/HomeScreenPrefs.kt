package com.bearinmind.launcher314.data

import android.content.Context

// SharedPreferences keys for Home Screen settings
private const val HOME_PREFS_NAME = "home_screen_settings"
private const val HOME_KEY_GRID_COLUMNS = "home_grid_columns"
private const val HOME_KEY_GRID_ROWS = "home_grid_rows"
private const val HOME_KEY_ICON_SIZE = "home_icon_size"
private const val HOME_KEY_ICON_SIZE_PERCENT = "home_icon_size_percent"
private const val HOME_KEY_DOCK_COLUMNS = "home_dock_columns"
private const val HOME_KEY_DOCK_ENABLED = "home_dock_enabled"

// Grid columns (X)
fun getHomeGridSize(context: Context): Int {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(HOME_KEY_GRID_COLUMNS, 4)
}

fun setHomeGridSize(context: Context, size: Int) {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(HOME_KEY_GRID_COLUMNS, size).apply()
}

// Grid rows (Y)
fun getHomeGridRows(context: Context): Int {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(HOME_KEY_GRID_ROWS, 6)
}

fun setHomeGridRows(context: Context, rows: Int) {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(HOME_KEY_GRID_ROWS, rows).apply()
}

fun getHomeIconSize(context: Context): Int {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(HOME_KEY_ICON_SIZE, 48)
}

fun setHomeIconSize(context: Context, size: Int) {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(HOME_KEY_ICON_SIZE, size).apply()
}

// Icon size shared between home screen and app drawer (unified setting)
fun getHomeIconSizePercent(context: Context): Int = getDrawerIconSizePercent(context)

fun setHomeIconSizePercent(context: Context, percent: Int) = setDrawerIconSizePercent(context, percent)

// Dock columns
fun getDockColumns(context: Context): Int {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(HOME_KEY_DOCK_COLUMNS, 5) // Default 5 dock slots
}

fun setDockColumns(context: Context, columns: Int) {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(HOME_KEY_DOCK_COLUMNS, columns).apply()
}

fun getDockEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(HOME_KEY_DOCK_ENABLED, true)
}

fun setDockEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(HOME_KEY_DOCK_ENABLED, enabled).apply()
}
