package com.bearinmind.launcher314.services

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ScreenLockAdminReceiver : DeviceAdminReceiver() {

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, ScreenLockAdminReceiver::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }

        fun lockScreen(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return try {
                if (dpm.isAdminActive(getComponentName(context))) {
                    dpm.lockNow()
                    true
                } else false
            } catch (_: Exception) { false }
        }

        fun requestEnable(context: Context) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows Launcher314 to lock the screen when you double-tap the home screen.")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun requestDisable(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.removeActiveAdmin(getComponentName(context))
        }
    }
}
