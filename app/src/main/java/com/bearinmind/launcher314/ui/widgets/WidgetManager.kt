package com.bearinmind.launcher314.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data class representing a placed widget on the home screen.
 * Uses Einstein Launcher's grid item model: startColumn, startRow, columnSpan, rowSpan.
 */
@Serializable
data class PlacedWidget(
    val appWidgetId: Int,
    val packageName: String,
    val className: String,
    // Grid position (Einstein style)
    val startColumn: Int,   // Starting column index
    val startRow: Int,      // Starting row index
    val columnSpan: Int,    // Width in cells
    val rowSpan: Int,       // Height in cells
    val page: Int = 0,      // Home screen page (default 0 for backward compat)
    val stackId: String? = null,  // Non-null when widget is part of a stack
    val stackOrder: Int = 0,      // Order within the stack (0 = first/primary)
    val paddingPercent: Int? = null,  // Per-widget padding override (null = use global)
    val fontScalePercent: Int? = null // Per-widget text size override (null = use global)
) {
    // Compatibility aliases
    val gridColumn: Int get() = startColumn
    val gridRow: Int get() = startRow
    val spanColumns: Int get() = columnSpan
    val spanRows: Int get() = rowSpan

    // Fossify-style bounds (for compatibility)
    val left: Int get() = startColumn
    val top: Int get() = startRow
    val right: Int get() = startColumn + columnSpan - 1
    val bottom: Int get() = startRow + rowSpan - 1
}

/**
 * Singleton manager for app widgets on the home screen.
 * Handles widget lifecycle: allocation, binding, configuration, and persistence.
 */
object WidgetManager {

    private const val PREFS_NAME = "launcher_widgets"
    private const val KEY_PLACED_WIDGETS = "placed_widgets"
    private const val REQUEST_PICK_APPWIDGET = 9001
    private const val REQUEST_CREATE_APPWIDGET = 9002
    const val REQUEST_BIND_APPWIDGET = 9003

    private var appWidgetHost: LauncherAppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null

    // Map to track created widget views for real-time resize
    private val widgetViews = mutableMapOf<Int, LauncherAppWidgetHostView>()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Initialize the widget manager with context.
     * Should be called when the launcher activity starts.
     */
    fun init(context: Context) {
        if (appWidgetHost == null) {
            appWidgetHost = LauncherAppWidgetHost(context.applicationContext, LauncherAppWidgetHost.HOST_ID)
            appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
        }
    }

    /**
     * Start listening for widget updates.
     * Should be called in Activity.onStart()
     */
    fun startListening() {
        try {
            appWidgetHost?.startListening()
        } catch (e: Exception) {
            // Host might already be listening
        }
        // Re-bind every cached host view to its provider. After the host has
        // been stopped (onStop -> onStart) the AppWidgetService only pushes
        // the latest RemoteViews to bindings that are re-established on
        // re-connect; cached views from before the stop otherwise stay
        // frozen at whatever RemoteViews they had the moment we stopped
        // listening. See issue #11.
        val manager = appWidgetManager ?: return
        for ((id, view) in widgetViews) {
            val providerInfo = manager.getAppWidgetInfo(id) ?: continue
            try {
                view.setAppWidget(id, providerInfo)
            } catch (e: Exception) {
                android.util.Log.w("WidgetManager", "Failed to rebind widget id=$id on resume", e)
            }
        }
    }

