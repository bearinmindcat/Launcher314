package com.bearinmind.launcher314.services

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.bearinmind.launcher314.MainActivity

/**
 * AppDrawerAccessibilityService
 *
 * An accessibility service that launches the app drawer when the user triggers
 * the accessibility button or accessibility shortcut.
 *
 * The user can trigger this via:
 * - Accessibility button (floating button or nav bar button)
 * - Accessibility shortcut (hold both volume keys)
 * - Gesture (configured in accessibility settings)
 */
class AppDrawerAccessibilityService : AccessibilityService() {

    private var accessibilityButtonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    companion object {
        const val ACTION_LOCK_SCREEN = "com.bearinmind.launcher314.LOCK_SCREEN"

        // Shared preferences
        private const val PREFS_NAME = "accessibility_drawer_settings"
        private const val KEY_ENABLED = "accessibility_drawer_enabled"

        fun lockScreen(context: Context) {
            val intent = Intent(ACTION_LOCK_SCREEN, null, context, AppDrawerAccessibilityService::class.java)
            context.startService(intent)
        }

        /**
         * Check if accessibility drawer is enabled in app settings
         */
        fun isEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, false)
        }

        /**
         * Enable or disable accessibility drawer in app settings
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        /**
         * Check if accessibility service is enabled in system settings
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${AppDrawerAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(serviceName)
        }

        /**
         * Open accessibility settings for user to enable the service
         */
        fun openAccessibilitySettings(context: Context) {
            val serviceComponentName = android.content.ComponentName(
                context.packageName,
                AppDrawerAccessibilityService::class.java.name
            )

            // Try direct service detail page (works on Samsung One UI and AOSP 14+)
            try {
                val intent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }

            // Try EXTRA_COMPONENT_NAME approach (some AOSP devices)
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponentName)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }

            // Fallback: general accessibility settings with hint
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.widget.Toast.makeText(
                context,
                "Go to Installed apps → Launcher314",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LOCK_SCREEN) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure the service to use the accessibility button
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            // Request the accessibility button (floating button or nav bar icon)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
            }
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }
        serviceInfo = info

        // Register the accessibility button callback (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupAccessibilityButton()
        }

        android.util.Log.d("AccessibilityDrawer", "Service connected - accessibility button enabled")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAccessibilityButton() {
        val controller = accessibilityButtonController ?: return

        accessibilityButtonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                android.util.Log.d("AccessibilityDrawer", "Accessibility button clicked!")
                if (isEnabled(this@AppDrawerAccessibilityService)) {
                    launchAppDrawer()
                }
            }

            override fun onAvailabilityChanged(controller: AccessibilityButtonController, available: Boolean) {
                android.util.Log.d("AccessibilityDrawer", "Accessibility button availability: $available")
            }
        }

        controller.registerAccessibilityButtonCallback(accessibilityButtonCallback!!)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to process accessibility events for this service
        // The service is triggered via the accessibility button callback instead
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the callback when service is destroyed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accessibilityButtonCallback?.let {
                accessibilityButtonController?.unregisterAccessibilityButtonCallback(it)
            }
        }
    }

    /**
     * Launch the app drawer activity
     */
    private fun launchAppDrawer() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "app_drawer")
        }
        startActivity(intent)
    }
}
