package com.bearinmind.launcher314.ui.drawer

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.launchApp
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import com.bearinmind.launcher314.ui.components.GridCellHoverIndicator
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun FolderContentScreen(
    folder: AppFolder,
    allApps: List<AppInfo>,
    gridSize: Int,
    iconSize: Int,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    onBack: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onRemoveApps: (List<String>) -> Unit = {},
    onUninstallApp: (AppInfo) -> Unit = {},
    onAppInfo: (AppInfo) -> Unit = {},
    onDeleteFolder: () -> Unit,
    onRenameFolder: (String) -> Unit,
    folders: List<AppFolder> = emptyList(),
    onMoveToFolder: (String, AppFolder) -> Unit = { _, _ -> },
    onMoveAppsToFolder: (List<String>, AppFolder) -> Unit = { _, _ -> },
    isFolderMenuExpanded: Boolean = false,
    onFolderMenuExpandedChange: (Boolean) -> Unit = {},
    onAddToHome: (AppInfo) -> Unit = {},
    onReorderApps: (List<String>) -> Unit = {},
    onEscapeToDrawer: (AppInfo, Offset) -> Unit = { _, _ -> },
    onEscapeDragMove: (Offset) -> Unit = {},
    onEscapeDragEnd: () -> Unit = {},
    iconClipShape: androidx.compose.ui.graphics.Shape? = null,
    iconBgColor: Int? = null,
    globalIconShapeName: String? = null
) {
    val context = LocalContext.current
    val hapticFeedback = rememberHapticFeedback()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val currentOnEscapeToDrawer by rememberUpdatedState(onEscapeToDrawer)
    val currentOnEscapeDragMove by rememberUpdatedState(onEscapeDragMove)
    val currentOnEscapeDragEnd by rememberUpdatedState(onEscapeDragEnd)

    // Multi-select state for apps in folder
    var selectedAppPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Inline editing state
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(folder.name) {
        mutableStateOf(TextFieldValue(folder.name, TextRange(folder.name.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Auto-save folder name as user types (debounced)
    LaunchedEffect(editedName.text) {
        if (editedName.text != folder.name && editedName.text.isNotBlank()) {
            kotlinx.coroutines.delay(300)
            onRenameFolder(editedName.text)
        }
    }

    val folderApps = remember(folder, allApps) {
        allApps.filter { it.packageName in folder.appPackageNames }
    }

    // Position-based cell map: cellIndex → packageName (supports empty cells/gaps)
    var folderCellMap by remember(folder.id) {
        val map = folder.appPackageNames.withIndex()
            .filter { it.value.isNotEmpty() }
            .associate { it.index to it.value }
        Log.d("FolderDebug", "FolderContentScreen remember(${folder.id}): init cellMap from ${folder.appPackageNames} -> $map")
        mutableStateOf(map)
    }
    LaunchedEffect(folder.appPackageNames) {
        val map = folder.appPackageNames.withIndex()
            .filter { it.value.isNotEmpty() }
            .associate { it.index to it.value }
        Log.d("FolderDebug", "FolderContentScreen LaunchedEffect: updating cellMap from ${folder.appPackageNames} -> $map")
        folderCellMap = map
    }

    // Save helper: convert cell map back to ordered list
    fun saveCellMap(map: Map<Int, String>) {
        val maxIdx = if (map.isEmpty()) -1 else map.keys.max()
        val ordered = if (maxIdx >= 0) {
            (0..maxIdx).map { idx -> map[idx] ?: "" }.dropLastWhile { it.isEmpty() }
        } else emptyList()
        onReorderApps(ordered)
    }

    // Grid dimensions
    val gridColumns = gridSize
    val maxOccupied = if (folderCellMap.isEmpty()) 0 else folderCellMap.keys.max() + 1
    val gridRows = maxOf(gridColumns, (maxOccupied + gridColumns - 1) / gridColumns)

    // Grid cell proportional sizes (matching home screen pattern)
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    // The folder content area is 2/3 of screen height, with 16dp padding on each side
    val folderContentHeight = screenHeightDp * 0.67f
    val cellWidth = (screenWidthDp - 32f) / gridColumns
    val cellHeight = (folderContentHeight - 32f) / gridRows
    val cellBasis = minOf(cellWidth, cellHeight)
    val markerHalfSize = (cellBasis * 0.073f).dp
    val plusMarkerSize = (cellBasis * 0.146f).dp
    val plusMarkerFont = (cellBasis * 0.122f).sp
    val hoverCornerRadius = (cellBasis * 0.146f).dp

    // Drag state
    val folderScope = rememberCoroutineScope()
    var draggedPkg by remember { mutableStateOf<String?>(null) }
    var draggedCellIdx by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragOriginalCellPos by remember { mutableStateOf<Offset?>(null) }
    var hoveredCell by remember { mutableStateOf<Int?>(null) }
    val cellPositions = remember { mutableStateMapOf<Int, Offset>() }
    var cellSize by remember { mutableStateOf(IntSize.Zero) }
    // Drop animation
    var isDropAnimating by remember { mutableStateOf(false) }
    val dropAnimProgress = remember { Animatable(0f) }
    var dropStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetOffset by remember { mutableStateOf(Offset.Zero) }
    val isDraggingInFolder = draggedPkg != null && !isDropAnimating
    var escapedToDrawer by remember { mutableStateOf(false) }

    // Context menu state
    var contextMenuCellIdx by remember { mutableStateOf<Int?>(null) }

    // Root overlay position
    var folderRootPos by remember { mutableStateOf(Offset.Zero) }

    // Header drop zone — drag app here to remove from folder
    var headerBottomY by remember { mutableStateOf(0f) }
    var dragOverHeader by remember { mutableStateOf(false) }

    // Request focus when entering edit mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header area - 1/3 of screen, tap to close (lighter background)
        // Dragging an app here removes it from the folder and closes back to drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    headerBottomY = pos.y + coords.size.height
                },
            contentAlignment = Alignment.Center
        ) {
            // Background layer - tap anywhere to close (but not on the text)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isEditing) {
                            onBack()
                        } else {
                            // Tap outside text while editing - save and close edit mode
                            if (editedName.text.isNotBlank()) {
                                onRenameFolder(editedName.text)
                            }
                            isEditing = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
            )

            // Folder name centered in the middle of the header
            if (isEditing) {
                BasicTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editedName.text.isNotBlank()) {
                                onRenameFolder(editedName.text)
                            }
                            isEditing = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .widthIn(min = 100.dp, max = 280.dp),
                    decorationBox = { innerTextField ->
                        // No decoration - just the raw text, no indicator line
                        innerTextField()
                    }
                )
            } else {
                Text(
                    text = folder.name,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        editedName = TextFieldValue(folder.name, TextRange(folder.name.length))
                        isEditing = true
                    }
                )
            }

            // Delete folder icon - bottom right of the header card
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete folder",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Apps area - 2/3 of screen (dark background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { folderRootPos = it.positionInRoot() }
        ) {
            if (folderCellMap.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Folder is empty",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Fixed Column/Row grid with drag-and-drop (matches home screen folder)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .graphicsLayer { clip = false }
                ) {
                    for (row in 0 until gridRows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer { clip = false }
                        ) {
                            for (col in 0 until gridColumns) {
                                val cellIdx = row * gridColumns + col
                                val pkg = folderCellMap[cellIdx]
                                val cellApp = pkg?.let { p -> allApps.find { it.packageName == p } }
                                val isDragged = cellApp != null && draggedPkg == cellApp.packageName

                                // Per-cell press feedback — mirrors the home-screen folder cell
                                // pattern. `isFingerDown` shows a 25%-black overlay while the
                                // finger is on the cell, `flashOverlay` is a one-shot 40% pulse
                                // fired on long-press (matches the home-screen "did the menu
                                // arm?" flash). The combined alpha is whichever is brighter.
                                var isFingerDown by remember(cellIdx) { mutableStateOf(false) }
                                var flashOverlay by remember(cellIdx) { mutableStateOf(false) }
                                val flashAlpha by animateFloatAsState(
                                    targetValue = if (flashOverlay) 0.4f else 0f,
                                    animationSpec = if (flashOverlay) tween(durationMillis = 80)
                                        else tween(durationMillis = 150),
                                    label = "drawerFolderFlashAlpha_$cellIdx",
                                    finishedListener = { if (flashOverlay) flashOverlay = false }
                                )
                                val cellOverlayAlpha = maxOf(if (isFingerDown) 0.25f else 0f, flashAlpha)
                                // Scale-up when this cell is the long-press target (context menu
                                // open or being dragged). Same 1.265 scale as HomeFolderAppItem.
                                val cellScale by animateFloatAsState(
                                    targetValue = if (contextMenuCellIdx == cellIdx || isDragged) 1.265f else 1f,
                                    animationSpec = tween(durationMillis = 150),
                                    label = "drawerFolderCellScale_$cellIdx"
                                )
                                // Fade the app-name label out when the icon scales up — same
                                // pattern the closed drawer-folder cell (FolderItem) and the
                                // home-screen folder cell (AppGridMovement.kt:1012) use, so
                                // the larger icon doesn't visually crowd into its label.
                                val cellLabelAlpha by animateFloatAsState(
                                    targetValue = if (contextMenuCellIdx == cellIdx || isDragged) 0f else 1f,
                                    animationSpec = tween(durationMillis = 150),
                                    label = "drawerFolderCellLabelAlpha_$cellIdx"
                                )
                                // Per-cell icon bounds (already scaled to 1.265×) so AnimatedPopup
                                // can anchor the long-press menu tight to the icon — same fix
                                // applied to the home-screen folder cell + drawer folder cell.
                                var cellIconBoundsInRoot by remember(cellIdx) {
                                    mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .graphicsLayer { clip = false }
                                        .onGloballyPositioned { coords ->
                                            cellPositions[cellIdx] = coords.positionInRoot()
                                            cellSize = coords.size
                                        }
                                        .pointerInput(Unit) {
                                            val touchSlop = viewConfiguration.touchSlop
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val startPosition = down.position

                                                // Only process if touch is within this cell
                                                if (startPosition.x < 0 || startPosition.x > size.width ||
                                                    startPosition.y < 0 || startPosition.y > size.height
                                                ) return@awaitEachGesture

                                                isFingerDown = true

                                                val longPress = awaitLongPressOrCancellation(down.id)

                                                if (longPress != null) {
                                                    val currentCellApp = folderCellMap[cellIdx]?.let { p -> allApps.find { it.packageName == p } }
                                                    if (currentCellApp != null && draggedPkg == null) {
                                                        // Long press on app cell - show context menu
                                                        hapticFeedback.performLongPress()
                                                        flashOverlay = true
                                                        contextMenuCellIdx = cellIdx

                                                        var dragStarted = false
                                                        var lastDragPosition = longPress.position

                                                        try {
                                                            while (true) {
                                                                val event = awaitPointerEvent()
                                                                val change = event.changes.firstOrNull() ?: break

                                                                if (change.pressed) {
                                                                    val dx = change.position.x - startPosition.x
                                                                    val dy = change.position.y - startPosition.y
                                                                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                                                    if (distance > touchSlop && !dragStarted) {
                                                                        // Movement after long press → start drag, hide menu
                                                                        dragStarted = true
                                                                        contextMenuCellIdx = null
                                                                        draggedPkg = currentCellApp.packageName
                                                                        draggedCellIdx = cellIdx
                                                                        dragOffset = Offset.Zero
                                                                        dragOriginalCellPos = cellPositions[cellIdx]
                                                                        lastDragPosition = change.position
                                                                    }

                                                                    if (dragStarted && draggedCellIdx == cellIdx) {
                                                                        val delta = Offset(
                                                                            change.position.x - lastDragPosition.x,
                                                                            change.position.y - lastDragPosition.y
                                                                        )
                                                                        lastDragPosition = change.position
                                                                        change.consume()

                                                                        if (escapedToDrawer) {
                                                                            // Already escaped — forward movement to drawer
                                                                            val cellPos = cellPositions[cellIdx]
                                                                            if (cellPos != null) {
                                                                                val absPos = Offset(
                                                                                    cellPos.x + cellSize.width / 2f + dragOffset.x + delta.x,
                                                                                    cellPos.y + cellSize.height / 2f + dragOffset.y + delta.y
                                                                                )
                                                                                dragOffset += delta
                                                                                currentOnEscapeDragMove(absPos)
                                                                            }
                                                                        } else {
                                                                            dragOffset += delta

                                                                            // Compute hovered cell & header detection
                                                                            val cellPos = cellPositions[cellIdx]
                                                                            if (cellPos != null && cellSize.width > 0) {
                                                                                val dragCenter = Offset(
                                                                                    cellPos.x + cellSize.width / 2f + dragOffset.x,
                                                                                    cellPos.y + cellSize.height / 2f + dragOffset.y
                                                                                )

                                                                                // If dragged into header area → escape to drawer
                                                                                if (dragCenter.y < headerBottomY && currentCellApp != null) {
                                                                                    escapedToDrawer = true
                                                                                    hoveredCell = null
                                                                                    dragOverHeader = false
                                                                                    // Cell is already invisible (isDragged alpha=0) and
                                                                                    // folder overlay is fading out — no need to modify cellMap
                                                                                    // here (would crash: layout node removed mid-gesture)
                                                                                    currentOnEscapeToDrawer(currentCellApp, dragCenter)
                                                                                } else {
                                                                                    hoveredCell = cellPositions.entries.firstOrNull { (_, pos) ->
                                                                                        dragCenter.x >= pos.x && dragCenter.x < pos.x + cellSize.width &&
                                                                                        dragCenter.y >= pos.y && dragCenter.y < pos.y + cellSize.height
                                                                                    }?.key
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    // Finger released
                                                                    if (dragStarted && draggedCellIdx == cellIdx) {
                                                                        // If we escaped to drawer, forward the release
                                                                        if (escapedToDrawer) {
                                                                            escapedToDrawer = false
                                                                            draggedPkg = null
                                                                            draggedCellIdx = null
                                                                            dragOffset = Offset.Zero
                                                                            dragOriginalCellPos = null
                                                                            hoveredCell = null
                                                                            currentOnEscapeDragEnd()
                                                                            break
                                                                        }
                                                                        val droppedPkg = draggedPkg
                                                                        val fromIdx = draggedCellIdx
                                                                        val toIdx = hoveredCell
                                                                        val originalPos = dragOriginalCellPos

                                                                        if (fromIdx != null && toIdx != null && fromIdx != toIdx &&
                                                                            droppedPkg != null && originalPos != null && !isDropAnimating
                                                                        ) {
                                                                            // Animate to target cell
                                                                            val targetPos = cellPositions[toIdx]
                                                                            val targetOffset = if (targetPos != null) {
                                                                                Offset(targetPos.x - originalPos.x, targetPos.y - originalPos.y)
                                                                            } else Offset.Zero

                                                                            dropStartOffset = dragOffset
                                                                            dropTargetOffset = targetOffset
                                                                            isDropAnimating = true
                                                                            hoveredCell = null

                                                                            folderScope.launch {
                                                                                dropAnimProgress.snapTo(0f)
                                                                                dropAnimProgress.animateTo(
                                                                                    1f,
                                                                                    tween(400, easing = FastOutSlowInEasing)
                                                                                )
                                                                                // Perform swap/move
                                                                                val newMap = folderCellMap.toMutableMap()
                                                                                newMap.remove(fromIdx)
                                                                                val existingAtTarget = newMap[toIdx]
                                                                                if (existingAtTarget != null) {
                                                                                    newMap[toIdx] = droppedPkg
                                                                                    newMap[fromIdx] = existingAtTarget
                                                                                } else {
                                                                                    newMap[toIdx] = droppedPkg
                                                                                }
                                                                                folderCellMap = newMap
                                                                                saveCellMap(newMap)

                                                                                draggedPkg = null
                                                                                draggedCellIdx = null
                                                                                dragOffset = Offset.Zero
                                                                                dragOriginalCellPos = null
                                                                                isDropAnimating = false
                                                                            }
                                                                        } else {
                                                                            // Drop back to original position
                                                                            if (originalPos != null && !isDropAnimating) {
                                                                                dropStartOffset = dragOffset
                                                                                dropTargetOffset = Offset.Zero
                                                                                isDropAnimating = true
                                                                                hoveredCell = null

                                                                                folderScope.launch {
                                                                                    dropAnimProgress.snapTo(0f)
                                                                                    dropAnimProgress.animateTo(
                                                                                        1f,
                                                                                        tween(300, easing = FastOutSlowInEasing)
                                                                                    )
                                                                                    draggedPkg = null
                                                                                    draggedCellIdx = null
                                                                                    dragOffset = Offset.Zero
                                                                                    dragOriginalCellPos = null
                                                                                    isDropAnimating = false
                                                                                }
                                                                            } else {
                                                                                draggedPkg = null
                                                                                draggedCellIdx = null
                                                                                dragOffset = Offset.Zero
                                                                                dragOriginalCellPos = null
                                                                                hoveredCell = null
                                                                                dragOverHeader = false
                                                                            }
                                                                        }
                                                                    }
                                                                    break
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            if (escapedToDrawer) {
                                                                escapedToDrawer = false
                                                                currentOnEscapeDragEnd()
                                                            }
                                                            if (dragStarted && draggedCellIdx == cellIdx) {
                                                                draggedPkg = null
                                                                draggedCellIdx = null
                                                                dragOffset = Offset.Zero
                                                                dragOriginalCellPos = null
                                                                hoveredCell = null
                                                                dragOverHeader = false
                                                            }
                                                        } finally {
                                                            isFingerDown = false
                                                        }
                                                    } else {
                                                        // Long-press fired but cell wasn't an app (or
                                                        // a drag was already in progress). Still need
                                                        // to clear press feedback.
                                                        isFingerDown = false
                                                    }
                                                } else {
                                                    // Long press cancelled — either finger lifted (tap) or
                                                    // moved past slop. The UP event was the one that caused
                                                    // awaitLongPressOrCancellation to return null, so it has
                                                    // already been delivered. We must NOT call
                                                    // waitForUpOrCancellation here: that would suspend until
                                                    // the NEXT pointer event, which only arrives on the next
                                                    // tap — and the gesture closes out before that, so the
                                                    // first tap is silently dropped (issue: drawer-folder
                                                    // app icons need multiple taps to launch). Read the most
                                                    // recent change off `currentEvent` instead — same
                                                    // pattern HomeFolderAppItem in the launcher uses.
                                                    val upEvent = currentEvent.changes.firstOrNull()
                                                    if (upEvent != null && !upEvent.pressed) {
                                                        val dx = upEvent.position.x - startPosition.x
                                                        val dy = upEvent.position.y - startPosition.y
                                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                        if (dist < touchSlop * 2) {
                                                            val currentCellApp = folderCellMap[cellIdx]?.let { p -> allApps.find { it.packageName == p } }
                                                            if (currentCellApp != null && draggedPkg == null) {
                                                                flashOverlay = true
                                                                launchApp(context, currentCellApp.packageName)
                                                            }
                                                        }
                                                    }
                                                    isFingerDown = false
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Hover indicator (shown when dragging over this cell, including the original position)
                                    if (isDraggingInFolder && hoveredCell == cellIdx) {
                                        GridCellHoverIndicator(
                                            isHovered = true,
                                            markerHalfSize = markerHalfSize,
                                            cornerRadius = hoverCornerRadius
                                        )
                                    }

                                    // "+" marker at bottom-right corner (interior intersections only)
                                    val showBottomRightMarker = (row < gridRows - 1) && (col < gridColumns - 1)
                                    val markerAlpha by animateFloatAsState(
                                        targetValue = if (isDraggingInFolder && showBottomRightMarker) 1f else 0f,
                                        animationSpec = tween(durationMillis = 200),
                                        label = "markerAlpha_$cellIdx"
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
                                                fontSize = plusMarkerFont,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    // App content (hidden when being dragged - overlay renders it instead)
                                    if (cellApp != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(markerHalfSize)
                                                .graphicsLayer {
                                                    clip = false
                                                    alpha = if (isDragged) 0f else 1f
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .wrapContentHeight(unbounded = true)
                                                    .graphicsLayer { clip = false },
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val shapedPath = if (globalIconShapeName != null) {
                                                    try {
                                                        if (iconBgColor != null) getOrGenerateBgColorShapedIcon(context, cellApp.packageName, globalIconShapeName, iconBgColor)
                                                        else getOrGenerateGlobalShapedIcon(context, cellApp.packageName, globalIconShapeName)
                                                    } catch (_: Exception) { null }
                                                } else null
                                                val displayPath = shapedPath ?: cellApp.iconPath
                                                val isShaped = shapedPath != null
                                                Box(
                                                    modifier = Modifier
                                                        .size(iconSize.dp)
                                                        .onGloballyPositioned { coords ->
                                                            // Always use the final target scale (1.265f) so
                                                            // the popup anchors to where the icon WILL be,
                                                            // not where it is mid-animation.
                                                            val targetScale = 1.265f
                                                            val pos = coords.positionInRoot()
                                                            val w = coords.size.width * targetScale
                                                            val h = coords.size.height * targetScale
                                                            val offsetX = (coords.size.width - w) / 2f
                                                            val offsetY = (coords.size.height - h) / 2f
                                                            cellIconBoundsInRoot = androidx.compose.ui.geometry.Rect(
                                                                pos.x + offsetX, pos.y + offsetY,
                                                                pos.x + offsetX + w, pos.y + offsetY + h
                                                            )
                                                        }
                                                        .graphicsLayer {
                                                            scaleX = cellScale
                                                            scaleY = cellScale
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = File(displayPath),
                                                        contentDescription = cellApp.name,
                                                        contentScale = if (isShaped) ContentScale.Fit else if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .then(if (!isShaped && iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                                                    )
                                                    // Press / flash dim overlay — same alpha math as
                                                    // home-screen folder cells, scoped to the icon so
                                                    // the label below doesn't flash with it.
                                                    if (cellOverlayAlpha > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .matchParentSize()
                                                                .graphicsLayer { alpha = cellOverlayAlpha }
                                                                .then(if (iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                                                                .background(Color.Black)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = cellApp.name,
                                                    fontSize = labelFontSize,
                                                    fontFamily = labelFontFamily ?: FontFamily.Default,
                                                    color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .graphicsLayer { alpha = cellLabelAlpha },
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        shadow = androidx.compose.ui.graphics.Shadow(
                                                            color = Color.Black,
                                                            offset = Offset(1f, 1f),
                                                            blurRadius = 3f
                                                        )
                                                    )
                                                )
                                            }
                                        }

                                        // Context menu (shown on long press without drag)
                                            AnimatedPopup(
                                                visible = contextMenuCellIdx == cellIdx &&
                                                    cellIconBoundsInRoot != androidx.compose.ui.geometry.Rect.Zero,
                                                onDismissRequest = { contextMenuCellIdx = null },
                                                iconBoundsInRoot = cellIconBoundsInRoot
                                            ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .defaultMinSize(minHeight = 48.dp)
                                                                .padding(horizontal = 16.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = cellApp.name,
                                                                fontWeight = FontWeight.Bold,
                                                                lineHeight = 22.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Divider()
                                                        DropdownMenuItem(
                                                            text = { Text("Add to home") },
                                                            leadingIcon = {
                                                                Icon(Icons.Outlined.Home, contentDescription = null)
                                                            },
                                                            onClick = {
                                                                contextMenuCellIdx = null
                                                                onAddToHome(cellApp)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Remove from folder") },
                                                            leadingIcon = {
                                                                Icon(Icons.Outlined.Delete, contentDescription = null)
                                                            },
                                                            onClick = {
                                                                contextMenuCellIdx = null
                                                                onRemoveApp(cellApp.packageName)
                                                            }
                                                        )
                                                        val otherFolders = folders.filter { it.id != folder.id }
                                                        if (otherFolders.isNotEmpty()) {
                                                            otherFolders.forEach { targetFolder ->
                                                                DropdownMenuItem(
                                                                    text = { Text("Move to ${targetFolder.name}") },
                                                                    leadingIcon = {
                                                                        Icon(Icons.Outlined.Folder, contentDescription = null)
                                                                    },
                                                                    onClick = {
                                                                        contextMenuCellIdx = null
                                                                        onMoveToFolder(cellApp.packageName, targetFolder)
                                                                    }
                                                                )
                                                            }
                                                        }
                                                        DropdownMenuItem(
                                                            text = { Text("App info") },
                                                            leadingIcon = {
                                                                Icon(Icons.Outlined.Info, contentDescription = null)
                                                            },
                                                            onClick = {
                                                                contextMenuCellIdx = null
                                                                onAppInfo(cellApp)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Uninstall") },
                                                            leadingIcon = {
                                                                Icon(Icons.Outlined.Delete, contentDescription = null)
                                                            },
                                                            onClick = {
                                                                contextMenuCellIdx = null
                                                                onUninstallApp(cellApp)
                                                            }
                                                        )
                                            }
                                    } else {
                                        // Empty cell — show hover indicator when dragged over
                                        GridCellHoverIndicator(
                                            isHovered = isDraggingInFolder && hoveredCell == cellIdx,
                                            markerHalfSize = markerHalfSize,
                                            cornerRadius = hoverCornerRadius
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Drag overlay — floating above the grid (hidden when escaped to drawer)
                if (draggedPkg != null && !escapedToDrawer) {
                    val draggedApp = allApps.find { it.packageName == draggedPkg }
                    val originalCellPos = dragOriginalCellPos
                    if (draggedApp != null && originalCellPos != null && cellSize.width > 0) {
                        val p = if (isDropAnimating) dropAnimProgress.value else 0f
                        val currentOffset = if (isDropAnimating) {
                            dropStartOffset + (dropTargetOffset - dropStartOffset) * p
                        } else dragOffset
                        val boxScale = 1.265f - 0.265f * p
                        val boxAlpha = 0.8f + 0.2f * p
                        val overlayTextAlpha = if (isDropAnimating) p else 0f

                        val appLeft = originalCellPos.x - folderRootPos.x + currentOffset.x
                        val appTop = originalCellPos.y - folderRootPos.y + currentOffset.y

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(appLeft.toInt(), appTop.toInt()) }
                                .size(
                                    width = with(density) { cellSize.width.toDp() },
                                    height = with(density) { cellSize.height.toDp() }
                                )
                                .zIndex(1000f)
                                .padding(markerHalfSize)
                                .graphicsLayer {
                                    scaleX = boxScale
                                    scaleY = boxScale
                                    alpha = boxAlpha
                                    clip = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .wrapContentHeight(unbounded = true)
                                    .graphicsLayer { clip = false },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = File(draggedApp.iconPath),
                                    contentDescription = draggedApp.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(iconSize.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = draggedApp.name,
                                    fontSize = labelFontSize,
                                    fontFamily = labelFontFamily ?: FontFamily.Default,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { alpha = overlayTextAlpha },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black,
                                            offset = Offset(1f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Folder?") },
            text = { Text("Apps inside this folder (${folder.name}) will be moved back to the drawer.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder()
                    showDeleteConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
