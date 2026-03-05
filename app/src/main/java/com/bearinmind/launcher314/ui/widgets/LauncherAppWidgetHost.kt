package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 * Custom AppWidgetHost for the launcher.
 * This manages all widget instances displayed on the home screen.
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
        return LauncherAppWidgetHostView(context)
    }

    companion object {
        // Unique host ID for this launcher
        const val HOST_ID = 314
    }
}