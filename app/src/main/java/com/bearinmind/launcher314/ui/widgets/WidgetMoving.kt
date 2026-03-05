package com.bearinmind.launcher314.ui.widgets

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.data.HomeGridCell

/**
 * WidgetMoving.kt - Contains widget drag and drop functionality for the launcher home screen.
 * Based on AOSP Launcher3 CellLayout.java positioning logic.
 */

/**
 * Data class to hold widget drag state
 */
data class WidgetDragState(
    val draggedWidget: PlacedWidget? = null,
    val dragOffset: Offset = Offset.Zero,
    val startPosition: Offset = Offset.Zero
)

/**
 * Calculate the center point of a widget region (AOSP regionToCenterPoint style).
 * For a widget spanning multiple cells, this gives the exact center of the bounding rectangle.
 *
 * AOSP formula (with gaps):
 *   centerX = left + (spanX * cellWidth + (spanX - 1) * widthGap) / 2
 *
 * Our formula (no explicit gaps - cells fill space):
 *   centerX = originCellPos.x + (spanColumns * cellWidth) / 2
 */
fun calculateWidgetCenter(
    originCellPos: Offset,
    spanColumns: Int,
    spanRows: Int,
    cellWidth: Int,
    cellHeight: Int
): Offset {
    val widgetWidth = spanColumns * cellWidth
    val widgetHeight = spanRows * cellHeight
    return Offset(
        originCellPos.x + widgetWidth / 2f,
        originCellPos.y + widgetHeight / 2f
    )
}

/**
 * Calculate the full bounding rectangle for a widget (AOSP regionToRect style).
 * Returns (left, top, width, height) in pixels.
 */
fun calculateWidgetBounds(
    originCellPos: Offset,
    spanColumns: Int,
    spanRows: Int,
    cellWidth: Int,
    cellHeight: Int
): WidgetBounds {
    val width = spanColumns * cellWidth
    val height = spanRows * cellHeight
    return WidgetBounds(
        left = originCellPos.x,
        top = originCellPos.y,
        width = width.toFloat(),
        height = height.toFloat()
    )
}

data class WidgetBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
    val center: Offset get() = Offset(centerX, centerY)
}

/**
 * Check if a widget can be placed at a given grid position.
 * Returns true if all cells needed for the widget are available (empty or part of the same widget).
 */
fun canPlaceWidgetAt(
    widget: PlacedWidget,
    targetCol: Int,
    targetRow: Int,
    gridColumns: Int,
    gridRows: Int,
    gridCells: List<HomeGridCell>
): Boolean {
    // Check bounds
    if (targetCol < 0 || targetRow < 0) return false
    if (targetCol + widget.columnSpan > gridColumns) return false
    if (targetRow + widget.rowSpan > gridRows) return false

    // Check if all cells are available
    for (row in targetRow until targetRow + widget.rowSpan) {
        for (col in targetCol until targetCol + widget.columnSpan) {
            val cellIndex = row * gridColumns + col
            val cell = gridCells.getOrNull(cellIndex)

            when (cell) {
                is HomeGridCell.Empty -> continue // Available
                is HomeGridCell.Widget -> {
                    // Only allow if it's the same widget being moved
                    if (cell.placedWidget.appWidgetId != widget.appWidgetId) return false
                }
                is HomeGridCell.WidgetSpan -> {
                    // Check if this span belongs to the widget being moved
                    val originCell = gridCells.getOrNull(cell.originPosition)
                    if (originCell is HomeGridCell.Widget &&
                        originCell.placedWidget.appWidgetId == widget.appWidgetId) {
                        continue // Part of the same widget, OK
                    }
                    return false
                }
                is HomeGridCell.App -> return false // Occupied by an app
                is HomeGridCell.Folder -> return false // Occupied by a folder
                null -> return false // Out of bounds
            }
        }
    }
    return true
}

/**
 * Calculate target cell (column, row) from drag position.
 * DEPRECATED: Use calculateWidgetDropTargetFromCenter for multi-cell widgets.
 */
