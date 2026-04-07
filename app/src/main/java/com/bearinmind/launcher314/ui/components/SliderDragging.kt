package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Touch radius around the thumb for drag detection.
 * User must touch within this distance of the thumb to start dragging.
 */
private val THUMB_TOUCH_RADIUS = 48.dp

/**
 * Configuration for a horizontal thumb-drag-only slider.
 */
data class HorizontalSliderConfig(
    val minValue: Float,
    val maxValue: Float,
    val tickValues: List<Int>,
    val labeledTickValues: List<Int> = tickValues,
    val snapTickValues: List<Int> = tickValues,
    val label: String,
    val showMinorTicks: Boolean = false,
    val labelSuffix: String = ""
)

/**
 * Configuration for a vertical thumb-drag-only slider.
 */
data class VerticalSliderConfig(
    val minValue: Float,
    val maxValue: Float,
    val majorTickValues: List<Int>,
    val minorTickValues: List<Int> = majorTickValues,
    val snapTickValues: List<Int> = majorTickValues,
    val label: String
)

/**
 * A horizontal slider that only responds to thumb dragging, not track clicks.
 * Prevents accidental value changes while scrolling.
 */
@Composable
fun ThumbDragHorizontalSlider(
    currentValue: Float,
    config: HorizontalSliderConfig,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overflowThreshold: Float = config.maxValue, // above this value, track/thumb turns red and snaps back
    onDoubleTap: (() -> Unit)? = null // optional double-tap to reset
) {
    // Cap at highest usable snap value (below overflow threshold)
    val dragMax = if (overflowThreshold >= config.maxValue) config.maxValue
        else config.snapTickValues.filter { it.toFloat() <= overflowThreshold }.maxOrNull()?.toFloat() ?: config.minValue

    val animatedValue = remember { Animatable(currentValue) }
    // Synchronous drag value — avoids one-frame lag from coroutineScope.launch { snapTo }
    var dragValue by remember { mutableFloatStateOf(currentValue) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isDragOnThumb by remember { mutableStateOf(false) }
    var isOverflowSnapping by remember { mutableStateOf(false) }

    // rememberUpdatedState prevents stale capture inside pointerInput(Unit)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    // Dark colors for disabled state (solid dark grey, not transparent)
    val disabledColor = Color(0xFF2A2A2A)
    val disabledTickColor = Color(0xFF3A3A3A)
    val disabledThumbColor = Color(0xFF444444)
    val disabledTextColor = Color(0xFF3A3A3A)

    LaunchedEffect(currentValue) {
        if (!isDragging && !isOverflowSnapping) {
            dragValue = currentValue
            animatedValue.animateTo(
                targetValue = currentValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Use dragValue during drag (synchronous), animatedValue during animation (snap-back bounce)
    val displayValue = if (isDragging) dragValue else animatedValue.value
    val thumbFraction = ((displayValue - config.minValue) / (config.maxValue - config.minValue)).coerceIn(0f, 1f)

    // Overflow zone colors (matches vertical slider exactly)
    val overflowRed = Color(0xFFCC4444)
    val overflowRange = config.maxValue - overflowThreshold
    val zoneRedFraction = if (overflowThreshold >= config.maxValue || displayValue <= overflowThreshold) 0f
        else if (overflowRange > 0f) ((displayValue - overflowThreshold) / overflowRange).coerceIn(0f, 1f) else 1f
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .then(
                        if (onDoubleTap != null && enabled) Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { onDoubleTap() }
                            )
                        } else Modifier
                    )
                    .pointerInput(enabled, overflowThreshold) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { offset: Offset ->
                                val x = offset.x
                                val width = size.width.toFloat()
                                val currentThumbFraction = (animatedValue.value - config.minValue) / (config.maxValue - config.minValue)
                                val currentThumbX = currentThumbFraction * width
                                val thumbTouchRadius = THUMB_TOUCH_RADIUS.toPx()

                                if (kotlin.math.abs(x - currentThumbX) <= thumbTouchRadius) {
                                    isDragging = true
                                    isDragOnThumb = true
                                } else {
                                    isDragOnThumb = false
                                }
                            },
                            onDragEnd = {
                                if (isDragOnThumb) {
                                    val validSnaps = config.snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - dragValue)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = dragValue > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    currentOnValueChange(snappedValue)
                                    currentOnValueChangeFinished()
                                    coroutineScope.launch {
                                        animatedValue.snapTo(dragValue) // sync animatable to drag position
                                        isDragging = false // safe: animatedValue now matches dragValue
                                        isDragOnThumb = false
                                        animatedValue.animateTo(
                                            targetValue = snappedValue,
                                            animationSpec = if (wasInOverflow) spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = 300f
                                            ) else spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        isOverflowSnapping = false
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isDragOnThumb) {
                                    val validSnaps = config.snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - dragValue)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = dragValue > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    currentOnValueChange(snappedValue)
                                    currentOnValueChangeFinished()
                                    coroutineScope.launch {
                                        animatedValue.snapTo(dragValue)
                                        isDragging = false
                                        isDragOnThumb = false
                                        animatedValue.animateTo(
                                            targetValue = snappedValue,
                                            animationSpec = if (wasInOverflow) spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = 300f
                                            ) else spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        isOverflowSnapping = false
                                    }
                                }
                            },
                            onHorizontalDrag = { change, _ ->
                                if (isDragOnThumb) {
                                    change.consume()
                                    val x = change.position.x
                                    val width = size.width.toFloat()
                                    val fraction = (x / width).coerceIn(0f, 1f)
                                    val newValue = (config.minValue + fraction * (config.maxValue - config.minValue))
                                        .coerceIn(config.minValue, config.maxValue)
                                    dragValue = newValue  // synchronous — no frame lag
                                    currentOnValueChange(newValue)
                                }
                            }
                        )
                    }
            ) {
                val trackWidth = maxWidth
                val trackCenterY = maxHeight / 2

                // Horizontal track line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.Center)
                        .background(if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else disabledColor)
                )

                // Red tint overlay on track — gradient fade at the threshold boundary
                if (overflowThreshold < config.maxValue && zoneRedFraction > 0f) {
                    val thresholdFraction = ((overflowThreshold - config.minValue) / (config.maxValue - config.minValue)).coerceIn(0f, 1f)
                    val fadePadding = 0.05f * (1f - thresholdFraction)
                    val fadeStart = (thresholdFraction - fadePadding).coerceAtLeast(0f)
                    val tintColor = overflowRed.copy(alpha = zoneRedFraction * 0.8f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.Center)
                            .background(
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        fadeStart to Color.Transparent,
                                        thresholdFraction to tintColor,
                                        1f to tintColor
                                    )
                                )
                            )
                    )
                }

                // Tick marks (two-layer like vertical slider: normal always visible, red overlay on top)
                if (config.showMinorTicks) {
                    config.snapTickValues.forEach { value ->
                        val fraction = (value - config.minValue) / (config.maxValue - config.minValue)
                        val xOffset = trackWidth * fraction
                        val isMajorTick = value in config.labeledTickValues
                        val tickWidth = if (isMajorTick) 2.dp else 1.dp
                        val tickHeight = if (isMajorTick) 8.dp else 5.dp
                        val tickXOff = xOffset - (if (isMajorTick) 1.dp else 0.5.dp)
                        val tickYOff = trackCenterY - (if (isMajorTick) 4.dp else 2.5.dp)

                        // Normal tick (always visible)
                        Box(
                            modifier = Modifier
                                .width(tickWidth)
                                .height(tickHeight)
                                .offset(x = tickXOff, y = tickYOff)
                                .background(
                                    if (!enabled) disabledTickColor
                                    else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (isMajorTick) 0.5f else 0.3f
                                    )
                                )
                        )
                        // Red overlay on tick (additive, matching vertical slider)
                        if (overflowThreshold < config.maxValue && value.toFloat() > overflowThreshold && zoneRedFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .width(tickWidth)
                                    .height(tickHeight)
                                    .offset(x = tickXOff, y = tickYOff)
                                    .background(overflowRed.copy(alpha = zoneRedFraction * 0.8f))
                            )
                        }
                    }
                } else {
                    // Show only major ticks
                    config.tickValues.forEachIndexed { index, _ ->
                        val fraction = index.toFloat() / (config.tickValues.size - 1)
                        val xOffset = trackWidth * fraction

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(8.dp)
                                .offset(x = xOffset - 1.dp, y = trackCenterY - 4.dp)
                                .background(if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else disabledTickColor)
                        )
                    }
                }

                // Thumb color: normal when at or below threshold, gradient red when past
                val thumbColor = if (!enabled) disabledThumbColor
                    else if (overflowThreshold >= config.maxValue || displayValue <= overflowThreshold) primaryColor
                    else {
                        val t = if (overflowRange > 0f) ((displayValue - overflowThreshold) / overflowRange).coerceIn(0f, 1f) else 1f
                        androidx.compose.ui.graphics.lerp(primaryColor, overflowRed, t)
                    }

                // Thumb
                val thumbXOffset = trackWidth * thumbFraction
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(20.dp)
                        .offset(x = thumbXOffset - 3.dp, y = trackCenterY - 10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(thumbColor)
                )
            }

            // Touch indicator when dragging
            if (isDragging) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val thumbXOffset = maxWidth * thumbFraction
                    val touchColor = if (!enabled) disabledThumbColor.copy(alpha = 0.15f)
                        else if (overflowThreshold < config.maxValue && displayValue > overflowThreshold)
                            overflowRed.copy(alpha = zoneRedFraction * 0.25f)
                        else primaryColor.copy(alpha = 0.15f)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .offset(x = thumbXOffset - 24.dp)
                            .clip(CircleShape)
                            .background(touchColor)
                    )
                }
            }
        }

        // Number labels
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            val trackWidth = maxWidth
            val labelWidth = (if (config.labelSuffix.isNotEmpty()) 28.dp else 20.dp)
            config.labeledTickValues.forEachIndexed { index, value ->
                val fraction = if (config.labeledTickValues.size > 1) {
                    (value - config.minValue) / (config.maxValue - config.minValue)
                } else {
                    index.toFloat()
                }
                val xOffset = trackWidth * fraction

                Box(
                    modifier = Modifier
                        .width(labelWidth)
                        .offset(x = xOffset - (labelWidth / 2)),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = kotlin.math.abs(currentValue - value) <
                        (config.maxValue - config.minValue) / (config.labeledTickValues.size * 2)
                    Text(
                        text = "$value${config.labelSuffix}",
                        fontSize = (if (config.labelSuffix.isNotEmpty()) 9.sp else 10.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (enabled) {
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        } else {
                            disabledTextColor
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Label
        Text(
            text = config.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else disabledTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A vertical slider that only responds to thumb dragging, not track clicks.
 * Prevents accidental value changes while scrolling.
 */
@Composable
fun ThumbDragVerticalSlider(
    currentValue: Float,
    sliderHeight: Dp,
    config: VerticalSliderConfig,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    overflowThreshold: Float = 125f  // above this value, track/thumb turns red (visual warning only)
) {
    // Cap at highest usable snap value (below overflow threshold)
    val dragMax = if (overflowThreshold >= config.maxValue) config.maxValue
        else config.snapTickValues.filter { it.toFloat() <= overflowThreshold }.maxOrNull()?.toFloat() ?: config.minValue
    val clampedValue = currentValue.coerceIn(config.minValue, config.maxValue)
    val animatedValue = remember { Animatable(clampedValue) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isDragOnThumb by remember { mutableStateOf(false) }
    var isOverflowSnapping by remember { mutableStateOf(false) }

    LaunchedEffect(clampedValue) {
        if (!isDragging && !isOverflowSnapping) {
            animatedValue.animateTo(
                targetValue = clampedValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .height(sliderHeight)
    ) {
        // Label at top
        Text(
            text = config.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.offset(x = 10.dp, y = (-12).dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Numbers on the left
            BoxWithConstraints(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
            ) {
                val totalHeight = maxHeight
                config.majorTickValues.reversed().forEachIndexed { index, value ->
                    val fraction = index.toFloat() / (config.majorTickValues.size - 1)
                    val yOffset = totalHeight * fraction
                    Text(
                        text = "$value",
                        fontSize = 8.sp,
                        fontWeight = if (currentValue.roundToInt() == value) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentValue.roundToInt() == value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = 4.dp, y = yOffset - 6.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Track, tick marks, and thumb
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(overflowThreshold) { // Rebuild when threshold changes
                        detectDragGestures(
                            onDragStart = { offset: Offset ->
                                val y = offset.y
                                val height = size.height.toFloat()
                                // Recalculate thumb position using current animated value (not captured thumbFraction)
                                val currentThumbFraction = (1f - (animatedValue.value - config.minValue) / (config.maxValue - config.minValue)).coerceIn(0f, 1f)
                                val currentThumbY = currentThumbFraction * height
                                val thumbTouchRadius = THUMB_TOUCH_RADIUS.toPx()

                                if (kotlin.math.abs(y - currentThumbY) <= thumbTouchRadius) {
                                    isDragging = true
                                    isDragOnThumb = true
                                } else {
                                    isDragOnThumb = false
                                }
                            },
                            onDragEnd = {
                                if (isDragOnThumb) {
                                    // === Icon size slider snap-back animation ===
                                    // When released from the red overflow zone, animate back
                                    // to the nearest valid tick with a bouncy spring.
                                    // stiffness = 300f controls the speed, DampingRatioMediumBouncy adds bounce.
                                    val validSnaps = config.snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - animatedValue.value)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = animatedValue.value > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    onValueChange(snappedValue)
                                    onValueChangeFinished()
                                    coroutineScope.launch {
                                        animatedValue.animateTo(
                                            targetValue = snappedValue,
                                            animationSpec = if (wasInOverflow) spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = 300f
                                            ) else spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        isOverflowSnapping = false
                                    }
                                }
                                isDragging = false
                                isDragOnThumb = false
                            },
                            onDragCancel = {
                                if (isDragOnThumb) {
                                    // === Icon size slider snap-back animation (on cancel) ===
                                    // Same logic as onDragEnd above.
                                    val validSnaps = config.snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - animatedValue.value)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = animatedValue.value > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    onValueChange(snappedValue)
                                    coroutineScope.launch {
                                        animatedValue.animateTo(
                                            targetValue = snappedValue,
                                            animationSpec = if (wasInOverflow) spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = 300f
                                            ) else spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        isOverflowSnapping = false
                                    }
                                }
                                isDragging = false
                                isDragOnThumb = false
                            },
                            onDrag = { change, _: Offset ->
                                if (isDragOnThumb) {
                                    change.consume()
                                    val y = change.position.y
                                    val height = size.height.toFloat()
                                    val fraction = 1f - (y / height).coerceIn(0f, 1f)
                                    val newValue = (config.minValue + fraction * (config.maxValue - config.minValue))
                                        .coerceIn(config.minValue, config.maxValue)
                                    coroutineScope.launch { animatedValue.snapTo(newValue) }
                                    onValueChange(newValue)
                                }
                            }
                        )
                    }
            ) {
                val trackHeight = maxHeight

                val overflowRed = Color(0xFFCC4444)
                val normalTrackColor = MaterialTheme.colorScheme.onSurface
                val overflowRange = config.maxValue - overflowThreshold
                // How red the overflow zone should be — driven by thumb position
                val zoneRedFraction = if (overflowThreshold >= config.maxValue || animatedValue.value <= overflowThreshold) 0f
                    else if (overflowRange > 0f) ((animatedValue.value - overflowThreshold) / overflowRange).coerceIn(0f, 1f) else 1f

                // Vertical track line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(normalTrackColor.copy(alpha = 0.3f))
                )
                // Red tint overlay on track — gradient fade at the threshold boundary
                if (overflowThreshold < config.maxValue && zoneRedFraction > 0f) {
                    val thresholdFraction = (1f - (overflowThreshold - config.minValue) / (config.maxValue - config.minValue)).coerceIn(0f, 1f)
                    // Extend 5% past threshold for a smooth fade
                    val fadePadding = 0.05f * (1f - thresholdFraction)
                    val extendedFraction = (thresholdFraction + fadePadding).coerceAtMost(1f)
                    val tintColor = overflowRed.copy(alpha = zoneRedFraction * 0.8f)
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight(extendedFraction)
                            .align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(
                                0f to tintColor,
                                (thresholdFraction / extendedFraction).coerceIn(0f, 1f) to tintColor,
                                1f to Color.Transparent
                            ))
                    )
                }

                // Minor tick marks
                config.minorTickValues.forEach { value ->
                    if (value !in config.majorTickValues) {
                        val fraction = 1f - (value - config.minValue) / (config.maxValue - config.minValue)
                        val yOffset = trackHeight * fraction

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(1.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = yOffset - 0.5.dp)
                                .background(normalTrackColor.copy(alpha = 0.3f))
                        )
                        // Red tint overlay on tick
                        if (overflowThreshold < config.maxValue && value.toFloat() > overflowThreshold && zoneRedFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(1.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = yOffset - 0.5.dp)
                                    .background(overflowRed.copy(alpha = zoneRedFraction * 0.8f))
                            )
                        }
                    }
                }

                // Major tick marks
                config.majorTickValues.reversed().forEachIndexed { index, value ->
                    val fraction = index.toFloat() / (config.majorTickValues.size - 1)
                    val yOffset = trackHeight * fraction

                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = yOffset - 1.dp)
                            .background(normalTrackColor.copy(alpha = 0.5f))
                    )
                    // Red tint overlay on tick
                    if (overflowThreshold < config.maxValue && value.toFloat() > overflowThreshold && zoneRedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = yOffset - 1.dp)
                                .background(overflowRed.copy(alpha = zoneRedFraction * 0.8f))
                        )
                    }
                }

                // Thumb position for rendering
                val renderThumbFraction = (1f - (animatedValue.value - config.minValue) / (config.maxValue - config.minValue)).coerceIn(0f, 1f)
                val thumbYOffset = trackHeight * renderThumbFraction

                // Thumb color: normal when at or below threshold, gradient red only when past it
                val primaryColor = MaterialTheme.colorScheme.primary
                val thumbColor = if (overflowThreshold >= config.maxValue || animatedValue.value <= overflowThreshold) {
                    primaryColor
                } else {
                    val t = if (overflowRange > 0f) ((animatedValue.value - overflowThreshold) / overflowRange).coerceIn(0f, 1f) else 1f
                    androidx.compose.ui.graphics.lerp(primaryColor, overflowRed, t)
                }

                // Touch indicator when dragging
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = thumbYOffset - 24.dp)
                            .clip(CircleShape)
                            .background(thumbColor.copy(alpha = 0.15f))
                    )
                }

                // Thumb - horizontal rectangle (gradient red near overflow zone)
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(6.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = thumbYOffset - 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(thumbColor)
                )
            }
        }
    }
}

