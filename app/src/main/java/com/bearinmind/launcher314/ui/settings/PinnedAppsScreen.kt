package com.bearinmind.launcher314.ui.settings

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.abs
import com.bearinmind.launcher314.data.getPinnedAppsOrder
import com.bearinmind.launcher314.data.setPinnedAppsOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PinAppInfo(
    val packageName: String,
    val name: String,
    val iconPath: String
)

/** Same slide-aside recipe as DrawerTabs: animates reshuffled chips; the dragged one snaps and follows the finger. */
private fun Modifier.animatePlacement(animate: Boolean): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }
    this
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(targetOffset, IntOffset.VectorConverter)
                .also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    if (animate) {
                        anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    } else {
                        anim.snapTo(targetOffset)
                    }
                }
            }
            anim.value - targetOffset
        }
}

/** Pick + reorder apps/folders pinned to the top of the drawer — same UI/behavior as HideAppsScreen. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PinnedAppsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var pinnedOrder by remember { mutableStateOf(getPinnedAppsOrder(context)) }
    var pendingRemoval by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allApps by remember { mutableStateOf<List<PinAppInfo>>(emptyList()) }
    var drawerFolders by remember { mutableStateOf<List<com.bearinmind.launcher314.data.AppFolder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val haptics = com.bearinmind.launcher314.helpers.rememberHapticFeedback()

    // Load all installed apps + drawer folders (folders are pinnable too)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val apps = activities.mapNotNull { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val iconDir = File(context.cacheDir, "app_icons")
                    val iconFile = File(iconDir, "$pkg.png")
                    val iconPath = if (iconFile.exists()) iconFile.absolutePath else ""
                    PinAppInfo(pkg, appName, iconPath)
                } catch (_: Exception) { null }
            }
                // Same duplicate-key crash fix as HideAppsScreen.
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }
            allApps = apps
            val allFolders = com.bearinmind.launcher314.data.loadDrawerData(context).folders
            // Top-level drawer folders only — sub-folders live inside a parent, not in the drawer list.
            val nestedIds = allFolders.flatMap { f ->
                f.appPackageNames.filter { com.bearinmind.launcher314.data.isFolderEntry(it) }
                    .map { com.bearinmind.launcher314.data.folderEntryId(it) }
            }.toSet()
            // Renames from the customize popup live on the customization, not folder.name.
            val custs = com.bearinmind.launcher314.data.loadAppCustomizations(context).customizations
            drawerFolders = allFolders.filter { it.id !in nestedIds }
                .map { f -> custs["folder_${f.id}"]?.customLabel?.let { f.copy(name = it) } ?: f }
                .sortedBy { it.name.lowercase() }
            isLoading = false
        }
    }

    // Minimal AppInfo list so MiniFolderBox can render real folder previews.
    val folderPreviewApps by remember {
        derivedStateOf { allApps.map { com.bearinmind.launcher314.data.AppInfo(it.name, it.packageName, it.iconPath) } }
    }

    // Markers resolvable to a chip (pin order can hold uninstalled apps / deleted folders).
    fun resolvable(marker: String): Boolean =
        if (com.bearinmind.launcher314.data.isFolderEntry(marker))
            drawerFolders.any { it.id == com.bearinmind.launcher314.data.folderEntryId(marker) }
        else allApps.any { it.packageName == marker }

    val pinnedChips by remember {
        derivedStateOf { pinnedOrder.filter { resolvable(it) } }
    }

    fun chipName(marker: String): String =
        if (com.bearinmind.launcher314.data.isFolderEntry(marker))
            drawerFolders.firstOrNull { it.id == com.bearinmind.launcher314.data.folderEntryId(marker) }?.name ?: ""
        else allApps.firstOrNull { it.packageName == marker }?.name ?: ""

    val folderList by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) drawerFolders
            else drawerFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val allAppsList by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) allApps.sortedBy { it.name.lowercase() }
            else allApps.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedBy { it.name.lowercase() }
        }
    }

    fun unpin(marker: String) {
        pinnedOrder = pinnedOrder - marker
        setPinnedAppsOrder(context, pinnedOrder)
    }

    fun pin(marker: String) {
        pinnedOrder = pinnedOrder + marker
        setPinnedAppsOrder(context, pinnedOrder)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search bar (same style as HideAppsScreen)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search Apps", color = Color.White.copy(alpha = 0.6f))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // Pinned section — chips reorder by long-press drag (tab-strip mechanics)
                AnimatedVisibility(
                    visible = pinnedChips.isNotEmpty(),
                    enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
                ) {
                    Column {
                        Text(
                            text = "Pinned (hold and drag to reorder)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(vertical = 12.dp)
                        ) {
                            val chipScroll = rememberScrollState()
                            // Reorder drag state — STABLE holders; the row's pointerInput never restarts.
                            val chipPositions = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
                            var draggingMarker by remember { mutableStateOf<String?>(null) }
                            val livePinsState = remember { mutableStateOf(pinnedChips) }
                            LaunchedEffect(pinnedChips) { if (draggingMarker == null) livePinsState.value = pinnedChips }
                            var livePins by livePinsState
                            val currentOrder by rememberUpdatedState(pinnedOrder)
                            val currentSearch by rememberUpdatedState(searchQuery)
                            var dragFingerX by remember { mutableFloatStateOf(0f) }
                            var dragStartX by remember { mutableFloatStateOf(0f) }
                            var dragInitialSlotLeft by remember { mutableFloatStateOf(0f) }
                            var settlingMarker by remember { mutableStateOf<String?>(null) }
                            val settleAnim = remember { Animatable(0f) }
                            val settleScope = rememberCoroutineScope()

                            val releaseDrag = {
                                val id = draggingMarker
                                if (id != null) {
                                    val residual = dragInitialSlotLeft + (dragFingerX - dragStartX) -
                                        (chipPositions[id]?.first?.toFloat() ?: 0f)
                                    settlingMarker = id
                                    settleScope.launch {
                                        settleAnim.snapTo(residual)
                                        settleAnim.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                                        if (settlingMarker == id) settlingMarker = null
                                    }
                                    // Commit: reordered visible pins + any unresolvable markers kept at the end.
                                    val leftovers = currentOrder.filter { it !in livePinsState.value }
                                    val newOrder = livePinsState.value + leftovers
                                    if (newOrder != currentOrder) {
                                        pinnedOrder = newOrder
                                        setPinnedAppsOrder(context, newOrder)
                                    }
                                }
                                draggingMarker = null
                            }

                            // Row-content coords for hit-testing (AnimatedVisibility gives chips their own space, positionInParent ~0).
                            var rowCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(chipScroll)
                                    .padding(horizontal = 12.dp)
                                    .onGloballyPositioned { rowCoords = it }
                                    // ONE long-press-drag gesture on the row (not per-chip) so a reorder can't kill its own gesture.
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                if (currentSearch.isNotBlank()) { draggingMarker = null; return@detectDragGesturesAfterLongPress }
                                                val hit = livePinsState.value.firstOrNull { m ->
                                                    val p = chipPositions[m]
                                                    p != null && offset.x >= p.first && offset.x <= p.first + p.second
                                                }
                                                if (hit != null) {
                                                    draggingMarker = hit
                                                    dragStartX = offset.x
                                                    dragFingerX = offset.x
                                                    dragInitialSlotLeft = (chipPositions[hit]?.first ?: 0).toFloat()
                                                    haptics.performLongPress()
                                                } else {
                                                    draggingMarker = null
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                val id = draggingMarker ?: return@detectDragGesturesAfterLongPress
                                                change.consume()
                                                dragFingerX += dragAmount.x
                                                val others = livePinsState.value.filter { it != id }
                                                var insert = others.size
                                                for (i in others.indices) {
                                                    val p = chipPositions[others[i]] ?: continue
                                                    val center = p.first + p.second / 2f
                                                    if (dragFingerX < center) { insert = i; break }
                                                }
                                                val rebuilt = others.toMutableList().also { it.add(insert, id) }
                                                if (rebuilt != livePinsState.value) livePins = rebuilt
                                            },
                                            onDragEnd = { releaseDrag() },
                                            onDragCancel = { releaseDrag() }
                                        )
                                    },
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                livePins.forEach { marker ->
                                    key(marker) {
                                        val isFolder = com.bearinmind.launcher314.data.isFolderEntry(marker)
                                        val folder = if (isFolder) drawerFolders.firstOrNull {
                                            it.id == com.bearinmind.launcher314.data.folderEntryId(marker)
                                        } else null
                                        val app = if (!isFolder) allApps.firstOrNull { it.packageName == marker } else null
                                        val name = folder?.name ?: app?.name ?: ""
                                        val matchesSearch = searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)
                                        val isDraggingThis = marker == draggingMarker
                                        val isSettlingThis = marker == settlingMarker
                                        val slotLeft = (chipPositions[marker]?.first ?: 0).toFloat()
                                        // Rendered pos = grab slot + finger delta, continuous across reshuffles.
                                        val translation = when {
                                            isDraggingThis -> (dragInitialSlotLeft + (dragFingerX - dragStartX)) - slotLeft
                                            isSettlingThis -> settleAnim.value
                                            else -> 0f
                                        }
                                        val lift by animateFloatAsState(
                                            targetValue = if (isDraggingThis || isSettlingThis) 1.15f else 1f,
                                            animationSpec = tween(150),
                                            label = "pinChipLift"
                                        )
                                        val isBeingRemoved = marker in pendingRemoval
                                        var itemVisible by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) { itemVisible = true }
                                        LaunchedEffect(isBeingRemoved) {
                                            if (isBeingRemoved) {
                                                itemVisible = false
                                                delay(250)
                                                unpin(marker)
                                                pendingRemoval = pendingRemoval - marker
                                            }
                                        }
                                        AnimatedVisibility(
                                            visible = itemVisible && matchesSearch,
                                            enter = fadeIn(tween(250)),
                                            exit = fadeOut(tween(200)),
                                            // The AV box is the Row's direct child — the node that moves on reshuffle.
                                            modifier = Modifier.animatePlacement(animate = !isDraggingThis && !isSettlingThis)
                                        ) {
                                        Column(
                                            modifier = Modifier
                                                .width(64.dp)
                                                .onGloballyPositioned { coords ->
                                                    val x = rowCoords?.localPositionOf(coords, androidx.compose.ui.geometry.Offset.Zero)?.x
                                                    if (x != null) chipPositions[marker] = x.toInt() to coords.size.width
                                                }
                                                .graphicsLayer {
                                                    translationX = translation
                                                    scaleX = lift
                                                    scaleY = lift
                                                }
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { pendingRemoval = pendingRemoval + marker }
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val iconSizeDp = 40.dp
                                            val badgeSize = iconSizeDp * 0.42f
                                            val badgeOffset = iconSizeDp * 0.083f
                                            Box(modifier = Modifier.size(iconSizeDp)) {
                                                if (folder != null) {
                                                    com.bearinmind.launcher314.ui.drawer.MiniFolderBox(
                                                        folder = folder,
                                                        allApps = folderPreviewApps,
                                                        size = iconSizeDp
                                                    )
                                                } else if (app != null && app.iconPath.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.size(iconSizeDp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(iconSizeDp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.1f))
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = badgeOffset, y = -badgeOffset)
                                                        .size(badgeSize)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF8B2020).copy(alpha = 0.85f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(badgeSize * 0.5f)
                                                            .height(2.dp)
                                                            .background(Color(0xFFCCCCCC).copy(alpha = 0.85f))
                                                    )
                                                }
                                            }
                                            Text(
                                                text = name,
                                                color = Color.White.copy(alpha = 0.87f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                            )
                                        }
                                        } // AnimatedVisibility
                                    }
                                }
                            }

                            // Scrollbar area — fixed height so card doesn't resize
                            val scrollbarColor = remember {
                                val baseColor = Color(com.bearinmind.launcher314.data.getScrollbarColor(context))
                                val intensity = com.bearinmind.launcher314.data.getScrollbarIntensity(context) / 100f
                                Color(baseColor.red * intensity, baseColor.green * intensity, baseColor.blue * intensity, baseColor.alpha)
                            }
                            val widthPercent = com.bearinmind.launcher314.data.getScrollbarWidthPercent(context) / 100f
                            val thumbHeight = (3.dp * widthPercent).coerceAtLeast(2.dp)
                            val isScrolling = chipScroll.isScrollInProgress
                            // ScrollState.maxValue is Int.MAX_VALUE before first measure — treat as "no scrollbar".
                            val hasScrollbar = chipScroll.maxValue in 1 until Int.MAX_VALUE
                            var scrollbarVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(isScrolling) {
                                if (isScrolling && hasScrollbar) { scrollbarVisible = true }
                                else { delay(1000); scrollbarVisible = false }
                            }
                            val scrollbarAlpha by animateFloatAsState(
                                targetValue = if (scrollbarVisible) 1f else 0f,
                                animationSpec = tween(300),
                                label = "pinnedScrollbarAlpha"
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(thumbHeight)
                                    .graphicsLayer { alpha = scrollbarAlpha }
                            ) {
                                // Re-read here — maxValue can settle to 0 between composition and this layout pass (0/0 = NaN = crash).
                                if (chipScroll.maxValue in 1 until Int.MAX_VALUE) {
                                    val scrollFraction = (chipScroll.value.toFloat() / chipScroll.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val thumbFraction = (4f / pinnedChips.size.coerceAtLeast(1)).coerceIn(0.2f, 1f)
                                    val trackWidth = maxWidth
                                    val thumbWidth = trackWidth * thumbFraction
                                    val thumbOffset = (trackWidth - thumbWidth) * scrollFraction
                                    Box(
                                        modifier = Modifier
                                            .offset(x = thumbOffset)
                                            .width(thumbWidth)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(thumbHeight / 2))
                                            .background(scrollbarColor.copy(alpha = 0.6f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    // Drawer folders — pinnable like apps
                    if (folderList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Folders",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(folderList, key = { "listfolder_${it.id}" }) { folder ->
                            val folderMarker = com.bearinmind.launcher314.data.folderEntry(folder.id)
                            val isPinned = folderMarker in pinnedOrder && folderMarker !in pendingRemoval
                            val togglePin = {
                                if (folderMarker in pinnedOrder) {
                                    pendingRemoval = pendingRemoval + folderMarker
                                } else {
                                    pin(folderMarker)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .animateItemPlacement(tween(300))
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clickable { togglePin() }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isPinned,
                                    onCheckedChange = { togglePin() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = Color(0xFF3A3A3A),
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Text(
                                    text = folder.name,
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.87f),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                com.bearinmind.launcher314.ui.drawer.MiniFolderBox(
                                    folder = folder,
                                    allApps = folderPreviewApps,
                                    size = 40.dp
                                )
                            }
                            if (folder.id != folderList.last().id) Divider(color = Color.White.copy(alpha = 0.1f))
                        }
                    }

                    // All apps section header
                    item {
                        Text(
                            text = "All apps",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // All apps list (same style as HideAppsScreen)
                    items(allAppsList, key = { "all_${it.packageName}" }) { app ->
                        val isPinned = app.packageName in pinnedOrder && app.packageName !in pendingRemoval
                        val togglePin = {
                            if (app.packageName in pinnedOrder) {
                                pendingRemoval = pendingRemoval + app.packageName
                            } else {
                                pin(app.packageName)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .animateItemPlacement(tween(300))
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { togglePin() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isPinned,
                                onCheckedChange = { togglePin() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = Color(0xFF3A3A3A),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )

                            Text(
                                text = app.name,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.87f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (app.iconPath.isNotEmpty()) {
                                AsyncImage(
                                    model = File(app.iconPath),
                                    contentDescription = app.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        if (app.packageName != allAppsList.last().packageName) Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
