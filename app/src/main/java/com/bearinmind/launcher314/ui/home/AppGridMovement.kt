package com.bearinmind.launcher314.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Folder
import com.bearinmind.launcher314.helpers.getIconShape
import com.bearinmind.launcher314.helpers.getShapedExpDir
import com.bearinmind.launcher314.helpers.FontManager
import com.bearinmind.launcher314.helpers.generateBgTintedIcon
import com.bearinmind.launcher314.helpers.generateShapedBgTintedIcon
import com.bearinmind.launcher314.helpers.getGlobalShapedDir
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.parseBlendMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.ui.components.GridCellHoverIndicator
import com.bearinmind.launcher314.ui.components.DockSlotHoverIndicator
import com.bearinmind.launcher314.ui.components.rememberHoverAlpha
import com.bearinmind.launcher314.ui.components.HoverIndicatorColor
import com.bearinmind.launcher314.data.AppCustomization
import com.bearinmind.launcher314.data.HomeAppInfo
import com.bearinmind.launcher314.data.HomeGridCell
import com.bearinmind.launcher314.data.DockFolder
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import java.io.File
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * AppGridMovement.kt - Contains draggable grid cell and dock slot composables
 * for the launcher home screen drag and drop functionality.
 *
 * Key feature: Uses checkIsDragOwner lambda to evaluate ownership dynamically
 * at call time, preventing multiple handlers from processing the same drag.
 */

