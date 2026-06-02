package com.bearinmind.launcher314.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Widget-style resize overlay for the folder popup.
 *
 * Matches [com.bearinmind.launcher314.ui.widgets.WidgetResizeOverlay]'s
 * language: dotted grey outline, fading dashed corner arcs, 4 edge + 4
 * corner drag handles, white highlight on the active edge / corner.
 *
 * MUST be rendered as a SIBLING of the popup Box (not a child) AND must
 * span the full screen — the popup's `.clip()` would block hit-testing
 * for any handle sticking out of the rounded rect, and the sibling dim
 * backdrop (fillMaxSize + clickable) would steal touches outside the
 * popup's bounds. By making the overlay's outer Box fillMaxSize too, the
 * handles live INSIDE the overlay's bounds and the overlay sits on TOP
 * of the backdrop in the z-order, so touches reach the handles first.
 *
 * Free-pixel (not grid-snapped). Every handle responds to "drag outward =
 * grow, drag inward = shrink".
 */
@Composable
fun FolderResizeOverlay(
    folderId: String,
    popupOffsetXpx: Float,
    popupOffsetYpx: Float,
    popupWidthPx: Float,
    popupHeightPx: Float,
    minWidthPx: Float,
    maxWidthPx: Float,
    minHeightPx: Float,
    maxHeightPx: Float,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    progress: Float = 1f,
    interactive: Boolean = true,
    // Folder-icon rect in ROOT px — the outline collapses INTO this rect as
    // progress → 0, matching the folder popup's clip-reveal-into-icon close.
    iconLeftPx: Float = popupOffsetXpx,
    iconTopPx: Float = popupOffsetYpx,
    iconRightPx: Float = popupOffsetXpx + popupWidthPx,
    iconBottomPx: Float = popupOffsetYpx + popupHeightPx
) {
    // Enter / exit animation. The outline + handles are drawn at a rect that
    // LERPS from the folder-icon rect (progress 0) to the full popup rect
    // (progress 1) — so it grows out of / collapses into the folder icon
    // exactly like the popup's clip-reveal, for a homogenous open/close.
    // Alpha follows progress so it fades in / out in sync.
    val p = progress.coerceIn(0f, 1f)
    val effOffsetXpx = iconLeftPx + (popupOffsetXpx - iconLeftPx) * p
    val effOffsetYpx = iconTopPx + (popupOffsetYpx - iconTopPx) * p
    val effRightPx = iconRightPx + ((popupOffsetXpx + popupWidthPx) - iconRightPx) * p
    val effBottomPx = iconBottomPx + ((popupOffsetYpx + popupHeightPx) - iconBottomPx) * p
    val effWidthPx = (effRightPx - effOffsetXpx).coerceAtLeast(1f)
    val effHeightPx = (effBottomPx - effOffsetYpx).coerceAtLeast(1f)
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }
    val edgeDragWidth = 32.dp
    val edgeDragWidthPx = with(density) { edgeDragWidth.toPx() }
    val cornerDragSize = 48.dp
    val cornerDragSizePx = with(density) { cornerDragSize.toPx() }
    val outlineColor = Color(0xFF666666)
    val highlightColor = Color.White

    var activeHandle by remember { mutableStateOf<String?>(null) }

    // CRITICAL: pointerInput(folderId) captures its gesture lambda ONCE
    // (folderId is stable for the open folder), so any value read directly
    // from a composable parameter inside detectDragGestures stays frozen at
    // the FIRST-composition value. Wrap the live values in
    // rememberUpdatedState and read .value inside the gesture handler so
    // each drag delta is applied to the LATEST width/height.
    val currentWidth = rememberUpdatedState(popupWidthPx)
    val currentHeight = rememberUpdatedState(popupHeightPx)
    val widthChangeCb = rememberUpdatedState(onWidthChange)
    val heightChangeCb = rememberUpdatedState(onHeightChange)
    val minW = rememberUpdatedState(minWidthPx)
    val maxW = rememberUpdatedState(maxWidthPx)
    val minH = rememberUpdatedState(minHeightPx)
    val maxH = rememberUpdatedState(maxHeightPx)

    fun growW(deltaPx: Float) {
        widthChangeCb.value(
            (currentWidth.value + deltaPx).coerceIn(minW.value, maxW.value)
        )
    }
    fun growH(deltaPx: Float) {
        heightChangeCb.value(
            (currentHeight.value + deltaPx).coerceIn(minH.value, maxH.value)
        )
    }

    // Handle visual heights/widths use the EFFECTIVE (animated) size so
    // edge handles shrink along with the outline during the exit anim.
    val popupWidthDp = with(density) { effWidthPx.toDp() }
    val popupHeightDp = with(density) { effHeightPx.toDp() }

    // Full-screen Box so every handle's absolute position lands INSIDE
    // its bounds. zIndex high so we sit above the dim backdrop and popup.
    // alpha = progress drives the fade in/out; canvas + handles use the
    // animated eff* values for shrink/grow about the popup center.
    Box(modifier = Modifier
        .fillMaxSize()
        .zIndex(10f)
        .graphicsLayer { alpha = progress }
    ) {
        val topEdgeActive = activeHandle in listOf("top", "topLeft", "topRight")
        val rightEdgeActive = activeHandle in listOf("right", "topRight", "bottomRight")
        val bottomEdgeActive = activeHandle in listOf("bottom", "bottomLeft", "bottomRight")
        val leftEdgeActive = activeHandle in listOf("left", "topLeft", "bottomLeft")

        val topLeftStart = if (leftEdgeActive) highlightColor else outlineColor
        val topLeftEnd = if (topEdgeActive) highlightColor else outlineColor
        val topRightStart = if (topEdgeActive) highlightColor else outlineColor
        val topRightEnd = if (rightEdgeActive) highlightColor else outlineColor
        val bottomRightStart = if (rightEdgeActive) highlightColor else outlineColor
        val bottomRightEnd = if (bottomEdgeActive) highlightColor else outlineColor
        val bottomLeftStart = if (bottomEdgeActive) highlightColor else outlineColor
        val bottomLeftEnd = if (leftEdgeActive) highlightColor else outlineColor

        // Canvas covers the entire screen; we draw the outline at the
        // animated effective rect (matches popup at progress=1, shrinks 8%
        // toward popup center at progress=0).
        Canvas(modifier = Modifier.fillMaxSize()) {
            val popupLeft = effOffsetXpx
            val popupTop = effOffsetYpx
            val popupRight = effOffsetXpx + effWidthPx
            val popupBottom = effOffsetYpx + effHeightPx
            val offsetPx = strokeWidthPx / 2
            val dashPattern = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val arcDiameter = cornerRadiusPx * 2
            val arcSize = Size(arcDiameter, arcDiameter)

            fun fadingArc(
                startAngle: Float,
                startColor: Color,
                endColor: Color,
                topLeft: Offset
            ) {
                val segments = 12
                val sweepPerSegment = 90f / segments
                for (i in 0 until segments) {
                    val t = i.toFloat() / (segments - 1)
                    val color = lerp(startColor, endColor, t)
                    drawArc(
                        color = color,
                        startAngle = startAngle + i * sweepPerSegment,
                        sweepAngle = sweepPerSegment + 0.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, pathEffect = dashPattern)
                    )
                }
            }

            fadingArc(180f, topLeftStart, topLeftEnd, Offset(popupLeft + offsetPx, popupTop + offsetPx))
            fadingArc(270f, topRightStart, topRightEnd,
                Offset(popupRight - arcDiameter - offsetPx, popupTop + offsetPx))
            fadingArc(0f, bottomRightStart, bottomRightEnd,
                Offset(popupRight - arcDiameter - offsetPx, popupBottom - arcDiameter - offsetPx))
            fadingArc(90f, bottomLeftStart, bottomLeftEnd,
                Offset(popupLeft + offsetPx, popupBottom - arcDiameter - offsetPx))

            val topColor = if (topEdgeActive) highlightColor else outlineColor
            val rightColor = if (rightEdgeActive) highlightColor else outlineColor
            val bottomColor = if (bottomEdgeActive) highlightColor else outlineColor
            val leftColor = if (leftEdgeActive) highlightColor else outlineColor

            drawLine(color = topColor,
                start = Offset(popupLeft + cornerRadiusPx + offsetPx, popupTop + offsetPx),
                end = Offset(popupRight - cornerRadiusPx - offsetPx, popupTop + offsetPx),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)
            drawLine(color = rightColor,
                start = Offset(popupRight - offsetPx, popupTop + cornerRadiusPx + offsetPx),
                end = Offset(popupRight - offsetPx, popupBottom - cornerRadiusPx - offsetPx),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)
            drawLine(color = bottomColor,
                start = Offset(popupRight - cornerRadiusPx - offsetPx, popupBottom - offsetPx),
                end = Offset(popupLeft + cornerRadiusPx + offsetPx, popupBottom - offsetPx),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)
            drawLine(color = leftColor,
                start = Offset(popupLeft + offsetPx, popupBottom - cornerRadiusPx - offsetPx),
                end = Offset(popupLeft + offsetPx, popupTop + cornerRadiusPx + offsetPx),
                strokeWidth = strokeWidthPx,
                pathEffect = dashPattern)
        }

        // ── Handles positioned absolutely via Modifier.offset ──
        // Each handle uses .offset { IntOffset(...) } with absolute pixel
        // coords (popup edge ± half handle thickness) so it lands exactly
        // straddling the popup's edge. Because the parent is fillMaxSize,
        // hit-testing reaches every handle.
        //
        // Only rendered while interactive — during the close anim the
        // outline keeps drawing (via the canvas above) and fades out, but
        // handle touches are gated off so a near-miss tap can't sneak a
        // grow on a popup that's already collapsing.
        if (interactive) {

        // Left edge.
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx - edgeDragWidthPx / 2f).roundToInt(),
                effOffsetYpx.roundToInt()
            ) }
            .size(width = edgeDragWidth, height = popupHeightDp)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "left" },
                    onDrag = { change, drag -> growW(-drag.x); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        // Right edge.
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx + effWidthPx - edgeDragWidthPx / 2f).roundToInt(),
                effOffsetYpx.roundToInt()
            ) }
            .size(width = edgeDragWidth, height = popupHeightDp)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "right" },
                    onDrag = { change, drag -> growW(drag.x); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        // Top edge.
        Box(modifier = Modifier
            .offset { IntOffset(
                effOffsetXpx.roundToInt(),
                (effOffsetYpx - edgeDragWidthPx / 2f).roundToInt()
            ) }
            .size(width = popupWidthDp, height = edgeDragWidth)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "top" },
                    onDrag = { change, drag -> growH(-drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        // Bottom edge.
        Box(modifier = Modifier
            .offset { IntOffset(
                effOffsetXpx.roundToInt(),
                (effOffsetYpx + effHeightPx - edgeDragWidthPx / 2f).roundToInt()
            ) }
            .size(width = popupWidthDp, height = edgeDragWidth)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "bottom" },
                    onDrag = { change, drag -> growH(drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )

        // ── Corner handles ──
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx - cornerDragSizePx / 2f).roundToInt(),
                (effOffsetYpx - cornerDragSizePx / 2f).roundToInt()
            ) }
            .size(cornerDragSize)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "topLeft" },
                    onDrag = { change, drag -> growW(-drag.x); growH(-drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx + effWidthPx - cornerDragSizePx / 2f).roundToInt(),
                (effOffsetYpx - cornerDragSizePx / 2f).roundToInt()
            ) }
            .size(cornerDragSize)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "topRight" },
                    onDrag = { change, drag -> growW(drag.x); growH(-drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx - cornerDragSizePx / 2f).roundToInt(),
                (effOffsetYpx + effHeightPx - cornerDragSizePx / 2f).roundToInt()
            ) }
            .size(cornerDragSize)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "bottomLeft" },
                    onDrag = { change, drag -> growW(-drag.x); growH(drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        Box(modifier = Modifier
            .offset { IntOffset(
                (effOffsetXpx + effWidthPx - cornerDragSizePx / 2f).roundToInt(),
                (effOffsetYpx + effHeightPx - cornerDragSizePx / 2f).roundToInt()
            ) }
            .size(cornerDragSize)
            .pointerInput(folderId) {
                detectDragGestures(
                    onDragStart = { activeHandle = "bottomRight" },
                    onDrag = { change, drag -> growW(drag.x); growH(drag.y); change.consume() },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
        )
        } // end if (interactive)
    }
}
