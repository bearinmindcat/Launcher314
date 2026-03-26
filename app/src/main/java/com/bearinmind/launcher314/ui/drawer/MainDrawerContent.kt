package com.bearinmind.launcher314.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.EscapeHoverState
import com.bearinmind.launcher314.data.HomeDragCallbacks
import com.bearinmind.launcher314.data.SortOption
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarHeightPercent
import com.bearinmind.launcher314.data.getScrollbarIntensity
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.getReverseDrawerSearchBar
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.bearinmind.launcher314.data.getAutoOpenKeyboard
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import com.bearinmind.launcher314.ui.components.LazyGridScrollbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun MainDrawerContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit = {},
    isDrawerFullyOpen: Boolean = false,
    dismissSearchTrigger: Int = 0,
    isLoading: Boolean,
    folders: List<AppFolder>,
    filteredApps: List<AppInfo>,
    allApps: List<AppInfo>,
    gridSize: Int,
    iconSize: Int,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    iconClipShape: androidx.compose.ui.graphics.Shape? = null,
    iconBgColor: Int? = null,
    globalIconShapeName: String? = null,
    drawerGridRows: Int,
    isPagedMode: Boolean,
    currentSortOption: SortOption,
    onSortOptionChanged: (SortOption) -> Unit,
    isSortAscending: Boolean,
    onSortDirectionChanged: (Boolean) -> Unit,
    onFolderClick: (AppFolder) -> Unit,
    onAppClick: (String) -> Unit,
    onUninstallApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onCreateFolderWithApps: (List<AppInfo>) -> Unit = {},
    onFolderPositioned: (String, Offset) -> Unit = { _, _ -> },
    onAddAppToFolder: (AppInfo, AppFolder) -> Unit = { _, _ -> },
    onDeleteFolder: (AppFolder) -> Unit = {},
    onAddToHome: (AppInfo) -> Unit = {},
    onAddFolderToHome: (AppFolder) -> Unit = {},
    homeDragCallbacks: HomeDragCallbacks = HomeDragCallbacks(),
    onBulkAddToFolder: (List<AppInfo>, AppFolder) -> Unit = { _, _ -> },
    isFolderMenuExpanded: Boolean = false,
    onFolderMenuExpandedChange: (Boolean) -> Unit = {},
    clearSelectionTrigger: Int = 0,
    dropAnimatingPackage: String? = null,
    onDropTargetPositioned: (Offset, IntSize) -> Unit = { _, _ -> },
    escapeHoverState: EscapeHoverState? = null,
    onCustomizeApp: (AppInfo) -> Unit = {}
) {
    val onDragToHome = homeDragCallbacks.onDragToHome
    val onDragToHomeMove = homeDragCallbacks.onDragToHomeMove
    val onDragToHomeDrop = homeDragCallbacks.onDragToHomeDrop
    var showMenu by remember { mutableStateOf(false) }

    // Multi-select state
    var selectedAppPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Selection mode is explicitly activated by tapping "Select" in the context menu
    var selectionModeActive by remember { mutableStateOf(false) }

    // Clear selection when triggered by parent (e.g., after folder creation)
    LaunchedEffect(clearSelectionTrigger) {
        if (clearSelectionTrigger > 0) {
            selectedAppPackages = emptySet()
            selectionModeActive = false
        }
    }

    val reverseSearchBar = getReverseDrawerSearchBar(LocalContext.current)

    // Track cell positions and sizes for drag overlay positioning (shared across paged/scroll modes)
    // Hoisted here so drag lambdas can access them for folder hover detection
    val drawerCellPositions = remember { mutableStateMapOf<String, Offset>() }
    val drawerCellSizes = remember { mutableStateMapOf<String, IntSize>() }

    // Drag-and-drop state for paged mode (return-to-origin only)
    var showDropZone by remember { mutableStateOf(false) } // drives crossfade, cleared on finger lift
    var drawerDraggedItem by remember { mutableStateOf<Any?>(null) } // AppInfo or AppFolder
    var drawerDragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var drawerDragOffset by remember { mutableStateOf(Offset.Zero) }
    var drawerIsDropAnimating by remember { mutableStateOf(false) }
    val drawerDropAnim = remember { Animatable(0f) }
    var drawerDragCurrentOffset by remember { mutableStateOf(Offset.Zero) } // snapshot for drop lerp
    var drawerDragCellSize by remember { mutableStateOf(IntSize.Zero) }
    val drawerDragScope = rememberCoroutineScope()
    val drawerHaptic = rememberHapticFeedback()

    // Drop zone state (for drag-to-home-screen)
    var dropZoneBounds by remember { mutableStateOf(Rect.Zero) }
    var isDropZoneHovered by remember { mutableStateOf(false) }
    // True after the item has been transferred to the home screen — gesture continues
    // forwarding position updates (like folder escape pattern)
    var transferredToHome by remember { mutableStateOf(false) }
    // Pending transfer job: 450ms delay before transitioning to home screen
    var pendingTransferJob by remember { mutableStateOf<Job?>(null) }

    // Folder hover state (for drag-into-folder)
    var hoveredFolderKey by remember { mutableStateOf<String?>(null) }
    var folderDropAnimating by remember { mutableStateOf(false) }
    var folderDropTargetPos by remember { mutableStateOf(Offset.Zero) } // folder center in root coords

    // Shared drag move/end lambdas (cell-agnostic)
    val drawerDragMove: (Offset) -> Unit = { delta ->
        drawerDragOffset += delta
        if (transferredToHome) {
            // Gesture still alive — forward cell center to home screen
            val cellCenter = Offset(drawerDragCellSize.width / 2f, drawerDragCellSize.height / 2f)
            onDragToHomeMove(drawerDragStartOffset + drawerDragOffset + cellCenter)
        } else if (drawerDraggedItem != null && dropZoneBounds != Rect.Zero) {
            // Check if drag overlay is over the drop zone
            val overlayPos = drawerDragStartOffset + drawerDragOffset
            // In reverse mode, use bottom-center of cell for hit testing (dragging down to bottom zone)
            val hitTestPos = if (reverseSearchBar) {
                Offset(overlayPos.x + drawerDragCellSize.width / 2f, overlayPos.y + drawerDragCellSize.height)
            } else {
                overlayPos
            }
            val wasHovered = isDropZoneHovered
            isDropZoneHovered = dropZoneBounds.contains(hitTestPos)
            // Delayed transition when entering the zone (450ms grace period)
            if (isDropZoneHovered && !wasHovered) {
                hoveredFolderKey = null // clear folder hover immediately
                pendingTransferJob?.cancel()
                pendingTransferJob = drawerDragScope.launch {
                    delay(600)
                    // Still in the zone after delay — transfer to home screen
                    if (isDropZoneHovered && drawerDraggedItem != null) {
                        val item = drawerDraggedItem!!
                        val cellCenter = Offset(drawerDragCellSize.width / 2f, drawerDragCellSize.height / 2f)
                        val pos = drawerDragStartOffset + drawerDragOffset
                        onDragToHome(item, pos + cellCenter)
                        transferredToHome = true
                    }
                }
            } else if (!isDropZoneHovered && wasHovered) {
                // Dragged back out of zone before delay — cancel pending transfer
                pendingTransferJob?.cancel()
                pendingTransferJob = null
            }

            // Check folder/app hover (only for AppInfo being dragged, not folders)
            if (!transferredToHome && !isDropZoneHovered && drawerDraggedItem is AppInfo) {
                val overlayCenterX = overlayPos.x + drawerDragCellSize.width / 2f
                val overlayCenterY = overlayPos.y + drawerDragCellSize.height / 2f
                val overlayCenter = Offset(overlayCenterX, overlayCenterY)
                val draggedPkg = (drawerDraggedItem as AppInfo).packageName

                val previousHovered = hoveredFolderKey
                // First check folders
                hoveredFolderKey = drawerCellPositions.entries.firstOrNull { (key, pos) ->
                    if (!key.startsWith("folder_")) return@firstOrNull false
                    val size = drawerCellSizes[key] ?: return@firstOrNull false
                    val folderId = key.removePrefix("folder_")
                    val folder = folders.firstOrNull { it.id == folderId } ?: return@firstOrNull false
                    if (draggedPkg in folder.appPackageNames) return@firstOrNull false
                    val bounds = Rect(pos.x, pos.y, pos.x + size.width.toFloat(), pos.y + size.height.toFloat())
                    bounds.contains(overlayCenter)
                }?.key

                // If not hovering a folder, check if hovering another app (for folder creation)
                if (hoveredFolderKey == null) {
                    hoveredFolderKey = drawerCellPositions.entries.firstOrNull { (key, pos) ->
                        if (!key.startsWith("app_")) return@firstOrNull false
                        val hoveredPkg = key.removePrefix("app_")
                        if (hoveredPkg == draggedPkg) return@firstOrNull false // Can't drop on self
                        val size = drawerCellSizes[key] ?: return@firstOrNull false
                        val bounds = Rect(pos.x, pos.y, pos.x + size.width.toFloat(), pos.y + size.height.toFloat())
                        bounds.contains(overlayCenter)
                    }?.key
                }

                if (hoveredFolderKey != previousHovered && hoveredFolderKey != null) {
                    drawerHaptic.performTextHandleMove()
                }
            } else if (transferredToHome || isDropZoneHovered) {
                hoveredFolderKey = null
            }
        }
    }
    val drawerDragEnd: () -> Unit = {
        showDropZone = false // Start crossfade back to search bar immediately on finger lift
        pendingTransferJob?.cancel()
        pendingTransferJob = null
        if (transferredToHome) {
            // Finger lifted after transfer — signal drop to home screen
            onDragToHomeDrop()
            transferredToHome = false
            drawerDraggedItem = null
            drawerDragOffset = Offset.Zero
            isDropZoneHovered = false
            hoveredFolderKey = null
        } else if (hoveredFolderKey != null && drawerDraggedItem is AppInfo) {
            val app = drawerDraggedItem as AppInfo
            val isAppDrop = hoveredFolderKey!!.startsWith("app_")

            if (isAppDrop) {
                // Finger lifted on another app — create a new folder with both apps
                val targetPkg = hoveredFolderKey!!.removePrefix("app_")
                val targetApp = filteredApps.find { it.packageName == targetPkg }
                val targetPos = drawerCellPositions[hoveredFolderKey!!] ?: Offset.Zero
                val targetSize = drawerCellSizes[hoveredFolderKey!!] ?: IntSize.Zero
                folderDropTargetPos = Offset(
                    targetPos.x + targetSize.width / 2f,
                    targetPos.y + targetSize.height / 2f
                )
                drawerDragCurrentOffset = drawerDragOffset
                folderDropAnimating = true
                drawerIsDropAnimating = true
                drawerDragScope.launch {
                    drawerDropAnim.snapTo(0f)
                    drawerDropAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                    if (targetApp != null) {
                        onCreateFolderWithApps(listOf(app, targetApp))
                    }
                    drawerDraggedItem = null
                    drawerDragOffset = Offset.Zero
                    drawerDragCurrentOffset = Offset.Zero
                    drawerIsDropAnimating = false
                    folderDropAnimating = false
                    hoveredFolderKey = null
                }
            } else {
                // Finger lifted on a folder — add app to folder with shrink animation
                val folderId = hoveredFolderKey!!.removePrefix("folder_")
                val targetFolder = folders.firstOrNull { it.id == folderId }
                val targetFolderPos = drawerCellPositions[hoveredFolderKey!!] ?: Offset.Zero
                val targetFolderSize = drawerCellSizes[hoveredFolderKey!!] ?: IntSize.Zero
                folderDropTargetPos = Offset(
                    targetFolderPos.x + targetFolderSize.width / 2f,
                    targetFolderPos.y + targetFolderSize.height / 2f
                )
                // Animate overlay shrinking into the folder
                drawerDragCurrentOffset = drawerDragOffset
                folderDropAnimating = true
                drawerIsDropAnimating = true
                drawerDragScope.launch {
                    drawerDropAnim.snapTo(0f)
                    drawerDropAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                    // Add app to folder after animation
                    if (targetFolder != null) {
                        onAddAppToFolder(app, targetFolder)
                    }
                drawerDraggedItem = null
                drawerDragOffset = Offset.Zero
                drawerDragCurrentOffset = Offset.Zero
                drawerIsDropAnimating = false
                folderDropAnimating = false
                hoveredFolderKey = null
            }
            } // close isAppDrop else
        } else if (isDropZoneHovered && drawerDraggedItem != null) {
            // Finger lifted on the zone — transition to home
            val cellCenter = Offset(drawerDragCellSize.width / 2f, drawerDragCellSize.height / 2f)
            onDragToHome(drawerDraggedItem!!, drawerDragStartOffset + drawerDragOffset + cellCenter)
            onDragToHomeDrop() // Immediate drop (finger is already up)
            drawerDraggedItem = null
            drawerDragOffset = Offset.Zero
            isDropZoneHovered = false
            hoveredFolderKey = null
        } else {
            // Return-to-origin animation
            drawerDragCurrentOffset = drawerDragOffset
            drawerIsDropAnimating = true
            hoveredFolderKey = null
            drawerDragScope.launch {
                drawerDropAnim.snapTo(0f)
                drawerDropAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                drawerDraggedItem = null
                drawerDragOffset = Offset.Zero
                drawerDragCurrentOffset = Offset.Zero
                drawerIsDropAnimating = false
            }
        }
    }

    val autoOpenKeyboard = getAutoOpenKeyboard(LocalContext.current)
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    // Dismiss search when triggered by parent (swipe down at top)
    LaunchedEffect(dismissSearchTrigger) {
        if (dismissSearchTrigger > 0) {
            focusManager.clearFocus()
            isSearchFocused = false
            onSearchQueryChange("")
            // Delay before telling parent search is inactive,
            // so layout fully restores before drawer can be swiped closed
            kotlinx.coroutines.delay(800)
            onSearchFocusChanged(false)
        }
    }

    // Auto-focus search bar only when drawer is fully open
    LaunchedEffect(isDrawerFullyOpen) {
        if (isDrawerFullyOpen && autoOpenKeyboard) {
            kotlinx.coroutines.delay(200)
            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }


    // Detect keyboard dismissal (e.g. back button) and clear focus + search
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen && isSearchFocused) {
            focusManager.clearFocus()
            isSearchFocused = false
            onSearchQueryChange("")
            // Delay before telling parent search is inactive,
            // so layout fully restores before drawer can be swiped closed
            kotlinx.coroutines.delay(800)
            onSearchFocusChanged(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(if (isKeyboardOpen) Modifier.imePadding() else Modifier)
            .graphicsLayer { clip = false }
    ) {
        // Search bar content — extracted so it can be placed at top or bottom
        val searchBarBlock: @Composable () -> Unit = {
        // Search bar and drop zone — stacked with animated alpha.
        // The grey search bar background fades from edges inward, revealing the drawer bg.
        // Combine normal drag and escape drag signals for drop zone visibility
        val effectiveShowDropZone = showDropZone || (escapeHoverState?.isEscapeDragActive == true)
        val searchBarAlpha by animateFloatAsState(
            targetValue = if (effectiveShowDropZone) 0f else 1f,
            animationSpec = tween(450),
            label = "searchBarAlpha"
        )
        val dropZoneAlpha by animateFloatAsState(
            targetValue = if (effectiveShowDropZone) 1f else 0f,
            animationSpec = tween(450),
            label = "dropZoneAlpha"
        )
        // Edge fade: when drop zone shows, left 25% and right 25% of grey bg fade out
        val edgeFade by animateFloatAsState(
            targetValue = if (effectiveShowDropZone) 0f else 1f,
            animationSpec = tween(450),
            label = "edgeFade"
        )
        // Smooth hover highlight animation
        val dropZoneHoverFraction by animateFloatAsState(
            targetValue = if (isDropZoneHovered || escapeHoverState?.isInDropZone == true) 1f else 0f,
            animationSpec = tween(150),
            label = "dropZoneHover"
        )
        // Single outer Box — one shared grey background, content crossfades on top
        val dropZoneDensity = LocalDensity.current
        val dropZoneScreenH = with(dropZoneDensity) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val bounds = if (reverseSearchBar) {
                        // Reverse mode: drop zone from top of search bar to bottom of screen
                        Rect(
                            pos.x, pos.y,
                            pos.x + coords.size.width, dropZoneScreenH
                        )
                    } else {
                        // Normal mode: drop zone extends from top of screen to bottom of search bar
                        Rect(
                            pos.x, 0f,
                            pos.x + coords.size.width, pos.y + coords.size.height
                        )
                    }
                    dropZoneBounds = bounds
                    escapeHoverState?.dropZoneBoundsRef?.value = bounds
                }
        ) {
            // Shared grey background — left/right/top edges fade out when drop zone shows
            val centerColor = lerp(Color(0xFF3B3B3B), Color(0xFF4A4A4A), dropZoneHoverFraction * if (dropZoneAlpha > 0.5f) 1f else 0f)
            val edgeColor = centerColor.copy(alpha = edgeFade)
            val cornerRadius = 24.dp
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .clip(RoundedCornerShape(cornerRadius))
                    .drawBehind {
                        // Base: horizontal gradient (left/right edge fade)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to edgeColor,
                                0.25f to centerColor,
                                0.75f to centerColor,
                                1.0f to edgeColor
                            )
                        )
                        // Mask: top half fades out, bottom half solid
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = edgeFade),
                                0.50f to Color.White,
                                1.0f to Color.White
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
            // Drop zone content (fades in when dragging)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 4.dp)
                    .graphicsLayer { alpha = dropZoneAlpha },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f + 0.4f * dropZoneHoverFraction)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to Home Screen",
                        color = Color.White.copy(alpha = 0.6f + 0.4f * dropZoneHoverFraction),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            // Search bar (fades out when dragging — transparent container, shared bg behind)
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                enabled = searchBarAlpha > 0.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .graphicsLayer { alpha = searchBarAlpha }
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onSearchFocusChanged(true)
                        }
                        // Don't call onSearchFocusChanged(false) here —
                        // the LaunchedEffect handles it with a delay so the
                        // drawer doesn't close before layout restores
                    },
                placeholder = { Text("Search", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                        // Menu button with combined dropdown
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }
                            // Combined dropdown menu
                                AnimatedPopup(
                                    visible = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    gapDp = 4
                                ) {
                                            // Sorting section header with direction toggle
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .clickable { onSortDirectionChanged(!isSortAscending) }
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Sorting",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Icon(
                                                        imageVector = if (isSortAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                                        contentDescription = if (isSortAscending) "Ascending" else "Descending",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Sort options
                                            SortOption.values().forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.displayName) },
                                                    onClick = {
                                                        onSortOptionChanged(option)
                                                    },
                                                    leadingIcon = {
                                                        val isSelected = currentSortOption == option
                                                        Icon(
                                                            imageVector = when (option) {
                                                                SortOption.MANUAL -> Icons.Outlined.Star
                                                                SortOption.UPDATED -> Icons.Outlined.Update
                                                                SortOption.NAME -> Icons.Outlined.SortByAlpha
                                                                SortOption.INSTALLED -> Icons.Outlined.GetApp
                                                                SortOption.SIZE -> Icons.Outlined.Storage
                                                            },
                                                            contentDescription = null,
                                                            modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        if (currentSortOption == option) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Check,
                                                                contentDescription = "Selected",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                                            DropdownMenuItem(
                                                text = { Text("Create folder") },
                                                onClick = {
                                                    showMenu = false
                                                    onCreateFolderClick()
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Settings") },
                                                onClick = {
                                                    showMenu = false
                                                    onSettingsClick()
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Settings, contentDescription = null)
                                                }
                                            )
                                }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = Color.White,
                    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                    disabledLeadingIconColor = Color.White.copy(alpha = 0.7f),
                    focusedTrailingIconColor = Color.White,
                    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                    disabledTrailingIconColor = Color.White.copy(alpha = 0.7f),
                    disabledPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White
                )
            )
        }
        }

        if (!reverseSearchBar) searchBarBlock()

        var drawerGridRootPos by remember { mutableStateOf(Offset.Zero) }

        // Shared wrapper Box for drag overlay to float above both modes
        var drawerWrapperRootPos by remember { mutableStateOf(Offset.Zero) }
        Box(modifier = Modifier
            .weight(1f)
            .zIndex(if (drawerDraggedItem != null) 1000f else 0f)
            .onGloballyPositioned { drawerWrapperRootPos = it.positionInRoot() }
        ) {
        // Loading or App grid
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (isPagedMode) {
            // Paged mode: HorizontalPager with fixed grid pages
            val itemsPerPage = gridSize * drawerGridRows

            // Read scrollbar settings for navigation dots
            val dotContext = LocalContext.current
            val dotBaseColor = getScrollbarColor(dotContext)
            val dotIntensity = getScrollbarIntensity(dotContext)
            val dotSize = (LocalConfiguration.current.screenWidthDp * 0.02f * getScrollbarWidthPercent(dotContext) / 100f).dp
            val dotColor = remember(dotBaseColor, dotIntensity) {
                val base = Color(dotBaseColor)
                val factor = (dotIntensity / 100f).coerceIn(0f, 1f)
                Color(
                    red = base.red * factor,
                    green = base.green * factor,
                    blue = base.blue * factor,
                    alpha = base.alpha
                )
            }

            // Combine folders and apps into a single display list
            // Folders go first, then apps
            val allDisplayItems = remember(folders, filteredApps, searchQuery, allApps) {
                val items = mutableListOf<Any>()
                if (searchQuery.isBlank()) {
                    // Show all folders when not searching
                    items.addAll(folders)
                } else {
                    // Only show folders that contain apps matching the search query
                    items.addAll(folders.filter { folder ->
                        folder.appPackageNames.any { pkg ->
                            allApps.any { app ->
                                app.packageName == pkg && app.name.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    })
                }
                items.addAll(filteredApps)
                items
            }

            val pageCount = if (allDisplayItems.isEmpty()) 1
                else (allDisplayItems.size + itemsPerPage - 1) / itemsPerPage

            val pagerState = rememberPagerState(pageCount = { pageCount })

            // Scroll to page 0 when search results change (e.g. user types in search bar)
            LaunchedEffect(allDisplayItems.size) {
                if (pagerState.currentPage != 0) {
                    pagerState.scrollToPage(0)
                }
            }

            // Cancel any active drag when pager scrolls (prevents app from "disappearing")
            LaunchedEffect(pagerState.currentPage) {
                if (drawerDraggedItem != null && !transferredToHome) {
                    drawerDraggedItem = null
                    drawerDragOffset = Offset.Zero
                    showDropZone = false
                    isDropZoneHovered = false
                    hoveredFolderKey = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!reverseSearchBar) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                    .onGloballyPositioned { drawerGridRootPos = it.positionInRoot() }
            ) {
                // Pager + nav dots always rendered
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollable(
                            state = rememberScrollableState { 0f },
                            orientation = Orientation.Vertical
                        )
                ) {
                    // Calculate cell height from screen dimensions (never changes with keyboard)
                    val screenH = LocalConfiguration.current.screenHeightDp.dp
                    val statusBarH = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val navBarH = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    // Non-grid space: status bar + search bar (~72dp) + nav dots (~30dp) + nav bar + padding (~16dp)
                    val nonGridSpace = statusBarH + navBarH + 118.dp
                    val stableCellH = if (drawerGridRows > 0) (screenH - nonGridSpace) / drawerGridRows else 80.dp

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pageIndex ->
                        val startIndex = pageIndex * itemsPerPage
                        val endIndex = minOf(startIndex + itemsPerPage, allDisplayItems.size)
                        val pageItems = if (startIndex < allDisplayItems.size) {
                            allDisplayItems.subList(startIndex, endIndex)
                        } else {
                            emptyList()
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                        val cellHeightDp = stableCellH
                        val pageGridState = rememberLazyGridState()

                        // Grid for this page — scrollable when search is focused
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridSize),
                            state = pageGridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            userScrollEnabled = isSearchFocused
                        ) {
                            // Fill with actual items + empty spacer items to fill the grid
                            val totalCells = itemsPerPage
                            items(
                                count = totalCells,
                                key = { idx ->
                                    val cellItem = if (idx < pageItems.size) pageItems[idx] else null
                                    when (cellItem) {
                                        is AppInfo -> "app_${cellItem.packageName}"
                                        is AppFolder -> "folder_${cellItem.id}"
                                        else -> "empty_${pageIndex}_$idx"
                                    }
                                }
                            ) { idx ->
                                val cellItem = if (idx < pageItems.size) pageItems[idx] else null
                                val cellKey = when (cellItem) {
                                    is AppInfo -> "app_${cellItem.packageName}"
                                    is AppFolder -> "folder_${cellItem.id}"
                                    else -> null
                                }
                                val isDragTarget = cellItem != null && cellItem == drawerDraggedItem

                                val cellDragStart: () -> Unit = {
                                    if (cellItem != null && drawerDraggedItem == null) {
                                        drawerDraggedItem = cellItem
                                        showDropZone = true
                                        val cellPos = cellKey?.let { drawerCellPositions[it] } ?: Offset.Zero
                                        drawerDragStartOffset = cellPos
                                        drawerDragOffset = Offset.Zero
                                        drawerDragCellSize = cellKey?.let { drawerCellSizes[it] } ?: IntSize.Zero
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .animateItemPlacement(tween(300))
                                        .height(cellHeightDp)
                                        .onGloballyPositioned { coords ->
                                            if (cellKey != null) {
                                                drawerCellPositions[cellKey] = coords.positionInRoot()
                                                drawerCellSizes[cellKey] = coords.size
                                                if (cellItem is AppInfo && cellItem.packageName == dropAnimatingPackage) {
                                                    onDropTargetPositioned(coords.positionInRoot(), coords.size)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cellItem != null) {
                                        val isDropAnimTarget = cellItem is AppInfo && cellItem.packageName == dropAnimatingPackage
                                        Box(
                                            modifier = Modifier.graphicsLayer {
                                                alpha = if (isDragTarget || isDropAnimTarget) 0f else 1f
                                            }
                                        ) {
                                            if (cellItem is AppFolder) {
                                                FolderItem(
                                                    folder = cellItem,
                                                    allApps = allApps,
                                                    iconSize = iconSize,
                                                    labelFontSize = labelFontSize,
                                                    labelFontFamily = labelFontFamily,
                                                    onClick = { onFolderClick(cellItem) },
                                                    onPositioned = { position -> onFolderPositioned(cellItem.id, position) },
                                                    onDelete = { onDeleteFolder(cellItem) },
                                                    onAddToHome = { onAddFolderToHome(cellItem) },
                                                    onDragStarted = if (!selectionModeActive) cellDragStart else null,
                                                    onDragMoved = if (!selectionModeActive) drawerDragMove else null,
                                                    onDragEnded = if (!selectionModeActive) drawerDragEnd else null,
                                                    isDragHovered = hoveredFolderKey == "folder_${cellItem.id}" ||
                                                        escapeHoverState?.folderId == cellItem.id,
                                                    draggedIconPath = if (hoveredFolderKey == "folder_${cellItem.id}" && drawerDraggedItem is AppInfo)
                                                        (drawerDraggedItem as AppInfo).iconPath
                                                    else if (escapeHoverState?.folderId == cellItem.id) escapeHoverState.iconPath
                                                    else null
                                                )
                                            } else if (cellItem is AppInfo) {
                                                val selectedApps = filteredApps.filter { it.packageName in selectedAppPackages }
                                                SelectableAppItem(
                                                    app = cellItem,
                                                    iconSize = iconSize,
                                                    labelFontSize = labelFontSize,
                                                    labelFontFamily = labelFontFamily,
                                                    iconClipShape = iconClipShape,
                                                    iconBgColor = iconBgColor,
                                                    globalIconShapeName = globalIconShapeName,
                                                    folderPreviewDraggedIconPath = if (cellKey != null && hoveredFolderKey == cellKey && drawerDraggedItem is AppInfo) (drawerDraggedItem as AppInfo).iconPath else null,
                                                    onClick = {
                                                        if (selectionModeActive) {
                                                            selectedAppPackages = if (cellItem.packageName in selectedAppPackages) {
                                                                selectedAppPackages - cellItem.packageName
                                                            } else {
                                                                selectedAppPackages + cellItem.packageName
                                                            }
                                                            if (selectedAppPackages.isEmpty()) {
                                                                selectionModeActive = false
                                                            }
                                                        } else {
                                                            onAppClick(cellItem.packageName)
                                                        }
                                                    },
                                                    onUninstall = { onUninstallApp(cellItem) },
                                                    onAppInfo = { onAppInfo(cellItem) },
                                                    onAddToHome = { onAddToHome(cellItem) },
                                                    folders = folders,
                                                    onAddToFolder = { folder -> onAddAppToFolder(cellItem, folder) },
                                                    onCreateFolderWithApps = { apps ->
                                                        onCreateFolderWithApps(apps)
                                                    },
                                                    onDeleteFolder = onDeleteFolder,
                                                    isFolderMenuExpanded = isFolderMenuExpanded,
                                                    onFolderMenuExpandedChange = onFolderMenuExpandedChange,
                                                    isSelected = cellItem.packageName in selectedAppPackages,
                                                    onSelectToggle = {
                                                        selectionModeActive = true
                                                        selectedAppPackages = if (cellItem.packageName in selectedAppPackages) {
                                                            selectedAppPackages - cellItem.packageName
                                                        } else {
                                                            selectedAppPackages + cellItem.packageName
                                                        }
                                                        if (selectedAppPackages.isEmpty()) {
                                                            selectionModeActive = false
                                                        }
                                                    },
                                                    selectionModeActive = selectionModeActive,
                                                    selectedCount = selectedAppPackages.size,
                                                    selectedApps = selectedApps,
                                                    onBulkAddToFolder = { folder ->
                                                        onBulkAddToFolder(selectedApps, folder)
                                                        selectedAppPackages = emptySet()
                                                        selectionModeActive = false
                                                    },
                                                    onBulkAddToHome = {
                                                        selectedApps.forEach { onAddToHome(it) }
                                                        selectedAppPackages = emptySet()
                                                        selectionModeActive = false
                                                    },
                                                    onDragStarted = if (!selectionModeActive) cellDragStart else null,
                                                    onDragMoved = if (!selectionModeActive) drawerDragMove else null,
                                                    onDragEnded = if (!selectionModeActive) drawerDragEnd else null,
                                                    onCustomize = { onCustomizeApp(cellItem) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Scrollbar — only visible when search is focused and page is scrollable
                        if (isSearchFocused) {
                            val sbCtx = LocalContext.current
                            val sbWPct = getScrollbarWidthPercent(sbCtx)
                            val sbHPct = getScrollbarHeightPercent(sbCtx)
                            val sbSW = LocalConfiguration.current.screenWidthDp.toFloat()
                            val sbSH = LocalConfiguration.current.screenHeightDp.toFloat()
                            val sbW = (sbSW * 0.02f * sbWPct / 100f).toInt()
                            val sbH = (sbSH * 0.20f * sbHPct / 100f).toInt()
                            val sbColor = getScrollbarColor(sbCtx)
                            val sbInt = getScrollbarIntensity(sbCtx)
                            val sbAdj = run {
                                val b = Color(sbColor); val f = (sbInt / 100f).coerceIn(0f, 1f)
                                Color(red = b.red * f, green = b.green * f, blue = b.blue * f, alpha = b.alpha)
                            }
                            LazyGridScrollbar(
                                gridState = pageGridState,
                                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()
                                    .padding(top = 8.dp, bottom = 8.dp, end = 4.dp),
                                thumbColor = sbAdj.copy(alpha = 0.3f),
                                thumbSelectedColor = sbAdj.copy(alpha = 0.9f),
                                trackColor = Color.Transparent,
                                thumbWidth = sbW.dp, thumbMinHeight = sbH.dp, scrollbarPadding = 0.dp,
                            )
                        }
                    }
                    }

                    // Page indicator dots
                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, top = if (isKeyboardOpen) 20.dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            repeat(pageCount) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage)
                                                dotColor.copy(alpha = 0.9f)
                                            else
                                                dotColor.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Scroll mode: LazyVerticalGrid with scrollbar
            val gridState = rememberLazyGridState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { drawerGridRootPos = it.positionInRoot() }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridSize),
                    contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Show folders first (filter to only matching folders during search)
                    val displayFolders = if (searchQuery.isBlank()) folders else folders.filter { folder ->
                        folder.appPackageNames.any { pkg ->
                            allApps.any { app ->
                                app.packageName == pkg && app.name.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    items(
                        items = displayFolders,
                        key = { "folder_${it.id}" },
                        contentType = { "folder" }
                    ) { folder ->
                        val cellKey = "folder_${folder.id}"
                        val isDragTarget = drawerDraggedItem == folder
                        val cellDragStart: () -> Unit = {
                            if (drawerDraggedItem == null) {
                                drawerDraggedItem = folder
                                showDropZone = true
                                drawerDragStartOffset = drawerCellPositions[cellKey] ?: Offset.Zero
                                drawerDragOffset = Offset.Zero
                                drawerDragCellSize = drawerCellSizes[cellKey] ?: IntSize.Zero
                            }
                        }
                        Box(
                            modifier = Modifier
                                .animateItemPlacement(tween(300))
                                .onGloballyPositioned { coords ->
                                    drawerCellPositions[cellKey] = coords.positionInRoot()
                                    drawerCellSizes[cellKey] = coords.size
                                }
                                .graphicsLayer {
                                    alpha = if (isDragTarget) 0f else 1f
                                }
                        ) {
                            FolderItem(
                                folder = folder,
                                allApps = allApps,
                                iconSize = iconSize,
                                labelFontSize = labelFontSize,
                                labelFontFamily = labelFontFamily,
                                onClick = { onFolderClick(folder) },
                                onPositioned = { position -> onFolderPositioned(folder.id, position) },
                                onDelete = { onDeleteFolder(folder) },
                                onAddToHome = { onAddFolderToHome(folder) },
                                onDragStarted = if (!selectionModeActive) cellDragStart else null,
                                onDragMoved = if (!selectionModeActive) drawerDragMove else null,
                                onDragEnded = if (!selectionModeActive) drawerDragEnd else null,
                                isDragHovered = hoveredFolderKey == cellKey ||
                                    escapeHoverState?.folderId == folder.id,
                                draggedIconPath = if (hoveredFolderKey == cellKey && drawerDraggedItem is AppInfo)
                                    (drawerDraggedItem as AppInfo).iconPath
                                else if (escapeHoverState?.folderId == folder.id) escapeHoverState.iconPath
                                else null
                            )
                        }
                    }

                    // Then show apps
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app" }
                    ) { app ->
                        val cellKey = "app_${app.packageName}"
                        val isDragTarget = drawerDraggedItem == app
                        val cellDragStart: () -> Unit = {
                            if (drawerDraggedItem == null) {
                                drawerDraggedItem = app
                                showDropZone = true
                                drawerDragStartOffset = drawerCellPositions[cellKey] ?: Offset.Zero
                                drawerDragOffset = Offset.Zero
                                drawerDragCellSize = drawerCellSizes[cellKey] ?: IntSize.Zero
                            }
                        }
                        // Get selected apps as AppInfo list for bulk operations
                        val selectedApps = filteredApps.filter { it.packageName in selectedAppPackages }

                        val isDropAnimTarget = app.packageName == dropAnimatingPackage
                        Box(
                            modifier = Modifier
                                .animateItemPlacement(tween(300))
                                .onGloballyPositioned { coords ->
                                    drawerCellPositions[cellKey] = coords.positionInRoot()
                                    drawerCellSizes[cellKey] = coords.size
                                    if (isDropAnimTarget) {
                                        onDropTargetPositioned(coords.positionInRoot(), coords.size)
                                    }
                                }
                                .graphicsLayer {
                                    alpha = if (isDragTarget || isDropAnimTarget) 0f else 1f
                                }
                        ) {
                            SelectableAppItem(
                                app = app,
                                iconSize = iconSize,
                                folderPreviewDraggedIconPath = if (hoveredFolderKey == cellKey && drawerDraggedItem is AppInfo) (drawerDraggedItem as AppInfo).iconPath else null,
                                labelFontSize = labelFontSize,
                                labelFontFamily = labelFontFamily,
                                iconClipShape = iconClipShape,
                                iconBgColor = iconBgColor,
                                globalIconShapeName = globalIconShapeName,
                                onClick = {
                                    if (selectionModeActive) {
                                        selectedAppPackages = if (app.packageName in selectedAppPackages) {
                                            selectedAppPackages - app.packageName
                                        } else {
                                            selectedAppPackages + app.packageName
                                        }
                                        if (selectedAppPackages.isEmpty()) {
                                            selectionModeActive = false
                                        }
                                    } else {
                                        onAppClick(app.packageName)
                                    }
                                },
                                onUninstall = { onUninstallApp(app) },
                                onAppInfo = { onAppInfo(app) },
                                onAddToHome = { onAddToHome(app) },
                                folders = folders,
                                onAddToFolder = { folder -> onAddAppToFolder(app, folder) },
                                onCreateFolderWithApps = { apps ->
                                    onCreateFolderWithApps(apps)
                                },
                                onDeleteFolder = onDeleteFolder,
                                isFolderMenuExpanded = isFolderMenuExpanded,
                                onFolderMenuExpandedChange = onFolderMenuExpandedChange,
                                isSelected = app.packageName in selectedAppPackages,
                                onSelectToggle = {
                                    selectionModeActive = true
                                    selectedAppPackages = if (app.packageName in selectedAppPackages) {
                                        selectedAppPackages - app.packageName
                                    } else {
                                        selectedAppPackages + app.packageName
                                    }
                                    if (selectedAppPackages.isEmpty()) {
                                        selectionModeActive = false
                                    }
                                },
                                selectionModeActive = selectionModeActive,
                                selectedCount = selectedAppPackages.size,
                                selectedApps = selectedApps,
                                onBulkAddToFolder = { folder ->
                                    onBulkAddToFolder(selectedApps, folder)
                                    selectedAppPackages = emptySet()
                                    selectionModeActive = false
                                },
                                onBulkAddToHome = {
                                    selectedApps.forEach { onAddToHome(it) }
                                    selectedAppPackages = emptySet()
                                    selectionModeActive = false
                                },
                                onDragStarted = if (!selectionModeActive) cellDragStart else null,
                                onDragMoved = if (!selectionModeActive) drawerDragMove else null,
                                onDragEnded = if (!selectionModeActive) drawerDragEnd else null,
                                onCustomize = { onCustomizeApp(app) }
                            )
                        }
                    }
                }

                // Custom scrollbar indicator (Einstein Launcher style)
                // Get scrollbar settings
                val scrollbarContext = LocalContext.current
                val scrollbarWidthPct = getScrollbarWidthPercent(scrollbarContext)
                val scrollbarHeightPct = getScrollbarHeightPercent(scrollbarContext)
                val scrollbarScreenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
                val scrollbarScreenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
                val scrollbarWidth = (scrollbarScreenWidthDp * 0.02f * scrollbarWidthPct / 100f).toInt()
                val scrollbarHeight = (scrollbarScreenHeightDp * 0.20f * scrollbarHeightPct / 100f).toInt()
                val scrollbarColor = getScrollbarColor(scrollbarContext)
                val scrollbarIntensity = getScrollbarIntensity(scrollbarContext)

                // Apply intensity to color (0% = black, 100% = original color)
                fun adjustColorIntensity(color: Int, intensity: Int): Color {
                    val baseColor = Color(color)
                    val factor = (intensity / 100f).coerceIn(0f, 1f)
                    return Color(
                        red = baseColor.red * factor,
                        green = baseColor.green * factor,
                        blue = baseColor.blue * factor,
                        alpha = baseColor.alpha
                    )
                }
                val adjustedColor = adjustColorIntensity(scrollbarColor, scrollbarIntensity)

                LazyGridScrollbar(
                    gridState = gridState,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 8.dp, end = 4.dp),
                    thumbColor = adjustedColor.copy(alpha = 0.3f),  // Darker when idle
                    thumbSelectedColor = adjustedColor.copy(alpha = 0.9f),  // Bright when scrolling
                    trackColor = Color.Transparent,  // No track
                    thumbWidth = scrollbarWidth.dp,
                    thumbMinHeight = scrollbarHeight.dp,
                    scrollbarPadding = 0.dp,
                    hideDelayMillis = 1500,
                    alwaysShow = true
                )
            }
        }

        // ========== Shared drag overlay (floats above both paged and scroll modes) ==========
        if (drawerDraggedItem != null && drawerDragCellSize.width > 0 && !transferredToHome) {
            val density = LocalDensity.current
            val p = if (drawerIsDropAnimating) drawerDropAnim.value else 0f

            // Position: lerp from current drag position back to origin during drop
            // For folder drop: lerp toward folder center instead of origin
            val currentOffset = if (folderDropAnimating && drawerIsDropAnimating) {
                // Move toward folder center
                val startX = drawerDragCurrentOffset.x
                val startY = drawerDragCurrentOffset.y
                val targetX = folderDropTargetPos.x - drawerDragStartOffset.x - drawerDragCellSize.width / 2f
                val targetY = folderDropTargetPos.y - drawerDragStartOffset.y - drawerDragCellSize.height / 2f
                Offset(
                    startX + (targetX - startX) * p,
                    startY + (targetY - startY) * p
                )
            } else if (drawerIsDropAnimating) {
                Offset(
                    drawerDragCurrentOffset.x * (1f - p),
                    drawerDragCurrentOffset.y * (1f - p)
                )
            } else {
                drawerDragOffset
            }

            // Scale: folder drop = shrink to 0.3, return-to-origin = 1.0, normal drop = 1.265→1.0
            val overlayScale = if (folderDropAnimating && drawerIsDropAnimating) {
                1.265f * (1f - p * 0.75f) // shrink from 1.265 to ~0.3
            } else if (drawerIsDropAnimating && drawerDragCurrentOffset == Offset.Zero) {
                1f
            } else {
                1.265f - 0.265f * p
            }
            // Alpha: folder drop = fade out, return-to-origin = 1.0, normal = 0.8→1.0
            val overlayAlpha = if (folderDropAnimating && drawerIsDropAnimating) {
                0.8f * (1f - p) // fade out
            } else if (drawerIsDropAnimating && drawerDragCurrentOffset == Offset.Zero) {
                1f
            } else {
                0.8f + 0.2f * p
            }
            // Text: hidden during drag, fade in during drop (return-to-origin = instant 1f)
            val overlayTextAlpha = if (folderDropAnimating && drawerIsDropAnimating) {
                0f // keep hidden during folder drop
            } else if (drawerIsDropAnimating && drawerDragCurrentOffset == Offset.Zero) {
                1f
            } else if (drawerIsDropAnimating) {
                p
            } else {
                0f
            }

            // Subtract wrapper position: cell positions are in root coords, overlay is inside wrapper
            val overlayX = drawerDragStartOffset.x - drawerWrapperRootPos.x + currentOffset.x
            val overlayY = drawerDragStartOffset.y - drawerWrapperRootPos.y + currentOffset.y

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { drawerDragCellSize.width.toDp() },
                        height = with(density) { drawerDragCellSize.height.toDp() }
                    )
                    .zIndex(1000f)
                    .graphicsLayer {
                        translationX = overlayX
                        translationY = overlayY
                        scaleX = overlayScale
                        scaleY = overlayScale
                        alpha = overlayAlpha
                        clip = false
                    },
                contentAlignment = Alignment.Center
            ) {
                val dragItem = drawerDraggedItem
                if (dragItem is AppFolder) {
                    // Folder overlay: 2x2 preview + label
                    val previewApps = remember(dragItem.appPackageNames, allApps) {
                        val validPkgs = dragItem.appPackageNames.filter { it.isNotEmpty() }
                        allApps.filter { it.packageName in validPkgs }.take(4)
                    }
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .size(iconSize.dp)
                                .clip(RoundedCornerShape((iconSize * 0.29f).dp))
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewApps.isNotEmpty()) {
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
                                        } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                        previewApps.getOrNull(1)?.let { app ->
                                            FolderPreviewIcon(app, miniIconSize)
                                        } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                        previewApps.getOrNull(2)?.let { app ->
                                            FolderPreviewIcon(app, miniIconSize)
                                        } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                        previewApps.getOrNull(3)?.let { app ->
                                            FolderPreviewIcon(app, miniIconSize)
                                        } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dragItem.name,
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
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }
                } else if (dragItem is AppInfo) {
                    // App overlay: icon + label (with shape/bg color applied)
                    val overlayCtx = LocalContext.current
                    val overlayIconPath = remember(dragItem.packageName, globalIconShapeName, iconBgColor) {
                        if (globalIconShapeName != null) {
                            try {
                                if (iconBgColor != null) {
                                    getOrGenerateBgColorShapedIcon(overlayCtx, dragItem.packageName, globalIconShapeName, iconBgColor)
                                } else {
                                    getOrGenerateGlobalShapedIcon(overlayCtx, dragItem.packageName, globalIconShapeName)
                                }
                            } catch (_: Exception) { null }
                        } else null
                    }
                    val overlayDisplayPath = overlayIconPath ?: dragItem.iconPath
                    val overlayIsShapedIcon = overlayIconPath != null
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = File(overlayDisplayPath),
                            contentDescription = null,
                            contentScale = if (overlayIsShapedIcon) ContentScale.Fit else if (iconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier
                                .size(iconSize.dp)
                                .then(if (!overlayIsShapedIcon && iconClipShape != null) Modifier.clip(iconClipShape) else Modifier)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dragItem.name,
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
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }
                }
            }
        }
        } // close shared wrapper Box

        if (reverseSearchBar) {
            searchBarBlock()
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}
