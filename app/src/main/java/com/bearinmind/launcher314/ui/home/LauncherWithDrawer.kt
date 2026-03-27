package com.bearinmind.launcher314.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bearinmind.launcher314.data.HomeScreenApp
import com.bearinmind.launcher314.data.HomeScreenData
import com.bearinmind.launcher314.data.HomeFolder
import com.bearinmind.launcher314.ui.drawer.AppDrawerScreen
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.HomeDragCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.bearinmind.launcher314.data.getHomeGridSize
import com.bearinmind.launcher314.data.getHomeGridRows
import com.bearinmind.launcher314.ui.widgets.WidgetManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Combined Launcher and App Drawer screen with swipe-to-reveal gesture.
 *
 * Implementation inspired by Einstein Launcher and Fossify Launcher:
 * - Uses swipeUpY tracking for drawer position
 * - Spring animation for opening, tween for closing
 * - Progress-based alpha for smooth fade transitions
 * - NestedScroll to intercept drawer scroll for closing gesture (only when at top)
 * - Rounded corners on app drawer like Fossify Launcher
 */
@Composable
fun LauncherWithDrawer(
    onSettingsClick: () -> Unit = {},
    onWidgetsClick: () -> Unit = {},
    openDrawerOnStart: Boolean = false,
    widgetRefreshTrigger: Int = 0,
    homeButtonTrigger: Int = 0,
    openDrawerTrigger: Int = 0
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()

    // Swipe tracking - starts at screenHeight (closed), goes to 0 (fully open)
    var lastSwipeUpY by rememberSaveable { mutableFloatStateOf(screenHeight) }
    val swipeUpY = remember { Animatable(lastSwipeUpY) }

    // Track if search is active in drawer (disables swipe-to-close)
    var isDrawerSearchActive by remember { mutableStateOf(false) }
    var dismissSearchTrigger by remember { mutableStateOf(0) }
    var searchDismissed by remember { mutableStateOf(false) }

    // Track if app drawer should be shown
    var showAppDrawer by remember { mutableStateOf(false) }

    // Reset search active state when drawer closes
    LaunchedEffect(showAppDrawer) {
        if (!showAppDrawer) {
            isDrawerSearchActive = false
            searchDismissed = false
        }
    }

    // Track if a folder is open on the home screen (blocks drawer swipe)
    var isFolderOpen by remember { mutableStateOf(false) }

    // Refresh trigger for home screen - increments when drawer closes
    var homeRefreshTrigger by remember { mutableIntStateOf(0) }

    // Drawer-to-home drag transition state
    var drawerToHomeActive by remember { mutableStateOf(false) }
    var drawerToHomeItem by remember { mutableStateOf<Any?>(null) }
    var drawerToHomeInitialPos by remember { mutableStateOf(Offset.Zero) }

    // Finger position tracked via drawer's gesture handler (like folder escape pattern)
    var drawerToHomeFingerPos by remember { mutableStateOf(Offset.Zero) }
    var drawerToHomeDropSignal by remember { mutableIntStateOf(0) }

    // Animated fade for drawer-to-home transition (keeps swipeUpY at 0 so gesture coords stay correct)
    // Phase 1 (0→0.5): drawer fades out.  Phase 2 (0.5→1.0): home screen fades in.
    val drawerToHomeProgress = remember { Animatable(0f) }
    val drawerToHomeFadeAlpha = if (drawerToHomeProgress.value <= 0.5f)
        1f - (drawerToHomeProgress.value / 0.5f)   // drawer: 1→0 in first half
    else 0f                                          // drawer stays gone in second half
    val homeScreenFadeInAlpha = if (drawerToHomeProgress.value <= 0.5f)
        0f                                           // home hidden in first half
    else (drawerToHomeProgress.value - 0.5f) / 0.5f // home: 0→1 in second half

    val onDrawerDragToHome: (Any, Offset) -> Unit = { item, fingerPos ->
        drawerToHomeItem = item
        drawerToHomeInitialPos = fingerPos
        drawerToHomeFingerPos = fingerPos
        drawerToHomeActive = true
        // Animate: phase 1 = drawer fades out, phase 2 = home fades in
        coroutineScope.launch {
            drawerToHomeProgress.snapTo(0f)
            drawerToHomeProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    val onDrawerDragToHomeMove: (Offset) -> Unit = { fingerPos ->
        drawerToHomeFingerPos = fingerPos
    }

    val onDrawerDragToHomeDrop: () -> Unit = {
        drawerToHomeDropSignal++
    }

    // Auto-open drawer if requested
    LaunchedEffect(openDrawerOnStart) {
        if (openDrawerOnStart) {
            showAppDrawer = true
            swipeUpY.snapTo(0f)
        }
    }

    // Home button pressed: close the drawer and return to home screen
    LaunchedEffect(homeButtonTrigger) {
        if (homeButtonTrigger > 0 && showAppDrawer) {
            swipeUpY.animateTo(
                targetValue = screenHeight,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            showAppDrawer = false
            homeRefreshTrigger++
        }
    }

    // Widget added or widget screen closed: ensure drawer is closed so home screen is visible
    LaunchedEffect(widgetRefreshTrigger) {
        if (widgetRefreshTrigger > 0 && showAppDrawer) {
            swipeUpY.snapTo(screenHeight)
            showAppDrawer = false
            homeRefreshTrigger++
        }
    }

    // Accessibility service / preview requested drawer open
    // Track last consumed trigger to avoid re-firing when composable re-enters composition
    var lastConsumedDrawerTrigger by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(openDrawerTrigger) {
        if (openDrawerTrigger > 0 && openDrawerTrigger != lastConsumedDrawerTrigger) {
            lastConsumedDrawerTrigger = openDrawerTrigger
            showAppDrawer = true
            swipeUpY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Safety: if screenHeight changed (e.g., returning from widget configure activity
    // with different insets), reset drawer to closed if it was meant to be closed.
    // Without this, a saved pixel position slightly less than new screenHeight
    // makes the drawer appear partially open.
    LaunchedEffect(screenHeight) {
        if (!showAppDrawer && swipeUpY.value != screenHeight) {
            swipeUpY.snapTo(screenHeight)
            lastSwipeUpY = screenHeight
        }
    }

    // Capture home screen screenshot for settings preview (via PixelCopy)
    val captureView = LocalView.current
    val captureActivity = remember(context) { context as? android.app.Activity }

    LaunchedEffect(showAppDrawer, widgetRefreshTrigger) {
        // Only capture when drawer is closed (home screen fully visible)
        if (!showAppDrawer && captureActivity != null) {
            delay(2500) // Wait for widgets and content to fully render
            // Double-check drawer didn't reopen during the delay
            if (!showAppDrawer) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        val w = captureView.width
                        val h = captureView.height
                        if (w > 0 && h > 0) {
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                w, h, android.graphics.Bitmap.Config.ARGB_8888
                            )
                            // Use suspendCancellableCoroutine to bridge PixelCopy callback
                            val success = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                                try {
                                    android.view.PixelCopy.request(
                                        captureActivity.window,
                                        bitmap,
                                        { result ->
                                            if (cont.isActive) {
                                                cont.resume(result == android.view.PixelCopy.SUCCESS) {}
                                            }
                                        },
                                        android.os.Handler(android.os.Looper.getMainLooper())
                                    )
                                } catch (e: Exception) {
                                    if (cont.isActive) cont.resume(false) {}
                                }
                            }
                            if (success) {
                                withContext(Dispatchers.IO) {
                                    val file = File(context.filesDir, "home_screen_preview.jpg")
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(
                                            android.graphics.Bitmap.CompressFormat.JPEG,
                                            85,
                                            out
                                        )
                                    }
                                }
                            }
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Calculate alpha for home screen (pager) based on swipe progress
    // Key on screenHeight so derivedStateOf recalculates if screen dimensions change
    val pagerAlpha by remember(screenHeight) {
        derivedStateOf {
            val threshold = screenHeight / 2
            ((swipeUpY.value - threshold) / threshold).coerceIn(0f, 1f)
        }
    }

    // Calculate alpha for app drawer (inverse of pager)
    val drawerAlpha by remember(screenHeight) {
        derivedStateOf {
            (1f - (swipeUpY.value / screenHeight)).coerceIn(0f, 1f)
        }
    }

    // Fixed corner radius for rounded top corners like Fossify Launcher
    val drawerCornerRadius = 0.dp

    // Action threshold - 100px triggers action
    val actionThreshold = 100f

    // Function to add app to home screen
    val addAppToHome: (AppInfo) -> Unit = { app ->
        // Load current home screen data
        val file = File(context.filesDir, "home_screen_data.json")
        val currentData = try {
            if (file.exists()) {
                Json.decodeFromString<HomeScreenData>(file.readText())
            } else {
                HomeScreenData()
            }
        } catch (e: Exception) {
            HomeScreenData()
        }

        val gridColumns = getHomeGridSize(context)
        val gridRows = getHomeGridRows(context)
        val totalCells = gridColumns * gridRows

        // Collect all occupied positions on page 0
        val occupiedPositions = mutableSetOf<Int>()

        // Apps
        currentData.apps.filter { it.page == 0 }.forEach { occupiedPositions.add(it.position) }

        // Folders
        currentData.folders.filter { it.page == 0 }.forEach { occupiedPositions.add(it.position) }

        // Widgets (span multiple cells)
        val placedWidgets = WidgetManager.loadPlacedWidgets(context)
        for (widget in placedWidgets) {
            for (r in widget.startRow until widget.startRow + widget.rowSpan) {
                for (c in widget.startColumn until widget.startColumn + widget.columnSpan) {
                    val pos = r * gridColumns + c
                    if (pos in 0 until totalCells) {
                        occupiedPositions.add(pos)
                    }
                }
            }
        }

        val emptyPosition = (0 until totalCells).firstOrNull { it !in occupiedPositions }

        if (emptyPosition != null) {
            // Add app to grid
            val newApps = currentData.apps + HomeScreenApp(
                packageName = app.packageName,
                position = emptyPosition
            )
            val newData = currentData.copy(apps = newApps)

            // Save updated data
            try {
                file.writeText(Json.encodeToString(newData))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val addFolderToHome: (AppFolder) -> Unit = { drawerFolder ->
        val file = File(context.filesDir, "home_screen_data.json")
        val currentData = try {
            if (file.exists()) {
                Json.decodeFromString<HomeScreenData>(file.readText())
            } else {
                HomeScreenData()
            }
        } catch (e: Exception) {
            HomeScreenData()
        }

        val gridColumns = getHomeGridSize(context)
        val gridRows = getHomeGridRows(context)
        val totalCells = gridColumns * gridRows

        val occupiedPositions = mutableSetOf<Int>()
        currentData.apps.filter { it.page == 0 }.forEach { occupiedPositions.add(it.position) }
        currentData.folders.filter { it.page == 0 }.forEach { occupiedPositions.add(it.position) }
        val placedWidgets = WidgetManager.loadPlacedWidgets(context)
        for (widget in placedWidgets) {
            for (r in widget.startRow until widget.startRow + widget.rowSpan) {
                for (c in widget.startColumn until widget.startColumn + widget.columnSpan) {
                    val pos = r * gridColumns + c
                    if (pos in 0 until totalCells) {
                        occupiedPositions.add(pos)
                    }
                }
            }
        }

        val emptyPosition = (0 until totalCells).firstOrNull { it !in occupiedPositions }
        if (emptyPosition != null) {
            val homeFolder = HomeFolder(
                name = drawerFolder.name,
                position = emptyPosition,
                page = 0,
                appPackageNames = drawerFolder.appPackageNames.filter { it.isNotEmpty() }
            )
            val newData = currentData.copy(folders = currentData.folders + homeFolder)
            try {
                file.writeText(Json.encodeToString(newData))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Update lastSwipeUpY when animation settles
    LaunchedEffect(swipeUpY.value) {
        lastSwipeUpY = swipeUpY.value
        // Only show drawer if meaningfully pulled up (not just a rounding difference)
        if (swipeUpY.value < screenHeight - 5f) {
            showAppDrawer = true
        }
    }

    // Back button: drawer open → close drawer, home screen → do nothing
    BackHandler(enabled = true) {
        if (showAppDrawer && swipeUpY.value < screenHeight) {
            // Drawer is visible — close it and return to home screen
            coroutineScope.launch {
                swipeUpY.animateTo(
                    targetValue = screenHeight,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
                showAppDrawer = false
                homeRefreshTrigger++
            }
        }
        // else: on home screen — consume back press silently (launcher shouldn't exit)
    }

    // NestedScrollConnection to intercept scroll events from app drawer
    // When drawer is partially open, ALL gestures control the drawer (not the list)
    // Only when fully open (swipeUpY == 0) does the list scroll normally
    val nestedScrollConnection = remember(screenHeight, isDrawerSearchActive) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When drawer is partially open (in transition), intercept ALL vertical gestures
                // This prevents the list from scrolling while drawer is being dragged
                android.util.Log.d("DrawerScroll", "PRE: swipeY=${swipeUpY.value} search=$isDrawerSearchActive avail=${available.y}")
                if (swipeUpY.value > 0 && !isDrawerSearchActive) {
                    coroutineScope.launch {
                        val newValue = (swipeUpY.value + available.y).coerceIn(0f, screenHeight)
                        swipeUpY.snapTo(newValue)
                    }
                    return available.copy(x = 0f)  // Consume all vertical scroll
                }
                return Offset.Zero
            }

            // Use onPostScroll to catch overscroll at the top of the list (when drawer is fully open)
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If drawer is fully open and there's leftover downward scroll
                // (list at top, can't scroll up more), start closing the drawer
                // Skip when search is active so user can scroll search results
                if (available.y > 0 && swipeUpY.value == 0f && isDrawerSearchActive && !searchDismissed) {
                    // Keyboard is up: dismiss it by triggering focus clear in MainDrawerContent
                    searchDismissed = true
                    dismissSearchTrigger++
                    return Offset(0f, available.y)
                }
                if (available.y > 0 && swipeUpY.value == 0f && !isDrawerSearchActive) {
                    coroutineScope.launch {
                        val newValue = (swipeUpY.value + available.y).coerceIn(0f, screenHeight)
                        swipeUpY.snapTo(newValue)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Handle fling to close drawer (only if drawer is partially open)
                if (swipeUpY.value > 0 && swipeUpY.value < screenHeight) {
                    val shouldClose = if (available.y > 1000f) {
                        true  // Fast downward fling = close
                    } else {
                        swipeUpY.value > actionThreshold
                    }

                    if (shouldClose) {
                        swipeUpY.animateTo(
                            targetValue = screenHeight,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        )
                        showAppDrawer = false
                                homeRefreshTrigger++
                    } else {
                        swipeUpY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Ensure drawer snaps to a stable state
                if (swipeUpY.value > 0 && swipeUpY.value < screenHeight) {
                    if (swipeUpY.value > actionThreshold) {
                        swipeUpY.animateTo(
                            targetValue = screenHeight,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        )
                        showAppDrawer = false
                                homeRefreshTrigger++
                    } else {
                        swipeUpY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Home screen (launcher) with gesture detection
        // Only handles the OPENING gesture (swipe up on home screen).
        // Layer 2's nestedScroll handles the CLOSING gesture (swipe down on drawer).
        val swipeDownEnabled = com.bearinmind.launcher314.data.getSwipeDownNotifications(context)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeDownEnabled) {
                    // Track whether this gesture started when the drawer was closed.
                    // Prevents Layer 1 from interfering with Layer 2's closing gesture.
                    var gestureOwnedByLayer1 = false
                    var isSwipeDown = false
                    var totalDragAmount = 0f

                    detectVerticalDragGestures(
                        onDragStart = {
                            // Only own gestures that start when drawer is closed and no folder is open
                            gestureOwnedByLayer1 = swipeUpY.value > screenHeight * 0.9f && !isFolderOpen
                            isSwipeDown = false
                            totalDragAmount = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!gestureOwnedByLayer1) return@detectVerticalDragGestures
                            totalDragAmount += dragAmount

                            // Detect swipe direction early
                            if (totalDragAmount > 50f && !isSwipeDown && swipeDownEnabled) {
                                isSwipeDown = true
                            }

                            if (isSwipeDown) {
                                change.consume()
                                return@detectVerticalDragGestures
                            }

                            change.consume()
                            coroutineScope.launch {
                                val newValue = (swipeUpY.value + dragAmount).coerceIn(0f, screenHeight)
                                swipeUpY.snapTo(newValue)
                            }
                        },
                        onDragEnd = {
                            if (!gestureOwnedByLayer1) return@detectVerticalDragGestures

                            if (isSwipeDown && totalDragAmount > actionThreshold) {
                                // Swipe down detected — expand notification or quick settings
                                val mode = com.bearinmind.launcher314.data.getSwipeDownMode(context)
                                if (mode == 1) {
                                    com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandQuickSettings(context)
                                } else {
                                    com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandNotificationPanel(context)
                                }
                                return@detectVerticalDragGestures
                            }

                            coroutineScope.launch {
                                if (swipeUpY.value < screenHeight - actionThreshold) {
                                    // Open drawer with spring animation
                                    swipeUpY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                } else {
                                    // Close with tween animation
                                    swipeUpY.animateTo(
                                        targetValue = screenHeight,
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    showAppDrawer = false
                                    homeRefreshTrigger++
                                }
                            }
                        },
                        onDragCancel = {
                            if (!gestureOwnedByLayer1) return@detectVerticalDragGestures
                            if (isSwipeDown) return@detectVerticalDragGestures
                            coroutineScope.launch {
                                swipeUpY.animateTo(
                                    targetValue = screenHeight,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                                showAppDrawer = false
                                homeRefreshTrigger++
                            }
                        }
                    )
                }
                .graphicsLayer {
                    alpha = if (drawerToHomeActive) homeScreenFadeInAlpha else pagerAlpha
                }
        ) {
            LauncherScreen(
                onOpenAppDrawer = {
                    coroutineScope.launch {
                        showAppDrawer = true
                        swipeUpY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onOpenSettings = onSettingsClick,
                onOpenWidgets = {
                    // Close drawer before navigating to widgets so returning lands on home screen
                    if (showAppDrawer) {
                        coroutineScope.launch {
                            swipeUpY.snapTo(screenHeight)
                        }
                        showAppDrawer = false
                        homeRefreshTrigger++
                    }
                    onWidgetsClick()
                },
                refreshTrigger = homeRefreshTrigger + widgetRefreshTrigger,
                onFolderOpenChanged = { isFolderOpen = it },
                externalDragItem = if (drawerToHomeActive) drawerToHomeItem else null,
                externalDragInitialPos = drawerToHomeInitialPos,
                externalDragFingerPos = drawerToHomeFingerPos,
                externalDragDropSignal = drawerToHomeDropSignal,
                onExternalDragComplete = {
                    drawerToHomeActive = false
                    drawerToHomeItem = null
                    // Drawer was faded out — snap position to closed and remove from composition
                    coroutineScope.launch {
                        drawerToHomeProgress.snapTo(0f)
                        swipeUpY.snapTo(screenHeight)
                    }
                    showAppDrawer = false
                    homeRefreshTrigger++
                }
            )
        }

        // Layer 2: App drawer - only render when being shown
        // Has rounded top corners like Fossify Launcher
        // Background is handled by AppDrawerScreen itself (with transparency support)
        if (showAppDrawer || swipeUpY.value < screenHeight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, swipeUpY.value.roundToInt()) }
                    .graphicsLayer {
                        alpha = if (drawerToHomeActive) drawerToHomeFadeAlpha else 1f
                    }
                    .clip(RoundedCornerShape(topStart = drawerCornerRadius, topEnd = drawerCornerRadius))
                    .nestedScroll(nestedScrollConnection)
            ) {
                AppDrawerScreen(
                    dismissSearchTrigger = dismissSearchTrigger,
                    onSearchActiveChanged = {
                        isDrawerSearchActive = it
                        if (it) searchDismissed = false
                    },
                    isDrawerFullyOpen = swipeUpY.value == 0f && showAppDrawer,
                    onSettingsClick = onSettingsClick,
                    onAddToHome = addAppToHome,
                    onAddFolderToHome = addFolderToHome,
                    homeDragCallbacks = HomeDragCallbacks(
                        onDragToHome = onDrawerDragToHome,
                        onDragToHomeMove = onDrawerDragToHomeMove,
                        onDragToHomeDrop = onDrawerDragToHomeDrop
                    )
                )
                // Block taps/interactions while drawer is not fully open (swiping, animating, or held mid-swipe)
                if (swipeUpY.isRunning || swipeUpY.value > 10f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                    )
                }
            }
        }

    }
}
