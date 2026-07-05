package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
    // When set, the popup is horizontally CENTERED as if it were this wide,
    // regardless of its actual (possibly wider) content. Lets a popup grow to
    // the right (e.g. a fly-out panel) WITHOUT the base region shifting — it
    // stays anchored under the icon. Clamping still uses the real width.
    private val xAnchorWidthPx: Int? = null,
    // Minimum gap kept between the popup and the left/right screen edges so a
    // wide popup never runs flush to (or past) an edge.
    private val edgeMarginPx: Int = 0,
    private val onPlacement: (isAbove: Boolean, arrowXPx: Float) -> Unit
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Use the icon's actual visual bounds for centering whenever the
        // caller provided them (e.g. detached free-floating icons whose
        // popup composable lives outside the icon's layout slot, so the
        // anchorBounds Compose hands us is unrelated to the icon's
        // position). Falls back to anchorBounds when no icon bounds were
        // supplied — preserves original behavior for grid cells whose
        // popup composable is nested inside the cell.
        val useIconBounds = iconBoundsInRoot != androidx.compose.ui.geometry.Rect.Zero
        val anchorCenterX: Float
        val iconTop: Int
        val iconBottom: Int
        if (useIconBounds) {
            anchorCenterX = (iconBoundsInRoot.left + iconBoundsInRoot.right) / 2f
            iconTop = iconBoundsInRoot.top.toInt()
            iconBottom = iconBoundsInRoot.bottom.toInt()
        } else {
            anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
            iconTop = anchorBounds.top
            iconBottom = anchorBounds.bottom
        }

        // Horizontal: center popup on anchor center, clamped to screen edges.
        // When xAnchorWidthPx is set, center as if the popup were that wide so
        // the base region stays put under the icon while extra content grows to
        // the right; clamp still uses the real width so it never runs off-screen.
        val centerWidth = (xAnchorWidthPx ?: popupContentSize.width).toFloat()
        val x = (anchorCenterX - centerWidth / 2f).toInt()
        // Keep an edge margin on BOTH sides; if the popup is too wide to honor
        // both, fall back to pinning at the left margin.
        val maxX = (windowSize.width - popupContentSize.width - edgeMarginPx)
        val clampedX = x.coerceIn(edgeMarginPx, maxX.coerceAtLeast(edgeMarginPx))

        val gap = gapPx
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
            isAbove = windowSize.height - iconBottom < iconTop
            y = if (isAbove) aboveY else belowY
        }

        // Arrow tip X = anchor center, relative to popup's left edge
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
    // When set, the popup may grow wider than the default 225dp up to this max
    // (content-driven), e.g. for a menu whose section flies out to the right.
    // Null keeps the original fixed 225dp width (every existing popup unchanged).
    maxWidthDp: Int? = null,
    // When set, the popup is positioned as if it were this wide so its base
    // region stays anchored under the icon while wider content (a fly-out
    // panel) grows to the right rather than re-centering the whole popup.
    xAnchorWidthDp: Int? = null,
    // Minimum gap kept between the popup and the left/right screen edges.
    edgeMarginDp: Int = 0,
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
            // Keep the window alive just long enough for the 110ms close anim.
            delay(120)
            showPopup = false
        }
    }

    if (showPopup) {
        val useArrow = popupPositionProvider == null
        val gapPx = with(density) { gapDp.dp.roundToPx() }
        val xAnchorWidthPx = xAnchorWidthDp?.let { with(density) { it.dp.roundToPx() } }
        val edgeMarginPx = with(density) { edgeMarginDp.dp.roundToPx() }
        val provider = popupPositionProvider ?: remember(iconBoundsInRoot, gapPx, xAnchorWidthPx, edgeMarginPx) {
            ArrowPopupPositionProvider(
                iconBoundsInRoot = iconBoundsInRoot,
                gapPx = gapPx,
                xAnchorWidthPx = xAnchorWidthPx,
                edgeMarginPx = edgeMarginPx
            ) { above, x ->
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
                maxWidthDp = maxWidthDp,
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
    maxWidthDp: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val show = visible && appeared

    // Lawnchair / Launcher3-style open & close: the menu POPS OUT of the icon
    // with a fast spring that slightly overshoots (transform origin is already
    // at the arrow tip, so it grows from the anchor), and shrinks back INTO
    // the icon on close with a quick accelerate-out. Much snappier than the
    // old flat 150ms tween from 0.9x.
    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0.5f,
        animationSpec = if (show) {
            spring(dampingRatio = 0.8f, stiffness = 1100f)
        } else {
            tween(110, easing = FastOutLinearInEasing)
        },
        label = "popupScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = if (show) {
            tween(90, easing = LinearOutSlowInEasing)
        } else {
            tween(110, easing = FastOutLinearInEasing)
        },
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
                .then(
                    if (maxWidthDp != null) Modifier.widthIn(min = 225.dp, max = maxWidthDp.dp)
                    else Modifier.width(225.dp)
                )
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
