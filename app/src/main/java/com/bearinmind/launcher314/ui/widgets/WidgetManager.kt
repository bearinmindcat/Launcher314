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
import com.bearinmind.launcher314.data.getHomeGridRows
import com.bearinmind.launcher314.data.getHomeGridSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Data class representing a placed widget on the home screen. Uses Einstein Launcher's grid item model: startColumn, startRow, columnSpan, rowSpan. */
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
    val fontScalePercent: Int? = null, // Per-widget text size override (null = use global)
    // Per-widget corner roundness override (null = global); when set it wins over the global toggle.
    val cornerRadiusPercent: Int? = null,
    // Stack slideshow: pager auto-advances every intervalSec; stored on EVERY stack member so any one can be read.
    val stackSlideshowEnabled: Boolean = false,
    val stackSlideshowIntervalSec: Int = 10
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

/** Singleton manager for app widgets on the home screen. Handles widget lifecycle: allocation, binding, configuration, and persistence. */
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

    /** Init on launcher start. Host is created ONCE with the app context and never rebuilt — rebuilding on recreation flipped rendered widgets to "Can't show content"; views still get the Activity context via createView(). */
    fun init(context: Context) {
        if (appWidgetHost == null) {
            appWidgetHost = LauncherAppWidgetHost(context.applicationContext, LauncherAppWidgetHost.HOST_ID)
            appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
        }
    }

    /** Start listening for widget updates. Should be called in Activity.onStart() */
    fun startListening() {
        try {
            appWidgetHost?.startListening()
        } catch (e: Exception) {
            // Host might already be listening
        }
        // Re-bind every cached host view — after a stop the service only pushes RemoteViews to re-established bindings; stale views stay frozen otherwise (issue #11).
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

    /** Stop listening for widget updates. Should be called in Activity.onStop() */
    fun stopListening() {
        try {
            appWidgetHost?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /** Allocate a new widget ID for binding. */
    fun allocateWidgetId(): Int {
        return appWidgetHost?.allocateAppWidgetId() ?: -1
    }

    /** Delete a widget ID and clean up resources. */
    fun deleteWidgetId(appWidgetId: Int) {
        appWidgetHost?.deleteAppWidgetId(appWidgetId)
    }

    /** Get the AppWidgetManager instance. */
    fun getAppWidgetManager(): AppWidgetManager? = appWidgetManager

    /** Get the AppWidgetHost instance. */
    fun getAppWidgetHost(): LauncherAppWidgetHost? = appWidgetHost

    /** Check if a widget needs to be bound (has permission). */
    fun bindWidget(context: Context, appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        val manager = appWidgetManager ?: return false

        // Seed a size-options bundle AT BIND (Launcher3's 3-arg bind) — Glance widgets compose from OPTION_APPWIDGET_SIZES at bind and never recover from a size-less first composition.
        val d = context.resources.displayMetrics.density
        val wdp = (providerInfo.minWidth / d).toInt().coerceAtLeast(40)
        val hdp = (providerInfo.minHeight / d).toInt().coerceAtLeast(40)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wdp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hdp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wdp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hdp)
            if (Build.VERSION.SDK_INT >= 31) {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(android.util.SizeF(wdp.toFloat(), hdp.toFloat()))
                )
            }
        }
        return try {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider, options)
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            manager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        }
    }

    /** Request bind permission for a widget. Returns an intent to launch if permission is needed. */
    fun createBindIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, providerInfo.profile)
            }
        }
    }

    /** Check if a widget needs configuration before it can be used. */
    fun needsConfiguration(providerInfo: AppWidgetProviderInfo): Boolean {
        return providerInfo.configure != null
    }

    /** Create an intent to configure a widget. */
    fun createConfigureIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent? {
        val configureComponent = providerInfo.configure ?: return null
        return Intent().apply {
            component = configureComponent
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    /** Create a widget host view for the given widget ID. Caches the view for later access (e.g., for resize operations). */
    fun createWidgetView(context: Context, appWidgetId: Int): LauncherAppWidgetHostView? {
        val host = appWidgetHost ?: return null
        val manager = appWidgetManager ?: return null
        val providerInfo = manager.getAppWidgetInfo(appWidgetId) ?: return null

        return try {
            val view = host.createView(context, appWidgetId, providerInfo) as? LauncherAppWidgetHostView
            if (view != null) {
                widgetViews[appWidgetId] = view
                // Glance renders "Can't show content" until OPTION_APPWIDGET_SIZES is published — seed from the provider's min size NOW; onGloballyPositioned refines to the real cell size later.
                try {
                    val d = context.resources.displayMetrics.density
                    updateWidgetViewSize(
                        appWidgetId,
                        (providerInfo.minWidth / d).toInt().coerceAtLeast(40),
                        (providerInfo.minHeight / d).toInt().coerceAtLeast(40)
                    )
                } catch (_: Exception) {}
            }
            view
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Failed to create widget view for id=$appWidgetId", e)
            null
        }
    }

    /** Get a cached widget view by ID. */
    fun getWidgetView(appWidgetId: Int): LauncherAppWidgetHostView? {
        return widgetViews[appWidgetId]
    }

    /** Get a cached widget view or create a new one if not cached. Reusing cached views avoids content flash when HorizontalPager re-composes a page (e.g., during cross-page drag scroll-back). */
    fun getOrCreateWidgetView(context: Context, appWidgetId: Int): LauncherAppWidgetHostView? {
        return widgetViews[appWidgetId] ?: createWidgetView(context, appWidgetId)
    }

    /** Update a widget's rendered size. Sizes must be in dp (not px). This calls updateAppWidgetSize on the widget's host view. */
    fun updateWidgetViewSize(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        val view = widgetViews[appWidgetId] ?: return
        val w = widthDp.coerceAtLeast(1)
        val h = heightDp.coerceAtLeast(1)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // Android 12+ single-call overload sets min/max + non-empty SIZES atomically — the legacy 4-arg path pushes an EMPTY sizes list first, which sticks Glance widgets in "Can't show content".
                view.updateAppWidgetSize(
                    Bundle(),
                    listOf(android.util.SizeF(w.toFloat(), h.toFloat()))
                )
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(Bundle(), w, h, w, h)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Failed to update widget size for id=$appWidgetId", e)
        }
    }

    /** Remove a widget view from the cache. */
    fun removeWidgetView(appWidgetId: Int) {
        widgetViews.remove(appWidgetId)
    }

    /** Drop every cached host view — MUST run in Activity.onDestroy(): views hold the ACTIVITY context in a singleton map, so keeping them across recreation leaks the dead activity + composition (progressive drawer lag); views recreate lazily and the app-context host keeps listening. */
    fun clearViewCache() {
        widgetViews.clear()
    }

    /** Re-bind every cached view on onProvidersChanged (any provider added/updated/removed) — Launcher3's pattern so a freshly-updated widget APK renders immediately. */
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

    /** Re-bind a single cached host view. Called by `LauncherAppWidgetHost.onProviderChanged` for the specific provider that was updated. */
    fun rebindCachedView(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        val view = widgetViews[appWidgetId] ?: return
        try {
            view.setAppWidget(appWidgetId, providerInfo)
        } catch (e: Exception) {
            android.util.Log.w("WidgetManager", "rebindCachedView failed for id=$appWidgetId", e)
        }
    }

    /** On system-initiated widget removal (e.g. provider uninstalled): strip the persisted entry, delete the host ID, and clear the cached view so no ghost cell remains. */
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

    /** API 35+ resumed-state hint for the host; called via reflection because AGP 8.2.0 doesn't expose the API 35 stub at compile time. */
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

    /** Re-apply rounded corner settings to all cached widget views. Called when the user changes the rounded corners toggle or radius. */
    fun refreshAllWidgetCorners(context: Context) {
        for ((_, view) in widgetViews) {
            view.applyRoundedCorners(context)
        }
    }

    /** Recreate a single widget view (needed when its per-widget font scale changes). The new view will use the updated Context from LauncherAppWidgetHost. */
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

    /** Recreate all cached widget views (needed when font scale changes). The new views will use the updated Context from LauncherAppWidgetHost. */
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

    /** Stack two widgets: the dropped one takes the target's position/size and both share a stackId. */
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

    /** Remove a widget from its stack and delete it from the home screen. If only one widget remains in the stack, dissolve the stack. */
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

    /** Get all widgets in a stack, ordered by stackOrder. */
    fun getStackWidgets(widgets: List<PlacedWidget>, stackId: String): List<PlacedWidget> {
        return widgets.filter { it.stackId == stackId }.sortedBy { it.stackOrder }
    }

    /** Apply slideshow settings to every stack member and persist; returns the updated list for the caller's state. */
    fun setStackSlideshow(
        context: Context,
        widgets: List<PlacedWidget>,
        stackId: String,
        enabled: Boolean,
        intervalSec: Int
    ): List<PlacedWidget> {
        val updated = widgets.map {
            if (it.stackId == stackId) {
                it.copy(
                    stackSlideshowEnabled = enabled,
                    stackSlideshowIntervalSec = intervalSec.coerceIn(5, 65)
                )
            } else it
        }
        savePlacedWidgets(context, updated)
        return updated
    }

    /** The home grid's real cell size in dp (width, height) — same math as LauncherScreen's grid layout, so spans match what actually renders. */
    fun homeCellSizeDp(context: Context): Pair<Float, Float> {
        val config = context.resources.configuration
        val screenWidthDp = config.screenWidthDp.toFloat()
        val screenHeightDp = config.screenHeightDp.toFloat()
        val gridColumns = getHomeGridSize(context).coerceAtLeast(1)
        val gridRows = getHomeGridRows(context).coerceAtLeast(1)
        val hPadF = com.bearinmind.launcher314.data.homeGridHPadFactor(context)
        val cellWidth = (screenWidthDp - screenWidthDp * hPadF * 2) / gridColumns
        val cellHeight = (screenHeightDp - 76f - screenWidthDp * 0.022f * 2) / gridRows
        return Pair(cellWidth.coerceAtLeast(1f), cellHeight.coerceAtLeast(1f))
    }

    /** Cells a widget needs (Launcher3 approach): declared targetCell sizes on S+, else min sizes against the REAL cell size — always clamped to the grid. */
    fun calculateCellSpan(context: Context, providerInfo: AppWidgetProviderInfo): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        val (cellW, cellH) = homeCellSizeDp(context)

        val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            providerInfo.targetCellWidth > 0 && providerInfo.targetCellHeight > 0
        ) {
            Pair(providerInfo.targetCellWidth, providerInfo.targetCellHeight)
        } else {
            Pair(
                calculateCellCount(providerInfo.minWidth, density, cellW),
                calculateCellCount(providerInfo.minHeight, density, cellH)
            )
        }

        return Pair(
            target.first.coerceIn(1, getHomeGridSize(context).coerceAtLeast(1)),
            target.second.coerceIn(1, getHomeGridRows(context).coerceAtLeast(1))
        )
    }

    /** Do two widgets on the same page overlap? */
    private fun overlaps(a: PlacedWidget, b: PlacedWidget): Boolean {
        if (a.page != b.page) return false
        return a.startColumn < b.startColumn + b.columnSpan &&
            b.startColumn < a.startColumn + a.columnSpan &&
            a.startRow < b.startRow + b.rowSpan &&
            b.startRow < a.startRow + a.rowSpan
    }

    /** Re-flow widgets after a grid-size change (Launcher3's GridSizeMigrationLogic): keep-if-fits, else shrink + row-major scan for a vacancy, spilling to later pages; app/folder cells count as occupied. */
    fun reconcileWidgetsToGrid(context: Context, widgets: List<PlacedWidget>): List<PlacedWidget> {
        if (widgets.isEmpty()) return widgets
        val columns = getHomeGridSize(context).coerceAtLeast(1)
        val rows = getHomeGridRows(context).coerceAtLeast(1)

        // Fast path: everything already fits and nothing overlaps.
        val outOfBounds = widgets.any {
            it.columnSpan > columns || it.rowSpan > rows ||
                it.startColumn < 0 || it.startRow < 0 ||
                it.startColumn + it.columnSpan > columns ||
                it.startRow + it.rowSpan > rows
        }
        val overlapping = widgets.indices.any { i ->
            (i + 1 until widgets.size).any { j -> overlaps(widgets[i], widgets[j]) }
        }
        if (!outOfBounds && !overlapping) return widgets

        // Cells already used by apps / folders, per page.
        val home = com.bearinmind.launcher314.data.loadHomeScreenData(context)
        val staticCells = mutableMapOf<Int, MutableSet<Int>>()
        home.apps.forEach { staticCells.getOrPut(it.page) { mutableSetOf() }.add(it.position) }
        home.folders.forEach { staticCells.getOrPut(it.page) { mutableSetOf() }.add(it.position) }

        val occupied = mutableMapOf<Int, MutableSet<Int>>()
        fun cellsOf(page: Int) = occupied.getOrPut(page) {
            (staticCells[page] ?: emptySet<Int>()).filter { it in 0 until columns * rows }.toMutableSet()
        }
        fun regionVacant(page: Int, col: Int, row: Int, w: Int, h: Int): Boolean {
            if (col < 0 || row < 0 || col + w > columns || row + h > rows) return false
            val taken = cellsOf(page)
            for (r in row until row + h) for (c in col until col + w) {
                if ((r * columns + c) in taken) return false
            }
            return true
        }
        fun mark(page: Int, col: Int, row: Int, w: Int, h: Int) {
            val taken = cellsOf(page)
            for (r in row until row + h) for (c in col until col + w) taken.add(r * columns + c)
        }

        val maxPage = maxOf(widgets.maxOf { it.page }, home.apps.maxOfOrNull { it.page } ?: 0)
        // Original reading order, so relative layout is broadly preserved.
        val ordered = widgets.sortedWith(
            compareBy({ it.page }, { it.startRow }, { it.startColumn })
        )

        val result = ordered.map { w ->
            val span = w.columnSpan.coerceIn(1, columns)
            val rowSpan = w.rowSpan.coerceIn(1, rows)
            // Smallest this widget may legally shrink to, if we need the room.
            val minSpan = try {
                val info = appWidgetManager?.getAppWidgetInfo(w.appWidgetId)
                if (info != null) getMinResizeCells(context, info) else Pair(1, 1)
            } catch (_: Exception) { Pair(1, 1) }

            var placed: PlacedWidget? = null
            for (page in w.page..maxPage + 1) {
                // Only the original page may keep the original spot.
                if (page == w.page && regionVacant(page, w.startColumn, w.startRow, span, rowSpan)) {
                    placed = w.copy(columnSpan = span, rowSpan = rowSpan)
                    break
                }
                for ((tryW, tryH) in listOf(
                    span to rowSpan,
                    minSpan.first.coerceIn(1, span) to minSpan.second.coerceIn(1, rowSpan)
                )) {
                    outer@ for (r in 0..rows - tryH) {
                        for (c in 0..columns - tryW) {
                            if (regionVacant(page, c, r, tryW, tryH)) {
                                placed = w.copy(
                                    page = page, startColumn = c, startRow = r,
                                    columnSpan = tryW, rowSpan = tryH
                                )
                                break@outer
                            }
                        }
                    }
                    if (placed != null) break
                }
                if (placed != null) break
            }
            val final = placed ?: w.copy(
                columnSpan = span, rowSpan = rowSpan,
                startColumn = w.startColumn.coerceIn(0, columns - span),
                startRow = w.startRow.coerceIn(0, rows - rowSpan)
            )
            mark(final.page, final.startColumn, final.startRow, final.columnSpan, final.rowSpan)
            final
        }

        savePlacedWidgets(context, result)
        return result
    }

    /** Cells needed for one dimension: ceil(sizeDp / realCellDp), min 1. */
    private fun calculateCellCount(sizeInPixels: Int, density: Float, cellDp: Float): Int {
        val sizeInDp = sizeInPixels / density
        return maxOf(kotlin.math.ceil(sizeInDp / cellDp).toInt(), 1)
    }

    // ---- Persistence ----

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Save placed widgets to persistent storage. */
    fun savePlacedWidgets(context: Context, widgets: List<PlacedWidget>) {
        val jsonString = json.encodeToString(widgets)
        getPrefs(context).edit().putString(KEY_PLACED_WIDGETS, jsonString).apply()
    }

    /** Load placed widgets from persistent storage. */
    fun loadPlacedWidgets(context: Context): List<PlacedWidget> {
        val jsonString = getPrefs(context).getString(KEY_PLACED_WIDGETS, null) ?: return emptyList()
        return try {
            // Self-heal widgets left oversized by a grid-size change.
            reconcileWidgetsToGrid(context, json.decodeFromString<List<PlacedWidget>>(jsonString))
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Add a widget to the placed widgets list. */
    fun addPlacedWidget(context: Context, widget: PlacedWidget) {
        val widgets = loadPlacedWidgets(context).toMutableList()
        widgets.add(widget)
        savePlacedWidgets(context, widgets)
    }

    /** Remove a widget from the placed widgets list. */
    fun removePlacedWidget(context: Context, appWidgetId: Int) {
        val widgets = loadPlacedWidgets(context).filter { it.appWidgetId != appWidgetId }
        savePlacedWidgets(context, widgets)
        deleteWidgetId(appWidgetId)
        // Clean the cached host view with the host-ID deletion — an orphan view could get re-parented into a sibling's container ("removed widget takes over another slot" bug).
        removeWidgetView(appWidgetId)
    }

    /** Update a widget's position and size (Einstein style). If the widget is in a stack, all widgets in the stack are updated to match. */
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

    /** Update a widget's position and size (legacy compatibility). */
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

    /** Update a widget's position only. If the widget is in a stack, all widgets in the stack are updated to match. */
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

    /** Update a widget's size only. If the widget is in a stack, all widgets in the stack are updated to match. */
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

    /** Minimum resize dimensions in cells, from the provider's minResizeWidth/Height. */
    fun getMinResizeCells(context: Context, providerInfo: AppWidgetProviderInfo): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        val (cellW, cellH) = homeCellSizeDp(context)

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
            calculateCellCount(minResizeWidth, density, cellW)
                .coerceAtMost(getHomeGridSize(context).coerceAtLeast(1)),
            calculateCellCount(minResizeHeight, density, cellH)
                .coerceAtMost(getHomeGridRows(context).coerceAtLeast(1))
        )
    }

    /** Maximum resize dimensions in cells, from the provider's maxResizeWidth/Height (Android S+). */
    fun getMaxResizeCells(context: Context, providerInfo: AppWidgetProviderInfo, maxGridCols: Int, maxGridRows: Int): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val density = context.resources.displayMetrics.density
            val (cellW, cellH) = homeCellSizeDp(context)
            val maxWidth = if (providerInfo.maxResizeWidth > 0) {
                calculateCellCount(providerInfo.maxResizeWidth, density, cellW)
            } else {
                maxGridCols
            }
            val maxHeight = if (providerInfo.maxResizeHeight > 0) {
                calculateCellCount(providerInfo.maxResizeHeight, density, cellH)
            } else {
                maxGridRows
            }
            return Pair(minOf(maxWidth, maxGridCols), minOf(maxHeight, maxGridRows))
        }
        return Pair(maxGridCols, maxGridRows)
    }

    /** Check if a widget supports resizing. */
    fun canResize(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            providerInfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE
        } else {
            false
        }
    }

    /** Check if a widget can resize horizontally. */
    fun canResizeHorizontally(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
        } else {
            false
        }
    }

    /** Check if a widget can resize vertically. */
    fun canResizeVertically(providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0
        } else {
            false
        }
    }
}