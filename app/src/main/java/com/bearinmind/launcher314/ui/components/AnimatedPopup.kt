package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

/** Positions the popup above or below the anchor and reports placement for the arrow */
private class ArrowPopupPositionProvider(
    private val iconBoundsInRoot: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    private val gapPx: Int = 6,
    private val onPlacement: (isAbove: Boolean, arrowXPx: Float) -> Unit
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Horizontal: center popup on anchor, clamped to screen edges
        val x = (anchorBounds.left + anchorBounds.right - popupContentSize.width) / 2
        val clampedX = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        // Use actual icon bounds if available, otherwise estimate
        val gap = gapPx
        val iconTop: Int
        val iconBottom: Int
        if (iconBoundsInRoot != androidx.compose.ui.geometry.Rect.Zero) {
            iconTop = iconBoundsInRoot.top.toInt()
            iconBottom = iconBoundsInRoot.bottom.toInt()
        } else {
            // Fallback: use full anchor bounds
            iconTop = anchorBounds.top
            iconBottom = anchorBounds.bottom
        }
        val belowY = iconBottom + gap
        val aboveY = iconTop - popupContentSize.height - gap

        val isAbove: Boolean
        val y: Int

        if (belowY + popupContentSize.height <= windowSize.height) {
            isAbove = false
            y = belowY
        } else if (aboveY >= 0) {
            isAbove = true
            y = aboveY
        } else {
            isAbove = windowSize.height - anchorBounds.bottom < anchorBounds.top
            y = if (isAbove) aboveY else belowY
        }

        // Arrow tip X = anchor center, relative to popup's left edge
        val anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
        val arrowX = anchorCenterX - clampedX

        onPlacement(isAbove, arrowX)
        return IntOffset(clampedX, y)
    }
}

@Composable
fun AnimatedPopup(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    popupPositionProvider: PopupPositionProvider? = null,
    iconSizePx: Int = 0,
    iconBoundsInRoot: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    gapDp: Int = 4,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var showPopup by remember { mutableStateOf(false) }
    var animateIn by remember { mutableStateOf(false) }
    var isAbove by remember { mutableStateOf(false) }
    var arrowXPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            showPopup = true
            animateIn = true
        } else if (showPopup) {
            animateIn = false
            delay(160)
            showPopup = false
        }
    }

    if (showPopup) {
        val useArrow = popupPositionProvider == null
        val gapPx = with(density) { gapDp.dp.roundToPx() }
        val provider = popupPositionProvider ?: remember(iconBoundsInRoot, gapPx) {
            ArrowPopupPositionProvider(iconBoundsInRoot = iconBoundsInRoot, gapPx = gapPx) { above, x ->
                isAbove = above
                arrowXPx = x
            }
        }

        Popup(
            popupPositionProvider = provider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedPopupContent(
                visible = animateIn,
                showArrow = useArrow,
                isAbove = isAbove,
                arrowXPx = arrowXPx,
                content = content
            )
        }
    }
}

@Composable
private fun AnimatedPopupContent(
    visible: Boolean,
    showArrow: Boolean = false,
    isAbove: Boolean = false,
    arrowXPx: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val show = visible && appeared

    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0.9f,
        animationSpec = tween(150),
        label = "popupScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(150),
        label = "popupAlpha"
    )

    // Compute transform origin: scale from the arrow tip direction
    val popupWidthPx = with(LocalDensity.current) { 225.dp.toPx() }
    val originX = if (showArrow && popupWidthPx > 0f) {
        (arrowXPx / popupWidthPx).coerceIn(0.1f, 0.9f)
    } else 0.5f
    val originY = if (showArrow) { if (isAbove) 1f else 0f } else 0.5f

    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
    val triangleHeight = 8.dp
    val triangleWidth = 16.dp

    // Overlap so the triangle connects seamlessly to the Surface (covers the shadow gap)
    val overlap = 2.dp

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = TransformOrigin(originX, originY)
            }
    ) {
        // Arrow pointing UP (popup is below the anchor item)
        if (showArrow && !isAbove) {
            Canvas(
                modifier = Modifier
                    .width(225.dp)
                    .height(triangleHeight)
            ) {
                val triW = triangleWidth.toPx()
                val triH = triangleHeight.toPx()
                val tipX = arrowXPx.coerceIn(triW / 2, size.width - triW / 2)
                val path = Path().apply {
                    moveTo(tipX, 0f)
                    lineTo(tipX - triW / 2, triH)
                    lineTo(tipX + triW / 2, triH)
                    close()
                }
                drawPath(path, color = surfaceColor)
            }
        }

        Surface(
            modifier = Modifier
                .width(225.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (showArrow && !isAbove) Modifier.offset(y = -overlap)
                    else Modifier
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                content()
            }
        }

        // Arrow pointing DOWN (popup is above the anchor item)
        if (showArrow && isAbove) {
            Canvas(
                modifier = Modifier
                    .width(225.dp)
                    .height(triangleHeight)
                    .offset(y = -overlap)
            ) {
                val triW = triangleWidth.toPx()
                val triH = triangleHeight.toPx()
                val tipX = arrowXPx.coerceIn(triW / 2, size.width - triW / 2)
                val path = Path().apply {
                    moveTo(tipX - triW / 2, 0f)
                    lineTo(tipX + triW / 2, 0f)
                    lineTo(tipX, triH)
                    close()
                }
                drawPath(path, color = surfaceColor)
            }
        }
    }
}
