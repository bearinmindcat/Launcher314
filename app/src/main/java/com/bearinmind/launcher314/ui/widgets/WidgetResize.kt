package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * WidgetResize.kt - Widget resize with smooth movement and threshold snapping
 *
 * Features:
 * - Smooth outline movement following finger
 * - Snaps to grid only when close to boundary (threshold-based)
 * - Uses existing grey grid indicators (hoveredWidgetCells)
 * - Real-time widget resizing
 */

// Constants
private val EDGE_DRAG_WIDTH = 32.dp  // Width of draggable edge area
private val CORNER_DRAG_SIZE = 48.dp  // Size of corner drag area (larger for easier touch)
private val STROKE_WIDTH = 2.dp  // Same stroke width for both dotted and solid
private val CORNER_RADIUS = 16.dp  // Radius for rounded corners
private const val SNAP_THRESHOLD = 0.4f  // Snap when within 40% of cell boundary
private val OUTLINE_COLOR = Color(0xFF666666)  // Grey for entire outline (corners + edges)
private val HIGHLIGHT_COLOR = Color.White  // White highlight when actively dragging

/**
 * Data class to hold widget resize state
 */
data class WidgetResizeState(
    val isResizing: Boolean = false,
    val resizingWidget: PlacedWidget? = null
)

/**
 * Data class for current resize dimensions (used by LauncherScreen to override widget size)
 */
data class ResizeDimensions(
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int
)

/**
 * Check if a widget supports resizing.
 * Force-resizes all widgets regardless of declared resizeMode, matching stock launcher behavior.
 * Many OEM widgets (e.g. ZUI weather) declare RESIZE_NONE but are resizable in stock launchers.
 */
fun canWidgetResize(context: android.content.Context, widget: PlacedWidget): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
    return providerInfo != null
}

/**
 * Get resize capabilities.
 * Force-allows both axes for widgets that declare RESIZE_NONE, matching stock launcher behavior.
 */