/**
 * DraggableGridCell - A single cell in the home screen grid
 * Supports tap, long-press context menu, and drag-and-drop
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableGridCell(
    cell: HomeGridCell,
    index: Int,
    iconSize: Int,
    gridColumns: Int,
    gridRows: Int,
    isEditMode: Boolean,
    isDragging: Boolean,
    checkIsDragOwner: () -> Boolean, // Lambda to check ownership dynamically at call time
    isAnyItemDragging: Boolean,
    isDropTarget: Boolean,
    isHovered: Boolean,
    isValidDropTarget: Boolean = true, // Whether this is a valid drop target (true = blue, false = red)
    isHoverTargetValid: Boolean = true, // When dragging, is the current hover position valid? (for icon tint)
    dragOffset: Offset,
    isWidgetDragging: Boolean = false, // Skip gesture detection when a widget is being dragged
    isAnyDragActive: () -> Boolean = { false }, // Dynamic check: is any drag in progress? Evaluated at call time inside gesture handlers
    // Proportional sizing params (defaults match 360dp phone with 4 columns)
    markerHalfSizeParam: Dp = 6.dp,
    plusMarkerSize: Dp = 12.dp,
    plusMarkerFontSize: TextUnit = 10.sp,
    appNameFontSize: TextUnit = 12.sp,
    appNameFontFamily: FontFamily? = null,
    iconTextSpacer: Dp = 4.dp,
    hoverCornerRadius: Dp = 12.dp,
    onPositioned: (Offset, IntSize) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    onLongPress: (Offset) -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo: () -> Unit,
    onCustomize: () -> Unit = {},
    onSelectToggle: () -> Unit = {},
    isSelected: Boolean = false,
    selectionModeActive: Boolean = false,
    onWidgetRemove: () -> Unit = {},
    isCustomizing: Boolean = false, // When true, keep icon scaled up while customize dialog is open
    globalIconSizePercent: Float = 100f, // Global icon size for absolute per-app scale
    globalIconShape: String? = null, // Global icon shape (EXP method) applied when no per-app shape
    globalIconBgColor: Int? = null, // Global icon background color (drawn behind icon within shape)
    removeLabel: String = "Remove from home",
    homeFolders: List<com.bearinmind.launcher314.data.HomeFolder> = emptyList(),
    onAddToFolder: (com.bearinmind.launcher314.data.HomeFolder) -> Unit = {},
    onCreateFolder: () -> Unit = {},
    onBulkRemove: () -> Unit = {},
    selectedCount: Int = 0,
    folderPreviewDraggedIconPath: String? = null, // When non-null, animates this cell into a folder preview
    isReceivingDrop: Boolean = false // When true, plays a pulse scale animation on the folder cell
) {
    val hapticFeedback = rememberHapticFeedback()
    // Use rememberUpdatedState so pointerInput always calls the latest callbacks
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentSelectionModeActive by rememberUpdatedState(selectionModeActive)
    val currentSelectedCount by rememberUpdatedState(selectedCount)
    var showContextMenu by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var cellPosition by remember { mutableStateOf(Offset.Zero) }
    var cellIntSize by remember { mutableStateOf(IntSize.Zero) }

    // Track if we're in a potential drag state (long press started but not yet dragging)
    var isLongPressActive by remember { mutableStateOf(false) }

    // Animated alpha for empty cell indicator - uses TileColorOnHover.kt
    val emptyCellAlpha = rememberHoverAlpha(isHovered = isHovered)

    // Calculate cell position in grid for marker logic
    val column = index % gridColumns
    val row = index / gridColumns
    // Show marker at bottom-right corner only if this intersection is interior
    val showBottomRightMarker = (row < gridRows - 1) && (column < gridColumns - 1)
    val markerHalfSize = markerHalfSizeParam

    // Outer container - NO scaling here, so "+" markers stay in place
    // clip = false allows text/icons to overflow cell bounds on tablets
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
            .onGloballyPositioned { coordinates ->
                cellPosition = coordinates.positionInRoot()
                cellIntSize = coordinates.size
                onPositioned(cellPosition, cellIntSize)
            },
        contentAlignment = Alignment.Center
    ) {
        // Empty cell indicator for cells with apps being dragged
        // Shows when hovering over the original position (stays in place, doesn't move with drag)
        // Uses GridCellHoverIndicator from TileColorOnHover.kt
        if (isDragging && isHovered) {
            GridCellHoverIndicator(isHovered = true, markerHalfSize = markerHalfSize, cornerRadius = hoverCornerRadius)
        }

        // "+" marker - positioned at corner, fades in/out when dragging an app
        val markerAlpha by animateFloatAsState(
            targetValue = if (isAnyItemDragging && showBottomRightMarker) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "markerAlpha"
        )
        if (markerAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = markerHalfSize, y = markerHalfSize)
                    .size(plusMarkerSize)
                    .graphicsLayer { alpha = markerAlpha },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = plusMarkerFontSize,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Content container - when dragging, app is rendered in overlay layer (LauncherScreen)
        // so we hide it here to prevent duplicate rendering
        // clip = false allows text to overflow cell bounds
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = false
                    // Hide content when dragging - it's rendered in overlay for proper z-ordering
                    // Use direct 0f/1f (no animation) to avoid one-frame flicker on drop
                    alpha = if (isDragging && (cell is HomeGridCell.App || cell is HomeGridCell.Folder)) 0f else 1f
                },
            contentAlignment = Alignment.Center
        ) {

        when (cell) {
            is HomeGridCell.Empty -> {
                // Empty cell - supports long-press for launcher settings menu
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPosition = down.position

                                // IMPORTANT: Check if touch is within this cell's bounds
                                if (startPosition.x < 0 || startPosition.x > size.width ||
                                    startPosition.y < 0 || startPosition.y > size.height) {
                                    return@awaitEachGesture
                                }

                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null) {
                                    // Skip if another drag is active
                                    if (isAnyDragActive()) return@awaitEachGesture
                                    // Calculate position relative to screen
                                    val touchPosition = cellPosition + longPress.position
                                    hapticFeedback.performLongPress()
                                    onLongPress(touchPosition)
                                } else {
                                    // Tap on empty cell
                                    onTap()
                                }
                            }
                        }
                ) {
                    // Empty cell hover indicator - uses TileColorOnHover.kt
                    GridCellHoverIndicator(isHovered = isHovered, isValidDropTarget = isValidDropTarget, markerHalfSize = markerHalfSize, cornerRadius = hoverCornerRadius)
                }
            }

            is HomeGridCell.App -> {
                // Animate icon scale when context menu is shown or when dragging (like app drawer)
                // Use snap() when ending drag to prevent double animation stutter
                val isScaledUp = showContextMenu || showBulkMenu || isDragging || isCustomizing || isSelected
                val animatedIconScale by animateFloatAsState(
                    targetValue = if (isScaledUp) 1.265f else 1f,
                    animationSpec = if (isScaledUp) tween(durationMillis = 150) else snap(),
                    label = "iconScale"
                )
                // Force 1f immediately when not in active interaction —
                // snap() can lag one frame causing a visible pulse of the larger icon
                val iconScale = if (isScaledUp) animatedIconScale else 1f
                var iconBoundsInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

                // Hide label when scaled up (context menu, dragging, customizing, or selected)
                val hideLabel = showContextMenu || isDragging || isCustomizing || isSelected
                val labelAlpha by animateFloatAsState(
                    targetValue = if (showContextMenu || isCustomizing || isSelected) 0f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "labelAlpha"
                )

                // Track if long press is active for visual feedback
                var isLongPressActive by remember { mutableStateOf(false) }
                var isFingerDown by remember { mutableStateOf(false) }

                // Dark press + flash overlay
                var flashOverlay by remember { mutableStateOf(false) }
                val flashAlpha by animateFloatAsState(
                    targetValue = if (flashOverlay) 0.4f else 0f,
                    animationSpec = if (flashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
                    label = "flash_alpha",
                    finishedListener = { if (flashOverlay) flashOverlay = false }
                )
                val overlayAlpha = maxOf(if (isFingerDown) 0.25f else 0f, flashAlpha)

                // Gesture handler on full cell area (not just icon/text)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isWidgetDragging) {
                            // CRITICAL: Skip ALL gesture processing when a widget is being dragged
                            // This allows the WidgetDragOverlay to receive touch events
                            if (isWidgetDragging) return@pointerInput

                            // Custom gesture handler inspired by Fossify Launcher
                            // Handles: tap, long press (show menu), long press + drag
                            // Touch area is the entire grid cell
                            val touchSlop = viewConfiguration.touchSlop

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPosition = down.position

                                // IMPORTANT: Check if touch is within this cell's bounds
                                // Since we use requireUnconsumed = false, we see ALL touches
                                // Only process if touch is actually in this cell
                                if (startPosition.x < 0 || startPosition.x > size.width ||
                                    startPosition.y < 0 || startPosition.y > size.height) {
                                    return@awaitEachGesture
                                }

                                isFingerDown = true
                                var dragStarted = false
                                var lastDragPosition = Offset.Zero

                                // Wait for long press (returns null if moved/cancelled/released early)
                                val longPress = awaitLongPressOrCancellation(down.id)

                                if (longPress != null) {
                                    // CRITICAL: Skip if another drag is already active.
                                    // awaitLongPressOrCancellation fires immediately when pointer
                                    // has been down for 400ms+ (from original cell's long press).
                                    // Without this check, the popup steals focus and causes icon flying.
                                    if (isAnyDragActive()) return@awaitEachGesture

                                    // Long press triggered - show menu immediately while holding
                                    isLongPressActive = true
                                    if (currentSelectionModeActive && currentSelectedCount > 0) {
                                        showBulkMenu = true
                                    } else {
                                        showContextMenu = true
                                    }
                                    flashOverlay = true
                                    hapticFeedback.performLongPress()

                                    // Phase 2: Wait for movement (drag) or release (menu stays)
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break

                                            if (change.pressed) {
                                                val dx = change.position.x - startPosition.x
                                                val dy = change.position.y - startPosition.y
                                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                                if (distance > touchSlop && !dragStarted) {
                                                    // Movement after long press = start drag, hide menu
                                                    dragStarted = true
                                                    showContextMenu = false
                                                    lastDragPosition = change.position
                                                    currentOnDragStart()
                                                }

                                                // CRITICAL: Only process drag if this handler is the actual drag owner
                                                // This prevents multiple handlers from adding to dragOffset
                                                // Use checkIsDragOwner() to evaluate ownership at call time
                                                if (dragStarted && checkIsDragOwner()) {
                                                    // Use our own lastDragPosition instead of change.previousPosition
                                                    // to avoid delta jumps when context menu popup steals focus
                                                    val dragDelta = Offset(
                                                        change.position.x - lastDragPosition.x,
                                                        change.position.y - lastDragPosition.y
                                                    )
                                                    lastDragPosition = change.position
                                                    change.consume()
                                                    onDrag(dragDelta)
                                                }
                                            } else {
                                                // Finger released - only call onDragEnd if we own the drag
                                                if (dragStarted && checkIsDragOwner()) {
                                                    onDragEnd()
                                                }
                                                // Menu stays visible if not dragged (already shown)
                                                break
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (dragStarted && checkIsDragOwner()) onDragEnd()
                                    } finally {
                                        isLongPressActive = false
                                        isFingerDown = false
                                    }
                                } else {
                                    isFingerDown = false
                                    // Long press cancelled - check if it was a tap (quick release)
                                    // awaitLongPressOrCancellation returns null for tap, so handle it
                                    val upEvent = currentEvent.changes.firstOrNull()
                                    if (upEvent != null && !upEvent.pressed) {
                                        onTap()
                                    }
                                }
                            }
                        }
                ) {
                    // Folder creation preview animation progress
                    // Remember last non-null icon path so fade-out can still render the preview
                    var lastDraggedIconPath by remember { mutableStateOf<String?>(null) }
                    if (folderPreviewDraggedIconPath != null) {
                        lastDraggedIconPath = folderPreviewDraggedIconPath
                    }
                    val folderPreviewProgress by animateFloatAsState(
                        targetValue = if (folderPreviewDraggedIconPath != null) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "folderPreviewProgress",
                        finishedListener = { value ->
                            if (value == 0f) lastDraggedIconPath = null
                        }
                    )
                    val effectiveDraggedIconPath = folderPreviewDraggedIconPath ?: lastDraggedIconPath

                    // Show hover indicator UNDERNEATH the app when something is dragged over this cell
                    // Only show blue (valid) indicator — invalid targets show red icon tint instead
                    // Suppress immediately when folder preview is about to show (no grey flicker)
                    if (isHovered && !isDragging && isValidDropTarget && folderPreviewDraggedIconPath == null && folderPreviewProgress == 0f) {
                        GridCellHoverIndicator(
                            isHovered = true,
                            isValidDropTarget = isValidDropTarget,
                            markerHalfSize = markerHalfSize,
                            cornerRadius = hoverCornerRadius
                        )
                    }

                    // App content centered within the same area as empty cell background
                    // Hidden when being dragged (overlay renders the app instead)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(markerHalfSize)
                            .graphicsLayer {
                                clip = false
                                alpha = if (isDragging) 0f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Normal app icon + label (fades out during folder preview)
                        Column(
                            modifier = Modifier
                                .wrapContentHeight(unbounded = true) // Measure at intrinsic height even if cell is smaller
                                .graphicsLayer {
                                    clip = false
                                    alpha = 1f - folderPreviewProgress
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val hasCustomIcon = cell.appInfo.customization?.customIconPath?.let { File(it).exists() } == true
                            val hasShapeExp = cell.appInfo.customization?.iconShapeExp != null
                            val hasPerAppShape = cell.appInfo.customization?.iconShape != null
                            val gridContext = LocalContext.current
                            val iconModelPath = if (hasCustomIcon) {
                                cell.appInfo.customization!!.customIconPath!!
                            } else if (hasShapeExp) {
                                File(getShapedExpDir(gridContext), "${cell.appInfo.packageName}.png").let {
                                    if (it.exists()) it.absolutePath else cell.appInfo.iconPath
                                }
                            } else if (!hasPerAppShape && globalIconShape != null) {
                                // Global shape fallback (EXP method)
                                File(getGlobalShapedDir(gridContext), "${cell.appInfo.packageName}.png").let {
                                    if (it.exists()) it.absolutePath
                                    else try { getOrGenerateGlobalShapedIcon(gridContext, cell.appInfo.packageName, globalIconShape!!) } catch (_: Exception) { cell.appInfo.iconPath }
                                }
                            } else cell.appInfo.iconPath
                            // Check for background-only tinted icon
                            val hasBgTint = cell.appInfo.customization?.iconTintBackgroundOnly == true && cell.appInfo.customization?.iconTintColor != null
                            val hasAnyShape = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
                            val gridEffectiveShape = cell.appInfo.customization?.iconShapeExp ?: cell.appInfo.customization?.iconShape ?: globalIconShape
                            val finalIconModelPath = if (hasBgTint && !hasCustomIcon) {
                                val tintColor = cell.appInfo.customization?.iconTintColor?.toInt() ?: 0
                                val tintAlpha = (cell.appInfo.customization?.iconTintIntensity ?: 100) / 100f
                                try {
                                    if (hasAnyShape && gridEffectiveShape != null) {
                                        generateShapedBgTintedIcon(gridContext, cell.appInfo.packageName, gridEffectiveShape, tintColor, tintAlpha)
                                    } else {
                                        generateBgTintedIcon(gridContext, cell.appInfo.packageName, tintColor, tintAlpha)
                                    }
                                } catch (_: Exception) { iconModelPath }
                            } else iconModelPath
                            val hasAnyExpShape = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
                            val iconClipShape = if (hasCustomIcon) {
                                getIconShape(cell.appInfo.customization?.iconShapeExp ?: cell.appInfo.customization?.iconShape ?: globalIconShape)
                            } else if (!hasAnyExpShape) getIconShape(cell.appInfo.customization?.iconShape) else null
                            val customTintFilter = if (hasBgTint) null else cell.appInfo.customization?.iconTintColor?.let { tintColor ->
                                val intensity = (cell.appInfo.customization?.iconTintIntensity ?: 100) / 100f
                                ColorFilter.tint(Color(tintColor.toInt()).copy(alpha = intensity), parseBlendMode(cell.appInfo.customization?.iconTintBlendMode))
                            }
                            val perAppSizePercent = cell.appInfo.customization?.iconSizePercent ?: globalIconSizePercent.toInt()
                            val perAppIconSizeDp = (iconSize * perAppSizePercent / globalIconSizePercent.toFloat()).dp
                            // When bg color is set, generate icon with user color as bg layer
                            val useBgColorIcon = globalIconBgColor != null && !hasCustomIcon
                            val bgColorEffectiveShape = if (useBgColorIcon) {
                                cell.appInfo.customization?.iconShapeExp
                                    ?: cell.appInfo.customization?.iconShape
                                    ?: globalIconShape
                            } else null
                            val displayIconPath = if (useBgColorIcon && bgColorEffectiveShape != null) {
                                try { getOrGenerateBgColorShapedIcon(gridContext, cell.appInfo.packageName, bgColorEffectiveShape, globalIconBgColor!!) }
                                catch (_: Exception) { finalIconModelPath }
                            } else finalIconModelPath
                            val isBgColorIcon = displayIconPath != finalIconModelPath
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        // Always use the final target scale (1.265f) so popup doesn't stutter during animation
                                        val targetScale = 1.265f
                                        val pos = coords.positionInRoot()
                                        val w = coords.size.width * targetScale
                                        val h = coords.size.height * targetScale
                                        val offsetX = (coords.size.width - w) / 2f
                                        val offsetY = (coords.size.height - h) / 2f
                                        iconBoundsInRoot = androidx.compose.ui.geometry.Rect(
                                            pos.x + offsetX, pos.y + offsetY,
                                            pos.x + offsetX + w, pos.y + offsetY + h
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        clip = false
                                    }
                            ) {
                                AsyncImage(
                                    model = File(displayIconPath),
                                    contentDescription = cell.appInfo.name,
                                    contentScale = if (isBgColorIcon) ContentScale.Fit else if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                                    colorFilter = if ((isDragging && !isHoverTargetValid) || (isHovered && !isValidDropTarget)) {
                                        ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                                    } else customTintFilter,
                                    modifier = Modifier
                                        .size(perAppIconSizeDp)
                                        .then(if (!isBgColorIcon && iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                                )

                                // Dark overlay (press + flash) — uses icon silhouette to match exact shape
                                if (overlayAlpha > 0f) {
                                    AsyncImage(
                                        model = File(finalIconModelPath),
                                        contentDescription = null,
                                        contentScale = if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                                        colorFilter = ColorFilter.tint(Color.Black, androidx.compose.ui.graphics.BlendMode.SrcIn),
                                        modifier = Modifier
                                            .size(perAppIconSizeDp)
                                            .then(if (iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                                            .graphicsLayer {
                                                alpha = overlayAlpha
                                            }
                                    )
                                }

                                // Selection circle overlay — shows on all icons when in selection mode
                                if (selectionModeActive) {
                                    val circleSize = (iconSize * 0.42f).dp
                                    val checkmarkSize = (iconSize * 0.27f).dp
                                    val circleBorder = (iconSize * 0.018f).dp
                                    val circleOffset = (iconSize * 0.083f).dp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = circleOffset, y = -circleOffset)
                                            .size(circleSize)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.Black.copy(alpha = 0.35f)
                                            )
                                            .border(
                                                width = circleBorder,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(checkmarkSize),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Label: respect per-app customization (hide or rename)
                            // Always render to keep icon position stable; fade alpha when hidden
                            val customization = cell.appInfo.customization
                            val labelHidden = customization?.hideLabel == true
                            val hideLabelAlpha by animateFloatAsState(
                                targetValue = if (labelHidden) 0f else 1f,
                                animationSpec = tween(durationMillis = 250),
                                label = "hideLabelAlpha"
                            )
                            val displayLabel = customization?.customLabel?.takeIf { it.isNotEmpty() }
                                ?: cell.appInfo.name

                            Spacer(modifier = Modifier.height(iconTextSpacer))

                            val perAppFontSize = customization?.iconTextSizePercent?.let { 12.sp * it / 100f } ?: appNameFontSize
                            val perAppFontFamily = customization?.labelFontId?.let { id ->
                                FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                    ?: FontManager.getImportedFonts(gridContext).find { it.id == id }?.fontFamily
                            } ?: appNameFontFamily ?: FontFamily.Default
                            Text(
                                text = displayLabel,
                                fontSize = perAppFontSize,
                                fontFamily = perAppFontFamily,
                                color = if ((isDragging && !isHoverTargetValid) || (isHovered && !isValidDropTarget))
                                    Color(0xFFFF6B6B) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = if (isDragging) 0f else labelAlpha * hideLabelAlpha },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }

                        // Folder creation preview (fades in when dragging app over this app)
                        if (folderPreviewProgress > 0f && effectiveDraggedIconPath != null) {
                            val folderBoxSize = iconSize.dp
                            val folderCornerRadius = (iconSize * 0.29f).dp
                            val previewScale = 0.85f + 0.15f * folderPreviewProgress

                            Column(
                                modifier = Modifier
                                    .wrapContentHeight(unbounded = true)
                                    .graphicsLayer {
                                        clip = false
                                        alpha = folderPreviewProgress
                                        scaleX = previewScale
                                        scaleY = previewScale
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(folderBoxSize)
                                        .clip(getIconShape(globalIconShape) ?: RoundedCornerShape(folderCornerRadius))
                                        .background(Color(0xFF1A1A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val padding = folderBoxSize * 0.08f
                                    val spacing = folderBoxSize * 0.04f
                                    val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2

                                    Column(
                                        modifier = Modifier.padding(padding),
                                        verticalArrangement = Arrangement.spacedBy(spacing)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            // Existing app icon (top-left)
                                            AsyncImage(
                                                model = File(cell.appInfo.iconPath),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(miniIconSize)
                                                    .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                            )
                                            // Dragged app icon (top-right)
                                            AsyncImage(
                                                model = File(effectiveDraggedIconPath),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(miniIconSize)
                                                    .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            // Empty slots (bottom row)
                                            Spacer(modifier = Modifier.size(miniIconSize))
                                            Spacer(modifier = Modifier.size(miniIconSize))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(iconTextSpacer))

                                Text(
                                    text = "Folder",
                                    fontSize = appNameFontSize,
                                    fontFamily = appNameFontFamily ?: FontFamily.Default,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black,
                                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }
                    }

                    // "+" markers are rendered in outer container, not here

                    // Context menu (shown on long press, like app drawer)
                    AnimatedPopup(
                            visible = showContextMenu && iconBoundsInRoot != androidx.compose.ui.geometry.Rect.Zero,
                            onDismissRequest = { showContextMenu = false },
                            iconBoundsInRoot = iconBoundsInRoot
                        ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = cell.appInfo.name,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 28.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    showContextMenu = false
                                                    onAppInfo()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = "App info"
                                            )
                                        }
                                    }
                                    Divider()

                                    // 1. Remove from home
                                    DropdownMenuItem(
                                        text = { Text(removeLabel) },
                                        onClick = {
                                            showContextMenu = false
                                            onRemove()
                                        },
                                        leadingIcon = { HomeOffIcon() }
                                    )

                                    // 2. Select
                                    DropdownMenuItem(
                                        text = { Text(if (isSelected) "Deselect" else "Select") },
                                        onClick = {
                                            showContextMenu = false
                                            onSelectToggle()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Outlined.Check else Icons.Outlined.Circle,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    // 3. Uninstall
                                    DropdownMenuItem(
                                        text = { Text("Uninstall") },
                                        onClick = {
                                            showContextMenu = false
                                            onUninstall()
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                    )

                                    // 4. Customize
                                    DropdownMenuItem(
                                        text = { Text("Customize") },
                                        onClick = {
                                            showContextMenu = false
                                            onCustomize()
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                    )

                                    // 5. Folder
                                    var folderExpanded by remember { mutableStateOf(false) }
                                    DropdownMenuItem(
                                        text = { Text("Folder") },
                                        onClick = { folderExpanded = !folderExpanded },
                                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) }
                                    )
                                    AnimatedVisibility(
                                        visible = folderExpanded,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                                    ) {
                                        Column {
                                            DropdownMenuItem(
                                                text = { Text("Create folder") },
                                                onClick = {
                                                    showContextMenu = false
                                                    onCreateFolder()
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                            if (homeFolders.isNotEmpty()) {
                                                Text(
                                                    text = "Move to",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                                )
                                                homeFolders.forEach { folder ->
                                                    DropdownMenuItem(
                                                        text = { Text(folder.name) },
                                                        onClick = {
                                                            showContextMenu = false
                                                            onAddToFolder(folder)
                                                        },
                                                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                                        modifier = Modifier.padding(start = 16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                        }

                // Bulk action menu (shown when long-pressing a selected app in selection mode)
                AnimatedPopup(
                    visible = showBulkMenu && iconBoundsInRoot != androidx.compose.ui.geometry.Rect.Zero,
                    onDismissRequest = { showBulkMenu = false },
                    iconBoundsInRoot = iconBoundsInRoot
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "$selectedCount selected",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                    }
                    Divider()

                    DropdownMenuItem(
                        text = { Text("Remove from home") },
                        onClick = {
                            showBulkMenu = false
                            onBulkRemove()
                        },
                        leadingIcon = { HomeOffIcon() }
                    )

                    var bulkFolderExpanded by remember { mutableStateOf(false) }
                    DropdownMenuItem(
                        text = { Text("Folder") },
                        onClick = { bulkFolderExpanded = !bulkFolderExpanded },
                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) }
                    )
                    AnimatedVisibility(
                        visible = bulkFolderExpanded,
                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text("Create folder") },
                                onClick = {
                                    showBulkMenu = false
                                    onCreateFolder()
                                },
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            if (homeFolders.isNotEmpty()) {
                                Text(
                                    text = "Move to",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                )
                                homeFolders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            showBulkMenu = false
                                            onAddToFolder(folder)
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }

            is HomeGridCell.Folder -> {
                // Folder cell - shows 2x2 preview grid of app icons
                var showFolderRemoveConfirm by remember { mutableStateOf(false) }
                val isFolderScaledUp = showContextMenu || isDragging || showFolderRemoveConfirm
                val animatedFolderScale by animateFloatAsState(
                    targetValue = if (isFolderScaledUp) 1.265f else 1f,
                    animationSpec = if (isFolderScaledUp) tween(durationMillis = 150) else snap(),
                    label = "folderIconScale"
                )
                val iconScale = if (isFolderScaledUp) animatedFolderScale else 1f

                // Hide label only for THIS cell when it's being dragged or has context menu open
                val hideFolderLabel = showContextMenu || isDragging || showFolderRemoveConfirm
                val folderLabelAlpha by animateFloatAsState(
                    targetValue = if (showContextMenu || showFolderRemoveConfirm) 0f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "folderLabelAlpha"
                )

                // Dark press + flash overlay for folder
                var isFolderFingerDown by remember { mutableStateOf(false) }
                var folderFlashOverlay by remember { mutableStateOf(false) }
                val folderFlashAlpha by animateFloatAsState(
                    targetValue = if (folderFlashOverlay) 0.4f else 0f,
                    animationSpec = if (folderFlashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
                    label = "folder_flash_alpha",
                    finishedListener = { if (folderFlashOverlay) folderFlashOverlay = false }
                )
                val folderOverlayAlpha = maxOf(if (isFolderFingerDown) 0.25f else 0f, folderFlashAlpha)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isWidgetDragging) {
                            if (isWidgetDragging) return@pointerInput
                            val touchSlop = viewConfiguration.touchSlop

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPosition = down.position

                                if (startPosition.x < 0 || startPosition.x > size.width ||
                                    startPosition.y < 0 || startPosition.y > size.height) {
                                    return@awaitEachGesture
                                }

                                isFolderFingerDown = true
                                var dragStarted = false
                                var lastDragPosition = Offset.Zero
                                val longPress = awaitLongPressOrCancellation(down.id)

                                if (longPress != null) {
                                    // Skip if another drag is already active (prevents popup stealing focus)
                                    if (isAnyDragActive()) return@awaitEachGesture

                                    isLongPressActive = true
                                    showContextMenu = true
                                    folderFlashOverlay = true
                                    hapticFeedback.performLongPress()

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break

                                            if (change.pressed) {
                                                val dx = change.position.x - startPosition.x
                                                val dy = change.position.y - startPosition.y
                                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                                if (distance > touchSlop && !dragStarted) {
                                                    dragStarted = true
                                                    showContextMenu = false
                                                    lastDragPosition = change.position
                                                    currentOnDragStart()
                                                }

                                                if (dragStarted && checkIsDragOwner()) {
                                                    val dragDelta = Offset(
                                                        change.position.x - lastDragPosition.x,
                                                        change.position.y - lastDragPosition.y
                                                    )
                                                    lastDragPosition = change.position
                                                    change.consume()
                                                    onDrag(dragDelta)
                                                }
                                            } else {
                                                if (dragStarted && checkIsDragOwner()) {
                                                    onDragEnd()
                                                }
                                                break
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (dragStarted && checkIsDragOwner()) onDragEnd()
                                    } finally {
                                        isLongPressActive = false
                                        isFolderFingerDown = false
                                    }
                                } else {
                                    isFolderFingerDown = false
                                    val upEvent = currentEvent.changes.firstOrNull()
                                    if (upEvent != null && !upEvent.pressed) {
                                        onTap()
                                    }
                                }
                            }
                        }
                ) {
                    // Folder add preview animation — shows dragged app icon in next empty slot
                    // Set directly (not conditional) so it clears immediately on drop,
                    // preventing ghost image at the add slot
                    var lastFolderDraggedIconPath by remember { mutableStateOf<String?>(null) }
                    lastFolderDraggedIconPath = folderPreviewDraggedIconPath
                    val folderAddProgress by animateFloatAsState(
                        targetValue = if (folderPreviewDraggedIconPath != null) 1f else 0f,
                        // Fade in over 300ms, but snap to 0 instantly on drop to prevent ghost image
                        animationSpec = if (folderPreviewDraggedIconPath != null) tween(durationMillis = 300) else snap(),
                        label = "folderAddProgress",
                        finishedListener = { value ->
                            if (value == 0f) lastFolderDraggedIconPath = null
                        }
                    )
                    val effectiveFolderDraggedIconPath = folderPreviewDraggedIconPath ?: lastFolderDraggedIconPath

                    // Hover indicator — only show blue (valid), suppress red and folder add preview
                    if (isHovered && !isDragging && isValidDropTarget && folderPreviewDraggedIconPath == null && folderAddProgress == 0f) {
                        GridCellHoverIndicator(
                            isHovered = true,
                            isValidDropTarget = isValidDropTarget,
                            markerHalfSize = markerHalfSize,
                            cornerRadius = hoverCornerRadius
                        )
                    }

                    // Folder content centered
                    // Hidden when being dragged (overlay renders the folder instead)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(markerHalfSize)
                            .graphicsLayer {
                                clip = false
                                alpha = if (isDragging) 0f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle scale pulse when accepting a dragged app
                        val folderAcceptScale = if (folderAddProgress > 0f) {
                            1f + 0.08f * folderAddProgress
                        } else iconScale

                        // Receive animation: pulse from 1.0 → 1.1 → 1.0 when a drop lands on this folder
                        val receiveScale by animateFloatAsState(
                            targetValue = if (isReceivingDrop) 1.1f else 1f,
                            animationSpec = tween(durationMillis = 200),
                            label = "folderReceiveScale"
                        )
                        val combinedScale = folderAcceptScale * receiveScale

                        Column(
                            modifier = Modifier
                                .wrapContentHeight(unbounded = true)
                                .graphicsLayer { clip = false },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Folder preview - 2x2 grid of app icons in a rounded square
                            val folderBoxSize = iconSize.dp
                            val folderCornerRadius = (iconSize * 0.29f).dp
                            // Determine which slot index the dragged app would go into
                            val addSlotIndex = cell.previewApps.size.coerceAtMost(3)
                            // Red tint for mini icons when hovered by an invalid drop (e.g. folder on folder)
                            val folderInvalidTint = if (isHovered && !isValidDropTarget && !isDragging) {
                                ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                            } else null

                            Box(
                                modifier = Modifier
                                    .size(folderBoxSize)
                                    .graphicsLayer {
                                        scaleX = combinedScale
                                        scaleY = combinedScale
                                        clip = false
                                    }
                                    .clip(getIconShape(globalIconShape) ?: RoundedCornerShape(folderCornerRadius))
                                    .background(Color(0xFF1A1A1A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.previewApps.isNotEmpty()) {
                                    val padding = folderBoxSize * 0.08f
                                    val spacing = folderBoxSize * 0.04f
                                    val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2

                                    Column(
                                        modifier = Modifier.padding(padding),
                                        verticalArrangement = Arrangement.spacedBy(spacing)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            // Slot 0
                                            cell.previewApps.getOrNull(0)?.let { app ->
                                                AsyncImage(
                                                    model = File(app.iconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = folderInvalidTint,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                )
                                            } ?: if (addSlotIndex == 0 && folderAddProgress > 0f && effectiveFolderDraggedIconPath != null) {
                                                AsyncImage(
                                                    model = File(effectiveFolderDraggedIconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                        .graphicsLayer { alpha = folderAddProgress }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                            // Slot 1
                                            cell.previewApps.getOrNull(1)?.let { app ->
                                                AsyncImage(
                                                    model = File(app.iconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = folderInvalidTint,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                )
                                            } ?: if (addSlotIndex == 1 && folderAddProgress > 0f && effectiveFolderDraggedIconPath != null) {
                                                AsyncImage(
                                                    model = File(effectiveFolderDraggedIconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                        .graphicsLayer { alpha = folderAddProgress }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            // Slot 2
                                            cell.previewApps.getOrNull(2)?.let { app ->
                                                AsyncImage(
                                                    model = File(app.iconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = folderInvalidTint,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                )
                                            } ?: if (addSlotIndex == 2 && folderAddProgress > 0f && effectiveFolderDraggedIconPath != null) {
                                                AsyncImage(
                                                    model = File(effectiveFolderDraggedIconPath),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(miniIconSize)
                                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                        .graphicsLayer { alpha = folderAddProgress }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                            // Slot 3 — when all 4 slots occupied and hovering, crossfade to dragged app
                                            if (folderAddProgress > 0f && effectiveFolderDraggedIconPath != null && cell.previewApps.size >= 4) {
                                                // Crossfade: existing app fades out, dragged app fades in
                                                Box(modifier = Modifier.size(miniIconSize)) {
                                                    cell.previewApps.getOrNull(3)?.let { app ->
                                                        AsyncImage(
                                                            model = File(app.iconPath),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            colorFilter = folderInvalidTint,
                                                            modifier = Modifier
                                                                .size(miniIconSize)
                                                                .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                                .graphicsLayer { alpha = 1f - folderAddProgress }
                                                        )
                                                    }
                                                    AsyncImage(
                                                        model = File(effectiveFolderDraggedIconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .size(miniIconSize)
                                                            .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                            .graphicsLayer { alpha = folderAddProgress }
                                                    )
                                                }
                                            } else {
                                                cell.previewApps.getOrNull(3)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderInvalidTint,
                                                        modifier = Modifier
                                                            .size(miniIconSize)
                                                            .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: if (addSlotIndex == 3 && folderAddProgress > 0f && effectiveFolderDraggedIconPath != null) {
                                                    AsyncImage(
                                                        model = File(effectiveFolderDraggedIconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .size(miniIconSize)
                                                            .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                            .graphicsLayer { alpha = folderAddProgress }
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(miniIconSize))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Dark overlay (press + flash)
                                if (folderOverlayAlpha > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .graphicsLayer { alpha = folderOverlayAlpha }
                                            .background(Color.Black)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(iconTextSpacer))

                            Text(
                                text = if (isHovered && !isDragging) "Folder" else cell.folder.name,
                                fontSize = appNameFontSize,
                                fontFamily = appNameFontFamily ?: FontFamily.Default,
                                color = if (isHovered && !isValidDropTarget && !isDragging)
                                    Color(0xFFFF6B6B) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = if (isDragging) 0f else folderLabelAlpha },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }
                    }

                    // Context menu
                    AnimatedPopup(
                            visible = showContextMenu,
                            onDismissRequest = { showContextMenu = false }
                        ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = cell.folder.name,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Divider()

                                    DropdownMenuItem(
                                        text = { Text("Remove folder") },
                                        onClick = {
                                            showContextMenu = false
                                            showFolderRemoveConfirm = true
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                    )
                        }

                    if (showFolderRemoveConfirm) {
                        AlertDialog(
                            onDismissRequest = { showFolderRemoveConfirm = false },
                            title = { Text("Remove Folder?") },
                            text = { Text("Apps inside this folder (${cell.folder.name}) will be removed from the launcher screen.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onRemove()
                                    showFolderRemoveConfirm = false
                                }) {
                                    Text("Remove")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showFolderRemoveConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            is HomeGridCell.Widget -> {
                // Widget origin cell - handles touch events and context menu
                // The actual widget view is rendered in the overlay layer in LauncherScreen
                var showWidgetMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Make this cell transparent - widget renders in overlay
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPosition = down.position

                                // Check if touch is within this cell's bounds
                                if (startPosition.x < 0 || startPosition.x > size.width ||
                                    startPosition.y < 0 || startPosition.y > size.height) {
                                    return@awaitEachGesture
                                }

                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null) {
                                    hapticFeedback.performLongPress()
                                    showWidgetMenu = true
                                }
                            }
                        }
                ) {
                    // Widget cell hover indicator - uses TileColorOnHover.kt
                    GridCellHoverIndicator(isHovered = isHovered, isValidDropTarget = isValidDropTarget, markerHalfSize = markerHalfSize, cornerRadius = hoverCornerRadius)

                    // Widget context menu
                    AnimatedPopup(
                            visible = showWidgetMenu,
                            onDismissRequest = { showWidgetMenu = false }
                        ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = "Widget",
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Divider()

                                    DropdownMenuItem(
                                        text = { Text("Remove widget") },
                                        onClick = {
                                            showWidgetMenu = false
                                            onWidgetRemove()
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                    )
                        }
                }
            }

            is HomeGridCell.WidgetSpan -> {
                // This cell is occupied by a multi-cell widget from another origin cell
                // Shows hover indicator when something is being dragged over it
                Box(modifier = Modifier.fillMaxSize()) {
                    // WidgetSpan cell hover indicator - uses TileColorOnHover.kt
                    GridCellHoverIndicator(isHovered = isHovered, isValidDropTarget = isValidDropTarget, markerHalfSize = markerHalfSize, cornerRadius = hoverCornerRadius)
                }
            }
        }
        } // Close inner content Box
    } // Close outer Box
}

/**
 * DockSlot - A single slot in the dock bar at the bottom
 * Same style as the main grid - Lawnchair style empty cells with hover animation
 * Fills available width from parent container
 * Supports drag and drop like grid cells
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockSlot(
    appInfo: HomeAppInfo?,
    slotIndex: Int,
    totalSlots: Int,
    iconSize: Int,
    isEditMode: Boolean,
    isDragging: Boolean,
    checkIsDragOwner: () -> Boolean, // Lambda to check ownership dynamically at call time
    isDropTarget: Boolean,
    isHovered: Boolean, // Shows hover indicator (includes original slot when dragging back over it)
    isValidDropTarget: Boolean = true, // Whether this is a valid drop target (true = blue, false = red)
    isHoverTargetValid: Boolean = true, // When dragging, is the current hover position valid? (for icon tint)
    dragOffset: Offset,
    // Dock folder support
    folderData: DockFolder? = null,
    folderPreviewApps: List<HomeAppInfo> = emptyList(),
    // Proportional sizing params (defaults match 360dp phone with 4 columns)
    markerHalfSizeParam: Dp = 6.dp,
    hoverCornerRadius: Dp = 12.dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo: () -> Unit,
    onCustomize: () -> Unit = {},
    isCustomizing: Boolean = false, // When true, keep icon scaled up while customize dialog is open
    globalIconSizePercent: Float = 100f, // Global icon size for absolute per-app scale
    globalIconShape: String? = null, // Global icon shape (EXP method) applied when no per-app shape
    globalIconBgColor: Int? = null, // Global icon background color (drawn behind icon within shape)
    onRenameDockFolder: (() -> Unit)? = null,
    homeFolders: List<com.bearinmind.launcher314.data.HomeFolder> = emptyList(),
    onAddToFolder: (com.bearinmind.launcher314.data.HomeFolder) -> Unit = {},
    onCreateFolder: () -> Unit = {}
) {
    val hapticFeedback = rememberHapticFeedback()
    // Match grid cell coordinate system - use same markerHalfSize for uniform sizing
    val markerHalfSize = markerHalfSizeParam

    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }

    // Track if long press is active for visual feedback
    var isLongPressActive by remember { mutableStateOf(false) }

    // Dark press + flash overlay
    var isDockFingerDown by remember { mutableStateOf(false) }
    var dockFlashOverlay by remember { mutableStateOf(false) }
    val dockFlashAlpha by animateFloatAsState(
        targetValue = if (dockFlashOverlay) 0.4f else 0f,
        animationSpec = if (dockFlashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
        label = "dock_flash_alpha",
        finishedListener = { if (dockFlashOverlay) dockFlashOverlay = false }
    )
    val dockOverlayAlpha = maxOf(if (isDockFingerDown) 0.25f else 0f, dockFlashAlpha)

    // Animate icon scale when context menu is shown or when dragging (like app drawer)
    // Use snap() when ending drag to prevent double animation stutter
    val isDockScaledUp = showContextMenu || isDragging || isCustomizing
    val animatedDockScale by animateFloatAsState(
        targetValue = if (isDockScaledUp) 1.265f else 1f,
        animationSpec = if (isDockScaledUp) tween(durationMillis = 150) else snap(),
        label = "dockIconScale"
    )
    val iconScale = if (isDockScaledUp) animatedDockScale else 1f

    // Fill available width and use square aspect ratio
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Square cells like grid
        contentAlignment = Alignment.Center
    ) {
        // Dock slot hover indicator - uses TileColorOnHover.kt
        // Only show background indicator for valid drop targets (blue)
        // Invalid targets show red icon tint instead (no red background)
        DockSlotHoverIndicator(isHovered = isHovered && isValidDropTarget, isValidDropTarget = isValidDropTarget, markerHalfSize = markerHalfSize, cornerRadius = hoverCornerRadius)


        if (appInfo != null && folderData == null) {
            // App content with drag support
            // When dragging, app is rendered in overlay layer (LauncherScreen)
            // so we hide it here to prevent duplicate rendering
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Hide content when dragging - it's rendered in overlay for proper z-ordering
                        // Use direct 0f/1f (no animation) to avoid one-frame flicker on drop
                        alpha = if (isDragging) 0f else 1f
                        clip = false // Allow scaled content to overflow
                    }
                    .padding(markerHalfSize)
                    .pointerInput(Unit) {
                        // Custom gesture handler - same as grid cells
                        val touchSlop = viewConfiguration.touchSlop

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPosition = down.position

                            // IMPORTANT: Check if touch is within this slot's bounds
                            // Since we use requireUnconsumed = false, we see ALL touches
                            // Only process if touch is actually in this slot
                            if (startPosition.x < 0 || startPosition.x > size.width ||
                                startPosition.y < 0 || startPosition.y > size.height) {
                                return@awaitEachGesture
                            }

                            isDockFingerDown = true
                            var dragStarted = false

                            // Wait for long press
                            val longPress = awaitLongPressOrCancellation(down.id)

                            if (longPress != null) {
                                // Long press triggered - show menu immediately while holding
                                isLongPressActive = true
                                showContextMenu = true
                                dockFlashOverlay = true
                                hapticFeedback.performLongPress()

                                // Wait for movement (drag) or release (menu stays)
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break

                                        if (change.pressed) {
                                            val dx = change.position.x - startPosition.x
                                            val dy = change.position.y - startPosition.y
                                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                            if (distance > touchSlop && !dragStarted) {
                                                // Movement after long press = start drag, hide menu
                                                dragStarted = true
                                                showContextMenu = false
                                                onDragStart()
                                            }

                                            // CRITICAL: Only process drag if this handler is the actual drag owner
                                            // This prevents multiple handlers from adding to dragOffset
                                            // Use checkIsDragOwner() to evaluate ownership at call time
                                            if (dragStarted && checkIsDragOwner()) {
                                                val dragDelta = Offset(
                                                    change.position.x - change.previousPosition.x,
                                                    change.position.y - change.previousPosition.y
                                                )
                                                change.consume()
                                                onDrag(dragDelta)
                                            }
                                        } else {
                                            // Finger released - only call onDragEnd if we own the drag
                                            if (dragStarted && checkIsDragOwner()) {
                                                onDragEnd()
                                            }
                                            // Menu stays visible if not dragged (already shown)
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (dragStarted && checkIsDragOwner()) onDragEnd()
                                } finally {
                                    isLongPressActive = false
                                    isDockFingerDown = false
                                }
                            } else {
                                isDockFingerDown = false
                                // Long press cancelled - check if it was a tap
                                val upEvent = currentEvent.changes.firstOrNull()
                                if (upEvent != null && !upEvent.pressed) {
                                    onTap()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val dockHasCustomIcon = appInfo?.customization?.customIconPath?.let { File(it).exists() } == true
                val dockHasShapeExp = appInfo?.customization?.iconShapeExp != null
                val dockHasPerAppShape = appInfo?.customization?.iconShape != null
                val dockContext = LocalContext.current
                val dockIconModelPath = if (dockHasCustomIcon && appInfo != null) {
                    appInfo.customization!!.customIconPath!!
                } else if (dockHasShapeExp && appInfo != null) {
                    File(getShapedExpDir(dockContext), "${appInfo.packageName}.png").let {
                        if (it.exists()) it.absolutePath else appInfo.iconPath
                    }
                } else if (!dockHasPerAppShape && globalIconShape != null && appInfo != null) {
                    File(getGlobalShapedDir(dockContext), "${appInfo.packageName}.png").let {
                        if (it.exists()) it.absolutePath
                        else try { getOrGenerateGlobalShapedIcon(dockContext, appInfo.packageName, globalIconShape!!) } catch (_: Exception) { appInfo.iconPath }
                    }
                } else appInfo?.iconPath ?: ""
                // Check for background-only tinted icon
                val dockHasBgTint = appInfo?.customization?.iconTintBackgroundOnly == true && appInfo?.customization?.iconTintColor != null
                val dockHasAnyShape = dockHasShapeExp || (!dockHasPerAppShape && globalIconShape != null)
                val dockEffectiveShape = appInfo?.customization?.iconShapeExp ?: appInfo?.customization?.iconShape ?: globalIconShape
                val dockFinalIconModelPath = if (dockHasBgTint && !dockHasCustomIcon && appInfo != null) {
                    val tintColor = appInfo.customization?.iconTintColor?.toInt() ?: 0
                    val tintAlpha = (appInfo.customization?.iconTintIntensity ?: 100) / 100f
                    try {
                        if (dockHasAnyShape && dockEffectiveShape != null) {
                            generateShapedBgTintedIcon(dockContext, appInfo.packageName, dockEffectiveShape, tintColor, tintAlpha)
                        } else {
                            generateBgTintedIcon(dockContext, appInfo.packageName, tintColor, tintAlpha)
                        }
                    } catch (_: Exception) { dockIconModelPath }
                } else dockIconModelPath
                val dockHasAnyExpShape = dockHasShapeExp || (!dockHasPerAppShape && globalIconShape != null)
                val dockIconClipShape = if (dockHasCustomIcon) {
                    getIconShape(appInfo?.customization?.iconShapeExp ?: appInfo?.customization?.iconShape ?: globalIconShape)
                } else if (!dockHasAnyExpShape) appInfo?.let { getIconShape(it.customization?.iconShape) } else null
                val dockCustomTintFilter = if (dockHasBgTint) null else appInfo?.customization?.iconTintColor?.let { tintColor ->
                    val intensity = (appInfo.customization?.iconTintIntensity ?: 100) / 100f
                    ColorFilter.tint(Color(tintColor.toInt()).copy(alpha = intensity), parseBlendMode(appInfo.customization?.iconTintBlendMode))
                }
                val dockPerAppSizePercent = appInfo?.customization?.iconSizePercent ?: globalIconSizePercent.toInt()
                val dockPerAppIconSizeDp = (iconSize * dockPerAppSizePercent / globalIconSizePercent.toFloat()).dp
                // When bg color is set, generate icon with user color as bg layer
                val dockUseBgColorIcon = globalIconBgColor != null && !dockHasCustomIcon && appInfo != null
                val dockBgColorEffectiveShape = if (dockUseBgColorIcon) {
                    appInfo?.customization?.iconShapeExp
                        ?: appInfo?.customization?.iconShape
                        ?: globalIconShape
                } else null
                val dockDisplayIconPath = if (dockUseBgColorIcon && dockBgColorEffectiveShape != null && appInfo != null) {
                    try { getOrGenerateBgColorShapedIcon(dockContext, appInfo.packageName, dockBgColorEffectiveShape, globalIconBgColor!!) }
                    catch (_: Exception) { dockFinalIconModelPath }
                } else dockFinalIconModelPath
                val dockIsBgColorIcon = dockDisplayIconPath != dockFinalIconModelPath
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = File(dockDisplayIconPath),
                        contentDescription = appInfo.name,
                        contentScale = if (dockIsBgColorIcon) ContentScale.Fit else if (dockIconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                        colorFilter = if ((isDragging && !isHoverTargetValid) || (isHovered && !isValidDropTarget)) {
                            ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        } else dockCustomTintFilter,
                        modifier = Modifier
                            .size(dockPerAppIconSizeDp)
                            .then(if (!dockIsBgColorIcon && dockIconClipShape != null) Modifier.clip(dockIconClipShape) else Modifier)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                clip = false
                            }
                    )

                    // Dark overlay (press + flash) — uses icon silhouette
                    if (dockOverlayAlpha > 0f) {
                        AsyncImage(
                            model = File(dockFinalIconModelPath),
                            contentDescription = null,
                            contentScale = if (dockIconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.Black, androidx.compose.ui.graphics.BlendMode.SrcIn),
                            modifier = Modifier
                                .size(dockPerAppIconSizeDp)
                                .then(if (dockIconClipShape != null) Modifier.clip(dockIconClipShape) else Modifier)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    alpha = dockOverlayAlpha
                                }
                        )
                    }
                }
            }

            // Context menu (shown on long press, like app drawer)
            val dockIconSizePxForPopup = with(LocalDensity.current) { iconSize.dp.toPx().toInt() }
            AnimatedPopup(
                    visible = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    iconSizePx = dockIconSizePxForPopup
                ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = appInfo.name,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 28.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            showContextMenu = false
                                            onAppInfo()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "App info"
                                    )
                                }
                            }
                            Divider()

                            // 1. Remove from home
                            DropdownMenuItem(
                                text = { Text("Remove from home") },
                                onClick = {
                                    showContextMenu = false
                                    onRemove()
                                },
                                leadingIcon = { HomeOffIcon() }
                            )

                            // 2. Select (placeholder)
                            DropdownMenuItem(
                                text = { Text("Select") },
                                onClick = {
                                    showContextMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Circle, contentDescription = null) }
                            )

                            // 3. Uninstall
                            DropdownMenuItem(
                                text = { Text("Uninstall") },
                                onClick = {
                                    showContextMenu = false
                                    onUninstall()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                            )

                            // 4. Customize
                            DropdownMenuItem(
                                text = { Text("Customize") },
                                onClick = {
                                    showContextMenu = false
                                    onCustomize()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                            )

                            // 5. Folder
                            var dockFolderExpanded by remember { mutableStateOf(false) }
                            DropdownMenuItem(
                                text = { Text("Folder") },
                                onClick = { dockFolderExpanded = !dockFolderExpanded },
                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) }
                            )
                            AnimatedVisibility(
                                visible = dockFolderExpanded,
                                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                            ) {
                                Column {
                                    DropdownMenuItem(
                                        text = { Text("Create folder") },
                                        onClick = {
                                            showContextMenu = false
                                            onCreateFolder()
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                    if (homeFolders.isNotEmpty()) {
                                        Text(
                                            text = "Move to",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                        )
                                        homeFolders.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder.name) },
                                                onClick = {
                                                    showContextMenu = false
                                                    onAddToFolder(folder)
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                }
        } else if (folderData != null) {
            // Dock folder content - 2x2 mini icon grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (isDragging) 0f else 1f
                        clip = false
                    }
                    .padding(markerHalfSize)
                    .pointerInput(Unit) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPosition = down.position
                            if (startPosition.x < 0 || startPosition.x > size.width ||
                                startPosition.y < 0 || startPosition.y > size.height) {
                                return@awaitEachGesture
                            }
                            isDockFingerDown = true
                            var dragStarted = false
                            val longPress = awaitLongPressOrCancellation(down.id)
                            if (longPress != null) {
                                isLongPressActive = true
                                showContextMenu = true
                                dockFlashOverlay = true
                                hapticFeedback.performLongPress()
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (change.pressed) {
                                            val dx = change.position.x - startPosition.x
                                            val dy = change.position.y - startPosition.y
                                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                            if (distance > touchSlop && !dragStarted) {
                                                dragStarted = true
                                                showContextMenu = false
                                                onDragStart()
                                            }
                                            if (dragStarted && checkIsDragOwner()) {
                                                val dragDelta = Offset(
                                                    change.position.x - change.previousPosition.x,
                                                    change.position.y - change.previousPosition.y
                                                )
                                                change.consume()
                                                onDrag(dragDelta)
                                            }
                                        } else {
                                            if (dragStarted && checkIsDragOwner()) onDragEnd()
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (dragStarted && checkIsDragOwner()) onDragEnd()
                                } finally {
                                    isLongPressActive = false
                                    isDockFingerDown = false
                                }
                            } else {
                                isDockFingerDown = false
                                val upEvent = currentEvent.changes.firstOrNull()
                                if (upEvent != null && !upEvent.pressed) {
                                    onTap()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Folder 2x2 icon grid (same style as grid folder cells)
                val folderBoxSize = iconSize.dp
                val folderCornerRadius = (iconSize * 0.29f).dp
                val folderInvalidTint = if ((isDragging && !isHoverTargetValid) || (isHovered && !isValidDropTarget)) {
                    ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                } else null

                Box(
                    modifier = Modifier
                        .size(folderBoxSize)
                        .clip(getIconShape(globalIconShape) ?: RoundedCornerShape(folderCornerRadius))
                        .background(Color(0xFF1A1A1A))
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            clip = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (folderPreviewApps.isNotEmpty()) {
                        val padding = folderBoxSize * 0.08f
                        val spacing = folderBoxSize * 0.04f
                        val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2
                        Column(
                            modifier = Modifier.padding(padding),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                folderPreviewApps.getOrNull(0)?.let { app ->
                                    AsyncImage(
                                        model = File(app.iconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = folderInvalidTint,
                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                    )
                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                folderPreviewApps.getOrNull(1)?.let { app ->
                                    AsyncImage(
                                        model = File(app.iconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = folderInvalidTint,
                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                    )
                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                folderPreviewApps.getOrNull(2)?.let { app ->
                                    AsyncImage(
                                        model = File(app.iconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = folderInvalidTint,
                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                    )
                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                folderPreviewApps.getOrNull(3)?.let { app ->
                                    AsyncImage(
                                        model = File(app.iconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = folderInvalidTint,
                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                    )
                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                            }
                        }
                    }

                    // Dark overlay (press + flash)
                    if (dockOverlayAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = dockOverlayAlpha }
                                .background(Color.Black)
                        )
                    }
                }
            }

            // Dock folder context menu
            AnimatedPopup(
                    visible = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = folderData.name,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Divider()

                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showContextMenu = false
                                    onRenameDockFolder?.invoke()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Remove from dock") },
                                onClick = {
                                    showContextMenu = false
                                    onRemove()
                                },
                                leadingIcon = { HomeOffIcon() }
                            )
                }
        }
    }
}

/**
 * Custom icon showing a home with a diagonal line through it (like "link off" style)
 * Used for "Remove from home" and "Remove from dock" menu items
 */
@Composable
fun HomeOffIcon(
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier.size(24.dp)) {
        // Home icon
        Icon(
            imageVector = Icons.Outlined.Home,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
        // Diagonal line through it
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = tint,
                start = Offset(size.width * 0.15f, size.height * 0.85f),
                end = Offset(size.width * 0.85f, size.height * 0.15f),
                strokeWidth = strokeWidth
            )
        }
    }
}

/**
 * Material Symbols Outlined resize icon
 * Based on https://fonts.google.com/icons?selected=Material+Symbols+Outlined:resize
 */
val ResizeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Resize",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
        ) {
            // Material Symbols "resize" icon path
            // Top-left corner bracket
            moveTo(120f, 360f)
            verticalLineTo(120f)
            horizontalLineTo(360f)
            verticalLineTo(200f)
            horizontalLineTo(200f)
            verticalLineTo(360f)
            horizontalLineTo(120f)
            close()
            // Bottom-right corner bracket
            moveTo(600f, 840f)
            verticalLineTo(760f)
            horizontalLineTo(760f)
            verticalLineTo(600f)
            horizontalLineTo(840f)
            verticalLineTo(840f)
            horizontalLineTo(600f)
            close()
        }
    }.build()
}
