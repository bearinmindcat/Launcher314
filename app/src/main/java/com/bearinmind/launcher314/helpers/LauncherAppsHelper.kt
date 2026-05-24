package com.bearinmind.launcher314.helpers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Profile classification used to group apps into tabs in the drawer.
 * Personal is the primary user. Work / Clone / Private are the three
 * profile types Kvaesitso also models separately. OTHER is a fallback for
 * unknown / future profile types so they don't disappear from the drawer.
 */
enum class ProfileType { PERSONAL, WORK, CLONE, PRIVATE, OTHER }

/**
 * Thin wrapper around [LauncherApps] + [UserManager] for work-profile / managed-
 * profile / cloned-profile support. Replaces the launcher's old
 * `PackageManager.queryIntentActivities` flow which only saw the personal user.
 *
 * Key behaviors:
 *  - [enumerateAllApps] returns a flat list of launchable activities across
 *    every active profile the launcher is allowed to see.
 *  - [serialFor] / [userHandleFor] convert between the two stable identifiers
 *    (the long-lived serial we persist on disk and the runtime `UserHandle`).
 *  - [startApp] launches in the correct profile via
 *    `LauncherApps.startMainActivity` — never use `getLaunchIntentForPackage`
 *    for non-personal apps; it silently fails.
 *  - [loadOrCacheBadgedIcon] caches a badged PNG keyed on `pkg + serial`. The
 *    framework's [LauncherActivityInfo.getBadgedIcon] applies the work-profile
 *    overlay for free, so the cached file already includes the badge.
 */
object LauncherAppsHelper {
    private const val TAG = "LauncherAppsHelper"

    /** Subdirectory under cacheDir that holds the badged icon PNGs. */
    private const val ICON_DIR = "app_icons"

    /**
     * Personal-profile serial sentinel. Saved entries that predate
     * work-profile support have userSerial = null; treat null as personal.
     */
    val PERSONAL_SERIAL: Long? = null

    /** All apps from every visible user profile. */
    fun enumerateAllApps(context: Context): List<LauncherActivityInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val users = try {
            userManager.userProfiles
        } catch (e: Throwable) {
            Log.w(TAG, "userProfiles failed, falling back to current user", e)
            listOf(android.os.Process.myUserHandle())
        }
        return users.flatMap { user ->
            try {
                launcherApps.getActivityList(null, user)
            } catch (e: Throwable) {
                Log.w(TAG, "getActivityList failed for user $user", e)
                emptyList()
            }
        }
    }

    /**
     * Stable serial number for a [UserHandle]. Persist this — `UserHandle`
     * itself is not stable across reboots / process restarts.
     * Returns null for the primary user so existing data stays on the null
     * sentinel (backward compat with pre-work-profile saves).
     */
    fun serialFor(context: Context, user: UserHandle): Long? {
        if (user == android.os.Process.myUserHandle()) return null
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        return um.getSerialNumberForUser(user)
    }

    /**
     * Resolve a persisted serial back to a runtime [UserHandle]. Returns the
     * primary user for null/missing serials, or null if the profile is gone
     * (uninstalled work profile, etc.).
     */
    fun userHandleFor(context: Context, serial: Long?): UserHandle? {
        if (serial == null) return android.os.Process.myUserHandle()
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        return um.getUserForSerialNumber(serial)
    }

    /**
     * Detect the profile type for a user. Uses Android 14+ [LauncherApps.getLauncherUserInfo]
     * when available — that returns a stable `userType` string for managed /
     * clone / private profiles. Pre-API-34 we can only distinguish managed
     * profiles via [UserManager.isManagedProfile]; everything else falls into
     * OTHER (still shown, just as a generic chip).
     */
    fun profileTypeFor(context: Context, user: UserHandle): ProfileType {
        if (user == android.os.Process.myUserHandle()) return ProfileType.PERSONAL
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val info = launcherApps.getLauncherUserInfo(user)
                when (info?.userType) {
                    "android.os.usertype.profile.MANAGED" -> return ProfileType.WORK
                    "android.os.usertype.profile.CLONE" -> return ProfileType.CLONE
                    "android.os.usertype.profile.PRIVATE" -> return ProfileType.PRIVATE
                }
            } catch (e: Throwable) {
                Log.w(TAG, "getLauncherUserInfo failed for $user", e)
            }
        }
        // Fallback for pre-API-34: there's no public API to query the user
        // type of an arbitrary profile (UserManager.isManagedProfile(int) is
        // restricted), so non-personal profiles are bucketed as WORK — the
        // common case across virtually every device. Cloned/private profiles
        // didn't broadly exist before API 34 anyway.
        return ProfileType.WORK
    }

    /**
     * Find a [LauncherActivityInfo] for (pkg, user). Returns null if the app
     * was uninstalled or the profile is gone.
     */
    fun findActivity(
        context: Context,
        packageName: String,
        user: UserHandle
    ): LauncherActivityInfo? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return try {
            launcherApps.getActivityList(packageName, user).firstOrNull()
        } catch (e: Throwable) {
            Log.w(TAG, "getActivityList($packageName) failed for $user", e)
            null
        }
    }

    /**
     * Launch an app in the correct profile. Returns true on success.
     * `personal` profile uses [LauncherApps.startMainActivity] too — it's
     * equivalent to the legacy path for the personal user but works
     * uniformly for every profile.
     */
    fun startApp(
        context: Context,
        packageName: String,
        userSerial: Long?
    ): Boolean {
        val user = userHandleFor(context, userSerial) ?: return false
        val activity = findActivity(context, packageName, user) ?: return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return try {
            launcherApps.startMainActivity(activity.componentName, user, null, null)
            true
        } catch (e: Throwable) {
            Log.w(TAG, "startMainActivity failed for $packageName user=$user", e)
            false
        }
    }

    /**
     * Cache a badged icon (work-profile overlay baked in) to disk for the
     * given (pkg, serial) and return the file path. Existing files are reused.
     * Personal apps and work apps cache to different files so the badge
     * doesn't leak between them.
     */
    fun loadOrCacheBadgedIcon(
        context: Context,
        activity: LauncherActivityInfo,
        userSerial: Long?
    ): String {
        val iconDir = File(context.cacheDir, ICON_DIR).also { it.mkdirs() }
        val key = if (userSerial == null) activity.applicationInfo.packageName
            else "${activity.applicationInfo.packageName}__u$userSerial"
        val file = File(iconDir, "$key.png")
        if (file.exists() && file.length() > 0) return file.absolutePath
        val drawable: Drawable = try {
            activity.getBadgedIcon(0)
        } catch (e: Throwable) {
            Log.w(TAG, "getBadgedIcon failed", e)
            context.packageManager.getApplicationIcon(activity.applicationInfo)
        }
        val bitmap = drawableToBitmap(drawable)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "icon cache write failed", e)
        }
        bitmap.recycle()
        return file.absolutePath
    }

    /**
     * Delete the cached badged icon for (pkg, serial). Called by the package-
     * change broadcast so the next read regenerates.
     */
    fun invalidateIconCache(context: Context, packageName: String, userSerial: Long?) {
        val iconDir = File(context.cacheDir, ICON_DIR)
        if (!iconDir.exists()) return
        val key = if (userSerial == null) packageName else "${packageName}__u$userSerial"
        File(iconDir, "$key.png").takeIf { it.exists() }?.delete()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