fun calculateWidgetDropTarget(
    dragPosition: Offset,
    cellPositions: Map<Int, Offset>,
    cellSize: IntSize,
    gridColumns: Int
): Pair<Int, Int>? {
    if (cellSize.width <= 0 || cellSize.height <= 0) return null

    // Find which cell the drag position is over
    for ((index, cellPos) in cellPositions) {
        val cellLeft = cellPos.x
        val cellTop = cellPos.y
        val cellRight = cellLeft + cellSize.width
        val cellBottom = cellTop + cellSize.height

        if (dragPosition.x >= cellLeft && dragPosition.x < cellRight &&
            dragPosition.y >= cellTop && dragPosition.y < cellBottom) {
            val col = index % gridColumns
            val row = index / gridColumns
            return Pair(col, row)
        }
    }
    return null
}

/**
 * Calculate target cell (column, row) for a widget based on its CENTER position.
 * This is the AOSP/Einstein-style positioning where the widget's center determines placement.
 *
 * For a widget of size (colSpan, rowSpan), this finds where the top-left cell should be
 * so the widget is centered at the given position.
 *
 * @param centerPosition The CENTER position of the widget in screen coordinates
 * @param cellPositions Map of cell indices to their screen positions
 * @param cellSize Size of each cell
 * @param gridColumns Number of columns in the grid
 * @param gridRows Number of rows in the grid
 * @param colSpan Number of columns the widget spans
 * @param rowSpan Number of rows the widget spans
 * @return Pair of (column, row) for the widget's TOP-LEFT cell, or null if invalid
 */
fun calculateWidgetDropTargetFromCenter(
    centerPosition: Offset,
    cellPositions: Map<Int, Offset>,
    cellSize: IntSize,
    gridColumns: Int,
    gridRows: Int,
    colSpan: Int,
    rowSpan: Int
): Pair<Int, Int>? {
    if (cellSize.width <= 0 || cellSize.height <= 0) return null
    if (cellPositions.isEmpty()) return null

    // Get the top-left cell position (cell 0) as the grid origin
    val gridOrigin = cellPositions[0] ?: return null

    // Convert center position to continuous grid coordinates
    // gridX = 0.0 means left edge of column 0, gridX = 1.0 means left edge of column 1
    val gridX = (centerPosition.x - gridOrigin.x) / cellSize.width.toFloat()
    val gridY = (centerPosition.y - gridOrigin.y) / cellSize.height.toFloat()

    // For a widget of span (colSpan, rowSpan):
    // The widget's center is at: (topLeftCol + colSpan/2, topLeftRow + rowSpan/2)
    // So: topLeftCol = gridX - colSpan/2, topLeftRow = gridY - rowSpan/2
    // Use kotlin.math.round to get nearest cell
    val topLeftCol = kotlin.math.round(gridX - colSpan / 2.0f).toInt()
    val topLeftRow = kotlin.math.round(gridY - rowSpan / 2.0f).toInt()

    // Clamp to valid range (widget must fit within grid)
    val clampedCol = topLeftCol.coerceIn(0, maxOf(0, gridColumns - colSpan))
    val clampedRow = topLeftRow.coerceIn(0, maxOf(0, gridRows - rowSpan))

    return Pair(clampedCol, clampedRow)
}

/**
 * Get the set of cell indices that a widget would occupy at a given position.
 * Used to highlight cells during widget movement (same as app movement).
 */
fun getWidgetTargetCells(
    widget: PlacedWidget,
    targetCol: Int,
    targetRow: Int,
    gridColumns: Int,
    gridRows: Int
): Set<Int> {
    val cells = mutableSetOf<Int>()

    // Check bounds first
    if (targetCol < 0 || targetRow < 0) return cells
    if (targetCol + widget.columnSpan > gridColumns) return cells
    if (targetRow + widget.rowSpan > gridRows) return cells

    // Add all cells the widget would occupy
    for (row in targetRow until targetRow + widget.rowSpan) {
        for (col in targetCol until targetCol + widget.columnSpan) {
            cells.add(row * gridColumns + col)
        }
    }
    return cells
}

/**
 * Widget drag overlay - Shows the dragged widget visual.
 * Cell highlighting and "+" markers are handled by the existing DraggableGridCell mechanism.
 *
 * This overlay covers the entire screen and responds to touch anywhere,
 * so the user can immediately drag after tapping "Move".
 *
 * CRITICAL: Uses PointerEventPass.Initial to intercept touches BEFORE other
 * components (like grid cells) can handle them.
 */
