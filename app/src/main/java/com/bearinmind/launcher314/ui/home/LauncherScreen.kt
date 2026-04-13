package com.bearinmind.launcher314.ui.home

import com.bearinmind.launcher314.ui.components.EdgeScrollIndicators
import com.bearinmind.launcher314.ui.components.handleEdgeScrollDetection
import com.bearinmind.launcher314.ui.components.GridCellHoverIndicator
import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.drawToBitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.bearinmind.launcher314.helpers.getIconShape
import com.bearinmind.launcher314.helpers.getShapedExpDir
import com.bearinmind.launcher314.helpers.getBgTintedDir
import com.bearinmind.launcher314.helpers.getShapedBgTintedDir
import com.bearinmind.launcher314.helpers.getGlobalShapedDir
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.generateShapedBgTintedIcon
import com.bearinmind.launcher314.helpers.generateBgTintedIcon
import com.bearinmind.launcher314.helpers.parseBlendMode
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import com.bearinmind.launcher314.data.getGlobalIconShape
import com.bearinmind.launcher314.data.getGlobalIconBgColor
import com.bearinmind.launcher314.data.getDockEnabled
import com.bearinmind.launcher314.data.getDoubleTapLockEnabled
import com.bearinmind.launcher314.data.getWidgetPaddingPercent
import com.bearinmind.launcher314.data.getWidgetRoundedCornersEnabled
import com.bearinmind.launcher314.data.getWidgetCornerRadiusPercent
import com.bearinmind.launcher314.data.WIDGET_MAX_PADDING_DP
import com.bearinmind.launcher314.data.WIDGET_MAX_CORNER_RADIUS_DP
import com.bearinmind.launcher314.services.AppDrawerAccessibilityService
import com.bearinmind.launcher314.ui.drawer.CreateFolderDialog
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.getDockColumns
import com.bearinmind.launcher314.data.getDrawerPagedMode
import com.bearinmind.launcher314.data.getGridSize
import com.bearinmind.launcher314.data.getHomeGridSize
import com.bearinmind.launcher314.data.getHomeGridRows
import com.bearinmind.launcher314.data.getHomeIconSizePercent
import com.bearinmind.launcher314.data.getIconTextSizePercent
import com.bearinmind.launcher314.helpers.FontManager
import androidx.compose.ui.text.font.FontFamily
import com.bearinmind.launcher314.helpers.openAppInfo
import com.bearinmind.launcher314.helpers.uninstallApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import com.bearinmind.launcher314.ui.widgets.WidgetManager
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import com.bearinmind.launcher314.ui.widgets.WidgetDragState
import com.bearinmind.launcher314.ui.widgets.handleWidgetMove
import com.bearinmind.launcher314.ui.widgets.getWidgetTargetCells
import com.bearinmind.launcher314.ui.widgets.calculateWidgetDropTarget
import com.bearinmind.launcher314.ui.widgets.calculateWidgetDropTargetFromCenter
import com.bearinmind.launcher314.ui.widgets.canPlaceWidgetAt
import com.bearinmind.launcher314.ui.widgets.calculateWidgetCenter
import com.bearinmind.launcher314.ui.widgets.WidgetResizeState
import com.bearinmind.launcher314.ui.widgets.WidgetResizeOverlay
import com.bearinmind.launcher314.ui.widgets.ResizeDimensions
import com.bearinmind.launcher314.ui.widgets.canWidgetResize
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarIntensity
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import kotlin.math.roundToInt
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import com.bearinmind.launcher314.ui.components.SliderConfigs
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.view.ViewGroup
import com.bearinmind.launcher314.data.GridItem
import com.bearinmind.launcher314.data.GridItemType
import com.bearinmind.launcher314.data.HomeScreenDataV2
import com.bearinmind.launcher314.data.HomeScreenApp
import com.bearinmind.launcher314.data.DockApp
import com.bearinmind.launcher314.data.DockFolder
import com.bearinmind.launcher314.data.HomeFolder
import com.bearinmind.launcher314.data.HomeScreenData
import com.bearinmind.launcher314.data.HomeAppInfo
import com.bearinmind.launcher314.data.HomeGridCell
import com.bearinmind.launcher314.data.RemoveAnimState
import com.bearinmind.launcher314.data.flatIndexToRowColumn
import com.bearinmind.launcher314.data.rowColumnToFlatIndex
import com.bearinmind.launcher314.data.migrateAppToGridItem
import com.bearinmind.launcher314.data.migrateWidgetToGridItem
import com.bearinmind.launcher314.data.getOccupiedCells
import com.bearinmind.launcher314.data.canPlaceGridItem
import com.bearinmind.launcher314.data.AppCustomization
import com.bearinmind.launcher314.data.AppCustomizations
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.loadAppCustomizations
import com.bearinmind.launcher314.data.loadHomeScreenData
import com.bearinmind.launcher314.data.setCustomization
import com.bearinmind.launcher314.data.removeCustomization
import com.bearinmind.launcher314.data.saveHomeScreenData
import com.bearinmind.launcher314.data.loadAvailableApps
import com.bearinmind.launcher314.data.launchApp
import com.bearinmind.launcher314.data.LauncherUtils
import com.bearinmind.launcher314.helpers.openWallpaperPicker
import com.bearinmind.launcher314.helpers.openWidgetPicker

/** Resolve the final icon path considering bg-tint and/or shaped+bg-tint bitmaps. */
private fun resolveBgTintIconPath(
    context: Context,
    packageName: String,
    hasAnyShape: Boolean,
    fallbackPath: String,
    shapeName: String? = null,
    tintColor: Int = 0,
    tintAlpha: Float = 1f
): String {
    return try {
        if (hasAnyShape && shapeName != null) {
            generateShapedBgTintedIcon(context, packageName, shapeName, tintColor, tintAlpha)
        } else {
            generateBgTintedIcon(context, packageName, tintColor, tintAlpha)
        }
    } catch (_: Exception) { fallbackPath }
}

/** Overlay label composable — respects hideLabel, customLabel, and customization. */
@Composable
private fun OverlayLabel(
    appInfo: HomeAppInfo,
    gridIconTextSpacer: Dp,
    gridAppNameFont: TextUnit,
    selectedFontFamily: FontFamily?,
    textAlpha: Float
) {
    if (appInfo.customization?.hideLabel == true) return
    val label = appInfo.customization?.customLabel?.takeIf { it.isNotEmpty() } ?: appInfo.name
    Spacer(modifier = Modifier.height(gridIconTextSpacer))
    Text(
        text = label,
        fontSize = gridAppNameFont,
        fontFamily = selectedFontFamily ?: FontFamily.Default,
        color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = textAlpha },
        style = MaterialTheme.typography.bodySmall.copy(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black,
                offset = Offset(1f, 1f),
                blurRadius = 3f
            )
        )
    )
}

/**
 * Overlay content for a dragged app — renders icon + label matching DraggableGridCell layout exactly.
 * Extracted to a separate composable to keep LauncherScreen method under JVM 64KB limit.
 */