    /**
     * Stop listening for widget updates.
     * Should be called in Activity.onStop()
     */
    fun stopListening() {
        try {
            appWidgetHost?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Allocate a new widget ID for binding.
     */
    fun allocateWidgetId(): Int {
        return appWidgetHost?.allocateAppWidgetId() ?: -1
    }

    /**
     * Delete a widget ID and clean up resources.
     */
    fun deleteWidgetId(appWidgetId: Int) {
        appWidgetHost?.deleteAppWidgetId(appWidgetId)
    }

    /**
     * Get the AppWidgetManager instance.
     */
    fun getAppWidgetManager(): AppWidgetManager? = appWidgetManager

    /**
     * Get the AppWidgetHost instance.
     */
    fun getAppWidgetHost(): LauncherAppWidgetHost? = appWidgetHost

    /**
     * Check if a widget needs to be bound (has permission).
     */
    fun bindWidget(context: Context, appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        val manager = appWidgetManager ?: return false

        // Try to bind the widget
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        } else {
            @Suppress("DEPRECATION")
            manager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        }
    }

    /**
     * Request bind permission for a widget.
     * Returns an intent to launch if permission is needed.
     */
    fun createBindIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, providerInfo.profile)
            }
        }
    }

    /**
     * Check if a widget needs configuration before it can be used.
     */
    fun needsConfiguration(providerInfo: AppWidgetProviderInfo): Boolean {
        return providerInfo.configure != null
    }

    /**
     * Create an intent to configure a widget.
     */
    fun createConfigureIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent? {
        val configureComponent = providerInfo.configure ?: return null
        return Intent().apply {
            component = configureComponent
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    /**
     * Create a widget host view for the given widget ID.
     * Caches the view for later access (e.g., for resize operations).
     */
    fun createWidgetView(context: Context, appWidgetId: Int): LauncherAppWidgetHostView? {
        val host = appWidgetHost ?: return null
        val manager = appWidgetManager ?: return null
        val providerInfo = manager.getAppWidgetInfo(appWidgetId) ?: return null

        return try {
            val view = host.createView(context, appWidgetId, providerInfo) as? LauncherAppWidgetHostView
            if (view != null) {
                widgetViews[appWidgetId] = view
            }
            view
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Failed to create widget view for id=$appWidgetId", e)
            null
        }
    }

    /**
     * Get a cached widget view by ID.
     */
    fun getWidgetView(appWidgetId: Int): LauncherAppWidgetHostView? {
        return widgetViews[appWidgetId]
    }

    /**
     * Get a cached widget view or create a new one if not cached.
     * Reusing cached views avoids content flash when HorizontalPager
     * re-composes a page (e.g., during cross-page drag scroll-back).
     */
    fun getOrCreateWidgetView(context: Context, appWidgetId: Int): LauncherAppWidgetHostView? {
        return widgetViews[appWidgetId] ?: createWidgetView(context, appWidgetId)
    }

    /**
     * Update a widget's rendered size. Sizes must be in dp (not px).
     * This calls updateAppWidgetSize on the widget's host view.
     */
    fun updateWidgetViewSize(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        val view = widgetViews[appWidgetId] ?: return
        try {
            view.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp)
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Failed to update widget size for id=$appWidgetId", e)
        }
    }

    /**
     * Remove a widget view from the cache.
     */
    fun removeWidgetView(appWidgetId: Int) {
        widgetViews.remove(appWidgetId)
    }

    /**
     * Re-bind every cached host view to its current provider info. Called
     * by `LauncherAppWidgetHost.onProvidersChanged` when *any* provider
     * package on the device is added / updated / removed. Matches what
     * Launcher3 does so that a freshly-installed APK (e.g. a widget app
     * the user just updated from the Play Store) immediately renders
     * with the new provider's metadata + RemoteViews.
     */
    fun rebindAllCachedViews() {
        val manager = appWidgetManager ?: return
        for ((id, view) in widgetViews) {
            val info = manager.getAppWidgetInfo(id) ?: continue
            try {
                view.setAppWidget(id, info)
            } catch (e: Exception) {
                android.util.Log.w("WidgetManager", "rebindAllCachedViews failed for id=$id", e)
            }
        }
    }

    /**
     * Re-bind a single cached host view. Called by
     * `LauncherAppWidgetHost.onProviderChanged` for the specific provider
     * that was updated.
     */
    fun rebindCachedView(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        val view = widgetViews[appWidgetId] ?: return
        try {
            view.setAppWidget(appWidgetId, providerInfo)
        } catch (e: Exception) {
            android.util.Log.w("WidgetManager", "rebindCachedView failed for id=$appWidgetId", e)
        }
    }

    /**
     * Called by `LauncherAppWidgetHost.onAppWidgetRemoved` when the
     * system removes a widget on its own — for example, when the
     * provider's APK is uninstalled. Strips the persisted `PlacedWidget`
     * entry, deletes the host ID, and clears the cached view so the
     * user doesn't see a ghost cell on the home screen.
     */
    fun handleProviderRemovedWidget(context: Context, appWidgetId: Int) {
        widgetViews.remove(appWidgetId)
        val widgets = loadPlacedWidgets(context).filter { it.appWidgetId != appWidgetId }
        savePlacedWidgets(context, widgets)
        try {
            appWidgetHost?.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {
            // Already deleted by the system; ignore.
        }
    }

    /**
     * API 35+ hint — pairs activity-resumed state with the host so the
     * framework can defer non-critical updates while the launcher is
     * paused (animation hand-off, etc.). No-op on older platforms.
     *
     * Called via reflection because AGP 8.2.0 (this project's plugin)
     * doesn't expose the API 35 stub at compile time even with
     * compileSdk = 35, so a direct method reference fails to compile.
     */
    fun setActivityResumed(resumed: Boolean) {
        if (Build.VERSION.SDK_INT < 35) return
        val host = appWidgetHost ?: return
        try {
            val method = host.javaClass.getMethod("setActivityResumed", java.lang.Boolean.TYPE)
            method.invoke(host, resumed)
        } catch (_: Throwable) {
            // Method not available on this device; ignore.
        }
    }

    /**
     * Re-apply rounded corner settings to all cached widget views.
     * Called when the user changes the rounded corners toggle or radius.
     */
    fun refreshAllWidgetCorners(context: Context) {
        for ((_, view) in widgetViews) {
            view.applyRoundedCorners(context)
        }
    }

    /**
     * Recreate a single widget view (needed when its per-widget font scale changes).
     * The new view will use the updated Context from LauncherAppWidgetHost.
     */
    fun recreateWidgetView(context: Context, appWidgetId: Int): LauncherAppWidgetHostView? {
        val host = appWidgetHost ?: return null
        val manager = appWidgetManager ?: return null
        val providerInfo = manager.getAppWidgetInfo(appWidgetId) ?: return null
        widgetViews.remove(appWidgetId)
        return try {
            val view = host.createView(context, appWidgetId, providerInfo) as? LauncherAppWidgetHostView
            if (view != null) {
                widgetViews[appWidgetId] = view
            }
            view
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Failed to recreate widget view for id=$appWidgetId", e)
            null
        }
    }

    /**
     * Recreate all cached widget views (needed when font scale changes).
     * The new views will use the updated Context from LauncherAppWidgetHost.
     */
    fun recreateAllWidgetViews(context: Context) {
        val host = appWidgetHost ?: return
        val manager = appWidgetManager ?: return
        val ids = widgetViews.keys.toList()
        widgetViews.clear()
        for (id in ids) {
            val providerInfo = manager.getAppWidgetInfo(id) ?: continue
            try {
                val view = host.createView(context, id, providerInfo) as? LauncherAppWidgetHostView
                if (view != null) {
                    widgetViews[id] = view
                }
            } catch (e: Exception) {
                android.util.Log.e("WidgetManager", "Failed to recreate widget view for id=$id", e)
            }
        }
    }

    /**
     * Stack two widgets together. The dropped widget takes the target widget's position/size.
     * Both widgets get the same stackId.
     */
    fun stackWidgets(context: Context, droppedWidgetId: Int, targetWidgetId: Int): List<PlacedWidget> {
        val widgets = loadPlacedWidgets(context).toMutableList()
        val target = widgets.find { it.appWidgetId == targetWidgetId } ?: return widgets
        val dropped = widgets.find { it.appWidgetId == droppedWidgetId } ?: return widgets

        // Use existing stackId or create a new one
        val stackId = target.stackId ?: "stack_${System.currentTimeMillis()}"

        // Find the highest stackOrder in this stack
        val maxOrder = widgets.filter { it.stackId == stackId }.maxOfOrNull { it.stackOrder } ?: 0

        // Update target to be in the stack (if not already)
        val updatedWidgets = widgets.map { w ->
            when (w.appWidgetId) {
                targetWidgetId -> w.copy(stackId = stackId, stackOrder = if (w.stackId == null) 0 else w.stackOrder)
                droppedWidgetId -> w.copy(
                    stackId = stackId,
                    stackOrder = maxOrder + 1,
                    startColumn = target.startColumn,
                    startRow = target.startRow,
                    columnSpan = target.columnSpan,
                    rowSpan = target.rowSpan,
                    page = target.page
                )
                else -> w
            }
        }

        savePlacedWidgets(context, updatedWidgets)
        return updatedWidgets
    }

    /**
     * Remove a widget from its stack and delete it from the home screen.
     * If only one widget remains in the stack, dissolve the stack.
     */
    fun removeFromStack(context: Context, appWidgetId: Int): List<PlacedWidget> {
        val widgets = loadPlacedWidgets(context).toMutableList()
        val widget = widgets.find { it.appWidgetId == appWidgetId } ?: return widgets
        val stackId = widget.stackId ?: return widgets

        // Remove the widget from the list
        val remaining = widgets.filter { it.appWidgetId != appWidgetId }

        // If only one widget left in the stack, dissolve it
        val updatedWidgets = remaining.map { w ->
            if (w.stackId == stackId) {
                val stillInStack = remaining.count { it.stackId == stackId }
                if (stillInStack <= 1) w.copy(stackId = null, stackOrder = 0) else w
            } else w
        }

        deleteWidgetId(appWidgetId)
        removeWidgetView(appWidgetId)
        savePlacedWidgets(context, updatedWidgets)
        return updatedWidgets
    }

    /**
     * Get all widgets in a stack, ordered by stackOrder.
     */
    fun getStackWidgets(widgets: List<PlacedWidget>, stackId: String): List<PlacedWidget> {
        return widgets.filter { it.stackId == stackId }.sortedBy { it.stackOrder }
    }

    /**
     * Calculate the number of grid cells a widget needs.
     * Based on Fossify Launcher approach:
     * - On Android S+ (API 31), prefer targetCellWidth/targetCellHeight if non-zero
     * - Otherwise calculate from minWidth/minHeight using formula: ceil((dpValue - 30) / 70)
     * - minWidth/minHeight are ALWAYS in pixels on all Android versions
     */
    fun calculateCellSpan(context: Context, providerInfo: AppWidgetProviderInfo): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetWidth = providerInfo.targetCellWidth
            val targetHeight = providerInfo.targetCellHeight
            if (targetWidth > 0 && targetHeight > 0) {
                // Use target cell dimensions directly (most accurate)
                Pair(targetWidth, targetHeight)
            } else {
                // Calculate from pixel dimensions
                Pair(
                    calculateCellCount(providerInfo.minWidth, density),
                    calculateCellCount(providerInfo.minHeight, density)
                )
            }
        } else {
            // Pre-Android S: calculate from pixel dimensions
            Pair(
                calculateCellCount(providerInfo.minWidth, density),
                calculateCellCount(providerInfo.minHeight, density)
            )
        }
    }

    /**
     * Calculate number of cells needed for a widget dimension.
     * Based on Fossify Launcher formula: ceil((dpValue - 30) / 70)
     * where 30dp is padding and 70dp is standard cell size.
     */
    private fun calculateCellCount(sizeInPixels: Int, density: Float): Int {
        val sizeInDp = sizeInPixels / density
        val cells = kotlin.math.ceil((sizeInDp - 30) / 70.0).toInt()
        return maxOf(cells, 1)
    }

    // ---- Persistence ----

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save placed widgets to persistent storage.
     */
    fun savePlacedWidgets(context: Context, widgets: List<PlacedWidget>) {
        val jsonString = json.encodeToString(widgets)
        getPrefs(context).edit().putString(KEY_PLACED_WIDGETS, jsonString).apply()
    }

    /**
     * Load placed widgets from persistent storage.
     */
    fun loadPlacedWidgets(context: Context): List<PlacedWidget> {
        val jsonString = getPrefs(context).getString(KEY_PLACED_WIDGETS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<PlacedWidget>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a widget to the placed widgets list.
     */
    fun addPlacedWidget(context: Context, widget: PlacedWidget) {
        val widgets = loadPlacedWidgets(context).toMutableList()
        widgets.add(widget)
        savePlacedWidgets(context, widgets)
    }

    /**
     * Remove a widget from the placed widgets list.
     */
    fun removePlacedWidget(context: Context, appWidgetId: Int) {
        val widgets = loadPlacedWidgets(context).filter { it.appWidgetId != appWidgetId }
        savePlacedWidgets(context, widgets)
        deleteWidgetId(appWidgetId)
        // FIX: Clean the cached LauncherAppWidgetHostView alongside the host-ID deletion
        // so a leftover orphan view can't be re-parented into a sibling widget's container
        // during recomposition (caused the "removed widget takes over another widget's slot"
        // bug). Matches the cleanup order used by removeFromStack().
        removeWidgetView(appWidgetId)
    }

    /**
     * Update a widget's position and size (Einstein style).
     * If the widget is in a stack, all widgets in the stack are updated to match.
     */
    fun updateWidget(
        context: Context,
        appWidgetId: Int,
        startColumn: Int,
        startRow: Int,
        columnSpan: Int,
        rowSpan: Int
    ) {
        val widgets = loadPlacedWidgets(context)
        val target = widgets.find { it.appWidgetId == appWidgetId }
        val stackId = target?.stackId

        val updatedWidgets = widgets.map {
            if (it.appWidgetId == appWidgetId || (stackId != null && it.stackId == stackId)) {
                it.copy(
                    startColumn = startColumn,
                    startRow = startRow,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan
                )
            } else it
        }
        savePlacedWidgets(context, updatedWidgets)
    }

    /**
     * Update a widget's position and size (legacy compatibility).
     */
    fun updateWidgetPositionAndSize(
        context: Context,
        appWidgetId: Int,
        row: Int,
        column: Int,
        spanRows: Int,
        spanColumns: Int
    ) {
        updateWidget(context, appWidgetId, column, row, spanColumns, spanRows)
    }

    /**
     * Update a widget's position only.
     * If the widget is in a stack, all widgets in the stack are updated to match.
     */
    fun updateWidgetPosition(context: Context, appWidgetId: Int, startColumn: Int, startRow: Int, page: Int? = null) {
        val widgets = loadPlacedWidgets(context)
        val target = widgets.find { it.appWidgetId == appWidgetId }
        val stackId = target?.stackId

        val updatedWidgets = widgets.map {
            if (it.appWidgetId == appWidgetId || (stackId != null && it.stackId == stackId)) {
                it.copy(startColumn = startColumn, startRow = startRow, page = page ?: it.page)
            } else it
        }
        savePlacedWidgets(context, updatedWidgets)
    }

    /**
     * Update a widget's size only.
     * If the widget is in a stack, all widgets in the stack are updated to match.
     */
    fun updateWidgetSize(context: Context, appWidgetId: Int, columnSpan: Int, rowSpan: Int) {
        val widgets = loadPlacedWidgets(context)
        val target = widgets.find { it.appWidgetId == appWidgetId }
        val stackId = target?.stackId

        val updatedWidgets = widgets.map {
            if (it.appWidgetId == appWidgetId || (stackId != null && it.stackId == stackId)) {
                it.copy(columnSpan = columnSpan, rowSpan = rowSpan)
            } else it
        }
        savePlacedWidgets(context, updatedWidgets)
    }

    /**
     * Get minimum resize dimensions for a widget (in cells).
     * Based on minResizeWidth/minResizeHeight from provider info.
     */
    fun getMinResizeCells(context: Context, providerInfo: AppWidgetProviderInfo): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density

        val minResizeWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.minResizeWidth
        } else {
            providerInfo.minWidth // Fallback to minWidth if minResizeWidth not available
        }

        val minResizeHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.minResizeHeight
        } else {
            providerInfo.minHeight
        }

        return Pair(
            calculateCellCount(minResizeWidth, density),
            calculateCellCount(minResizeHeight, density)
        )
    }

    /**
     * Get maximum resize dimensions for a widget (in cells).
     * Based on maxResizeWidth/maxResizeHeight from provider info (Android S+).
     */
    fun getMaxResizeCells(context: Context, providerInfo: AppWidgetProviderInfo, maxGridCols: Int, maxGridRows: Int): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val density = context.resources.displayMetrics.density
            val maxWidth = if (providerInfo.maxResizeWidth > 0) {
                calculateCellCount(providerInfo.maxResizeWidth, density)
            } else {
                maxGridCols
            }
            val maxHeight = if (providerInfo.maxResizeHeight > 0) {
                calculateCellCount(providerInfo.maxResizeHeight, density)
            } else {
                maxGridRows
            }
            return Pair(minOf(maxWidth, maxGridCols), minOf(maxHeight, maxGridRows))
        }
        return Pair(maxGridCols, maxGridRows)
    }

    /**
     * Check if a widget supports resizing.
     */
    fun canResize(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            providerInfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE
        } else {
            false
        }
    }

    /**
     * Check if a widget can resize horizontally.
     */
    fun canResizeHorizontally(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
        } else {
            false
        }
    }

    /**
     * Check if a widget can resize vertically.
     */
    fun canResizeVertically(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0
        } else {
            false
        }
    }
}