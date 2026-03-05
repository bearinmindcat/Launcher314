package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom scrollbar indicator for LazyVerticalGrid.
 * Styled like Einstein Launcher's scrollbar.
 *
 * Features:
 * - Thin rounded thumb
 * - Right-side positioning
 * - Auto-hide after inactivity
 * - Draggable for quick scrolling
 * - Proportional thumb size based on content
 */
@Composable
fun LazyGridScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.White.copy(alpha = 0.5f),
    thumbSelectedColor: Color = Color.White.copy(alpha = 0.8f),
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    thumbWidth: Dp = 4.dp,
    thumbMinHeight: Dp = 48.dp,
    scrollbarPadding: Dp = 4.dp,
    hideDelayMillis: Int = 1500,
    alwaysShow: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Track if user is interacting with scrollbar
    var isThumbSelected by remember { mutableStateOf(false) }

    // Track if scrollbar should be visible
    var isVisible by remember { mutableStateOf(alwaysShow) }

    // Track scroll activity for auto-hide
    val isScrolling = gridState.isScrollInProgress

    // Show scrollbar when scrolling or selected
    LaunchedEffect(isScrolling, isThumbSelected, alwaysShow) {
        if (isScrolling || isThumbSelected || alwaysShow) {
            isVisible = true
        } else {
            delay(hideDelayMillis.toLong())
            isVisible = false
        }
    }

    // Animate visibility
    val alpha by animateFloatAsState(
        targetValue = if (isVisible || alwaysShow) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha"
    )

    // Calculate scroll position
    val layoutInfo = gridState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsCount = layoutInfo.visibleItemsInfo.size

    // Don't render if no content to scroll (but still take up space)
    if (totalItemsCount == 0) {
        Box(modifier = modifier.width(thumbWidth + scrollbarPadding * 2))
        return
    }

    // Calculate scroll offset (0 to 1) - use precise calculation with item offset
    val firstVisibleItemInfo = layoutInfo.visibleItemsInfo.firstOrNull()
    val firstVisibleIndex = firstVisibleItemInfo?.index ?: 0
    val firstVisibleItemOffset = firstVisibleItemInfo?.offset?.y ?: 0

    // Calculate fractional scroll position for smooth movement
    val scrollableItems = (totalItemsCount - visibleItemsCount).coerceAtLeast(1)
    val itemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 1
    val fractionalOffset = if (itemHeight > 0) {
        -firstVisibleItemOffset.toFloat() / itemHeight.toFloat()
    } else {
        0f
    }
    val scrollOffset = ((firstVisibleIndex + fractionalOffset) / scrollableItems.toFloat())
        .coerceIn(0f, 1f)

    // Animate thumb color - light up when scrolling OR when thumb is selected
    val isActive = isScrolling || isThumbSelected
    val animatedThumbColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) thumbSelectedColor else thumbColor,
        animationSpec = tween(durationMillis = if (isActive) 100 else 500),  // Quick light up, slow fade
        label = "thumbColor"
    )

    BoxWithConstraints(
        modifier = modifier
            .width(thumbWidth + scrollbarPadding * 2)
    ) {
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val thumbMinHeightPx = with(density) { thumbMinHeight.toPx() }

        // FIXED thumb height - use the minimum height parameter directly
        val thumbHeightDp = thumbMinHeight

        // Calculate thumb position - smooth movement based on scroll offset
        val availableScrollSpace = maxHeightPx - thumbMinHeightPx
        val thumbOffsetPx = scrollOffset * availableScrollSpace
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }

        // Track background
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(thumbWidth)
                .align(Alignment.CenterEnd)
                .padding(end = scrollbarPadding)
                .alpha(alpha)
                .clip(RoundedCornerShape(thumbWidth / 2))
                .background(trackColor)
        )

        // Thumb - FIXED SIZE, only position changes
        Box(
            modifier = Modifier
                .width(thumbWidth)
                .height(thumbHeightDp)
                .align(Alignment.TopEnd)
                .padding(end = scrollbarPadding)
                .offset(y = thumbOffsetDp)
                .alpha(alpha)
                .clip(RoundedCornerShape(thumbWidth / 2))
                .background(animatedThumbColor)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isThumbSelected = true },
                        onDragEnd = { isThumbSelected = false },
                        onDragCancel = { isThumbSelected = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()

                            // Calculate new scroll position based on drag
                            val dragRatio = if (availableScrollSpace > 0) dragAmount / availableScrollSpace else 0f
                            val newScrollOffset = (scrollOffset + dragRatio).coerceIn(0f, 1f)

                            // Calculate target item index
                            val maxScrollIndex = (totalItemsCount - visibleItemsCount).coerceAtLeast(0)
                            val targetIndex = (newScrollOffset * maxScrollIndex).toInt()
                                .coerceIn(0, totalItemsCount - 1)

                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isThumbSelected = true
                            tryAwaitRelease()
                            isThumbSelected = false
                        }
                    )
                }
        )
    }
}

/**
 * Simplified scrollbar that just shows position without drag functionality.
 * Lighter weight alternative.
 */
@Composable
fun SimpleScrollIndicator(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.White.copy(alpha = 0.4f),
    thumbWidth: Dp = 3.dp,
    thumbMinHeight: Dp = 40.dp,
    hideDelayMillis: Int = 1500
) {
    var isVisible by remember { mutableStateOf(false) }
    val isScrolling = gridState.isScrollInProgress

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            isVisible = true
        } else {
            delay(hideDelayMillis.toLong())
            isVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorAlpha"
    )

    val layoutInfo = gridState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsCount = layoutInfo.visibleItemsInfo.size

    if (totalItemsCount == 0) return

    val thumbSizeRatio = (visibleItemsCount.toFloat() / totalItemsCount.toFloat())
        .coerceIn(0.1f, 1f)

    val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val scrollOffset = if (totalItemsCount > visibleItemsCount) {
        firstVisibleIndex.toFloat() / (totalItemsCount - visibleItemsCount).toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(thumbWidth + 8.dp)
            .alpha(alpha)
    ) {
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val thumbMinHeightPx = with(LocalDensity.current) { thumbMinHeight.toPx() }

        val thumbHeightPx = (maxHeightPx * thumbSizeRatio).coerceAtLeast(thumbMinHeightPx)
        val thumbHeightDp = with(LocalDensity.current) { thumbHeightPx.toDp() }

        val availableScrollSpace = maxHeightPx - thumbHeightPx
        val thumbOffsetPx = scrollOffset * availableScrollSpace
        val thumbOffsetDp = with(LocalDensity.current) { thumbOffsetPx.toDp() }

        Box(
            modifier = Modifier
                .width(thumbWidth)
                .height(thumbHeightDp)
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
                .offset(y = thumbOffsetDp)
                .clip(RoundedCornerShape(thumbWidth / 2))
                .background(thumbColor)
        )
    }
}
