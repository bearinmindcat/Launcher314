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
    val stackOrder: Int = 0       // Order within the stack (0 = first/primary)
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
     * Re-apply rounded corner settings to all cached widget views.
     * Called when the user changes the rounded corners toggle or radius.
     */
    fun refreshAllWidgetCorners(context: Context) {
        for (view in widgetViews.values) {
            view.applyRoundedCorners(context)
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
     * Unstack a widget — remove it from its stack and place it standalone.
     */
    fun unstackWidget(context: Context, appWidgetId: Int): List<PlacedWidget> {
        val widgets = loadPlacedWidgets(context).toMutableList()
        val widget = widgets.find { it.appWidgetId == appWidgetId } ?: return widgets
        val stackId = widget.stackId ?: return widgets

        val updatedWidgets = widgets.map { w ->
            if (w.appWidgetId == appWidgetId) {
                w.copy(stackId = null, stackOrder = 0)
            } else if (w.stackId == stackId) {
                // If only one widget remains in the stack, dissolve the stack
                val remainingInStack = widgets.count { it.stackId == stackId && it.appWidgetId != appWidgetId }
                if (remainingInStack <= 1) w.copy(stackId = null, stackOrder = 0) else w
            } else w
        }

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
    }

    /**
     * Update a widget's position and size (Einstein style).
     */
    fun updateWidget(
        context: Context,
        appWidgetId: Int,
        startColumn: Int,
        startRow: Int,
        columnSpan: Int,
        rowSpan: Int
    ) {
        val widgets = loadPlacedWidgets(context).map {
            if (it.appWidgetId == appWidgetId) {
                it.copy(
                    startColumn = startColumn,
                    startRow = startRow,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan
                )
            } else it
        }
        savePlacedWidgets(context, widgets)
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
     */
    fun updateWidgetPosition(context: Context, appWidgetId: Int, startColumn: Int, startRow: Int, page: Int? = null) {
        val widgets = loadPlacedWidgets(context).map {
            if (it.appWidgetId == appWidgetId) {
                it.copy(startColumn = startColumn, startRow = startRow, page = page ?: it.page)
            } else it
        }
        savePlacedWidgets(context, widgets)
    }

    /**
     * Update a widget's size only.
     */
    fun updateWidgetSize(context: Context, appWidgetId: Int, columnSpan: Int, rowSpan: Int) {
        val widgets = loadPlacedWidgets(context).map {
            if (it.appWidgetId == appWidgetId) {
                it.copy(columnSpan = columnSpan, rowSpan = rowSpan)
            } else it
        }
        savePlacedWidgets(context, widgets)
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