private fun getResizeCapabilities(context: android.content.Context, widget: PlacedWidget): Pair<Boolean, Boolean> {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
        ?: return Pair(true, true)

    // If widget declares RESIZE_NONE, force-allow both axes (stock launcher behavior)
    if (providerInfo.resizeMode == AppWidgetProviderInfo.RESIZE_NONE) {
        return Pair(true, true)
    }

    val canHorizontal = (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
    val canVertical = (providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0

    return Pair(canHorizontal, canVertical)
}

/**
 * Get minimum resize cells for a widget
 */
private fun getMinResizeCells(context: android.content.Context, widget: PlacedWidget): Pair<Int, Int> {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
        ?: return Pair(1, 1)
    return WidgetManager.getMinResizeCells(context, providerInfo)
}

/**
 * Widget resize overlay with smooth movement and threshold snapping
 */
@Composable
fun WidgetResizeOverlay(
    widget: PlacedWidget,
    cellWidth: Int,
    cellHeight: Int,
    gridColumns: Int,
    gridRows: Int,
    occupiedCells: Set<Pair<Int, Int>>,
    onResizeCellsChanged: (Set<Int>) -> Unit,
    onResizeDimensionsChanged: (ResizeDimensions) -> Unit,  // Called when dimensions change for widget resize
    onResizeValidityChanged: (Boolean) -> Unit,  // Called when resize validity changes (for indicator colors)
    onResizeComplete: (newColumn: Int, newRow: Int, newColumnSpan: Int, newRowSpan: Int) -> Unit,
    onResizeCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val strokeWidthPx = with(density) { STROKE_WIDTH.toPx() }
    val cornerRadiusPx = with(density) { CORNER_RADIUS.toPx() }

    val (canResizeHorizontal, canResizeVertical) = remember(widget) {
        getResizeCapabilities(context, widget)
    }
    val (minWidthCells, minHeightCells) = remember(widget) {
        getMinResizeCells(context, widget)
    }

    // Pixel-based frame position (for smooth movement)
    val frameLeft = remember { Animatable((widget.startColumn * cellWidth).toFloat()) }
    val frameTop = remember { Animatable((widget.startRow * cellHeight).toFloat()) }
    val frameRight = remember { Animatable(((widget.startColumn + widget.columnSpan) * cellWidth).toFloat()) }
    val frameBottom = remember { Animatable(((widget.startRow + widget.rowSpan) * cellHeight).toFloat()) }

    // Cell-based position (snapped) - tracks the TARGET position (where user wants to resize to)
    var cellsLeft by remember { mutableIntStateOf(widget.startColumn) }
    var cellsTop by remember { mutableIntStateOf(widget.startRow) }
    var cellsRight by remember { mutableIntStateOf(widget.startColumn + widget.columnSpan - 1) }
    var cellsBottom by remember { mutableIntStateOf(widget.startRow + widget.rowSpan - 1) }

    // Track if current resize position is valid
    var isCurrentResizeValid by remember { mutableStateOf(true) }

    // Track which handle is actively being dragged for highlight effect
    // Values: null, "left", "right", "top", "bottom", "topLeft", "topRight", "bottomLeft", "bottomRight"
    var activeHandle by remember { mutableStateOf<String?>(null) }

    // Original values (for reference)
    val originalLeft = widget.startColumn
    val originalTop = widget.startRow
    val originalRight = widget.startColumn + widget.columnSpan - 1
    val originalBottom = widget.startRow + widget.rowSpan - 1

    // Computed spans
    val currentColumnSpan = cellsRight - cellsLeft + 1
    val currentRowSpan = cellsBottom - cellsTop + 1

    // Helper to check if value is close to cell boundary (within threshold)
    fun shouldSnap(pixelPos: Float, cellSize: Int): Boolean {
        val cellPos = pixelPos / cellSize
        val fraction = cellPos - cellPos.toInt()
        return fraction < SNAP_THRESHOLD || fraction > (1 - SNAP_THRESHOLD)
    }

    // Helper to snap to nearest cell
    fun snapToCell(pixelPos: Float, cellSize: Int): Int {
        return (pixelPos / cellSize + 0.5f).toInt()
    }

    // Helper to check if a proposed resize is valid
    fun isResizeValid(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        if (left < 0 || top < 0 || right >= gridColumns || bottom >= gridRows) return false
        if (right < left || bottom < top) return false
        if (right - left + 1 < minWidthCells) return false
        if (bottom - top + 1 < minHeightCells) return false

        for (col in left..right) {
            for (row in top..bottom) {
                if (occupiedCells.contains(Pair(col, row))) {
                    return false
                }
            }
        }
        return true
    }

    // Update cells to show target position and check validity
    fun updateCellsAndResize(newLeft: Int, newTop: Int, newRight: Int, newBottom: Int) {
        if (newLeft != cellsLeft || newTop != cellsTop || newRight != cellsRight || newBottom != cellsBottom) {
            cellsLeft = newLeft
            cellsTop = newTop
            cellsRight = newRight
            cellsBottom = newBottom

            val newColumnSpan = newRight - newLeft + 1
            val newRowSpan = newBottom - newTop + 1

            // Check validity and update state
            val valid = isResizeValid(newLeft, newTop, newRight, newBottom)
            isCurrentResizeValid = valid
            onResizeValidityChanged(valid)

            // Update hover cells (always show target position for visual feedback)
            val cells = mutableSetOf<Int>()
            for (col in newLeft..newRight) {
                for (row in newTop..newBottom) {
                    cells.add(row * gridColumns + col)
                }
            }
            onResizeCellsChanged(cells)

            // Only update widget dimensions if resize is valid
            if (valid) {
                onResizeDimensionsChanged(ResizeDimensions(newLeft, newTop, newColumnSpan, newRowSpan))
            }
        }
    }

    // Initialize hover cells on first composition
    LaunchedEffect(Unit) {
        val cells = mutableSetOf<Int>()
        for (col in cellsLeft..cellsRight) {
            for (row in cellsTop..cellsBottom) {
                cells.add(row * gridColumns + col)
            }
        }
        onResizeCellsChanged(cells)
        onResizeDimensionsChanged(ResizeDimensions(cellsLeft, cellsTop, currentColumnSpan, currentRowSpan))
        onResizeValidityChanged(true)  // Initial position is always valid
    }

    // Snap frame to cells with animation
    fun snapFrameToCells() {
        scope.launch {
            launch { frameLeft.animateTo((cellsLeft * cellWidth).toFloat(), spring(stiffness = Spring.StiffnessMedium)) }
            launch { frameTop.animateTo((cellsTop * cellHeight).toFloat(), spring(stiffness = Spring.StiffnessMedium)) }
            launch { frameRight.animateTo(((cellsRight + 1) * cellWidth).toFloat(), spring(stiffness = Spring.StiffnessMedium)) }
            launch { frameBottom.animateTo(((cellsBottom + 1) * cellHeight).toFloat(), spring(stiffness = Spring.StiffnessMedium)) }
        }
    }

    val previewWidth = frameRight.value - frameLeft.value
    val previewHeight = frameBottom.value - frameTop.value

    // Full screen tap catcher - any tap exits resize mode
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(997f)
            .pointerInput(isCurrentResizeValid, cellsLeft, cellsTop, cellsRight, cellsBottom) {
                detectTapGestures {
                    // Exit resize mode on any tap
                    onResizeCellsChanged(emptySet())
                    if (isCurrentResizeValid) {
                        val finalColumnSpan = cellsRight - cellsLeft + 1
                        val finalRowSpan = cellsBottom - cellsTop + 1
                        onResizeComplete(cellsLeft, cellsTop, finalColumnSpan, finalRowSpan)
                    } else {
                        onResizeCancel()
                    }
                }
            }
    )

    // Preview outline with draggable handles
    Box(
        modifier = Modifier
            .offset { IntOffset(frameLeft.value.roundToInt(), frameTop.value.roundToInt()) }
            .size(
                width = with(density) { previewWidth.toDp() },
                height = with(density) { previewHeight.toDp() }
            )
            .zIndex(999f)
    ) {
        // Draw the outline: solid white edges when active, dotted corners that fade
        val arcDiameter = cornerRadiusPx * 2

        // Which edges are active (solid white)
        val topEdgeActive = activeHandle in listOf("top", "topLeft", "topRight")
        val rightEdgeActive = activeHandle in listOf("right", "topRight", "bottomRight")
        val bottomEdgeActive = activeHandle in listOf("bottom", "bottomLeft", "bottomRight")
        val leftEdgeActive = activeHandle in listOf("left", "topLeft", "bottomLeft")

        // Corner: which adjacent edges feed into it
        // Each corner connects two edges. The "start" of the arc is one edge, the "end" is the other.
        // The arc fades from the active edge's color to grey at the far end.
        // topLeft corner: left edge meets top edge (arc goes 180° to 270°, i.e. left-side to top-side)
        val topLeftStartColor = if (leftEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR   // 180° end (left)
        val topLeftEndColor = if (topEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR       // 270° end (top)
        // topRight corner: top edge meets right edge (arc goes 270° to 360°)
        val topRightStartColor = if (topEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR    // 270° end (top)
        val topRightEndColor = if (rightEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR    // 360° end (right)
        // bottomRight corner: right edge meets bottom edge (arc goes 0° to 90°)
        val bottomRightStartColor = if (rightEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR  // 0° end (right)
        val bottomRightEndColor = if (bottomEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR   // 90° end (bottom)
        // bottomLeft corner: bottom edge meets left edge (arc goes 90° to 180°)
        val bottomLeftStartColor = if (bottomEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR  // 90° end (bottom)
        val bottomLeftEndColor = if (leftEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR      // 180° end (left)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val offset = strokeWidthPx / 2
            val dashPattern = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Helper: draw a corner arc as segments that fade between two colors
            fun drawFadingArc(
                startAngle: Float,
                startColor: Color,
                endColor: Color,
                arcTopLeft: Offset,
                arcSize: androidx.compose.ui.geometry.Size
            ) {
                val segments = 12
                val sweepPerSegment = 90f / segments
                for (i in 0 until segments) {
                    val t = i.toFloat() / (segments - 1)
                    val color = androidx.compose.ui.graphics.lerp(startColor, endColor, t)
                    drawArc(
                        color = color,
                        startAngle = startAngle + i * sweepPerSegment,
                        sweepAngle = sweepPerSegment + 0.5f, // tiny overlap to avoid gaps
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, pathEffect = dashPattern)
                    )
                }
            }

            val arcSz = androidx.compose.ui.geometry.Size(arcDiameter, arcDiameter)

            // TOP-LEFT CORNER (180° to 270°): fades from left edge color to top edge color
            drawFadingArc(180f, topLeftStartColor, topLeftEndColor, Offset(offset, offset), arcSz)

            // TOP-RIGHT CORNER (270° to 360°): fades from top edge color to right edge color
            drawFadingArc(270f, topRightStartColor, topRightEndColor,
                Offset(size.width - arcDiameter - offset, offset), arcSz)

            // BOTTOM-RIGHT CORNER (0° to 90°): fades from right edge color to bottom edge color
            drawFadingArc(0f, bottomRightStartColor, bottomRightEndColor,
                Offset(size.width - arcDiameter - offset, size.height - arcDiameter - offset), arcSz)

            // BOTTOM-LEFT CORNER (90° to 180°): fades from bottom edge color to left edge color
            drawFadingArc(90f, bottomLeftStartColor, bottomLeftEndColor,
                Offset(offset, size.height - arcDiameter - offset), arcSz)

            // EDGES: 100% solid color (white when active, grey when not)
            val topColor = if (topEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR
            val rightColor = if (rightEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR
            val bottomColor = if (bottomEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR
            val leftColor = if (leftEdgeActive) HIGHLIGHT_COLOR else OUTLINE_COLOR

            // TOP EDGE
            drawLine(color = topColor,
                start = Offset(cornerRadiusPx + offset, offset),
                end = Offset(size.width - cornerRadiusPx - offset, offset),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)

            // RIGHT EDGE
            drawLine(color = rightColor,
                start = Offset(size.width - offset, cornerRadiusPx + offset),
                end = Offset(size.width - offset, size.height - cornerRadiusPx - offset),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)

            // BOTTOM EDGE
            drawLine(color = bottomColor,
                start = Offset(size.width - cornerRadiusPx - offset, size.height - offset),
                end = Offset(cornerRadiusPx + offset, size.height - offset),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)

            // LEFT EDGE
            drawLine(color = leftColor,
                start = Offset(offset, size.height - cornerRadiusPx - offset),
                end = Offset(offset, cornerRadiusPx + offset),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)
        }

        // Draggable edge areas (invisible, but capture drag gestures on the lines)
        val edgeDragWidthPx = with(density) { EDGE_DRAG_WIDTH.toPx() }

        // Left edge drag area
        if (canResizeHorizontal) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-EDGE_DRAG_WIDTH / 2))
                    .size(width = EDGE_DRAG_WIDTH, height = with(density) { previewHeight.toDp() })
                    .pointerInput(cellWidth) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "left" },
                            onDrag = { change, dragAmount ->
                                val newLeft = (frameLeft.value + dragAmount.x).coerceAtLeast(0f)
                                    .coerceAtMost(frameRight.value - minWidthCells * cellWidth)
                                scope.launch { frameLeft.snapTo(newLeft) }

                                if (shouldSnap(frameLeft.value, cellWidth)) {
                                    val snappedCell = snapToCell(frameLeft.value, cellWidth)
                                        .coerceIn(0, cellsRight - minWidthCells + 1)
                                    updateCellsAndResize(snappedCell, cellsTop, cellsRight, cellsBottom)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Right edge drag area
        if (canResizeHorizontal) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = EDGE_DRAG_WIDTH / 2)
                    .size(width = EDGE_DRAG_WIDTH, height = with(density) { previewHeight.toDp() })
                    .pointerInput(cellWidth) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "right" },
                            onDrag = { change, dragAmount ->
                                val newRight = (frameRight.value + dragAmount.x)
                                    .coerceAtLeast(frameLeft.value + minWidthCells * cellWidth)
                                    .coerceAtMost((gridColumns * cellWidth).toFloat())
                                scope.launch { frameRight.snapTo(newRight) }

                                if (shouldSnap(frameRight.value, cellWidth)) {
                                    val snappedCell = (snapToCell(frameRight.value, cellWidth) - 1)
                                        .coerceIn(cellsLeft + minWidthCells - 1, gridColumns - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, snappedCell, cellsBottom)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Top edge drag area
        if (canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-EDGE_DRAG_WIDTH / 2))
                    .size(width = with(density) { previewWidth.toDp() }, height = EDGE_DRAG_WIDTH)
                    .pointerInput(cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "top" },
                            onDrag = { change, dragAmount ->
                                val newTop = (frameTop.value + dragAmount.y).coerceAtLeast(0f)
                                    .coerceAtMost(frameBottom.value - minHeightCells * cellHeight)
                                scope.launch { frameTop.snapTo(newTop) }

                                if (shouldSnap(frameTop.value, cellHeight)) {
                                    val snappedCell = snapToCell(frameTop.value, cellHeight)
                                        .coerceIn(0, cellsBottom - minHeightCells + 1)
                                    updateCellsAndResize(cellsLeft, snappedCell, cellsRight, cellsBottom)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Bottom edge drag area
        if (canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = EDGE_DRAG_WIDTH / 2)
                    .size(width = with(density) { previewWidth.toDp() }, height = EDGE_DRAG_WIDTH)
                    .pointerInput(cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "bottom" },
                            onDrag = { change, dragAmount ->
                                val newBottom = (frameBottom.value + dragAmount.y)
                                    .coerceAtLeast(frameTop.value + minHeightCells * cellHeight)
                                    .coerceAtMost((gridRows * cellHeight).toFloat())
                                scope.launch { frameBottom.snapTo(newBottom) }

                                if (shouldSnap(frameBottom.value, cellHeight)) {
                                    val snappedCell = (snapToCell(frameBottom.value, cellHeight) - 1)
                                        .coerceIn(cellsTop + minHeightCells - 1, gridRows - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, cellsRight, snappedCell)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Corner drag areas for RESIZING (diagonal resize from corners)
        // Top-left corner (resize left + top edges)
        if (canResizeHorizontal || canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -CORNER_DRAG_SIZE / 2, y = -CORNER_DRAG_SIZE / 2)
                    .size(CORNER_DRAG_SIZE)
                    .pointerInput(cellWidth, cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "topLeft" },
                            onDrag = { change, dragAmount ->
                                if (canResizeHorizontal) {
                                    val newLeft = (frameLeft.value + dragAmount.x).coerceAtLeast(0f)
                                        .coerceAtMost(frameRight.value - minWidthCells * cellWidth)
                                    scope.launch { frameLeft.snapTo(newLeft) }
                                }
                                if (canResizeVertical) {
                                    val newTop = (frameTop.value + dragAmount.y).coerceAtLeast(0f)
                                        .coerceAtMost(frameBottom.value - minHeightCells * cellHeight)
                                    scope.launch { frameTop.snapTo(newTop) }
                                }
                                if (canResizeHorizontal && shouldSnap(frameLeft.value, cellWidth)) {
                                    val snappedCell = snapToCell(frameLeft.value, cellWidth)
                                        .coerceIn(0, cellsRight - minWidthCells + 1)
                                    updateCellsAndResize(snappedCell, cellsTop, cellsRight, cellsBottom)
                                }
                                if (canResizeVertical && shouldSnap(frameTop.value, cellHeight)) {
                                    val snappedCell = snapToCell(frameTop.value, cellHeight)
                                        .coerceIn(0, cellsBottom - minHeightCells + 1)
                                    updateCellsAndResize(cellsLeft, snappedCell, cellsRight, cellsBottom)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Top-right corner (resize right + top edges)
        if (canResizeHorizontal || canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = CORNER_DRAG_SIZE / 2, y = -CORNER_DRAG_SIZE / 2)
                    .size(CORNER_DRAG_SIZE)
                    .pointerInput(cellWidth, cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "topRight" },
                            onDrag = { change, dragAmount ->
                                if (canResizeHorizontal) {
                                    val newRight = (frameRight.value + dragAmount.x)
                                        .coerceAtLeast(frameLeft.value + minWidthCells * cellWidth)
                                        .coerceAtMost((gridColumns * cellWidth).toFloat())
                                    scope.launch { frameRight.snapTo(newRight) }
                                }
                                if (canResizeVertical) {
                                    val newTop = (frameTop.value + dragAmount.y).coerceAtLeast(0f)
                                        .coerceAtMost(frameBottom.value - minHeightCells * cellHeight)
                                    scope.launch { frameTop.snapTo(newTop) }
                                }
                                if (canResizeHorizontal && shouldSnap(frameRight.value, cellWidth)) {
                                    val snappedCell = (snapToCell(frameRight.value, cellWidth) - 1)
                                        .coerceIn(cellsLeft + minWidthCells - 1, gridColumns - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, snappedCell, cellsBottom)
                                }
                                if (canResizeVertical && shouldSnap(frameTop.value, cellHeight)) {
                                    val snappedCell = snapToCell(frameTop.value, cellHeight)
                                        .coerceIn(0, cellsBottom - minHeightCells + 1)
                                    updateCellsAndResize(cellsLeft, snappedCell, cellsRight, cellsBottom)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Bottom-left corner (resize left + bottom edges)
        if (canResizeHorizontal || canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -CORNER_DRAG_SIZE / 2, y = CORNER_DRAG_SIZE / 2)
                    .size(CORNER_DRAG_SIZE)
                    .pointerInput(cellWidth, cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "bottomLeft" },
                            onDrag = { change, dragAmount ->
                                if (canResizeHorizontal) {
                                    val newLeft = (frameLeft.value + dragAmount.x).coerceAtLeast(0f)
                                        .coerceAtMost(frameRight.value - minWidthCells * cellWidth)
                                    scope.launch { frameLeft.snapTo(newLeft) }
                                }
                                if (canResizeVertical) {
                                    val newBottom = (frameBottom.value + dragAmount.y)
                                        .coerceAtLeast(frameTop.value + minHeightCells * cellHeight)
                                        .coerceAtMost((gridRows * cellHeight).toFloat())
                                    scope.launch { frameBottom.snapTo(newBottom) }
                                }
                                if (canResizeHorizontal && shouldSnap(frameLeft.value, cellWidth)) {
                                    val snappedCell = snapToCell(frameLeft.value, cellWidth)
                                        .coerceIn(0, cellsRight - minWidthCells + 1)
                                    updateCellsAndResize(snappedCell, cellsTop, cellsRight, cellsBottom)
                                }
                                if (canResizeVertical && shouldSnap(frameBottom.value, cellHeight)) {
                                    val snappedCell = (snapToCell(frameBottom.value, cellHeight) - 1)
                                        .coerceIn(cellsTop + minHeightCells - 1, gridRows - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, cellsRight, snappedCell)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }

        // Bottom-right corner (resize right + bottom edges)
        if (canResizeHorizontal || canResizeVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = CORNER_DRAG_SIZE / 2, y = CORNER_DRAG_SIZE / 2)
                    .size(CORNER_DRAG_SIZE)
                    .pointerInput(cellWidth, cellHeight) {
                        detectDragGestures(
                            onDragStart = { activeHandle = "bottomRight" },
                            onDrag = { change, dragAmount ->
                                if (canResizeHorizontal) {
                                    val newRight = (frameRight.value + dragAmount.x)
                                        .coerceAtLeast(frameLeft.value + minWidthCells * cellWidth)
                                        .coerceAtMost((gridColumns * cellWidth).toFloat())
                                    scope.launch { frameRight.snapTo(newRight) }
                                }
                                if (canResizeVertical) {
                                    val newBottom = (frameBottom.value + dragAmount.y)
                                        .coerceAtLeast(frameTop.value + minHeightCells * cellHeight)
                                        .coerceAtMost((gridRows * cellHeight).toFloat())
                                    scope.launch { frameBottom.snapTo(newBottom) }
                                }
                                if (canResizeHorizontal && shouldSnap(frameRight.value, cellWidth)) {
                                    val snappedCell = (snapToCell(frameRight.value, cellWidth) - 1)
                                        .coerceIn(cellsLeft + minWidthCells - 1, gridColumns - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, snappedCell, cellsBottom)
                                }
                                if (canResizeVertical && shouldSnap(frameBottom.value, cellHeight)) {
                                    val snappedCell = (snapToCell(frameBottom.value, cellHeight) - 1)
                                        .coerceIn(cellsTop + minHeightCells - 1, gridRows - 1)
                                    updateCellsAndResize(cellsLeft, cellsTop, cellsRight, snappedCell)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                activeHandle = null
                                snapFrameToCells()
                            },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            )
        }
    }
}