package com.bearinmind.launcher314.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
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

    // Accessibility service / preview requested drawer open
    // Track last consumed trigger to avoid re-firing when composable re-enters composition
    var lastConsumedDrawerTrigger by rememberSaveable { mutableIntStateOf(0) }
    // Whether the drawer was intentionally opened via trigger (guards against widget refresh closing it)
    var drawerOpenedViaTrigger by remember { mutableStateOf(false) }

    // Widget added or widget screen closed: ensure drawer is closed so home screen is visible
    // Skip if the drawer was just intentionally opened via openDrawerTrigger (race condition)
    var lastConsumedWidgetTrigger by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(widgetRefreshTrigger) {
        if (widgetRefreshTrigger > 0 && widgetRefreshTrigger != lastConsumedWidgetTrigger) {
            lastConsumedWidgetTrigger = widgetRefreshTrigger
            if (showAppDrawer && !drawerOpenedViaTrigger) {
                swipeUpY.snapTo(screenHeight)
                showAppDrawer = false
                homeRefreshTrigger++
            }
            drawerOpenedViaTrigger = false
        }
    }

    LaunchedEffect(openDrawerTrigger) {
        if (openDrawerTrigger > 0 && openDrawerTrigger != lastConsumedDrawerTrigger) {
            lastConsumedDrawerTrigger = openDrawerTrigger
            drawerOpenedViaTrigger = true
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
        // ========== CUSTOM WALLPAPER BACKDROP ==========
        // Paints UNDER all launcher content. When wallpaper mode = "custom" we show
        // the imported image (scale-cropped to fill the screen). The dim overlay and
        // optional blur apply to both custom and system wallpaper — for system wallpaper
        // the backdrop is transparent so the system wallpaper shows through, but dim
        // still stacks on top via the overlay Box below.
        val wallpaperMode by remember { mutableStateOf(com.bearinmind.launcher314.data.getWallpaperMode(context)) }
        val wallpaperCacheVersion = com.bearinmind.launcher314.data.getWallpaperCacheVersion(context)
        val wallpaperDim = com.bearinmind.launcher314.data.getWallpaperDim(context)
        val wallpaperBlurPercent = com.bearinmind.launcher314.data.getWallpaperBlur(context)
        val customWallpaperPath = remember(wallpaperCacheVersion, wallpaperMode) {
            if (wallpaperMode == com.bearinmind.launcher314.data.WALLPAPER_MODE_CUSTOM)
                com.bearinmind.launcher314.data.getCustomWallpaperPath(context)
            else null
        }

        // Preview override: when the wallpaper editor's "Preview" button
        // is active, render the in-progress edited bitmap in place of the
        // saved custom wallpaper so the user can see the new look behind
        // the real home-screen chrome.
        val wallpaperPreview = com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview
        if (wallpaperPreview != null) {
            val previewEdit = wallpaperPreview.edit
            // Include BOTH the edit's own blur AND the launcher's global
            // wallpaper-blur preference, approximated as an additive radius,
            // so the preview matches what the launcher actually paints on
            // the home screen post-Apply (the saved bitmap already has the
            // edit blur baked in, and the launcher adds its own on top).
            val editBlurPx = (previewEdit.blur / 100f * 25f)
            val launcherBlurPx = (wallpaperBlurPercent / 100f * 25f)
            val combinedBlurDp = (editBlurPx + launcherBlurPx).dp
            val previewBlurMod = if (combinedBlurDp > 0.dp && android.os.Build.VERSION.SDK_INT >= 31) {
                Modifier.graphicsLayer {
                    renderEffect = androidx.compose.ui.graphics.BlurEffect(
                        radiusX = with(density) { combinedBlurDp.toPx() },
                        radiusY = with(density) { combinedBlurDp.toPx() },
                        edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                    )
                }
            } else Modifier
            // Apply the crop rectangle once per edit change (avoids reallocating
            // the cropped Bitmap on every recomposition).
            val previewCropped = remember(
                wallpaperPreview.sourceBitmap,
                previewEdit.cropLeft, previewEdit.cropTop,
                previewEdit.cropRight, previewEdit.cropBottom
            ) {
                val src = wallpaperPreview.sourceBitmap
                val inset = previewEdit.cropLeft > 0.001f || previewEdit.cropTop > 0.001f ||
                    previewEdit.cropRight < 0.999f || previewEdit.cropBottom < 0.999f
                if (inset) {
                    val l = (previewEdit.cropLeft * src.width).toInt().coerceIn(0, src.width - 1)
                    val t = (previewEdit.cropTop * src.height).toInt().coerceIn(0, src.height - 1)
                    val r = (previewEdit.cropRight * src.width).toInt().coerceIn(l + 1, src.width)
                    val b = (previewEdit.cropBottom * src.height).toInt().coerceIn(t + 1, src.height)
                    android.graphics.Bitmap.createBitmap(src, l, t, r - l, b - t)
                } else src
            }
            // Render math. Translation is strictly clamped so the bitmap's
            // edges never detach from the screen's edges. Overshoot shows up
            // instead as an iOS-style directional stretch applied in a
            // second graphicsLayer, pivoted at the OPPOSITE edge from the
            // drag direction — so dragging right anchors the left edge and
            // stretches the right side outward. Releasing springs overshoot
            // back to zero (the gesture-end LaunchedEffect above).
            val renderScreenWpx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val renderScreenHpx = with(density) { configuration.screenHeightDp.dp.toPx() }
            val baseScale = previewEdit.scale.coerceIn(1f, 5f)
            val baseMaxOffX = (baseScale - 1f) * renderScreenWpx / 2f
            val baseMaxOffY = (baseScale - 1f) * renderScreenHpx / 2f
            val renderOffX = previewEdit.offsetX.coerceIn(-baseMaxOffX, baseMaxOffX)
            val renderOffY = previewEdit.offsetY.coerceIn(-baseMaxOffY, baseMaxOffY)
            val overshootX = previewEdit.offsetX - renderOffX
            val overshootY = previewEdit.offsetY - renderOffY
            val stretchFromOvershootX = if (overshootX != 0f)
                rubberbandAttenuate(kotlin.math.abs(overshootX), 180f, 0.55f) / renderScreenWpx
            else 0f
            val stretchFromOvershootY = if (overshootY != 0f)
                rubberbandAttenuate(kotlin.math.abs(overshootY), 180f, 0.55f) / renderScreenHpx
            else 0f
            val stretchFromScale = if (previewEdit.scale < 1f)
                rubberbandAttenuate(1f - previewEdit.scale, 0.5f, 0.55f)
            else 0f
            // Pinch-in stretch is applied to ONE axis only — the one the
            // user's first finger landed closer to an edge on — and uses the
            // opposite-edge pivot (so only the touched side moves, the
            // anchored side stays put).
            val pinchAnchorState = com.bearinmind.launcher314.data.WallpaperPreviewBus.pinchAnchor
            val pinchStretchX = if (stretchFromScale > 0f && pinchAnchorState.isHorizontal) stretchFromScale else 0f
            val pinchStretchY = if (stretchFromScale > 0f && !pinchAnchorState.isHorizontal) stretchFromScale else 0f
            val stretchX = stretchFromOvershootX + pinchStretchX
            val stretchY = stretchFromOvershootY + pinchStretchY
            // Pivot priority:
            //   1. If there's offset overshoot, anchor opposite the drag dir.
            //   2. Otherwise if pinch-in stretch active, use the first-finger
            //      anchor (opposite edge of the touch).
            //   3. Else center.
            val stretchPivotX: Float
            val stretchPivotY: Float
            when {
                overshootX != 0f || overshootY != 0f -> {
                    stretchPivotX = when {
                        overshootX > 0f -> 0f
                        overshootX < 0f -> 1f
                        else -> 0.5f
                    }
                    stretchPivotY = when {
                        overshootY > 0f -> 0f
                        overshootY < 0f -> 1f
                        else -> 0.5f
                    }
                }
                stretchFromScale > 0f -> {
                    stretchPivotX = pinchAnchorState.pivot.pivotFractionX
                    stretchPivotY = pinchAnchorState.pivot.pivotFractionY
                }
                else -> { stretchPivotX = 0.5f; stretchPivotY = 0.5f }
            }
            androidx.compose.foundation.Image(
                bitmap = previewCropped.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                colorFilter = wallpaperPreview.colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    // OUTER graphicsLayer (applied last, wraps result): the
                    // directional rubber-band stretch. Pivot = opposite edge
                    // of the drag direction, scaleX/scaleY bump only on the
                    // axis being over-dragged, so the anchor edge stays put
                    // and the dragged side stretches outward.
                    .graphicsLayer {
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                            stretchPivotX,
                            stretchPivotY
                        )
                        scaleX = 1f + stretchX
                        scaleY = 1f + stretchY
                    }
                    // INNER graphicsLayer (applied first): the user's normal
                    // pan/zoom/rotation/flip with translation already clamped
                    // to [-maxOff, +maxOff] so edges are always against the
                    // screen. The stretch layer above then grows from there.
                    .graphicsLayer {
                        rotationZ = previewEdit.rotationDegrees.toFloat()
                        scaleX = baseScale * (if (previewEdit.flipH) -1f else 1f)
                        scaleY = baseScale * (if (previewEdit.flipV) -1f else 1f)
                        translationX = renderOffX
                        translationY = renderOffY
                    }
                    .then(previewBlurMod)
            )
            // Vignette overlay on top of the preview bitmap.
            if (previewEdit.vignette > 0) {
                val alphaMax = (previewEdit.vignette / 100f * 0.85f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = alphaMax * 0.4f),
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = alphaMax)
                                ),
                                radius = 1800f
                            )
                        )
                )
            }
        } else if (customWallpaperPath != null) {
            val blurRadiusDp = (wallpaperBlurPercent / 100f * 25f).dp
            coil.compose.AsyncImage(
                model = java.io.File(customWallpaperPath),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (wallpaperBlurPercent > 0 && android.os.Build.VERSION.SDK_INT >= 31) {
                            Modifier.graphicsLayer {
                                renderEffect = androidx.compose.ui.graphics.BlurEffect(
                                    radiusX = with(density) { blurRadiusDp.toPx() },
                                    radiusY = with(density) { blurRadiusDp.toPx() },
                                    edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                                )
                            }
                        } else Modifier
                    )
            )
        }

        // Dim overlay applies to BOTH custom and system wallpaper. Keep it behind the
        // rest of the launcher content so text/icons aren't dimmed — it only tints the
        // wallpaper.
        // Apply the launcher's wallpaper-dim overlay even during the editor
        // Preview, so the preview accurately reflects how the image will
        // look on the actual home screen (post-Apply the launcher lays the
        // same dim on top, which previously caused the preview to look
        // noticeably brighter than the saved result).
        if (wallpaperDim > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = (wallpaperDim / 100f).coerceIn(0f, 1f)))
            )
        }

        // Layer 1: Home screen (launcher) with gesture detection
        // Only handles the OPENING gesture (swipe up on home screen).
        // Layer 2's nestedScroll handles the CLOSING gesture (swipe down on drawer).
        val swipeDownEnabled = com.bearinmind.launcher314.data.getSwipeDownNotifications(context)

        // Resolve global text color for home screen labels (re-read when refresh triggers)
        var homeTextColorRaw by remember { mutableStateOf(com.bearinmind.launcher314.data.getGlobalTextColor(context)) }
        var homeTextIntensity by remember { mutableIntStateOf(com.bearinmind.launcher314.data.getGlobalTextColorIntensity(context)) }
        // Re-read on any refresh or composition re-entry (use applicationContext for consistent SharedPreferences)
        val appCtx = context.applicationContext
        homeTextColorRaw = com.bearinmind.launcher314.data.getGlobalTextColor(appCtx)
        homeTextIntensity = com.bearinmind.launcher314.data.getGlobalTextColorIntensity(appCtx)
        val homeTextColor = if (homeTextColorRaw != null) {
            val i = homeTextIntensity / 100f
            val b = androidx.compose.ui.graphics.Color(homeTextColorRaw!!)
            androidx.compose.ui.graphics.Color(b.red * i, b.green * i, b.blue * i, b.alpha)
        } else androidx.compose.ui.graphics.Color.White

        val homeFolderBorder = run {
            val bgc = com.bearinmind.launcher314.data.getGlobalIconBgColor(context)
            val intensity = com.bearinmind.launcher314.data.getGlobalIconBgIntensity(context)
            if (bgc != null) androidx.compose.ui.graphics.Color(bgc).copy(alpha = (intensity / 100f).coerceIn(0f, 1f))
            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
        }
        androidx.compose.runtime.CompositionLocalProvider(
            com.bearinmind.launcher314.ui.theme.LocalLabelTextColor provides homeTextColor,
            com.bearinmind.launcher314.ui.theme.LocalFolderBorderColor provides homeFolderBorder
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeDownEnabled) {
                    // FIX: Custom gesture handler replacing detectVerticalDragGestures.
                    // The built-in detector consumes events during its verticalDrag loop
                    // even when we return early from onVerticalDrag, which stole events
                    // from widgets underneath and caused stuttery vertical scrolling on
                    // calendar widgets etc. Bailing at DOWN before any slop tracking or
                    // consumption lets the widget receive a clean, uninterrupted event
                    // stream.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Bail early if a widget is being touched — never run any slop
                        // tracking or consume any events. Just wait for the pointer to
                        // be released so the gesture scope ends cleanly.
                        if (com.bearinmind.launcher314.ui.widgets.WidgetTouchState.isWidgetTouchActive) {
                            do {
                                val e = awaitPointerEvent()
                                if (e.changes.all { !it.pressed }) break
                            } while (true)
                            return@awaitEachGesture
                        }

                        // Bail if drawer already visible or a folder is open.
                        if (swipeUpY.value <= screenHeight * 0.9f || isFolderOpen) {
                            do {
                                val e = awaitPointerEvent()
                                if (e.changes.all { !it.pressed }) break
                            } while (true)
                            return@awaitEachGesture
                        }

                        // Own the gesture — replicate detectVerticalDragGestures logic.
                        var overSlop = 0f
                        val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            overSlop = over
                        } ?: return@awaitEachGesture

                        var isSwipeDown = false
                        var totalDragAmount = overSlop

                        // Process the slop-trigger event as the first drag.
                        if (totalDragAmount > 50f && swipeDownEnabled) isSwipeDown = true
                        if (!isSwipeDown) {
                            coroutineScope.launch {
                                swipeUpY.snapTo((swipeUpY.value + overSlop).coerceIn(0f, screenHeight))
                            }
                        }
                        drag.consume()

                        val success = verticalDrag(drag.id) { change ->
                            val dy = change.positionChange().y
                            totalDragAmount += dy
                            if (totalDragAmount > 50f && !isSwipeDown && swipeDownEnabled) {
                                isSwipeDown = true
                            }
                            if (!isSwipeDown) {
                                coroutineScope.launch {
                                    swipeUpY.snapTo((swipeUpY.value + dy).coerceIn(0f, screenHeight))
                                }
                            }
                            change.consume()
                        }

                        if (success) {
                            // onDragEnd equivalent
                            if (isSwipeDown && totalDragAmount > actionThreshold) {
                                val mode = com.bearinmind.launcher314.data.getSwipeDownMode(context)
                                if (mode == 1) {
                                    com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandQuickSettings(context)
                                } else {
                                    com.bearinmind.launcher314.helpers.NotificationPanelHelper.expandNotificationPanel(context)
                                }
                            } else {
                                coroutineScope.launch {
                                    if (swipeUpY.value < screenHeight - actionThreshold) {
                                        swipeUpY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    } else {
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
                            }
                        } else {
                            // onDragCancel equivalent
                            if (!isSwipeDown) {
                                coroutineScope.launch {
                                    swipeUpY.animateTo(
                                        targetValue = screenHeight,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                    showAppDrawer = false
                                    homeRefreshTrigger++
                                }
                            }
                        }
                    }
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
        } // CompositionLocalProvider

        // Preview-mode overlay: blocks all interaction with the launcher
        // beneath (apps / icons / widgets / dock / drawer) and surfaces two
        // outlined top buttons — Exit (return to editor, preserving state)
        // and Apply (bake + save the wallpaper, then dismiss). Visible only
        // while the wallpaper editor's Preview is active.
        val previewEntry = com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview
        if (previewEntry != null) {
            val previewScope = androidx.compose.runtime.rememberCoroutineScope()
            var isApplyingPreview by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            // Confirmation dialog gating the real wallpaper apply call —
            // matches the Material3 AlertDialog style used by the drawer's
            // folder-delete popup (plain Text title/body + two TextButtons).
            var showApplyConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            // Pinch-to-zoom + drag-to-pan with iOS-style rubber-band feel:
            // gestures write RAW values to the bus (no hard clamp) so the
            // image visually drags past bounds; the render layer applies a
            // rubberband curve so the displayed excess asymptotically tops
            // out. On release, a spring animates raw values back inside
            // bounds — produces the "stretch, then snap" behaviour the user
            // asked for.
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            // The pinch anchor (which side the first finger landed on) lives
            // in `WallpaperPreviewBus.pinchAnchor` so the preview-backdrop
            // render code (above) can read it too.
            val previewTransformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
                val prev = com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview ?: return@rememberTransformableState
                val rawScale = prev.edit.scale * zoomChange
                val rawOffX = prev.edit.offsetX + panChange.x
                val rawOffY = prev.edit.offsetY + panChange.y
                val newEdit = prev.edit.copy(
                    scale = rawScale.coerceIn(0.4f, 6f),  // soft limits to prevent runaway
                    offsetX = rawOffX,
                    offsetY = rawOffY
                )
                com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = prev.copy(edit = newEdit)
            }
            // Spring-back: when the gesture ends, animate raw scale / offset
            // from their current (possibly overshooting) values to the
            // nearest in-bounds target. The LaunchedEffect re-keys on
            // `isTransformInProgress`, so a new gesture mid-animation cancels
            // the spring cleanly.
            androidx.compose.runtime.LaunchedEffect(previewTransformState.isTransformInProgress) {
                if (previewTransformState.isTransformInProgress) return@LaunchedEffect
                val start = com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview
                    ?: return@LaunchedEffect
                val startScale = start.edit.scale
                val startOffX = start.edit.offsetX
                val startOffY = start.edit.offsetY
                val targetScale = startScale.coerceIn(1f, 5f)
                val tMaxOffX = (targetScale - 1f) * screenWidthPx / 2f
                val tMaxOffY = (targetScale - 1f) * screenHeightPx / 2f
                val targetOffX = startOffX.coerceIn(-tMaxOffX, tMaxOffX)
                val targetOffY = startOffY.coerceIn(-tMaxOffY, tMaxOffY)
                if (startScale == targetScale && startOffX == targetOffX && startOffY == targetOffY) {
                    // Already in bounds — still commit so pendingResumeEdit stays in sync.
                    com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit = start.edit
                    return@LaunchedEffect
                }
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    // Critically-damped spring: smoothly arrives at the
                    // target with no overshoot — no secondary bounce in the
                    // opposite direction.
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ) { t, _ ->
                    val cur = com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview ?: return@animate
                    val animEdit = cur.edit.copy(
                        scale = startScale + (targetScale - startScale) * t,
                        offsetX = startOffX + (targetOffX - startOffX) * t,
                        offsetY = startOffY + (targetOffY - startOffY) * t
                    )
                    com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = cur.copy(edit = animEdit)
                    com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit = animEdit
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-screen gesture layer: transformable handles pan/zoom;
                // clickable(indication = null) swallows stray taps so app
                // icons / widgets below can't be launched while previewing.
                val sinkInteraction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // First-finger tracker. Captures the position where
                        // each new gesture starts, classifies it as
                        // horizontal-side or vertical-side based on which
                        // axis it's farther from center, and stores the
                        // OPPOSITE edge as the pinch pivot. Doesn't consume,
                        // so transformable still gets the same down event.
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val fx = (down.position.x / w).coerceIn(0f, 1f)
                                val fy = (down.position.y / h).coerceIn(0f, 1f)
                                val dx = kotlin.math.abs(fx - 0.5f)
                                val dy = kotlin.math.abs(fy - 0.5f)
                                com.bearinmind.launcher314.data.WallpaperPreviewBus.pinchAnchor = if (dx >= dy) {
                                    com.bearinmind.launcher314.data.WallpaperPreviewBus.PinchAnchor(
                                        pivot = com.bearinmind.launcher314.data.WallpaperPreviewBus
                                            .ColorFilterIndependentTransformOrigin(
                                                pivotFractionX = if (fx < 0.5f) 1f else 0f,
                                                pivotFractionY = 0.5f
                                            ),
                                        isHorizontal = true
                                    )
                                } else {
                                    com.bearinmind.launcher314.data.WallpaperPreviewBus.PinchAnchor(
                                        pivot = com.bearinmind.launcher314.data.WallpaperPreviewBus
                                            .ColorFilterIndependentTransformOrigin(
                                                pivotFractionX = 0.5f,
                                                pivotFractionY = if (fy < 0.5f) 1f else 0f
                                            ),
                                        isHorizontal = false
                                    )
                                }
                            }
                        }
                        .transformable(previewTransformState)
                        .clickable(
                            interactionSource = sinkInteraction,
                            indication = null,
                            onClick = {}
                        )
                )
                // Exit + Apply button row pinned to the top-right, matching
                // the editor's top action bar layout (Reset / Preview / Apply
                // sit on the right, outlined, 12dp radius, 1dp #888 border).
                // Exit uses the same red as the editor's Reset (#E53935).
                // Solid dark fill so the buttons stay legible over any
                // wallpaper. 98% opacity so there's still a hint of the image
                // behind, but anything > 90% reads as "solid" to the user.
                val buttonFill = Color(0xFF121212).copy(alpha = 0.98f)
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = null
                            onSettingsClick()
                        },
                        enabled = !isApplyingPreview,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF888888)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = buttonFill
                        )
                    ) {
                        androidx.compose.material3.Text(
                            "Exit",
                            color = Color(0xFFE53935),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showApplyConfirm = true },
                        enabled = !isApplyingPreview,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF888888)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = buttonFill
                        )
                    ) {
                        androidx.compose.material3.Text(
                            "Apply",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                }
                // Apply confirmation dialog — same bare Material3 AlertDialog
                // the folder-remove flow uses. On Apply: actually runs the
                // wallpaper-apply, clears the preview bus, closes the dialog.
                if (showApplyConfirm) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showApplyConfirm = false },
                        title = { androidx.compose.material3.Text("Set as wallpaper") },
                        text = {
                            androidx.compose.material3.Text(
                                "Pressing \"apply\" will set this image as the current wallpaper for both your homescreen & lockscreen."
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showApplyConfirm = false
                                val bmp = previewEntry.sourceBitmap
                                val edit = previewEntry.edit
                                com.bearinmind.launcher314.ui.settings.applyEdited(
                                    context = context,
                                    source = bmp,
                                    edit = edit,
                                    target = android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK,
                                    scope = previewScope,
                                    setApplying = { isApplyingPreview = it },
                                    setStatus = { /* no status surface here */ },
                                    onSuccess = {
                                        com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = null
                                        com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit = null
                                    }
                                )
                            }) { androidx.compose.material3.Text("Apply") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showApplyConfirm = false }) {
                                androidx.compose.material3.Text("Cancel")
                            }
                        }
                    )
                }
                if (isApplyingPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * iOS-style rubber-band clamp. Values inside `[minV, maxV]` pass through;
 * anything past a boundary is attenuated toward an asymptotic ceiling
 * (`limit` = max displayed overshoot). Produces the "stretch, then it can't
 * stretch any further" feel. `c = 0.55` is Apple's published constant.
 */
private fun rubberbandClamp(
    value: Float,
    minV: Float,
    maxV: Float,
    limit: Float = 300f,
    c: Float = 0.55f
): Float {
    return when {
        value in minV..maxV -> value
        value > maxV -> maxV + rubberbandAttenuate(value - maxV, limit, c)
        else -> minV - rubberbandAttenuate(minV - value, limit, c)
    }
}

private fun rubberbandAttenuate(excess: Float, limit: Float, c: Float): Float {
    if (excess <= 0f || limit <= 0f) return 0f
    return (1f - 1f / (excess * c / limit + 1f)) * limit
}
