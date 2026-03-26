package com.bearinmind.launcher314.ui.drawer

import android.util.Log
import androidx.activity.compose.BackHandler
import com.bearinmind.launcher314.data.HomeAppInfo
import com.bearinmind.launcher314.data.loadAppCustomizations
import com.bearinmind.launcher314.data.setCustomization
import com.bearinmind.launcher314.data.removeCustomization
import com.bearinmind.launcher314.ui.home.AppCustomizeDialog
import com.bearinmind.launcher314.helpers.clearCachedIconsForPackage
import androidx.compose.runtime.snapshotFlow
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.rememberUpdatedState
import android.content.IntentFilter
import android.os.Build
import com.bearinmind.launcher314.helpers.uninstallApp
import com.bearinmind.launcher314.helpers.openAppInfo
import com.bearinmind.launcher314.data.getGridSize
import com.bearinmind.launcher314.data.getDrawerIconSizePercent
import com.bearinmind.launcher314.data.getIconTextSizePercent
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.getScrollbarHeightPercent
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarIntensity
import com.bearinmind.launcher314.helpers.getDrawerTransparency
import com.bearinmind.launcher314.data.getDrawerGridRows
import com.bearinmind.launcher314.data.getDrawerPagedMode
import com.bearinmind.launcher314.data.getGlobalIconShape
import com.bearinmind.launcher314.data.getGlobalIconBgColor
import com.bearinmind.launcher314.helpers.FontManager
import com.bearinmind.launcher314.helpers.getIconShape
import androidx.compose.ui.text.font.FontFamily
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bearinmind.launcher314.R
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.ui.components.GridCellHoverIndicator
import com.bearinmind.launcher314.ui.components.LazyGridScrollbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.zIndex
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.bearinmind.launcher314.data.SortOption
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.DrawerData
import com.bearinmind.launcher314.data.HomeDragCallbacks
import com.bearinmind.launcher314.data.EscapeHoverState
import com.bearinmind.launcher314.data.loadDrawerData
import com.bearinmind.launcher314.data.saveDrawerData
import com.bearinmind.launcher314.data.getInstalledApps
import com.bearinmind.launcher314.data.drawableToBitmap
import com.bearinmind.launcher314.data.saveBitmapToFile
import com.bearinmind.launcher314.data.launchApp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    onSearchActiveChanged: (Boolean) -> Unit = {},
    dismissSearchTrigger: Int = 0,
    isDrawerFullyOpen: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onAddToHome: (AppInfo) -> Unit = {},
    onAddFolderToHome: (AppFolder) -> Unit = {},
    homeDragCallbacks: HomeDragCallbacks = HomeDragCallbacks()
) {
    val onDragToHome = homeDragCallbacks.onDragToHome
    val onDragToHomeMove = homeDragCallbacks.onDragToHomeMove
    val onDragToHomeDrop = homeDragCallbacks.onDragToHomeDrop
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Grid size and icon size from settings
    var gridSize by remember { mutableStateOf(getGridSize(context)) }
    var iconSizePercent by remember { mutableStateOf(getDrawerIconSizePercent(context)) }
    var drawerGridRows by remember { mutableStateOf(getDrawerGridRows(context)) }
    var isPagedMode by remember { mutableStateOf(getDrawerPagedMode(context)) }
    var iconTextSizePercent by remember { mutableStateOf(getIconTextSizePercent(context)) }
    var selectedFontFamily by remember { mutableStateOf(FontManager.getSelectedFontFamily(context)) }
    var globalIconShape by remember { mutableStateOf(getGlobalIconShape(context)) }
    var globalIconBgColor by remember { mutableStateOf(getGlobalIconBgColor(context)) }

    // Customize dialog state for drawer apps
    var customizingDrawerApp by remember { mutableStateOf<AppInfo?>(null) }
    var appCustomizations by remember { mutableStateOf(loadAppCustomizations(context)) }

    customizingDrawerApp?.let { app ->
        val homeAppInfo = HomeAppInfo(
            name = app.name,
            packageName = app.packageName,
            iconPath = app.iconPath,
            customization = appCustomizations.customizations[app.packageName]
        )
        AppCustomizeDialog(
            context = context,
            appInfo = homeAppInfo,
            currentCustomization = appCustomizations.customizations[app.packageName],
            globalIconSizePercent = iconSizePercent,
            globalIconTextSizePercent = iconTextSizePercent,
            globalIconShape = globalIconShape,
            globalIconBgColor = globalIconBgColor,
            onSave = { newCustomization ->
                appCustomizations = setCustomization(context, appCustomizations, app.packageName, newCustomization)
                customizingDrawerApp = null
            },
            onReset = {
                appCustomizations = removeCustomization(context, appCustomizations, app.packageName)
                customizingDrawerApp = null
            },
            onDismiss = { customizingDrawerApp = null }
        )
    }

    // Compute icon dp from percentage using fixed reference (screenWidth / 4)
    // Uses reference column count of 4 so icon size is consistent across screens regardless of actual column count
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val iconSize = (screenWidthDp / 4f * 0.55f * iconSizePercent / 100f).toInt()
    val appLabelFontSize = 12.sp * iconTextSizePercent / 100f

    // Trigger to refresh the app list (incremented on resume and package changes)
    var appRefreshTrigger by remember { mutableIntStateOf(0) }

    // Refresh settings when screen becomes visible (coming back from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                gridSize = getGridSize(context)
                iconSizePercent = getDrawerIconSizePercent(context)
                drawerGridRows = getDrawerGridRows(context)
                isPagedMode = getDrawerPagedMode(context)
                iconTextSizePercent = getIconTextSizePercent(context)
                selectedFontFamily = FontManager.getSelectedFontFamily(context)
                globalIconShape = getGlobalIconShape(context)
                globalIconBgColor = getGlobalIconBgColor(context)
                // Refresh app list (picks up uninstalls, new installs, etc.)
                appRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Listen for package install/uninstall broadcasts to refresh the app list
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                // Clear cached icons for the changed package so they regenerate fresh
                val packageName = intent.data?.schemeSpecificPart
                if (packageName != null) {
                    clearCachedIconsForPackage(ctx, packageName)
                }
                appRefreshTrigger++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var appsToMoveToNewFolder by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var openFolder by remember { mutableStateOf<AppFolder?>(null) }

    // Back button: if folder is open, close folder and return to drawer
    BackHandler(enabled = openFolder != null) {
        openFolder = null
    }

    // Trigger to clear selection in MainDrawerContent after folder creation
    var clearSelectionTrigger by remember { mutableIntStateOf(0) }

    // Global folder menu expanded state - shared between drawer and folder screens
    var globalFolderMenuExpanded by remember { mutableStateOf(false) }

    // Folder animation state
    var folderPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var clickedFolderPosition by remember { mutableStateOf(Offset.Zero) }
    var isFolderVisible by remember { mutableStateOf(false) }

    // Folder escape drag state — when an app is dragged out of a folder back to the drawer
    var folderEscapedApp by remember { mutableStateOf<AppInfo?>(null) }
    var folderEscapedFromFolderId by remember { mutableStateOf<String?>(null) }
    var folderEscapeDragPos by remember { mutableStateOf(Offset.Zero) }
    val folderEscapeScope = rememberCoroutineScope()

    // Visual-only escape close animation (separate from interactive overlay to avoid pointer crashes)
    val escapeCloseAnim = remember { Animatable(0f) }
    var escapeCloseOriginX by remember { mutableFloatStateOf(0.5f) }
    var escapeCloseOriginY by remember { mutableFloatStateOf(0.5f) }
    var escapeCloseFolderName by remember { mutableStateOf("") }
    var escapeCloseApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // Drop animation for escaped app icon (flies from release point to grid cell or shrinks into folder)
    val escapeDropAnim = remember { Animatable(0f) }
    var escapeDropApp by remember { mutableStateOf<AppInfo?>(null) }
    var escapeDropStartPos by remember { mutableStateOf(Offset.Zero) }
    var escapeDropTargetPos by remember { mutableStateOf<Offset?>(null) }
    var escapeDropTargetSize by remember { mutableStateOf(IntSize.Zero) }
    var escapeDropToFolder by remember { mutableStateOf(false) } // true = shrink into folder

    // Folder hover during escape drag (hovering escaped app over another folder)
    var escapeHoveredFolderId by remember { mutableStateOf<String?>(null) }

    // Drop zone bounds (shared with MainDrawerContent) for escape drag-to-home
    val drawerDropZoneBoundsState = remember { mutableStateOf(Rect.Zero) }
    var escapeTransferredToHome by remember { mutableStateOf(false) }
    var escapePendingHomeJob by remember { mutableStateOf<Job?>(null) }
    var escapeInDropZone by remember { mutableStateOf(false) }

    // Sort state
    var currentSortOption by remember { mutableStateOf(SortOption.NAME) }
    var isSortAscending by remember { mutableStateOf(true) }

    // Screen dimensions for animation calculations
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Folders state
    var folders by remember { mutableStateOf<List<AppFolder>>(emptyList()) }

    // Load folders from storage
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            folders = loadDrawerData(context).folders
        }
    }

    // Save folders when changed
    fun saveFolders(newFolders: List<AppFolder>) {
        Log.d("FolderDebug", "saveFolders: saving ${newFolders.size} folders")
        newFolders.forEach { f -> Log.d("FolderDebug", "  folder '${f.name}' (${f.id}): apps=${f.appPackageNames}") }
        folders = newFolders
        saveDrawerData(context, DrawerData(folders = newFolders))
    }

    // Load/refresh app list whenever trigger changes (initial load, resume, package changes)
    LaunchedEffect(appRefreshTrigger) {
        withContext(Dispatchers.IO) {
            val apps = getInstalledApps(context)
            withContext(Dispatchers.Main) {
                allApps = apps
                isLoading = false
            }
        }
    }

    // Get apps that are in folders
    val appsInFolders by remember {
        derivedStateOf {
            folders.flatMap { it.appPackageNames }.toSet()
        }
    }

    // Filter and sort apps based on search query (apps in folders stay in folders)
    val filteredApps by remember {
        derivedStateOf {
            val availableApps = allApps.filter { it.packageName !in appsInFolders }
            val searched = if (searchQuery.isBlank()) {
                availableApps
            } else {
                availableApps.filter { app ->
                    app.name.contains(searchQuery, ignoreCase = true)
                }
            }
            // Apply sorting with direction
            val sorted = when (currentSortOption) {
                SortOption.NAME -> if (isSortAscending) searched.sortedBy { it.name.lowercase() } else searched.sortedByDescending { it.name.lowercase() }
                SortOption.INSTALLED -> if (isSortAscending) searched.sortedBy { it.installTime } else searched.sortedByDescending { it.installTime }
                SortOption.UPDATED -> if (isSortAscending) searched.sortedBy { it.lastUpdateTime } else searched.sortedByDescending { it.lastUpdateTime }
                SortOption.SIZE -> if (isSortAscending) searched.sortedBy { it.sizeBytes } else searched.sortedByDescending { it.sizeBytes }
                SortOption.MANUAL -> if (isSortAscending) searched.sortedBy { it.name.lowercase() } else searched.sortedByDescending { it.name.lowercase() }
            }
            sorted
        }
    }

    // Animation values
    val animationProgress by animateFloatAsState(
        targetValue = if (isFolderVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "folder_animation"
    )

    // Calculate transform origin based on folder position
    val transformOriginX = if (screenWidthPx > 0) clickedFolderPosition.x / screenWidthPx else 0.5f
    val transformOriginY = if (screenHeightPx > 0) clickedFolderPosition.y / screenHeightPx else 0.5f

    // Trigger visual-only close animation when escape starts (uses snapshotFlow
    // so the animation runs to completion even after state changes)
    LaunchedEffect(Unit) {
        snapshotFlow { folderEscapedApp }
            .collect { escaped ->
                if (escaped != null) {
                    // Capture current state for visual animation
                    // Note: compute from State vars directly (not transformOriginX/Y which are
                    // regular vals captured stale by LaunchedEffect(Unit))
                    escapeCloseOriginX = if (screenWidthPx > 0) clickedFolderPosition.x / screenWidthPx else 0.5f
                    escapeCloseOriginY = if (screenHeightPx > 0) clickedFolderPosition.y / screenHeightPx else 0.5f
                    escapeCloseFolderName = openFolder?.name ?: ""
                    escapeCloseApps = openFolder?.appPackageNames
                        ?.filter { it.isNotEmpty() && it != escaped.packageName }
                        ?.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                        ?: emptyList()
                    escapeCloseAnim.snapTo(1f)
                    escapeCloseAnim.animateTo(0f, tween(300))
                }
            }
    }

    // Update folder visibility when openFolder changes
    LaunchedEffect(openFolder) {
        if (openFolder != null) {
            // Store the position of the clicked folder
            clickedFolderPosition = folderPositions[openFolder!!.id] ?: Offset(screenWidthPx / 2, screenHeightPx / 2)
            isFolderVisible = true
        }
    }

    // Calculate background with transparency setting
    // 0% = fully opaque (no transparency, solid black background)
    // 100% = fully transparent (see through to home screen behind)
    val drawerTransparency = getDrawerTransparency(LocalContext.current)
    val backgroundAlpha = (100 - drawerTransparency) / 100f
    val drawerBackground = Color(0xFF121212).copy(alpha = backgroundAlpha)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(drawerBackground)
    ) {
        // Main drawer content (always rendered)
        // During escape drag, filter the escaped app out of the source folder's preview
        val displayFolders = if (folderEscapedApp != null && folderEscapedFromFolderId != null) {
            folders.map { f ->
                if (f.id == folderEscapedFromFolderId) {
                    f.copy(appPackageNames = f.appPackageNames - folderEscapedApp!!.packageName)
                } else f
            }
        } else folders

        MainDrawerContent(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isDrawerFullyOpen = isDrawerFullyOpen,
            dismissSearchTrigger = dismissSearchTrigger,
            onSearchFocusChanged = { focused ->
                onSearchActiveChanged(focused)
                // Prevent keyboard from resizing the drawer layout
                val window = (context as? android.app.Activity)?.window
                if (focused) {
                    @Suppress("DEPRECATION")
                    window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                } else {
                    @Suppress("DEPRECATION")
                    window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            },
            isLoading = isLoading,
            folders = displayFolders,
            filteredApps = filteredApps,
            allApps = allApps,
            gridSize = gridSize,
            iconSize = iconSize,
            labelFontSize = appLabelFontSize,
            labelFontFamily = selectedFontFamily,
            iconClipShape = getIconShape(globalIconShape),
            iconBgColor = globalIconBgColor,
            globalIconShapeName = globalIconShape,
            drawerGridRows = drawerGridRows,
            isPagedMode = isPagedMode,
            currentSortOption = currentSortOption,
            onSortOptionChanged = { currentSortOption = it },
            isSortAscending = isSortAscending,
            onSortDirectionChanged = { isSortAscending = it },
            onFolderClick = { clickedFolder ->
                // Always use the latest folder from state (UI item may be stale after recent adds)
                val latestFolder = folders.find { it.id == clickedFolder.id } ?: clickedFolder
                Log.d("FolderDebug", "onFolderClick: opening folder '${latestFolder.name}' (${latestFolder.id})")
                Log.d("FolderDebug", "  apps in folder: ${latestFolder.appPackageNames}")
                clickedFolderPosition = folderPositions[latestFolder.id] ?: Offset(screenWidthPx / 2, screenHeightPx / 2)
                openFolder = latestFolder
            },
            onAppClick = { launchApp(context, it) },
            onUninstallApp = { app -> uninstallApp(context, app.packageName) },
            onAppInfo = { app -> openAppInfo(context, app.packageName) },
            onSettingsClick = onSettingsClick,
            onCreateFolderClick = { showCreateFolderDialog = true },
            onCreateFolderWithApps = { apps ->
                appsToMoveToNewFolder = apps
                showCreateFolderDialog = true
            },
            onFolderPositioned = { folderId, position ->
                folderPositions = folderPositions + (folderId to position)
            },
            onAddAppToFolder = { app, folder ->
                val updatedFolder = folder.copy(
                    appPackageNames = folder.appPackageNames + app.packageName
                )
                Log.d("FolderDebug", "onAddAppToFolder: adding ${app.packageName} to folder '${folder.name}' (${folder.id})")
                Log.d("FolderDebug", "  old apps: ${folder.appPackageNames}")
                Log.d("FolderDebug", "  new apps: ${updatedFolder.appPackageNames}")
                saveFolders(folders.map { if (it.id == folder.id) updatedFolder else it })
                // Update openFolder so FolderContentScreen sees the new app
                if (openFolder?.id == folder.id) {
                    openFolder = updatedFolder
                }
            },
            onDeleteFolder = { folder ->
                // Delete the folder - apps will automatically return to main drawer
                saveFolders(folders.filter { it.id != folder.id })
            },
            onAddToHome = onAddToHome,
            onAddFolderToHome = onAddFolderToHome,
            homeDragCallbacks = HomeDragCallbacks(
                onDragToHome = onDragToHome,
                onDragToHomeMove = onDragToHomeMove,
                onDragToHomeDrop = onDragToHomeDrop
            ),
            onBulkAddToFolder = { apps, folder ->
                // Add all selected apps to the folder
                val updatedFolder = folder.copy(
                    appPackageNames = folder.appPackageNames + apps.map { it.packageName }
                )
                saveFolders(folders.map { if (it.id == folder.id) updatedFolder else it })
                if (openFolder?.id == folder.id) {
                    openFolder = updatedFolder
                }
            },
            isFolderMenuExpanded = globalFolderMenuExpanded,
            onFolderMenuExpandedChange = { globalFolderMenuExpanded = it },
            clearSelectionTrigger = clearSelectionTrigger,
            dropAnimatingPackage = escapeDropApp?.packageName,
            onDropTargetPositioned = { pos, size ->
                if (escapeDropApp != null && escapeDropTargetPos == null) {
                    escapeDropTargetPos = pos
                    escapeDropTargetSize = size
                    // Now we have both start and target — animate
                    folderEscapeScope.launch {
                        escapeDropAnim.snapTo(0f)
                        escapeDropAnim.animateTo(
                            1f,
                            tween(300, easing = FastOutSlowInEasing)
                        )
                        escapeDropApp = null
                    }
                }
            },
            onCustomizeApp = { app -> customizingDrawerApp = app },
            escapeHoverState = EscapeHoverState(
                folderId = if (escapeHoveredFolderId != null && folderEscapedApp != null) escapeHoveredFolderId else null,
                iconPath = if (escapeHoveredFolderId != null && folderEscapedApp != null) folderEscapedApp?.iconPath else null,
                dropZoneBoundsRef = drawerDropZoneBoundsState,
                isEscapeDragActive = folderEscapedApp != null,
                isInDropZone = escapeInDropZone
            )
        )

        // Folder content overlay with animation from folder position
        if (openFolder != null || animationProgress > 0f) {
            openFolder?.let { currentFolder ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // During escape drag: freeze scale at 1 to avoid pointer
                            // coordinate distortion, hide with alpha=0 instead
                            scaleX = if (folderEscapedApp != null) 1f else animationProgress
                            scaleY = if (folderEscapedApp != null) 1f else animationProgress
                            // Set transform origin to folder position
                            transformOrigin = TransformOrigin(
                                transformOriginX.coerceIn(0f, 1f),
                                transformOriginY.coerceIn(0f, 1f)
                            )
                            alpha = if (folderEscapedApp != null) 0f else animationProgress
                        }
                ) {
                    FolderContentScreen(
                        folder = currentFolder,
                        allApps = allApps,
                        gridSize = gridSize,
                        iconSize = iconSize,
                        labelFontSize = appLabelFontSize,
                        labelFontFamily = selectedFontFamily,
                        onBack = {
                            isFolderVisible = false
                            // Delay clearing openFolder until animation completes
                        },
                        onRemoveApp = { packageName ->
                            val updatedFolder = currentFolder.copy(
                                appPackageNames = currentFolder.appPackageNames - packageName
                            )
                            saveFolders(folders.map { if (it.id == updatedFolder.id) updatedFolder else it })
                            openFolder = updatedFolder
                        },
                        onRemoveApps = { packageNames ->
                            val updatedFolder = currentFolder.copy(
                                appPackageNames = currentFolder.appPackageNames - packageNames.toSet()
                            )
                            saveFolders(folders.map { if (it.id == updatedFolder.id) updatedFolder else it })
                            openFolder = updatedFolder
                        },
                        onUninstallApp = { app -> uninstallApp(context, app.packageName) },
                        onAppInfo = { app -> openAppInfo(context, app.packageName) },
                        onDeleteFolder = {
                            saveFolders(folders.filter { it.id != currentFolder.id })
                            isFolderVisible = false
                        },
                        onRenameFolder = { newName ->
                            val updatedFolder = currentFolder.copy(name = newName)
                            saveFolders(folders.map { if (it.id == updatedFolder.id) updatedFolder else it })
                            openFolder = updatedFolder
                        },
                        folders = folders,
                        onMoveToFolder = { packageName, targetFolder ->
                            // Remove from current folder
                            val updatedCurrentFolder = currentFolder.copy(
                                appPackageNames = currentFolder.appPackageNames - packageName
                            )
                            // Add to target folder
                            val updatedTargetFolder = targetFolder.copy(
                                appPackageNames = targetFolder.appPackageNames + packageName
                            )
                            // Update both folders
                            saveFolders(folders.map { folder ->
                                when (folder.id) {
                                    currentFolder.id -> updatedCurrentFolder
                                    targetFolder.id -> updatedTargetFolder
                                    else -> folder
                                }
                            })
                            openFolder = updatedCurrentFolder
                        },
                        onMoveAppsToFolder = { packageNames, targetFolder ->
                            // Remove from current folder
                            val updatedCurrentFolder = currentFolder.copy(
                                appPackageNames = currentFolder.appPackageNames - packageNames.toSet()
                            )
                            // Add to target folder
                            val updatedTargetFolder = targetFolder.copy(
                                appPackageNames = targetFolder.appPackageNames + packageNames
                            )
                            // Update both folders
                            saveFolders(folders.map { folder ->
                                when (folder.id) {
                                    currentFolder.id -> updatedCurrentFolder
                                    targetFolder.id -> updatedTargetFolder
                                    else -> folder
                                }
                            })
                            openFolder = updatedCurrentFolder
                        },
                        isFolderMenuExpanded = globalFolderMenuExpanded,
                        onFolderMenuExpandedChange = { globalFolderMenuExpanded = it },
                        onAddToHome = onAddToHome,
                        onReorderApps = { newAppNames ->
                            val updatedFolder = currentFolder.copy(appPackageNames = newAppNames)
                            saveFolders(folders.map { if (it.id == updatedFolder.id) updatedFolder else it })
                            openFolder = updatedFolder
                        },
                        onEscapeToDrawer = { app, dragAbsPos ->
                            // Start escape: show overlay on finger, start folder close animation
                            folderEscapedApp = app
                            folderEscapedFromFolderId = currentFolder.id
                            folderEscapeDragPos = dragAbsPos
                            // Start smooth close animation immediately
                            isFolderVisible = false
                        },
                        onEscapeDragMove = { absPos ->
                            folderEscapeDragPos = absPos
                            if (escapeTransferredToHome) {
                                // Already transferred — forward position to home screen
                                onDragToHomeMove(absPos)
                            } else {
                                // Check drop zone (drag-to-home)
                                val wasInZone = escapeInDropZone
                                val dzBounds = drawerDropZoneBoundsState.value
                                escapeInDropZone = dzBounds != Rect.Zero &&
                                    dzBounds.contains(absPos)
                                if (escapeInDropZone && !wasInZone) {
                                    escapeHoveredFolderId = null
                                    escapePendingHomeJob?.cancel()
                                    escapePendingHomeJob = folderEscapeScope.launch {
                                        delay(600)
                                        if (escapeInDropZone && folderEscapedApp != null) {
                                            onDragToHome(folderEscapedApp!!, absPos)
                                            escapeTransferredToHome = true
                                        }
                                    }
                                } else if (!escapeInDropZone && wasInZone) {
                                    escapePendingHomeJob?.cancel()
                                    escapePendingHomeJob = null
                                }
                                // Detect folder hover (only when not in drop zone)
                                if (!escapeInDropZone) {
                                    val cellApproxSize = with(density) { iconSize.dp.toPx() } * 1.5f
                                    escapeHoveredFolderId = folderPositions.entries.firstOrNull { (folderId, centerPos) ->
                                        val half = cellApproxSize / 2f
                                        absPos.x in (centerPos.x - half)..(centerPos.x + half) &&
                                            absPos.y in (centerPos.y - half)..(centerPos.y + half)
                                    }?.key
                                }
                            }
                        },
                        onEscapeDragEnd = {
                            val escapedApp = folderEscapedApp
                            val folderId = folderEscapedFromFolderId
                            val targetFolderId = escapeHoveredFolderId

                            // Cancel any pending home transfer
                            escapePendingHomeJob?.cancel()
                            escapePendingHomeJob = null

                            if (escapeTransferredToHome) {
                                // Dropped on home screen — keep app in folder, just signal drop
                                onDragToHomeDrop()
                                escapeTransferredToHome = false
                                escapeInDropZone = false
                                escapeHoveredFolderId = null
                                folderEscapedFromFolderId = null
                                folderEscapedApp = null
                                openFolder = null
                            } else if (targetFolderId != null) {
                                // Persist folder changes
                                if (escapedApp != null && folderId != null) {
                                    if (targetFolderId != folderId) {
                                        val updatedFolders = folders.map { f ->
                                            when {
                                                f.id == folderId ->
                                                    f.copy(appPackageNames = f.appPackageNames - escapedApp.packageName)
                                                f.id == targetFolderId && escapedApp.packageName !in f.appPackageNames ->
                                                    f.copy(appPackageNames = f.appPackageNames + escapedApp.packageName)
                                                else -> f
                                            }
                                        }
                                        saveFolders(updatedFolders)
                                    }
                                }
                                // Shrink animation into folder
                                val folderCenter = folderPositions[targetFolderId]
                                escapeDropApp = folderEscapedApp
                                escapeDropStartPos = folderEscapeDragPos
                                escapeDropTargetPos = folderCenter
                                escapeDropTargetSize = IntSize.Zero
                                escapeDropToFolder = true
                                folderEscapeScope.launch {
                                    escapeDropAnim.snapTo(0f)
                                    escapeDropAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                                    escapeDropApp = null
                                    escapeDropToFolder = false
                                }
                                escapeInDropZone = false
                                escapeHoveredFolderId = null
                                folderEscapedFromFolderId = null
                                folderEscapedApp = null
                                openFolder = null
                            } else {
                                // Dropped on drawer — remove from folder, animate to grid cell
                                if (escapedApp != null && folderId != null) {
                                    val folder = folders.find { it.id == folderId }
                                    if (folder != null) {
                                        val updatedFolder = folder.copy(
                                            appPackageNames = folder.appPackageNames - escapedApp.packageName
                                        )
                                        saveFolders(folders.map { if (it.id == folderId) updatedFolder else it })
                                    }
                                }
                                escapeDropApp = folderEscapedApp
                                escapeDropStartPos = folderEscapeDragPos
                                escapeDropTargetPos = null
                                escapeInDropZone = false
                                folderEscapedFromFolderId = null
                                folderEscapedApp = null
                                openFolder = null
                            }
                        }
                    )
                }
            }
        }

        // Visual-only escape close animation — separate from interactive overlay
        // so the close animation can scale/fade without distorting pointer coordinates
        if (escapeCloseAnim.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = escapeCloseAnim.value
                        scaleX = p
                        scaleY = p
                        alpha = p
                        transformOrigin = TransformOrigin(
                            escapeCloseOriginX.coerceIn(0f, 1f),
                            escapeCloseOriginY.coerceIn(0f, 1f)
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header area (visual only)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.33f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = escapeCloseFolderName,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    // Content area with app icons (visual only)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            val cols = gridSize
                            val rows = maxOf(cols, (escapeCloseApps.size + cols - 1) / cols)
                            for (row in 0 until rows) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    for (col in 0 until cols) {
                                        val idx = row * cols + col
                                        val app = escapeCloseApps.getOrNull(idx)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (app != null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    AsyncImage(
                                                        model = java.io.File(app.iconPath),
                                                        contentDescription = app.name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.size(iconSize.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = app.name,
                                                        fontSize = appLabelFontSize,
                                                        fontFamily = selectedFontFamily ?: FontFamily.Default,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drag overlay for app escaped from folder — rendered above everything
        // Hidden when transferred to home screen (home screen renders its own overlay)
        if (folderEscapedApp != null && !escapeTransferredToHome) {
            val escapedApp = folderEscapedApp!!
            val dragDensity = LocalDensity.current
            val escIconSize = with(dragDensity) { iconSize.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (folderEscapeDragPos.x - escIconSize / 2).toInt(),
                            (folderEscapeDragPos.y - escIconSize / 2).toInt()
                        )
                    }
                    .size(iconSize.dp)
                    .zIndex(1000f)
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                        alpha = 0.9f
                    }
            ) {
                AsyncImage(
                    model = java.io.File(escapedApp.iconPath),
                    contentDescription = escapedApp.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Drop animation — icon flies to grid cell, shrinks into folder, or fades to bottom-center
        if (escapeDropApp != null) {
            val dropApp = escapeDropApp!!
            val dropDensity = LocalDensity.current
            val escIconSizePx = with(dropDensity) { iconSize.dp.toPx() }
            val p = escapeDropAnim.value
            val targetPos = escapeDropTargetPos
            val isFolderDrop = escapeDropToFolder && targetPos != null
            val isOffPage = !escapeDropToFolder && targetPos != null && escapeDropTargetSize == IntSize.Zero
            // Interpolate position
            val currentX = if (targetPos != null) {
                val endX = when {
                    isFolderDrop -> targetPos.x  // folder center
                    isOffPage -> targetPos.x     // bottom-center
                    else -> targetPos.x + escapeDropTargetSize.width / 2f  // grid cell center
                }
                escapeDropStartPos.x + (endX - escapeDropStartPos.x) * p
            } else escapeDropStartPos.x
            val currentY = if (targetPos != null) {
                val endY = when {
                    isFolderDrop -> targetPos.y  // folder center
                    isOffPage -> targetPos.y     // bottom-center
                    else -> targetPos.y + escapeDropTargetSize.height / 3f  // grid cell
                }
                escapeDropStartPos.y + (endY - escapeDropStartPos.y) * p
            } else escapeDropStartPos.y
            // Scale
            val currentScale = when {
                isFolderDrop -> 1.1f * (1f - p * 0.75f) // shrink from 1.1 to ~0.275 (like normal folder drop)
                isOffPage -> 1.1f * (1f - p)             // shrink to 0
                else -> 1.1f - 0.1f * p                  // 1.1 → 1.0
            }
            // Alpha
            val currentAlpha = when {
                isFolderDrop -> (1f - p).coerceAtLeast(0f)  // fade out
                isOffPage -> (1f - p).coerceAtLeast(0f)     // fade out
                else -> 0.9f + 0.1f * p                     // 0.9 → 1.0
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (currentX - escIconSizePx / 2).toInt(),
                            (currentY - escIconSizePx / 2).toInt()
                        )
                    }
                    .size(iconSize.dp)
                    .zIndex(999f)
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        alpha = currentAlpha
                    }
            ) {
                AsyncImage(
                    model = java.io.File(dropApp.iconPath),
                    contentDescription = dropApp.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Fallback: if the drop target cell isn't composed (e.g., on a different page in paged mode),
    // onDropTargetPositioned will never fire. Animate icon toward bottom-center and fade out.
    LaunchedEffect(escapeDropApp) {
        if (escapeDropApp != null) {
            delay(100) // give composition time to fire onDropTargetPositioned
            if (escapeDropTargetPos == null) {
                // Target cell not visible — animate toward bottom-center (page dots area)
                escapeDropTargetPos = Offset(screenWidthPx / 2f, screenHeightPx)
                escapeDropTargetSize = IntSize.Zero
                folderEscapeScope.launch {
                    escapeDropAnim.snapTo(0f)
                    escapeDropAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                    escapeDropApp = null
                }
            }
        }
    }

    // Clear openFolder after close animation completes — but NOT during an
    // active escape drag (removing the composable while pointer is down crashes)
    LaunchedEffect(animationProgress) {
        if (animationProgress == 0f && !isFolderVisible && openFolder != null && folderEscapedApp == null) {
            openFolder = null
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = {
                showCreateFolderDialog = false
                appsToMoveToNewFolder = emptyList()
                clearSelectionTrigger++
            },
            onCreate = { folderName ->
                // Create folder with apps if any were selected/highlighted
                val newFolder = AppFolder(
                    name = folderName,
                    appPackageNames = appsToMoveToNewFolder.map { it.packageName }
                )
                saveFolders(folders + newFolder)
                showCreateFolderDialog = false
                appsToMoveToNewFolder = emptyList()
                clearSelectionTrigger++
            }
        )
    }
}

// MainDrawerContent moved to MainDrawerContent.kt
// FolderItem, FolderPreviewIcon, AppItem, FolderAppItem, SelectableAppItem, CreateFolderDialog moved to DrawerAppItems.kt
// FolderContentScreen moved to DrawerFolderContent.kt


// Storage functions moved to data/DrawerStorage.kt
// drawableToBitmap, saveBitmapToFile moved to data/HomeScreenStorage.kt
// launchApp moved to data/HomeScreenStorage.kt

