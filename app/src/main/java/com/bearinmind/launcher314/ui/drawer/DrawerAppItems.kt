package com.bearinmind.launcher314.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bearinmind.launcher314.R
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import java.io.File
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderItem(
    folder: AppFolder,
    allApps: List<AppInfo>,
    iconSize: Int = 48,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    onClick: () -> Unit,
    onPositioned: (Offset) -> Unit = {},
    onDelete: () -> Unit = {},
    onAddToHome: () -> Unit = {},
    onDragStarted: (() -> Unit)? = null,
    onDragMoved: ((Offset) -> Unit)? = null,
    onDragEnded: (() -> Unit)? = null,
    isDragHovered: Boolean = false,
    draggedIconPath: String? = null
) {
    val hapticFeedback = rememberHapticFeedback()
    var showContextMenu by remember { mutableStateOf(false) }

    // Get the first 4 apps in this folder for preview (filter empty gap markers)
    val previewApps = remember(folder.appPackageNames, allApps) {
        val validPkgs = folder.appPackageNames.filter { it.isNotEmpty() }
        allApps.filter { it.packageName in validPkgs }.take(4)
    }

    // Animated fade-in for dragged icon preview in folder
    var lastDraggedIconPath by remember { mutableStateOf<String?>(null) }
    if (draggedIconPath != null) lastDraggedIconPath = draggedIconPath
    val dragIconProgress by animateFloatAsState(
        targetValue = if (draggedIconPath != null) 1f else 0f,
        animationSpec = if (draggedIconPath != null) tween(durationMillis = 300) else snap(),
        label = "folderDragIconProgress",
        finishedListener = { value -> if (value == 0f) lastDraggedIconPath = null }
    )
    val effectiveDraggedIconPath = draggedIconPath ?: lastDraggedIconPath
    val addSlotIndex = previewApps.size.coerceAtMost(3)

    // Animate scale when context menu is shown (matches home screen folder style)
    val menuScale by animateFloatAsState(
        targetValue = if (showContextMenu) 1.265f else 1f,
        animationSpec = if (showContextMenu) tween(durationMillis = 150) else snap(),
        label = "folder_scale"
    )

    // Animate scale when another item is being dragged over this folder
    val hoverScale by animateFloatAsState(
        targetValue = if (isDragHovered) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "folder_hover_scale"
    )

    val scale = menuScale * hoverScale

    // Hide label when context menu is shown (matches app behavior)
    val labelAlpha by animateFloatAsState(
        targetValue = if (showContextMenu) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "folder_label_alpha"
    )

    // Dark press + flash overlay
    var isFingerDown by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var flashOverlay by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashOverlay) 0.4f else 0f,
        animationSpec = if (flashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
        label = "flash_alpha",
        finishedListener = { if (flashOverlay) flashOverlay = false }
    )
    val overlayAlpha = maxOf(if (isFingerDown || isPressed) 0.25f else 0f, flashAlpha)

    // Whether drag callbacks are provided (paged mode)
    val dragEnabled = onDragStarted != null

    // Keep drag callbacks always up-to-date (avoids stale capture in pointerInput)
    val currentOnDragStarted by rememberUpdatedState(onDragStarted)
    val currentOnDragMoved by rememberUpdatedState(onDragMoved)
    val currentOnDragEnded by rememberUpdatedState(onDragEnded)

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onPositioned(
                    Offset(
                        position.x + coordinates.size.width / 2,
                        position.y + coordinates.size.height / 2
                    )
                )
            }
    )   {
        Column(
            modifier = Modifier
                .wrapContentHeight(unbounded = true)
                .then(
                    if (dragEnabled) {
                        // Unified gesture: tap, long press (context menu), long press + drag
                        Modifier.pointerInput(Unit) {
                            val touchSlop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                isFingerDown = true
                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null) {
                                    // Long press detected — show context menu, track for drag
                                    hapticFeedback.performLongPress()
                                    showContextMenu = true
                                    flashOverlay = true
                                    var dragStarted = false
                                    var lastPos = longPress.position
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (change.pressed) {
                                                val dx = change.position.x - down.position.x
                                                val dy = change.position.y - down.position.y
                                                val dist = sqrt(dx * dx + dy * dy)
                                                if (dist > touchSlop && !dragStarted) {
                                                    dragStarted = true
                                                    showContextMenu = false
                                                    currentOnDragStarted!!()
                                                    lastPos = change.position
                                                }
                                                if (dragStarted) {
                                                    val delta = Offset(
                                                        change.position.x - lastPos.x,
                                                        change.position.y - lastPos.y
                                                    )
                                                    lastPos = change.position
                                                    change.consume()
                                                    currentOnDragMoved?.invoke(delta)
                                                }
                                            } else {
                                                if (dragStarted) currentOnDragEnded?.invoke()
                                                break
                                            }
                                        }
                                    } catch (_: Exception) {
                                        if (dragStarted) currentOnDragEnded?.invoke()
                                    } finally {
                                        isFingerDown = false
                                    }
                                } else {
                                    isFingerDown = false
                                    // Only tap if finger actually lifted and didn't move (prevents swipe launching apps)
                                    val upChange = currentEvent.changes.firstOrNull { it.id == down.id }
                                    if (upChange != null && !upChange.pressed) {
                                        val dx = upChange.position.x - down.position.x
                                        val dy = upChange.position.y - down.position.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist <= touchSlop) {
                                            flashOverlay = true
                                            onClick()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Default: combinedClickable (scroll mode, folder inside, etc.)
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                flashOverlay = true
                                onClick()
                            },
                            onLongClick = {
                                showContextMenu = true
                                flashOverlay = true
                                hapticFeedback.performLongPress()
                            }
                        )
                    }
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Folder preview - 2x2 grid of app icons (same size as regular icons for alignment)
            BoxWithConstraints(
                modifier = Modifier
                    .size(iconSize.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        clip = false
                    }
                    .clip(RoundedCornerShape((iconSize * 0.29f).dp))
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                if (previewApps.isNotEmpty() || (draggedIconPath != null && dragIconProgress > 0f)) {
                    val boxSize = maxWidth
                    val padding = boxSize * 0.08f
                    val spacing = boxSize * 0.04f
                    val miniIconSize = (boxSize - padding * 2 - spacing) / 2

                    Column(
                        modifier = Modifier.padding(padding),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            previewApps.getOrNull(0)?.let { app ->
                                FolderPreviewIcon(app, miniIconSize)
                            } ?: if (addSlotIndex == 0 && draggedIconPath != null && dragIconProgress > 0f) {
                                AsyncImage(
                                    model = File(draggedIconPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                        .graphicsLayer { alpha = dragIconProgress }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(miniIconSize))
                            }
                            previewApps.getOrNull(1)?.let { app ->
                                FolderPreviewIcon(app, miniIconSize)
                            } ?: if (addSlotIndex == 1 && draggedIconPath != null && dragIconProgress > 0f) {
                                AsyncImage(
                                    model = File(draggedIconPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                        .graphicsLayer { alpha = dragIconProgress }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(miniIconSize))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            previewApps.getOrNull(2)?.let { app ->
                                FolderPreviewIcon(app, miniIconSize)
                            } ?: if (addSlotIndex == 2 && draggedIconPath != null && dragIconProgress > 0f) {
                                AsyncImage(
                                    model = File(draggedIconPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                        .graphicsLayer { alpha = dragIconProgress }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(miniIconSize))
                            }
                            // Slot 3 — when all 4 slots occupied and hovering, crossfade to dragged app
                            if (draggedIconPath != null && dragIconProgress > 0f && previewApps.size >= 4) {
                                Box(modifier = Modifier.size(miniIconSize)) {
                                    previewApps.getOrNull(3)?.let { app ->
                                        FolderPreviewIcon(
                                            app, miniIconSize,
                                            alpha = 1f - dragIconProgress
                                        )
                                    }
                                    AsyncImage(
                                        model = File(draggedIconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(miniIconSize)
                                            .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                            .graphicsLayer { alpha = dragIconProgress }
                                    )
                                }
                            } else {
                                previewApps.getOrNull(3)?.let { app ->
                                    FolderPreviewIcon(app, miniIconSize)
                                } ?: if (addSlotIndex == 3 && draggedIconPath != null && dragIconProgress > 0f) {
                                    AsyncImage(
                                        model = File(draggedIconPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(miniIconSize)
                                            .clip(RoundedCornerShape(miniIconSize * 0.2f))
                                            .graphicsLayer { alpha = dragIconProgress }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(miniIconSize))
                                }
                            }
                        }
                    }
                }

                // Dark overlay (press + flash)
                if (overlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = overlayAlpha }
                            .background(Color.Black)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folder.name,
                fontSize = labelFontSize,
                fontFamily = labelFontFamily ?: FontFamily.Default,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = labelAlpha },
                style = MaterialTheme.typography.bodySmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }

        // Context menu for folder
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
            AnimatedPopup(
                visible = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                        // Folder name header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = folder.name,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        DropdownMenuItem(
                            text = { Text("Add to home") },
                            onClick = {
                                showContextMenu = false
                                onAddToHome()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = {
                                showContextMenu = false
                                showDeleteConfirmDialog = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )
            }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Folder?") },
                text = { Text("Apps inside this folder (${folder.name}) will be moved back to the drawer.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
internal fun FolderPreviewIcon(app: AppInfo, size: Dp = 20.dp, alpha: Float = 1f) {
    AsyncImage(
        model = File(app.iconPath),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.2f)) // Scale corner radius
            .graphicsLayer { this.alpha = alpha }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppItem(
    app: AppInfo,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = File(app.iconPath),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            fontSize = labelFontSize,
            fontFamily = labelFontFamily ?: FontFamily.Default,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderAppItem(
    app: AppInfo,
    iconSize: Int = 48,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit = {},
    onAppInfo: () -> Unit = {},
    folders: List<AppFolder> = emptyList(),
    currentFolderId: String = "",
    onMoveToFolder: (AppFolder) -> Unit = {},
    isFolderMenuExpanded: Boolean = false,
    onFolderMenuExpandedChange: (Boolean) -> Unit = {},
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    selectedCount: Int = 0,
    onBulkRemove: () -> Unit = {},
    onBulkMoveToFolder: (AppFolder) -> Unit = {}
) {
    val hapticFeedback = rememberHapticFeedback()
    var showContextMenu by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }


    // Show selection circle only when in selection mode (user tapped "Select")
    val showSelectionCircle = isSelected || selectedCount > 0

    // Scaled selection circle sizes (proportional to icon size)
    val circleSize = (iconSize * 0.42f).dp
    val checkmarkSize = (iconSize * 0.27f).dp
    val circleBorder = (iconSize * 0.018f).dp
    val circleOffset = (iconSize * 0.083f).dp

    // Animate scale when context menu is shown OR app is selected (matches home screen 1.265f)
    val isScaledUp = showContextMenu || showBulkMenu || isSelected
    val scale by animateFloatAsState(
        targetValue = if (isScaledUp) 1.265f else 1f,
        animationSpec = if (isScaledUp) tween(durationMillis = 150) else snap(),
        label = "icon_scale"
    )

    // Hide label when scaled up (matches home screen app behavior)
    val labelAlpha by animateFloatAsState(
        targetValue = if (isScaledUp) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "label_alpha"
    )

    // Dark press + flash overlay
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var flashOverlay by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashOverlay) 0.4f else 0f,
        animationSpec = if (flashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
        label = "flash_alpha",
        finishedListener = { if (flashOverlay) flashOverlay = false }
    )
    val overlayAlpha = maxOf(if (isPressed) 0.25f else 0f, flashAlpha)

    Box {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        flashOverlay = true
                        onClick()
                    },
                    onLongClick = {
                        hapticFeedback.performLongPress()
                        flashOverlay = true
                        // If this app is selected and there are selections, show bulk menu
                        if (isSelected && selectedCount > 0) {
                            showBulkMenu = true
                        } else {
                            showContextMenu = true
                        }
                    }
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with selection circle overlay
            Box(
                modifier = Modifier.size(iconSize.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(app.iconPath),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )

                // Dark overlay (press + flash) — uses icon silhouette to match exact shape
                if (overlayAlpha > 0f) {
                    AsyncImage(
                        model = File(app.iconPath),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn),
                        modifier = Modifier
                            .size(iconSize.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = overlayAlpha
                            }
                    )
                }

                // Selection circle overlay - positioned at top-right corner
                // Purely visual — tap handled by parent Column's onClick
                if (showSelectionCircle) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = circleOffset, y = -circleOffset)
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Black.copy(alpha = 0.35f)
                            )
                            .border(
                                width = circleBorder,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name,
                fontSize = labelFontSize,
                fontFamily = labelFontFamily ?: FontFamily.Default,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = labelAlpha }
            )
        }

        // Single app context menu dropdown
            AnimatedPopup(
                visible = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                        // App name header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = app.name,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        // Select option - for multi-select
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

                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = {
                                showContextMenu = false
                                onUninstall()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("App info") },
                            onClick = {
                                showContextMenu = false
                                onAppInfo()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) }
                        )

                        // Folder section - expandable
                        DropdownMenuItem(
                            text = { Text("Folder") },
                            onClick = { onFolderMenuExpandedChange(!isFolderMenuExpanded) },
                            leadingIcon = {
                                if (isFolderMenuExpanded) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_open),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        // Expanded folder options — animate in/out
                        AnimatedVisibility(
                            visible = isFolderMenuExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column {
                                // Remove from folder option (instead of Create folder)
                                DropdownMenuItem(
                                    text = { Text("Remove") },
                                    onClick = {
                                        showContextMenu = false
                                        onRemove()
                                    },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_folder_limited), contentDescription = null) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                                // Move to section header
                                val otherFolders = folders.filter { it.id != currentFolderId }
                                if (otherFolders.isNotEmpty()) {
                                    Text(
                                        text = "Move to",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                    )

                                    // List other folders with drive_file_move icon
                                    otherFolders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            onClick = {
                                                showContextMenu = false
                                                onMoveToFolder(folder)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_drive_file_move),
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
            }

        // Bulk action menu for multiple selected apps
            AnimatedPopup(
                visible = showBulkMenu,
                onDismissRequest = { showBulkMenu = false }
            ) {
                        // Header showing selection count
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "$selectedCount apps selected",
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        // Folder section - expandable
                        DropdownMenuItem(
                            text = { Text("Folder") },
                            onClick = { onFolderMenuExpandedChange(!isFolderMenuExpanded) },
                            leadingIcon = {
                                if (isFolderMenuExpanded) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_open),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        // Expanded folder options for bulk — animate in/out
                        AnimatedVisibility(
                            visible = isFolderMenuExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column {
                                // Remove all from folder
                                DropdownMenuItem(
                                    text = { Text("Remove all") },
                                    onClick = {
                                        showBulkMenu = false
                                        onBulkRemove()
                                    },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_folder_limited), contentDescription = null) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                                // Move to section header
                                val otherFolders = folders.filter { it.id != currentFolderId }
                                if (otherFolders.isNotEmpty()) {
                                    Text(
                                        text = "Move all to",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                    )

                                    // List other folders
                                    otherFolders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            onClick = {
                                                showBulkMenu = false
                                                onBulkMoveToFolder(folder)
                                            },
                                            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_drive_file_move), contentDescription = null) },
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectableAppItem(
    app: AppInfo,
    iconSize: Int = 48,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    onClick: () -> Unit,
    onUninstall: () -> Unit = {},
    onAppInfo: () -> Unit = {},
    onAddToHome: () -> Unit = {},
    folders: List<AppFolder> = emptyList(),
    onAddToFolder: (AppFolder) -> Unit = {},
    onCreateFolderWithApps: (List<AppInfo>) -> Unit = {},
    onDeleteFolder: (AppFolder) -> Unit = {},
    isFolderMenuExpanded: Boolean = false,
    onFolderMenuExpandedChange: (Boolean) -> Unit = {},
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    selectionModeActive: Boolean = false,
    selectedCount: Int = 0,
    selectedApps: List<AppInfo> = emptyList(),
    onBulkAddToFolder: (AppFolder) -> Unit = {},
    onBulkAddToHome: () -> Unit = {},
    onDragStarted: (() -> Unit)? = null,
    onDragMoved: ((Offset) -> Unit)? = null,
    onDragEnded: (() -> Unit)? = null,
    iconClipShape: androidx.compose.ui.graphics.Shape? = null,
    iconBgColor: Int? = null,
    globalIconShapeName: String? = null
) {
    val drawerItemContext = LocalContext.current
    val hapticFeedback = rememberHapticFeedback()
    var showContextMenu by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var folderToDeleteFromMenu by remember { mutableStateOf<AppFolder?>(null) }


    // Show selection circle only when selection mode is explicitly active (user tapped "Select")
    val showSelectionCircle = selectionModeActive

    // Scaled selection circle sizes (proportional to icon size)
    val circleSize = (iconSize * 0.42f).dp
    val checkmarkSize = (iconSize * 0.27f).dp
    val circleBorder = (iconSize * 0.018f).dp
    val circleOffset = (iconSize * 0.083f).dp

    // Animate scale when context menu is shown OR app is selected (matches home screen 1.265f)
    val isScaledUp = showContextMenu || showBulkMenu || isSelected
    val scale by animateFloatAsState(
        targetValue = if (isScaledUp) 1.265f else 1f,
        animationSpec = if (isScaledUp) tween(durationMillis = 150) else snap(),
        label = "icon_scale"
    )

    // Hide label when scaled up (matches home screen app behavior)
    val labelAlpha by animateFloatAsState(
        targetValue = if (isScaledUp) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "label_alpha"
    )

    // Dark press + flash overlay
    var isFingerDown by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var flashOverlay by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashOverlay) 0.4f else 0f,
        animationSpec = if (flashOverlay) tween(durationMillis = 80) else tween(durationMillis = 150),
        label = "flash_alpha",
        finishedListener = { if (flashOverlay) flashOverlay = false }
    )
    val overlayAlpha = maxOf(if (isFingerDown || isPressed) 0.25f else 0f, flashAlpha)

    // Whether drag callbacks are provided (paged mode)
    val dragEnabled = onDragStarted != null

    // Keep drag callbacks always up-to-date (avoids stale capture in pointerInput)
    val currentOnDragStarted by rememberUpdatedState(onDragStarted)
    val currentOnDragMoved by rememberUpdatedState(onDragMoved)
    val currentOnDragEnded by rememberUpdatedState(onDragEnded)

    Box {
        Column(
            modifier = Modifier
                .wrapContentHeight(unbounded = true)
                .then(
                    if (dragEnabled) {
                        // Unified gesture: tap, long press (context menu), long press + drag
                        Modifier.pointerInput(app.packageName) {
                            val touchSlop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                isFingerDown = true
                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null) {
                                    hapticFeedback.performLongPress()
                                    flashOverlay = true
                                    if (isSelected && selectedCount > 0) {
                                        showBulkMenu = true
                                    } else {
                                        showContextMenu = true
                                    }
                                    var dragStarted = false
                                    var lastPos = longPress.position
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (change.pressed) {
                                                val dx = change.position.x - down.position.x
                                                val dy = change.position.y - down.position.y
                                                val dist = sqrt(dx * dx + dy * dy)
                                                if (dist > touchSlop && !dragStarted) {
                                                    dragStarted = true
                                                    showContextMenu = false
                                                    showBulkMenu = false
                                                    currentOnDragStarted!!()
                                                    lastPos = change.position
                                                }
                                                if (dragStarted) {
                                                    val delta = Offset(
                                                        change.position.x - lastPos.x,
                                                        change.position.y - lastPos.y
                                                    )
                                                    lastPos = change.position
                                                    change.consume()
                                                    currentOnDragMoved?.invoke(delta)
                                                }
                                            } else {
                                                if (dragStarted) currentOnDragEnded?.invoke()
                                                break
                                            }
                                        }
                                    } catch (_: Exception) {
                                        if (dragStarted) currentOnDragEnded?.invoke()
                                    } finally {
                                        isFingerDown = false
                                    }
                                } else {
                                    isFingerDown = false
                                    // Only tap if finger actually lifted and didn't move (prevents swipe launching apps)
                                    val upChange = currentEvent.changes.firstOrNull { it.id == down.id }
                                    if (upChange != null && !upChange.pressed) {
                                        val dx = upChange.position.x - down.position.x
                                        val dy = upChange.position.y - down.position.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist <= touchSlop) {
                                            flashOverlay = true
                                            onClick()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Default: combinedClickable (scroll mode, etc.)
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                flashOverlay = true
                                onClick()
                            },
                            onLongClick = {
                                hapticFeedback.performLongPress()
                                flashOverlay = true
                                if (isSelected && selectedCount > 0) {
                                    showBulkMenu = true
                                } else {
                                    showContextMenu = true
                                }
                            }
                        )
                    }
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with selection circle overlay
            // When bg color is set, use pre-generated icon with user color as bg layer
            val drawerShapedIconPath = if (globalIconShapeName != null) {
                try {
                    if (iconBgColor != null) {
                        getOrGenerateBgColorShapedIcon(drawerItemContext, app.packageName, globalIconShapeName, iconBgColor)
                    } else {
                        getOrGenerateGlobalShapedIcon(drawerItemContext, app.packageName, globalIconShapeName)
                    }
                } catch (_: Exception) { null }
            } else null
            val drawerIsShapedIcon = drawerShapedIconPath != null
            val drawerDisplayIconPath = drawerShapedIconPath ?: app.iconPath
            Box(
                modifier = Modifier.size(iconSize.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(drawerDisplayIconPath),
                    contentDescription = null,
                    contentScale = if (drawerIsShapedIcon) ContentScale.Fit else if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .then(if (!drawerIsShapedIcon && iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )

                // Dark overlay (press + flash) — uses icon silhouette to match exact shape
                if (overlayAlpha > 0f) {
                    AsyncImage(
                        model = File(drawerDisplayIconPath),
                        contentDescription = null,
                        contentScale = if (drawerIsShapedIcon) ContentScale.Fit else if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn),
                        modifier = Modifier
                            .size(iconSize.dp)
                            .then(if (!drawerIsShapedIcon && iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = overlayAlpha
                            }
                    )
                }

                // Selection circle overlay - positioned at top-right corner
                // Purely visual — tap handled by parent Column's onClick (toggles selection in selection mode)
                if (showSelectionCircle) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = circleOffset, y = -circleOffset)
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Black.copy(alpha = 0.35f)
                            )
                            .border(
                                width = circleBorder,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name,
                fontSize = labelFontSize,
                fontFamily = labelFontFamily ?: FontFamily.Default,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = labelAlpha }
            )
        }

        // Single app context menu dropdown
            AnimatedPopup(
                visible = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                        // App name header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = app.name,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        // Select option - for multi-select
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

                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = {
                                showContextMenu = false
                                onUninstall()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("App info") },
                            onClick = {
                                showContextMenu = false
                                onAppInfo()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Add to home") },
                            onClick = {
                                showContextMenu = false
                                onAddToHome()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) }
                        )

                        // Folder section - expandable (uses global state)
                        DropdownMenuItem(
                            text = { Text("Folder") },
                            onClick = { onFolderMenuExpandedChange(!isFolderMenuExpanded) },
                            leadingIcon = {
                                if (isFolderMenuExpanded) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_open),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        // Expanded folder options — animate in/out
                        AnimatedVisibility(
                            visible = isFolderMenuExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column {
                                // Create folder option - passes current app to be moved to new folder
                                DropdownMenuItem(
                                    text = { Text("Create folder") },
                                    onClick = {
                                        showContextMenu = false
                                        onCreateFolderWithApps(listOf(app))
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                                // Move to section header
                                if (folders.isNotEmpty()) {
                                    Text(
                                        text = "Move to",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                    )

                                    // List all folders with delete button
                                    folders.forEach { folder ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(folder.name) },
                                                onClick = {
                                                    showContextMenu = false
                                                    onAddToFolder(folder)
                                                },
                                                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_drive_file_move), contentDescription = null) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    showContextMenu = false
                                                    folderToDeleteFromMenu = folder
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Delete folder",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
            }

        // Folder delete confirmation (extracted outside Popup so it persists after menu closes)
        folderToDeleteFromMenu?.let { folderToDelete ->
            AlertDialog(
                onDismissRequest = { folderToDeleteFromMenu = null },
                title = { Text("Delete Folder?") },
                text = { Text("Apps inside this folder (${folderToDelete.name}) will be moved back to the drawer.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteFolder(folderToDelete)
                        folderToDeleteFromMenu = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDeleteFromMenu = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Bulk action menu for multiple selected apps
            AnimatedPopup(
                visible = showBulkMenu,
                onDismissRequest = { showBulkMenu = false }
            ) {
                        // Header showing selection count
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "$selectedCount apps selected",
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        // Add all selected to home
                        DropdownMenuItem(
                            text = { Text("Add to home") },
                            onClick = {
                                showBulkMenu = false
                                onBulkAddToHome()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) }
                        )

                        // Folder section - expandable (uses global state)
                        DropdownMenuItem(
                            text = { Text("Folder") },
                            onClick = { onFolderMenuExpandedChange(!isFolderMenuExpanded) },
                            leadingIcon = {
                                if (isFolderMenuExpanded) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_open),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        // Expanded folder options for bulk add — animate in/out
                        AnimatedVisibility(
                            visible = isFolderMenuExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column {
                                // Create folder option - passes all selected apps to be moved to new folder
                                DropdownMenuItem(
                                    text = { Text("Create folder") },
                                    onClick = {
                                        showBulkMenu = false
                                        onCreateFolderWithApps(selectedApps)
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                                // Move to section header
                                if (folders.isNotEmpty()) {
                                    Text(
                                        text = "Move to",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp)
                                    )

                                    // List all folders
                                    folders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            onClick = {
                                                showBulkMenu = false
                                                onBulkAddToFolder(folder)
                                            },
                                            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_drive_file_move), contentDescription = null) },
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
            }
    }
}

@Composable
internal fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
