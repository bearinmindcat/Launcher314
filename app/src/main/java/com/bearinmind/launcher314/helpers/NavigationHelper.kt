package com.bearinmind.launcher314.helpers

import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Utility functions for making the launcher navigation bar fully transparent.
 * This removes the grey scrim that Android typically applies to the navigation bar.
 */

/**
 * Makes the navigation bar and status bar fully transparent for launcher mode.
 * Should be called after super.onCreate() in the Activity.
 *
 * @param activity The ComponentActivity to apply transparent navigation to
 */
fun applyTransparentNavigation(activity: ComponentActivity) {
    // Use enableEdgeToEdge with fully transparent bars
    // Both use SystemBarStyle.dark for light/white icons over the wallpaper
    activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )

    // Disable status bar contrast enforcement so it stays transparent.
    // Navigation bar contrast is left enabled (default) so the system adds
    // a subtle scrim behind 3-button navigation, matching One UI / other launchers.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        activity.window.isStatusBarContrastEnforced = false
    }
}

/**
 * Disables the navigation bar contrast enforcement on Android 10+.
 * This prevents Android from adding a grey scrim behind the navigation bar.
 *
 * @param window The Window to disable contrast enforcement on
 */
fun disableNavigationBarContrast(window: Window) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
    }
}

/**
 * Sets both navigation bar and status bar colors to fully transparent.
 *
 * @param window The Window to set transparent colors on
 */
fun setTransparentSystemBars(window: Window) {
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT
}
