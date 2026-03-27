package com.bearinmind.launcher314.helpers

import android.content.Context
import android.os.Build
import android.util.Log

object NotificationPanelHelper {

    /**
     * Expand the notification shade using StatusBarManager via reflection.
     * Requires android.permission.EXPAND_STATUS_BAR (normal permission).
     */
    @Suppress("WrongConstant")
    fun expandNotificationPanel(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            Log.e("NotificationPanel", "Failed to expand notification panel", e)
        }
    }

    /**
     * Expand quick settings panel directly.
     */
    @Suppress("WrongConstant")
    fun expandQuickSettings(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandSettingsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            Log.e("NotificationPanel", "Failed to expand quick settings", e)
        }
    }
}