@Composable
private fun OverlayAppContent(
    context: android.content.Context,
    appInfo: HomeAppInfo,
    iconSizeDp: Int,
    iconSizePercent: Int,
    gridIconTextSpacer: Dp,
    gridAppNameFont: TextUnit,
    selectedFontFamily: FontFamily?,
    textAlpha: Float,
    globalIconShape: String?,
    showLabel: Boolean,
    globalIconBgColor: Int? = null
) {
    val hasCustomIcon = appInfo.customization?.customIconPath?.let { File(it).exists() } == true
    val hasShapeExp = appInfo.customization?.iconShapeExp != null
    val hasPerAppShape = appInfo.customization?.iconShape != null
    val iconPath = if (hasCustomIcon) {
        appInfo.customization!!.customIconPath!!
    } else if (hasShapeExp) {
        File(getShapedExpDir(context), "${appInfo.packageName}.png").let {
            if (it.exists()) it.absolutePath else appInfo.iconPath
        }
    } else if (!hasPerAppShape && globalIconShape != null) {
        File(getGlobalShapedDir(context), "${appInfo.packageName}.png").let {
            if (it.exists()) it.absolutePath
            else try { getOrGenerateGlobalShapedIcon(context, appInfo.packageName, globalIconShape) } catch (_: Exception) { appInfo.iconPath }
        }
    } else appInfo.iconPath
    val hasBgTint = appInfo.customization?.iconTintBackgroundOnly == true && appInfo.customization?.iconTintColor != null
    val hasAnyShape = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
    val effectiveShape = appInfo.customization?.iconShapeExp ?: appInfo.customization?.iconShape ?: globalIconShape
    val finalIconPath = if (hasBgTint && !hasCustomIcon) {
        val tintColor = appInfo.customization?.iconTintColor?.toInt() ?: 0
        val tintAlpha = (appInfo.customization?.iconTintIntensity ?: 100) / 100f
        resolveBgTintIconPath(context, appInfo.packageName, hasAnyShape, iconPath, effectiveShape, tintColor, tintAlpha)
    } else iconPath
    val hasAnyExp = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
    val clipShape = if (hasCustomIcon) {
        getIconShape(appInfo.customization?.iconShapeExp ?: appInfo.customization?.iconShape ?: globalIconShape)
    } else if (!hasAnyExp) getIconShape(appInfo.customization?.iconShape) else null
    val tintFilter = if (hasBgTint) null else appInfo.customization?.iconTintColor?.let { tintColor ->
        val intensity = (appInfo.customization?.iconTintIntensity ?: 100) / 100f
        ColorFilter.tint(Color(tintColor.toInt()).copy(alpha = intensity), parseBlendMode(appInfo.customization?.iconTintBlendMode))
    }
    // Use actual dp size to match cell layout (not graphicsLayer scale)
    val perAppSizePercent = appInfo.customization?.iconSizePercent ?: iconSizePercent
    val perAppIconSizeDp = (iconSizeDp * perAppSizePercent / iconSizePercent.toFloat()).dp
    val perAppFontSize = appInfo.customization?.iconTextSizePercent?.let { 12.sp * it / 100f } ?: gridAppNameFont
    val perAppFontFamily = appInfo.customization?.labelFontId?.let { id ->
        FontManager.bundledFonts.find { it.id == id }?.fontFamily
            ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
    } ?: selectedFontFamily ?: FontFamily.Default
    val labelHidden = appInfo.customization?.hideLabel == true
    val displayLabel = appInfo.customization?.customLabel?.takeIf { it.isNotEmpty() } ?: appInfo.name
    // When bg color is set, generate icon with user color as bg layer
    val useBgColorIcon = globalIconBgColor != null && !hasCustomIcon
    val bgColorEffectiveShape = if (useBgColorIcon) {
        appInfo.customization?.iconShapeExp
            ?: appInfo.customization?.iconShape
            ?: globalIconShape
    } else null
    val displayIconPath = if (useBgColorIcon && bgColorEffectiveShape != null) {
        try { getOrGenerateBgColorShapedIcon(context, appInfo.packageName, bgColorEffectiveShape, globalIconBgColor!!) }
        catch (_: Exception) { finalIconPath }
    } else finalIconPath
    val isBgColorIcon = displayIconPath != finalIconPath

    Column(
        modifier = Modifier
            .wrapContentHeight(unbounded = true)
            .graphicsLayer { clip = false },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = File(displayIconPath),
                contentDescription = appInfo.name,
                contentScale = if (isBgColorIcon) ContentScale.Fit else if (clipShape != null) ContentScale.Crop else ContentScale.Fit,
                colorFilter = tintFilter,
                modifier = Modifier
                    .size(perAppIconSizeDp)
                    .then(if (!isBgColorIcon && clipShape != null) Modifier.clip(clipShape) else Modifier)
            )
        }
        // Always render label (alpha 0 when hidden) to match cell layout
        if (showLabel) {
            Spacer(modifier = Modifier.height(gridIconTextSpacer))
            Text(
                text = displayLabel,
                fontSize = perAppFontSize,
                fontFamily = perAppFontFamily,
                color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (labelHidden) 0f else textAlpha },
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

/**
 * LauncherScreen - A home screen with drag and drop app placement
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LauncherScreen(
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenWidgets: () -> Unit = {},
    refreshTrigger: Int = 0,
    onFolderOpenChanged: (Boolean) -> Unit = {},
    externalDragItem: Any? = null,
    externalDragInitialPos: Offset = Offset.Zero,
    externalDragFingerPos: Offset = Offset.Zero,
    externalDragDropSignal: Int = 0,
    onExternalDragComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    // edgeScrollZonePx is computed below in proportional sizing section

    // Grid settings (columns x rows)
    var gridColumns by remember { mutableStateOf(getHomeGridSize(context)) }
    var gridRows by remember { mutableStateOf(getHomeGridRows(context)) }
    var iconSizePercent by remember { mutableStateOf(getHomeIconSizePercent(context)) }
    var iconTextSizePercent by remember { mutableStateOf(getIconTextSizePercent(context)) }
    var selectedFontFamily by remember { mutableStateOf(FontManager.getSelectedFontFamily(context)) }
    var globalIconShape by remember { mutableStateOf(getGlobalIconShape(context)) }
    var globalIconBgColor by remember { mutableStateOf(getGlobalIconBgColor(context)) }
    var globalIconBgIntensity by remember { mutableIntStateOf(com.bearinmind.launcher314.data.getGlobalIconBgIntensity(context)) }
    // Re-read on every recomposition so it picks up changes from settings immediately
    // Use applicationContext to ensure same SharedPreferences instance as settings
    val appContext = context.applicationContext
    globalIconBgIntensity = com.bearinmind.launcher314.data.getGlobalIconBgIntensity(appContext)
    globalIconShape = getGlobalIconShape(appContext)
    globalIconBgColor = getGlobalIconBgColor(appContext)


    // Persistent stack page state — remembers which widget is shown in each stack
    val stackPageMap = remember { mutableStateMapOf<String, Int>() }
    // Load saved stack pages from prefs on first composition
    LaunchedEffect(Unit) {
        val prefs = appContext.getSharedPreferences("app_drawer_settings", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("widget_stack_pages", null)
        if (saved != null) {
            saved.split(",").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    stackPageMap[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
        }
    }

    // Home screen selection state — uses "page_position" keys to uniquely identify cells
    var selectedHomeCells by remember { mutableStateOf<Set<String>>(emptySet()) }
    var homeSelectionModeActive by remember { mutableStateOf(false) }
    var showCreateHomeFolderDialog by remember { mutableStateOf(false) }
    var pendingFolderCellKey by remember { mutableStateOf("") }

    // Dock settings - get from user preferences (needed for proportional sizing below)
    val dockSlots = getDockColumns(context)
    val isDockEnabled = getDockEnabled(context)

    // ========== Proportional Sizing ==========
    // Compute cell dimensions and derive all sizes proportionally.
    // Uses min(cellWidth, cellHeight) so content always fits the constraining dimension.
    // Fractions calibrated so a 360dp phone with 4 cols produces the current hardcoded values.
    // Reference: 360dp phone, 4 cols, 6 rows → cellWidth=82dp, cellHeight~110dp → basis=82dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val gridHPadding = (screenWidthDp * 0.044f).dp   // ~16dp on 360dp phone
    val gridVPadding = (screenWidthDp * 0.022f).dp    // ~8dp on 360dp phone
    val edgeScrollZone = (screenWidthDp * 0.111f).dp  // ~40dp on 360dp phone
    val gridCellWidth = (screenWidthDp - screenWidthDp * 0.044f * 2) / gridColumns
    val dockCellWidth = (screenWidthDp - screenWidthDp * 0.044f * 2) / dockSlots
    // Estimate cell height: screen height minus dock (~56dp), nav dots (~20dp), vertical padding
    val gridCellHeight = (screenHeightDp - 76f - screenWidthDp * 0.022f * 2) / gridRows
    // Use the smaller of width/height so content never overflows
    val gridCellBasis = minOf(gridCellWidth, gridCellHeight)
    val dockCellBasis = minOf(dockCellWidth, gridCellHeight)

    // Grid cell proportional sizes (structural — based on constraining dimension)
    val gridMarkerHalfSize = (gridCellBasis * 0.073f).dp
    val gridPlusMarkerSize = (gridCellBasis * 0.146f).dp
    val gridPlusMarkerFont = (gridCellBasis * 0.122f).sp
    val gridHoverCornerRadius = (gridCellBasis * 0.146f).dp

    // Text and spacer — scaled by icon text size percent, matching app drawer
    val gridAppNameFont = 12.sp * iconTextSizePercent / 100f
    val gridIconTextSpacer = 4.dp

    // Dock proportional sizes
    val dockMarkerHalfSize = (dockCellBasis * 0.073f).dp
    val dockHoverCornerRadius = (dockCellBasis * 0.146f).dp

    // Icon size from percentage using fixed reference (screenWidth / 4)
    // Uses reference column count of 4 so icon size is consistent across screens regardless of actual column count
    val iconSizeDp = (screenWidthDp / 4f * 0.55f * iconSizePercent / 100f).toInt()

    // Per-app icon size overflow threshold: same universal formula as global settings slider
    // (min of home screen threshold and drawer threshold), converted to per-app scale
    val iconRef = screenWidthDp / 4f * 0.55f
    val gridMarkerPadding = gridCellBasis * 0.073f * 2f
    val homeOverflowThreshold = ((gridCellWidth - gridMarkerPadding) / iconRef * 100f).coerceIn(50f, 125f)
    val drawerGridSize = getGridSize(context)
    val drawerPaged = getDrawerPagedMode(context)
    val drawerHPad = if (drawerPaged) 16f else 28f
    val drawerCellWidth = (screenWidthDp - drawerHPad) / drawerGridSize
    val drawerOverflowThreshold = ((drawerCellWidth - 16f) / iconRef * 100f).coerceIn(50f, 125f)
    val universalOverflowThreshold = minOf(homeOverflowThreshold, drawerOverflowThreshold)

    // Edge scroll zone in pixels (proportional to screen width)
    val edgeScrollZonePx = with(density) { edgeScrollZone.toPx() }

    // Home screen apps
    var homeApps by remember { mutableStateOf<List<HomeScreenApp>>(emptyList()) }
    var dockApps by remember { mutableStateOf<List<DockApp>>(emptyList()) }
    var dockFolders by remember { mutableStateOf<List<DockFolder>>(emptyList()) }
    var homeFolders by remember { mutableStateOf<List<HomeFolder>>(emptyList()) }
    var allAvailableApps by remember { mutableStateOf<List<HomeAppInfo>>(emptyList()) }
    val hiddenApps = remember { com.bearinmind.launcher314.data.getHiddenApps(appContext) }
    var appCustomizations by remember { mutableStateOf(AppCustomizations()) }
    var iconCacheVersion by remember { mutableIntStateOf(0) }
    var customizingApp by remember { mutableStateOf<HomeAppInfo?>(null) }
    var customizingFolder by remember { mutableStateOf<com.bearinmind.launcher314.data.HomeFolder?>(null) }
    var customizingDockFolder by remember { mutableStateOf<com.bearinmind.launcher314.data.DockFolder?>(null) }
    var placedWidgets by remember { mutableStateOf<List<PlacedWidget>>(emptyList()) }
    // Per-widget refresh counter — bump after WidgetManager.recreateWidgetView() so
    // WidgetHostView re-fetches the new view instance from cache.
    var widgetViewRefreshKeys by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var globalWidgetPaddingPercent by remember { mutableIntStateOf(getWidgetPaddingPercent(appContext)) }
    var globalWidgetFontScalePercent by remember { mutableIntStateOf(com.bearinmind.launcher314.data.getWidgetFontScalePercent(appContext)) }
    var widgetRoundedCornersEnabled by remember { mutableStateOf(getWidgetRoundedCornersEnabled(appContext)) }
    var widgetCornerRadiusPercent by remember { mutableIntStateOf(getWidgetCornerRadiusPercent(appContext)) }
    var isLoading by remember { mutableStateOf(true) }

    // Edit mode and drag state
    var isEditMode by remember { mutableStateOf(false) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggedFromDock by remember { mutableStateOf(false) } // true = dragging from dock
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedItemPosition by remember { mutableStateOf(Offset.Zero) }

    // Grid cell positions for drop detection
    var cellPositions by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var cellSize by remember { mutableStateOf(IntSize.Zero) }

    // Dock positions for drop detection
    var dockPositions by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var dockSlotSize by remember { mutableStateOf(IntSize.Zero) }
    var hoveredDockSlot by remember { mutableStateOf<Int?>(null) }

    // Grid cell hover detection during drag (for showing empty cell indicator)
    var hoveredGridCell by remember { mutableStateOf<Int?>(null) }

    // Track if current hover target is valid (for icon red tint)
    var isHoveredCellValid by remember { mutableStateOf(true) }
    var isHoveredDockSlotValid by remember { mutableStateOf(true) }

    // Drop animation - single progress (0→1) drives position, scale, and alpha in lockstep
    val dropAnimProgress = remember { Animatable(0f) }
    var isDropAnimating by remember { mutableStateOf(false) }
    var dropStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetOffset by remember { mutableStateOf(Offset.Zero) }
    val dropScope = rememberCoroutineScope()

    // Remove-from-home animation state (consolidated to reduce register count)
    val removeState = remember { RemoveAnimState() }

    // Stored app info for overlay rendering (survives data changes during animation)
    var draggedAppInfo by remember { mutableStateOf<HomeAppInfo?>(null) }
    // When dragging a folder, store its data for the 2x2 preview overlay
    var draggedFolderData by remember { mutableStateOf<HomeFolder?>(null) }
    var draggedFolderPreviewApps by remember { mutableStateOf<List<HomeAppInfo>>(emptyList()) }
    // Track if drop target is dock (no text) or grid (text fades in)
    var dropTargetIsDock by remember { mutableStateOf(false) }
    var dropCreatesFolder by remember { mutableStateOf(false) }

    // Track which page the drag started from (for cross-page moves)
    var dragSourcePage by remember { mutableIntStateOf(0) }
    // Store original cell position at drag start (survives page changes)
    var dragOriginalCellPos by remember { mutableStateOf<Offset?>(null) }
    // Track pager position at last drag event — used to subtract pager scroll from cell deltas
    var lastDragPagerPos by remember { mutableFloatStateOf(0f) }
    // Edge scroll state - when dragging near screen edges
    var edgeScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // When a page transition cancels the cell-level gesture, this flag tells the
    // root-level handler to pick up the ongoing drag
    var dragContinuedAfterPageSwitch by remember { mutableStateOf(false) }
    // Folder drag-out state: when dragging an app out of an open folder onto the grid
    var dragFromFolderApp by remember { mutableStateOf<HomeAppInfo?>(null) }
    var dragFromFolderId by remember { mutableStateOf<String?>(null) }
    var dragFromFolderPage by remember { mutableStateOf(0) }
    // True while the cell's gesture handler is still tracking the pointer after escape
    var escapedToHomeGrid by remember { mutableStateOf(false) }
    // Grid area offset (root-relative) for geometry-based cell lookup
    var gridAreaOffset by remember { mutableStateOf(Offset.Zero) }
    // Grid area box position and size (for edge-scroll indicator positioning)
    var gridAreaBoxOffset by remember { mutableStateOf(Offset.Zero) }
    var gridAreaBoxSize by remember { mutableStateOf(IntSize.Zero) }
    // Edge-scroll hover state — true when drag is in the left/right edge zone
    var isHoveringLeftEdge by remember { mutableStateOf(false) }
    var isHoveringRightEdge by remember { mutableStateOf(false) }
    // Brief cooldown after page scroll — suppresses indicator so it flashes out
    var edgeIndicatorSuppressed by remember { mutableStateOf(false) }

    // External drag from drawer
    var externalDragActive by remember { mutableStateOf(false) }
    // State-backed copy of externalDragItem (parameter is stale inside LaunchedEffects)
    var externalDragItemState by remember { mutableStateOf<Any?>(null) }

    // Widget drag state - using WidgetDragState from WidgetMoving.kt
    var widgetDragState by remember { mutableStateOf(WidgetDragState()) }

    // Hovered widget cells - cells that will be highlighted during widget drag (drop target)
    // Uses the same highlighting as app movement
    var hoveredWidgetCells by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Widget drag validity - is the current drop target valid?
    var isWidgetDropTargetValid by remember { mutableStateOf(true) }

    // Widget original cells - cells occupied by widget before drag (should show blue, not red)
    var widgetOriginalCells by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // True when dragged widget is hovering over another widget (stack target)
    var isWidgetOverWidget by remember { mutableStateOf(false) }

    // True when user is swiping within a stacked widget pager
    var isStackSwipeActive by remember { mutableStateOf(false) }

    // Widget drop animation state
    val widgetDropAnim = remember { Animatable(0f) }
    var isWidgetDropAnimating by remember { mutableStateOf(false) }
    var widgetDropStartOffset by remember { mutableStateOf(Offset.Zero) }
    var widgetDropTargetOffset by remember { mutableStateOf(Offset.Zero) }
    var widgetDropWidgetId by remember { mutableIntStateOf(-1) }

    // Widget drag bitmap - snapshot for root-level overlay (avoids HorizontalPager clipping)
    var widgetDragBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var widgetDragScreenPos by remember { mutableStateOf(Offset.Zero) }
    var widgetDragSizePx by remember { mutableStateOf(Pair(0, 0)) }
    // Widget cross-page drag state
    var widgetDragSourcePage by remember { mutableIntStateOf(0) }
    var widgetEdgeScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var widgetDragContinuedAfterPageSwitch by remember { mutableStateOf(false) }

    // Widget resize state - for resizing widgets with preview outline
    var widgetResizeState by remember { mutableStateOf(WidgetResizeState()) }

    // Current resize dimensions - used to override widget visual size during resize
    var currentResizeDimensions by remember { mutableStateOf<ResizeDimensions?>(null) }

    // Page state (persisted via SharedPreferences)
    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    var totalPages by remember { mutableIntStateOf(prefs.getInt("launcher_total_pages", 1)) }
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    var removingLastDot by remember { mutableStateOf(false) }
    var addingLastDot by remember { mutableStateOf(false) }

    // Folder state - for creating and opening folders on home screen
    var openHomeFolder by remember { mutableStateOf<HomeFolder?>(null) }
    LaunchedEffect(openHomeFolder) { onFolderOpenChanged(openHomeFolder != null) }
    var folderCreationHoverJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var folderCreationTargetIndex by remember { mutableStateOf<Int?>(null) }
    var showFolderCreationIndicator by remember { mutableStateOf(false) }
    // Folder receive animation — which cell/dock slot is playing the "pulse" animation on drop
    var folderReceiveAnimIndex by remember { mutableStateOf<Int?>(null) }
    var folderReceiveDockSlot by remember { mutableStateOf<Int?>(null) }

    // Launcher settings menu (shown on long-press of empty area)
    var showLauncherSettingsMenu by remember { mutableStateOf(false) }
    var launcherMenuPosition by remember { mutableStateOf(Offset.Zero) }

    // Swipe detection for app drawer
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f

    // Load home screen apps, dock, and widgets (reloads when refreshTrigger changes)
    LaunchedEffect(refreshTrigger) {
        withContext(Dispatchers.IO) {
            val data = loadHomeScreenData(context)
            homeApps = data.apps
            dockApps = data.dockApps
            dockFolders = data.dockFolders
            homeFolders = data.folders
            allAvailableApps = loadAvailableApps(context)
            appCustomizations = loadAppCustomizations(context)
            placedWidgets = WidgetManager.loadPlacedWidgets(context)
            isLoading = false
        }
        // Refresh all icon settings (may have changed in Settings)
        globalIconShape = getGlobalIconShape(context)
        globalIconBgColor = getGlobalIconBgColor(context)
        globalIconBgIntensity = com.bearinmind.launcher314.data.getGlobalIconBgIntensity(context)
        iconSizePercent = getHomeIconSizePercent(context)
        iconTextSizePercent = getIconTextSizePercent(context)
        selectedFontFamily = FontManager.getSelectedFontFamily(context)
        // Refresh widget settings (may have changed in Widgets screen)
        globalWidgetPaddingPercent = getWidgetPaddingPercent(appContext)
        globalWidgetFontScalePercent = com.bearinmind.launcher314.data.getWidgetFontScalePercent(appContext)
        widgetRoundedCornersEnabled = getWidgetRoundedCornersEnabled(appContext)
        widgetCornerRadiusPercent = getWidgetCornerRadiusPercent(appContext)
        // Clear Coil memory cache so fresh icons load
        coil.Coil.imageLoader(context).memoryCache?.clear()
    }

    // Refresh settings
    LaunchedEffect(Unit) {
        gridColumns = getHomeGridSize(context)
        gridRows = getHomeGridRows(context)
        iconSizePercent = getHomeIconSizePercent(context)
        iconTextSizePercent = getIconTextSizePercent(context)
        selectedFontFamily = FontManager.getSelectedFontFamily(context)
        globalIconShape = getGlobalIconShape(context)
        globalIconBgColor = getGlobalIconBgColor(context)
    }

    // Save functions - state update is immediate, disk I/O is async to avoid jank
    fun saveAllData() {
        dropScope.launch(Dispatchers.IO) {
            saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = dockApps, folders = homeFolders, dockFolders = dockFolders))
        }
    }

    fun saveHomeApps(apps: List<HomeScreenApp>) {
        homeApps = apps
        dropScope.launch(Dispatchers.IO) {
            saveHomeScreenData(context, HomeScreenData(apps = apps, dockApps = dockApps, folders = homeFolders, dockFolders = dockFolders))
        }
    }

    fun saveDockApps(apps: List<DockApp>) {
        dockApps = apps
        dropScope.launch(Dispatchers.IO) {
            saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = apps, folders = homeFolders, dockFolders = dockFolders))
        }
    }

    fun saveHomeFolders(folders: List<HomeFolder>) {
        homeFolders = folders
        dropScope.launch(Dispatchers.IO) {
            saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = dockApps, folders = folders, dockFolders = dockFolders))
        }
    }

    fun saveDockFolders(folders: List<DockFolder>) {
        dockFolders = folders
        dropScope.launch(Dispatchers.IO) {
            saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = dockApps, folders = homeFolders, dockFolders = folders))
        }
    }

    // Build grid cells for a specific page
    val totalCells = gridColumns * gridRows
    fun buildGridCellsForPage(page: Int): List<HomeGridCell> {
        val cells = MutableList<HomeGridCell>(totalCells) { HomeGridCell.Empty }

        // Place widgets for this specific page (only primary widget per stack, sorted by stackOrder)
        val seenStacks = mutableSetOf<String>()
        placedWidgets.filter { it.page == page }.sortedBy { it.stackOrder }.filter { w ->
            val sid = w.stackId
            if (sid == null) true
            else if (seenStacks.contains(sid)) false
            else { seenStacks.add(sid); true }
        }.forEach { widget ->
            val originPos = widget.gridRow * gridColumns + widget.gridColumn
            if (originPos < totalCells) {
                cells[originPos] = HomeGridCell.Widget(widget, originPos)
                for (row in 0 until widget.spanRows) {
                    for (col in 0 until widget.spanColumns) {
                        if (row == 0 && col == 0) continue
                        val spanPos = (widget.gridRow + row) * gridColumns + (widget.gridColumn + col)
                        if (spanPos < totalCells && spanPos != originPos) {
                            cells[spanPos] = HomeGridCell.WidgetSpan(originPos)
                        }
                    }
                }
            }
        }

        // Place folders for this specific page
        homeFolders.filter { it.page == page }.forEach { folder ->
            if (folder.position < totalCells) {
                val currentCell = cells[folder.position]
                if (currentCell is HomeGridCell.Empty) {
                    val previewApps = folder.appPackageNames.filter { it.isNotEmpty() && it !in hiddenApps }.take(4).mapNotNull { pkg ->
                        allAvailableApps.find { it.packageName == pkg }
                    }
                    cells[folder.position] = HomeGridCell.Folder(folder, previewApps, folder.position)
                }
            }
        }

        // Place apps for this specific page only
        homeApps.filter { it.page == page }.forEach { homeApp ->
            if (homeApp.position < totalCells) {
                val currentCell = cells[homeApp.position]
                if (currentCell is HomeGridCell.Empty) {
                    allAvailableApps.find { it.packageName == homeApp.packageName }?.let { appInfo ->
                        val cust = appCustomizations.customizations[homeApp.packageName]
                        val customizedInfo = if (cust != null) appInfo.copy(customization = cust) else appInfo
                        cells[homeApp.position] = HomeGridCell.App(customizedInfo, homeApp.position)
                    }
                }
            }
        }
        return cells.toList()
    }
    // gridCells for the current page (used by drag/drop handlers)
    val gridCells = remember(homeApps, allAvailableApps, placedWidgets, homeFolders, totalCells, gridColumns, currentPage, appCustomizations) {
        buildGridCellsForPage(currentPage)
    }

    // Haptic feedback when hovering an app over a folder or another app during drag
    val gridDragHaptic = rememberHapticFeedback()
    LaunchedEffect(hoveredGridCell) {
        val hovered = hoveredGridCell ?: return@LaunchedEffect
        if (draggedItemIndex == null) return@LaunchedEffect
        val pageCells = buildGridCellsForPage(pagerState.targetPage)
        val cell = pageCells.getOrNull(hovered) ?: return@LaunchedEffect
        val isDraggingFolder = draggedFolderData != null
        if (!isDraggingFolder && (cell is HomeGridCell.Folder || (cell is HomeGridCell.App && hovered != draggedItemIndex))) {
            gridDragHaptic.performTextHandleMove()
        }
    }

    // Recalculate widget hover cells when the page changes during a widget drag.
    // Without this, the old hover state from the source page remains until the user moves.
    LaunchedEffect(pagerState.targetPage, widgetDragState.draggedWidget) {
        if (widgetDragState.draggedWidget == null) return@LaunchedEffect
        // Don't recalculate hover cells during drop animation (scroll-back would
        // set hoveredWidgetCells on intermediate pages, causing red tint/grey spaces)
        if (isWidgetDropAnimating) return@LaunchedEffect
        val widget = widgetDragState.draggedWidget ?: return@LaunchedEffect
        if (cellSize.width <= 0 || cellSize.height <= 0) return@LaunchedEffect
        val widgetCenter = widgetDragState.startPosition + widgetDragState.dragOffset
        val dropTarget = calculateWidgetDropTargetFromCenter(
            widgetCenter, cellPositions, cellSize,
            gridColumns, gridRows,
            widget.spanColumns, widget.spanRows
        )
        val targetCol = dropTarget?.first ?: -1
        val targetRow = dropTarget?.second ?: -1
        if (targetCol >= 0 && targetRow >= 0) {
            val targetCells = getWidgetTargetCells(widget, targetCol, targetRow, gridColumns, gridRows)
            hoveredWidgetCells = targetCells
            val draggedSid = widget.stackId
            val otherWidgets = placedWidgets.filter { it.appWidgetId != widget.appWidgetId && it.page == pagerState.targetPage && (draggedSid == null || it.stackId != draggedSid) }
            val hoveringOverWidget = otherWidgets.any { other ->
                val otherCells = mutableSetOf<Int>()
                for (r in other.startRow until other.startRow + other.rowSpan) {
                    for (c in other.startColumn until other.startColumn + other.columnSpan) {
                        otherCells.add(r * gridColumns + c)
                    }
                }
                targetCells.any { otherCells.contains(it) }
            }
            val atOriginal = targetCol == widget.startColumn && targetRow == widget.startRow && pagerState.targetPage == widget.page
            isWidgetOverWidget = hoveringOverWidget && !atOriginal
            isWidgetDropTargetValid = if (hoveringOverWidget && !atOriginal) true
                else canPlaceWidgetAt(widget, targetCol, targetRow, gridColumns, gridRows, buildGridCellsForPage(pagerState.targetPage))
        } else {
            hoveredWidgetCells = emptySet()
            isWidgetDropTargetValid = true
            isWidgetOverWidget = false
        }
    }

    // Insert new app into folder's package list at the first empty slot,
    // or append at the end if no gaps exist.
    fun addAppToFolder(existingNames: List<String>, newPkg: String): List<String> {
        val firstEmpty = existingNames.indexOfFirst { it.isEmpty() }
        return if (firstEmpty >= 0) {
            existingNames.toMutableList().apply { set(firstEmpty, newPkg) }
        } else {
            existingNames + newPkg
        }
    }

    // Handle drop - move app/folder to new position, create folders, or add to folders
    // fromPage/toPage track which pages the source and destination are on
    fun handleDrop(fromIndex: Int, toIndex: Int, fromPage: Int = currentPage, toPage: Int = currentPage) {
        android.util.Log.d("FolderDrop", "handleDrop called: from=$fromIndex to=$toIndex fromPage=$fromPage toPage=$toPage currentPage=$currentPage")
        if (fromIndex == toIndex && fromPage == toPage) {
            android.util.Log.d("FolderDrop", "handleDrop: EARLY RETURN - same index and page")
            return
        }

        // Use fresh grid cells to avoid stale data from pointerInput capture
        val freshFromCells = buildGridCellsForPage(fromPage)
        val freshToCells = buildGridCellsForPage(toPage)
        val fromCell = freshFromCells.getOrNull(fromIndex)
        val toCell = freshToCells.getOrNull(toIndex)
        android.util.Log.d("FolderDrop", "handleDrop: fromCell=${fromCell?.javaClass?.simpleName} toCell=${toCell?.javaClass?.simpleName} fromPage==currentPage=${fromPage == currentPage} gridCells.size=${gridCells.size}")

        // For cross-page drops, check if the source app exists in homeApps
        val sourceApp = if (fromCell is HomeGridCell.App) {
            fromCell
        } else {
            // Look up the app from homeApps directly (cross-page drag)
            val homeApp = homeApps.find { it.position == fromIndex && it.page == fromPage }
            homeApp?.let { ha ->
                allAvailableApps.find { it.packageName == ha.packageName }?.let { appInfo ->
                    HomeGridCell.App(appInfo, fromIndex)
                }
            }
        }

        // Check if dragging a folder
        val sourceFolder = if (fromCell is HomeGridCell.Folder) fromCell else null
        android.util.Log.d("FolderDrop", "handleDrop: sourceFolder=${sourceFolder != null} sourceApp=${sourceApp != null}")

        // Case 1: Dragging a folder to an empty cell → move the folder
        if (sourceFolder != null && (toCell is HomeGridCell.Empty || toCell == null)) {
            android.util.Log.d("FolderDrop", "handleDrop: Case 1 - folder to empty cell, folderId=${sourceFolder.folder.id}")
            val updatedFolders = homeFolders.map { folder ->
                if (folder.id == sourceFolder.folder.id) {
                    folder.copy(position = toIndex, page = toPage)
                } else folder
            }
            saveHomeFolders(updatedFolders)
            android.util.Log.d("FolderDrop", "handleDrop: Case 1 - saved! New position=$toIndex page=$toPage")
            return
        }

        // Case 2: App dropped on another App → create a folder
        if (sourceApp != null && toCell is HomeGridCell.App) {
            val targetAppPkg = toCell.appInfo.packageName
            val sourceAppPkg = sourceApp.appInfo.packageName

            // Create new folder at the target position with both apps
            val newFolder = HomeFolder(
                name = "Folder",
                position = toIndex,
                page = toPage,
                appPackageNames = listOf(targetAppPkg, sourceAppPkg)
            )

            // Remove both apps from homeApps
            val updatedApps = homeApps.toMutableList()
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage }
            updatedApps.removeAll { it.position == toIndex && it.page == toPage }

            homeApps = updatedApps
            val updatedFolders = homeFolders + newFolder
            homeFolders = updatedFolders
            dropScope.launch(Dispatchers.IO) {
                saveHomeScreenData(context, HomeScreenData(apps = updatedApps, dockApps = dockApps, folders = updatedFolders, dockFolders = dockFolders))
            }
            return
        }

        // Case 3: App dropped on a Folder → add app to folder
        if (sourceApp != null && toCell is HomeGridCell.Folder) {
            val updatedApps = homeApps.toMutableList()
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage }
            homeApps = updatedApps

            val updatedFolders = homeFolders.map { folder ->
                if (folder.id == toCell.folder.id) {
                    folder.copy(appPackageNames = addAppToFolder(folder.appPackageNames, sourceApp.appInfo.packageName))
                } else folder
            }
            homeFolders = updatedFolders
            dropScope.launch(Dispatchers.IO) {
                saveHomeScreenData(context, HomeScreenData(apps = updatedApps, dockApps = dockApps, folders = updatedFolders, dockFolders = dockFolders))
            }
            return
        }

        // Case 4: App to empty cell → standard move
        if (sourceApp != null && (toCell is HomeGridCell.Empty || toCell == null)) {
            val updatedApps = homeApps.toMutableList()

            // Safety: verify target position isn't already occupied in homeApps
            val targetOccupied = updatedApps.any { it.position == toIndex && it.page == toPage }
            if (targetOccupied) {
                android.util.Log.w("FolderDrop", "handleDrop: target position $toIndex on page $toPage already occupied, aborting move")
                return
            }

            // Remove app from old position and page
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage }

            // Move to new cell on target page
            updatedApps.add(HomeScreenApp(sourceApp.appInfo.packageName, toIndex, toPage))

            saveHomeApps(updatedApps)
        }
    }

    // Find cell index from screen position
    fun findCellIndex(position: Offset): Int? {
        cellPositions.forEach { (index, cellPos) ->
            if (position.x >= cellPos.x && position.x < cellPos.x + cellSize.width &&
                position.y >= cellPos.y && position.y < cellPos.y + cellSize.height
            ) {
                return index
            }
        }
        return null
    }

    // Find dock slot index from screen position
    fun findDockSlotIndex(position: Offset): Int? {
        dockPositions.forEach { (index, slotPos) ->
            if (position.x >= slotPos.x && position.x < slotPos.x + dockSlotSize.width &&
                position.y >= slotPos.y && position.y < slotPos.y + dockSlotSize.height
            ) {
                return index
            }
        }
        return null
    }

    // Handle drop from grid to dock
    fun handleDropToDock(fromGridIndex: Int, toDockSlot: Int, fromPage: Int = currentPage) {
        val existingDockApp = dockApps.find { it.position == toDockSlot }
        val existingDockFolder = dockFolders.find { it.position == toDockSlot }

        // Find the source app from homeApps (page-aware)
        val sourceHomeApp = homeApps.find { it.position == fromGridIndex && it.page == fromPage }
        val sourceAppInfo = sourceHomeApp?.let { ha ->
            allAvailableApps.find { it.packageName == ha.packageName }
        }
        if (sourceAppInfo == null) return

        val updatedGridApps = homeApps.filter { !(it.position == fromGridIndex && it.page == fromPage) }

        if (existingDockApp == null && existingDockFolder == null) {
            // Empty slot → place app
            val updatedDockApps = dockApps + DockApp(sourceAppInfo.packageName, toDockSlot)
            homeApps = updatedGridApps
            dockApps = updatedDockApps
            dropScope.launch(Dispatchers.IO) {
                saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = updatedDockApps, folders = homeFolders, dockFolders = dockFolders))
            }
        } else if (existingDockFolder != null) {
            // Dock folder → add app to folder
            val updatedDockFolders = dockFolders.map { f ->
                if (f.id == existingDockFolder.id) f.copy(appPackageNames = addAppToFolder(f.appPackageNames, sourceAppInfo.packageName))
                else f
            }
            homeApps = updatedGridApps
            dockFolders = updatedDockFolders
            dropScope.launch(Dispatchers.IO) {
                saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = dockApps, folders = homeFolders, dockFolders = updatedDockFolders))
            }
        } else if (existingDockApp != null) {
            // Dock app → create new dock folder
            val updatedDockApps = dockApps.filter { it.position != toDockSlot }
            val newFolder = DockFolder(
                name = "Folder",
                position = toDockSlot,
                appPackageNames = listOf(existingDockApp.packageName, sourceAppInfo.packageName)
            )
            homeApps = updatedGridApps
            dockApps = updatedDockApps
            dockFolders = dockFolders + newFolder
            dropScope.launch(Dispatchers.IO) {
                saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = updatedDockApps, folders = homeFolders, dockFolders = dockFolders + newFolder))
            }
        }
    }

    // Handle drop from dock to grid (only empty grid cells allowed)
    fun handleDropFromDockToGrid(fromDockSlot: Int, toGridIndex: Int) {
        val dockApp = dockApps.find { it.position == fromDockSlot }
        val toCell = gridCells.getOrNull(toGridIndex)

        if (dockApp != null) {
            val appInfo = allAvailableApps.find { it.packageName == dockApp.packageName }
            if (appInfo != null) {
                // Drop on empty cell → standard move
                if (toCell is HomeGridCell.Empty) {
                    val updatedDockApps = dockApps.filter { it.position != fromDockSlot }
                    val updatedGridApps = homeApps + HomeScreenApp(dockApp.packageName, toGridIndex, currentPage)

                    homeApps = updatedGridApps
                    dockApps = updatedDockApps
                    dropScope.launch(Dispatchers.IO) {
                        saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = updatedDockApps, folders = homeFolders, dockFolders = dockFolders))
                    }
                }
                // Drop on app → create folder
                else if (toCell is HomeGridCell.App) {
                    val updatedDockApps = dockApps.filter { it.position != fromDockSlot }
                    val updatedGridApps = homeApps.toMutableList()
                    updatedGridApps.removeAll { it.position == toGridIndex && it.page == currentPage }

                    val newFolder = HomeFolder(
                        name = "Folder",
                        position = toGridIndex,
                        page = currentPage,
                        appPackageNames = listOf(toCell.appInfo.packageName, dockApp.packageName)
                    )
                    val updatedFolders = homeFolders + newFolder

                    homeApps = updatedGridApps
                    dockApps = updatedDockApps
                    homeFolders = updatedFolders
                    dropScope.launch(Dispatchers.IO) {
                        saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = updatedDockApps, folders = updatedFolders, dockFolders = dockFolders))
                    }
                }
                // Drop on folder → add to folder
                else if (toCell is HomeGridCell.Folder) {
                    val updatedDockApps = dockApps.filter { it.position != fromDockSlot }
                    val updatedFolders = homeFolders.map { folder ->
                        if (folder.id == toCell.folder.id) {
                            folder.copy(appPackageNames = addAppToFolder(folder.appPackageNames, dockApp.packageName))
                        } else folder
                    }

                    dockApps = updatedDockApps
                    homeFolders = updatedFolders
                    dropScope.launch(Dispatchers.IO) {
                        saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = updatedDockApps, folders = updatedFolders, dockFolders = dockFolders))
                    }
                }
            }
        }
    }

    // Shared drop logic — called from cell-level onDragEnd and root-level drag continuation
    fun performDrop() {
        dragContinuedAfterPageSwitch = false
        isHoveringLeftEdge = false
        isHoveringRightEdge = false
        edgeIndicatorSuppressed = false
        val intendedPage = pagerState.targetPage
        edgeScrollJob?.cancel()
        edgeScrollJob = null
        dropScope.launch { pagerState.scrollToPage(intendedPage) }

        // Handle folder-sourced drag drop (with animation)
        if (dragFromFolderApp != null) {
            val centerPos = draggedItemPosition + Offset(cellSize.width / 2f, cellSize.height / 2f)
            val targetGridIndex = findCellIndex(centerPos) ?: run {
                // Fallback: compute from grid geometry
                if (cellSize.width > 0 && cellSize.height > 0) {
                    val relX = centerPos.x - gridAreaOffset.x
                    val relY = centerPos.y - gridAreaOffset.y
                    if (relX >= 0 && relY >= 0) {
                        val col = (relX / cellSize.width).toInt().coerceIn(0, gridColumns - 1)
                        val row = (relY / cellSize.height).toInt().coerceIn(0, gridRows - 1)
                        row * gridColumns + col
                    } else null
                } else null
            }

            val targetCells = buildGridCellsForPage(intendedPage)
            val folderApp = dragFromFolderApp!!

            // Helper: find first empty cell on this page as fallback
            fun findFirstEmptyCell(): Int? = targetCells.indexOfFirst { it is HomeGridCell.Empty }.takeIf { it >= 0 }

            val effectiveIndex = targetGridIndex ?: findFirstEmptyCell()
            val originalPos = dragOriginalCellPos ?: Offset.Zero

            if (effectiveIndex != null && !isDropAnimating) {
                val targetCell = targetCells.getOrNull(effectiveIndex)
                val willCreateFolderFromEscape = targetCell is HomeGridCell.App || targetCell is HomeGridCell.Folder

                // Calculate target position for animation
                val targetPos = cellPositions[effectiveIndex] ?: run {
                    val tRow = effectiveIndex / gridColumns
                    val tCol = effectiveIndex % gridColumns
                    Offset(gridAreaOffset.x + tCol * cellSize.width, gridAreaOffset.y + tRow * cellSize.height)
                }
                val targetOffset = Offset(targetPos.x - originalPos.x, targetPos.y - originalPos.y)

                // Build the drop action
                val dropAction: () -> Unit = {
                    when (targetCell) {
                        is HomeGridCell.Empty -> {
                            homeApps = homeApps + HomeScreenApp(folderApp.packageName, effectiveIndex, intendedPage)
                            saveAllData()
                        }
                        is HomeGridCell.App -> {
                            val targetAppPkg = targetCell.appInfo?.packageName
                            if (targetAppPkg != null) {
                                homeApps = homeApps.filter { !(it.position == effectiveIndex && it.page == intendedPage) }
                                val newFolder = HomeFolder(
                                    name = "Folder",
                                    position = effectiveIndex,
                                    page = intendedPage,
                                    appPackageNames = listOf(targetAppPkg, folderApp.packageName)
                                )
                                saveHomeFolders(homeFolders + newFolder)
                            }
                        }
                        is HomeGridCell.Folder -> {
                            val updatedFolders = homeFolders.map { f ->
                                if (f.id == targetCell.folder.id) {
                                    f.copy(appPackageNames = addAppToFolder(f.appPackageNames, folderApp.packageName))
                                } else f
                            }
                            saveHomeFolders(updatedFolders)
                        }
                        else -> {
                            val emptyIdx = findFirstEmptyCell()
                            if (emptyIdx != null) {
                                homeApps = homeApps + HomeScreenApp(folderApp.packageName, emptyIdx, intendedPage)
                                saveAllData()
                            }
                        }
                    }
                }

                // Set up drop animation (same pattern as grid/dock drops)
                dropTargetIsDock = false
                dropCreatesFolder = willCreateFolderFromEscape
                if (willCreateFolderFromEscape) {
                    folderReceiveAnimIndex = effectiveIndex
                }
                dropStartOffset = dragOffset
                dropTargetOffset = targetOffset
                isDropAnimating = true
                isEditMode = false
                hoveredGridCell = null
                hoveredDockSlot = null
                showFolderCreationIndicator = false

                dropScope.launch {
                    dropAnimProgress.snapTo(0f)
                    dropAnimProgress.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
                    dropAction()
                    folderReceiveAnimIndex = null
                    if (dropCreatesFolder) dropCreatesFolder = false
                    dragFromFolderApp = null
                    dragFromFolderId = null
                    dragFromFolderPage = 0
                    dragOffset = Offset.Zero
                    draggedAppInfo = null
                    draggedFolderData = null
                    draggedFolderPreviewApps = emptyList()
                    dragOriginalCellPos = null
                    isDropAnimating = false
                    isEditMode = false
                }
            } else {
                // No valid target - just reset
                dragFromFolderApp = null
                dragFromFolderId = null
                dragFromFolderPage = 0
                dragOffset = Offset.Zero
                draggedAppInfo = null
                draggedFolderData = null
                draggedFolderPreviewApps = emptyList()
                dragOriginalCellPos = null
                isEditMode = false
                hoveredGridCell = null
                hoveredDockSlot = null
                isHoveredCellValid = true
                showFolderCreationIndicator = false
                // Scroll back to source page if page changed
                if (dragSourcePage != intendedPage) {
                    dropScope.launch { pagerState.animateScrollToPage(dragSourcePage) }
                }
            }
            return
        }

        val centerPos = draggedItemPosition + Offset(cellSize.width / 2f, cellSize.height / 2f)
        val targetDockSlot = findDockSlotIndex(centerPos)
        var targetGridIndex = findCellIndex(centerPos)

        // Fallback: compute target cell from grid geometry
        if (targetGridIndex == null && targetDockSlot == null && cellSize.width > 0 && cellSize.height > 0) {
            val relX = centerPos.x - gridAreaOffset.x
            val relY = centerPos.y - gridAreaOffset.y
            if (relX >= 0 && relY >= 0) {
                val col = (relX / cellSize.width).toInt().coerceIn(0, gridColumns - 1)
                val row = (relY / cellSize.height).toInt().coerceIn(0, gridRows - 1)
                targetGridIndex = row * gridColumns + col
            }
        }

        val currentDraggedIndex = draggedItemIndex
        val originalPos = dragOriginalCellPos ?: (if (currentDraggedIndex != null) cellPositions[currentDraggedIndex] else null)

        // Check if this drop targets a folder (create new or add to existing)
        // Both cases use the same shrink+fade overlay animation
        val isDraggingAFolder = draggedFolderData != null
        val willCreateFolder = if (currentDraggedIndex != null && !isDraggingAFolder) {
            if (targetGridIndex != null && (targetGridIndex != currentDraggedIndex || dragSourcePage != intendedPage)) {
                val dropCells = buildGridCellsForPage(intendedPage)
                val targetCell = dropCells.getOrNull(targetGridIndex)
                targetCell is HomeGridCell.App || targetCell is HomeGridCell.Folder
            } else if (targetDockSlot != null) {
                val existDockApp = dockApps.find { it.position == targetDockSlot }
                val existDockFolder = dockFolders.find { it.position == targetDockSlot }
                existDockApp != null || existDockFolder != null
            } else false
        } else false

        hoveredDockSlot = null
        isHoveredCellValid = true
        isHoveredDockSlotValid = true
        folderCreationHoverJob?.cancel()
        folderCreationHoverJob = null

        // Keep folder preview visible during drop animation for smooth folder creation
        if (!willCreateFolder) {
            hoveredGridCell = null
            showFolderCreationIndicator = false
        }

        android.util.Log.d("FolderDrop", "performDrop: draggedIndex=$currentDraggedIndex targetGrid=$targetGridIndex targetDock=$targetDockSlot originalPos=$originalPos isDropAnimating=$isDropAnimating draggedFolderData=${draggedFolderData != null}")

        if (currentDraggedIndex != null && originalPos != null && !isDropAnimating) {
            val targetOffset: Offset
            var dropAction: (() -> Unit)? = null

            if (targetDockSlot != null) {
                val dockPos = dockPositions[targetDockSlot]
                val existingDockApp = dockApps.find { it.position == targetDockSlot }
                val existingDockFolder = dockFolders.find { it.position == targetDockSlot }
                val isSlotEmpty = existingDockApp == null && existingDockFolder == null
                // Valid: empty slot for any drag, or occupied slot for single app drag (folder add / folder create)
                val isDraggingApp = draggedFolderData == null
                val isValid = dockPos != null && (isSlotEmpty || (isDraggingApp && (existingDockFolder != null || existingDockApp != null)))
                targetOffset = if (isValid && dockPos != null) {
                    Offset(dockPos.x - originalPos.x, dockPos.y - originalPos.y)
                } else Offset.Zero
                if (isValid) {
                    val srcPage = dragSourcePage
                    if (draggedFolderData != null) {
                        // Grid folder → dock (empty slot only)
                        val folderData = draggedFolderData!!
                        dropAction = {
                            val newDockFolder = DockFolder(
                                id = folderData.id,
                                name = folderData.name,
                                position = targetDockSlot,
                                appPackageNames = folderData.appPackageNames
                            )
                            homeFolders = homeFolders.filter { it.id != folderData.id }
                            dockFolders = dockFolders + newDockFolder
                            saveAllData()
                        }
                    } else {
                        // handleDropToDock now handles empty, folder, and app-on-app cases
                        dropAction = { handleDropToDock(currentDraggedIndex, targetDockSlot, srcPage) }
                    }
                }
            } else if (targetGridIndex != null && (targetGridIndex != currentDraggedIndex || dragSourcePage != intendedPage)) {
                val dropPageCells = buildGridCellsForPage(intendedPage)
                val targetCell = dropPageCells.getOrNull(targetGridIndex)
                // Valid targets: empty cells, apps (creates folder), existing folders (adds to folder)
                // Folders can only be dropped on empty cells
                val isDroppingFolder = draggedFolderData != null
                val isValid = targetCell is HomeGridCell.Empty ||
                    (!isDroppingFolder && (targetCell is HomeGridCell.App ||
                        targetCell is HomeGridCell.Folder))
                android.util.Log.d("FolderDrop", "performDrop grid: targetCell=${targetCell?.javaClass?.simpleName} isValid=$isValid targetGridIndex=$targetGridIndex currentDraggedIndex=$currentDraggedIndex")
                val targetPos = if (isValid) {
                    cellPositions[targetGridIndex] ?: run {
                        val tRow = targetGridIndex / gridColumns
                        val tCol = targetGridIndex % gridColumns
                        Offset(
                            gridAreaOffset.x + tCol * cellSize.width,
                            gridAreaOffset.y + tRow * cellSize.height
                        )
                    }
                } else null
                targetOffset = if (targetPos != null) {
                    Offset(targetPos.x - originalPos.x, targetPos.y - originalPos.y)
                } else Offset.Zero
                if (isValid && targetPos != null) {
                    val srcPage = dragSourcePage
                    val dstPage = intendedPage
                    dropAction = { handleDrop(currentDraggedIndex, targetGridIndex, srcPage, dstPage) }
                }
            } else {
                targetOffset = Offset.Zero
            }

            dropTargetIsDock = targetDockSlot != null && dropAction != null
            dropCreatesFolder = willCreateFolder && dropAction != null
            // Start folder receive pulse animation on the target cell/dock slot
            if (willCreateFolder && dropAction != null) {
                if (targetGridIndex != null) folderReceiveAnimIndex = targetGridIndex
                if (targetDockSlot != null) folderReceiveDockSlot = targetDockSlot
            }

            if (dropAction == null && dragOffset.getDistance() < 1f) {
                // No actual drag movement — skip animation and clean up immediately
                hoveredGridCell = null
                showFolderCreationIndicator = false
                draggedItemIndex = null
                dragOffset = Offset.Zero
                draggedAppInfo = null
                draggedFolderData = null
                draggedFolderPreviewApps = emptyList()
                dragOriginalCellPos = null
                isEditMode = false
                // Scroll back to source page if page changed
                if (dragSourcePage != intendedPage) {
                    dropScope.launch { pagerState.animateScrollToPage(dragSourcePage) }
                }
            } else {
                dropStartOffset = dragOffset
                dropTargetOffset = targetOffset
                isDropAnimating = true
                isEditMode = false // Hide grid markers immediately on release
                val capturedAction = dropAction
                // For invalid drops on a different page, scroll back to the source page
                val scrollBackPage = if (capturedAction == null && dragSourcePage != intendedPage) dragSourcePage else null
                dropScope.launch {
                    // For invalid cross-page drops: scroll back and animate return simultaneously
                    coroutineScope {
                        if (scrollBackPage != null) {
                            launch {
                                pagerState.animateScrollToPage(
                                    scrollBackPage,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                        launch {
                            dropAnimProgress.snapTo(0f)
                            dropAnimProgress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                        }
                    }
                    capturedAction?.invoke()
                    // Clean up hover and folder preview state after animation
                    hoveredGridCell = null
                    showFolderCreationIndicator = false
                    folderReceiveAnimIndex = null
                    folderReceiveDockSlot = null
                    if (dropCreatesFolder) {
                        dropCreatesFolder = false
                    }
                    draggedItemIndex = null
                    dragOffset = Offset.Zero
                    draggedAppInfo = null
                    draggedFolderData = null
                    draggedFolderPreviewApps = emptyList()
                    dragOriginalCellPos = null
                    isDropAnimating = false
                    isEditMode = false
                }
            }
        } else {
            hoveredGridCell = null
            showFolderCreationIndicator = false
            draggedItemIndex = null
            dragOffset = Offset.Zero
            draggedAppInfo = null
            draggedFolderData = null
            draggedFolderPreviewApps = emptyList()
            dragOriginalCellPos = null
            isEditMode = false
            // Scroll back to source page if page changed
            if (dragSourcePage != intendedPage) {
                dropScope.launch { pagerState.animateScrollToPage(dragSourcePage) }
            }
        }
    }

    // Shared widget drop logic — called from widget gesture handler and root-level continuation
    fun performWidgetDrop() {
        widgetDragContinuedAfterPageSwitch = false
        isHoveringLeftEdge = false
        isHoveringRightEdge = false
        edgeIndicatorSuppressed = false
        widgetEdgeScrollJob?.cancel()
        widgetEdgeScrollJob = null
        val widget = widgetDragState.draggedWidget ?: run {
            widgetDragState = WidgetDragState()
            widgetDragBitmap = null
            isWidgetOverWidget = false
            return
        }
        val dropPage = pagerState.targetPage

        val widgetCenter = widgetDragState.startPosition + widgetDragState.dragOffset
        val finalTarget = calculateWidgetDropTargetFromCenter(
            widgetCenter, cellPositions, cellSize,
            gridColumns, gridRows,
            widget.spanColumns, widget.spanRows
        )
        val finalCol = finalTarget?.first
        val finalRow = finalTarget?.second
        // Check if dropping on another widget (for stacking) — only if actively hovering over one
        // and NOT dropping back at the widget's original position
        val isBackAtOriginalPos = finalCol == widget.startColumn && finalRow == widget.startRow && dropPage == widget.page
        val droppingOnWidget = if (isWidgetOverWidget && finalTarget != null && !isBackAtOriginalPos) {
            val targetCells = getWidgetTargetCells(widget, finalCol!!, finalRow!!, gridColumns, gridRows)
            val dropSid = widget.stackId
            val otherWidgets = placedWidgets.filter { it.appWidgetId != widget.appWidgetId && it.page == dropPage && (dropSid == null || it.stackId != dropSid) }
            otherWidgets.find { other ->
                val otherCells = mutableSetOf<Int>()
                for (r in other.startRow until other.startRow + other.rowSpan) {
                    for (c in other.startColumn until other.startColumn + other.columnSpan) {
                        otherCells.add(r * gridColumns + c)
                    }
                }
                targetCells.any { otherCells.contains(it) }
            }
        } else null

        val finalValid = if (droppingOnWidget != null) true
            else if (finalTarget != null) {
                canPlaceWidgetAt(widget, finalCol!!, finalRow!!, gridColumns, gridRows, buildGridCellsForPage(dropPage))
            } else false

        val currentDragOffset = widgetDragState.dragOffset

        hoveredWidgetCells = emptySet()
        isWidgetDropTargetValid = true
        isWidgetOverWidget = false

        val isCrossPage = widgetDragSourcePage != dropPage

        if (!finalValid && isCrossPage) {
            // INVALID CROSS-PAGE DROP: animate overlay back to origin and scroll
            // back to source page simultaneously, both with same 300ms duration.
            dropScope.launch {
                widgetDropStartOffset = currentDragOffset
                widgetDropTargetOffset = Offset.Zero
                widgetDropWidgetId = widget.appWidgetId
                widgetDropAnim.snapTo(0f)
                isWidgetDropAnimating = true
                coroutineScope {
                    launch {
                        pagerState.animateScrollToPage(
                            widgetDragSourcePage,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        widgetDropAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                    }
                }
                // Clear drag state first — composable becomes visible underneath overlay
                widgetDragState = WidgetDragState()
                widgetOriginalCells = emptySet()
                // Wait one frame for composable's AndroidView to render
                delay(32)
                // Now hide overlay
                isWidgetDropAnimating = false
                widgetDropWidgetId = -1
                widgetDragBitmap = null
            }
            return
        }

        // Valid drop OR invalid same-page drop: snap pager and animate overlay
        dropScope.launch { pagerState.scrollToPage(dropPage) }

        // Calculate overlay-based target: animate the bitmap overlay from current
        // screen position to the target cell position (or stacking target's origin)
        val overlayTargetOffset = if (finalValid && finalCol != null && finalRow != null) {
            val animTargetIndex = if (droppingOnWidget != null) {
                // Animate to the target widget's origin cell
                droppingOnWidget.gridRow * gridColumns + droppingOnWidget.gridColumn
            } else {
                finalRow * gridColumns + finalCol
            }
            val targetCellPos = cellPositions[animTargetIndex]
            if (targetCellPos != null) {
                Offset(
                    targetCellPos.x - widgetDragScreenPos.x,
                    targetCellPos.y - widgetDragScreenPos.y
                )
            } else currentDragOffset
        } else {
            // Invalid same-page drop: animate back to original position
            Offset.Zero
        }

        dropScope.launch {
            widgetDropStartOffset = currentDragOffset
            widgetDropTargetOffset = overlayTargetOffset
            widgetDropWidgetId = widget.appWidgetId
            widgetDropAnim.snapTo(0f)
            isWidgetDropAnimating = true
            widgetDropAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            if (finalValid && finalCol != null && finalRow != null) {
                if (droppingOnWidget != null) {
                    placedWidgets = WidgetManager.stackWidgets(context, widget.appWidgetId, droppingOnWidget.appWidgetId)
                } else {
                    placedWidgets = handleWidgetMove(context, widget, finalCol, finalRow, dropPage)
                }
            }
            // Clear drag state first — composable becomes visible underneath overlay
            widgetDragState = WidgetDragState()
            widgetOriginalCells = emptySet()
            // Wait one frame for composable's AndroidView to render
            delay(32)
            // Now hide overlay
            isWidgetDropAnimating = false
            widgetDropWidgetId = -1
            widgetDragBitmap = null
        }
    }

    // Set up home grid drag state when escaping a folder drag.
    // Must be defined at this scope so it accesses the OUTER dragOffset (not the folder's local one).
    fun setupFolderEscapeDrag(
        app: HomeAppInfo,
        folderId: String,
        folderPage: Int,
        originPos: Offset?,
        escapeDragOffset: Offset
    ) {
        dragFromFolderApp = app
        dragFromFolderId = folderId
        dragFromFolderPage = folderPage
        draggedAppInfo = app
        dragOriginalCellPos = originPos
        dragOffset = escapeDragOffset
        draggedItemPosition = (originPos ?: Offset.Zero) + escapeDragOffset
        dragContinuedAfterPageSwitch = true
        isEditMode = true
    }

    // Update outer drag position with a delta — called from folder cell's gesture handler
    // after escape, since the cell's handler continues to own the pointer.
    fun updateEscapedDrag(delta: Offset) {
        dragOffset += delta
        draggedItemPosition = (dragOriginalCellPos ?: Offset.Zero) + dragOffset
        val centerPos = draggedItemPosition + Offset(cellSize.width / 2f, cellSize.height / 2f)
        val targetDockSlot = findDockSlotIndex(centerPos)
        hoveredDockSlot = targetDockSlot
        hoveredGridCell = if (targetDockSlot == null) findCellIndex(centerPos) else null
        if (targetDockSlot != null) {
            val draggingFolder = draggedFolderData != null
            val existDockApp = dockApps.find { it.position == targetDockSlot }
            val existDockFolder = dockFolders.find { it.position == targetDockSlot }
            val isSlotEmpty = existDockApp == null && existDockFolder == null
            isHoveredDockSlotValid = isSlotEmpty ||
                (!draggingFolder && (existDockFolder != null || existDockApp != null))
            isHoveredCellValid = true
            showFolderCreationIndicator = !draggingFolder && existDockApp != null && existDockFolder == null
        } else if (hoveredGridCell != null) {
            val pageCells = buildGridCellsForPage(pagerState.targetPage)
            val targetCell = pageCells.getOrNull(hoveredGridCell!!)
            val draggingFolder = draggedFolderData != null
            isHoveredCellValid = targetCell is HomeGridCell.Empty ||
                (!draggingFolder && (targetCell is HomeGridCell.App ||
                    targetCell is HomeGridCell.Folder))
            showFolderCreationIndicator = !draggingFolder && targetCell is HomeGridCell.App
            isHoveredDockSlotValid = true
        } else {
            isHoveredCellValid = true
            isHoveredDockSlotValid = true
            showFolderCreationIndicator = false
        }
    }

    // Track if widget is being dragged for gesture conflict prevention
    val isWidgetBeingDragged = widgetDragState.draggedWidget != null

    // ========== EXTERNAL DRAG FROM DRAWER ==========
    // Uses the folder-escape pattern: the drawer's gesture handler stays alive
    // (drawer is off-screen, not removed) and forwards position + drop via state.
    // No reliance on pointerInput root handler for event capture.

    var lastExternalDropSignal by remember { mutableIntStateOf(0) }

    // Setup: Convert drawer types to home types and initialize drag state
    LaunchedEffect(externalDragItem) {
        if (externalDragItem == null || externalDragActive) return@LaunchedEffect
        // Wait for cell size to be available (grid may need a frame to measure)
        var waitFrames = 0
        while ((cellSize.width == 0 || cellSize.height == 0) && waitFrames < 60) {
            kotlinx.coroutines.delay(16)
            waitFrames++
        }
        if (cellSize.width == 0 || cellSize.height == 0) return@LaunchedEffect

        when (externalDragItem) {
            is com.bearinmind.launcher314.data.AppInfo -> {
                draggedAppInfo = HomeAppInfo(
                    name = externalDragItem.name,
                    packageName = externalDragItem.packageName,
                    iconPath = externalDragItem.iconPath
                )
                draggedFolderData = null
                draggedFolderPreviewApps = emptyList()
            }
            is com.bearinmind.launcher314.data.AppFolder -> {
                draggedFolderData = HomeFolder(
                    name = externalDragItem.name,
                    position = 0,
                    page = 0,
                    appPackageNames = externalDragItem.appPackageNames
                )
                draggedFolderPreviewApps = externalDragItem.appPackageNames
                    .take(4)
                    .mapNotNull { pkg -> allAvailableApps.find { it.packageName == pkg } }
                draggedAppInfo = null
            }
        }

        val fingerPos = externalDragInitialPos
        dragOriginalCellPos = Offset(
            fingerPos.x - cellSize.width / 2f,
            fingerPos.y - cellSize.height / 2f
        )
        dragOffset = Offset.Zero
        draggedItemPosition = dragOriginalCellPos!!
        draggedItemIndex = null
        draggedFromDock = false
        externalDragItemState = externalDragItem
        externalDragActive = true
        isEditMode = true
    }

    // Position tracking: drawer's gesture handler forwards finger position via state
    // (like updateEscapedDrag in the folder escape pattern)
    LaunchedEffect(externalDragFingerPos) {
        if (!externalDragActive) return@LaunchedEffect
        val fingerPos = externalDragFingerPos
        draggedItemPosition = Offset(
            fingerPos.x - cellSize.width / 2f,
            fingerPos.y - cellSize.height / 2f
        )
        dragOffset = draggedItemPosition - (dragOriginalCellPos ?: Offset.Zero)

        // Hover detection
        val targetDockSlot = findDockSlotIndex(fingerPos)
        hoveredDockSlot = targetDockSlot
        hoveredGridCell = if (targetDockSlot == null) findCellIndex(fingerPos) else null

        val draggingFolder = externalDragItemState is com.bearinmind.launcher314.data.AppFolder
        if (targetDockSlot != null) {
            val existingDockApp = dockApps.find { it.position == targetDockSlot }
            val existingDockFolder = dockFolders.find { it.position == targetDockSlot }
            val isSlotEmpty = existingDockApp == null && existingDockFolder == null
            // Allow drop on empty slot, or on existing dock folder/app if dragging a single app
            isHoveredDockSlotValid = isSlotEmpty ||
                (!draggingFolder && (existingDockFolder != null || existingDockApp != null))
            isHoveredCellValid = true
            showFolderCreationIndicator = !draggingFolder && existingDockApp != null && existingDockFolder == null
        } else if (hoveredGridCell != null) {
            val pageCells = buildGridCellsForPage(pagerState.targetPage)
            val targetCell = pageCells.getOrNull(hoveredGridCell!!)
            isHoveredCellValid = targetCell is HomeGridCell.Empty ||
                (!draggingFolder && (targetCell is HomeGridCell.App ||
                    targetCell is HomeGridCell.Folder))
            isHoveredDockSlotValid = true
            showFolderCreationIndicator = !draggingFolder && targetCell is HomeGridCell.App
        } else {
            isHoveredCellValid = true
            isHoveredDockSlotValid = true
            showFolderCreationIndicator = false
        }

        // Edge scroll near screen edges
        edgeScrollJob = handleEdgeScrollDetection(
            dragCenterX = fingerPos.x, edgeScrollZonePx = edgeScrollZonePx,
            screenWidthPx = screenWidthPx, currentPage = pagerState.currentPage,
            totalPages = totalPages, isScrollInProgress = pagerState.isScrollInProgress,
            currentJob = edgeScrollJob, scope = dropScope, pagerState = pagerState,
            setHoveringLeft = { isHoveringLeftEdge = it },
            setHoveringRight = { isHoveringRightEdge = it },
            setSuppressed = { edgeIndicatorSuppressed = it }
        )
    }

    // Shared cleanup for external drag
    fun cleanupExternalDrag() {
        externalDragActive = false
        externalDragItemState = null
        draggedAppInfo = null
        draggedFolderData = null
        draggedFolderPreviewApps = emptyList()
        draggedItemIndex = null
        dragOffset = Offset.Zero
        dragOriginalCellPos = null
        isEditMode = false
        hoveredGridCell = null
        hoveredDockSlot = null
        edgeScrollJob?.cancel()
        edgeScrollJob = null
        onExternalDragComplete()
    }

    // Drop handling: drawer signals finger-up via drop signal counter
    fun performExternalDrop() {
        val item = externalDragItemState ?: return cleanupExternalDrag()
        val originalPos = dragOriginalCellPos ?: return cleanupExternalDrag()
        val intendedPage = pagerState.targetPage
        val targetGridCell = hoveredGridCell
        val targetDockSlot = hoveredDockSlot
        edgeScrollJob?.cancel()
        edgeScrollJob = null

        var targetOffset = Offset.Zero
        var dropAction: (() -> Unit)? = null

        if (targetDockSlot != null) {
            val dockPos = dockPositions[targetDockSlot]
            val existingDockApp = dockApps.find { it.position == targetDockSlot }
            val existingDockFolder = dockFolders.find { it.position == targetDockSlot }
            val isSlotEmpty = existingDockApp == null && existingDockFolder == null
            if (dockPos != null) {
                targetOffset = Offset(dockPos.x - originalPos.x, dockPos.y - originalPos.y)
                if (isSlotEmpty) {
                    dropAction = {
                        when (item) {
                            is com.bearinmind.launcher314.data.AppInfo -> {
                                dockApps = dockApps + DockApp(
                                    packageName = item.packageName,
                                    position = targetDockSlot
                                )
                                saveAllData()
                            }
                            is com.bearinmind.launcher314.data.AppFolder -> {
                                dockFolders = dockFolders + DockFolder(
                                    name = item.name,
                                    position = targetDockSlot,
                                    appPackageNames = item.appPackageNames
                                )
                                saveDockFolders(dockFolders)
                            }
                        }
                    }
                } else if (item is com.bearinmind.launcher314.data.AppInfo && existingDockFolder != null) {
                    // App dropped on existing dock folder → add to folder
                    dropAction = {
                        val updatedDockFolders = dockFolders.map { f ->
                            if (f.id == existingDockFolder.id) {
                                f.copy(appPackageNames = addAppToFolder(f.appPackageNames, item.packageName))
                            } else f
                        }
                        dockFolders = updatedDockFolders
                        saveDockFolders(updatedDockFolders)
                    }
                } else if (item is com.bearinmind.launcher314.data.AppInfo && existingDockApp != null) {
                    // App dropped on existing dock app → create new dock folder
                    dropAction = {
                        val updatedDockApps = dockApps.filter { it.position != targetDockSlot }
                        val newFolder = DockFolder(
                            name = "Folder",
                            position = targetDockSlot,
                            appPackageNames = listOf(existingDockApp.packageName, item.packageName)
                        )
                        dockApps = updatedDockApps
                        dockFolders = dockFolders + newFolder
                        dropScope.launch(Dispatchers.IO) {
                            saveHomeScreenData(context, HomeScreenData(apps = homeApps, dockApps = updatedDockApps, folders = homeFolders, dockFolders = dockFolders + newFolder))
                        }
                    }
                }
            }
        } else if (targetGridCell != null) {
            val pageCells = buildGridCellsForPage(intendedPage)
            val targetCell = pageCells.getOrNull(targetGridCell)
            val targetPos = cellPositions[targetGridCell] ?: run {
                val tRow = targetGridCell / gridColumns
                val tCol = targetGridCell % gridColumns
                Offset(
                    gridAreaOffset.x + tCol * cellSize.width,
                    gridAreaOffset.y + tRow * cellSize.height
                )
            }
            targetOffset = Offset(targetPos.x - originalPos.x, targetPos.y - originalPos.y)

            if (targetCell is HomeGridCell.Empty) {
                dropAction = {
                    when (item) {
                        is com.bearinmind.launcher314.data.AppInfo -> {
                            homeApps = homeApps + HomeScreenApp(
                                packageName = item.packageName,
                                position = targetGridCell,
                                page = intendedPage
                            )
                            saveAllData()
                        }
                        is com.bearinmind.launcher314.data.AppFolder -> {
                            saveHomeFolders(homeFolders + HomeFolder(
                                name = item.name,
                                position = targetGridCell,
                                page = intendedPage,
                                appPackageNames = item.appPackageNames
                            ))
                        }
                    }
                }
            } else if (item is com.bearinmind.launcher314.data.AppInfo && targetCell is HomeGridCell.Folder) {
                // App dropped on existing grid folder → add to folder
                dropAction = {
                    val updatedFolders = homeFolders.map { folder ->
                        if (folder.id == targetCell.folder.id) {
                            folder.copy(appPackageNames = addAppToFolder(folder.appPackageNames, item.packageName))
                        } else folder
                    }
                    homeFolders = updatedFolders
                    saveHomeFolders(updatedFolders)
                }
            } else if (item is com.bearinmind.launcher314.data.AppInfo && targetCell is HomeGridCell.App) {
                // App dropped on existing grid app → create new folder
                dropAction = {
                    val updatedApps = homeApps.toMutableList()
                    updatedApps.removeAll { it.position == targetGridCell && it.page == intendedPage }
                    val newFolder = HomeFolder(
                        name = "Folder",
                        position = targetGridCell,
                        page = intendedPage,
                        appPackageNames = listOf(targetCell.appInfo.packageName, item.packageName)
                    )
                    homeApps = updatedApps
                    homeFolders = homeFolders + newFolder
                    dropScope.launch(Dispatchers.IO) {
                        saveHomeScreenData(context, HomeScreenData(apps = updatedApps, dockApps = dockApps, folders = homeFolders, dockFolders = dockFolders))
                    }
                }
            }
        }

        if (dropAction == null) {
            // No valid target — just clean up
            cleanupExternalDrag()
            return
        }

        // Determine if this drop creates a new folder (app on app)
        val createsFolder = if (item is com.bearinmind.launcher314.data.AppInfo) {
            if (targetDockSlot != null) {
                val existingDockApp = dockApps.find { it.position == targetDockSlot }
                val existingDockFolder = dockFolders.find { it.position == targetDockSlot }
                existingDockApp != null && existingDockFolder == null
            } else if (targetGridCell != null) {
                val pageCells = buildGridCellsForPage(intendedPage)
                pageCells.getOrNull(targetGridCell) is HomeGridCell.App
            } else false
        } else false

        // Determine if dropping onto an existing folder (dock or grid)
        val dropsOntoFolder = if (item is com.bearinmind.launcher314.data.AppInfo) {
            if (targetDockSlot != null) dockFolders.find { it.position == targetDockSlot } != null
            else if (targetGridCell != null) {
                val pageCells = buildGridCellsForPage(intendedPage)
                pageCells.getOrNull(targetGridCell) is HomeGridCell.Folder
            } else false
        } else false

        // Start folder receive pulse animation on the target cell/dock slot
        if ((createsFolder || dropsOntoFolder) && dropAction != null) {
            if (targetGridCell != null) folderReceiveAnimIndex = targetGridCell
            if (targetDockSlot != null) folderReceiveDockSlot = targetDockSlot
        }

        // Animate the drop (same pattern as normal grid/dock drops)
        dropTargetIsDock = targetDockSlot != null
        dropCreatesFolder = createsFolder
        dropStartOffset = dragOffset
        dropTargetOffset = targetOffset
        isDropAnimating = true
        isEditMode = false
        hoveredGridCell = null
        hoveredDockSlot = null

        val capturedAction = dropAction
        dropScope.launch {
            dropAnimProgress.snapTo(0f)
            dropAnimProgress.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
            capturedAction?.invoke()
            folderReceiveAnimIndex = null
            folderReceiveDockSlot = null
            isDropAnimating = false
            cleanupExternalDrag()
        }
    }

    LaunchedEffect(externalDragDropSignal) {
        if (externalDragDropSignal > lastExternalDropSignal && externalDragActive) {
            lastExternalDropSignal = externalDragDropSignal
            performExternalDrop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Root-level drag continuation: when a page transition cancels the cell-level
            // gesture, this parent handler picks up the ongoing drag. As a PARENT pointerInput
            // (not a sibling Box), events flow through the normal hierarchy — children process
            // first in Main pass, then this handler. It only acts when a continuation flag is set.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        // ---- Widget drag continuation ----
                        if (widgetDragContinuedAfterPageSwitch && widgetDragState.draggedWidget != null) {
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val dragDelta = Offset(
                                    change.position.x - change.previousPosition.x,
                                    change.position.y - change.previousPosition.y
                                )
                                change.consume()

                                widgetDragState = widgetDragState.copy(
                                    dragOffset = widgetDragState.dragOffset + dragDelta
                                )

                                val widgetCenter = widgetDragState.startPosition + widgetDragState.dragOffset
                                val widgetCenterX = widgetCenter.x

                                // Edge scroll
                                widgetEdgeScrollJob = handleEdgeScrollDetection(
                                    dragCenterX = widgetCenterX, edgeScrollZonePx = edgeScrollZonePx,
                                    screenWidthPx = screenWidthPx, currentPage = pagerState.currentPage,
                                    totalPages = totalPages, isScrollInProgress = pagerState.isScrollInProgress,
                                    currentJob = widgetEdgeScrollJob, scope = dropScope, pagerState = pagerState,
                                    setHoveringLeft = { isHoveringLeftEdge = it },
                                    setHoveringRight = { isHoveringRightEdge = it },
                                    setSuppressed = { edgeIndicatorSuppressed = it }
                                )

                                // Update hovered cells for highlighting
                                val widget = widgetDragState.draggedWidget!!
                                val dropTarget = calculateWidgetDropTargetFromCenter(
                                    widgetCenter, cellPositions, cellSize,
                                    gridColumns, gridRows,
                                    widget.spanColumns, widget.spanRows
                                )
                                val targetCol = dropTarget?.first ?: -1
                                val targetRow = dropTarget?.second ?: -1
                                if (targetCol >= 0 && targetRow >= 0) {
                                    val targetCells = getWidgetTargetCells(widget, targetCol, targetRow, gridColumns, gridRows)
                                    hoveredWidgetCells = targetCells

                                    val draggedSid2 = widget.stackId
                                    val otherWidgets = placedWidgets.filter { it.appWidgetId != widget.appWidgetId && it.page == pagerState.targetPage && (draggedSid2 == null || it.stackId != draggedSid2) }
                                    val hoveringOverWidget = otherWidgets.any { other ->
                                        val otherCells = mutableSetOf<Int>()
                                        for (r in other.startRow until other.startRow + other.rowSpan) {
                                            for (c in other.startColumn until other.startColumn + other.columnSpan) {
                                                otherCells.add(r * gridColumns + c)
                                            }
                                        }
                                        targetCells.any { otherCells.contains(it) }
                                    }
                                    val atOriginal2 = targetCol == widget.startColumn && targetRow == widget.startRow && pagerState.targetPage == widget.page
                                    isWidgetOverWidget = hoveringOverWidget && !atOriginal2
                                    isWidgetDropTargetValid = if (hoveringOverWidget && !atOriginal2) true
                                        else canPlaceWidgetAt(widget, targetCol, targetRow, gridColumns, gridRows, buildGridCellsForPage(pagerState.targetPage))
                                } else {
                                    hoveredWidgetCells = emptySet()
                                    isWidgetDropTargetValid = true
                                    isWidgetOverWidget = false
                                }
                            } else {
                                performWidgetDrop()
                            }
                            continue
                        }

                        // ---- App/folder drag continuation ----
                        val hasFolderDrag = dragFromFolderApp != null
                        if (!dragContinuedAfterPageSwitch || escapedToHomeGrid || (!hasFolderDrag && (draggedItemIndex == null || draggedFromDock))) {
                            continue
                        }
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            val dragDelta = Offset(
                                change.position.x - change.previousPosition.x,
                                change.position.y - change.previousPosition.y
                            )
                            change.consume()

                            dragOffset += dragDelta
                            draggedItemPosition = (dragOriginalCellPos ?: Offset.Zero) + dragOffset
                            val centerPos = draggedItemPosition + Offset(cellSize.width / 2f, cellSize.height / 2f)

                            val targetDockSlot = findDockSlotIndex(centerPos)
                            hoveredDockSlot = targetDockSlot
                            hoveredGridCell = if (targetDockSlot == null) findCellIndex(centerPos) else null

                            if (targetDockSlot != null) {
                                isHoveredDockSlotValid = dockApps.find { it.position == targetDockSlot } == null
                                isHoveredCellValid = true
                                showFolderCreationIndicator = false
                            } else if (hoveredGridCell != null) {
                                val pageCells = buildGridCellsForPage(pagerState.targetPage)
                                val targetCell = pageCells.getOrNull(hoveredGridCell!!)
                                val draggingFolder = draggedFolderData != null
                                isHoveredCellValid = targetCell is HomeGridCell.Empty ||
                                    (!draggingFolder && (targetCell is HomeGridCell.App ||
                                        targetCell is HomeGridCell.Folder))
                                showFolderCreationIndicator = !draggingFolder &&
                                    targetCell is HomeGridCell.App &&
                                    hoveredGridCell != draggedItemIndex
                                isHoveredDockSlotValid = true
                            } else {
                                isHoveredCellValid = true
                                isHoveredDockSlotValid = true
                                showFolderCreationIndicator = false
                            }

                            edgeScrollJob = handleEdgeScrollDetection(
                                dragCenterX = centerPos.x, edgeScrollZonePx = edgeScrollZonePx,
                                screenWidthPx = screenWidthPx, currentPage = pagerState.currentPage,
                                totalPages = totalPages, isScrollInProgress = pagerState.isScrollInProgress,
                                currentJob = edgeScrollJob, scope = dropScope, pagerState = pagerState,
                                setHoveringLeft = { isHoveringLeftEdge = it },
                                setHoveringRight = { isHoveringRightEdge = it },
                                setSuppressed = { edgeIndicatorSuppressed = it }
                            )
                        } else {
                            performDrop()
                        }
                    }
                }
            }
            // Skip gesture processing when widget is being dragged
            .pointerInput(isWidgetBeingDragged) {
                if (isWidgetBeingDragged) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { },
                    onDragEnd = { },
                    onDragCancel = { },
                    onDrag = { _, _ -> }
                )
            }
            // Swipe-up to open app drawer - also skip when widget dragging or resizing
            .pointerInput(isEditMode, isWidgetBeingDragged, widgetResizeState.isResizing) {
                if (!isEditMode && !isWidgetBeingDragged && !widgetResizeState.isResizing) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { _, dragAmount ->
                            swipeOffset += dragAmount.y
                            if (swipeOffset < -swipeThreshold) {
                                onOpenAppDrawer()
                                swipeOffset = 0f
                            }
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (getDoubleTapLockEnabled(context)) {
                            AppDrawerAccessibilityService.lockScreen(context)
                        }
                    }
                )
            }
    ) {
        // Background is transparent - system wallpaper shows through via theme
        // Main content - respects system bars (status bar & navigation bar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // App grid area - takes full screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .zIndex(if (isWidgetBeingDragged) 1f else 0f)
                    .onGloballyPositioned { coordinates ->
                        gridAreaBoxOffset = coordinates.positionInRoot()
                        gridAreaBoxSize = coordinates.size
                    }
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // Disable manual swipe during drag to prevent conflicts
                        userScrollEnabled = draggedItemIndex == null && !isDropAnimating && !externalDragActive && !isWidgetBeingDragged && !isStackSwipeActive
                    ) { page ->
                    // Grid with drag and drop - using Column/Row for proper space distribution
                    // The outer Column applies padding so both grid cells AND widgets respect the same bounds
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = gridHPadding, vertical = gridVPadding)
                            .graphicsLayer { clip = false } // Allow bottom row text to overflow into padding
                    ) {
                        // Inner Box contains both grid and widget overlay
                        // This ensures widgets are positioned within the same padded bounds as apps
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { clip = false } // Allow text overflow
                                .onGloballyPositioned { coordinates ->
                                    gridAreaOffset = coordinates.positionInRoot()
                                }
                        ) {
                            // Grid content - clip = false allows text/icons to overflow cell bounds
                            Column(modifier = Modifier.fillMaxSize().graphicsLayer { clip = false }) {
                                for (row in 0 until gridRows) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .graphicsLayer { clip = false }
                                    ) {
                                        for (column in 0 until gridColumns) {
                                    val index = row * gridColumns + column
                                    val pageCells = if (page == currentPage) gridCells else buildGridCellsForPage(page)
                                    val cell = pageCells.getOrElse(index) { HomeGridCell.Empty }
                                    val isDragging = draggedItemIndex == index && !draggedFromDock && page == dragSourcePage
                                    val isDropTarget = draggedItemIndex != null && (draggedItemIndex != index || page != dragSourcePage)
                                    // Check if this cell is hovered by:
                                    // - App drag (hoveredGridCell)
                                    // - Widget drop target (hoveredWidgetCells) — but NOT during widget-over-widget (stacking)
                                    //   and only on the correct page (drag target page or resize widget's page)
                                    val widgetHoverPage = if (widgetResizeState.isResizing) widgetResizeState.resizingWidget?.page ?: 0
                                        else pagerState.targetPage
                                    val isHovered = hoveredGridCell == index ||
                                                    (hoveredWidgetCells.contains(index) && !isWidgetOverWidget && page == widgetHoverPage)
                                    // Any item dragging includes both apps and widgets
                                    // Exclude drop animation so "+" markers disappear instantly on release
                                    val isAnyDragging = (draggedItemIndex != null && !isDropAnimating) || (widgetDragState.draggedWidget != null && !isWidgetDropAnimating)
                                    // Valid drop target for apps: original position OR empty cell
                                    // For widgets: original cells are valid, others depend on isWidgetDropTargetValid
                                    val isDraggingAFolder = draggedFolderData != null
                                    val isValidDropTarget = when {
                                        // This cell is being dragged - always valid (blue) at original position
                                        isDragging -> true
                                        // Empty cell - always valid drop target (blue)
                                        cell is HomeGridCell.Empty -> true
                                        // Folder cell - valid drop target only for APP drags (not folder drags)
                                        cell is HomeGridCell.Folder && hoveredGridCell == index && (draggedItemIndex != null || externalDragActive) && !isDraggingAFolder -> true
                                        // App cell hovered by another app - valid (creates folder), not for folder drags
                                        cell is HomeGridCell.App && hoveredGridCell == index && (draggedItemIndex != null || externalDragActive) && draggedItemIndex != index && !isDraggingAFolder -> true
                                        // Widget resize/drag - original cells are always valid (blue, not red)
                                        // Only on the SOURCE page — on other pages, same indices are different cells
                                        hoveredWidgetCells.contains(index) && widgetOriginalCells.contains(index) && page == widgetDragSourcePage -> true
                                        // Widget resize/drag - non-original cells show red if overlapping other apps
                                        // Only on the correct page (target page for drag, widget page for resize)
                                        hoveredWidgetCells.contains(index) && page == widgetHoverPage -> isWidgetDropTargetValid
                                        // App being dragged and hovering over this occupied cell - INVALID (red)
                                        hoveredGridCell == index && draggedItemIndex != null -> false
                                        // Not hovered - default to true (no indicator shown anyway)
                                        else -> true
                                    }

                                    // zIndex ensures app/folder content (text/icon) draws above empty cells
                                    val cellPosition = when (cell) {
                                        is HomeGridCell.App -> cell.position
                                        is HomeGridCell.Folder -> cell.position
                                        else -> -1
                                    }
                                    val isRemoving = removeState.gridKey != null && removeState.gridKey == (cellPosition to page)
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .zIndex(if (cell is HomeGridCell.App || cell is HomeGridCell.Folder) 1f else 0f)
                                        .graphicsLayer {
                                            clip = false
                                            if (isRemoving) {
                                                alpha = removeState.anim.value
                                                scaleX = removeState.anim.value
                                                scaleY = removeState.anim.value
                                            }
                                        }
                                    ) {
                                        DraggableGridCell(
                                            cell = cell,
                                            index = index,
                                            iconSize = iconSizeDp,
                                            gridColumns = gridColumns,
                                            gridRows = gridRows,
                                            isEditMode = isEditMode,
                                            isDragging = isDragging,
                                            // Lambda to check ownership dynamically (evaluated at call time, not capture time)
                                            checkIsDragOwner = { draggedItemIndex == index && !draggedFromDock && page == dragSourcePage },
                                            isAnyItemDragging = isAnyDragging,
                                            isDropTarget = isDropTarget,
                                            isHovered = isHovered,
                                            isValidDropTarget = isValidDropTarget,
                                            // When this cell is being dragged, is the hover target valid? (for icon tint)
                                            isHoverTargetValid = if (isDragging) {
                                                when {
                                                    hoveredDockSlot != null -> isHoveredDockSlotValid
                                                    hoveredGridCell != null -> isHoveredCellValid
                                                    else -> true
                                                }
                                            } else true,
                                            dragOffset = if (isDragging) dragOffset else Offset.Zero,
                                            // CRITICAL: Skip gesture processing when widget, escape drag, or another cell's drag is active
                                            // Prevents other cells from picking up the pointer and showing popups that steal focus
                                            isWidgetDragging = widgetDragState.draggedWidget != null || escapedToHomeGrid ||
                                                (draggedItemIndex != null && draggedItemIndex != index),
                                            // Dynamic check evaluated inside gesture handler AFTER long press fires
                                            // Prevents popup when pointer has been down 400ms+ from original cell's press
                                            isAnyDragActive = { draggedItemIndex != null || dragFromFolderApp != null || externalDragActive },
                                            // Proportional sizing
                                            markerHalfSizeParam = gridMarkerHalfSize,
                                            plusMarkerSize = gridPlusMarkerSize,
                                            plusMarkerFontSize = gridPlusMarkerFont,
                                            appNameFontSize = gridAppNameFont,
                                            appNameFontFamily = selectedFontFamily,
                                            iconTextSpacer = gridIconTextSpacer,
                                            hoverCornerRadius = gridHoverCornerRadius,
                                            folderPreviewDraggedIconPath = if (hoveredGridCell == index && !isDraggingAFolder && (
                                                showFolderCreationIndicator ||
                                                (cell is HomeGridCell.Folder && (draggedItemIndex != null || dragFromFolderApp != null || externalDragActive) && draggedItemIndex != index)
                                            )) draggedAppInfo?.iconPath else null,
                                            isReceivingDrop = folderReceiveAnimIndex == index,
                                            folderCustomization = if (cell is HomeGridCell.Folder) appCustomizations.customizations["folder_${cell.folder.id}"] else null,
                                            onPositioned = { position, size ->
                                                cellPositions = cellPositions + (index to position)
                                                cellSize = size
                                            },
                                            onDragStart = {
                                                // Only start drag if not already dragging something else
                                                if (draggedItemIndex == null && !isDropAnimating && dragFromFolderApp == null) {
                                                    lastDragPagerPos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                                    if (cell is HomeGridCell.App) {
                                                        isEditMode = true
                                                        draggedItemIndex = index
                                                        draggedFromDock = false
                                                        draggedAppInfo = cell.appInfo
                                                        dragSourcePage = currentPage
                                                        dragOriginalCellPos = cellPositions[index]
                                                    } else if (cell is HomeGridCell.Folder) {
                                                        isEditMode = true
                                                        draggedItemIndex = index
                                                        draggedFromDock = false
                                                        draggedAppInfo = cell.previewApps.firstOrNull()
                                                        draggedFolderData = cell.folder
                                                        draggedFolderPreviewApps = cell.previewApps
                                                        dragSourcePage = currentPage
                                                        dragOriginalCellPos = cellPositions[index]
                                                    }
                                                }
                                            },
                                            onDrag = { offset ->
                                                // The cell's local coordinate system moves with the pager.
                                                // Subtract the pager's movement so dragOffset tracks
                                                // only actual finger movement in screen space.
                                                val currentPagerPos = pagerState.currentPage +
                                                    pagerState.currentPageOffsetFraction
                                                val pagerDelta = ((currentPagerPos - lastDragPagerPos) *
                                                    screenWidthPx).toFloat()
                                                lastDragPagerPos = currentPagerPos
                                                dragOffset += Offset(offset.x - pagerDelta, offset.y)
                                                // Use dragOriginalCellPos (captured at drag start) for stable positioning
                                                // cellPositions[index] can change during recomposition, causing position jumps
                                                draggedItemPosition = (dragOriginalCellPos ?: cellPositions[index] ?: Offset.Zero) + dragOffset
                                                // Check if hovering over dock or grid cell
                                                val centerPos = draggedItemPosition + Offset(cellSize.width / 2f, cellSize.height / 2f)
                                                val targetDockSlot = findDockSlotIndex(centerPos)
                                                hoveredDockSlot = targetDockSlot
                                                // Track hovered grid cell (only if not hovering dock)
                                                // Include original position so empty cell indicator shows there too
                                                val targetGridCell = if (targetDockSlot == null) {
                                                    findCellIndex(centerPos)
                                                } else null
                                                hoveredGridCell = targetGridCell

                                                // Check if hover target is valid (for icon red tint)
                                                if (targetDockSlot != null) {
                                                    // Hovering over dock - check if slot is empty
                                                    val dockSlotApp = dockApps.find { it.position == targetDockSlot }
                                                    val dockSlotFolder = dockFolders.find { it.position == targetDockSlot }
                                                    isHoveredDockSlotValid = dockSlotApp == null && dockSlotFolder == null
                                                    isHoveredCellValid = true // Not hovering grid
                                                    showFolderCreationIndicator = false
                                                } else if (targetGridCell != null) {
                                                    // Hovering over grid - use fresh build to avoid stale capture in pointerInput
                                                    val pageCells = buildGridCellsForPage(pagerState.targetPage)
                                                    val targetCell = pageCells.getOrNull(targetGridCell)
                                                    // Folders can only be dropped on empty cells
                                                    val draggingFolder = draggedFolderData != null
                                                    isHoveredCellValid = targetGridCell == index ||
                                                        targetCell is HomeGridCell.Empty ||
                                                        (!draggingFolder && (targetCell is HomeGridCell.App ||
                                                            targetCell is HomeGridCell.Folder))
                                                    showFolderCreationIndicator = !draggingFolder &&
                                                        targetCell is HomeGridCell.App &&
                                                        targetGridCell != index
                                                    isHoveredDockSlotValid = true // Not hovering dock
                                                } else {
                                                    isHoveredCellValid = true
                                                    isHoveredDockSlotValid = true
                                                    showFolderCreationIndicator = false
                                                }

                                                // Edge scroll detection - switch pages when dragging near screen edges
                                                edgeScrollJob = handleEdgeScrollDetection(
                                                    dragCenterX = centerPos.x, edgeScrollZonePx = edgeScrollZonePx,
                                                    screenWidthPx = screenWidthPx, currentPage = pagerState.currentPage,
                                                    totalPages = totalPages, isScrollInProgress = pagerState.isScrollInProgress,
                                                    currentJob = edgeScrollJob, scope = dropScope, pagerState = pagerState,
                                                    setHoveringLeft = { isHoveringLeftEdge = it },
                                                    setHoveringRight = { isHoveringRightEdge = it },
                                                    setSuppressed = { edgeIndicatorSuppressed = it }
                                                )
                                            },
                                            onDragEnd = {
                                                // The cell gesture gets cancelled when the pager scrolls
                                                // (the cell composable leaves the visible area). If a page
                                                // transition happened or is in progress, keep the drag alive
                                                // so the root-level handler can continue tracking.
                                                val pagerScrolling = pagerState.isScrollInProgress ||
                                                    (edgeScrollJob?.isActive == true)
                                                val pageChanged = pagerState.targetPage != dragSourcePage
                                                if ((pagerScrolling || pageChanged) && draggedItemIndex != null) {
                                                    dragContinuedAfterPageSwitch = true
                                                } else {
                                                    performDrop()
                                                }
                                            },
                                            onTap = {
                                                if (homeSelectionModeActive) {
                                                    if (cell is HomeGridCell.App) {
                                                        val cellKey = "${currentPage}_${index}"
                                                        selectedHomeCells = if (cellKey in selectedHomeCells) selectedHomeCells - cellKey else selectedHomeCells + cellKey
                                                        if (selectedHomeCells.isEmpty()) homeSelectionModeActive = false
                                                    } else {
                                                        // Tap on empty cell or folder while in selection mode — deselect all
                                                        selectedHomeCells = emptySet()
                                                        homeSelectionModeActive = false
                                                    }
                                                } else if (cell is HomeGridCell.App && !isEditMode) {
                                                    launchApp(context, cell.appInfo.packageName)
                                                } else if (cell is HomeGridCell.Folder && !isEditMode) {
                                                    openHomeFolder = cell.folder
                                                }
                                            },
                                            onLongPress = { touchPosition ->
                                                if (cell is HomeGridCell.Empty) {
                                                    launcherMenuPosition = touchPosition
                                                    showLauncherSettingsMenu = true
                                                } else if (cell is HomeGridCell.App) {
                                                    isEditMode = true
                                                } else if (cell is HomeGridCell.Folder) {
                                                    isEditMode = true
                                                }
                                            },
                                            onRemove = {
                                                val cellPos = when (cell) {
                                                    is HomeGridCell.App -> cell.position
                                                    is HomeGridCell.Folder -> cell.position
                                                    else -> -1
                                                }
                                                if (cellPos >= 0) {
                                                    dropScope.launch {
                                                        removeState.gridKey = cellPos to page
                                                        removeState.anim.snapTo(1f)
                                                        removeState.anim.animateTo(
                                                            0f,
                                                            tween(300, easing = FastOutSlowInEasing)
                                                        )
                                                        if (cell is HomeGridCell.App) {
                                                            saveHomeApps(homeApps.filter { !(it.position == cell.position && it.page == page) })
                                                        } else if (cell is HomeGridCell.Folder) {
                                                            saveHomeFolders(homeFolders.filter { it.id != cell.folder.id })
                                                        }
                                                        removeState.gridKey = null
                                                    }
                                                }
                                            },
                                            onUninstall = {
                                                if (cell is HomeGridCell.App) {
                                                    uninstallApp(context, cell.appInfo.packageName)
                                                }
                                            },
                                            onAppInfo = {
                                                if (cell is HomeGridCell.App) {
                                                    openAppInfo(context, cell.appInfo.packageName)
                                                }
                                            },
                                            onCustomize = {
                                                if (cell is HomeGridCell.App) {
                                                    customizingApp = cell.appInfo
                                                } else if (cell is HomeGridCell.Folder) {
                                                    customizingFolder = cell.folder
                                                }
                                            },
                                            onWidgetRemove = {
                                                if (cell is HomeGridCell.Widget) {
                                                    WidgetManager.removePlacedWidget(context, cell.placedWidget.appWidgetId)
                                                    placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                                }
                                            },
                                            isCustomizing = (cell is HomeGridCell.App && customizingApp?.packageName == cell.appInfo.packageName) ||
                                                (cell is HomeGridCell.Folder && customizingFolder?.id == cell.folder.id),
                                            globalIconSizePercent = iconSizePercent.toFloat(),
                                            globalIconShape = globalIconShape,
                                            globalIconBgColor = globalIconBgColor,
                                            globalIconBgIntensity = globalIconBgIntensity,
                                            isSelected = if (cell is HomeGridCell.App) "${currentPage}_${index}" in selectedHomeCells else false,
                                            selectionModeActive = homeSelectionModeActive,
                                            selectedCount = selectedHomeCells.size,
                                            onSelectToggle = {
                                                if (cell is HomeGridCell.App) {
                                                    homeSelectionModeActive = true
                                                    val cellKey = "${currentPage}_${index}"
                                                    selectedHomeCells = if (cellKey in selectedHomeCells) selectedHomeCells - cellKey else selectedHomeCells + cellKey
                                                    if (selectedHomeCells.isEmpty()) homeSelectionModeActive = false
                                                }
                                            },
                                            onBulkRemove = {
                                                // Remove all selected apps from home screen by their positions
                                                val updatedApps = homeApps.toMutableList()
                                                selectedHomeCells.forEach { cellKey ->
                                                    val parts = cellKey.split("_")
                                                    if (parts.size == 2) {
                                                        val page = parts[0].toIntOrNull() ?: return@forEach
                                                        val pos = parts[1].toIntOrNull() ?: return@forEach
                                                        updatedApps.removeAll { it.page == page && it.position == pos }
                                                    }
                                                }
                                                homeApps = updatedApps
                                                saveHomeApps(updatedApps)
                                                selectedHomeCells = emptySet()
                                                homeSelectionModeActive = false
                                            },
                                            onCreateFolder = {
                                                pendingFolderCellKey = "${currentPage}_${index}"
                                                showCreateHomeFolderDialog = true
                                            }
                                            )
                                        }
                                    }
                                }
                            }
                            } // End of inner grid Column

                            // Widget overlay layer - renders widgets on top of grid cells
                            // Inside the same Box as the grid, so widgets respect the same padded bounds
                            // Widgets now support direct long-press + drag (like apps)
                            if (cellSize.width > 0 && cellSize.height > 0) {
                                // Filter widgets for this page, skipping non-primary stacked widgets
                                // Sort by stackOrder so the primary widget (order=0) is always picked first per stack
                                val processedStacks = mutableSetOf<String>()
                                placedWidgets.filter { it.page == page }.sortedBy { it.stackOrder }.filter { widget ->
                                    val sid = widget.stackId
                                    if (sid == null) true // standalone widget
                                    else if (processedStacks.contains(sid)) false // already handled
                                    else { processedStacks.add(sid); true } // primary in stack
                                }.forEach { widget ->
                                    key(widget.appWidgetId, widget.stackId) {
                                    val originCellIndex = widget.gridRow * gridColumns + widget.gridColumn
                                    val originCellPos = cellPositions[originCellIndex]

                                    if (originCellPos != null) {
                                        // Check if THIS widget is being resized (use resize dimensions)
                                        val isThisWidgetResizing = widgetResizeState.isResizing &&
                                            widgetResizeState.resizingWidget?.appWidgetId == widget.appWidgetId
                                        val resizeDims = if (isThisWidgetResizing) currentResizeDimensions else null

                                        // Calculate widget position relative to the grid area
                                        // Use resize dimensions if resizing, otherwise use widget's stored values
                                        val widgetLeft = if (resizeDims != null) {
                                            resizeDims.column * cellSize.width
                                        } else {
                                            originCellPos.x - gridAreaOffset.x
                                        }
                                        val widgetTop = if (resizeDims != null) {
                                            resizeDims.row * cellSize.height
                                        } else {
                                            originCellPos.y - gridAreaOffset.y
                                        }
                                        val widgetWidth = if (resizeDims != null) {
                                            resizeDims.columnSpan * cellSize.width
                                        } else {
                                            widget.spanColumns * cellSize.width
                                        }
                                        val widgetHeight = if (resizeDims != null) {
                                            resizeDims.rowSpan * cellSize.height
                                        } else {
                                            widget.spanRows * cellSize.height
                                        }

                                        // Check if THIS widget is being dragged
                                        val isThisWidgetDragging = widgetDragState.draggedWidget?.appWidgetId == widget.appWidgetId
                                        val isThisWidgetDropAnimating = isWidgetDropAnimating && widgetDropWidgetId == widget.appWidgetId

                                        // Visual effects for dragging (like apps)
                                        // During drop animation, smoothly return to normal over 400ms
                                        val widgetScale by animateFloatAsState(
                                            targetValue = if (isThisWidgetDragging && !isThisWidgetDropAnimating) 1.1f else 1f,
                                            animationSpec = tween(if (isThisWidgetDropAnimating) 400 else 150),
                                            label = "widgetScale"
                                        )
                                        val widgetAlpha by animateFloatAsState(
                                            targetValue = if (isThisWidgetDragging && !isThisWidgetDropAnimating) 0.8f else 1f,
                                            animationSpec = tween(if (isThisWidgetDropAnimating) 400 else 150),
                                            label = "widgetAlpha"
                                        )

                                        // State for this widget's context menu
                                        var showWidgetMenu by remember { mutableStateOf(false) }
                                        var currentStackPage by remember { mutableIntStateOf(0) }
                                        val hapticFeedback = rememberHapticFeedback()

                                        // Track local drag state for this widget's gesture handler
                                        var localDragStarted by remember { mutableStateOf(false) }

                                        // Track widget's ACTUAL screen position (before graphicsLayer transforms)
                                        // This is the single source of truth for position calculations
                                        var widgetBaseScreenPos by remember { mutableStateOf(Offset.Zero) }

                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    IntOffset(widgetLeft.toInt(), widgetTop.toInt())
                                                }
                                                .size(
                                                    width = with(density) { widgetWidth.toDp() },
                                                    height = with(density) { widgetHeight.toDp() }
                                                )
                                                .zIndex(if (isThisWidgetDragging || isThisWidgetDropAnimating) 100f else 1f)
                                                // Track actual screen position BEFORE graphicsLayer transforms
                                                .onGloballyPositioned { coords ->
                                                    widgetBaseScreenPos = coords.positionInRoot()
                                                }
                                                .graphicsLayer {
                                                    // Apply drag offset or drop animation translation
                                                    if (isThisWidgetDropAnimating) {
                                                        val p = widgetDropAnim.value
                                                        translationX = widgetDropStartOffset.x + (widgetDropTargetOffset.x - widgetDropStartOffset.x) * p
                                                        translationY = widgetDropStartOffset.y + (widgetDropTargetOffset.y - widgetDropStartOffset.y) * p
                                                    } else if (isThisWidgetDragging) {
                                                        translationX = widgetDragState.dragOffset.x
                                                        translationY = widgetDragState.dragOffset.y
                                                    }
                                                    scaleX = widgetScale
                                                    scaleY = widgetScale
                                                    // Hide original during drag AND drop animation (overlay handles both)
                                                    alpha = if ((isThisWidgetDragging || isThisWidgetDropAnimating) && widgetDragBitmap != null) 0f else widgetAlpha
                                                }
                                                // Gesture handler for long-press + drag (like apps)
                                                // Key includes span so lambda refreshes after resize
                                                .pointerInput(widget.appWidgetId, widget.columnSpan, widget.rowSpan) {
                                                    val touchSlop = viewConfiguration.touchSlop

                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                        val startPosition = down.position

                                                        // Check bounds
                                                        if (startPosition.x < 0 || startPosition.x > size.width ||
                                                            startPosition.y < 0 || startPosition.y > size.height) {
                                                            return@awaitEachGesture
                                                        }

                                                        localDragStarted = false

                                                        // Capture widget center at the START of this gesture
                                                        // Using 'size' from pointerInput scope = actual widget size in pixels
                                                        val widgetCenterAtGestureStart = Offset(
                                                            widgetBaseScreenPos.x + size.width / 2f,
                                                            widgetBaseScreenPos.y + size.height / 2f
                                                        )

                                                        // Wait for long press
                                                        val longPress = awaitLongPressOrCancellation(down.id)

                                                        if (longPress != null) {
                                                            // Long press triggered - show menu and prepare for drag
                                                            showWidgetMenu = true
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

                                                                        if (distance > touchSlop && !localDragStarted) {
                                                                            // Movement after long press = start drag, hide menu
                                                                            localDragStarted = true
                                                                            showWidgetMenu = false

                                                                            // Calculate widget's original cells (before drag)
                                                                            val originalCells = mutableSetOf<Int>()
                                                                            for (r in 0 until widget.spanRows) {
                                                                                for (c in 0 until widget.spanColumns) {
                                                                                    val cellIndex = (widget.gridRow + r) * gridColumns + (widget.gridColumn + c)
                                                                                    originalCells.add(cellIndex)
                                                                                }
                                                                            }
                                                                            widgetOriginalCells = originalCells

                                                                            // Capture widget bitmap for root-level drag overlay (above dock bar)
                                                                            // For stacked widgets, capture the currently visible widget
                                                                            try {
                                                                                val stackedWidgets = if (widget.stackId != null) {
                                                                                    WidgetManager.getStackWidgets(placedWidgets, widget.stackId!!)
                                                                                } else listOf(widget)
                                                                                // Try each widget in the stack until we get a valid bitmap
                                                                                var captured = false
                                                                                for (sw in stackedWidgets) {
                                                                                    val wv = WidgetManager.getWidgetView(sw.appWidgetId)
                                                                                    if (wv != null && wv.width > 0 && wv.height > 0) {
                                                                                        widgetDragBitmap = wv.drawToBitmap().asImageBitmap()
                                                                                        widgetDragScreenPos = widgetBaseScreenPos
                                                                                        widgetDragSizePx = Pair(widgetWidth, widgetHeight)
                                                                                        captured = true
                                                                                        break
                                                                                    }
                                                                                }
                                                                            } catch (_: Exception) {}

                                                                            // Start widget drag mode
                                                                            widgetDragSourcePage = pagerState.currentPage
                                                                            widgetDragState = WidgetDragState(
                                                                                draggedWidget = widget,
                                                                                dragOffset = Offset.Zero,
                                                                                startPosition = widgetCenterAtGestureStart  // Store CENTER position
                                                                            )
                                                                        }

                                                                        // Process drag if this widget is being dragged
                                                                        if (localDragStarted && widgetDragState.draggedWidget?.appWidgetId == widget.appWidgetId) {
                                                                            val dragDelta = Offset(
                                                                                change.position.x - change.previousPosition.x,
                                                                                change.position.y - change.previousPosition.y
                                                                            )
                                                                            change.consume()

                                                                            // No pager correction needed: the widget's graphicsLayer
                                                                            // { translationX = dragOffset.x } causes Compose to
                                                                            // inverse-transform pointer events, so deltas are already
                                                                            // in screen space (not affected by pager scroll).
                                                                            widgetDragState = widgetDragState.copy(
                                                                                dragOffset = widgetDragState.dragOffset + dragDelta
                                                                            )

                                                                            // Calculate widget CENTER position
                                                                            val widgetCenter = widgetCenterAtGestureStart + widgetDragState.dragOffset

                                                                            // Edge scroll: check if widget center is near screen edges
                                                                            widgetEdgeScrollJob = handleEdgeScrollDetection(
                                                                                dragCenterX = widgetCenter.x, edgeScrollZonePx = edgeScrollZonePx,
                                                                                screenWidthPx = screenWidthPx, currentPage = pagerState.currentPage,
                                                                                totalPages = totalPages, isScrollInProgress = pagerState.isScrollInProgress,
                                                                                currentJob = widgetEdgeScrollJob, scope = dropScope, pagerState = pagerState,
                                                                                setHoveringLeft = { isHoveringLeftEdge = it },
                                                                                setHoveringRight = { isHoveringRightEdge = it },
                                                                                setSuppressed = { edgeIndicatorSuppressed = it }
                                                                            )

                                                                            // Use CENTER-based drop target calculation
                                                                            val dropTarget = calculateWidgetDropTargetFromCenter(
                                                                                widgetCenter, cellPositions, cellSize,
                                                                                gridColumns, gridRows,
                                                                                widget.spanColumns, widget.spanRows
                                                                            )
                                                                            val targetCol = dropTarget?.first ?: -1
                                                                            val targetRow = dropTarget?.second ?: -1

                                                                            if (targetCol >= 0 && targetRow >= 0) {
                                                                                // Calculate target cells
                                                                                val targetCells = getWidgetTargetCells(widget, targetCol, targetRow, gridColumns, gridRows)
                                                                                hoveredWidgetCells = targetCells

                                                                                // Check if hovering over another widget (for stacking)
                                                                                val draggedSid3 = widget.stackId
                                                                                val otherWidgets = placedWidgets.filter { it.appWidgetId != widget.appWidgetId && it.page == pagerState.targetPage && (draggedSid3 == null || it.stackId != draggedSid3) }
                                                                                val hoveringOverWidget = otherWidgets.any { other ->
                                                                                    val otherCells = mutableSetOf<Int>()
                                                                                    for (r in other.startRow until other.startRow + other.rowSpan) {
                                                                                        for (c in other.startColumn until other.startColumn + other.columnSpan) {
                                                                                            otherCells.add(r * gridColumns + c)
                                                                                        }
                                                                                    }
                                                                                    targetCells.any { otherCells.contains(it) }
                                                                                }
                                                                                val atOriginal3 = targetCol == widget.startColumn && targetRow == widget.startRow && pagerState.targetPage == widget.page
                                                                                isWidgetOverWidget = hoveringOverWidget && !atOriginal3

                                                                                // Check if placement is valid on the current target page
                                                                                isWidgetDropTargetValid = if (hoveringOverWidget && !atOriginal3) true
                                                                                    else canPlaceWidgetAt(widget, targetCol, targetRow, gridColumns, gridRows, buildGridCellsForPage(pagerState.targetPage))
                                                                            } else {
                                                                                hoveredWidgetCells = emptySet()
                                                                                isWidgetDropTargetValid = true
                                                                                isWidgetOverWidget = false
                                                                            }
                                                                        }
                                                                    } else {
                                                                        // Finger released (or gesture lost during page scroll)
                                                                        if (localDragStarted && widgetDragState.draggedWidget?.appWidgetId == widget.appWidgetId) {
                                                                            val pagerScrolling = pagerState.isScrollInProgress ||
                                                                                (widgetEdgeScrollJob?.isActive == true)
                                                                            val pageChanged = pagerState.targetPage != widgetDragSourcePage
                                                                            if (pagerScrolling || pageChanged) {
                                                                                // Page transition in progress — keep drag alive,
                                                                                // root handler will continue tracking
                                                                                widgetDragContinuedAfterPageSwitch = true
                                                                            } else {
                                                                                performWidgetDrop()
                                                                            }
                                                                        }
                                                                        localDragStarted = false
                                                                        break
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                // Gesture cancelled — usually because pager scroll
                                                                // removed this composable. If a page transition happened
                                                                // or is in progress, keep the drag alive for the root handler.
                                                                if (localDragStarted && widgetDragState.draggedWidget != null) {
                                                                    val pagerScrolling = pagerState.isScrollInProgress ||
                                                                        (widgetEdgeScrollJob?.isActive == true)
                                                                    val pageChanged = pagerState.targetPage != widgetDragSourcePage
                                                                    if (pagerScrolling || pageChanged) {
                                                                        widgetDragContinuedAfterPageSwitch = true
                                                                    } else {
                                                                        // Genuine error — reset
                                                                        widgetEdgeScrollJob?.cancel()
                                                                        widgetEdgeScrollJob = null
                                                                        widgetDragState = WidgetDragState()
                                                                        hoveredWidgetCells = emptySet()
                                                                        widgetOriginalCells = emptySet()
                                                                        isWidgetDropTargetValid = true
                                                                        isWidgetOverWidget = false
                                                                        widgetDragBitmap = null
                                                                    }
                                                                } else {
                                                                    widgetDragState = WidgetDragState()
                                                                    hoveredWidgetCells = emptySet()
                                                                    widgetOriginalCells = emptySet()
                                                                    isWidgetDropTargetValid = true
                                                                    isWidgetOverWidget = false
                                                                    widgetDragBitmap = null
                                                                }
                                                                localDragStarted = false
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            // Calculate this widget's cells to check if something is hovering over it
                                            val thisWidgetCells = mutableSetOf<Int>()
                                            for (r in widget.startRow until widget.startRow + widget.rowSpan) {
                                                for (c in widget.startColumn until widget.startColumn + widget.columnSpan) {
                                                    thisWidgetCells.add(r * gridColumns + c)
                                                }
                                            }

                                            // Check if an app is being dragged over this widget
                                            val isAppHoveringOverThisWidget = hoveredGridCell != null &&
                                                thisWidgetCells.contains(hoveredGridCell) &&
                                                draggedItemIndex != null

                                            // Check if another widget is being dragged/resized over this widget
                                            // Exclude: this widget being dragged, resized, same stack as dragged widget, or during drop animation
                                            val draggedStackId = widgetDragState.draggedWidget?.stackId
                                            val isInSameStackAsDragged = draggedStackId != null && widget.stackId == draggedStackId
                                            val isOtherWidgetHoveringOverThis = !isThisWidgetDragging &&
                                                !isThisWidgetResizing &&
                                                !isInSameStackAsDragged &&
                                                !isWidgetDropAnimating &&
                                                hoveredWidgetCells.isNotEmpty() &&
                                                thisWidgetCells.any { hoveredWidgetCells.contains(it) }

                                            // Apply red tint when:
                                            // 1. This widget is being dragged over an invalid target
                                            // 2. This widget is being resized to an invalid size (overlapping)
                                            // 3. An app is being dragged over this widget (can't drop on widget)
                                            val showWidgetRedTint = (isThisWidgetDragging && !isWidgetDropTargetValid) ||
                                                (isThisWidgetResizing && !isWidgetDropTargetValid) ||
                                                isAppHoveringOverThisWidget

                                            // Apply blue tint when another widget is hovering over this one (stack target)
                                            val showWidgetBlueTint = isOtherWidgetHoveringOverThis

                                            // Calculate effective padding and corner radius for this widget
                                            val effectivePaddingPercent = widget.paddingPercent ?: globalWidgetPaddingPercent
                                            val effectivePaddingDp = (effectivePaddingPercent / 100f * WIDGET_MAX_PADDING_DP).dp
                                            val effectiveCornerRadiusDp = if (widgetRoundedCornersEnabled) {
                                                widgetCornerRadiusPercent / 100f * WIDGET_MAX_CORNER_RADIUS_DP
                                            } else 0f

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(effectivePaddingDp)
                                                    .graphicsLayer {
                                                        // Render to offscreen buffer so blend mode works on widget content
                                                        compositingStrategy = CompositingStrategy.Offscreen
                                                    }
                                                    .drawWithContent {
                                                        drawContent() // Draw the widget first
                                                        // Draw tint ONLY on widget's visible content using SrcAtop blend
                                                        if (showWidgetRedTint) {
                                                            drawRect(
                                                                color = Color(0xFFFF6B6B).copy(alpha = 0.5f),
                                                                blendMode = BlendMode.SrcAtop
                                                            )
                                                        } else if (showWidgetBlueTint) {
                                                            drawRect(
                                                                color = Color(0xFF1A3D7A).copy(alpha = 0.7f),
                                                                blendMode = BlendMode.SrcAtop
                                                            )
                                                        }
                                                    }
                                            ) {
                                                val stackWidgets = if (widget.stackId != null) {
                                                    WidgetManager.getStackWidgets(placedWidgets, widget.stackId!!)
                                                } else listOf(widget)

                                                if (stackWidgets.size > 1) {
                                                    // Stacked widgets — swipeable pager with nav-style dots inside widget
                                                    val savedPage = (stackPageMap[widget.stackId] ?: 0).coerceIn(0, stackWidgets.size - 1)
                                                    val stackPagerState = rememberPagerState(initialPage = savedPage, pageCount = { stackWidgets.size })
                                                    // Sync current page to outer state for context menu + persist
                                                    LaunchedEffect(stackPagerState.currentPage) {
                                                        currentStackPage = stackPagerState.currentPage
                                                        if (widget.stackId != null) {
                                                            stackPageMap[widget.stackId!!] = stackPagerState.currentPage
                                                            // Save to prefs
                                                            val encoded = stackPageMap.entries.joinToString(",") { "${it.key}=${it.value}" }
                                                            appContext.getSharedPreferences("app_drawer_settings", android.content.Context.MODE_PRIVATE)
                                                                .edit().putString("widget_stack_pages", encoded).apply()
                                                        }
                                                    }
                                                    val stackDotBaseColor = getScrollbarColor(context)
                                                    val stackDotIntensity = getScrollbarIntensity(context)
                                                    val stackDotColor = remember(stackDotBaseColor, stackDotIntensity) {
                                                        val base = Color(stackDotBaseColor)
                                                        val factor = (stackDotIntensity / 100f).coerceIn(0f, 1f)
                                                        Color(
                                                            red = base.red * factor,
                                                            green = base.green * factor,
                                                            blue = base.blue * factor,
                                                            alpha = base.alpha
                                                        )
                                                    }
                                                    val stackDotSize = (screenWidthDp * 0.02f * getScrollbarWidthPercent(context) / 100f).dp

                                                    // Show card chrome (background, outline, dots) while swiping, dragging a widget, + linger after
                                                    val isStackSwiping = stackPagerState.isScrollInProgress
                                                    val showChromeDuringDrag = isWidgetBeingDragged && !isThisWidgetDragging
                                                    val showChromeDuringMenu = showWidgetMenu && widget.stackId != null
                                                    // Linger only applies to swipe, not drag
                                                    var isSwipeLingering by remember { mutableStateOf(false) }
                                                    LaunchedEffect(isStackSwiping) {
                                                        if (isStackSwiping) {
                                                            isSwipeLingering = true
                                                        } else if (isSwipeLingering) {
                                                            delay(1000)
                                                            isSwipeLingering = false
                                                        }
                                                    }
                                                    val showStackChrome = showChromeDuringDrag || showChromeDuringMenu || isStackSwiping || isSwipeLingering
                                                    // Different animation speeds: fast when dragging a widget, smooth when swiping stack
                                                    val chromeDuration = if (showChromeDuringDrag) {
                                                        100 // Fast appear/disappear when holding another widget
                                                    } else if (showStackChrome) {
                                                        120 // Appear when swiping
                                                    } else {
                                                        400 // Disappear after swiping
                                                    }
                                                    val stackChromeAlpha by animateFloatAsState(
                                                        targetValue = if (showStackChrome) 1f else 0f,
                                                        animationSpec = tween(
                                                            durationMillis = chromeDuration,
                                                            easing = FastOutSlowInEasing
                                                        ),
                                                        label = "stackChrome"
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .pointerInput(Unit) {
                                                                awaitEachGesture {
                                                                    awaitFirstDown(requireUnconsumed = false)
                                                                    isStackSwipeActive = true
                                                                    try {
                                                                        while (true) {
                                                                            val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                                                            if (event.changes.all { !it.pressed }) break
                                                                        }
                                                                    } finally {
                                                                        isStackSwipeActive = false
                                                                    }
                                                                }
                                                            }
                                                            .clip(RoundedCornerShape(effectiveCornerRadiusDp.dp))
                                                            .then(
                                                                if (stackChromeAlpha > 0f) Modifier
                                                                    .background(Color.Black.copy(alpha = 0.4f * stackChromeAlpha))
                                                                    .border(1.dp, Color(0xFF888888).copy(alpha = stackChromeAlpha), RoundedCornerShape(effectiveCornerRadiusDp.dp))
                                                                else Modifier
                                                            )
                                                    ) {
                                                        HorizontalPager(
                                                            state = stackPagerState,
                                                            modifier = Modifier.fillMaxSize(),
                                                            userScrollEnabled = !isThisWidgetDragging
                                                        ) { stackPage ->
                                                            WidgetHostView(
                                                                placedWidget = stackWidgets[stackPage],
                                                                modifier = Modifier.fillMaxSize(),
                                                                cornerRadiusDp = effectiveCornerRadiusDp,
                                                                viewRefreshKey = widgetViewRefreshKeys[stackWidgets[stackPage].appWidgetId] ?: 0,
                                                                onLongPress = {},
                                                                onRemove = {
                                                                    WidgetManager.removePlacedWidget(context, stackWidgets[stackPage].appWidgetId)
                                                                    placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                                                }
                                                            )
                                                        }
                                                        // Page dots (only visible while swiping)
                                                        if (stackChromeAlpha > 0f) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomCenter)
                                                                    .padding(bottom = 4.dp)
                                                                    .graphicsLayer { alpha = stackChromeAlpha },
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                repeat(stackWidgets.size) { dotIndex ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(stackDotSize)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                if (stackPagerState.currentPage == dotIndex)
                                                                                    stackDotColor.copy(alpha = 0.9f)
                                                                                else stackDotColor.copy(alpha = 0.3f)
                                                                            )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    // Single widget (no stack)
                                                    WidgetHostView(
                                                        placedWidget = widget,
                                                        modifier = Modifier.fillMaxSize(),
                                                        cornerRadiusDp = effectiveCornerRadiusDp,
                                                        viewRefreshKey = widgetViewRefreshKeys[widget.appWidgetId] ?: 0,
                                                        onLongPress = {},
                                                        onRemove = {
                                                            WidgetManager.removePlacedWidget(context, widget.appWidgetId)
                                                            placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                                        }
                                                    )
                                                }
                                            }

                                            // Widget context menu (shown on long-press, hidden if user drags)
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

                                                            // Resize option (always shown, greyed out if widget doesn't support resizing)
                                                            val isResizable = canWidgetResize(context, widget)
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        text = if (isResizable) "Resize" else "Can't resize",
                                                                        color = if (isResizable) LocalContentColor.current
                                                                            else LocalContentColor.current.copy(alpha = 0.38f)
                                                                    )
                                                                },
                                                                onClick = {
                                                                    if (isResizable) {
                                                                        showWidgetMenu = false
                                                                        // Populate original cells so they stay blue during resize
                                                                        val originalCells = mutableSetOf<Int>()
                                                                        for (r in 0 until widget.spanRows) {
                                                                            for (c in 0 until widget.spanColumns) {
                                                                                originalCells.add((widget.gridRow + r) * gridColumns + (widget.gridColumn + c))
                                                                            }
                                                                        }
                                                                        widgetOriginalCells = originalCells
                                                                        widgetResizeState = WidgetResizeState(
                                                                            isResizing = true,
                                                                            resizingWidget = widget
                                                                        )
                                                                    }
                                                                },
                                                                enabled = isResizable,
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.AspectRatio,
                                                                        contentDescription = null,
                                                                        tint = if (isResizable) LocalContentColor.current
                                                                            else LocalContentColor.current.copy(alpha = 0.38f)
                                                                    )
                                                                }
                                                            )

                                                            // Per-widget text size slider
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 16.dp)
                                                                    .padding(bottom = 4.dp)
                                                            ) {
                                                                ThumbDragHorizontalSlider(
                                                                    currentValue = (widget.fontScalePercent ?: globalWidgetFontScalePercent).toFloat(),
                                                                    config = SliderConfigs.widgetTextSize,
                                                                    onValueChange = { newVal ->
                                                                        val newPercent = newVal.roundToInt()
                                                                        val perWidgetValue = if (newPercent == globalWidgetFontScalePercent) null else newPercent
                                                                        placedWidgets = placedWidgets.map {
                                                                            if (it.appWidgetId == widget.appWidgetId) {
                                                                                it.copy(fontScalePercent = perWidgetValue)
                                                                            } else it
                                                                        }
                                                                        WidgetManager.savePlacedWidgets(context, placedWidgets)
                                                                    },
                                                                    onValueChangeFinished = {
                                                                        // Recreate this widget's view so the new font scale takes effect,
                                                                        // then bump the refresh key so WidgetHostView re-fetches it.
                                                                        WidgetManager.recreateWidgetView(context, widget.appWidgetId)
                                                                        widgetViewRefreshKeys = widgetViewRefreshKeys +
                                                                            (widget.appWidgetId to ((widgetViewRefreshKeys[widget.appWidgetId] ?: 0) + 1))
                                                                    },
                                                                    onDoubleTap = {
                                                                        placedWidgets = placedWidgets.map {
                                                                            if (it.appWidgetId == widget.appWidgetId) {
                                                                                it.copy(fontScalePercent = null)
                                                                            } else it
                                                                        }
                                                                        WidgetManager.savePlacedWidgets(context, placedWidgets)
                                                                        WidgetManager.recreateWidgetView(context, widget.appWidgetId)
                                                                        widgetViewRefreshKeys = widgetViewRefreshKeys +
                                                                            (widget.appWidgetId to ((widgetViewRefreshKeys[widget.appWidgetId] ?: 0) + 1))
                                                                    }
                                                                )
                                                            }

                                                            // Per-widget padding slider
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 16.dp)
                                                                    .padding(bottom = 4.dp)
                                                            ) {
                                                                ThumbDragHorizontalSlider(
                                                                    currentValue = (widget.paddingPercent ?: globalWidgetPaddingPercent).toFloat(),
                                                                    config = SliderConfigs.widgetPadding,
                                                                    onValueChange = { newVal ->
                                                                        val newPercent = newVal.roundToInt()
                                                                        val perWidgetValue = if (newPercent == globalWidgetPaddingPercent) null else newPercent
                                                                        placedWidgets = placedWidgets.map {
                                                                            if (it.appWidgetId == widget.appWidgetId) {
                                                                                it.copy(paddingPercent = perWidgetValue)
                                                                            } else it
                                                                        }
                                                                        WidgetManager.savePlacedWidgets(context, placedWidgets)
                                                                    },
                                                                    onValueChangeFinished = { },
                                                                    onDoubleTap = {
                                                                        placedWidgets = placedWidgets.map {
                                                                            if (it.appWidgetId == widget.appWidgetId) {
                                                                                it.copy(paddingPercent = null)
                                                                            } else it
                                                                        }
                                                                        WidgetManager.savePlacedWidgets(context, placedWidgets)
                                                                    }
                                                                )
                                                            }

                                                            // Remove option
                                                            DropdownMenuItem(
                                                                text = { Text(if (widget.stackId != null) "Remove from stack" else "Remove widget") },
                                                                onClick = {
                                                                    showWidgetMenu = false
                                                                    if (widget.stackId != null) {
                                                                        // Remove the currently visible widget in the stack
                                                                        val visibleStackWidgets = WidgetManager.getStackWidgets(placedWidgets, widget.stackId!!)
                                                                        val visibleWidget = visibleStackWidgets.getOrNull(currentStackPage) ?: widget
                                                                        placedWidgets = WidgetManager.removeFromStack(context, visibleWidget.appWidgetId)
                                                                    } else {
                                                                        WidgetManager.removePlacedWidget(context, widget.appWidgetId)
                                                                        placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                                                    }
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = null
                                                                    )
                                                                }
                                                            )

                                                            // Delete entire stack option (only for stacked widgets)
                                                            if (widget.stackId != null) {
                                                                DropdownMenuItem(
                                                                    text = { Text("Delete stack") },
                                                                    onClick = {
                                                                        showWidgetMenu = false
                                                                        val stackWidgetsToRemove = WidgetManager.getStackWidgets(placedWidgets, widget.stackId!!)
                                                                        stackWidgetsToRemove.forEach { sw ->
                                                                            WidgetManager.removePlacedWidget(context, sw.appWidgetId)
                                                                        }
                                                                        placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                                                    },
                                                                    leadingIcon = {
                                                                        // Three small trashcans arranged in a triangle.
                                                                        // Total footprint matches a standard 24dp icon so it
                                                                        // visually balances "Remove from stack" above it.
                                                                        Box(modifier = Modifier.size(24.dp)) {
                                                                            val small = 13.dp
                                                                            Icon(
                                                                                imageVector = Icons.Outlined.Delete,
                                                                                contentDescription = null,
                                                                                modifier = Modifier
                                                                                    .size(small)
                                                                                    .align(Alignment.TopCenter)
                                                                            )
                                                                            Icon(
                                                                                imageVector = Icons.Outlined.Delete,
                                                                                contentDescription = null,
                                                                                modifier = Modifier
                                                                                    .size(small)
                                                                                    .align(Alignment.BottomStart)
                                                                            )
                                                                            Icon(
                                                                                imageVector = Icons.Outlined.Delete,
                                                                                contentDescription = null,
                                                                                modifier = Modifier
                                                                                    .size(small)
                                                                                    .align(Alignment.BottomEnd)
                                                                            )
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                }
                                        }
                                    }
                                }
                                    } // key
                            }

                            // Widget resize overlay - shows preview outline with draggable handles
                            // Only render on the page where the widget lives
                            if (page == (widgetResizeState.resizingWidget?.page ?: 0) && widgetResizeState.isResizing && widgetResizeState.resizingWidget != null) {
                                val resizingWidget = widgetResizeState.resizingWidget!!

                                // Calculate occupied cells as (column, row) pairs (excluding the resizing widget's cells)
                                val occupiedCellPairs = remember(gridCells, resizingWidget) {
                                    val cells = mutableSetOf<Pair<Int, Int>>()
                                    gridCells.forEachIndexed { index, cell ->
                                        val col = index % gridColumns
                                        val row = index / gridColumns
                                        when (cell) {
                                            is HomeGridCell.App -> cells.add(Pair(col, row))
                                            is HomeGridCell.Folder -> cells.add(Pair(col, row))
                                            is HomeGridCell.Widget -> {
                                                // Only add if it's a different widget
                                                if (cell.placedWidget.appWidgetId != resizingWidget.appWidgetId) {
                                                    cells.add(Pair(col, row))
                                                }
                                            }
                                            is HomeGridCell.WidgetSpan -> {
                                                // Only add if it belongs to a different widget
                                                val originCell = gridCells.getOrNull(cell.originPosition)
                                                val parentWidgetId = (originCell as? HomeGridCell.Widget)?.placedWidget?.appWidgetId
                                                if (parentWidgetId != resizingWidget.appWidgetId) {
                                                    cells.add(Pair(col, row))
                                                }
                                            }
                                            is HomeGridCell.Empty -> { /* Not occupied */ }
                                        }
                                    }
                                    cells.toSet()
                                }

                                WidgetResizeOverlay(
                                    widget = resizingWidget,
                                    cellWidth = cellSize.width,
                                    cellHeight = cellSize.height,
                                    gridColumns = gridColumns,
                                    gridRows = gridRows,
                                    occupiedCells = occupiedCellPairs,
                                    onResizeCellsChanged = { cells ->
                                        // Update hoveredWidgetCells to show grey indicators
                                        hoveredWidgetCells = cells
                                    },
                                    onResizeDimensionsChanged = { dims ->
                                        // Update visual resize dimensions (widget will re-render at new size)
                                        currentResizeDimensions = dims
                                    },
                                    onResizeValidityChanged = { valid ->
                                        // Update validity for indicator colors (red = invalid, grey = valid)
                                        isWidgetDropTargetValid = valid
                                    },
                                    onResizeComplete = { newColumn, newRow, newColumnSpan, newRowSpan ->
                                        // Final update and exit resize mode
                                        WidgetManager.updateWidget(
                                            context,
                                            resizingWidget.appWidgetId,
                                            newColumn,
                                            newRow,
                                            newColumnSpan,
                                            newRowSpan
                                        )
                                        placedWidgets = WidgetManager.loadPlacedWidgets(context)
                                        hoveredWidgetCells = emptySet()
                                        widgetOriginalCells = emptySet()
                                        currentResizeDimensions = null
                                        isWidgetDropTargetValid = true  // Reset validity
                                        // Exit resize mode
                                        widgetResizeState = WidgetResizeState()
                                    },
                                    onResizeCancel = {
                                        // Exit resize mode without saving
                                        hoveredWidgetCells = emptySet()
                                        widgetOriginalCells = emptySet()
                                        currentResizeDimensions = null
                                        isWidgetDropTargetValid = true  // Reset validity
                                        widgetResizeState = WidgetResizeState()
                                    }
                                )
                            }
                        } // End of inner Box (contains grid + widget overlay)
                    } // End of padded Column
                    } // End of HorizontalPager
                } // End of else (not loading)
            } // End of weight(1f) Box

            // Page indicator dots (home page = rounded triangle, others = circle)
            // Uses scrollbar personalization settings for size, color, intensity
            val navDotSize = (screenWidthDp * 0.02f * getScrollbarWidthPercent(context) / 100f).dp
            val navDotBaseColor = getScrollbarColor(context)
            val navDotIntensity = getScrollbarIntensity(context)
            val navDotColor = remember(navDotBaseColor, navDotIntensity) {
                val base = Color(navDotBaseColor)
                val factor = (navDotIntensity / 100f).coerceIn(0f, 1f)
                Color(
                    red = base.red * factor,
                    green = base.green * factor,
                    blue = base.blue * factor,
                    alpha = base.alpha
                )
            }
            // Equilateral triangle height = dot diameter + 10% (canvas slightly wider to fit)
            val triangleSize = navDotSize * 2f / 1.732f * 1.1f
            // Fixed height container so dot size changes don't shift the grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxOf(triangleSize, navDotSize) + 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalPages) { page ->
                        val isActive = page == currentPage
                        val dotColor = if (isActive) navDotColor.copy(alpha = 0.9f)
                            else navDotColor.copy(alpha = 0.3f)
                        // Animate last dot in/out for smooth add/remove
                        val isLastDotAnimating = (removingLastDot || addingLastDot) && page == totalPages - 1
                        val dotVisible = !(removingLastDot && page == totalPages - 1)
                        val dotProgress by animateFloatAsState(
                            targetValue = if (dotVisible) 1f else 0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "dotProgress"
                        )
                        // Full slot width = element size + 8dp horizontal padding
                        val fullWidth = if (page == 0) (triangleSize + 8.dp) else (navDotSize + 8.dp)

                        if (dotProgress > 0f || isLastDotAnimating) {
                            Box(
                                modifier = Modifier
                                    .width(fullWidth * dotProgress)
                                    .graphicsLayer { alpha = dotProgress; clip = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (page == 0) {
                                    // Home page: equilateral rounded triangle, full-width base = dot diameter
                                    Canvas(modifier = Modifier.size(triangleSize)) {
                                        val w = size.width
                                        val h = size.height
                                        // Equilateral: base = full width, height = w * √3/2, nudged slightly up
                                        val triH = w * 0.866f
                                        val topY = (h - triH) / 2f - h * 0.05f
                                        val top = Offset(w / 2f, topY)
                                        val bl = Offset(0f, topY + triH)
                                        val br = Offset(w, topY + triH)
                                        val r = 0.38f
                                        fun lerp(a: Offset, b: Offset, t: Float) =
                                            Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                                        val p1 = lerp(top, bl, r)
                                        val p2 = lerp(top, bl, 1f - r)
                                        val p3 = lerp(bl, br, r)
                                        val p4 = lerp(bl, br, 1f - r)
                                        val p5 = lerp(br, top, r)
                                        val p6 = lerp(br, top, 1f - r)
                                        val path = Path().apply {
                                            moveTo(p1.x, p1.y)
                                            lineTo(p2.x, p2.y)
                                            quadraticBezierTo(bl.x, bl.y, p3.x, p3.y)
                                            lineTo(p4.x, p4.y)
                                            quadraticBezierTo(br.x, br.y, p5.x, p5.y)
                                            lineTo(p6.x, p6.y)
                                            quadraticBezierTo(top.x, top.y, p1.x, p1.y)
                                            close()
                                        }
                                        drawPath(path, dotColor)
                                    }
                                } else {
                                    // Other pages: circle dot
                                    Box(
                                        modifier = Modifier
                                            .size(navDotSize)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dock bar at bottom
            if (isDockEnabled) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gridHPadding, vertical = gridVPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(dockSlots) { slot ->
                    val dockApp = dockApps.find { it.position == slot }
                    val appInfo = dockApp?.let { da ->
                        allAvailableApps.find { it.packageName == da.packageName }?.let { info ->
                            val cust = appCustomizations.customizations[da.packageName]
                            if (cust != null) info.copy(customization = cust) else info
                        }
                    }
                    val dockFolder = dockFolders.find { it.position == slot }
                    val dockFolderPreviewApps = dockFolder?.appPackageNames?.filter { it.isNotEmpty() && it !in hiddenApps }?.take(4)?.mapNotNull { pkg ->
                        allAvailableApps.find { it.packageName == pkg }
                    } ?: emptyList()
                    val slotOccupied = appInfo != null || dockFolder != null
                    val isDockSlotDragging = draggedFromDock && draggedItemIndex == slot
                    val isSlotDropTarget = hoveredDockSlot == slot && (draggedItemIndex != null || externalDragActive) &&
                        !(draggedFromDock && draggedItemIndex == slot) // Don't highlight self as drop target
                    // isSlotHovered includes original slot - shows hover indicator when dragging back over it
                    val isSlotHovered = hoveredDockSlot == slot && (draggedItemIndex != null || externalDragActive)
                    // Valid drop target: original position OR empty slot
                    // Allow dropping single apps on folders/apps (to add to folder or create folder)
                    // Works for internal drags (grid→dock, dock→dock) and external drags (drawer→dock)
                    val isDraggingSingleApp = draggedFolderData == null && externalDragItemState !is com.bearinmind.launcher314.data.AppFolder
                    val canDropOnOccupied = isDraggingSingleApp && slotOccupied && !isDockSlotDragging
                    val isSlotValidDropTarget = isDockSlotDragging || !slotOccupied || canDropOnOccupied

                    // Check if a dragged widget overlaps this dock slot (widgets can't be placed on dock)
                    val isWidgetOverDockSlot = isWidgetBeingDragged && !isWidgetDropAnimating && run {
                        val slotPos = dockPositions[slot] ?: return@run false
                        val wLeft = widgetDragScreenPos.x + widgetDragState.dragOffset.x
                        val wTop = widgetDragScreenPos.y + widgetDragState.dragOffset.y
                        val wRight = wLeft + widgetDragSizePx.first
                        val wBottom = wTop + widgetDragSizePx.second
                        val sRight = slotPos.x + dockSlotSize.width
                        val sBottom = slotPos.y + dockSlotSize.height
                        wLeft < sRight && wRight > slotPos.x && wTop < sBottom && wBottom > slotPos.y
                    }

                    val isDockSlotRemoving = removeState.dockSlot == slot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { coordinates ->
                                dockPositions = dockPositions + (slot to coordinates.positionInRoot())
                                dockSlotSize = coordinates.size
                            }
                            .graphicsLayer {
                                if (isDockSlotRemoving) {
                                    alpha = removeState.anim.value
                                    scaleX = removeState.anim.value
                                    scaleY = removeState.anim.value
                                }
                            }
                    ) {
                        DockSlot(
                            appInfo = if (dockFolder == null) appInfo else null,
                            slotIndex = slot,
                            totalSlots = dockSlots,
                            iconSize = iconSizeDp,
                            isEditMode = isEditMode,
                            isDragging = isDockSlotDragging,
                            // Lambda to check ownership dynamically (evaluated at call time, not capture time)
                            checkIsDragOwner = { draggedFromDock && draggedItemIndex == slot },
                            isDropTarget = isSlotDropTarget,
                            isHovered = isSlotHovered || isWidgetOverDockSlot,
                            isValidDropTarget = if (isWidgetOverDockSlot) false else isSlotValidDropTarget,
                            // When this slot is being dragged, is the hover target valid? (for icon tint)
                            isHoverTargetValid = if (isDockSlotDragging) {
                                when {
                                    hoveredGridCell != null -> isHoveredCellValid
                                    hoveredDockSlot != null -> isHoveredDockSlotValid
                                    else -> true
                                }
                            } else true,
                            dragOffset = if (isDockSlotDragging) dragOffset else Offset.Zero,
                            folderData = dockFolder,
                            folderPreviewApps = dockFolderPreviewApps,
                            folderPreviewDraggedIconPath = if (dockFolder != null && hoveredDockSlot == slot &&
                                draggedFolderData == null && (draggedItemIndex != null || externalDragActive || dragFromFolderApp != null)
                            ) draggedAppInfo?.iconPath else null,
                            isReceivingDrop = folderReceiveDockSlot == slot,
                            // Proportional sizing
                            markerHalfSizeParam = dockMarkerHalfSize,
                            hoverCornerRadius = dockHoverCornerRadius,
                            onTap = {
                                if (dockFolder != null) {
                                    // Open dock folder using the existing folder overlay system
                                    openHomeFolder = HomeFolder(
                                        id = dockFolder.id,
                                        name = dockFolder.name,
                                        position = -1, // Not a grid position
                                        page = -1,
                                        appPackageNames = dockFolder.appPackageNames
                                    )
                                } else if (appInfo != null) {
                                    launchApp(context, appInfo.packageName)
                                }
                            },
                            onLongPress = { },
                            onDragStart = {
                                // Only start drag if not already dragging something else
                                if (draggedItemIndex == null && !isDropAnimating) {
                                    if (dockFolder != null) {
                                        // Start dragging a dock folder
                                        isEditMode = true
                                        draggedItemIndex = slot
                                        draggedFromDock = true
                                        draggedAppInfo = dockFolderPreviewApps.firstOrNull()
                                        draggedFolderData = HomeFolder(
                                            id = dockFolder.id,
                                            name = dockFolder.name,
                                            position = dockFolder.position,
                                            page = -1,
                                            appPackageNames = dockFolder.appPackageNames
                                        )
                                        draggedFolderPreviewApps = dockFolderPreviewApps
                                    } else if (appInfo != null) {
                                        isEditMode = true
                                        draggedItemIndex = slot
                                        draggedFromDock = true
                                        draggedAppInfo = appInfo
                                    }
                                }
                            },
                            onDrag = { offset ->
                                dragOffset += offset
                                draggedItemPosition = dockPositions[slot]?.plus(dragOffset) ?: Offset.Zero
                                // Check if hovering over grid or dock
                                val centerPos = draggedItemPosition + Offset(dockSlotSize.width / 2f, dockSlotSize.height / 2f)
                                val targetGridCell = findCellIndex(centerPos)
                                val targetDockSlot = findDockSlotIndex(centerPos)

                                hoveredGridCell = targetGridCell
                                // Include original slot in hoveredDockSlot so it shows hover indicator
                                // (isSlotDropTarget already excludes self for drop logic)
                                hoveredDockSlot = targetDockSlot

                                val isDraggingFolder = draggedFolderData != null

                                // Check if hover target is valid (for icon red tint)
                                if (targetGridCell != null) {
                                    // Hovering over grid - check if cell is empty, app (folder creation), or folder
                                    val targetCell = gridCells.getOrNull(targetGridCell)
                                    isHoveredCellValid = if (isDraggingFolder) {
                                        targetCell is HomeGridCell.Empty
                                    } else {
                                        targetCell is HomeGridCell.Empty ||
                                            targetCell is HomeGridCell.App ||
                                            targetCell is HomeGridCell.Folder
                                    }
                                    showFolderCreationIndicator = !isDraggingFolder && targetCell is HomeGridCell.App
                                    isHoveredDockSlotValid = true // Not hovering dock
                                } else if (targetDockSlot != null) {
                                    // Hovering over dock - check if slot is valid
                                    val dockSlotApp = dockApps.find { it.position == targetDockSlot }
                                    val dockSlotFolder = dockFolders.find { it.position == targetDockSlot }
                                    val dockSlotEmpty = dockSlotApp == null && dockSlotFolder == null
                                    isHoveredDockSlotValid = targetDockSlot == slot || dockSlotEmpty ||
                                        (!isDraggingFolder && (dockSlotFolder != null || dockSlotApp != null))
                                    isHoveredCellValid = true // Not hovering grid
                                    showFolderCreationIndicator = !isDraggingFolder && dockSlotApp != null && dockSlotFolder == null && targetDockSlot != slot
                                } else {
                                    isHoveredCellValid = true
                                    isHoveredDockSlotValid = true
                                    showFolderCreationIndicator = false
                                }
                            },
                            onDragEnd = {
                                val centerPos = draggedItemPosition + Offset(dockSlotSize.width / 2f, dockSlotSize.height / 2f)
                                val targetGridIndex = findCellIndex(centerPos)
                                val targetDockSlot = findDockSlotIndex(centerPos)
                                val currentDraggedSlot = draggedItemIndex
                                val originalDockPos = if (currentDraggedSlot != null) dockPositions[currentDraggedSlot] else null

                                // Clear hover states immediately (fixes dock red tint flash)
                                hoveredDockSlot = null
                                hoveredGridCell = null
                                isHoveredCellValid = true
                                isHoveredDockSlotValid = true

                                if (currentDraggedSlot != null && originalDockPos != null && !isDropAnimating) {
                                    // Calculate target offset and drop action
                                    val targetOffset: Offset
                                    var dropAction: (() -> Unit)? = null
                                    val isDraggingDockFolder = dockFolder != null

                                    if (targetGridIndex != null) {
                                        val targetCell = gridCells.getOrNull(targetGridIndex)
                                        val isValid = if (isDraggingDockFolder) {
                                            targetCell is HomeGridCell.Empty
                                        } else {
                                            targetCell is HomeGridCell.Empty ||
                                                targetCell is HomeGridCell.App ||
                                                targetCell is HomeGridCell.Folder
                                        }
                                        val targetPos = if (isValid) cellPositions[targetGridIndex] else null
                                        targetOffset = if (targetPos != null) {
                                            Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                        } else Offset.Zero
                                        if (isValid) {
                                            if (isDraggingDockFolder && dockFolder != null) {
                                                // Dock folder → grid: convert to HomeFolder
                                                dropAction = {
                                                    val newHomeFolder = HomeFolder(
                                                        id = dockFolder.id,
                                                        name = dockFolder.name,
                                                        position = targetGridIndex,
                                                        page = currentPage,
                                                        appPackageNames = dockFolder.appPackageNames
                                                    )
                                                    homeFolders = homeFolders + newHomeFolder
                                                    dockFolders = dockFolders.filter { it.id != dockFolder.id }
                                                    saveAllData()
                                                }
                                            } else {
                                                dropAction = { handleDropFromDockToGrid(currentDraggedSlot, targetGridIndex) }
                                            }
                                        }
                                    } else if (targetDockSlot != null && targetDockSlot != currentDraggedSlot) {
                                        val fromApp = dockApps.find { it.position == currentDraggedSlot }
                                        val toApp = dockApps.find { it.position == targetDockSlot }
                                        val toFolder = dockFolders.find { it.position == targetDockSlot }
                                        val slotEmpty = toApp == null && toFolder == null

                                        if (isDraggingDockFolder && dockFolder != null && slotEmpty) {
                                            // Move dock folder to another dock slot
                                            val targetPos = dockPositions[targetDockSlot]
                                            targetOffset = if (targetPos != null) {
                                                Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                            } else Offset.Zero
                                            dropAction = {
                                                dockFolders = dockFolders.map { f ->
                                                    if (f.id == dockFolder.id) f.copy(position = targetDockSlot)
                                                    else f
                                                }
                                                saveAllData()
                                            }
                                        } else if (fromApp != null && slotEmpty) {
                                            // Move dock app to empty slot
                                            val targetPos = dockPositions[targetDockSlot]
                                            targetOffset = if (targetPos != null) {
                                                Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                            } else Offset.Zero
                                            dropAction = {
                                                val updatedDockApps = dockApps.toMutableList()
                                                updatedDockApps.removeAll { it.position == currentDraggedSlot }
                                                updatedDockApps.add(DockApp(fromApp.packageName, targetDockSlot))
                                                saveDockApps(updatedDockApps)
                                            }
                                        } else if (fromApp != null && toApp != null) {
                                            // Dock app on dock app → create dock folder
                                            val targetPos = dockPositions[targetDockSlot]
                                            targetOffset = if (targetPos != null) {
                                                Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                            } else Offset.Zero
                                            dropAction = {
                                                val updatedDockApps = dockApps.filter {
                                                    it.position != currentDraggedSlot && it.position != targetDockSlot
                                                }
                                                val newDockFolder = DockFolder(
                                                    name = "Folder",
                                                    position = targetDockSlot,
                                                    appPackageNames = listOf(toApp.packageName, fromApp.packageName)
                                                )
                                                dockApps = updatedDockApps
                                                dockFolders = dockFolders + newDockFolder
                                                saveAllData()
                                            }
                                        } else if (fromApp != null && toFolder != null) {
                                            // Dock app on dock folder → add to folder
                                            val targetPos = dockPositions[targetDockSlot]
                                            targetOffset = if (targetPos != null) {
                                                Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                            } else Offset.Zero
                                            dropAction = {
                                                val updatedDockApps = dockApps.filter { it.position != currentDraggedSlot }
                                                dockApps = updatedDockApps
                                                dockFolders = dockFolders.map { f ->
                                                    if (f.id == toFolder.id) f.copy(appPackageNames = addAppToFolder(f.appPackageNames, fromApp.packageName))
                                                    else f
                                                }
                                                saveAllData()
                                            }
                                        } else {
                                            targetOffset = Offset.Zero
                                        }
                                    } else {
                                        targetOffset = Offset.Zero
                                    }

                                    // Track destination type for text visibility
                                    // Dock source: true = going to dock (no text), false = going to grid (text fades in)
                                    dropTargetIsDock = !(targetGridIndex != null && dropAction != null)

                                    // Start folder receive pulse on dock target if dropping on folder/app
                                    if (dropAction != null && !isDraggingDockFolder && targetDockSlot != null) {
                                        val targetHasContent = dockApps.any { it.position == targetDockSlot } ||
                                            dockFolders.any { it.position == targetDockSlot }
                                        if (targetHasContent) folderReceiveDockSlot = targetDockSlot
                                    }

                                    // Animate overlay to target, then execute drop atomically
                                    dropStartOffset = dragOffset
                                    dropTargetOffset = targetOffset
                                    isDropAnimating = true
                                    val capturedAction = dropAction
                                    dropScope.launch {
                                        dropAnimProgress.snapTo(0f)
                                        dropAnimProgress.animateTo(
                                            1f,
                                            tween(
                                                durationMillis = 400,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        // All state changes in same frame: drop + overlay removal = seamless
                                        capturedAction?.invoke()
                                        folderReceiveDockSlot = null
                                        draggedItemIndex = null
                                        dragOffset = Offset.Zero
                                        draggedFromDock = false
                                        draggedAppInfo = null
                                        draggedFolderData = null
                                        draggedFolderPreviewApps = emptyList()
                                        isDropAnimating = false
                                        isEditMode = false
                                    }
                                } else {
                                    draggedItemIndex = null
                                    dragOffset = Offset.Zero
                                    draggedFromDock = false
                                    draggedAppInfo = null
                                    draggedFolderData = null
                                    draggedFolderPreviewApps = emptyList()
                                    isEditMode = false
                                }
                            },
                            onRemove = {
                                dropScope.launch {
                                    removeState.dockSlot = slot
                                    removeState.anim.snapTo(1f)
                                    removeState.anim.animateTo(
                                        0f,
                                        tween(300, easing = FastOutSlowInEasing)
                                    )
                                    if (dockFolder != null) {
                                        saveDockFolders(dockFolders.filter { it.id != dockFolder.id })
                                    } else if (appInfo != null) {
                                        saveDockApps(dockApps.filter { it.position != slot })
                                    }
                                    removeState.dockSlot = null
                                }
                            },
                            onUninstall = {
                                if (appInfo != null) {
                                    uninstallApp(context, appInfo.packageName)
                                }
                            },
                            onAppInfo = {
                                if (appInfo != null) {
                                    openAppInfo(context, appInfo.packageName)
                                }
                            },
                            onCustomize = {
                                if (dockFolder != null) {
                                    customizingDockFolder = dockFolder
                                } else if (appInfo != null) {
                                    customizingApp = appInfo
                                }
                            },
                            isCustomizing = appInfo != null && customizingApp?.packageName == appInfo.packageName,
                            globalIconSizePercent = iconSizePercent.toFloat(),
                            globalIconShape = globalIconShape,
                            globalIconBgColor = globalIconBgColor,
                            globalIconBgIntensity = globalIconBgIntensity
                        )
                    }
                }
            }
        }

        // ========== EDGE SCROLL INDICATORS ==========
        // Rounded rectangles on left/right edges — only visible when hovering in edge zone
        EdgeScrollIndicators(
            hoveringLeft = isHoveringLeftEdge && !edgeIndicatorSuppressed && pagerState.currentPage == pagerState.targetPage,
            hoveringRight = isHoveringRightEdge && !edgeIndicatorSuppressed && pagerState.currentPage == pagerState.targetPage,
            showLeft = pagerState.currentPage > 0,
            showRight = pagerState.currentPage < totalPages - 1,
            gridHPaddingPx = with(density) { gridHPadding.toPx() },
            screenWidthPx = screenWidthPx,
            modifier = Modifier.zIndex(500f)
        )

        // ========== WIDGET DRAG OVERLAY ==========
        // Rendered at root level so it draws above dock bar (HorizontalPager clips internally)
        // Shows during both active drag AND drop animation (overlay animates to target cell)
        // Show during active drag AND during drop animation (overlay stays visible
        // until composable has rendered underneath, preventing flash on transition)
        if (widgetDragBitmap != null && (isWidgetBeingDragged || isWidgetDropAnimating)) {
            val showOverlayRedTint = !isWidgetDropTargetValid && !isWidgetOverWidget
            val showOverlayBlueTint = isWidgetOverWidget

            // Check if dragged widget is part of a stack (look up from current placedWidgets)
            val draggedWidgetId = widgetDragState.draggedWidget?.appWidgetId ?: widgetDropWidgetId
            val draggedFromPlaced = placedWidgets.find { it.appWidgetId == draggedWidgetId }
            val isDraggedStack = draggedFromPlaced?.stackId != null
            val dragStackCount = if (isDraggedStack && draggedFromPlaced != null) {
                placedWidgets.count { it.stackId == draggedFromPlaced.stackId }
            } else 0

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { widgetDragSizePx.first.toDp() },
                        height = with(density) { widgetDragSizePx.second.toDp() }
                    )
                    .zIndex(1000f)
                    .graphicsLayer {
                        if (isWidgetDropAnimating) {
                            // Animate overlay from current drag position to target cell
                            val p = widgetDropAnim.value
                            translationX = widgetDragScreenPos.x + widgetDropStartOffset.x + (widgetDropTargetOffset.x - widgetDropStartOffset.x) * p
                            translationY = widgetDragScreenPos.y + widgetDropStartOffset.y + (widgetDropTargetOffset.y - widgetDropStartOffset.y) * p
                            scaleX = 1.1f + (1f - 1.1f) * p  // 1.1 → 1.0
                            scaleY = 1.1f + (1f - 1.1f) * p
                            alpha = 0.8f + (1f - 0.8f) * p    // 0.8 → 1.0
                        } else {
                            translationX = widgetDragScreenPos.x + widgetDragState.dragOffset.x
                            translationY = widgetDragScreenPos.y + widgetDragState.dragOffset.y
                            scaleX = 1.1f
                            scaleY = 1.1f
                            alpha = 0.8f
                        }
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        if (showOverlayRedTint) {
                            drawRect(
                                color = Color(0xFFFF6B6B).copy(alpha = 0.5f),
                                blendMode = BlendMode.SrcAtop
                            )
                        } else if (showOverlayBlueTint) {
                            drawRect(
                                color = Color(0xFF1A3D7A).copy(alpha = 0.7f),
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isDraggedStack) Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color(0xFF888888), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                ) {
                    Image(
                        bitmap = widgetDragBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Stack nav dots on the drag overlay
                    if (isDraggedStack && dragStackCount > 1) {
                        val overlayDotBaseColor = getScrollbarColor(context)
                        val overlayDotIntensity = getScrollbarIntensity(context)
                        val overlayDotColor = remember(overlayDotBaseColor, overlayDotIntensity) {
                            val base = Color(overlayDotBaseColor)
                            val factor = (overlayDotIntensity / 100f).coerceIn(0f, 1f)
                            Color(base.red * factor, base.green * factor, base.blue * factor, base.alpha)
                        }
                        val overlayDotSize = (screenWidthDp * 0.02f * getScrollbarWidthPercent(context) / 100f).dp

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(dragStackCount) { dotIndex ->
                                Box(
                                    modifier = Modifier
                                        .size(overlayDotSize)
                                        .clip(CircleShape)
                                        .background(
                                            if (dotIndex == 0) overlayDotColor.copy(alpha = 0.9f)
                                            else overlayDotColor.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ========== FOLDER DRAG-OUT OVERLAY ==========
        // When dragging an app out of a folder, show floating icon at drag position
        // Coordinates are root-relative (same as existing grid drag overlay)
        if (dragFromFolderApp != null && cellSize.width > 0) {
            val folderDragApp = dragFromFolderApp!!
            val originalPos = dragOriginalCellPos ?: Offset.Zero

            val p = if (isDropAnimating) dropAnimProgress.value else 0f
            val currentOffset = if (isDropAnimating) {
                dropStartOffset + (dropTargetOffset - dropStartOffset) * p
            } else dragOffset
            // Scale: 1.1 during drag → shrink into folder or settle to 1.0
            val boxScale = if (dropCreatesFolder && isDropAnimating) {
                1.1f * (1f - p * 0.6f) // Shrink into folder
            } else if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                1f // Return to origin — match cell scale to avoid transition flicker
            } else {
                1.1f - 0.1f * p // 1.1 during drag → 1.0 at drop end
            }
            val boxAlpha = if (dropCreatesFolder && isDropAnimating) {
                0.8f * (1f - p) // Fade out into folder
            } else if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                1f // Return to origin — constant alpha to avoid brightness flash
            } else {
                0.8f + 0.2f * p // 0.8 during drag → 1.0 at drop end
            }
            val textAlpha = if (isDropAnimating && !dropCreatesFolder) p else 0f

            val appLeft = originalPos.x + currentOffset.x
            val appTop = originalPos.y + currentOffset.y

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { cellSize.width.toDp() },
                        height = with(density) { cellSize.height.toDp() }
                    )
                    .zIndex(1000f)
                    .graphicsLayer {
                        translationX = appLeft
                        translationY = appTop
                        alpha = boxAlpha
                        scaleX = boxScale
                        scaleY = boxScale
                        clip = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(gridMarkerHalfSize),
                    contentAlignment = Alignment.Center
                ) {
                    OverlayAppContent(context, folderDragApp, iconSizeDp, iconSizePercent, gridIconTextSpacer, gridAppNameFont, selectedFontFamily, textAlpha, globalIconShape, showLabel = true, globalIconBgColor = globalIconBgColor)
                }
            }
        }

        // ========== DRAGGED APP OVERLAY ==========
        // Rendered at root level so it draws above dock bar, page dots, and everything else
        // Layout MUST match DraggableGridCell exactly (padding, always-present text)
        // to prevent visual jump when overlay is replaced by in-cell content
        if ((draggedItemIndex != null || externalDragActive) && !draggedFromDock && dragFromFolderApp == null && cellSize.width > 0) {
            val appInfo = draggedAppInfo
            val originalCellPos = dragOriginalCellPos ?: draggedItemIndex?.let { cellPositions[it] }

            if ((appInfo != null || draggedFolderData != null) && originalCellPos != null) {
                val p = if (isDropAnimating) dropAnimProgress.value else 0f
                val currentOffset = if (isDropAnimating) {
                    dropStartOffset + (dropTargetOffset - dropStartOffset) * p
                } else dragOffset
                // When creating a folder, shrink & fade the overlay into the folder preview
                val boxScale = if (dropCreatesFolder && isDropAnimating) {
                    1.265f * (1f - p * 0.6f) // Shrink into folder
                } else if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                    1f // Return to origin — match cell scale to avoid transition flicker
                } else {
                    1.265f - 0.265f * p // 1.265 during drag → 1.0 at drop end
                }
                val boxAlpha = if (dropCreatesFolder && isDropAnimating) {
                    0.8f * (1f - p) // Fade out into folder
                } else if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                    1f // Return to origin — constant alpha to avoid brightness flash
                } else {
                    0.8f + 0.2f * p
                }
                val textAlpha = if (isDropAnimating && !dropTargetIsDock && !dropCreatesFolder) p else 0f

                val targetW = if (isDropAnimating && dropTargetIsDock) dockSlotSize.width else cellSize.width
                val targetH = if (isDropAnimating && dropTargetIsDock) dockSlotSize.height else cellSize.height
                val boxW = cellSize.width + (targetW - cellSize.width) * p
                val boxH = cellSize.height + (targetH - cellSize.height) * p

                // dragOffset was corrected at the transition point (onDragEnd during page
                // scroll) so it represents true finger movement. No pager scroll compensation
                // needed here — the overlay is in the root Box, outside the pager.
                val appLeft = originalCellPos.x + currentOffset.x
                val appTop = originalCellPos.y + currentOffset.y

                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { boxW.toDp() },
                            height = with(density) { boxH.toDp() }
                        )
                        .zIndex(1000f)
                        .graphicsLayer {
                            translationX = appLeft
                            translationY = appTop
                            scaleX = boxScale
                            scaleY = boxScale
                            alpha = boxAlpha
                            clip = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Box with padding to match DraggableGridCell layout
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(gridMarkerHalfSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (draggedFolderData != null) {
                            // ---- Folder drag overlay: 2x2 preview grid ----
                            val folderName = draggedFolderData!!.name
                            // Red tint when hovering over an invalid target
                            val folderOverlayInvalid = when {
                                hoveredDockSlot != null -> !isHoveredDockSlotValid
                                hoveredGridCell != null -> !isHoveredCellValid
                                else -> false
                            }
                            val folderOverlayTint = if (folderOverlayInvalid) {
                                ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), BlendMode.SrcAtop)
                            } else null
                            Column(
                                modifier = Modifier
                                    .wrapContentHeight(unbounded = true)
                                    .graphicsLayer { clip = false },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val folderBoxSize = iconSizeDp.dp
                                val folderCornerRadius = (iconSizeDp * 0.29f).dp
                                val folderBoxBg = if (folderOverlayInvalid) Color(0xFF4A1A1A) else Color(0xFF1A1A1A)
                                Box(
                                    modifier = Modifier
                                        .size(folderBoxSize)
                                        .clip(getIconShape(globalIconShape) ?: RoundedCornerShape(folderCornerRadius))
                                        .background(folderBoxBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (draggedFolderPreviewApps.isNotEmpty()) {
                                        val padding = folderBoxSize * 0.08f
                                        val spacing = folderBoxSize * 0.04f
                                        val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2
                                        Column(
                                            modifier = Modifier.padding(padding),
                                            verticalArrangement = Arrangement.spacedBy(spacing)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                draggedFolderPreviewApps.getOrNull(0)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                                draggedFolderPreviewApps.getOrNull(1)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                draggedFolderPreviewApps.getOrNull(2)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                                draggedFolderPreviewApps.getOrNull(3)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                        }
                                    }
                                }
                                // Only show folder name text during drop animation (same as app text behavior)
                                if (!isDropAnimating || !dropTargetIsDock) {
                                    Spacer(modifier = Modifier.height(gridIconTextSpacer))
                                    Text(
                                        text = folderName,
                                        fontSize = gridAppNameFont,
                                        fontFamily = selectedFontFamily ?: FontFamily.Default,
                                        color = if (folderOverlayInvalid) Color(0xFFFF6B6B) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { alpha = textAlpha },
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
                        } else if (appInfo != null) {
                            // ---- Normal app drag overlay ----
                            OverlayAppContent(context, appInfo, iconSizeDp, iconSizePercent, gridIconTextSpacer, gridAppNameFont, selectedFontFamily, textAlpha, globalIconShape, showLabel = !isDropAnimating || !dropTargetIsDock, globalIconBgColor = globalIconBgColor)
                        }
                    }
                }
            }
        }

        // ========== DRAGGED DOCK APP/FOLDER OVERLAY ==========
        // Layout matches DockSlot structure for seamless overlay→in-cell transition
        if (draggedItemIndex != null && draggedFromDock && dockSlotSize.width > 0) {
            val appInfo = draggedAppInfo
            val originalDockPos = dockPositions[draggedItemIndex!!]

            if ((appInfo != null || draggedFolderData != null) && originalDockPos != null) {
                val p = if (isDropAnimating) dropAnimProgress.value else 0f
                val currentOffset = if (isDropAnimating) {
                    dropStartOffset + (dropTargetOffset - dropStartOffset) * p
                } else dragOffset
                val boxScale = if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                    1f // Return to origin — match cell scale to avoid transition flicker
                } else {
                    1.265f - 0.265f * p
                }
                val boxAlpha = if (isDropAnimating && dropTargetOffset == Offset.Zero) {
                    1f // Return to origin — constant alpha to avoid brightness flash
                } else {
                    0.8f + 0.2f * p
                }
                val textAlpha = if (isDropAnimating && !dropTargetIsDock) p else 0f

                val targetW = if (isDropAnimating && !dropTargetIsDock) cellSize.width else dockSlotSize.width
                val targetH = if (isDropAnimating && !dropTargetIsDock) cellSize.height else dockSlotSize.height
                val boxW = dockSlotSize.width + (targetW - dockSlotSize.width) * p
                val boxH = dockSlotSize.height + (targetH - dockSlotSize.height) * p

                // Use root-relative coordinates directly
                val appLeft = originalDockPos.x + currentOffset.x
                val appTop = originalDockPos.y + currentOffset.y

                // Use target marker size based on where the drop is landing
                val overlayMarkerHalfSize = if (isDropAnimating && !dropTargetIsDock) gridMarkerHalfSize else dockMarkerHalfSize

                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { boxW.toDp() },
                            height = with(density) { boxH.toDp() }
                        )
                        .zIndex(1000f)
                        .graphicsLayer {
                            translationX = appLeft
                            translationY = appTop
                            scaleX = boxScale
                            scaleY = boxScale
                            alpha = boxAlpha
                            clip = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Box with padding to match DockSlot/DraggableGridCell layout
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(overlayMarkerHalfSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (draggedFolderData != null) {
                            // ---- Dock folder drag overlay: 2x2 preview grid ----
                            val folderName = draggedFolderData!!.name
                            val folderOverlayInvalid = when {
                                hoveredDockSlot != null -> !isHoveredDockSlotValid
                                hoveredGridCell != null -> !isHoveredCellValid
                                else -> false
                            }
                            val folderOverlayTint = if (folderOverlayInvalid) {
                                ColorFilter.tint(Color(0xFFFF6B6B).copy(alpha = 0.6f), BlendMode.SrcAtop)
                            } else null
                            Column(
                                modifier = Modifier
                                    .wrapContentHeight(unbounded = true)
                                    .graphicsLayer { clip = false },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val folderBoxSize = iconSizeDp.dp
                                val folderCornerRadius = (iconSizeDp * 0.29f).dp
                                val folderBoxBg = if (folderOverlayInvalid) Color(0xFF4A1A1A) else Color(0xFF1A1A1A)
                                Box(
                                    modifier = Modifier
                                        .size(folderBoxSize)
                                        .clip(getIconShape(globalIconShape) ?: RoundedCornerShape(folderCornerRadius))
                                        .background(folderBoxBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (draggedFolderPreviewApps.isNotEmpty()) {
                                        val padding = folderBoxSize * 0.08f
                                        val spacing = folderBoxSize * 0.04f
                                        val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2
                                        Column(
                                            modifier = Modifier.padding(padding),
                                            verticalArrangement = Arrangement.spacedBy(spacing)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                draggedFolderPreviewApps.getOrNull(0)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                                draggedFolderPreviewApps.getOrNull(1)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                draggedFolderPreviewApps.getOrNull(2)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                                draggedFolderPreviewApps.getOrNull(3)?.let { app ->
                                                    AsyncImage(
                                                        model = File(app.iconPath),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Fit,
                                                        colorFilter = folderOverlayTint,
                                                        modifier = Modifier.size(miniIconSize).clip(RoundedCornerShape(miniIconSize * 0.2f))
                                                    )
                                                } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                            }
                                        }
                                    }
                                }
                                if (!isDropAnimating || !dropTargetIsDock) {
                                    Spacer(modifier = Modifier.height(gridIconTextSpacer))
                                    Text(
                                        text = folderName,
                                        fontSize = gridAppNameFont,
                                        fontFamily = selectedFontFamily ?: FontFamily.Default,
                                        color = if (folderOverlayInvalid) Color(0xFFFF6B6B) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { alpha = textAlpha },
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
                        } else if (appInfo != null) {
                            // ---- Normal dock app drag overlay ----
                            OverlayAppContent(context, appInfo, iconSizeDp, iconSizePercent, gridIconTextSpacer, gridAppNameFont, selectedFontFamily, textAlpha, globalIconShape, showLabel = !isDropAnimating || !dropTargetIsDock, globalIconBgColor = globalIconBgColor)
                        }
                    }
                }
            }
        }
    }

    // ========== Home Screen Folder Content Overlay (drawer-style full screen) ==========
    // Keep a reference to the last opened folder so content persists during close animation
    // ========== CREATE FOLDER DIALOG (from selection mode) ==========
    if (showCreateHomeFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateHomeFolderDialog = false },
            onCreate = { name ->
                val parts = pendingFolderCellKey.split("_")
                val folderPage = parts.getOrNull(0)?.toIntOrNull() ?: currentPage
                val folderPos = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val selectedPkgs = mutableListOf<String>()
                val updatedApps = homeApps.toMutableList()
                selectedHomeCells.forEach { cellKey ->
                    val p = cellKey.split("_")
                    if (p.size == 2) {
                        val pg = p[0].toIntOrNull() ?: return@forEach
                        val pos = p[1].toIntOrNull() ?: return@forEach
                        val app = updatedApps.find { it.page == pg && it.position == pos }
                        if (app != null) {
                            selectedPkgs.add(app.packageName)
                            updatedApps.removeAll { it.page == pg && it.position == pos }
                        }
                    }
                }
                if (selectedPkgs.isNotEmpty()) {
                    val newFolder = HomeFolder(
                        name = name,
                        position = folderPos,
                        page = folderPage,
                        appPackageNames = selectedPkgs
                    )
                    homeApps = updatedApps
                    homeFolders = homeFolders + newFolder
                    saveHomeScreenData(context, HomeScreenData(apps = updatedApps, dockApps = dockApps, folders = homeFolders, dockFolders = dockFolders))
                }
                selectedHomeCells = emptySet()
                homeSelectionModeActive = false
                showCreateHomeFolderDialog = false
            }
        )
    }

    // ========== APP CUSTOMIZE DIALOG ==========
    customizingApp?.let { app ->
        AppCustomizeDialog(
            context = context,
            appInfo = app,
            currentCustomization = appCustomizations.customizations[app.packageName],
            globalIconSizePercent = iconSizePercent.toFloat().toInt(),
            globalIconTextSizePercent = iconTextSizePercent,
            iconSizeOverflowThreshold = universalOverflowThreshold,
            globalIconShape = globalIconShape,
            globalIconBgColor = globalIconBgColor,
            onSave = { newCustomization ->
                appCustomizations = setCustomization(context, appCustomizations, app.packageName, newCustomization)
                iconCacheVersion++
                // Reload app list so iconPath re-resolves (picks up icon_pack_cache changes)
                dropScope.launch {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val freshApps = loadAvailableApps(context)
                        val debugApp = freshApps.find { it.packageName == app.packageName }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            allAvailableApps = freshApps
                            coil.Coil.imageLoader(context).memoryCache?.clear()
                            coil.Coil.imageLoader(context).diskCache?.clear()
                            android.widget.Toast.makeText(context, "iconPath: ${debugApp?.iconPath?.takeLast(50)}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
                customizingApp = null
            },
            onReset = {
                appCustomizations = removeCustomization(context, appCustomizations, app.packageName)
                dropScope.launch {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val freshApps = loadAvailableApps(context)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            allAvailableApps = freshApps
                            coil.Coil.imageLoader(context).memoryCache?.clear()
                            coil.Coil.imageLoader(context).diskCache?.clear()
                        }
                    }
                }
                customizingApp = null
            },
            onDismiss = { customizingApp = null }
        )
    }

    // ========== FOLDER CUSTOMIZE DIALOG (Home screen) ==========
    customizingFolder?.let { folder ->
        val folderKey = "folder_${folder.id}"
        val folderPreviewIcons = remember(folder.appPackageNames, allAvailableApps, globalIconShape, globalIconBgColor, globalIconBgIntensity) {
            folder.appPackageNames.filter { it.isNotEmpty() }.take(4).map { pkg ->
                resolveMiniIconPath(appContext, pkg, allAvailableApps.find { it.packageName == pkg }?.iconPath ?: "", globalIconShape, globalIconBgColor, globalIconBgIntensity)
            }
        }
        FolderCustomizeDialog(
            context = context,
            folderName = folder.name,
            folderId = folder.id,
            currentCustomization = appCustomizations.customizations[folderKey],
            globalIconSizePercent = iconSizePercent.toFloat().toInt(),
            globalIconTextSizePercent = iconTextSizePercent,
            iconSizeOverflowThreshold = universalOverflowThreshold,
            globalIconShape = globalIconShape,
            globalIconBgColor = globalIconBgColor,
            globalIconBgIntensity = globalIconBgIntensity,
            previewAppIcons = folderPreviewIcons,
            onSave = { newCustomization ->
                appCustomizations = setCustomization(context, appCustomizations, folderKey, newCustomization)
                customizingFolder = null
            },
            onReset = {
                appCustomizations = removeCustomization(context, appCustomizations, folderKey)
                customizingFolder = null
            },
            onDismiss = { customizingFolder = null }
        )
    }

    // ========== FOLDER CUSTOMIZE DIALOG (Dock) ==========
    customizingDockFolder?.let { folder ->
        val folderKey = "folder_${folder.id}"
        val dockFolderPreviewIcons = remember(folder.appPackageNames, allAvailableApps, globalIconShape, globalIconBgColor, globalIconBgIntensity) {
            folder.appPackageNames.filter { it.isNotEmpty() }.take(4).map { pkg ->
                resolveMiniIconPath(appContext, pkg, allAvailableApps.find { it.packageName == pkg }?.iconPath ?: "", globalIconShape, globalIconBgColor, globalIconBgIntensity)
            }
        }
        FolderCustomizeDialog(
            context = context,
            folderName = folder.name,
            folderId = folder.id,
            currentCustomization = appCustomizations.customizations[folderKey],
            globalIconSizePercent = iconSizePercent.toFloat().toInt(),
            globalIconTextSizePercent = iconTextSizePercent,
            iconSizeOverflowThreshold = universalOverflowThreshold,
            globalIconShape = globalIconShape,
            globalIconBgColor = globalIconBgColor,
            globalIconBgIntensity = globalIconBgIntensity,
            previewAppIcons = dockFolderPreviewIcons,
            onSave = { newCustomization ->
                appCustomizations = setCustomization(context, appCustomizations, folderKey, newCustomization)
                customizingDockFolder = null
            },
            onReset = {
                appCustomizations = removeCustomization(context, appCustomizations, folderKey)
                customizingDockFolder = null
            },
            onDismiss = { customizingDockFolder = null }
        )
    }

    var lastOpenedFolder by remember { mutableStateOf<HomeFolder?>(null) }
    // Store folder cell origin in pixels (top-left x, top-left y, width, height)
    var folderCellOrigin by remember { mutableStateOf(Offset.Zero) }
    var folderCellOriginSize by remember { mutableStateOf(IntSize.Zero) }
    if (openHomeFolder != null) {
        lastOpenedFolder = openHomeFolder
        val folderPos = if (openHomeFolder!!.position >= 0) {
            cellPositions[openHomeFolder!!.position]
        } else {
            // Dock folder — look up position by ID
            val df = dockFolders.find { it.id == openHomeFolder!!.id }
            df?.let { dockPositions[it.position] }
        }
        if (folderPos != null && cellSize.width > 0) {
            folderCellOrigin = folderPos
            folderCellOriginSize = if (openHomeFolder!!.position >= 0) cellSize else dockSlotSize
        }
    }
    val folderAnimProgress by animateFloatAsState(
        targetValue = if (openHomeFolder != null) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "folderOpenClose",
        finishedListener = { if (it == 0f && !escapedToHomeGrid) lastOpenedFolder = null }
    )

    // Visual-only folder close animation during escape drag.
    // Rendered as a separate Box so it doesn't affect pointer coordinates
    // in the interactive overlay (which stays full-scale + invisible).
    val escapeCloseAnim = remember { Animatable(0f) }
    var escapeCloseCellOrigin by remember { mutableStateOf(Offset.Zero) }
    var escapeCloseCellSize by remember { mutableStateOf(IntSize.Zero) }
    var escapeCloseFolderName by remember { mutableStateOf("") }
    var escapeCloseIconPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    // Use snapshotFlow so the animation runs to completion even after drop
    // (LaunchedEffect(key) would cancel the coroutine when key changes)
    LaunchedEffect(Unit) {
        snapshotFlow { escapedToHomeGrid }
            .collect { escaped ->
                if (escaped) {
                    escapeCloseCellOrigin = folderCellOrigin
                    escapeCloseCellSize = folderCellOriginSize
                    escapeCloseFolderName = lastOpenedFolder?.name ?: "Folder"
                    escapeCloseIconPaths = (lastOpenedFolder?.appPackageNames ?: emptyList())
                        .filter { it.isNotEmpty() }
                        .take(4)
                        .mapNotNull { pkg -> allAvailableApps.find { it.packageName == pkg }?.iconPath }
                    escapeCloseAnim.snapTo(1f)
                    escapeCloseAnim.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
                    // FIX: Without this, lastOpenedFolder was never nulled after escape drag
                    // because finishedListener ran while escapedToHomeGrid was still true.
                    // The folder overlay stayed in the tree with stale remember() cache.
                    if (openHomeFolder == null) {
                        lastOpenedFolder = null
                    }
                }
            }
    }
    if (escapeCloseAnim.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = escapeCloseAnim.value
                    alpha = p
                    val startScaleX = if (size.width > 0) escapeCloseCellSize.width / size.width else 0.5f
                    val startScaleY = if (size.height > 0) escapeCloseCellSize.height / size.height else 0.5f
                    scaleX = startScaleX + (1f - startScaleX) * p
                    scaleY = startScaleY + (1f - startScaleY) * p
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = escapeCloseCellOrigin.x * (1f - p)
                    translationY = escapeCloseCellOrigin.y * (1f - p)
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (1/3) — matches folder overlay header
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
                        color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current
                    )
                }
                // Content (2/3) — app grid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Render captured app icons in a grid matching the folder layout
                    if (escapeCloseIconPaths.isNotEmpty()) {
                        val folderIconSize = iconSizeDp.dp
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            for (fRow in 0 until gridRows) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (fCol in 0 until gridColumns) {
                                        val idx = fRow * gridColumns + fCol
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (idx < escapeCloseIconPaths.size) {
                                                AsyncImage(
                                                    model = File(escapeCloseIconPaths[idx]),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.size(folderIconSize)
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

    // Keep the folder overlay alive during escape drag (cell's gesture handler needs it)
    if (lastOpenedFolder != null && (folderAnimProgress > 0f || escapedToHomeGrid)) {
        val folder = openHomeFolder ?: lastOpenedFolder!!
        val folderDropScope = rememberCoroutineScope()

        // Position-based cell map: cellIndex → packageName (supports empty cells between apps, filters hidden)
        // FIX: Keyed on appPackageNames too — previously only folder.id, so re-opening
        // the same folder after removing an app showed stale cached data.
        val baseFolderCellMap = remember(folder.id, folder.appPackageNames, hiddenApps) {
            folder.appPackageNames.withIndex()
                .filter { it.value.isNotEmpty() && it.value !in hiddenApps }
                .associate { it.index to it.value }
        }
        // Mutable overlay for drag reordering within the folder (resets when folder data changes)
        var folderCellMap by remember(folder.id, folder.appPackageNames) {
            mutableStateOf(baseFolderCellMap)
        }
        // Sync when folder data changes externally (e.g., app removed via escape drag)
        LaunchedEffect(baseFolderCellMap) {
            folderCellMap = baseFolderCellMap
        }
        // Resolve package names to HomeAppInfo for rendering
        val folderApps = remember(folderCellMap, allAvailableApps) {
            folderCellMap.mapNotNull { (_, pkg) ->
                allAvailableApps.find { it.packageName == pkg }
            }
        }

        // Helper to save folder cell map back to ordered list
        val isDockFolder = folder.page == -1
        fun saveFolderCellMap(map: Map<Int, String>) {
            val maxIdx = if (map.isEmpty()) -1 else map.keys.max()
            val ordered = if (maxIdx >= 0) {
                (0..maxIdx).map { idx -> map[idx] ?: "" }.dropLastWhile { it.isEmpty() }
            } else emptyList()
            if (isDockFolder) {
                val newDockFolders = dockFolders.map { f ->
                    if (f.id == folder.id) f.copy(appPackageNames = ordered) else f
                }
                saveDockFolders(newDockFolders)
                val updatedDF = newDockFolders.find { it.id == folder.id }
                openHomeFolder = if (updatedDF != null) HomeFolder(id = updatedDF.id, name = updatedDF.name, position = -1, page = -1, appPackageNames = updatedDF.appPackageNames) else null
            } else {
                val newFolders = homeFolders.map { f ->
                    if (f.id == folder.id) f.copy(appPackageNames = ordered) else f
                }
                saveHomeFolders(newFolders)
                openHomeFolder = newFolders.find { it.id == folder.id }
            }
        }

        // Drag state
        var draggedPkg by remember { mutableStateOf<String?>(null) }
        var draggedFolderCellIdx by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var dragOriginalFolderCellPos by remember { mutableStateOf<Offset?>(null) }
        // Track hovered cell index for hover indicator (like home grid)
        var hoveredFolderCell by remember { mutableStateOf<Int?>(null) }
        val folderCellPositions = remember { mutableStateMapOf<Int, Offset>() }
        var folderCellSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        // Drop animation state (matches home screen pattern)
        var isFolderDropAnimating by remember { mutableStateOf(false) }
        val folderDropAnimProgress = remember { Animatable(0f) }
        var folderDropStartOffset by remember { mutableStateOf(Offset.Zero) }
        var folderDropTargetOffset by remember { mutableStateOf(Offset.Zero) }

        var isFolderNameEditing by remember { mutableStateOf(false) }
        var editedFolderName by remember(folder.name) {
            mutableStateOf(TextFieldValue(folder.name, TextRange(folder.name.length)))
        }
        val folderNameFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        var showFolderDeleteConfirm by remember { mutableStateOf(false) }

        LaunchedEffect(isFolderNameEditing) {
            if (isFolderNameEditing) {
                folderNameFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        // Auto-save folder name as user types (debounced)
        LaunchedEffect(editedFolderName.text) {
            if (editedFolderName.text != folder.name && editedFolderName.text.isNotBlank()) {
                kotlinx.coroutines.delay(300)
                if (isDockFolder) {
                    saveDockFolders(dockFolders.map {
                        if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                    })
                } else {
                    saveHomeFolders(homeFolders.map {
                        if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                    })
                }
                openHomeFolder = folder.copy(name = editedFolderName.text)
            }
        }

        var folderOverlayRootPos by remember { mutableStateOf(Offset.Zero) }
        var folderHeaderBottomY by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { folderOverlayRootPos = it.positionInRoot() }
                .graphicsLayer {
                    if (escapedToHomeGrid) {
                        // During escape drag: keep overlay at full scale but invisible.
                        // The home grid overlay renders the dragged icon instead.
                        // CRITICAL: scale must stay 1.0 so pointer coordinates aren't
                        // distorted by the inverse-transform — otherwise deltas get
                        // amplified as the close animation scales down, causing the icon to fly.
                        // A separate visual-only Box handles the close animation.
                        alpha = 0f
                    } else {
                        val p = folderAnimProgress
                        alpha = p
                        val startScaleX = if (size.width > 0) folderCellOriginSize.width / size.width else 0.5f
                        val startScaleY = if (size.height > 0) folderCellOriginSize.height / size.height else 0.5f
                        scaleX = startScaleX + (1f - startScaleX) * p
                        scaleY = startScaleY + (1f - startScaleY) * p
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = folderCellOrigin.x * (1f - p)
                        translationY = folderCellOrigin.y * (1f - p)
                    }
                }
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header area - 1/3 of screen, lighter background
            // Dragging an app into this area closes the folder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.33f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onGloballyPositioned { coords ->
                        folderHeaderBottomY = coords.positionInRoot().y + coords.size.height
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background tap to close (or save edit)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isFolderNameEditing) {
                                openHomeFolder = null
                            } else {
                                if (editedFolderName.text.isNotBlank()) {
                                    if (isDockFolder) {
                                        saveDockFolders(dockFolders.map {
                                            if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                                        })
                                    } else {
                                        saveHomeFolders(homeFolders.map {
                                            if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                                        })
                                    }
                                    openHomeFolder = folder.copy(name = editedFolderName.text)
                                }
                                isFolderNameEditing = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        }
                )

                // Folder name - tap to edit
                if (isFolderNameEditing) {
                    BasicTextField(
                        value = editedFolderName,
                        onValueChange = { editedFolderName = it },
                        textStyle = TextStyle(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (editedFolderName.text.isNotBlank()) {
                                    if (isDockFolder) {
                                        saveDockFolders(dockFolders.map {
                                            if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                                        })
                                    } else {
                                        saveHomeFolders(homeFolders.map {
                                            if (it.id == folder.id) it.copy(name = editedFolderName.text) else it
                                        })
                                    }
                                    openHomeFolder = folder.copy(name = editedFolderName.text)
                                }
                                isFolderNameEditing = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .focusRequester(folderNameFocusRequester)
                            .widthIn(min = 100.dp, max = 280.dp),
                        decorationBox = { innerTextField -> innerTextField() }
                    )
                } else {
                    Text(
                        text = folder.name,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            editedFolderName = TextFieldValue(folder.name, TextRange(folder.name.length))
                            isFolderNameEditing = true
                        }
                    )
                }

                // Delete folder button - bottom right of header
                IconButton(
                    onClick = { showFolderDeleteConfirm = true },
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

            // Apps area - 2/3 of screen (dark background, sharp corners)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Resolve cell map to HomeAppInfo for rendering
                val folderCellAppMap = remember(folderCellMap, allAvailableApps) {
                    folderCellMap.mapNotNull { (idx, pkg) ->
                        allAvailableApps.find { it.packageName == pkg }?.let { idx to it }
                    }.toMap()
                }
                val isDraggingInFolder = draggedPkg != null && !isFolderDropAnimating

                Column(modifier = Modifier.fillMaxSize().graphicsLayer { clip = false }) {
                    for (fRow in 0 until gridRows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer { clip = false }
                        ) {
                            for (fCol in 0 until gridColumns) {
                                val cellIdx = fRow * gridColumns + fCol
                                val cellApp = folderCellAppMap[cellIdx]
                                val isDragged = cellApp != null && draggedPkg == cellApp.packageName

                                val folderGridCell: HomeGridCell = if (cellApp != null) {
                                    HomeGridCell.App(cellApp, cellIdx)
                                } else {
                                    HomeGridCell.Empty
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .graphicsLayer { clip = false }
                                ) {
                                    DraggableGridCell(
                                        cell = folderGridCell,
                                        index = cellIdx,
                                        iconSize = iconSizeDp,
                                        gridColumns = gridColumns,
                                        gridRows = gridRows,
                                        isEditMode = false,
                                        isDragging = isDragged,
                                        checkIsDragOwner = { draggedFolderCellIdx == cellIdx },
                                        isAnyItemDragging = isDraggingInFolder,
                                        isDropTarget = false,
                                        isHovered = isDraggingInFolder && hoveredFolderCell == cellIdx,
                                        dragOffset = if (isDragged) dragOffset else Offset.Zero,
                                        markerHalfSizeParam = gridMarkerHalfSize,
                                        plusMarkerSize = gridPlusMarkerSize,
                                        plusMarkerFontSize = gridPlusMarkerFont,
                                        appNameFontSize = gridAppNameFont,
                                        appNameFontFamily = selectedFontFamily,
                                        iconTextSpacer = gridIconTextSpacer,
                                        hoverCornerRadius = gridHoverCornerRadius,
                                        removeLabel = "Remove from folder",
                                        onPositioned = { pos, size ->
                                            folderCellPositions[cellIdx] = pos
                                            folderCellSize = size
                                        },
                                        onDragStart = {
                                            if (cellApp != null) {
                                                draggedPkg = cellApp.packageName
                                                draggedFolderCellIdx = cellIdx
                                                dragOffset = Offset.Zero
                                                dragOriginalFolderCellPos = folderCellPositions[cellIdx]
                                            }
                                        },
                                        onDrag = { delta ->
                                            // After escape, forward all drag deltas to the outer (home grid) system
                                            if (escapedToHomeGrid) {
                                                updateEscapedDrag(delta)
                                                return@DraggableGridCell
                                            }

                                            dragOffset += delta

                                            // Compute hovered folder cell from drag center
                                            val cellPos = folderCellPositions[draggedFolderCellIdx ?: cellIdx]
                                            if (cellPos != null && folderCellSize.width > 0) {
                                                val dragCenter = Offset(
                                                    cellPos.x + folderCellSize.width / 2f + dragOffset.x,
                                                    cellPos.y + folderCellSize.height / 2f + dragOffset.y
                                                )

                                                // If dragged into header area, escape to home grid drag
                                                if (dragCenter.y < folderHeaderBottomY && cellApp != null) {
                                                    val escapedApp = allAvailableApps.find { it.packageName == cellApp.packageName }
                                                    if (escapedApp != null) {
                                                        // Remove app from folder (replace with empty string to preserve positions)
                                                        if (isDockFolder) {
                                                            val updatedDFs = dockFolders.map { f ->
                                                                if (f.id == folder.id) f.copy(appPackageNames = f.appPackageNames.map { if (it == cellApp.packageName) "" else it }.dropLastWhile { it.isEmpty() }) else f
                                                            }
                                                            val updatedDF = updatedDFs.find { it.id == folder.id }
                                                            if (updatedDF != null && updatedDF.appPackageNames.count { it.isNotEmpty() } <= 1) {
                                                                val remainingPkg = updatedDF.appPackageNames.firstOrNull { it.isNotEmpty() }
                                                                if (remainingPkg != null) {
                                                                    dockApps = dockApps + DockApp(remainingPkg, updatedDF.position)
                                                                }
                                                                saveDockFolders(dockFolders.filter { it.id != folder.id })
                                                            } else {
                                                                saveDockFolders(updatedDFs)
                                                            }
                                                        } else {
                                                            val updatedFolders = homeFolders.map { f ->
                                                                if (f.id == folder.id) {
                                                                    f.copy(appPackageNames = f.appPackageNames.map { if (it == cellApp.packageName) "" else it }.dropLastWhile { it.isEmpty() })
                                                                } else f
                                                            }
                                                            val updated = updatedFolders.find { it.id == folder.id }
                                                            if (updated != null && updated.appPackageNames.count { it.isNotEmpty() } <= 1) {
                                                                val remainingPkg = updated.appPackageNames.firstOrNull { it.isNotEmpty() }
                                                                if (remainingPkg != null) {
                                                                    homeApps = homeApps + HomeScreenApp(
                                                                        packageName = remainingPkg,
                                                                        position = folder.position,
                                                                        page = folder.page
                                                                    )
                                                                }
                                                                saveHomeFolders(homeFolders.filter { it.id != folder.id })
                                                            } else {
                                                                saveHomeFolders(updatedFolders)
                                                            }
                                                        }

                                                        // Set up home grid drag via outer-scope function
                                                        // FIX: Dock folders have position=-1, so cellPositions[-1] was null.
                                                        // Use dockPositions instead for correct drag overlay positioning.
                                                        val folderOriginPos = if (isDockFolder) {
                                                            val df = dockFolders.find { it.id == folder.id }
                                                            if (df != null) dockPositions[df.position] else null
                                                        } else {
                                                            cellPositions[folder.position]
                                                        }
                                                        val currentAbsPos = cellPos + dragOffset
                                                        val escapeDragOff = if (folderOriginPos != null) {
                                                            currentAbsPos - folderOriginPos
                                                        } else Offset.Zero
                                                        setupFolderEscapeDrag(
                                                            app = escapedApp,
                                                            folderId = folder.id,
                                                            folderPage = folder.page,
                                                            originPos = folderOriginPos,
                                                            escapeDragOffset = escapeDragOff
                                                        )

                                                        // Mark as escaped — keep draggedFolderCellIdx so
                                                        // checkIsDragOwner() stays true and the cell's handler
                                                        // continues to forward events via onDrag/onDragEnd
                                                        escapedToHomeGrid = true
                                                        hoveredFolderCell = null
                                                        openHomeFolder = null
                                                    }
                                                    return@DraggableGridCell
                                                }

                                                hoveredFolderCell = folderCellPositions.entries.firstOrNull { (_, pos) ->
                                                    dragCenter.x >= pos.x && dragCenter.x < pos.x + folderCellSize.width &&
                                                    dragCenter.y >= pos.y && dragCenter.y < pos.y + folderCellSize.height
                                                }?.key
                                            }
                                        },
                                        onDragEnd = {
                                            // If we escaped to home grid, drop on the grid
                                            if (escapedToHomeGrid) {
                                                performDrop()
                                                escapedToHomeGrid = false
                                                lastOpenedFolder = null
                                                draggedPkg = null
                                                draggedFolderCellIdx = null
                                                dragOffset = Offset.Zero
                                                dragOriginalFolderCellPos = null
                                                return@DraggableGridCell
                                            }

                                            val fromIdx = draggedFolderCellIdx
                                            val toIdx = hoveredFolderCell
                                            val originalPos = dragOriginalFolderCellPos
                                            val pkg = draggedPkg

                                            if (fromIdx != null && toIdx != null && fromIdx != toIdx && pkg != null && originalPos != null && !isFolderDropAnimating) {
                                                // Compute drop animation target
                                                val targetPos = folderCellPositions[toIdx]
                                                val targetOffset = if (targetPos != null) {
                                                    Offset(targetPos.x - originalPos.x, targetPos.y - originalPos.y)
                                                } else Offset.Zero

                                                folderDropStartOffset = dragOffset
                                                folderDropTargetOffset = targetOffset
                                                isFolderDropAnimating = true
                                                hoveredFolderCell = null

                                                folderDropScope.launch {
                                                    folderDropAnimProgress.snapTo(0f)
                                                    folderDropAnimProgress.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))

                                                    // Perform the move
                                                    val newMap = folderCellMap.toMutableMap()
                                                    newMap.remove(fromIdx)
                                                    val existingAtTarget = newMap[toIdx]
                                                    if (existingAtTarget != null) {
                                                        // Swap
                                                        newMap[toIdx] = pkg
                                                        newMap[fromIdx] = existingAtTarget
                                                    } else {
                                                        // Move to empty cell
                                                        newMap[toIdx] = pkg
                                                    }
                                                    folderCellMap = newMap
                                                    saveFolderCellMap(newMap)

                                                    // Reset
                                                    draggedPkg = null
                                                    draggedFolderCellIdx = null
                                                    dragOffset = Offset.Zero
                                                    dragOriginalFolderCellPos = null
                                                    isFolderDropAnimating = false
                                                }
                                            } else {
                                                // Drop back to original position
                                                if (originalPos != null && !isFolderDropAnimating) {
                                                    folderDropStartOffset = dragOffset
                                                    folderDropTargetOffset = Offset.Zero
                                                    isFolderDropAnimating = true
                                                    hoveredFolderCell = null

                                                    folderDropScope.launch {
                                                        folderDropAnimProgress.snapTo(0f)
                                                        folderDropAnimProgress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                                                        draggedPkg = null
                                                        draggedFolderCellIdx = null
                                                        dragOffset = Offset.Zero
                                                        dragOriginalFolderCellPos = null
                                                        isFolderDropAnimating = false
                                                    }
                                                } else {
                                                    draggedPkg = null
                                                    draggedFolderCellIdx = null
                                                    dragOffset = Offset.Zero
                                                    dragOriginalFolderCellPos = null
                                                    hoveredFolderCell = null
                                                }
                                            }
                                        },
                                        onTap = {
                                            if (cellApp != null && draggedPkg == null) {
                                                openHomeFolder = null
                                                launchApp(context, cellApp.packageName)
                                            }
                                        },
                                        onLongPress = { /* no-op for folder empty cells */ },
                                        onRemove = {
                                            if (cellApp != null) {
                                                if (isDockFolder) {
                                                    val updatedDFs = dockFolders.map { f ->
                                                        if (f.id == folder.id) {
                                                            f.copy(appPackageNames = f.appPackageNames.map { if (it == cellApp.packageName) "" else it }.dropLastWhile { it.isEmpty() })
                                                        } else f
                                                    }
                                                    val updatedDF = updatedDFs.find { it.id == folder.id }
                                                    if (updatedDF != null && updatedDF.appPackageNames.count { it.isNotEmpty() } <= 1) {
                                                        // Dock folder dissolves → remaining app becomes dock app
                                                        val remainingPkg = updatedDF.appPackageNames.firstOrNull { it.isNotEmpty() }
                                                        if (remainingPkg != null) {
                                                            dockApps = dockApps + DockApp(remainingPkg, updatedDF.position)
                                                        }
                                                        saveDockFolders(dockFolders.filter { it.id != folder.id })
                                                        openHomeFolder = null
                                                    } else {
                                                        saveDockFolders(updatedDFs)
                                                        openHomeFolder = if (updatedDF != null) HomeFolder(id = updatedDF.id, name = updatedDF.name, position = -1, page = -1, appPackageNames = updatedDF.appPackageNames) else null
                                                    }
                                                } else {
                                                    val updatedFolders = homeFolders.map { f ->
                                                        if (f.id == folder.id) {
                                                            f.copy(appPackageNames = f.appPackageNames.map { if (it == cellApp.packageName) "" else it }.dropLastWhile { it.isEmpty() })
                                                        } else f
                                                    }
                                                    val updated = updatedFolders.find { it.id == folder.id }
                                                    if (updated != null && updated.appPackageNames.count { it.isNotEmpty() } <= 1) {
                                                        val remainingPkg = updated.appPackageNames.firstOrNull { it.isNotEmpty() }
                                                        if (remainingPkg != null) {
                                                            homeApps = homeApps + HomeScreenApp(
                                                                packageName = remainingPkg,
                                                                position = folder.position,
                                                                page = folder.page
                                                            )
                                                        }
                                                        saveHomeFolders(homeFolders.filter { it.id != folder.id })
                                                        openHomeFolder = null
                                                    } else {
                                                        saveHomeFolders(updatedFolders)
                                                        openHomeFolder = updated
                                                    }
                                                }
                                            }
                                        },
                                        onUninstall = {
                                            if (cellApp != null) uninstallApp(context, cellApp.packageName)
                                        },
                                        onAppInfo = {
                                            if (cellApp != null) openAppInfo(context, cellApp.packageName)
                                        },
                                        onCustomize = {
                                            if (cellApp != null) {
                                                customizingApp = cellApp
                                            }
                                        },
                                        isCustomizing = cellApp != null && customizingApp?.packageName == cellApp.packageName,
                                        globalIconSizePercent = iconSizePercent.toFloat(),
                                        globalIconShape = globalIconShape,
                                        globalIconBgColor = globalIconBgColor,
                                        globalIconBgIntensity = globalIconBgIntensity
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } // end Column

        // Drag overlay — rendered outside clipped areas so it can move freely
        // Don't show folder drag overlay after escape (home grid overlay takes over)
        if (draggedPkg != null && !escapedToHomeGrid) {
            val draggedApp = folderApps.find { it.packageName == draggedPkg }
            val originalCellPos = dragOriginalFolderCellPos
            if (draggedApp != null && originalCellPos != null && folderCellSize.width > 0) {
                val p = if (isFolderDropAnimating) folderDropAnimProgress.value else 0f
                val currentOffset = if (isFolderDropAnimating) {
                    folderDropStartOffset + (folderDropTargetOffset - folderDropStartOffset) * p
                } else dragOffset
                val boxScale = if (isFolderDropAnimating && folderDropTargetOffset == Offset.Zero) {
                    1f // Return to origin — match cell scale to avoid transition flicker
                } else {
                    1.265f - 0.265f * p
                }
                val boxAlpha = if (isFolderDropAnimating && folderDropTargetOffset == Offset.Zero) {
                    1f // Return to origin — constant alpha to avoid brightness flash
                } else {
                    0.8f + 0.2f * p
                }
                val overlayTextAlpha = if (isFolderDropAnimating && folderDropTargetOffset == Offset.Zero) {
                    1f // Return to origin — constant to avoid flicker
                } else if (isFolderDropAnimating) p else 0f

                val appLeft = originalCellPos.x - folderOverlayRootPos.x + currentOffset.x
                val appTop = originalCellPos.y - folderOverlayRootPos.y + currentOffset.y

                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { folderCellSize.width.toDp() },
                            height = with(density) { folderCellSize.height.toDp() }
                        )
                        .zIndex(1000f)
                        .graphicsLayer {
                            translationX = appLeft
                            translationY = appTop
                            scaleX = boxScale
                            scaleY = boxScale
                            alpha = boxAlpha
                            clip = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(gridMarkerHalfSize),
                        contentAlignment = Alignment.Center
                    ) {
                        OverlayAppContent(context, draggedApp, iconSizeDp, iconSizePercent, gridIconTextSpacer, gridAppNameFont, selectedFontFamily, overlayTextAlpha, globalIconShape, showLabel = true, globalIconBgColor = globalIconBgColor)
                    }
                }
            }
        }
        } // end wrapper Box

        // Delete folder confirmation dialog
        if (showFolderDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showFolderDeleteConfirm = false },
                title = { Text("Remove Folder?") },
                text = { Text("Apps inside this folder (${folder.name}) will be removed from the launcher screen.") },
                confirmButton = {
                    TextButton(onClick = {
                        saveHomeFolders(homeFolders.filter { it.id != folder.id })
                        openHomeFolder = null
                        showFolderDeleteConfirm = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFolderDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Launcher settings menu (shown on long-press of empty area)
        // Custom position provider that places menu at touch position
        // Shows above touch by default, below if too close to top
        val menuPositionProvider = remember(launcherMenuPosition) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val touchX = launcherMenuPosition.x.toInt()
                    val touchY = launcherMenuPosition.y.toInt()
                    val menuHeight = popupContentSize.height
                    val menuWidth = popupContentSize.width

                    // Determine if menu should appear above or below the touch point
                    // Show above by default, but if too close to top, show below
                    val topMargin = with(density) { 50.dp.toPx().toInt() }
                    val showBelow = touchY < menuHeight + topMargin

                    // Calculate Y position
                    val menuY = if (showBelow) {
                        touchY // Show below touch point
                    } else {
                        (touchY - menuHeight).coerceAtLeast(0) // Show above touch point
                    }

                    // Keep menu within horizontal bounds
                    val menuX = touchX.coerceIn(0, (windowSize.width - menuWidth).coerceAtLeast(0))

                    return IntOffset(menuX, menuY)
                }
            }
        }

        AnimatedPopup(
            visible = showLauncherSettingsMenu,
            onDismissRequest = { showLauncherSettingsMenu = false },
            popupPositionProvider = menuPositionProvider
        ) {
                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Launcher Menu",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Divider()

                    // Settings option
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            showLauncherSettingsMenu = false
                            onOpenSettings()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    )
                    // Wallpaper option
                    DropdownMenuItem(
                        text = { Text("Wallpaper") },
                        onClick = {
                            showLauncherSettingsMenu = false
                            openWallpaperPicker(context)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Wallpaper,
                                contentDescription = "Wallpaper"
                            )
                        }
                    )
                    // Widgets option
                    DropdownMenuItem(
                        text = { Text("Widgets") },
                        onClick = {
                            showLauncherSettingsMenu = false
                            onOpenWidgets()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Widgets,
                                contentDescription = "Widgets"
                            )
                        }
                    )
                    // Add Screen option
                    DropdownMenuItem(
                        text = { Text("Add Screen") },
                        onClick = {
                            addingLastDot = true
                            totalPages++
                            prefs.edit().putInt("launcher_total_pages", totalPages).apply()
                            showLauncherSettingsMenu = false
                            dropScope.launch {
                                delay(350) // Match dot animation duration (300ms) + margin
                                addingLastDot = false
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Screen"
                            )
                        }
                    )
                    // Remove Screen option
                    val canRemove = currentPage != 0 && totalPages > 1
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (canRemove) "Remove Screen" else "Can't Remove",
                                color = if (canRemove) Color.Unspecified
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        onClick = {
                            if (canRemove) {
                                showLauncherSettingsMenu = false
                                removingLastDot = true
                                val targetPage = pagerState.currentPage - 1
                                dropScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                    delay(350) // Let dot fade-out animation complete (300ms + margin)
                                    totalPages -= 1
                                    removingLastDot = false
                                    prefs.edit().putInt("launcher_total_pages", totalPages).apply()
                                }
                            }
                        },
                        enabled = canRemove,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove Screen",
                                tint = if (canRemove) LocalContentColor.current
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    )
        }
}

// WidgetHostView moved to ui/home/HomeWidgetHostView.kt

// DraggableGridCell and DockSlot moved to Utility/DrawerMovement/AppGridMovement.kt
// Storage functions moved to data/HomeScreenStorage.kt
// openWallpaperPicker, openWidgetPicker moved to helpers/AppActions.kt

/**
 * App item inside a home screen folder.
 * Supports tap (launch), long-press (context menu), and long-press+drag (reorder within folder).
 */
@Composable
private fun HomeFolderAppItem(
    app: HomeAppInfo,
    iconSize: Int,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFontFamily: FontFamily? = null,
    isDragged: Boolean = false,
    globalIconShape: String? = null,
    globalIconBgColor: Int? = null,
    globalIconBgIntensity: Int = 100,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val hapticFeedback = rememberHapticFeedback()
    var showContextMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showContextMenu || isDragged) 1.265f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "folderAppScale"
    )

    Box {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = down.position
                        val longPress = awaitLongPressOrCancellation(down.id)

                        if (longPress != null) {
                            hapticFeedback.performLongPress()
                            var dragStarted = false

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val dx = change.position.x - startPosition.x
                                        val dy = change.position.y - startPosition.y
                                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                        if (!dragStarted && distance > touchSlop) {
                                            dragStarted = true
                                            onDragStart()
                                        }

                                        if (dragStarted) {
                                            val delta = Offset(
                                                change.position.x - change.previousPosition.x,
                                                change.position.y - change.previousPosition.y
                                            )
                                            change.consume()
                                            onDrag(delta)
                                        }
                                    } else {
                                        if (dragStarted) {
                                            onDragEnd()
                                        } else {
                                            showContextMenu = true
                                        }
                                        break
                                    }
                                }
                            } catch (_: Exception) {
                                if (dragStarted) onDragEnd()
                            }
                        } else {
                            val upEvent = currentEvent.changes.firstOrNull()
                            if (upEvent != null && !upEvent.pressed) {
                                onClick()
                            }
                        }
                    }
                }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val folderItemContext = LocalContext.current
            val displayIconPath = remember(app.packageName, globalIconShape, globalIconBgColor, globalIconBgIntensity) {
                if (globalIconBgColor != null && globalIconShape != null) {
                    try { com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon(folderItemContext, app.packageName, globalIconShape, globalIconBgColor, globalIconBgIntensity) }
                    catch (_: Exception) {
                        if (globalIconShape != null) {
                            try { com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon(folderItemContext, app.packageName, globalIconShape) }
                            catch (_: Exception) { app.iconPath }
                        } else app.iconPath
                    }
                } else if (globalIconShape != null) {
                    try { com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon(folderItemContext, app.packageName, globalIconShape) }
                    catch (_: Exception) { app.iconPath }
                } else app.iconPath
            }
            AsyncImage(
                model = File(displayIconPath),
                contentDescription = app.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(iconSize.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name,
                fontSize = labelFontSize,
                fontFamily = labelFontFamily ?: FontFamily.Default,
                color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

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
                                text = app.name,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider()

                        DropdownMenuItem(
                            text = { Text("Remove from folder") },
                            onClick = {
                                showContextMenu = false
                                onRemove()
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
                            text = { Text("Uninstall") },
                            onClick = {
                                showContextMenu = false
                                onUninstall()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )
            }
    }
}

// launchApp moved to data/HomeScreenStorage.kt
// LauncherUtils moved to data/LauncherUtils.kt
