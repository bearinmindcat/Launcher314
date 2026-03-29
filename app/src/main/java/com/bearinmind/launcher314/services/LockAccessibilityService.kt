package com.bearinmind.launcher314.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

class LockAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_LOCK = "com.bearinmind.launcher314.LOCK_SCREEN"

        fun isEnabled(context: Context): Boolean {
            val am = ContextCompat.getSystemService(context, AccessibilityManager::class.java) ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            return enabledServices.any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                it.resolveInfo.serviceInfo.name == LockAccessibilityService::class.java.name
            }
        }

        fun lockScreen(context: Context) {
            val intent = Intent(ACTION_LOCK, null, context, LockAccessibilityService::class.java)
            context.startService(intent)
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LOCK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
