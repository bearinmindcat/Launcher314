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

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        val scaledContext = createScaledContext(context, appWidgetId)
        return LauncherAppWidgetHostView(scaledContext)
    }

    /**
     * Create a Context with modified fontScale for widget text sizing.
     * Per-widget override (PlacedWidget.fontScalePercent) takes precedence over the
     * global setting. Multiplies with the system fontScale to preserve accessibility.
     */
    private fun createScaledContext(context: Context, appWidgetId: Int): Context {
        val perWidget = WidgetManager.loadPlacedWidgets(context)
            .find { it.appWidgetId == appWidgetId }
            ?.fontScalePercent
        val scalePercent = perWidget ?: getWidgetFontScalePercent(context)
        if (scalePercent == 100) return context

        val multiplier = scalePercent / 100f
        val config = Configuration(context.resources.configuration)
        config.fontScale = config.fontScale * multiplier
        return context.createConfigurationContext(config)
    }

    companion object {
        // Unique host ID for this launcher
        const val HOST_ID = 314
    }
}
