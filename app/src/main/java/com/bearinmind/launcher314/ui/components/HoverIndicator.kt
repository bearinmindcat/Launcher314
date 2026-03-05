package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TileColorOnHover.kt - Hover indicator logic for grid cells and dock slots.
 *
 * This file contains the reusable hover indicator component that shows a grey/blue
 * tile when dragging items over grid cells, widget cells, or dock slots.
 *
 * Used by:
 * - Grid cells (Empty, App, Widget, WidgetSpan) in AppGridMovement.kt
 * - Dock slots in AppGridMovement.kt
 *
 * The indicator fades in when hovered and fades out when not hovered,
 * providing visual feedback during drag operations.
 */

/**
 * Default color for hover indicator - light blue/grey (valid drop target)
 */
val HoverIndicatorColor = Color(0xFFD3E3FD)

/**
 * Color for invalid drop target - red
 */
val HoverIndicatorColorInvalid = Color(0xFFFF6B6B)

/**
 * Default alpha when hovered
 */
const val HoverIndicatorAlpha = 0.3f

/**
 * Alpha for invalid drop target (slightly more visible)
 */
const val HoverIndicatorAlphaInvalid = 0.4f

/**
 * Default corner radius for hover indicator
 */
val HoverIndicatorCornerRadius = 12.dp

/**
 * Animated hover indicator that fades in/out based on hover state.
 *
 * @param isHovered Whether this cell/slot is currently being hovered over
 * @param modifier Optional modifier for the outer container
 * @param padding Padding around the indicator (default 6.dp to match marker half size)
 * @param color Color of the indicator (default HoverIndicatorColor)
 * @param targetAlpha Alpha when hovered (default HoverIndicatorAlpha)
 * @param cornerRadius Corner radius of the indicator (default HoverIndicatorCornerRadius)
 * @param fadeInDuration Duration of fade in animation in ms (default 100)
 * @param fadeOutDuration Duration of fade out animation in ms (default 200)
 */
@Composable
fun HoverIndicator(
    isHovered: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = 6.dp,
    color: Color = HoverIndicatorColor,
    targetAlpha: Float = HoverIndicatorAlpha,
    cornerRadius: Dp = HoverIndicatorCornerRadius,
    fadeInDuration: Int = 100,
    fadeOutDuration: Int = 200
) {
    val alpha by animateFloatAsState(
        targetValue = if (isHovered) targetAlpha else 0f,
        animationSpec = tween(
            durationMillis = if (isHovered) fadeInDuration else fadeOutDuration,
            easing = FastOutSlowInEasing
        ),
        label = "hoverIndicatorAlpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

/**
 * Hover indicator for grid cells - square with standard styling.
 * Use this in DraggableGridCell for Empty, App, Widget, and WidgetSpan cells.
 *
 * @param isHovered Whether this cell is currently being hovered over
 * @param isValidDropTarget Whether this is a valid drop target (true = blue, false = red)
 * @param markerHalfSize Padding to match the "+" marker positioning (default 6.dp)
 */
@Composable
fun GridCellHoverIndicator(
    isHovered: Boolean,
    isValidDropTarget: Boolean = true,
    markerHalfSize: Dp = 6.dp,
    cornerRadius: Dp = HoverIndicatorCornerRadius
) {
    HoverIndicator(
        isHovered = isHovered,
        padding = markerHalfSize,
        color = if (isValidDropTarget) HoverIndicatorColor else HoverIndicatorColorInvalid,
        targetAlpha = if (isValidDropTarget) HoverIndicatorAlpha else HoverIndicatorAlphaInvalid,
        cornerRadius = cornerRadius
    )
}

/**
 * Hover indicator for dock slots - uses same styling as grid cells.
 * Use this in DockSlot component.
 *
 * @param isHovered Whether this dock slot is currently being hovered over
 * @param isValidDropTarget Whether this is a valid drop target (true = blue, false = red)
 * @param markerHalfSize Padding to match grid cell styling (default 6.dp)
 */
@Composable
fun DockSlotHoverIndicator(
    isHovered: Boolean,
    isValidDropTarget: Boolean = true,
    markerHalfSize: Dp = 6.dp,
    cornerRadius: Dp = HoverIndicatorCornerRadius
) {
    HoverIndicator(
        isHovered = isHovered,
        padding = markerHalfSize,
        color = if (isValidDropTarget) HoverIndicatorColor else HoverIndicatorColorInvalid,
        targetAlpha = if (isValidDropTarget) HoverIndicatorAlpha else HoverIndicatorAlphaInvalid,
        cornerRadius = cornerRadius
    )
}

/**
 * Creates the animated alpha value for a hover indicator.
 * Use this when you need just the alpha value without the full composable.
 *
 * @param isHovered Whether the item is being hovered over
 * @param targetAlpha Alpha when hovered (default HoverIndicatorAlpha)
 * @param fadeInDuration Duration of fade in animation in ms (default 100)
 * @param fadeOutDuration Duration of fade out animation in ms (default 200)
 * @return Animated alpha value
 */
@Composable
fun rememberHoverAlpha(
    isHovered: Boolean,
    targetAlpha: Float = HoverIndicatorAlpha,
    fadeInDuration: Int = 100,
    fadeOutDuration: Int = 200
): Float {
    val alpha by animateFloatAsState(
        targetValue = if (isHovered) targetAlpha else 0f,
        animationSpec = tween(
            durationMillis = if (isHovered) fadeInDuration else fadeOutDuration,
            easing = FastOutSlowInEasing
        ),
        label = "hoverAlpha"
    )
    return alpha
}
