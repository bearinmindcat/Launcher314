package com.bearinmind.launcher314.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerInputChange
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
import com.bearinmind.launcher314.data.dispatch
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
    // Launcher3 phone (non-sheet) all-apps SHIFT RANGE = R.dimen
    // .all_apps_starting_vertical_translate = 300dp. The drawer does NOT slide
    // up the full screen from the bottom — on a phone it starts only 300dp below
    // its open position and rises that short distance WHILE fading in. (Tablets/
    // "sheet" mode use the full height; phones use this small fixed translate.)
    // This is the canonical closed position / gesture range for the drawer.
    val drawerRangePx = with(density) { 300.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()

    // Swipe tracking - starts at screenHeight (closed), goes to 0 (fully open)
    var lastSwipeUpY by rememberSaveable { mutableFloatStateOf(drawerRangePx) }
    val swipeUpY = remember { Animatable(lastSwipeUpY) }

    // SYNCHRONOUS drag position (the "1:1 with the finger" fix). While actively
    // dragging we write this plain float DIRECTLY on the touch event (no
    // coroutineScope.launch { swipeUpY.snapTo() }, which deferred the position to
    // the NEXT frame and made the drawer trail the finger by ~1 frame — the main
    // reason it didn't feel 1:1 with Lawnchair). The Animatable (swipeUpY) is
    // used only for the release settle. effectiveSwipeY unifies the two: read it
    // everywhere instead of swipeUpY.value.
    var dragShift by remember { mutableFloatStateOf(lastSwipeUpY) }
    var isDrawerDragging by remember { mutableStateOf(false) }
    val effectiveSwipeY by remember(screenHeight) {
        derivedStateOf { if (isDrawerDragging) dragShift else swipeUpY.value }
    }

    // SETTLE physics: a soft springy bounce as the drawer lands, with asymmetric
    // springs for direction:
    //   • OPEN  (target 0):       crisp + bouncy  — spring(0.6, 1500)
    //   • CLOSE (target range):   softer, gentler — spring(0.8, 800)
    // The release velocity carries into the spring so it continues your flick's
    // momentum, overshoots slightly past the resting point, and bounces back.
    // (swipeUpY has no bounds, so the overshoot physically moves the layer; the
    // progress consumers clamp to 0..1 so alphas/blur never go out of range.)
    suspend fun settleDrawer(target: Float, velocityPxPerSec: Float) {
        val opening = target < swipeUpY.value
        swipeUpY.animateTo(
            targetValue = target,
            animationSpec = if (opening)
                spring(dampingRatio = 0.6f, stiffness = 1500f)
            else
                spring(dampingRatio = 0.8f, stiffness = 800f),
            initialVelocity = velocityPxPerSec
        )
    }

    // #1 — Material You drawer tint (1:1 with Lawnchair): the all-apps scrim color
    // is derived from the WALLPAPER (dynamic color, API 31+) and flips with the
    // system light/dark mode, instead of a fixed #121212. Scoped to JUST the scrim
    // so the rest of the app's gray theme is untouched. Falls back to #121212 below
    // API 31 (no dynamic color available).
    val systemDark = isSystemInDarkTheme()
    val drawerDynamicScheme = if (Build.VERSION.SDK_INT >= 31) {
        if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    val drawerScrimColor = drawerDynamicScheme?.surface
        ?: androidx.compose.ui.graphics.Color(0xFF121212)
    // The label/text color that CONTRASTS the dynamic scrim (Lawnchair flips the
    // drawer text color with its background). Used as the drawer's default label
    // color so a light Material-You scrim in light mode never hides white labels.
    val drawerOnColor = drawerDynamicScheme?.onSurface
        ?: androidx.compose.ui.graphics.Color.White

    // Noise grain: a tiled grayscale-noise brush overlaid faintly on the
    // frosted drawer so it reads like real glass, not flat plastic blur.
    // Generated once.
    val noiseBrush = remember {
        val n = 128
        val px = IntArray(n * n)
        val rnd = java.util.Random(7L)
        for (i in px.indices) {
            val v = rnd.nextInt(256)
            px[i] = android.graphics.Color.argb(255, v, v, v)
        }
        val bmp = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, n, 0, 0, n, n)
        androidx.compose.ui.graphics.ShaderBrush(
            androidx.compose.ui.graphics.ImageShader(
                bmp.asImageBitmap(),
                androidx.compose.ui.graphics.TileMode.Repeated,
                androidx.compose.ui.graphics.TileMode.Repeated
            )
        )
    }

    // Track if search is active in drawer (disables swipe-to-close)
    var isDrawerSearchActive by remember { mutableStateOf(false) }
    var dismissSearchTrigger by remember { mutableStateOf(0) }
    var searchDismissed by remember { mutableStateOf(false) }

    // Track if app drawer should be shown
    var showAppDrawer by remember { mutableStateOf(false) }

    // Recent Apps overlay state — toggled by GestureAction.ShowRecentApps.
    // The overlay composable itself lands in a later phase; the flag is
    // wired up now so the dispatcher has a target.
    var showRecentAppsOverlay by remember { mutableStateOf(false) }

    // Bridge between the gesture dispatcher and the host's UI state.
    // Each callback wraps the existing state mutation we already used to
    // perform from inline branches.
    val gestureUiCallbacks = remember {
        object : com.bearinmind.launcher314.data.GestureUiCallbacks {
            override fun openDrawer() {
                coroutineScope.launch {
                    showAppDrawer = true
                    settleDrawer(0f, 0f)   // Lawnchair decelerate open
                }
            }
            override fun showRecentApps() {
                showRecentAppsOverlay = true
            }
        }
    }

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

    // REST-STATE RESET (Launcher3 calls moveToRestState() in onStop): when the
    // launcher stops — e.g. you LAUNCH AN APP FROM THE DRAWER, or the screen
    // turns off — the drawer snaps closed, and coming back always lands on the
    // home screen, never a stale open drawer.
    val drawerLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(drawerLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (showAppDrawer || isDrawerDragging)) {
                isDrawerDragging = false
                showAppDrawer = false
                homeRefreshTrigger++
                coroutineScope.launch { swipeUpY.snapTo(drawerRangePx) }
            }
        }
        drawerLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { drawerLifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            settleDrawer(drawerRangePx, 0f)   // Lawnchair decelerate close
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
                swipeUpY.snapTo(drawerRangePx)
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
            settleDrawer(0f, 0f)   // Lawnchair decelerate open
        }
    }

    // Safety: if screenHeight changed (e.g., returning from widget configure activity
    // with different insets), reset drawer to closed if it was meant to be closed.
    // Without this, a saved pixel position slightly less than new screenHeight
    // makes the drawer appear partially open.
    LaunchedEffect(screenHeight) {
        if (!showAppDrawer && swipeUpY.value != drawerRangePx) {
            swipeUpY.snapTo(drawerRangePx)
            lastSwipeUpY = drawerRangePx
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

    // ===================== LAWNCHAIR / LAUNCHER3 MANUAL ALL-APPS =====================
    // Faithful port of Launcher3 AllAppsSwipeController (the *_MANUAL variants).
    // ONE linear progress 0 (home) -> 1 (all-apps) drives everything, but each
    // visual property is CLAMPED to its own sub-range (clampToProgress) — the
    // STAGGER is what makes it read as Lawnchair. Vertical shift is LINEAR 1:1
    // with the finger; responsiveness for short swipes comes from the FLING
    // commit, NOT from gain/decelerate.
    // EXACT Launcher3 AllAppsSwipeController manual ranges, but computed AT DRAW
    // TIME inside each layer's graphicsLayer lambda (reading effectiveSwipeY there
    // defers the read to the draw phase). Composition holds only BOOLEAN gates via
    // derivedStateOf — they invalidate composition ONLY when they flip, so a drag
    // frame re-draws three layers and recomposes NOTHING. This is the Compose
    // equivalent of how Lawnchair animates (per-frame property sets on views,
    // never re-inflating) and is what keeps the drag at full frame rate.
    //
    // The ranges (p = openness 0 home -> 1 all-apps, all LINEAR — the "fast then
    // slow at the top" feel is the RELEASE SETTLE, not the drag):
    //   home  scale/alpha/blur : 0.0   -> 0.4   (WORKSPACE_SCALE/BLUR_MANUAL)
    //   scrim fade             : 0.117 -> 0.4   (SCRIM_FADE_MANUAL)
    //   content fade           : 0.4   -> 0.8   (ALL_APPS_FADE_MANUAL)
    //   translation            : full 300dp, 1:1 with the finger
    val drawerComposed by remember {
        derivedStateOf { showAppDrawer || effectiveSwipeY < drawerRangePx }
    }
    val drawerFullyOpen by remember {
        derivedStateOf { effectiveSwipeY == 0f && showAppDrawer }
    }
    val drawerBlockerVisible by remember {
        derivedStateOf { effectiveSwipeY > 10f }
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
        if (swipeUpY.value < drawerRangePx - 5f) {
            showAppDrawer = true
        } else if (!isDrawerDragging) {
            // Settled fully closed and not actively dragging — force showAppDrawer
            // false so the full-screen drawer Box (and its tap-blocker) is removed
            // from the composition and can't swallow home-screen touches. Safety
            // net against any release path that forgets to reset it.
            showAppDrawer = false
        }
    }

    // Back button: drawer open → close drawer, home screen → do nothing
    BackHandler(enabled = true) {
        if (showAppDrawer && swipeUpY.value < drawerRangePx) {
            // Drawer is visible — close it and return to home screen
            coroutineScope.launch {
                settleDrawer(drawerRangePx, 0f)   // Lawnchair decelerate close
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
                // While the drawer is partially open / actively being dragged closed,
                // intercept ALL vertical scroll and write the SYNCHRONOUS drag float
                // directly (no launch{snapTo} lag — same 1:1 fix as the open drag).
                val cur = if (isDrawerDragging) dragShift else swipeUpY.value
                if (cur > 0f && !isDrawerSearchActive) {
                    if (!isDrawerDragging) { dragShift = swipeUpY.value; isDrawerDragging = true }
                    dragShift = (dragShift + available.y).coerceIn(0f, drawerRangePx)
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
                if (available.y > 0f && !isDrawerDragging && swipeUpY.value == 0f && !isDrawerSearchActive) {
                    // Begin the close drag synchronously from fully-open.
                    dragShift = swipeUpY.value
                    isDrawerDragging = true
                    dragShift = (dragShift + available.y).coerceIn(0f, drawerRangePx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Close commit mirrors the OPEN rule: a fling sets direction (down
                // closes, up reopens) regardless of distance; otherwise commit by
                // position past 0.4. Hand the synchronous drag position to the
                // Animatable, clear the drag flag, then DECELERATE-settle.
                if (isDrawerDragging) {
                    val startY = dragShift
                    swipeUpY.snapTo(startY)
                    isDrawerDragging = false
                    val flingThreshold = 600f
                    val closedness = startY / drawerRangePx  // 0 open -> 1 closed
                    val shouldClose = when {
                        available.y > flingThreshold -> true
                        available.y < -flingThreshold -> false
                        else -> closedness > 0.4f
                    }
                    if (shouldClose) {
                        settleDrawer(drawerRangePx, available.y)
                        showAppDrawer = false
                        homeRefreshTrigger++
                    } else {
                        settleDrawer(0f, available.y)
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Safety net: if a drag is somehow still active after onPreFling,
                // settle it to the nearest stable state.
                if (isDrawerDragging) {
                    val startY = dragShift
                    swipeUpY.snapTo(startY)
                    isDrawerDragging = false
                    if (startY / drawerRangePx > 0.4f) {
                        settleDrawer(drawerRangePx, available.y)
                        showAppDrawer = false
                        homeRefreshTrigger++
                    } else {
                        settleDrawer(0f, available.y)
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
            // Base user wallpaper-blur + a depth ramp tied to the first 40% of the
            // swipe (Launcher3 BLUR_MANUAL 0->0.4), so the wallpaper softens as the
            // drawer takes over. Radius computed at DRAW time (state read inside the
            // graphicsLayer lambda) so dragging doesn't recompose the image.
            coil.compose.AsyncImage(
                model = java.io.File(customWallpaperPath),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            Modifier.graphicsLayer {
                                val pr = (1f - (effectiveSwipeY / drawerRangePx)).coerceIn(0f, 1f)
                                val ph = (pr / 0.4f).coerceIn(0f, 1f)
                                val r = wallpaperBlurPercent / 100f * 25.dp.toPx() + ph * 22.dp.toPx()
                                renderEffect = if (r > 0.5f) {
                                    androidx.compose.ui.graphics.BlurEffect(
                                        radiusX = r,
                                        radiusY = r,
                                        edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                                    )
                                } else null
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

        // ===== Lawnchair DepthController: blur + zoom-out the WALLPAPER =====
        // 1:1 with Lawnchair's frosted depth: the SYSTEM/live wallpaper behind the
        // (transparent) launcher window is blurred via Window.setBackgroundBlurRadius
        // and pushed back via WallpaperManager.setWallpaperZoomOut, both ramped with
        // the depth phase (BLUR_MANUAL 0->0.4). Custom wallpaper is blurred by its
        // own RenderEffect above. API 31+, only where cross-window blur is supported
        // (Samsung One UI does). On unsupported devices this is a graceful no-op.
        if (Build.VERSION.SDK_INT >= 31 &&
            wallpaperMode != com.bearinmind.launcher314.data.WALLPAPER_MODE_CUSTOM) {
            val depthActivity = remember(context) { context.findActivity() }
            val maxWindowBlurPx = with(density) { 23.dp.toPx() }   // Launcher3 max depth blur
            // setWallpaperZoomOut is a hidden API (not in the public SDK), so we
            // reach it via REFLECTION the way third-party launchers (Lawnchair/Nova)
            // do. If the hidden-API blocklist or the OEM blocks it, this resolves to
            // null and we simply skip the zoom (graceful — never crashes).
            val zoomMethod = remember {
                runCatching {
                    android.app.WallpaperManager::class.java.getMethod(
                        "setWallpaperZoomOut",
                        android.os.IBinder::class.java,
                        Float::class.javaPrimitiveType
                    )
                }.getOrNull()
            }
            DisposableEffect(depthActivity) {
                onDispose {
                    depthActivity?.window?.let { w ->
                        runCatching { w.setBackgroundBlurRadius(0) }
                        runCatching {
                            val wm = w.context.getSystemService(Context.WALLPAPER_SERVICE)
                                as? android.app.WallpaperManager
                            w.decorView.windowToken?.let { tok -> zoomMethod?.invoke(wm, tok, 0f) }
                        }
                    }
                }
            }
            LaunchedEffect(depthActivity) {
                val win = depthActivity?.window ?: return@LaunchedEffect
                val wmSvc = win.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val blurSupported = wmSvc?.isCrossWindowBlurEnabled == true
                val wallpaperMgr = win.context.getSystemService(Context.WALLPAPER_SERVICE)
                    as? android.app.WallpaperManager
                // FIX: read the SNAPSHOT state (effectiveSwipeY) inside the flow and
                // derive the phase here. Previously this read the plain `homePhase`
                // val, which was captured once and never updated — so the blur never
                // actually ramped. Now it tracks the drag/animation live.
                snapshotFlow { effectiveSwipeY }.collect { y ->
                    val prog = (1f - (y / drawerRangePx)).coerceIn(0f, 1f)
                    val blurPhase = (prog / 0.4f).coerceIn(0f, 1f)   // BLUR_MANUAL 0->0.4
                    if (blurSupported) {
                        runCatching { win.setBackgroundBlurRadius((blurPhase * maxWindowBlurPx).roundToInt()) }
                    }
                    // Wallpaper zoom-out tracks the full open progress (Lawnchair
                    // pushes the wallpaper back gradually as you open). Subtle (0.5 max).
                    if (zoomMethod != null) {
                        runCatching {
                            win.decorView.windowToken?.let { tok ->
                                zoomMethod.invoke(wallpaperMgr, tok, (prog * 0.5f).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
        }

        // Layer 1: Home screen (launcher) with gesture detection
        // Only handles the OPENING gesture (swipe up on home screen).
        // Layer 2's nestedScroll handles the CLOSING gesture (swipe down on drawer).
        // Swipe-down is "enabled" iff the user hasn't reassigned the swipe-down
        // gesture to None — the dispatcher itself decides what the action is.
        val swipeDownEnabled = com.bearinmind.launcher314.data
            .getGestureEnabled(context, com.bearinmind.launcher314.data.GestureId.SWIPE_DOWN) &&
            com.bearinmind.launcher314.data
                .getGestureAction(context, com.bearinmind.launcher314.data.GestureId.SWIPE_DOWN) !=
                com.bearinmind.launcher314.data.GestureAction.None

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
        val homeHideIconText = com.bearinmind.launcher314.data.getHideIconText(appCtx)
        androidx.compose.runtime.CompositionLocalProvider(
            com.bearinmind.launcher314.ui.theme.LocalLabelTextColor provides homeTextColor,
            com.bearinmind.launcher314.ui.theme.LocalFolderBorderColor provides homeFolderBorder,
            com.bearinmind.launcher314.ui.theme.LocalHideIconText provides homeHideIconText
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

                        // Bail when a detached icon is in edit mode — vertical drags
                        // there are for moving / resizing the icon, not for sliding
                        // the drawer up or firing the swipe-down action.
                        if (com.bearinmind.launcher314.ui.widgets.DetachedEditState.isEditing) {
                            do {
                                val e = awaitPointerEvent()
                                if (e.changes.all { !it.pressed }) break
                            } while (true)
                            return@awaitEachGesture
                        }

                        // Bail if drawer already visible or a folder is open.
                        if (swipeUpY.value <= drawerRangePx * 0.9f || isFolderOpen) {
                            do {
                                val e = awaitPointerEvent()
                                if (e.changes.all { !it.pressed }) break
                            } while (true)
                            return@awaitEachGesture
                        }

                        // Own the gesture only when it is clearly VERTICAL — the
                        // Launcher3 / Lawnchair "single-axis" rule. The built-in
                        // awaitVerticalTouchSlopOrCancellation fires the instant the
                        // vertical component alone crosses slop, with NO comparison to
                        // horizontal travel, so a mostly-sideways diagonal (e.g. a page
                        // swipe on an edge page, or a flick toward the status bar) could
                        // hijack the drawer / notification gesture — the "diagonal feels
                        // off vs One UI" problem.
                        //
                        // Instead, accumulate total dx/dy WITHOUT consuming until the
                        // travel passes slop, then commit to the drawer ONLY if the
                        // vertical travel dominates (|dy| > |dx|). If horizontal wins,
                        // bail without consuming so the HorizontalPager (a CHILD, so it
                        // sees events first) keeps the gesture cleanly. If a child has
                        // already consumed the move, defer to it.
                        var overSlop = 0f
                        val touchSlop = viewConfiguration.touchSlop
                        var accumDx = 0f
                        var accumDy = 0f
                        var committedDrag: PointerInputChange? = null
                        while (committedDrag == null) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@awaitEachGesture
                            if (!change.pressed) return@awaitEachGesture        // released before slop
                            if (change.isConsumed) return@awaitEachGesture      // child (pager) owns it
                            accumDx += change.positionChange().x
                            accumDy += change.positionChange().y
                            if (accumDx * accumDx + accumDy * accumDy > touchSlop * touchSlop) {
                                if (kotlin.math.abs(accumDy) > kotlin.math.abs(accumDx)) {
                                    // Vertical dominates — take the gesture.
                                    overSlop = accumDy
                                    change.consume()
                                    committedDrag = change
                                } else {
                                    // Horizontal dominates — let the pager have it.
                                    return@awaitEachGesture
                                }
                            }
                        }
                        val drag = committedDrag

                        // INTENT LOCKED BY INITIAL DIRECTION (the home gesture only
                        // runs while the drawer is closed, so up = open-drawer, down =
                        // swipe-down action). overSlop's sign is the trigger direction.
                        // Locking it means a swipe that STARTS up and then reverses
                        // down just closes the drawer — it never fires the swipe-down
                        // action (no stray notification shade), and vice-versa. This is
                        // the fix for "other touch events firing while dragging".
                        val isSwipeDown = overSlop > 0f && swipeDownEnabled
                        var totalDragAmount = overSlop
                        // Track fling velocity so release can commit Lawnchair-style:
                        // any upward fling opens (downward closes) regardless of how
                        // far you dragged — this is why short swipes still open.
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPointerInputChange(drag)

                        // Only translate the drawer mid-drag when swipe-up is
                        // assigned to OpenDrawer (the default). When the user
                        // reassigns swipe-up to "open an app" or similar, the
                        // drawer must NOT slide up while the finger moves —
                        // the action fires on release without visual drawer
                        // motion. `swipeUpMovesDrawer` is captured at gesture
                        // start so it doesn't change mid-drag.
                        val swipeUpEnabled = com.bearinmind.launcher314.data
                            .getGestureEnabled(context, com.bearinmind.launcher314.data.GestureId.SWIPE_UP)
                        val swipeUpMovesDrawer = swipeUpEnabled && com.bearinmind.launcher314.data
                            .getGestureAction(context, com.bearinmind.launcher314.data.GestureId.SWIPE_UP)
                            .let { it is com.bearinmind.launcher314.data.GestureAction.OpenDrawer }

                        // Process the slop-trigger event as the first drag.
                        if (!isSwipeDown && swipeUpMovesDrawer) {
                            // SYNCHRONOUS: seed the drag float from the current
                            // position and write it directly (no launch{snapTo}),
                            // so the drawer tracks the finger with zero frame lag.
                            dragShift = swipeUpY.value
                            isDrawerDragging = true
                            showAppDrawer = true
                            dragShift = (dragShift + overSlop).coerceIn(0f, drawerRangePx)
                        }
                        drag.consume()

                        val success = verticalDrag(drag.id) { change ->
                            velocityTracker.addPointerInputChange(change)
                            val dy = change.positionChange().y
                            totalDragAmount += dy
                            if (!isSwipeDown && swipeUpMovesDrawer) {
                                // LINEAR 1:1 finger mapping (Launcher3 manual drag is
                                // LINEAR — no gain, no decelerate), written SYNCHRONOUSLY
                                // on the touch event so the drawer moves exactly with
                                // the finger. Short-swipe responsiveness comes from the
                                // fling commit below.
                                isDrawerDragging = true
                                dragShift = (dragShift + dy).coerceIn(0f, drawerRangePx)
                            }
                            change.consume()
                        }

                        if (success) {
                            // onDragEnd equivalent
                            if (isSwipeDown) {
                                // Pure swipe-DOWN gesture (locked by initial direction):
                                // the drawer was never engaged, so just fire the user's
                                // swipe-down action if they dragged down far enough. No
                                // drawer state to reset (it never moved).
                                if (totalDragAmount > actionThreshold) {
                                    com.bearinmind.launcher314.data.getGestureAction(
                                        context,
                                        com.bearinmind.launcher314.data.GestureId.SWIPE_DOWN
                                    ).let { action ->
                                        action.dispatch(context, gestureUiCallbacks)
                                    }
                                }
                            } else {
                                // Capture fling velocity (px/s). Negative = upward
                                // (opening). Read BEFORE launching so it's the true
                                // release speed.
                                val releaseVelocityY = velocityTracker.calculateVelocity().y
                                coroutineScope.launch {
                                    // Hand the synchronous drag position to the
                                    // Animatable, then stop reading dragShift so the
                                    // settle spring drives the visuals seamlessly.
                                    if (swipeUpMovesDrawer) swipeUpY.snapTo(dragShift)
                                    isDrawerDragging = false
                                    val swipeUpAction = com.bearinmind.launcher314.data.getGestureAction(
                                        context,
                                        com.bearinmind.launcher314.data.GestureId.SWIPE_UP
                                    )
                                    // Launcher3 commit rule (AbstractStateChangeTouch-
                                    // Controller): a FLING overrides the position
                                    // threshold entirely — up opens, down closes,
                                    // whatever the drag distance. Otherwise commit if
                                    // progress passed SUCCESS_TRANSITION_PROGRESS, which
                                    // for the manual all-apps transition is 0.4
                                    // (ALL_APPS_STATE_TRANSITION_MANUAL).
                                    val flingThreshold = 600f   // px/s — a normal
                                    // brisk swipe counts as a fling and opens, so you
                                    // don't have to drag a long way.
                                    val openProgress = 1f - (swipeUpY.value / drawerRangePx)
                                    val shouldOpen = when {
                                        releaseVelocityY < -flingThreshold -> true
                                        releaseVelocityY > flingThreshold -> false
                                        else -> openProgress > 0.4f   // ALL_APPS_STATE_TRANSITION_MANUAL
                                    }
                                    if (shouldOpen &&
                                        swipeUpAction is com.bearinmind.launcher314.data.GestureAction.OpenDrawer) {
                                        // Commit open — Launcher3 decelerate settle, with
                                        // a duration scaled by the release velocity.
                                        showAppDrawer = true
                                        settleDrawer(0f, releaseVelocityY)
                                    } else {
                                        // Cancel back to home (short slow drag, or a
                                        // downward fling), or fire a reassigned action.
                                        settleDrawer(drawerRangePx, releaseVelocityY)
                                        showAppDrawer = false
                                        homeRefreshTrigger++
                                        if (swipeUpEnabled &&
                                            swipeUpAction !is com.bearinmind.launcher314.data.GestureAction.OpenDrawer &&
                                            swipeUpAction != com.bearinmind.launcher314.data.GestureAction.None &&
                                            totalDragAmount < -actionThreshold) {
                                            swipeUpAction.dispatch(context, gestureUiCallbacks)
                                        }
                                    }
                                }
                            }
                        } else {
                            // onDragCancel equivalent
                            if (!isSwipeDown) {
                                coroutineScope.launch {
                                    if (swipeUpMovesDrawer) swipeUpY.snapTo(dragShift)
                                    isDrawerDragging = false
                                    settleDrawer(drawerRangePx, 0f)
                                    showAppDrawer = false
                                    homeRefreshTrigger++
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    // Frosted-dissolve look, computed AT DRAW TIME: reading
                    // effectiveSwipeY inside this lambda defers the read to the draw
                    // phase, so a drag frame only re-DRAWS this layer — it does NOT
                    // recompose the whole launcher tree (home grid + widgets + drawer)
                    // per pixel like the old composition-computed vals did. Per-frame
                    // property sets, never re-layout — that's the fluidity.
                    // Home APPS fade out (1 -> 0), scale back, and BLUR as they fade
                    // (frost dissolve) over the first 40% of the swipe; through the
                    // transparent drawer you then see the blurred WALLPAPER.
                    if (drawerToHomeActive) {
                        alpha = homeScreenFadeInAlpha
                        renderEffect = null
                    } else {
                        val pr = (1f - (effectiveSwipeY / drawerRangePx)).coerceIn(0f, 1f)
                        val ph = (pr / 0.4f).coerceIn(0f, 1f)   // WORKSPACE/BLUR_MANUAL 0->0.4
                        alpha = 1f - ph
                        val s = 1f - 0.08f * ph                  // 1.0 -> 0.92
                        scaleX = s
                        scaleY = s
                        renderEffect = if (Build.VERSION.SDK_INT >= 31 && ph > 0.001f) {
                            val r = ph * 30.dp.toPx()            // 0 -> 30dp depth blur
                            androidx.compose.ui.graphics.BlurEffect(
                                r, r, androidx.compose.ui.graphics.TileMode.Clamp
                            )
                        } else null
                    }
                }
        ) {
            LauncherScreen(
                gestureUiCallbacks = gestureUiCallbacks,
                onOpenAppDrawer = {
                    coroutineScope.launch {
                        showAppDrawer = true
                        settleDrawer(0f, 0f)   // Lawnchair decelerate open
                    }
                },
                onOpenSettings = onSettingsClick,
                onOpenWidgets = {
                    // Close drawer before navigating to widgets so returning lands on home screen
                    if (showAppDrawer) {
                        coroutineScope.launch {
                            swipeUpY.snapTo(drawerRangePx)
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
                        swipeUpY.snapTo(drawerRangePx)
                    }
                    showAppDrawer = false
                    homeRefreshTrigger++
                }
            )
        }

        // Layer 1.5: SCRIM — the all-apps dark background (Launcher3 scrim).
        // FULL-SCREEN and NON-TRANSLATING so its top edge never slides up the
        // screen (the exact "I can see the top edge of the drawer" complaint).
        // Fades in with the content; the user's drawer transparency setting caps
        // its max opacity. Gated on effectiveSwipeY so it renders during the
        // synchronous drag (swipeUpY only updates on release).
        if (drawerComposed) {
            val scrimMaxAlpha = ((100 - com.bearinmind.launcher314.helpers
                .getDrawerTransparency(context)) / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // SCRIM_FADE_MANUAL 0.117 -> 0.4, computed at DRAW time
                        // (state read inside the lambda = no per-frame recomposition).
                        alpha = if (drawerToHomeActive) {
                            drawerToHomeFadeAlpha * scrimMaxAlpha
                        } else {
                            val pr = (1f - (effectiveSwipeY / drawerRangePx)).coerceIn(0f, 1f)
                            ((pr - 0.117f) / 0.283f).coerceIn(0f, 1f) * scrimMaxAlpha
                        }
                    }
                    .background(drawerScrimColor)
            )

            // Layer 1.6: NOISE GRAIN — faint tiled grayscale noise over the frosted
            // area. Makes the blur read as real glass instead of flat plastic.
            // Fades with the scrim; draw-phase alpha like everything else.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (drawerToHomeActive) {
                            drawerToHomeFadeAlpha * 0.05f
                        } else {
                            val pr = (1f - (effectiveSwipeY / drawerRangePx)).coerceIn(0f, 1f)
                            ((pr - 0.117f) / 0.283f).coerceIn(0f, 1f) * 0.05f
                        }
                    }
                    .background(noiseBrush)
            )
        }

        // Layer 2: App drawer CONTENT — icons / search, transparent background
        // (the scrim above provides the dark). Launcher3: this is the same view
        // as the translation, so it DRAGS UP 1:1 WITH THE FINGER (offset =
        // effectiveSwipeY) and fades in as it rises. effectiveSwipeY is the
        // SYNCHRONOUS drag position, so it tracks the finger with no frame lag.
        if (drawerComposed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, effectiveSwipeY.roundToInt()) }
                    .graphicsLayer {
                        // ALL_APPS_FADE_MANUAL 0.4 -> 0.8, computed at DRAW time so
                        // dragging never recomposes the app grid underneath.
                        alpha = if (drawerToHomeActive) drawerToHomeFadeAlpha else {
                            val pr = (1f - (effectiveSwipeY / drawerRangePx)).coerceIn(0f, 1f)
                            ((pr - 0.4f) / 0.4f).coerceIn(0f, 1f)
                        }
                    }
                    .clip(RoundedCornerShape(topStart = drawerCornerRadius, topEnd = drawerCornerRadius))
                    .nestedScroll(nestedScrollConnection)
            ) {
                AppDrawerScreen(
                    dismissSearchTrigger = dismissSearchTrigger,
                    // Background is the external scrim above (Launcher3 separates
                    // scrim fade from content fade), so the drawer paints none.
                    drawBackground = false,
                    // Drawer label color contrasts the dynamic Material-You scrim
                    // (used only when the user hasn't set a custom global text color).
                    defaultLabelColor = drawerOnColor,
                    onSearchActiveChanged = {
                        isDrawerSearchActive = it
                        if (it) searchDismissed = false
                    },
                    isDrawerFullyOpen = drawerFullyOpen,
                    onSettingsClick = onSettingsClick,
                    onAddToHome = addAppToHome,
                    onAddFolderToHome = addFolderToHome,
                    homeDragCallbacks = HomeDragCallbacks(
                        onDragToHome = onDrawerDragToHome,
                        onDragToHomeMove = onDrawerDragToHomeMove,
                        onDragToHomeDrop = onDrawerDragToHomeDrop
                    )
                )
                // Block taps/interactions while the drawer is still visibly
                // mid-transition (icons not yet at their resting position).
                // FIX: previously also gated on `swipeUpY.isRunning`, which kept
                // the blocker alive for the full ~600-1000 ms tail of the
                // StiffnessLow spring after the drawer was visually open. That
                // made swipe-down-to-close feel unresponsive immediately after
                // swipe-up-to-open. Once value <= 10f the drawer is "open
                // enough" and stray-tap risk is gone.
                if (drawerBlockerVisible) {
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
                            // Just clear the preview backdrop. The launcher's
                            // pending-resume watcher reopens the editor with
                            // the in-flight edit state — no settings round-
                            // trip needed since the wallpaper section there
                            // was removed.
                            com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = null
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

    // Recent Apps overlay (issue #40, Phase 4). Triggered when the user
    // assigns a gesture to ShowRecentApps. Overlays the entire launcher; a
    // backdrop tap or the Close button dismisses it.
    com.bearinmind.launcher314.ui.home.RecentAppsOverlay(
        visible = showRecentAppsOverlay,
        onDismiss = { showRecentAppsOverlay = false }
    )
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

/** Unwrap a (possibly ContextWrapper-nested) Context to its hosting Activity. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
