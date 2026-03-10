package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ========== Edge Scroll Indicators ==========
// Rounded rectangles on left/right screen edges during drag operations.
// Blue = can scroll to that direction, Red = no page in that direction.
// Appears when dragging an app/widget/folder into the edge zone.

// Edge scroll delay (ms) before page actually scrolls
const val EDGE_SCROLL_DELAY_MS = 700L
// Cooldown (ms) after page scroll before indicator reappears
const val EDGE_SCROLL_COOLDOWN_MS = 50L

@Composable
fun EdgeScrollIndicators(
    hoveringLeft: Boolean,
    hoveringRight: Boolean,
    showLeft: Boolean,
    showRight: Boolean,
    gridHPaddingPx: Float,
    screenWidthPx: Float,
    modifier: Modifier = Modifier
) {
    // Show when hovering in edge zone — blue if can scroll, red if no page in that direction
    val leftAlpha by animateFloatAsState(
        targetValue = if (hoveringLeft) 0.35f else 0f,
        animationSpec = if (hoveringLeft) tween(200, easing = FastOutSlowInEasing) else tween(150, easing = FastOutSlowInEasing),
        label = "edgeScrollLeftAlpha"
    )
    val rightAlpha by animateFloatAsState(
        targetValue = if (hoveringRight) 0.35f else 0f,
        animationSpec = if (hoveringRight) tween(200, easing = FastOutSlowInEasing) else tween(150, easing = FastOutSlowInEasing),
        label = "edgeScrollRightAlpha"
    )

    if (leftAlpha > 0f || rightAlpha > 0f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            if (leftAlpha > 0f) {
                drawEdgeRect(
                    isLeft = true,
                    isValid = showLeft,
                    gridHPaddingPx = gridHPaddingPx,
                    screenWidthPx = screenWidthPx,
                    alpha = leftAlpha
                )
            }
            if (rightAlpha > 0f) {
                drawEdgeRect(
                    isLeft = false,
                    isValid = showRight,
                    gridHPaddingPx = gridHPaddingPx,
                    screenWidthPx = screenWidthPx,
                    alpha = rightAlpha
                )
            }
        }
    }
}

// Draws a rounded rectangle edge indicator spanning the full screen height.
// Blue when valid (can scroll), red when invalid (no page in that direction).
// Uses the same colors as the grid cell hover indicators.
private fun DrawScope.drawEdgeRect(
    isLeft: Boolean,
    isValid: Boolean,
    gridHPaddingPx: Float,
    screenWidthPx: Float,
    alpha: Float
) {
    val width = gridHPaddingPx * 0.5f
    val left = if (isLeft) 0f else screenWidthPx - width
    val cornerR = width * 0.4f
    val color = if (isValid) HoverIndicatorColor else HoverIndicatorColorInvalid
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
        size = androidx.compose.ui.geometry.Size(width, size.height),
        cornerRadius = CornerRadius(cornerR, cornerR)
    )
}

// ========== Edge Scroll Detection Helper ==========
// Handles hover state updates and scroll job launching for edge-scroll zones.
// Called from all drag handlers (app, widget, folder, root continuation).
//
// Returns the updated edge scroll Job (may be the same, a new one, or null).
@OptIn(ExperimentalFoundationApi::class)
fun handleEdgeScrollDetection(
    dragCenterX: Float,
    edgeScrollZonePx: Float,
    screenWidthPx: Float,
    currentPage: Int,
    totalPages: Int,
    isScrollInProgress: Boolean,
    currentJob: Job?,
    scope: CoroutineScope,
    pagerState: PagerState,
    setHoveringLeft: (Boolean) -> Unit,
    setHoveringRight: (Boolean) -> Unit,
    setSuppressed: (Boolean) -> Unit
): Job? {
    val inLeftZone = dragCenterX < edgeScrollZonePx
    val inRightZone = dragCenterX > screenWidthPx - edgeScrollZonePx
    setHoveringLeft(inLeftZone)
    setHoveringRight(inRightZone)

    return if (inLeftZone && currentPage > 0) {
        if (currentJob == null || currentJob.isActive != true) {
            scope.launch {
                delay(EDGE_SCROLL_DELAY_MS)
                setSuppressed(true)
                pagerState.animateScrollToPage(currentPage - 1)
                delay(EDGE_SCROLL_COOLDOWN_MS)
                setSuppressed(false)
            }
        } else currentJob
    } else if (inRightZone && currentPage < totalPages - 1) {
        if (currentJob == null || currentJob.isActive != true) {
            scope.launch {
                delay(EDGE_SCROLL_DELAY_MS)
                setSuppressed(true)
                pagerState.animateScrollToPage(currentPage + 1)
                delay(EDGE_SCROLL_COOLDOWN_MS)
                setSuppressed(false)
            }
        } else currentJob
    } else if (!inLeftZone && !inRightZone) {
        if (!isScrollInProgress) {
            currentJob?.cancel()
            null
        } else currentJob
    } else currentJob
}