@Composable
fun WidgetDragOverlay(
    dragState: WidgetDragState,
    cellPositions: Map<Int, Offset>,
    cellSize: IntSize,
    gridAreaOffset: Offset,
    gridColumns: Int,
    gridRows: Int,
    gridCells: List<HomeGridCell>,
    onDrag: (Offset) -> Unit,
    onDragEnd: (PlacedWidget, Int?, Int?) -> Unit,
    onDragCancel: () -> Unit
) {
    val widget = dragState.draggedWidget ?: return
    if (cellSize.width <= 0 || cellSize.height <= 0) return

    val density = LocalDensity.current

    // Calculate widget dimensions using AOSP-style formula
    // widgetWidth = spanX * cellWidth (no gaps in our grid)
    val widgetWidthPx = widget.spanColumns * cellSize.width
    val widgetHeightPx = widget.spanRows * cellSize.height

    // Current drag position (startPosition is the CENTER of the widget)
    val currentDragPos = dragState.startPosition + dragState.dragOffset

    // Calculate where the widget's TOP-LEFT corner should be
    // so the widget is CENTERED on the drag position
    val widgetLeft = currentDragPos.x - gridAreaOffset.x - (widgetWidthPx / 2f)
    val widgetTop = currentDragPos.y - gridAreaOffset.y - (widgetHeightPx / 2f)

    // Full-screen touch area to capture drag gestures anywhere
    // Uses PointerEventPass.Initial to intercept touches BEFORE other components
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(widget.appWidgetId) {
                // Use awaitPointerEventScope with Initial pass to intercept touches first
                awaitPointerEventScope {
                    var totalDragOffset = Offset.Zero
                    var isDragging = false

                    while (true) {
                        // Wait for pointer events at Initial pass (intercepts before other handlers)
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes

                        when {
                            // Handle touch down - start tracking
                            changes.any { it.pressed && !isDragging } -> {
                                isDragging = true
                                // Consume to prevent other handlers from seeing it
                                changes.forEach { it.consume() }
                            }

                            // Handle drag movement
                            isDragging && changes.any { it.pressed } -> {
                                changes.forEach { change ->
                                    if (change.pressed) {
                                        val dragAmount = change.positionChange()
                                        if (dragAmount != Offset.Zero) {
                                            totalDragOffset += dragAmount
                                            onDrag(dragAmount)
                                        }
                                        change.consume()
                                    }
                                }
                            }

                            // Handle touch up - finalize drag
                            isDragging && changes.none { it.pressed } -> {
                                // Calculate final drop target using accumulated offset
                                val finalDragPos = dragState.startPosition + dragState.dragOffset
                                val finalTarget = calculateWidgetDropTarget(
                                    finalDragPos, cellPositions, cellSize, gridColumns
                                )
                                val finalCol = finalTarget?.first
                                val finalRow = finalTarget?.second
                                val finalValid = if (finalTarget != null) {
                                    canPlaceWidgetAt(widget, finalCol!!, finalRow!!, gridColumns, gridRows, gridCells)
                                } else false

                                if (finalValid && finalCol != null && finalRow != null) {
                                    onDragEnd(widget, finalCol, finalRow)
                                } else {
                                    onDragCancel()
                                }

                                // Consume and reset
                                changes.forEach { it.consume() }
                                isDragging = false
                                totalDragOffset = Offset.Zero
                                break // Exit the loop after drag ends
                            }
                        }
                    }
                }
            }
    ) {
        // Dragged widget visual (semi-transparent, centered on drag point)
        Box(
            modifier = Modifier
                .offset { IntOffset(widgetLeft.toInt(), widgetTop.toInt()) }
                .size(
                    width = with(density) { widgetWidthPx.toDp() },
                    height = with(density) { widgetHeightPx.toDp() }
                )
                .graphicsLayer {
                    alpha = 0.8f
                    scaleX = 1.05f
                    scaleY = 1.05f
                    shadowElevation = 16f
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Widget placeholder visual during drag
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenWith,
                    contentDescription = "Moving widget",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Drag to move",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Handles completing a widget move operation.
 * Updates the widget position in storage and returns updated widget list.
 */
fun handleWidgetMove(
    context: Context,
    widget: PlacedWidget,
    targetCol: Int,
    targetRow: Int,
    targetPage: Int = widget.page
): List<PlacedWidget> {
    WidgetManager.updateWidgetPosition(context, widget.appWidgetId, targetCol, targetRow, targetPage)
    return WidgetManager.loadPlacedWidgets(context)
}