/**
 * Pre-configured slider configs for common use cases
 */
object SliderConfigs {
    val scrollbarWidth = HorizontalSliderConfig(
        minValue = 50f,
        maxValue = 150f,
        tickValues = (50..150 step 5).toList(),
        labeledTickValues = listOf(50, 75, 100, 125, 150),
        snapTickValues = (50..150 step 5).toList(),
        showMinorTicks = true,
        label = "Width (%)",
        labelSuffix = "%"
    )

    val scrollbarHeight = HorizontalSliderConfig(
        minValue = 50f,
        maxValue = 150f,
        tickValues = (50..150 step 5).toList(),
        labeledTickValues = listOf(50, 75, 100, 125, 150),
        snapTickValues = (50..150 step 5).toList(),
        showMinorTicks = true,
        label = "Height (%)",
        labelSuffix = "%"
    )

    val colorIntensity = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = listOf(0, 25, 50, 75, 100),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "Color Intensity",
        labelSuffix = "%"
    )

    val iconColorIntensity = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = listOf(0, 25, 50, 75, 100),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "",
        labelSuffix = "%"
    )

    val tintIntensity = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = listOf(0, 25, 50, 75, 100),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "Tint Intensity",
        labelSuffix = "%"
    )

    val perAppIconSize = HorizontalSliderConfig(
        minValue = 50f,
        maxValue = 125f,
        tickValues = listOf(50, 75, 100, 125),
        labeledTickValues = listOf(50, 75, 100, 125),
        snapTickValues = (50..125 step 5).toList(),
        showMinorTicks = true,
        label = "Icon Size",
        labelSuffix = "%"
    )

    val drawerTransparency = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = (0..100 step 5).toList(),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "Drawer Transparency",
        labelSuffix = "%"
    )

    val gridColumns = HorizontalSliderConfig(
        minValue = 3f,
        maxValue = 7f,
        tickValues = listOf(3, 4, 5, 6, 7),
        label = "App Grid Columns"
    )

    val gridRows = HorizontalSliderConfig(
        minValue = 4f,
        maxValue = 8f,
        tickValues = listOf(4, 5, 6, 7, 8),
        label = "App Grid Rows"
    )

    val drawerRows = HorizontalSliderConfig(
        minValue = 4f,
        maxValue = 8f,
        tickValues = listOf(4, 5, 6, 7, 8),
        label = "App Drawer Rows"
    )

    val dockColumns = HorizontalSliderConfig(
        minValue = 1f,
        maxValue = 7f,
        tickValues = listOf(1, 2, 3, 4, 5, 6, 7),
        label = "Dock Columns"
    )

    val iconTextSize = HorizontalSliderConfig(
        minValue = 50f,
        maxValue = 150f,
        tickValues = (50..150 step 5).toList(),
        labeledTickValues = listOf(50, 75, 100, 125, 150),
        snapTickValues = (50..150 step 5).toList(),
        showMinorTicks = true,
        label = "Text Size",
        labelSuffix = "%"
    )

    val iconSizePercent = VerticalSliderConfig(
        minValue = 50f,
        maxValue = 125f,
        majorTickValues = listOf(50, 75, 100, 125),
        minorTickValues = (50..125 step 5).toList(),
        snapTickValues = (50..125 step 5).toList(),
        label = "Icon Size"
    )

    val cornerRoundness = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = listOf(0, 25, 50, 75, 100),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "Corner Roundness (%)",
        labelSuffix = "%"
    )

    val widgetTextSize = HorizontalSliderConfig(
        minValue = 50f,
        maxValue = 150f,
        tickValues = (50..150 step 5).toList(),
        labeledTickValues = listOf(50, 75, 100, 125, 150),
        snapTickValues = (50..150 step 5).toList(),
        showMinorTicks = true,
        label = "Widget Text Size (%)",
        labelSuffix = "%"
    )

    val widgetPadding = HorizontalSliderConfig(
        minValue = 0f,
        maxValue = 100f,
        tickValues = listOf(0, 25, 50, 75, 100),
        labeledTickValues = listOf(0, 25, 50, 75, 100),
        snapTickValues = (0..100 step 5).toList(),
        showMinorTicks = true,
        label = "Widget Spacing (%)",
        labelSuffix = "%"
    )
}
