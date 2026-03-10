package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Custom vertical slider for icon size selection.
 * Supports smooth dragging with animated snap-on-release behavior.
 */
@Composable
fun VerticalIconSizeSlider(
    currentSize: Float,
    sliderHeight: Dp,
    isLinked: Boolean,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit,
    overflowThreshold: Float = 125f  // above this value, track/thumb turns red (visual warning only)
) {
    val minValue = 50f
    val effectiveMax = 125f
    // Major tick values (at every 25%)
    val majorTickValues = listOf(50, 75, 100, 125)
    // Minor tick values (every 5%)
    val minorTickValues = (50..125 step 5).toList()
    // Snap to 5% increments (when linked, snap to linked values within range)
    val snapTickValues = if (isLinked) listOf(67, 80, 100, 133).filter { it <= 125 } else minorTickValues

    // Cap at highest usable snap value (below overflow threshold)
    val dragMax = if (overflowThreshold >= effectiveMax) effectiveMax
        else snapTickValues.filter { it.toFloat() <= overflowThreshold }.maxOrNull()?.toFloat() ?: minValue
    // Clamp currentSize to full range (allow dragging into red zone)
    val clampedSize = currentSize.coerceIn(minValue, effectiveMax)
    // Animated value for smooth transitions
    val animatedValue = remember { Animatable(clampedSize) }
    val coroutineScope = rememberCoroutineScope()

    // Track if user is currently dragging
    var isDragging by remember { mutableStateOf(false) }
    var isDragOnThumb by remember { mutableStateOf(false) }  // Only drag if started on thumb
    var isOverflowSnapping by remember { mutableStateOf(false) }

    // Sync animated value with external changes (e.g., from linked slider)
    // Animate smoothly when not dragging
    LaunchedEffect(clampedSize) {
        if (!isDragging && !isOverflowSnapping) {
            animatedValue.animateTo(
                targetValue = clampedSize,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .height(sliderHeight)
    ) {
        // Icon Size label at top
        Text(
            text = "Icon Size",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.offset(x = 10.dp, y = (-12).dp)
        )

        // Custom vertical slider with tick marks
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Numbers on the left (only major tick values)
            BoxWithConstraints(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
            ) {
                val totalHeight = maxHeight
                majorTickValues.reversed().forEachIndexed { index, value ->
                    val fraction = index.toFloat() / (majorTickValues.size - 1)
                    val yOffset = totalHeight * fraction
                    Text(
                        text = "$value",
                        fontSize = 8.sp,
                        fontWeight = if (currentSize.roundToInt() == value) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentSize.roundToInt() == value)
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
                    .pointerInput(isLinked, overflowThreshold) { // Rebuild when linked state or threshold changes
                        detectDragGestures(
                            onDragStart = { offset ->
                                val y = offset.y
                                val height = size.height.toFloat()
                                // Compute thumb position from current animated value (not stale captured thumbFraction)
                                val currentThumbFraction = (1f - (animatedValue.value - minValue) / (effectiveMax - minValue)).coerceIn(0f, 1f)
                                val currentThumbY = currentThumbFraction * height
                                val thumbTouchRadius = 48.dp.toPx()  // Touch area around thumb

                                // Only start drag if touch is on the thumb
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
                                    val validSnaps = snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - animatedValue.value)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = animatedValue.value > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    onSizeChange(snappedValue)
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
                                        onSizeChangeFinished()
                                    }
                                }
                                isDragging = false
                                isDragOnThumb = false
                            },
                            onDragCancel = {
                                if (isDragOnThumb) {
                                    // === Icon size slider snap-back animation (on cancel) ===
                                    // Same logic as onDragEnd above.
                                    val validSnaps = snapTickValues.filter { it.toFloat() <= dragMax }
                                    val snappedValue = validSnaps.minByOrNull {
                                        kotlin.math.abs(it - animatedValue.value)
                                    }?.toFloat() ?: dragMax
                                    val wasInOverflow = animatedValue.value > dragMax
                                    if (wasInOverflow) isOverflowSnapping = true
                                    onSizeChange(snappedValue)
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
                            onDrag = { change, _ ->
                                if (isDragOnThumb) {
                                    change.consume()
                                    val y = change.position.y
                                    val height = size.height.toFloat()
                                    val fraction = 1f - (y / height).coerceIn(0f, 1f)
                                    val newValue = (minValue + fraction * (effectiveMax - minValue)).coerceIn(minValue, effectiveMax)
                                    coroutineScope.launch {
                                        animatedValue.snapTo(newValue)
                                    }
                                    onSizeChange(newValue)
                                }
                            }
                        )
                    }
            ) {
                val trackHeight = maxHeight

                val overflowRed = Color(0xFFCC4444)
                val normalTrackColor = MaterialTheme.colorScheme.onSurface
                val overflowRange = effectiveMax - overflowThreshold
                // How red the overflow zone should be — driven by thumb position
                val zoneRedFraction = if (overflowThreshold >= effectiveMax || animatedValue.value <= overflowThreshold) 0f
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
                if (overflowThreshold < effectiveMax && zoneRedFraction > 0f) {
                    val thresholdFraction = (1f - (overflowThreshold - minValue) / (effectiveMax - minValue)).coerceIn(0f, 1f)
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

                // Minor tick marks (every 5%)
                minorTickValues.forEach { value ->
                    if (value !in majorTickValues) {
                        val fraction = 1f - (value - minValue) / (effectiveMax - minValue)
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
                        if (overflowThreshold < effectiveMax && value.toFloat() > overflowThreshold && zoneRedFraction > 0f) {
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
                majorTickValues.reversed().forEachIndexed { index, value ->
                    val fraction = index.toFloat() / (majorTickValues.size - 1)
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
                    if (overflowThreshold < effectiveMax && value.toFloat() > overflowThreshold && zoneRedFraction > 0f) {
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

                // Touch area indicator (visible when dragging)
                val thumbFraction = (1f - (animatedValue.value - minValue) / (effectiveMax - minValue)).coerceIn(0f, 1f)
                val thumbYOffset = trackHeight * thumbFraction

                // Thumb color: normal when at or below threshold, gradient red only when past it
                val primaryColor = MaterialTheme.colorScheme.primary
                val thumbColor = if (overflowThreshold >= effectiveMax || animatedValue.value <= overflowThreshold) {
                    primaryColor
                } else {
                    val t = if (overflowRange > 0f) ((animatedValue.value - overflowThreshold) / overflowRange).coerceIn(0f, 1f) else 1f
                    androidx.compose.ui.graphics.lerp(primaryColor, overflowRed, t)
                }

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

                // Thumb - horizontal rectangle (red when in overflow zone)
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
 * Custom horizontal slider for column count selection.
 * Supports smooth dragging with animated snap-on-release behavior.
 */
@Composable
fun BottomColumnsSlider(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    val tickValues = listOf(3, 4, 5, 6, 7)
    val minValue = 3f
    val maxValue = 7f

    // Animated value for smooth transitions
    val animatedValue = remember { Animatable(currentSize) }
    val coroutineScope = rememberCoroutineScope()

    // Track if user is currently dragging
    var isDragging by remember { mutableStateOf(false) }
    var isDragOnThumb by remember { mutableStateOf(false) }  // Only drag if started on thumb

    // Sync animated value with external changes (e.g., from linked slider)
    // Animate smoothly when not dragging
    LaunchedEffect(currentSize) {
        if (!isDragging) {
            animatedValue.animateTo(
                targetValue = currentSize,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Calculate thumb position for use in both wrapper Box and inner BoxWithConstraints
    val thumbFraction = (animatedValue.value - minValue) / (maxValue - minValue)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Wrapper Box to allow touch indicator overflow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), // Taller to accommodate the circle
            contentAlignment = Alignment.Center
        ) {
            // Custom horizontal slider
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val x = offset.x
                            val width = size.width.toFloat()
                            // Compute thumb position from current animated value (not stale captured thumbFraction)
                            val currentThumbFraction = (animatedValue.value - minValue) / (maxValue - minValue)
                            val currentThumbX = currentThumbFraction * width
                            val thumbTouchRadius = 48.dp.toPx()  // Touch area around thumb

                            // Only start drag if touch is on the thumb
                            if (kotlin.math.abs(x - currentThumbX) <= thumbTouchRadius) {
                                isDragging = true
                                isDragOnThumb = true
                            } else {
                                isDragOnThumb = false
                            }
                        },
                        onDragEnd = {
                            if (isDragOnThumb) {
                                // Calculate snapped value immediately
                                val snappedValue = tickValues.minByOrNull {
                                    kotlin.math.abs(it - animatedValue.value)
                                }?.toFloat() ?: animatedValue.value
                                // Update parent state with snapped value first
                                onSizeChange(snappedValue)
                                // Then animate to snapped position
                                coroutineScope.launch {
                                    animatedValue.animateTo(
                                        targetValue = snappedValue,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onSizeChangeFinished()
                                }
                            }
                            isDragging = false
                            isDragOnThumb = false
                        },
                        onDragCancel = {
                            if (isDragOnThumb) {
                                // Calculate snapped value immediately
                                val snappedValue = tickValues.minByOrNull {
                                    kotlin.math.abs(it - animatedValue.value)
                                }?.toFloat() ?: animatedValue.value
                                // Update parent state with snapped value first
                                onSizeChange(snappedValue)
                                // Then animate to snapped position
                                coroutineScope.launch {
                                    animatedValue.animateTo(
                                        targetValue = snappedValue,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                            isDragging = false
                            isDragOnThumb = false
                        },
                        onDrag = { change, _ ->
                            if (isDragOnThumb) {
                                change.consume()
                                val x = change.position.x
                                val width = size.width.toFloat()
                                val fraction = (x / width).coerceIn(0f, 1f)
                                val newValue = (minValue + fraction * (maxValue - minValue)).coerceIn(minValue, maxValue)
                                coroutineScope.launch {
                                    animatedValue.snapTo(newValue)
                                }
                                onSizeChange(newValue)
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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )

                // Tick marks
                tickValues.forEachIndexed { index, _ ->
                    val fraction = index.toFloat() / (tickValues.size - 1)
                    val xOffset = trackWidth * fraction

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .offset(x = xOffset - 1.dp, y = trackCenterY - 4.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }

                // Thumb - vertical rectangle
                val thumbXOffset = trackWidth * thumbFraction
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(20.dp)
                        .offset(x = thumbXOffset - 3.dp, y = trackCenterY - 10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            } // End BoxWithConstraints

            // Touch area indicator drawn in wrapper Box (outside BoxWithConstraints to avoid clipping)
            if (isDragging) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val thumbXOffset = maxWidth * thumbFraction
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .offset(x = thumbXOffset - 24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                }
            }
        } // End wrapper Box

        // Number labels
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            val trackWidth = maxWidth
            tickValues.forEachIndexed { index, value ->
                val fraction = index.toFloat() / (tickValues.size - 1)
                val xOffset = trackWidth * fraction

                Text(
                    text = "$value",
                    fontSize = 10.sp,
                    fontWeight = if (currentSize.roundToInt() == value) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentSize.roundToInt() == value)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.offset(x = xOffset - 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // App Drawer Columns label
        Text(
            text = "App Drawer Columns",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
