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
        val scaledContext = createScaledContext(context)
        return LauncherAppWidgetHostView(scaledContext)
    }

    /**
     * Create a Context with modified fontScale for widget text sizing.
     * Multiplies with the system fontScale to preserve accessibility settings.
     */
    private fun createScaledContext(context: Context): Context {
        val scalePercent = getWidgetFontScalePercent(context)
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
