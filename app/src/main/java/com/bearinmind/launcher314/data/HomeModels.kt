package com.bearinmind.launcher314.data

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import kotlinx.serialization.Serializable

// ========== Einstein Launcher Style Data Model ==========

/**
 * Type of grid item (app or widget)
 */
enum class GridItemType {
    APP,
    WIDGET
}

/**
 * Unified grid item supporting both apps and widgets with row/column positioning.
 * Based on Einstein Launcher / AOSP Launcher3 approach.
 */
@Serializable
data class GridItem(
    val id: String,                    // Unique identifier (packageName for apps, "widget_$appWidgetId" for widgets)
    val type: GridItemType,
    val row: Int,                      // Grid row (0-based)
    val column: Int,                   // Grid column (0-based)
    val rowSpan: Int = 1,              // Height in cells (always 1 for apps)
    val columnSpan: Int = 1,           // Width in cells (1 for apps, variable for widgets)

    // App-specific data
    val packageName: String? = null,

    // Widget-specific data
    val appWidgetId: Int? = null,
    val widgetClassName: String? = null
)

/**
 * Home screen data with unified GridItem model
 */
@Serializable
data class HomeScreenDataV2(
    val items: List<GridItem> = emptyList(),
    val dockApps: List<DockApp> = emptyList()
)

// ========== Legacy Data Model (for migration) ==========

// Data class for home screen apps with grid position
@Serializable
data class HomeScreenApp(
    val packageName: String,
    val position: Int, // Grid position (0-based index)
    val page: Int = 0 // Which screen/page this app is on
)

// Data class for dock apps (bottom bar)
@Serializable
data class DockApp(
    val packageName: String,
    val position: Int, // Dock position within a page (0-4 typically)
    val page: Int = 0  // Which dock page this app is on (default 0 for backward compat)
)

// Data class for dock folders (bottom bar)
@Serializable
data class DockFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val position: Int,
    val appPackageNames: List<String> = emptyList(),
    val page: Int = 0  // Which dock page this folder is on
)

// Data class for home screen folders
@Serializable
data class HomeFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val position: Int, // Grid position (0-based index)
    val page: Int = 0,
    val appPackageNames: List<String> = emptyList()
)

@Serializable
data class HomeScreenData(
    val apps: List<HomeScreenApp> = emptyList(),
    val dockApps: List<DockApp> = emptyList(),
    val folders: List<HomeFolder> = emptyList(),
    val dockFolders: List<DockFolder> = emptyList()
)

// App info for display
data class HomeAppInfo(
    val name: String,
    val packageName: String,
    val iconPath: String,
    val customization: AppCustomization? = null
)

// ========== Grid Cell Model (for rendering) ==========

// Represents a cell in the home screen grid (can be empty, contain an app, or a widget)
sealed class HomeGridCell {
    object Empty : HomeGridCell()
    data class App(val appInfo: HomeAppInfo, val position: Int) : HomeGridCell()
    data class Folder(val folder: HomeFolder, val previewApps: List<HomeAppInfo>, val position: Int) : HomeGridCell()
    data class Widget(val placedWidget: PlacedWidget, val position: Int) : HomeGridCell()
    // For cells occupied by a multi-cell widget (not the origin cell)
    data class WidgetSpan(val originPosition: Int) : HomeGridCell()
}

/** Holds remove-from-home animation state (consolidated to reduce register count in LauncherScreen). */
class RemoveAnimState {
    var gridKey by mutableStateOf<Pair<Int, Int>?>(null) // (position, page)
    var dockSlot by mutableStateOf<Int?>(null)
    val anim = Animatable(1f)
}

// ========== Migration Helpers ==========

/**
 * Convert flat index position to row/column (Einstein Launcher style)
 */
fun flatIndexToRowColumn(position: Int, gridColumns: Int): Pair<Int, Int> {
    val row = position / gridColumns
    val column = position % gridColumns
    return row to column
}

/**
 * Convert row/column to flat index
 */
fun rowColumnToFlatIndex(row: Int, column: Int, gridColumns: Int): Int {
    return row * gridColumns + column
}

/**
 * Migrate legacy HomeScreenApp to GridItem
 */
fun migrateAppToGridItem(app: HomeScreenApp, gridColumns: Int): GridItem {
    val (row, column) = flatIndexToRowColumn(app.position, gridColumns)
    return GridItem(
        id = app.packageName,
        type = GridItemType.APP,
        row = row,
        column = column,
        rowSpan = 1,
        columnSpan = 1,
        packageName = app.packageName
    )
}

/**
 * Migrate PlacedWidget to GridItem
 */
fun migrateWidgetToGridItem(widget: PlacedWidget): GridItem {
    return GridItem(
        id = "widget_${widget.appWidgetId}",
        type = GridItemType.WIDGET,
        row = widget.startRow,
        column = widget.startColumn,
        rowSpan = widget.rowSpan,
        columnSpan = widget.columnSpan,
        appWidgetId = widget.appWidgetId,
        widgetClassName = widget.className
    )
}

// ========== Collision Detection ==========

/**
 * Get all cells occupied by a grid item
 */
fun getOccupiedCells(item: GridItem): Set<Pair<Int, Int>> {
    val cells = mutableSetOf<Pair<Int, Int>>()
    for (r in item.row until item.row + item.rowSpan) {
        for (c in item.column until item.column + item.columnSpan) {
            cells.add(r to c)
        }
    }
    return cells
}

/**
 * Check if an item can be placed at a position without collisions
 */
fun canPlaceGridItem(
    row: Int, column: Int,
    rowSpan: Int, columnSpan: Int,
    gridRows: Int, gridColumns: Int,
    occupiedCells: Set<Pair<Int, Int>>,
    excludeItemId: String? = null,
    allItems: List<GridItem> = emptyList()
): Boolean {
    // Check bounds
    if (row < 0 || column < 0) return false
    if (row + rowSpan > gridRows) return false
    if (column + columnSpan > gridColumns) return false

    // Build set of cells occupied by other items
    val cellsToCheck = if (excludeItemId != null) {
        val excludedCells = allItems.find { it.id == excludeItemId }?.let { getOccupiedCells(it) } ?: emptySet()
        occupiedCells - excludedCells
    } else {
        occupiedCells
    }

    // Check if any required cell is occupied
    for (r in row until row + rowSpan) {
        for (c in column until column + columnSpan) {
            if (Pair(r, c) in cellsToCheck) return false
        }
    }
    return true
}
