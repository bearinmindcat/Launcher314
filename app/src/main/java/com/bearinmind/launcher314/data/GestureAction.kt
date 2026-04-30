package com.bearinmind.launcher314.data

import android.content.Context

/**
 * The set of gestures the user can assign actions to.
 *
 * SWIPE_LEFT / SWIPE_RIGHT have storage support but no detector yet — they
 * collide with the home-screen HorizontalPager's page-change gesture and are
 * deferred to a follow-up "boundary rubberband" detector.
 */
enum class GestureId {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP
}

/**
 * Action vocabulary for [GestureId] assignments. Persistence stores a string
 * key per action; [OpenApp] additionally stores its target package in a
 * companion preference key.
 */
sealed class GestureAction {
    object None : GestureAction()
    object OpenDrawer : GestureAction()
    object OpenNotifications : GestureAction()
    object OpenQuickSettings : GestureAction()
    object LockScreen : GestureAction()
    object ShowRecentApps : GestureAction()
    data class OpenApp(val packageName: String) : GestureAction()

    /**
     * Stable string identifier for serialization. [OpenApp] is identified by
     * just "open_app"; the target package lives in a separate preference key
     * so the action key stays a fixed-vocabulary enum.
     */
    val storageKey: String
        get() = when (this) {
            None -> "none"
            OpenDrawer -> "open_drawer"
            OpenNotifications -> "open_notifications"
            OpenQuickSettings -> "open_quick_settings"
            LockScreen -> "lock_screen"
            ShowRecentApps -> "show_recent_apps"
            is OpenApp -> "open_app"
        }

    companion object {
        /**
         * Inverse of [storageKey]. [OpenApp] is reconstructed from the stored
         * key plus its companion package name; an empty package falls back to
         * [None].
         */
        fun fromStorageKey(key: String, openAppPackage: String): GestureAction = when (key) {
            "none" -> None
            "open_drawer" -> OpenDrawer
            "open_notifications" -> OpenNotifications
            "open_quick_settings" -> OpenQuickSettings
            "lock_screen" -> LockScreen
            "show_recent_apps" -> ShowRecentApps
            "open_app" -> if (openAppPackage.isNotEmpty()) OpenApp(openAppPackage) else None
            else -> None
        }
    }
}

/**
 * UI-side hooks the action dispatcher needs because the host composable owns
 * those animations / overlay states. System-level actions (notifications,
 * quick settings, lock, app launch) are self-contained and handled by the
 * dispatcher directly.
 */
interface GestureUiCallbacks {
    fun openDrawer()
    fun showRecentApps()
}

/**
 * Run the action. UI actions go through [GestureUiCallbacks]; system-level
 * actions call into existing helpers directly. [None] is intentionally a
 * no-op (the gesture has been disabled by the user).
 */
fun GestureAction.dispatch(context: Context, ui: GestureUiCallbacks) {
    when (this) {
        GestureAction.None -> Unit
        GestureAction.OpenDrawer -> ui.openDrawer()
        GestureAction.OpenNotifications ->
            com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandNotificationPanel(context)
        GestureAction.OpenQuickSettings ->
            com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandQuickSettings(context)
        GestureAction.LockScreen ->
            com.bearinmind.launcher314.services.AppDrawerAccessibilityService.lockScreen(context)
        GestureAction.ShowRecentApps -> ui.showRecentApps()
        is GestureAction.OpenApp -> launchApp(context, packageName)
    }
}

/**
 * True if this action will cause the drawer to slide up. Used by the swipe-up
 * gesture handler to know whether it should animate the drawer mid-drag (the
 * existing rubber-band feel) or hold the drawer in place and only fire the
 * action on release.
 */
val GestureAction.movesDrawer: Boolean
    get() = this is GestureAction.OpenDrawer
