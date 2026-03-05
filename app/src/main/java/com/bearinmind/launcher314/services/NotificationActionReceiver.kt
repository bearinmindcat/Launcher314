package com.bearinmind.launcher314.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bearinmind.launcher314.MainActivity
import com.bearinmind.launcher314.R

/**
 * NotificationDrawerAction
 *
 * Manages a persistent notification with an action button to open the app drawer.
 * The notification stays in the notification shade and provides quick access.
 *
 * Features:
 * - Persistent notification (ongoing, not dismissible)
 * - Tap notification or action button to open drawer
 * - Low priority to minimize visual intrusion
 */
object NotificationDrawerAction {

    private const val CHANNEL_ID = "app_drawer_channel"
    private const val CHANNEL_NAME = "App Drawer Quick Access"
    private const val NOTIFICATION_ID = 1001

    private const val PREFS_NAME = "notification_action_prefs"
    private const val KEY_ENABLED = "notification_enabled"

    const val ACTION_OPEN_DRAWER = "com.bearinmind.launcher314.ACTION_OPEN_DRAWER"

    /**
     * Check if notification action is enabled
     */
    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    /**
     * Enable or disable notification action
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        if (enabled) {
            showNotification(context)
        } else {
            hideNotification(context)
        }
    }

    /**
     * Create the notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound, minimized
            ).apply {
                description = "Quick access to open the app drawer"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show the persistent notification
     */
    fun showNotification(context: Context) {
        createNotificationChannel(context)

        // Intent to open the app drawer when notification is tapped
        val openDrawerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "app_drawer")
        }
        val openDrawerPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openDrawerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for the "Open" action button
        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_OPEN_DRAWER
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Drawer")
            .setContentText("Tap to open app drawer")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true) // Makes it persistent (not dismissible)
            .setShowWhen(false) // Hide timestamp
            .setContentIntent(openDrawerPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Open Drawer",
                actionPendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        android.util.Log.d("NotificationAction", "Persistent notification shown")
    }

    /**
     * Hide/remove the notification
     */
    fun hideNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        android.util.Log.d("NotificationAction", "Notification hidden")
    }

    /**
     * Restore notification if it was enabled (call on app startup or boot)
     */
    fun restoreIfEnabled(context: Context) {
        if (isEnabled(context)) {
            showNotification(context)
        }
    }
}

/**
 * BroadcastReceiver to handle notification action button clicks
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationDrawerAction.ACTION_OPEN_DRAWER) {
            android.util.Log.d("NotificationAction", "Action button clicked - opening drawer")

            val openDrawerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "app_drawer")
            }
            context.startActivity(openDrawerIntent)
        }
    }
}

/**
 * BroadcastReceiver to restore notification on device boot
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("NotificationAction", "Boot completed - restoring notification if enabled")
            NotificationDrawerAction.restoreIfEnabled(context)
        }
    }
}
