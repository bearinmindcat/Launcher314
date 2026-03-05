package com.bearinmind.launcher314.services

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.bearinmind.launcher314.MainActivity
import com.bearinmind.launcher314.R

/**
 * AppDrawerTileService
 *
 * A Quick Settings tile that appears in the notification shade.
 * When tapped, it launches the app drawer.
 *
 * The user can add this tile to their Quick Settings panel by:
 * 1. Pulling down the notification shade
 * 2. Tapping the edit (pencil) icon
 * 3. Dragging "App Drawer" tile to the active tiles area
 */
@RequiresApi(Build.VERSION_CODES.N)
class AppDrawerTileService : TileService() {

    companion object {
        private const val PREFS_NAME = "quick_settings_tile_prefs"
        private const val KEY_ENABLED = "tile_enabled"

        /**
         * Check if the tile feature is enabled in app settings
         */
        fun isEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, false)
        }

        /**
         * Enable or disable the tile feature in app settings
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        }
    }

    /**
     * Called when the tile is added to Quick Settings
     */
    override fun onTileAdded() {
        super.onTileAdded()
        android.util.Log.d("AppDrawerTile", "Tile added to Quick Settings")
        updateTileState()
    }

    /**
     * Called when the tile is removed from Quick Settings
     */
    override fun onTileRemoved() {
        super.onTileRemoved()
        android.util.Log.d("AppDrawerTile", "Tile removed from Quick Settings")
    }

    /**
     * Called when the tile becomes visible
     */
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    /**
     * Called when the tile is no longer visible
     */
    override fun onStopListening() {
        super.onStopListening()
    }

    /**
     * Called when the user taps the tile
     */
    override fun onClick() {
        super.onClick()
        android.util.Log.d("AppDrawerTile", "Tile clicked - launching app drawer")

        // Launch the app drawer
        launchAppDrawer()

        // Collapse the notification shade
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12+, we need to use a different approach
            // The shade collapses automatically when starting an activity
        }
    }

    /**
     * Update the tile's visual state
     */
    private fun updateTileState() {
        val tile = qsTile ?: return

        tile.state = Tile.STATE_INACTIVE
        tile.label = "App Drawer"
        tile.contentDescription = "Open App Drawer"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Tap to open"
        }

        // Set the icon
        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)

        tile.updateTile()
    }

    /**
     * Launch the app drawer activity
     */
    private fun launchAppDrawer() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "app_drawer")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires startActivityAndCollapse with PendingIntent
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
