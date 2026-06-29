package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Configuration
import com.bearinmind.launcher314.data.getWidgetFontScalePercent

/**
 * Custom AppWidgetHost for the launcher.
 * This manages all widget instances displayed on the home screen.
 * Supports custom font scaling for widget text via createConfigurationContext.
 */
class LauncherAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context, hostId) {

    private val hostContext: Context = context.applicationContext

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        val scaledContext = createScaledContext(context, appWidgetId)
        return LauncherAppWidgetHostView(scaledContext)
    }

    /**
     * Called by the system when ANY widget provider package on the device
     * is added / updated / removed. Launcher3 uses this hook to re-bind
     * every cached host view so the new provider info (icon, label, sizing
     * constraints) is reflected without restarting the launcher. Without
     * this, an updated APK would still render with stale provider info
     * until the user kills the launcher process.
     */
    override fun onProvidersChanged() {
        super.onProvidersChanged()
        WidgetManager.rebindAllCachedViews()
    }

    /**
     * Called when a SINGLE provider's info changes (typically when the
     * provider's APK is reinstalled). Re-bind only the affected cached
     * view so its `AppWidgetProviderInfo` is current.
     */
    override fun onProviderChanged(appWidgetId: Int, appWidget: AppWidgetProviderInfo?) {
        super.onProviderChanged(appWidgetId, appWidget)
        if (appWidget != null) {
            WidgetManager.rebindCachedView(appWidgetId, appWidget)
        }
    }

    /**
     * Called by the system when a widget is removed because its provider
     * package was uninstalled (or the provider deleted the widget). We
     * delete the host-side allocation, drop the cached view, and remove
     * the persisted `PlacedWidget` so the user doesn't see a "ghost" cell
     * on the home screen.
     */
    override fun onAppWidgetRemoved(appWidgetId: Int) {
        super.onAppWidgetRemoved(appWidgetId)
        WidgetManager.handleProviderRemovedWidget(hostContext, appWidgetId)
    }

    /**
     * Create a Context with modified fontScale for widget text sizing.
     * Per-widget override (PlacedWidget.fontScalePercent) takes precedence over the
     * global setting. Multiplies with the system fontScale to preserve accessibility.
     */
    private fun createScaledContext(context: Context, appWidgetId: Int): Context {
        // IMPORTANT: do NOT wrap the widget's context in createConfigurationContext to
        // apply a font scale. A config-context is not a proper UI context, and using it
        // for the AppWidgetHostView makes many providers (Samsung clock/weather and other
        // Material-You widgets) fail to render with "Can't show content" — verified via
        // logcat: a non-100% widget font scale -> WRAPPING -> the provider's RemoteViews
        // are delivered but never display. Widget rendering correctness wins over the
        // per-widget font-scale feature, so always use the launcher's real (themed,
        // display-associated) context.
        return context
    }

    companion object {
        // Unique host ID for this launcher
        const val HOST_ID = 314
    }
}
