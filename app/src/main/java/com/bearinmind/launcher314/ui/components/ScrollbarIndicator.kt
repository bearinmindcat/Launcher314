package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
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
 * Shared, Compose-observable flag: is the user actively dragging the app-drawer
 * scrollbar thumb right now? Set by [LazyGridScrollbar], read by the drawer's
 * nested-scroll connection in LauncherWithDrawer.
 *
 * Why: grabbing the scrollbar can leave the drawer's `isDrawerDragging` flag
 * stuck TRUE — a list overscroll starts a drawer-close drag, then the scrollbar
 * steals the touch so the list never fires onPreFling/onPostFling to reset it.
 * The stuck flag then makes the nested-scroll intercept EVERY gesture, freezing
 * home (the "swipes do nothing after using the scrollbar" bug). This flag lets
 * the drawer (a) refuse to start a close-drag while the scrollbar owns the touch
 * and (b) clear a leaked one the instant the scrollbar is grabbed.
 */
object DrawerScrollbarState {
    var isActive by mutableStateOf(false)
}

/**
 * SYNCHRONOUS "is the drawer list at the top?" check, read by the drawer's
 * close-gesture controller when a gesture starts (Launcher3's
 * shouldContainerScroll reads the RecyclerView position per touch event the
 * same way). A provider on purpose: publishing a Boolean through a
 * snapshotFlow collector lagged during fast repeated swipes, refusing closes
 * until the user paused.
 */
object DrawerListState {
    var isAtTopProvider: () -> Boolean = { true }
    /** True while an open drawer folder is scrolled down, so a swipe-down scrolls it back instead of closing the drawer (issue #114). */
    var folderCanScrollUp: () -> Boolean = { false }
    fun isAtTop(): Boolean = isAtTopProvider() && !folderCanScrollUp()
}

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
        // pointerInput(Unit) keeps its first lambda, so it read the scroll position from first composition forever; read live values instead.
        val liveScrollOffset by rememberUpdatedState(scrollOffset)
        val liveScrollSpace by rememberUpdatedState(availableScrollSpace)
        val liveMaxIndex by rememberUpdatedState((totalItemsCount - visibleItemsCount).coerceAtLeast(0))
        val liveTotal by rememberUpdatedState(totalItemsCount)
        var dragThumbOffset by remember { mutableFloatStateOf(0f) }

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
                        onDragStart = { isThumbSelected = true; DrawerScrollbarState.isActive = true; dragThumbOffset = liveScrollOffset },
                        onDragEnd = { isThumbSelected = false; DrawerScrollbarState.isActive = false },
                        onDragCancel = { isThumbSelected = false; DrawerScrollbarState.isActive = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()

                            // Thumb follows the finger (accumulated), so whole-item rounding can't stall it
                            val dragRatio = if (liveScrollSpace > 0) dragAmount / liveScrollSpace else 0f
                            dragThumbOffset = (dragThumbOffset + dragRatio).coerceIn(0f, 1f)

                            // Calculate target item index
                            val targetIndex = (dragThumbOffset * liveMaxIndex).toInt()
                                .coerceIn(0, (liveTotal - 1).coerceAtLeast(0))

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
                            DrawerScrollbarState.isActive = true
                            tryAwaitRelease()
                            isThumbSelected = false
                            DrawerScrollbarState.isActive = false
                        }
                    )
                }
        )
    }
}

/**
 * Scrollbar for a plain scrolling container (Column/Row with verticalScroll),
 * i.e. one backed by a [ScrollState] rather than a LazyGridState. Same look as
 * [LazyGridScrollbar]: thin rounded thumb on the right, auto-hide, draggable.
 * Renders nothing when there's nothing to scroll.
 */
@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.White.copy(alpha = 0.3f),
    thumbSelectedColor: Color = Color.White.copy(alpha = 0.8f),
    thumbWidth: Dp = 4.dp,
    thumbMinHeight: Dp = 24.dp,
    hideDelayMillis: Int = 1200,
    alwaysShow: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val maxScroll = scrollState.maxValue
    // Content fits — nothing to scroll, so no scrollbar.
    if (maxScroll <= 0) return

    var isThumbSelected by remember { mutableStateOf(false) }
    val isScrolling = scrollState.isScrollInProgress
    var isVisible by remember { mutableStateOf(alwaysShow) }
    LaunchedEffect(isScrolling, isThumbSelected, alwaysShow) {
        if (isScrolling || isThumbSelected || alwaysShow) {
            isVisible = true
        } else {
            delay(hideDelayMillis.toLong())
            isVisible = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible || alwaysShow) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha"
    )
    val isActive = isScrolling || isThumbSelected
    val animatedThumbColor by animateColorAsState(
        targetValue = if (isActive) thumbSelectedColor else thumbColor,
        animationSpec = tween(durationMillis = if (isActive) 100 else 500),
        label = "thumbColor"
    )

    BoxWithConstraints(modifier = modifier.width(thumbWidth)) {
        val trackPx = with(density) { maxHeight.toPx() }
        val thumbMinHeightPx = with(density) { thumbMinHeight.toPx() }
        // Thumb size proportional to viewport / content (viewport ≈ track height).
        val contentPx = trackPx + maxScroll
        val thumbHeightPx = (trackPx * (trackPx / contentPx)).coerceAtLeast(thumbMinHeightPx)
        val available = (trackPx - thumbHeightPx).coerceAtLeast(0f)
        val scrollFraction = (scrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
        val thumbHeightDp = with(density) { thumbHeightPx.toDp() }
        val thumbOffsetDp = with(density) { (scrollFraction * available).toDp() }
        val liveAvailable by rememberUpdatedState(available) // pointerInput(Unit) keeps its first lambda

        Box(
            modifier = Modifier
                .width(thumbWidth)
                .height(thumbHeightDp)
                .align(Alignment.TopEnd)
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
                            // Relative to the live scroll position (a captured fraction snapped it back to the start).
                            if (liveAvailable > 0f) scrollState.dispatchRawDelta(dragAmount / liveAvailable * scrollState.maxValue)
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
