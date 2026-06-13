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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.animation.core.snap
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.bearinmind.launcher314.data.DeviceWallpaperEdit
import com.bearinmind.launcher314.data.dispatch
import com.bearinmind.launcher314.data.WALLPAPER_MODE_DEVICE
import com.bearinmind.launcher314.data.WallpaperPreviewBus
import com.bearinmind.launcher314.data.getDeviceWallpaperEdit
import com.bearinmind.launcher314.data.getDeviceWallpaperSourcePath
import com.bearinmind.launcher314.data.setDeviceWallpaperEdit
import com.bearinmind.launcher314.data.setWallpaperMode
import com.bearinmind.launcher314.helpers.WallpaperHelper
import com.bearinmind.launcher314.ui.settings.WallpaperEditorScreen
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
import androidx.compose.ui.window.Dialog
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
    if (com.bearinmind.launcher314.ui.theme.LocalHideIconText.current) return
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
 * Resolve a folder's (index → package) cell map to (index → HomeAppInfo),
 * reflowing any app whose index lands OUTSIDE the visible grid into the first
 * free in-grid cell. Prevents an app from being real in appPackageNames but
 * never rendered (so it shows in the compacted 2x2 closed preview yet vanishes
 * when the folder opens). In-grid gaps are preserved so drag-reorder works.
 * Top-level (not inline) to keep LauncherScreen under the JVM 64KB method limit.
 */
// Re-derive the live folder for the open-folder overlay from the current
// homeFolders / dockFolders by ID. The open snapshot (openHomeFolder /
// lastOpenedFolder) is a point-in-time copy that does NOT update when the
// underlying folder changes (e.g. re-adding an escaped app), so rendering off
// it showed stale until a full reload. Falls back to the snapshot mid-close.
// Top-level (not inline) to keep LauncherScreen under the JVM 64KB limit.
// Resolve the app shown in a folder cell. Falls back to the actively-dragged
// app for its owning cell so that cell keeps rendering as App for the whole
// drag (even after escape removes it from the map) — otherwise the cell flips
// App→Empty mid-drag, disposing the running pointerInput and auto-dropping.
// Top-level (not inline) to keep LauncherScreen under the JVM 64KB limit.
private fun resolveFolderCellApp(
    folderCellAppMap: Map<Int, HomeAppInfo>,
    cellIdx: Int,
    draggedPkg: String?,
    draggedFolderCellIdx: Int?,
    allAvailableApps: List<HomeAppInfo>
): HomeAppInfo? {
    return folderCellAppMap[cellIdx]
        ?: if (draggedPkg != null && draggedFolderCellIdx == cellIdx)
            allAvailableApps.find { it.packageName == draggedPkg } else null
}

private fun liveFolder(
    snapshot: HomeFolder,
    homeFolders: List<HomeFolder>,
    dockFolders: List<DockFolder>
): HomeFolder {
    return if (snapshot.page == -1) {
        dockFolders.find { it.id == snapshot.id }
            ?.let { HomeFolder(id = it.id, name = it.name, position = -1, page = -1, appPackageNames = it.appPackageNames) }
            ?: snapshot
    } else {
        homeFolders.find { it.id == snapshot.id } ?: snapshot
    }
}

private fun reflowFolderCells(
    folderCellMap: Map<Int, String>,
    allAvailableApps: List<HomeAppInfo>,
    cellCount: Int
): Map<Int, HomeAppInfo> {
    val resolved = folderCellMap.mapNotNull { (idx, pkg) ->
        allAvailableApps.find { it.packageName == pkg }?.let { idx to it }
    }
    val placed = resolved.filter { it.first in 0 until cellCount }.toMap().toMutableMap()
    val overflow = resolved.filter { it.first !in 0 until cellCount }.map { it.second }
    if (overflow.isNotEmpty()) {
        var c = 0
        for (app in overflow) {
            while (c < cellCount && placed.containsKey(c)) c++
            if (c < cellCount) { placed[c] = app; c++ } else break
        }
    }
    return placed.toMap()
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
    // Experimental "Use original" mode — bypass every image-side override
    // (custom icon path, shape, tint, icon-pack) and render the app's own
    // launcher icon as-is.
    val useOrig = appInfo.customization?.useOriginalIcon == true
    val hasCustomIcon = !useOrig && appInfo.customization?.customIconPath?.let { File(it).exists() } == true
    val hasShapeExp = !useOrig && appInfo.customization?.iconShapeExp != null
    val hasPerAppShape = !useOrig && appInfo.customization?.iconShape != null

    // ── Async global-shaped icon ─────────────────────────────────────────
    // On cold cache, getOrGenerateGlobalShapedIcon decodes the drawable,
    // draws to a 192x192 bitmap, and writes a PNG synchronously — that
    // used to block the UI thread per grid cell on first launch / after
    // a shape change. Now: if the cache file exists return its path
    // immediately, otherwise show the raw icon and run the generator on
    // Dispatchers.IO; Coil swaps to the shaped path on the next recomp.
    val needGlobalShape = !useOrig && !hasCustomIcon && !hasShapeExp && !hasPerAppShape && globalIconShape != null
    var globalShapeAsyncPath by remember(appInfo.packageName, globalIconShape, needGlobalShape) {
        val initial: String = if (needGlobalShape && globalIconShape != null) {
            val f = File(getGlobalShapedDir(context), "${appInfo.packageName}.png")
            if (f.exists()) f.absolutePath else appInfo.iconPath
        } else appInfo.iconPath
        mutableStateOf(initial)
    }
    LaunchedEffect(needGlobalShape, appInfo.packageName, globalIconShape) {
        if (needGlobalShape && globalIconShape != null) {
            val f = File(getGlobalShapedDir(context), "${appInfo.packageName}.png")
            if (!f.exists()) {
                val gen = withContext(Dispatchers.IO) {
                    try { getOrGenerateGlobalShapedIcon(context, appInfo.packageName, globalIconShape) }
                    catch (_: Exception) { null }
                }
                if (gen != null) globalShapeAsyncPath = gen
            }
        }
    }

    val iconPath = if (useOrig) appInfo.iconPath
    else if (hasCustomIcon) {
        appInfo.customization!!.customIconPath!!
    } else if (hasShapeExp) {
        File(getShapedExpDir(context), "${appInfo.packageName}.png").let {
            if (it.exists()) it.absolutePath else appInfo.iconPath
        }
    } else if (!hasPerAppShape && globalIconShape != null) {
        globalShapeAsyncPath
    } else appInfo.iconPath
    val hasBgTint = !useOrig && appInfo.customization?.iconTintBackgroundOnly == true && appInfo.customization?.iconTintColor != null
    val hasAnyShape = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
    val effectiveShape = appInfo.customization?.iconShapeExp ?: appInfo.customization?.iconShape ?: globalIconShape
    val finalIconPath = if (hasBgTint && !hasCustomIcon) {
        val tintColor = appInfo.customization?.iconTintColor?.toInt() ?: 0
        val tintAlpha = (appInfo.customization?.iconTintIntensity ?: 100) / 100f
        resolveBgTintIconPath(context, appInfo.packageName, hasAnyShape, iconPath, effectiveShape, tintColor, tintAlpha)
    } else iconPath
    val hasAnyExp = hasShapeExp || (!hasPerAppShape && globalIconShape != null)
    val clipShape = if (useOrig) null
    else if (hasCustomIcon) {
        getIconShape(appInfo.customization?.iconShapeExp ?: appInfo.customization?.iconShape ?: globalIconShape)
    } else if (!hasAnyExp) getIconShape(appInfo.customization?.iconShape) else null
    val tintFilter = if (useOrig || hasBgTint) null else appInfo.customization?.iconTintColor?.let { tintColor ->
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
    val useBgColorIcon = !useOrig && globalIconBgColor != null && !hasCustomIcon
    val bgColorEffectiveShape = if (useBgColorIcon) {
        appInfo.customization?.iconShapeExp
            ?: appInfo.customization?.iconShape
            ?: globalIconShape
    } else null
    // ── Async bg-color shaped icon ───────────────────────────────────────
    // Same pattern as the global-shape generator above — show the
    // un-bg-colored icon immediately, then swap in the generated one once
    // IO is done. Stops the per-cell freeze when a global bg color is set
    // and the cache is cold.
    val needBgColorIcon = useBgColorIcon && bgColorEffectiveShape != null && globalIconBgColor != null
    var bgColorAsyncPath by remember(appInfo.packageName, bgColorEffectiveShape, globalIconBgColor, needBgColorIcon) {
        val initial: String = if (needBgColorIcon) {
            // Cache-FIRST init (mirrors the grid cell). A cached bg-color icon
            // shows IMMEDIATELY instead of starting on the no-bg icon and then
            // async-swapping — that swap was the one-frame "ghost flash" to the
            // original icon when picking up an app. Filename mirrors
            // getOrGenerateBgColorShapedIcon (pkg_shape_colorHex_intensity).
            val colorHex = Integer.toHexString(globalIconBgColor!!)
            val intensity = com.bearinmind.launcher314.data.getGlobalIconBgIntensity(context)
            val cacheFile = File(
                com.bearinmind.launcher314.helpers.getBgColorShapedDir(context),
                "${appInfo.packageName}_${bgColorEffectiveShape}_${colorHex}_${intensity}.png"
            )
            if (cacheFile.exists()) cacheFile.absolutePath else finalIconPath
        } else finalIconPath
        mutableStateOf(initial)
    }
    LaunchedEffect(needBgColorIcon, appInfo.packageName, bgColorEffectiveShape, globalIconBgColor) {
        if (needBgColorIcon) {
            val gen = withContext(Dispatchers.IO) {
                try { getOrGenerateBgColorShapedIcon(context, appInfo.packageName, bgColorEffectiveShape!!, globalIconBgColor!!) }
                catch (_: Exception) { null }
            }
            if (gen != null) bgColorAsyncPath = gen
        }
    }
    val displayIconPath = if (needBgColorIcon) bgColorAsyncPath else finalIconPath
    val isBgColorIcon = displayIconPath != finalIconPath

    val iconTextOverride = appInfo.customization?.iconText?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .wrapContentHeight(unbounded = true)
            .graphicsLayer { clip = false },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconTextOverride != null) {
            // Text-as-icon (Total-Launcher style). Use wrapContentSize so
            // long text (e.g. "Morphe") isn't ellipsized inside the
            // icon-square — let the glyph breathe at its natural width.
            // The cell's per-app size slider still scales the whole Box
            // via the parent graphicsLayer, so the text grows / shrinks
            // with it. Vertical footprint is kept >= the icon size so
            // rows of mixed text/icon items stay aligned.
            val iconTextColorLong = appInfo.customization?.iconTextColor
            val iconTextIntensity = (appInfo.customization?.iconTextColorIntensity ?: 100) / 100f
            val iconTextResolvedColor = if (iconTextColorLong != null)
                Color(iconTextColorLong.toInt()).copy(alpha = iconTextIntensity.coerceIn(0f, 1f))
            else com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current
            val iconTextFontFamily = appInfo.customization?.iconTextFontId?.let { id ->
                FontManager.bundledFonts.find { it.id == id }?.fontFamily
                    ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
            } ?: perAppFontFamily
            Box(
                modifier = Modifier.heightIn(min = perAppIconSizeDp),
                contentAlignment = Alignment.Center
            ) {
                // Fall back to a fixed default (28sp) when the user hasn't
                // picked a custom size — same default the dialog's "Size"
                // chip shows so the on-screen value always matches.
                val iconAsTextSp = appInfo.customization?.iconAsTextSizeSp ?: 28
                Text(
                    text = iconTextOverride,
                    color = iconTextResolvedColor,
                    fontSize = iconAsTextSp.sp,
                    fontFamily = iconTextFontFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        } else {
            Box(
                modifier = Modifier.size(perAppIconSizeDp),
                contentAlignment = Alignment.Center
            ) {
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
        }
        // Label below — suppressed when in text-as-icon mode so the user
        // doesn't see the icon-text AND the app label stacked (the duplicate
        // "Morphe / Morphe" the user reported).
        if (showLabel && !com.bearinmind.launcher314.ui.theme.LocalHideIconText.current && iconTextOverride == null) {
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

/** Folder drag overlay — renders the 2x2 mini icon preview with per-app and per-folder customizations. */
@Composable
private fun OverlayFolderContent(
    context: android.content.Context,
    folderData: HomeFolder,
    folderCust: AppCustomization?,
    previewApps: List<HomeAppInfo>,
    iconSizeDp: Int,
    gridIconTextSpacer: Dp,
    gridAppNameFont: TextUnit,
    selectedFontFamily: FontFamily?,
    textAlpha: Float,
    globalIconShape: String?,
    globalIconBgColor: Int?,
    globalIconBgIntensity: Int,
    isInvalid: Boolean,
    showLabel: Boolean
) {
    val folderName = folderCust?.customLabel ?: folderData.name
    val folderOverlayTint = if (isInvalid) {
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
        val folderBoxBg = if (isInvalid) Color(0xFF4A1A1A) else Color(0xFF1A1A1A)
        val folderShape = getIconShape(folderCust?.iconShapeExp ?: globalIconShape) ?: RoundedCornerShape(folderCornerRadius)
        val folderBorderColor = if (folderCust?.iconTintColor != null) {
            val intensity = (folderCust.iconTintIntensity ?: 100) / 100f
            Color(folderCust.iconTintColor).copy(alpha = intensity.coerceIn(0f, 1f))
        } else com.bearinmind.launcher314.ui.theme.LocalFolderBorderColor.current
        Box(modifier = Modifier.size(folderBoxSize), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.matchParentSize().background(folderBoxBg, folderShape))
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .graphicsLayer { clip = true; shape = folderShape },
                contentAlignment = Alignment.Center
            ) {
                if (previewApps.isNotEmpty()) {
                    val padding = folderBoxSize * 0.08f
                    val spacing = folderBoxSize * 0.04f
                    val miniIconSize = (folderBoxSize - padding * 2 - spacing) / 2
                    val defaultMiniClip = if (globalIconShape != null) getIconShape(globalIconShape) ?: RoundedCornerShape(miniIconSize * 0.2f) else RoundedCornerShape(miniIconSize * 0.2f)
                    Column(
                        modifier = Modifier.padding(padding),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (row in 0..1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                for (col in 0..1) {
                                    val slotIdx = row * 2 + col
                                    previewApps.getOrNull(slotIdx)?.let { app ->
                                        val miniPath = remember(app.packageName, app.customization, globalIconShape, globalIconBgColor, globalIconBgIntensity) {
                                            resolveMiniIconPath(context, app.packageName, app.iconPath, globalIconShape, globalIconBgColor, globalIconBgIntensity, app.customization)
                                        }
                                        val perAppClip = app.customization?.let { c -> getIconShape(c.iconShapeExp ?: c.iconShape) } ?: defaultMiniClip
                                        val perAppTint = if (app.customization?.iconTintBackgroundOnly != true) app.customization?.iconTintColor?.let { tc ->
                                            val i = (app.customization.iconTintIntensity ?: 100) / 100f
                                            ColorFilter.tint(Color(tc.toInt()).copy(alpha = i), parseBlendMode(app.customization.iconTintBlendMode))
                                        } else null
                                        AsyncImage(
                                            model = File(miniPath),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            colorFilter = folderOverlayTint ?: perAppTint,
                                            modifier = Modifier.size(miniIconSize).clip(perAppClip)
                                        )
                                    } ?: Spacer(modifier = Modifier.size(miniIconSize))
                                }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.matchParentSize().border(1.dp, folderBorderColor, folderShape))
        }
        val folderHideLabel = folderCust?.hideLabel ?: false
        if (!folderHideLabel && showLabel && !com.bearinmind.launcher314.ui.theme.LocalHideIconText.current) {
            Spacer(modifier = Modifier.height(gridIconTextSpacer))
            val folderLabelColor = if (isInvalid) Color(0xFFFF6B6B)
                else if (folderCust?.labelColor != null) {
                    val li = (folderCust.labelColorIntensity ?: 100) / 100f
                    Color(folderCust.labelColor).copy(alpha = li.coerceIn(0f, 1f))
                } else Color.White
            val folderFontSize = folderCust?.iconTextSizePercent?.let { gridAppNameFont * it / 100f } ?: gridAppNameFont
            val folderFontFamily = folderCust?.labelFontId?.let { id ->
                FontManager.bundledFonts.find { it.id == id }?.fontFamily
                    ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
            } ?: selectedFontFamily ?: FontFamily.Default
            Text(
                text = folderName,
                fontSize = folderFontSize,
                fontFamily = folderFontFamily,
                color = folderLabelColor,
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
    onExternalDragComplete: () -> Unit = {},
    gestureUiCallbacks: com.bearinmind.launcher314.data.GestureUiCallbacks? = null
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
    val dockPagesCount = com.bearinmind.launcher314.data.getDockPages(context)
    // Current dock page driven by the dock HorizontalPager — used by drag-and-drop logic
    // so dropping into the dock targets the page the user is currently viewing.
    var currentDockPage by remember { mutableIntStateOf(0) }

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
    // Detached-icon edit mode — lives at LauncherScreen scope (not per-page) so
    // the HorizontalPager's userScrollEnabled can lock pager swipes while a
    // detached icon is being edited, and so only ONE icon across all pages can
    // be in edit mode at a time. Issue #48.
    var editingPackageName by remember { mutableStateOf<String?>(null) }
    // Mirror editingPackageName into the static DetachedEditState flag so the
    // parent LauncherWithDrawer's swipe-down handler can bail and never slide
    // the drawer up while the user is moving / resizing a detached icon.
    LaunchedEffect(editingPackageName) {
        com.bearinmind.launcher314.ui.widgets.DetachedEditState.isEditing =
            (editingPackageName != null)
    }
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
    // Actual visible bounds of each folder icon (root coords), reported by
    // DraggableGridCell.onFolderIconPositioned. Used by the folder-open
    // popup to align its edge exactly with the icon's real bounds instead
    // of an iconSizeDp-based approximation that misses by a few dp.
    val folderIconBoundsMap = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }

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
    // Root Box and dock Box positions in root coords — used by the swipe-right
    // detector (issue #40) to bail when the gesture starts inside the dock so it
    // doesn't fight the dock's HorizontalPager page-changes.
    var rootBoxTopY by remember { mutableFloatStateOf(0f) }
    var dockTopY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
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

    // FIX: Always look up the CURRENT widget from placedWidgets by appWidgetId.
    // The PlacedWidget stored in widgetResizeState.resizingWidget is a snapshot
    // captured at the moment resize started — if the widget was moved to another
    // page beforehand (or any state changed), the snapshot's .page field can be
    // stale, causing the resize overlay and position indicators to render on the
    // wrong screen. Fresh lookup ensures page-aware rendering everywhere.
    val resizingWidgetFresh: com.bearinmind.launcher314.ui.widgets.PlacedWidget? =
        widgetResizeState.resizingWidget?.appWidgetId?.let { id ->
            placedWidgets.find { it.appWidgetId == id }
        }
    val resizingWidgetPage: Int = resizingWidgetFresh?.page ?: widgetResizeState.resizingWidget?.page ?: 0

    // Current resize dimensions - used to override widget visual size during resize
    var currentResizeDimensions by remember { mutableStateOf<ResizeDimensions?>(null) }

    // Page state (persisted via SharedPreferences)
    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    var totalPages by remember { mutableIntStateOf(prefs.getInt("launcher_total_pages", 1)) }
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    // Persist the current home page index so MainActivity's add-widget flow
    // can land the widget on the page the user is actually viewing instead
    // of always defaulting to page 0.
    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putInt("launcher_current_page", pagerState.currentPage).apply()
    }

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

    // Wallpaper picker dialog — appears when the user taps the menu's
    // "Wallpaper" entry. Three buttons: Cancel, Default (system picker),
    // Custom (our editor).
    var showWallpaperPickDialog by remember { mutableStateOf(false) }
    // When non-null, opens the WallpaperEditorScreen on the imported source
    // and stays open until the user dismisses or applies the edit.
    var customWallpaperSourcePath by remember { mutableStateOf<String?>(null) }
    val pickCustomWallpaper = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            dropScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    WallpaperHelper.importDeviceWallpaperSource(context, uri)
                }
                if (saved != null) {
                    // Reset edits so the new image starts clean (no stale
                    // scale/crop carried over from a prior wallpaper).
                    setDeviceWallpaperEdit(context, DeviceWallpaperEdit())
                    customWallpaperSourcePath = saved
                }
            }
        }
    }
    // Auto-reopen the editor after a Preview round-trip. WallpaperPreviewBus
    // holds `pendingResumeEdit` whenever the editor's Preview button was
    // tapped; the launcher's preview-backdrop renderer (LauncherWithDrawer)
    // clears `activePreview` when the user taps Exit/Apply. Watching both
    // lets us reopen the editor on the same source the moment the preview
    // overlay is dismissed (Exit), preserving in-flight edit state.
    val previewActive = WallpaperPreviewBus.activePreview
    val pendingResume = WallpaperPreviewBus.pendingResumeEdit
    LaunchedEffect(previewActive, pendingResume) {
        if (previewActive == null && pendingResume != null && customWallpaperSourcePath == null) {
            val saved = getDeviceWallpaperSourcePath(context)
            if (saved != null) customWallpaperSourcePath = saved
        }
    }

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
                    // Apply per-app customizations so folder mini icons show custom icons/shapes/tints
                    val previewApps = folder.appPackageNames.filter { it.isNotEmpty() && it !in hiddenApps }.take(4).mapNotNull { pkg ->
                        allAvailableApps.find { it.packageName == pkg }?.let { info ->
                            val cust = appCustomizations.customizations[pkg]
                            if (cust != null) info.copy(customization = cust) else info
                        }
                    }
                    cells[folder.position] = HomeGridCell.Folder(folder, previewApps, folder.position)
                }
            }
        }

        // Place apps for this specific page only. Apps with the experimental
        // detachedFromGrid flag are skipped here so the cell stays empty;
        // they get rendered separately as a free-floating overlay (issue #48).
        homeApps.filter { it.page == page }.forEach { homeApp ->
            if (homeApp.position < totalCells) {
                val currentCell = cells[homeApp.position]
                if (currentCell is HomeGridCell.Empty) {
                    allAvailableApps.find { it.packageName == homeApp.packageName }?.let { appInfo ->
                        val cust = appCustomizations.customizations[homeApp.packageName]
                        if (cust?.detachedFromGrid == true) return@let
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

    // FIX: Exit widget resize mode as soon as the user swipes to a different page.
    // The resize overlay is bound to a single page (the widget's page); if the user
    // navigates away, resize can't be completed visually and the overlay/indicators
    // become invisible but state-active. Auto-cancel in that case.
    LaunchedEffect(pagerState.currentPage, widgetResizeState.isResizing) {
        if (!widgetResizeState.isResizing) return@LaunchedEffect
        val resizingPage = resizingWidgetFresh?.page ?: return@LaunchedEffect
        if (pagerState.currentPage != resizingPage) {
            hoveredWidgetCells = emptySet()
            widgetOriginalCells = emptySet()
            currentResizeDimensions = null
            isWidgetDropTargetValid = true
            widgetResizeState = WidgetResizeState()
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
    // Detached-from-grid apps (issue #48) keep their (position, page) row in
    // homeApps but render as a free-floating overlay — they don't claim a grid
    // cell. Every (position, page)-keyed removeAll / filter on homeApps below
    // must ignore them, or a drag-drop / remove operation on a NON-detached
    // app sharing that position would wipe the detached app's row from
    // storage and the overlay would silently disappear.
    fun isAttached(app: HomeScreenApp): Boolean =
        appCustomizations.customizations[app.packageName]?.detachedFromGrid != true

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

            // Remove both apps from homeApps (skip detached entries — they're
            // not really at that cell, see isAttached() above).
            val updatedApps = homeApps.toMutableList()
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage && isAttached(it) }
            updatedApps.removeAll { it.position == toIndex && it.page == toPage && isAttached(it) }

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
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage && isAttached(it) }
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

            // Safety: verify target position isn't already occupied in homeApps.
            // Detached-from-grid apps (issue #48) keep their original (position,
            // page) row in homeApps even though they render as a free-floating
            // overlay and their cell is treated as Empty in buildGridCellsForPage.
            // The occupancy check here must mirror that — otherwise dropping into
            // a visually-empty cell that was once a detached app's home position
            // gets rejected with "already occupied".
            val targetOccupied = updatedApps.any {
                it.position == toIndex && it.page == toPage &&
                    appCustomizations.customizations[it.packageName]?.detachedFromGrid != true
            }
            if (targetOccupied) {
                android.util.Log.w("FolderDrop", "handleDrop: target position $toIndex on page $toPage already occupied, aborting move")
                return
            }

            // Remove app from old position and page (only the attached entry —
            // a detached app sharing the source cell stays put).
            updatedApps.removeAll { it.position == fromIndex && it.page == fromPage && isAttached(it) }

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
        // Multi-page-dock-aware: target the currently visible dock page.
        val existingDockApp = dockApps.find { it.position == toDockSlot && it.page == currentDockPage }
        val existingDockFolder = dockFolders.find { it.position == toDockSlot && it.page == currentDockPage }

        // Find the source app from homeApps (page-aware)
        val sourceHomeApp = homeApps.find { it.position == fromGridIndex && it.page == fromPage }
        val sourceAppInfo = sourceHomeApp?.let { ha ->
            allAvailableApps.find { it.packageName == ha.packageName }
        }
        if (sourceAppInfo == null) return

        val updatedGridApps = homeApps.filter { !(it.position == fromGridIndex && it.page == fromPage && isAttached(it)) }

        if (existingDockApp == null && existingDockFolder == null) {
            // Empty slot → place app
            val updatedDockApps = dockApps + DockApp(sourceAppInfo.packageName, toDockSlot, page = currentDockPage)
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
            val updatedDockApps = dockApps.filter { !(it.position == toDockSlot && it.page == currentDockPage) }
            val newFolder = DockFolder(
                name = "Folder",
                position = toDockSlot,
                appPackageNames = listOf(existingDockApp.packageName, sourceAppInfo.packageName),
                page = currentDockPage
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
        val dockApp = dockApps.find { it.position == fromDockSlot && it.page == currentDockPage }
        val toCell = gridCells.getOrNull(toGridIndex)

        if (dockApp != null) {
            val appInfo = allAvailableApps.find { it.packageName == dockApp.packageName }
            if (appInfo != null) {
                // Drop on empty cell → standard move
                if (toCell is HomeGridCell.Empty) {
                    val updatedDockApps = dockApps.filter { !(it.position == fromDockSlot && it.page == currentDockPage) }
                    val updatedGridApps = homeApps + HomeScreenApp(dockApp.packageName, toGridIndex, currentPage)

                    homeApps = updatedGridApps
                    dockApps = updatedDockApps
                    dropScope.launch(Dispatchers.IO) {
                        saveHomeScreenData(context, HomeScreenData(apps = updatedGridApps, dockApps = updatedDockApps, folders = homeFolders, dockFolders = dockFolders))
                    }
                }
                // Drop on app → create folder
                else if (toCell is HomeGridCell.App) {
                    val updatedDockApps = dockApps.filter { !(it.position == fromDockSlot && it.page == currentDockPage) }
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
                    val updatedDockApps = dockApps.filter { !(it.position == fromDockSlot && it.page == currentDockPage) }
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

            // Drop where it actually landed. If the drop resolves to the
            // source folder's own cell, targetCell is that Folder → the Folder
            // branch re-adds it; anywhere else → that cell; unresolved → first
            // empty cell. NOTE: do NOT special-case "near the folder" or
            // default unresolved drops back to the source folder — right after
            // escape the drop cell briefly resolves to null, and those
            // fallbacks made the app snap back into the folder even when
            // dropped clearly away (had to wait for it to settle).
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
                                homeApps = homeApps.filter { !(it.position == effectiveIndex && it.page == intendedPage && isAttached(it)) }
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
                val existDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                val existDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
                val existingDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                val existingDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
                                appPackageNames = folderData.appPackageNames,
                                page = currentDockPage
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
            val existDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
            val existDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
            val existingDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
            val existingDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
            val existingDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
            val existingDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
            val isSlotEmpty = existingDockApp == null && existingDockFolder == null
            if (dockPos != null) {
                targetOffset = Offset(dockPos.x - originalPos.x, dockPos.y - originalPos.y)
                if (isSlotEmpty) {
                    dropAction = {
                        when (item) {
                            is com.bearinmind.launcher314.data.AppInfo -> {
                                dockApps = dockApps + DockApp(
                                    packageName = item.packageName,
                                    position = targetDockSlot,
                                    page = currentDockPage
                                )
                                saveAllData()
                            }
                            is com.bearinmind.launcher314.data.AppFolder -> {
                                dockFolders = dockFolders + DockFolder(
                                    name = item.name,
                                    position = targetDockSlot,
                                    appPackageNames = item.appPackageNames,
                                    page = currentDockPage
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
                        val updatedDockApps = dockApps.filter { !(it.position == targetDockSlot && it.page == currentDockPage) }
                        val newFolder = DockFolder(
                            name = "Folder",
                            position = targetDockSlot,
                            appPackageNames = listOf(existingDockApp.packageName, item.packageName),
                            page = currentDockPage
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
                    updatedApps.removeAll { it.position == targetGridCell && it.page == intendedPage && isAttached(it) }
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
                val existingDockApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                val existingDockFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
                existingDockApp != null && existingDockFolder == null
            } else if (targetGridCell != null) {
                val pageCells = buildGridCellsForPage(intendedPage)
                pageCells.getOrNull(targetGridCell) is HomeGridCell.App
            } else false
        } else false

        // Determine if dropping onto an existing folder (dock or grid)
        val dropsOntoFolder = if (item is com.bearinmind.launcher314.data.AppInfo) {
            if (targetDockSlot != null) dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage } != null
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
            .onGloballyPositioned { rootBoxTopY = it.positionInRoot().y }
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
                                isHoveredDockSlotValid = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage } == null
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
            .pointerInput(editingPackageName) {
                // Skip while a detached icon is in edit mode — a stray
                // double-tap during free-move/resize would otherwise fire
                // the user's double-tap action (e.g. open an app).
                if (editingPackageName != null) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        // Issue #40: dispatch via the user-assigned action.
                        // Falls back to the legacy lock-screen behavior if
                        // the gestureUiCallbacks aren't wired (e.g. preview).
                        if (!com.bearinmind.launcher314.data.getGestureEnabled(
                                context,
                                com.bearinmind.launcher314.data.GestureId.DOUBLE_TAP
                            )) return@detectTapGestures
                        val action = com.bearinmind.launcher314.data.getGestureAction(
                            context,
                            com.bearinmind.launcher314.data.GestureId.DOUBLE_TAP
                        )
                        if (gestureUiCallbacks != null) {
                            action.dispatch(context, gestureUiCallbacks)
                        } else if (action is com.bearinmind.launcher314.data.GestureAction.LockScreen) {
                            AppDrawerAccessibilityService.lockScreen(context)
                        }
                    }
                )
            }
            // Issue #40: swipe-right detector. Fires only on home page 0 (where
            // the HorizontalPager has nothing to scroll to leftward), so it
            // never collides with normal page-changes. On any other page, the
            // pager handles the horizontal drag normally and this block bails.
            // Also bails while a detached icon is in edit mode — a horizontal
            // drag on the icon body (free-move) would otherwise fire the
            // swipe-right action on release and launch the assigned app.
            .pointerInput(isEditMode, isWidgetBeingDragged, widgetResizeState.isResizing, editingPackageName) {
                if (isEditMode || isWidgetBeingDragged || widgetResizeState.isResizing || editingPackageName != null) {
                    return@pointerInput
                }
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (pagerState.currentPage != 0) return@awaitEachGesture
                    if (gestureUiCallbacks == null) return@awaitEachGesture
                    // Bail if the touch starts inside the dock — the dock has its
                    // own HorizontalPager for paging dock items and we don't want
                    // those page-change swipes to also fire the swipe-right action.
                    val touchRootY = down.position.y + rootBoxTopY
                    if (touchRootY >= dockTopY) return@awaitEachGesture
                    if (!com.bearinmind.launcher314.data.getGestureEnabled(
                            context,
                            com.bearinmind.launcher314.data.GestureId.SWIPE_RIGHT
                        )) return@awaitEachGesture
                    val action = com.bearinmind.launcher314.data.getGestureAction(
                        context,
                        com.bearinmind.launcher314.data.GestureId.SWIPE_RIGHT
                    )
                    if (action is com.bearinmind.launcher314.data.GestureAction.None) {
                        return@awaitEachGesture
                    }

                    val startX = down.position.x
                    val startY = down.position.y
                    var maxDx = 0f
                    var committed = false
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        // Respect consume: if a child handler (a detached icon
                        // being long-press-dragged, the icon-edit drag handler,
                        // a widget, anything) has claimed this gesture, bail
                        // BEFORE we commit. Without this check the swipe-right
                        // action would fire on release even when the user was
                        // really just dragging a detached icon rightward.
                        if (change.isConsumed && !committed) {
                            return@awaitEachGesture
                        }
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (!committed) {
                            // Bail if drag becomes vertical (swipe-up etc.) or if
                            // we left page 0 mid-gesture (a paging swipe started).
                            if (kotlin.math.abs(dy) > touchSlop &&
                                kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                return@awaitEachGesture
                            }
                            if (pagerState.currentPage != 0) return@awaitEachGesture
                            if (dx > touchSlop) {
                                committed = true
                                change.consume()
                            }
                        } else {
                            change.consume()
                        }
                        if (dx > maxDx) maxDx = dx
                        if (!change.pressed) break
                    } while (true)

                    val threshold = size.width * 0.20f
                    if (committed && maxDx > threshold) {
                        action.dispatch(context, gestureUiCallbacks)
                    }
                }
            }
            // FIX: Tap anywhere on the home screen background to exit widget resize
            // mode. Previously resize mode persisted indefinitely (even across GitHub
            // trips) because nothing outside the resize overlay dismissed it.
            .pointerInput(widgetResizeState.isResizing) {
                if (widgetResizeState.isResizing) {
                    detectTapGestures(
                        onTap = {
                            hoveredWidgetCells = emptySet()
                            widgetOriginalCells = emptySet()
                            currentResizeDimensions = null
                            isWidgetDropTargetValid = true
                            widgetResizeState = WidgetResizeState()
                        }
                    )
                }
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
                    // Custom snap spec for the home page swipe — matches
                    // AOSP Launcher3 / Lawnchair's Workspace exactly:
                    //   curve: f(t) = (t-1)^5 + 1  (quintic ease-out — fast
                    //          start, decelerates aggressively to a soft
                    //          landing). From AOSP Interpolators.SCROLL.
                    //   duration: 750 ms (phone). From Trebuchet's
                    //          config_pageSnapAnimationDuration.
                    // Default decay path is preserved so a hard flick still
                    // flings naturally before the snap takes over.
                    val homePageSnapSpec = remember {
                        val scrollEasing = androidx.compose.animation.core.Easing { t ->
                            val u = t - 1f
                            u * u * u * u * u + 1f
                        }
                        tween<Float>(durationMillis = 750, easing = scrollEasing)
                    }
                    val homePagerFlingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = homePageSnapSpec
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = homePagerFlingBehavior,
                        // Disable manual swipe during drag, when a detached
                        // icon is in edit mode, or when widgets are being
                        // manipulated.
                        userScrollEnabled = draggedItemIndex == null && !isDropAnimating && !externalDragActive && !isWidgetBeingDragged && !isStackSwipeActive && editingPackageName == null
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
                                    val widgetHoverPage = if (widgetResizeState.isResizing) resizingWidgetPage
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
                                        // Only on the SOURCE/resize page — on other pages, same indices are different cells.
                                        // FIX: During resize, widgetDragSourcePage may be stale from a prior drag; use
                                        // widgetHoverPage (which points at resizingWidgetPage during resize) so the
                                        // "original cells" highlight only appears on the widget's actual page.
                                        hoveredWidgetCells.contains(index) && widgetOriginalCells.contains(index) && page == widgetHoverPage -> true
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
                                            onFolderIconPositioned = { bounds ->
                                                folderIconBoundsMap[index] = bounds
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
                                                    // Hovering over dock - check if slot is empty (on the visible page)
                                                    val dockSlotApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                                                    val dockSlotFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
                                                } else if (cell is HomeGridCell.App && !isEditMode && editingPackageName == null) {
                                                    // Don't launch the underlying grid app when a
                                                    // detached icon is in edit mode — a tap on this
                                                    // cell is almost always a misfire from the user
                                                    // trying to grab a stretched detached icon whose
                                                    // visual extent overflows its outer-Box hit area.
                                                    launchApp(context, cell.appInfo.packageName, cell.appInfo.userSerial)
                                                } else if (cell is HomeGridCell.Folder && !isEditMode && editingPackageName == null) {
                                                    openHomeFolder = cell.folder
                                                }
                                            },
                                            onLongPress = { touchPosition ->
                                                // While a detached icon is in edit mode, modal
                                                // semantics apply — a long-press on a grid cell
                                                // must NOT pop up the launcher menu or enter the
                                                // grid edit mode. The user has to tap-outside
                                                // first to exit the detached icon's edit mode.
                                                if (editingPackageName == null) {
                                                    if (cell is HomeGridCell.Empty) {
                                                        launcherMenuPosition = touchPosition
                                                        showLauncherSettingsMenu = true
                                                    } else if (cell is HomeGridCell.App) {
                                                        isEditMode = true
                                                    } else if (cell is HomeGridCell.Folder) {
                                                        isEditMode = true
                                                    }
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
                                                            saveHomeApps(homeApps.filter { !(it.position == cell.position && it.page == page && isAttached(it)) })
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
                                                        updatedApps.removeAll { it.page == page && it.position == pos && isAttached(it) }
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
                                        var showWidgetCustomize by remember { mutableStateOf(false) }
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

                                                        // FIX: Block the widget's own long-press + drag handler while
                                                        // any resize is active — previously the widget could still be
                                                        // dragged during resize, leaving both overlays live at once.
                                                        if (widgetResizeState.isResizing) {
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
                                            // Per-widget corner override wins regardless of the global
                                            // toggle; null falls back to the global enabled+radius.
                                            val effectiveCornerRadiusDp = widget.cornerRadiusPercent
                                                ?.let { it / 100f * WIDGET_MAX_CORNER_RADIUS_DP }
                                                ?: if (widgetRoundedCornersEnabled) {
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
                                                    // Slideshow auto-advance — when the stack's
                                                    // slideshow flag is on, CROSSFADE to the next
                                                    // member every N seconds (no horizontal swipe
                                                    // animation). Paused while the user has the
                                                    // context menu open or is dragging a widget.
                                                    val slideshowEnabled = widget.stackSlideshowEnabled
                                                    val slideshowIntervalMs = widget.stackSlideshowIntervalSec.coerceIn(5, 65) * 1000L
                                                    val slideshowAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
                                                    LaunchedEffect(
                                                        slideshowEnabled,
                                                        slideshowIntervalMs,
                                                        stackWidgets.size,
                                                        showWidgetMenu,
                                                        isWidgetBeingDragged
                                                    ) {
                                                        // ALWAYS restore visibility on (re)start. This
                                                        // effect restarts whenever a key flips (menu
                                                        // open/close, slideshow settings change) — if
                                                        // that restart cancels a fade MID-TRANSITION at
                                                        // alpha ~0, the stack would stay INVISIBLE until
                                                        // a future tick completed a full fade cycle
                                                        // (the "stack is blank until I click around"
                                                        // bug). Snapping to 1 here makes every restart
                                                        // begin from a visible pager.
                                                        slideshowAlpha.snapTo(1f)
                                                        if (!slideshowEnabled) return@LaunchedEffect
                                                        if (stackWidgets.size <= 1) return@LaunchedEffect
                                                        while (true) {
                                                            kotlinx.coroutines.delay(slideshowIntervalMs)
                                                            // Don't fight the user: skip the tick
                                                            // if the menu is open or any widget is
                                                            // being dragged.
                                                            if (showWidgetMenu || isWidgetBeingDragged) continue
                                                            // Fade out current → instant jump to
                                                            // next → fade in. Total ~600 ms.
                                                            slideshowAlpha.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = androidx.compose.animation.core.tween(300)
                                                            )
                                                            val next = (stackPagerState.currentPage + 1) % stackWidgets.size
                                                            stackPagerState.scrollToPage(next)
                                                            slideshowAlpha.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = androidx.compose.animation.core.tween(300)
                                                            )
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
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                // Slideshow crossfade alpha — wraps
                                                                // only the pager content (not the
                                                                // chrome / dots) so just the widget
                                                                // fades during auto-advance.
                                                                .graphicsLayer { alpha = slideshowAlpha.value },
                                                            userScrollEnabled = !isThisWidgetDragging
                                                        ) { stackPage ->
                                                            WidgetHostView(
                                                                placedWidget = stackWidgets[stackPage],
                                                                modifier = Modifier.fillMaxSize(),
                                                                cornerRadiusDp = effectiveCornerRadiusDp,
                                                                viewRefreshKey = widgetViewRefreshKeys[stackWidgets[stackPage].appWidgetId] ?: 0,
                                                                isInStack = true,
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

                                                            // (Per-widget text size + spacing sliders
                                                            // moved into the Customize popup below —
                                                            // same style as the per-app customize.)

                                                            // (Widget slideshow toggle + interval moved
                                                            // into the Customize popup below, alongside
                                                            // the text size + spacing sliders.)

                                                            // Customize option — opens the per-widget
                                                            // customize popup (text size + spacing),
                                                            // same style as the per-app customize.
                                                            DropdownMenuItem(
                                                                text = { Text("Customize") },
                                                                onClick = {
                                                                    showWidgetMenu = false
                                                                    showWidgetCustomize = true
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Edit,
                                                                        contentDescription = null
                                                                    )
                                                                }
                                                            )

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

                                                // Per-widget customize popup (text size + spacing),
                                                // same style as the per-app customize dialog.
                                                if (showWidgetCustomize) {
                                                    com.bearinmind.launcher314.ui.widgets.WidgetCustomizeDialog(
                                                        initialFontScalePercent = widget.fontScalePercent,
                                                        initialPaddingPercent = widget.paddingPercent,
                                                        globalFontScalePercent = globalWidgetFontScalePercent,
                                                        globalPaddingPercent = globalWidgetPaddingPercent,
                                                        initialCornerRadiusPercent = widget.cornerRadiusPercent,
                                                        // Effective global roundness: 0 when toggle off
                                                        globalCornerRadiusPercent =
                                                            if (widgetRoundedCornersEnabled) widgetCornerRadiusPercent else 0,
                                                        isStack = widget.stackId != null,
                                                        initialSlideshowEnabled = widget.stackSlideshowEnabled,
                                                        initialSlideshowIntervalSec = widget.stackSlideshowIntervalSec,
                                                        onSaveSlideshow = { enabled, intervalSec ->
                                                            widget.stackId?.let { sid ->
                                                                placedWidgets = WidgetManager.setStackSlideshow(
                                                                    context, placedWidgets, sid, enabled, intervalSec
                                                                )
                                                            }
                                                        },
                                                        onSave = { fs, pad, corner ->
                                                            placedWidgets = placedWidgets.map {
                                                                if (it.appWidgetId == widget.appWidgetId) {
                                                                    it.copy(
                                                                        fontScalePercent = fs,
                                                                        paddingPercent = pad,
                                                                        cornerRadiusPercent = corner
                                                                    )
                                                                } else it
                                                            }
                                                            WidgetManager.savePlacedWidgets(context, placedWidgets)
                                                            WidgetManager.recreateWidgetView(context, widget.appWidgetId)
                                                            widgetViewRefreshKeys = widgetViewRefreshKeys +
                                                                (widget.appWidgetId to ((widgetViewRefreshKeys[widget.appWidgetId] ?: 0) + 1))
                                                            showWidgetCustomize = false
                                                        },
                                                        onDismiss = { showWidgetCustomize = false }
                                                    )
                                                }
                                        }
                                    }
                                }
                                    } // key
                            }

                            // Widget resize overlay - shows preview outline with draggable handles
                            // Only render on the page where the widget lives.
                            // FIX: Use fresh widget lookup (resizingWidgetFresh) so a post-drag page
                            // change is reflected — previously the overlay could render on the old page.
                            if (widgetResizeState.isResizing && resizingWidgetFresh != null && page == resizingWidgetFresh.page) {
                                val resizingWidget = resizingWidgetFresh

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

                            // Detached (free-floating) icons for this page — issue #48.
                            // Apps whose customization.detachedFromGrid == true skip
                            // the grid render path (see buildGridCellsForPage) and
                            // get drawn here at their saved (detachedX, detachedY).
                            // Long-press initiates a drag; on release the new
                            // position is persisted back into AppCustomization.
                            val detachedAppsForPage = remember(homeApps, allAvailableApps, appCustomizations, page) {
                                homeApps.filter { it.page == page }.mapNotNull { homeApp ->
                                    val cust = appCustomizations.customizations[homeApp.packageName]
                                    if (cust?.detachedFromGrid != true) return@mapNotNull null
                                    val info = allAvailableApps.find { it.packageName == homeApp.packageName }
                                        ?: return@mapNotNull null
                                    Triple(homeApp, info.copy(customization = cust), cust)
                                }
                            }
                            val detachedHaptic = rememberHapticFeedback()
                            // Active-edit overlay state for the Total-Launcher-style
                            // bracket + crosshair + coord labels. Per-page (only the
                            // page containing the editing icon renders the overlay).
                            // editingPackageName itself is hoisted to LauncherScreen
                            // scope so the pager swipe can be locked while editing.
                            var activeEditBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                            // Icon's geometric centre in page coords (NOT the
                            // bounding-box centre, which now drifts when the
                            // box is widened to wrap a wide label). Drives the
                            // crosshair anchor in the Canvas.
                            var activeEditIconCenter by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                            // In-flight resize multipliers — independent X and Y
                            // so edge handles can stretch the icon along one
                            // axis only (true widget-style edge resize). Reset
                            // to 1.0 when the drag ends; the final values are
                            // committed into detachedScaleX / detachedScaleY.
                            var activeResizeMultiplierX by remember { mutableStateOf(1f) }
                            var activeResizeMultiplierY by remember { mutableStateOf(1f) }
                            // Which of the 8 handles (0..7, indices listed
                            // below) is currently being dragged, or null. The
                            // active one is drawn larger + white in the Canvas
                            // (the ripple highlight) and adjacent edges fade
                            // toward white as well, mirroring WidgetResize.
                            var activeResizeHandle by remember { mutableStateOf<Int?>(null) }
                            // In-flight position shift during a handle resize.
                            // Keeps the OPPOSITE handle's edge / corner pinned
                            // by translating the icon by -anchor * halfSize *
                            // (multiplier - 1). Applied to graphicsLayer
                            // translation AND folded into updateEditBounds so
                            // the bracket follows the icon's visual position.
                            // Commits into detachedX/Y on release.
                            var activeResizeShift by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                            // Edit-mode modal blocker — rendered BEFORE the
                            // detached icons so the icons sit on top of it for
                            // pointer-dispatch priority. Consumes every event
                            // that lands OUTSIDE the editing icon's bracket
                            // area, blocking parent gestures (swipe-up for
                            // drawer, swipe-down for notifications, page-pager
                            // swipes, taps on other apps/widgets). Touches
                            // inside the icon are passed through (no consume),
                            // so the icon's own pointerInput handles drag. A
                            // tap outside that ends without dragging exits
                            // edit mode.
                            if (editingPackageName != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(editingPackageName) {
                                            // Resize-handle hit zones extend ~18dp past the
                                            // bracket on every edge. Treat that ring as "inside"
                                            // so the blocker doesn't consume handle touches.
                                            val handlePadPx = with(density) { 18.dp.toPx() }
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val bounds = activeEditBounds
                                                if (bounds != null) {
                                                    val expandedBounds = androidx.compose.ui.geometry.Rect(
                                                        bounds.left - handlePadPx,
                                                        bounds.top - handlePadPx,
                                                        bounds.right + handlePadPx,
                                                        bounds.bottom + handlePadPx
                                                    )
                                                    if (expandedBounds.contains(down.position)) {
                                                        // Inside icon or its handle ring —
                                                        // don't consume; let the icon's drag
                                                        // handler / the handle's drag handler
                                                        // claim it.
                                                        return@awaitEachGesture
                                                    }
                                                }
                                                // Outside icon — fully claim this
                                                // gesture so no parent handler fires.
                                                down.consume()
                                                val touchSlop = viewConfiguration.touchSlop
                                                val startPos = down.position
                                                var moved = false
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id }
                                                        ?: break
                                                    if (change.pressed) {
                                                        val dx = change.position.x - startPos.x
                                                        val dy = change.position.y - startPos.y
                                                        if (kotlin.math.sqrt(dx * dx + dy * dy) > touchSlop) {
                                                            moved = true
                                                        }
                                                        change.consume()
                                                    } else {
                                                        if (!moved) {
                                                            // Tap outside — exit edit mode.
                                                            editingPackageName = null
                                                            activeEditBounds = null
                                                            activeEditIconCenter = null
                                                        }
                                                        change.consume()
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                )
                            }

                            // Defer rendering until the grid has measured cellSize so
                            // we have a real cell pixel size to anchor "detach in place".
                            if (cellSize != IntSize.Zero) {
                                detachedAppsForPage.forEach { (homeApp, appInfo, cust) ->
                                    key(homeApp.packageName) {
                                        // Default position = the icon's original grid cell
                                        // (top-left in pixels within the grid Box) so toggling
                                        // detach makes the icon stay in place visually.
                                        val defaultX = (homeApp.position % gridColumns) * cellSize.width.toFloat()
                                        val defaultY = (homeApp.position / gridColumns) * cellSize.height.toFloat()
                                        // Derive position DIRECTLY from cust so the icon's layout
                                        // position updates in the same recomp as the rest of the
                                        // committed state. Wrapped in rememberUpdatedState so the
                                        // gesture coroutine's `updateEditBounds()` calls (which
                                        // run from the captured OLD closure when the coroutine
                                        // doesn't restart) still see the latest cust values — if
                                        // we left these as plain vals the OLD closure would
                                        // compute bracket bounds at the PRE-commit position, then
                                        // the per-icon LaunchedEffect would correct them one
                                        // frame later, producing the crosshair ghost flash.
                                        val posX by rememberUpdatedState(cust.detachedX ?: defaultX)
                                        val posY by rememberUpdatedState(cust.detachedY ?: defaultY)
                                        // In-flight drag offset, applied visually via
                                        // graphicsLayer.translation so the outer Box's
                                        // layout position stays stable for the duration of
                                        // the gesture (otherwise positionChange() cancels
                                        // out as the layout shifts under the pointer).
                                        var dragOffsetX by remember { mutableStateOf(0f) }
                                        var dragOffsetY by remember { mutableStateOf(0f) }
                                        var isDragging by remember { mutableStateOf(false) }
                                        var showContextMenu by remember { mutableStateOf(false) }
                                        var iconBoundsInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
                                        var isFingerDown by remember { mutableStateOf(false) }
                                        var flashOverlay by remember { mutableStateOf(false) }
                                        val isCustomizingThis = customizingApp?.packageName == homeApp.packageName
                                        val inEditMode = editingPackageName == homeApp.packageName
                                        // Read the live inEditMode via rememberUpdatedState
                                        // so the pointerInput coroutine doesn't have to
                                        // restart (and lose in-flight pointers) when the
                                        // user enters edit mode after a drag.
                                        val inEditModeState = rememberUpdatedState(inEditMode)
                                        // Live editingPackageName for the gesture coroutine.
                                        // When ANOTHER detached icon is in edit mode, this
                                        // icon's gesture handler must bail so the user is
                                        // forced to tap-outside first (modal-edit semantics).
                                        val editingPackageNameState = rememberUpdatedState(editingPackageName)
                                        // Detached icons NEVER scale up via interaction state — the
                                        // crosshair + bracket are the visual feedback, and the
                                        // per-app stored stretch is the user-controlled size.
                                        // Adding a 1.265× pop on long-press / drag / customize
                                        // makes the icon jump around relative to its committed
                                        // bracket bounds, which the user explicitly doesn't want.
                                        val effectiveScale = 1f
                                        val flashAlpha by animateFloatAsState(
                                            targetValue = if (flashOverlay) 0.4f else 0f,
                                            animationSpec = if (flashOverlay) tween(80) else tween(150),
                                            label = "detachedFlash_${homeApp.packageName}",
                                            finishedListener = { if (flashOverlay) flashOverlay = false }
                                        )
                                        val overlayAlpha = maxOf(if (isFingerDown) 0.25f else 0f, flashAlpha)
                                        // Measure the label text with the SAME style the actual
                                        // OverlayAppContent uses (font size + font family) so
                                        // the bounding box matches the rendered width. Without
                                        // the font family the measurement under-reports for
                                        // any custom font that's wider than the default.
                                        val labelTextForBox = appInfo.customization?.customLabel?.takeIf { it.isNotEmpty() }
                                            ?: appInfo.name
                                        val labelFontSizeForBox = appInfo.customization?.iconTextSizePercent?.let { 12.sp * it / 100f }
                                            ?: gridAppNameFont
                                        val labelFontFamilyForBox = appInfo.customization?.labelFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: selectedFontFamily ?: androidx.compose.ui.text.font.FontFamily.Default
                                        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                                        // Measure with the EXACT typography the actual Text
                                        // composable uses (bodySmall with custom fontSize +
                                        // fontFamily). bodySmall sets lineHeight = 16.sp at
                                        // 12.sp font, so the rendered height is ~33% taller
                                        // than the raw font-metric height — without using
                                        // the same style the bounding box under-runs the
                                        // label and the bracket cuts off below the text.
                                        val bodySmallStyle = MaterialTheme.typography.bodySmall
                                        val labelMeasured = remember(labelTextForBox, labelFontSizeForBox, labelFontFamilyForBox, bodySmallStyle) {
                                            textMeasurer.measure(
                                                text = labelTextForBox,
                                                style = bodySmallStyle.copy(
                                                    fontSize = labelFontSizeForBox,
                                                    fontFamily = labelFontFamilyForBox
                                                )
                                            )
                                        }
                                        val measuredLabelWidthPx = labelMeasured.size.width.toFloat()
                                        val measuredLabelHeightPx = labelMeasured.size.height.toFloat()

                                        // ── Bounds-calc constants (hoisted to Composable scope so
                                        // the LaunchedEffect below + the pointerInput can both
                                        // use them). ─────────────────────────────────────────────
                                        val perAppPctOuter = appInfo.customization?.iconSizePercent ?: iconSizePercent
                                        val iconSquarePxOuter = with(density) {
                                            (iconSizeDp * perAppPctOuter / iconSizePercent.toFloat()).dp.toPx()
                                        }
                                        // Text-as-icon footprint: measure the actual glyph using
                                        // the SAME font + SP the renderer applies, so the bracket
                                        // wraps the visible text (e.g. "morphe" overflows the
                                        // icon-square otherwise). Image mode keeps the square.
                                        val iconTextString = appInfo.customization?.iconText?.takeIf { it.isNotBlank() }
                                        val iconTextSpForBox = appInfo.customization?.iconAsTextSizeSp ?: 28
                                        // Mirror OverlayAppContent's font fallback EXACTLY:
                                        //   iconTextFontId → (labelFontId-as-perAppFontFamily) → global.
                                        // Skipping the label-font step here made the TextMeasurer
                                        // produce a narrower width than the actual rendered glyph
                                        // when the user has a per-app label font set, so the
                                        // bracket fell short of the visible text.
                                        val perAppLabelFontFamilyForBox = appInfo.customization?.labelFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: selectedFontFamily ?: androidx.compose.ui.text.font.FontFamily.Default
                                        val iconTextFontFamilyForBox = appInfo.customization?.iconTextFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: perAppLabelFontFamilyForBox
                                        val iconTextMeasured = remember(iconTextString, iconTextSpForBox, iconTextFontFamilyForBox) {
                                            iconTextString?.let { txt ->
                                                textMeasurer.measure(
                                                    text = txt,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        fontSize = iconTextSpForBox.sp,
                                                        fontFamily = iconTextFontFamilyForBox
                                                    )
                                                )
                                            }
                                        }
                                        val iconWidthPxOuter = iconTextMeasured?.size?.width?.toFloat() ?: iconSquarePxOuter
                                        val iconHeightPxOuter = iconTextMeasured?.size?.height?.toFloat() ?: iconSquarePxOuter
                                        // Image-mode also keeps a single iconPx for backwards-compat
                                        // with the resize-handle code which uses one scalar.
                                        val iconPxOuter = iconSquarePxOuter
                                        // In text mode the label is suppressed (see OverlayAppContent)
                                        // so the bracket has no label area below the glyph — zero out
                                        // the label dims so computeEditBoundsAndCenter doesn't carve
                                        // out empty space for it.
                                        val labelOffsetPxOuter = if (iconTextString != null) 0f
                                            else with(density) { gridIconTextSpacer.toPx() } + measuredLabelHeightPx
                                        val cappedLabelWidthOuter = if (iconTextString != null) 0f
                                            else kotlin.math.min(measuredLabelWidthPx, cellSize.width.toFloat())
                                        val boundsPaddingPxOuter = with(density) { 12.dp.toPx() }
                                        // Edit mode no longer scales the icon up (the crosshair
                                        // is the visual cue), so the bracket / icon-visual scale
                                        // factor is just 1.0 — only the per-axis stored stretch
                                        // and in-flight resize multiplier change the icon size.
                                        val editScaleOuter = 1f

                                        // Stored stretch multipliers from prior commits (null = 1.0).
                                        // Wrapped in rememberUpdatedState for the same reason as
                                        // posX/posY above — the gesture coroutine's bounds calc
                                        // needs to see fresh stored scales after a commit, not
                                        // the stale ones captured at coroutine creation time.
                                        val storedScaleX by rememberUpdatedState(appInfo.customization?.detachedScaleX ?: 1f)
                                        val storedScaleY by rememberUpdatedState(appInfo.customization?.detachedScaleY ?: 1f)

                                        fun computeEditBoundsAndCenter(): Pair<androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Offset> {
                                            val rx = if (inEditMode) activeResizeMultiplierX else 1f
                                            val ry = if (inEditMode) activeResizeMultiplierY else 1f
                                            val combinedScaleX = editScaleOuter * storedScaleX * rx
                                            val combinedScaleY = editScaleOuter * storedScaleY * ry
                                            // Use the text-measured dims in text mode so the bracket
                                            // wraps a wide glyph like "morphe"; image mode keeps the
                                            // icon-square footprint.
                                            val effIconWidth = iconWidthPxOuter * combinedScaleX
                                            val effIconHeight = iconHeightPxOuter * combinedScaleY
                                            // Label sits below the icon at scaled spacing — uses Y
                                            // scale only since the label is vertically below icon.
                                            val effLabelOffset = labelOffsetPxOuter * combinedScaleY
                                            val effLabelWidth = cappedLabelWidthOuter * combinedScaleX
                                            // Resize shift pins the opposite edge / corner during
                                            // a handle drag — fold into the visual center so the
                                            // bracket follows the icon, not the unshifted layout.
                                            val shiftX = if (inEditMode) activeResizeShift.x else 0f
                                            val shiftY = if (inEditMode) activeResizeShift.y else 0f
                                            val cellCenterY = posY + dragOffsetY + shiftY + cellSize.height / 2f
                                            val cx = posX + dragOffsetX + shiftX + cellSize.width / 2f
                                            val cy = cellCenterY - effLabelOffset / 2f
                                            val widthPx = kotlin.math.max(effIconWidth, effLabelWidth) + boundsPaddingPxOuter
                                            // Symmetric vertical padding so the label can't
                                            // ever spill out the bottom due to small label-
                                            // metric vs. rendered-height drift, and a matching
                                            // sliver above the icon for visual balance.
                                            val vPad = boundsPaddingPxOuter / 2f
                                            return androidx.compose.ui.geometry.Rect(
                                                left = cx - widthPx / 2f,
                                                top = cy - effIconHeight / 2f - vPad,
                                                right = cx + widthPx / 2f,
                                                bottom = cy + effIconHeight / 2f + effLabelOffset + vPad
                                            ) to androidx.compose.ui.geometry.Offset(cx, cy)
                                        }

                                        // (Bounds-update LaunchedEffect removed — all writes to
                                        // activeEditBounds / activeEditIconCenter now happen
                                        // INLINE from the gesture handlers:
                                        //   - icon move drag: updateEditBounds() during drag,
                                        //     commitDragEnd inline write on release
                                        //   - resize handle drag: inline write in onDrag, plus
                                        //     a final inline write in changedToUp
                                        // The LaunchedEffect was racing those inline writes — it
                                        // ran after recomposition and wrote bounds that, while
                                        // mathematically identical, came from a slightly
                                        // different code path. Even with mutableStateOf's
                                        // equality check, the extra write was producing a
                                        // perceptible split-second ghost on resize commit.)
                                        // In text mode the visible glyph ("morphe") can be
                                        // wider/taller than the icon-square (cellSize), so the
                                        // pointer-detection Box has to grow to cover the visible
                                        // text — otherwise a tap on the overflow part of the
                                        // glyph never registers. Position is shifted by half the
                                        // size delta so the icon's center stays in the same
                                        // visual spot (the bracket calc still uses cellSize.w/2
                                        // to find the icon center, which matches because the
                                        // inner Box is fillMaxSize and the OverlayAppContent
                                        // Column is centered in it).
                                        val isTextModeForLayout = appInfo.customization?.iconText?.isNotBlank() == true
                                        val tapPadPx = with(density) { 12.dp.toPx() }
                                        val outerWPx = if (isTextModeForLayout) {
                                            kotlin.math.max(cellSize.width.toFloat(), iconWidthPxOuter * storedScaleX + tapPadPx * 2)
                                        } else cellSize.width.toFloat()
                                        val outerHPx = if (isTextModeForLayout) {
                                            kotlin.math.max(cellSize.height.toFloat(), iconHeightPxOuter * storedScaleY + tapPadPx * 2)
                                        } else cellSize.height.toFloat()
                                        val outerOffXPx = posX - (outerWPx - cellSize.width) / 2f
                                        val outerOffYPx = posY - (outerHPx - cellSize.height) / 2f
                                        Box(
                                            modifier = Modifier
                                                .offset { IntOffset(outerOffXPx.toInt(), outerOffYPx.toInt()) }
                                                .size(
                                                    width = with(LocalDensity.current) { outerWPx.toDp() },
                                                    height = with(LocalDensity.current) { outerHPx.toDp() }
                                                )
                                                // NOTE: graphicsLayer scale is applied to the
                                                // INNER visual Box below, not here. Putting it on
                                                // the same Box as pointerInput would inverse-
                                                // transform pointer deltas (a 10 px finger move
                                                // would land as ~7.9 px in layout space at 1.265×
                                                // scale), making the icon stutter behind the
                                                // finger during drag. See drag-patterns memory
                                                // note on `graphicsLayer scale distorts pointer
                                                // deltas`.
                                                .pointerInput(homeApp.packageName) {
                                                    val touchSlop = viewConfiguration.touchSlop
                                                    // Helper: compute the icon's visible bounds
                                                    // (in PAGE coords) given the current pos +
                                                    // drag offset. Used to update the editing
                                                    // overlay (corner brackets + crosshair).
                                                    val perAppPctEdit = appInfo.customization?.iconSizePercent ?: iconSizePercent
                                                    val iconPxEdit = with(density) {
                                                        (iconSizeDp * perAppPctEdit / iconSizePercent.toFloat()).dp.toPx()
                                                    }
                                                    // Real label area = spacer + actual measured
                                                    // label height (was previously hardcoded to
                                                    // 16.dp which over-estimated for small fonts
                                                    // and under-estimated for big ones).
                                                    val labelOffsetPxEdit = with(density) { gridIconTextSpacer.toPx() } +
                                                        measuredLabelHeightPx
                                                    val scaledIconPxEdit = iconPxEdit * 1.265f
                                                    // Cap measured label width at the cell width
                                                    // since OverlayAppContent uses fillMaxWidth +
                                                    // single-line ellipsis, so the label can never
                                                    // visually exceed the cell.
                                                    val cappedLabelWidth = kotlin.math.min(measuredLabelWidthPx, cellSize.width.toFloat())
                                                    // Padding around the bounding box — generous
                                                    // enough to wrap text-shadow blur (~3 px) and
                                                    // any letter-spacing slack that TextMeasurer
                                                    // doesn't account for perfectly.
                                                    val boundsPaddingPx = with(density) { 12.dp.toPx() }
                                                    // The icon's inner visual Box has a
                                                    // graphicsLayer { scaleX = 1.265 } applied
                                                    // while editing. graphicsLayer doesn't change
                                                    // layout but DOES scale the rendered pixels —
                                                    // so the icon AND label visually take up
                                                    // 1.265× their layout size, with the scale
                                                    // centred on the cell centre. Every spatial
                                                    // value used to compute the bounding box has
                                                    // to be multiplied by this scale.
                                                    fun updateEditBounds() {
                                                        // Use the hoisted helper at the per-icon
                                                        // Composable scope so the bounds-calc is
                                                        // shared with the LaunchedEffect that
                                                        // tracks activeResizeMultiplier changes.
                                                        val (b, c) = computeEditBoundsAndCenter()
                                                        activeEditBounds = b
                                                        activeEditIconCenter = c
                                                    }
                                                    fun commitDragEnd() {
                                                        // posX/posY now derive from cust.detachedX/Y,
                                                        // so committing IS the position update — no
                                                        // separate local-state write needed.
                                                        val newX = posX + dragOffsetX
                                                        val newY = posY + dragOffsetY
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                        // CRITICAL: re-read cust from appCustomizations
                                                        // here. The captured `cust` val is the OLD
                                                        // customization from this composition's
                                                        // initial run — if a prior gesture (resize,
                                                        // customize dialog) updated the cust in
                                                        // between, `cust.copy(...)` would clobber
                                                        // those changes (e.g. resize → move would
                                                        // reset detachedScaleX/Y back to pre-resize).
                                                        val freshCust = appCustomizations.customizations[homeApp.packageName]
                                                            ?: AppCustomization()
                                                        val newCust = freshCust.copy(
                                                            detachedX = newX,
                                                            detachedY = newY
                                                        )
                                                        appCustomizations = setCustomization(
                                                            context,
                                                            appCustomizations,
                                                            homeApp.packageName,
                                                            newCust
                                                        )
                                                        // INLINE bounds write with the NEW position
                                                        // and the FRESH cust. The post-commit
                                                        // `updateEditBounds()` call would otherwise
                                                        // read posX via rememberUpdatedState, which
                                                        // hasn't been re-evaluated yet (no recomp
                                                        // has run), so it would compute the bracket
                                                        // at the OLD position. The LaunchedEffect
                                                        // would correct it one frame later — that's
                                                        // the crosshair ghost. Doing it inline
                                                        // writes the right value in the same
                                                        // snapshot as the cust change.
                                                        // Not gated on inEditMode — entry-drag
                                                        // commits also need the bounds (we're
                                                        // about to enter edit mode), and the
                                                        // captured `inEditMode` val is stale right
                                                        // after editingPackageName changes.
                                                        val storedSx = newCust.detachedScaleX ?: 1f
                                                        val storedSy = newCust.detachedScaleY ?: 1f
                                                        val combinedScaleX = editScaleOuter * storedSx
                                                        val combinedScaleY = editScaleOuter * storedSy
                                                        // Text-aware: a text-mode icon ("morphe")
                                                        // is wider/taller than the icon-square, so
                                                        // use the measured glyph dims here too —
                                                        // otherwise commit reverts the bracket to
                                                        // the square footprint after a move/long-
                                                        // press-into-edit gesture.
                                                        val effIconW = iconWidthPxOuter * combinedScaleX
                                                        val effIconH = iconHeightPxOuter * combinedScaleY
                                                        val effLabelOff = labelOffsetPxOuter * combinedScaleY
                                                        val effLabelW = cappedLabelWidthOuter * combinedScaleX
                                                        val cellCY = newY + cellSize.height / 2f
                                                        val cxN = newX + cellSize.width / 2f
                                                        val cyN = cellCY - effLabelOff / 2f
                                                        val widthPxN = kotlin.math.max(effIconW, effLabelW) + boundsPaddingPxOuter
                                                        val vPadN = boundsPaddingPxOuter / 2f
                                                        activeEditBounds = androidx.compose.ui.geometry.Rect(
                                                            left = cxN - widthPxN / 2f,
                                                            top = cyN - effIconH / 2f - vPadN,
                                                            right = cxN + widthPxN / 2f,
                                                            bottom = cyN + effIconH / 2f + effLabelOff + vPadN
                                                        )
                                                        activeEditIconCenter = androidx.compose.ui.geometry.Offset(cxN, cyN)
                                                    }
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                        // MODAL bail: if another detached icon is
                                                        // currently in edit mode, this icon must NOT
                                                        // react at all — the modal blocker will treat
                                                        // the touch as a tap-outside and exit the
                                                        // other icon's edit mode on release. The user
                                                        // has to then tap THIS icon again to interact
                                                        // with it.
                                                        val activeEditPkg = editingPackageNameState.value
                                                        if (activeEditPkg != null && activeEditPkg != homeApp.packageName) {
                                                            // Drain events for this pointer until lift
                                                            // so the gesture scope ends cleanly without
                                                            // running long-press / launch logic.
                                                            do {
                                                                val e = awaitPointerEvent()
                                                                if (e.changes.all { !it.pressed }) break
                                                            } while (true)
                                                            return@awaitEachGesture
                                                        }
                                                        val startPos = down.position
                                                        isFingerDown = true

                                                        if (inEditModeState.value) {
                                                            // Already in edit mode — any touch+drag
                                                            // repositions, no long-press needed. On
                                                            // release we commit position but STAY in
                                                            // edit mode (the page-level tap-outside
                                                            // handler exits it). Consume the down
                                                            // IMMEDIATELY so the home swipe-up-to-
                                                            // drawer gesture (and any other parent
                                                            // detector) can't claim this gesture and
                                                            // accidentally launch a drawer app like
                                                            // 1dm on release.
                                                            down.consume()
                                                            var dragStarted = false
                                                            try {
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    val change = event.changes.firstOrNull { it.id == down.id }
                                                                        ?: break
                                                                    if (change.pressed) {
                                                                        val dx = change.position.x - startPos.x
                                                                        val dy = change.position.y - startPos.y
                                                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                                        if (!dragStarted && dist > touchSlop) {
                                                                            dragStarted = true
                                                                            isDragging = true
                                                                            updateEditBounds()
                                                                        }
                                                                        if (dragStarted) {
                                                                            val delta = change.positionChange()
                                                                            dragOffsetX += delta.x
                                                                            dragOffsetY += delta.y
                                                                            updateEditBounds()
                                                                        }
                                                                        // Always consume while pressed in
                                                                        // edit mode — including pre-slop
                                                                        // micro-movements — so the home
                                                                        // swipe-up-to-drawer parent never
                                                                        // sees this gesture.
                                                                        change.consume()
                                                                    } else {
                                                                        isFingerDown = false
                                                                        if (dragStarted) {
                                                                            isDragging = false
                                                                            // commitDragEnd writes
                                                                            // activeEditBounds inline
                                                                            // with the new position,
                                                                            // so no separate
                                                                            // updateEditBounds()
                                                                            // call is needed (and
                                                                            // would cause a ghost
                                                                            // by reading the still-
                                                                            // stale rememberUpdated
                                                                            // state values).
                                                                            commitDragEnd()
                                                                        }
                                                                        // Consume the release so a rogue
                                                                        // app underneath the finger
                                                                        // doesn't fire its clickable when
                                                                        // we drop the icon over it.
                                                                        change.consume()
                                                                        // Stay in edit mode.
                                                                        break
                                                                    }
                                                                }
                                                            } catch (_: Throwable) {
                                                                isFingerDown = false
                                                                isDragging = false
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                            }
                                                            return@awaitEachGesture
                                                        }

                                                        // Not in edit mode — original flow:
                                                        // tap → launch, long-press → popup,
                                                        // long-press + drag → enter edit mode.
                                                        val longPress = awaitLongPressOrCancellation(down.id)
                                                        if (longPress == null) {
                                                            isFingerDown = false
                                                            // awaitLongPressOrCancellation returns null
                                                            // for BOTH "released early" (= tap) AND
                                                            // "moved beyond touchSlop" (= swipe/graze).
                                                            // Only launch when the pointer actually
                                                            // released — matching the regular grid's
                                                            // tap check at AppGridMovement.kt:455. A
                                                            // light graze across the icon was firing
                                                            // launchApp because of the missing check.
                                                            val upEvent = currentEvent.changes.firstOrNull { it.id == down.id }
                                                            if (upEvent != null && !upEvent.pressed) {
                                                                launchApp(context, homeApp.packageName, homeApp.userSerial)
                                                            }
                                                            return@awaitEachGesture
                                                        }
                                                        showContextMenu = true
                                                        flashOverlay = true
                                                        detachedHaptic.performLongPress()
                                                        var dragStarted = false
                                                        try {
                                                            while (true) {
                                                                val event = awaitPointerEvent()
                                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                                    ?: break
                                                                if (change.pressed) {
                                                                    val dx = change.position.x - startPos.x
                                                                    val dy = change.position.y - startPos.y
                                                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                                    if (!dragStarted && dist > touchSlop) {
                                                                        dragStarted = true
                                                                        showContextMenu = false
                                                                        isDragging = true
                                                                        updateEditBounds()
                                                                    }
                                                                    if (dragStarted) {
                                                                        val delta = change.positionChange()
                                                                        dragOffsetX += delta.x
                                                                        dragOffsetY += delta.y
                                                                        updateEditBounds()
                                                                        change.consume()
                                                                    }
                                                                } else {
                                                                    isFingerDown = false
                                                                    if (dragStarted) {
                                                                        isDragging = false
                                                                        // commitDragEnd writes bounds
                                                                        // inline with the new pos +
                                                                        // fresh cust — no separate
                                                                        // updateEditBounds() needed
                                                                        // (which would re-read the
                                                                        // still-stale rememberUpdated
                                                                        // posX and ghost the bracket).
                                                                        commitDragEnd()
                                                                        // ENTER edit mode — overlay
                                                                        // persists, future touches
                                                                        // drag without long-press,
                                                                        // tap-outside exits.
                                                                        editingPackageName = homeApp.packageName
                                                                        // Consume release to prevent any
                                                                        // app underneath from launching.
                                                                        change.consume()
                                                                    }
                                                                    // If not dragged, popup stays
                                                                    // open until user dismisses it.
                                                                    break
                                                                }
                                                            }
                                                        } catch (_: Throwable) {
                                                            isFingerDown = false
                                                            isDragging = false
                                                            dragOffsetX = 0f
                                                            dragOffsetY = 0f
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Inner visual Box owns the graphicsLayer scale +
                                            // the live drag translation so the OUTER pointerInput
                                            // sees raw, un-scaled, un-translated finger coords.
                                            // Without this split the drag stutters: 1/1.265 from
                                            // the scale and the layout-shift feedback loop from
                                            // moving the layout under the pointer.
                                            // Resize multiplier only applies while THIS icon
                                            // is the one in edit mode (otherwise the global
                                            // resize state would scale all detached icons).
                                            // Independent X / Y stretch — stored on the
                                            // customization plus the in-flight multiplier
                                            // while THIS icon is the one being edited.
                                            val storedStretchX = appInfo.customization?.detachedScaleX ?: 1f
                                            val storedStretchY = appInfo.customization?.detachedScaleY ?: 1f
                                            val liveResizeX = if (inEditMode) activeResizeMultiplierX else 1f
                                            val liveResizeY = if (inEditMode) activeResizeMultiplierY else 1f
                                            // Resize shift pins the OPPOSITE edge / corner
                                            // during a handle drag — added to the live drag
                                            // translation only for THIS icon while editing.
                                            val resizeShiftX = if (inEditMode) activeResizeShift.x else 0f
                                            val resizeShiftY = if (inEditMode) activeResizeShift.y else 0f
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        translationX = dragOffsetX + resizeShiftX
                                                        translationY = dragOffsetY + resizeShiftY
                                                        scaleX = effectiveScale * storedStretchX * liveResizeX
                                                        scaleY = effectiveScale * storedStretchY * liveResizeY
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                OverlayAppContent(
                                                    context = context,
                                                    appInfo = appInfo,
                                                    iconSizeDp = iconSizeDp,
                                                    iconSizePercent = iconSizePercent,
                                                    gridIconTextSpacer = gridIconTextSpacer,
                                                    gridAppNameFont = gridAppNameFont,
                                                    selectedFontFamily = selectedFontFamily,
                                                    textAlpha = 1f,
                                                    globalIconShape = globalIconShape,
                                                    showLabel = true,
                                                    globalIconBgColor = globalIconBgColor
                                                )
                                                // Invisible anchor matching the icon's visible
                                                // bounds for popup positioning. Lives inside the
                                                // scaled Box so positionInRoot reflects the
                                                // visually-enlarged icon when scaled up.
                                                val perAppPercent = appInfo.customization?.iconSizePercent ?: iconSizePercent
                                                val perAppIconDp = (iconSizeDp * perAppPercent / iconSizePercent.toFloat()).dp
                                                val labelOffsetDp = gridIconTextSpacer + 16.dp
                                                val isTextModeForOverlay = appInfo.customization?.iconText?.isNotBlank() == true
                                                Box(
                                                    modifier = Modifier
                                                        .size(perAppIconDp)
                                                        .align(Alignment.Center)
                                                        .offset(y = -(labelOffsetDp / 2))
                                                        .onGloballyPositioned { coords ->
                                                            iconBoundsInRoot = coords.boundsInRoot()
                                                        }
                                                )
                                                // Press / flash overlay — same dim feedback as the
                                                // regular grid icons. Sits over the icon area; in
                                                // text-as-icon mode it covers the FULL visible
                                                // glyph (wider/taller than the icon-square) and
                                                // centers on the text. Rounded enough to look
                                                // like a soft press indication on either icon or
                                                // glyph.
                                                if (overlayAlpha > 0f) {
                                                    val overlayWDp = if (isTextModeForOverlay)
                                                        with(LocalDensity.current) { iconWidthPxOuter.toDp() }
                                                    else perAppIconDp
                                                    val overlayHDp = if (isTextModeForOverlay)
                                                        with(LocalDensity.current) { iconHeightPxOuter.toDp() }
                                                    else perAppIconDp
                                                    val overlayCornerDp = kotlin.math.min(overlayWDp.value, overlayHDp.value).dp * 0.28f
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = overlayWDp, height = overlayHDp)
                                                            .align(Alignment.Center)
                                                            .let { m ->
                                                                if (isTextModeForOverlay) m
                                                                else m.offset(y = -(labelOffsetDp / 2))
                                                            }
                                                            .clip(RoundedCornerShape(overlayCornerDp))
                                                            .background(Color.Black.copy(alpha = overlayAlpha))
                                                    )
                                                }
                                            }
                                        }

                                        // Long-press popup — same actions as a regular grid app.
                                        com.bearinmind.launcher314.ui.components.AnimatedPopup(
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
                                                            openAppInfo(context, homeApp.packageName)
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
                                            DropdownMenuItem(
                                                text = { Text("Remove from home") },
                                                onClick = {
                                                    showContextMenu = false
                                                    saveHomeApps(homeApps.filter {
                                                        !(it.packageName == homeApp.packageName && it.page == page && it.position == homeApp.position)
                                                    })
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Uninstall") },
                                                onClick = {
                                                    showContextMenu = false
                                                    uninstallApp(context, homeApp.packageName)
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Customize") },
                                                onClick = {
                                                    showContextMenu = false
                                                    customizingApp = appInfo
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Total-Launcher-style editing overlay for the
                            // currently-dragging detached icon (issue #48):
                            //   - Solid grey corner brackets at the icon
                            //   - Dashed grey lines between the brackets
                            //   - Dashed crosshair arms from the icon
                            //     center extending to the page edges
                            //     (right + top arms only)
                            activeEditBounds?.let { bounds ->
                                val overlayColor = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
                                val strokeDp = 2.dp
                                val cornerLenDp = 18.dp
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokePx = strokeDp.toPx()
                                        val cornerPx = cornerLenDp.toPx()
                                        // Extend crosshair lines past the page padding
                                        // so they reach the actual screen edges. Parent
                                        // Box + Column both have graphicsLayer { clip = false },
                                        // so drawing past `size` is allowed.
                                        val padHpx = gridHPadding.toPx()
                                        val padVpx = gridVPadding.toPx()
                                        // Crosshair anchors on the BOUNDING-BOX center —
                                        // halfway between icon top and label bottom —
                                        // so the side dashed lines visually halve the
                                        // whole detached element (icon + label), not
                                        // just the icon.
                                        val centerX = (bounds.left + bounds.right) / 2f
                                        val centerY = (bounds.top + bounds.bottom) / 2f
                                        val activeIdx = activeResizeHandle
                                        val highlightColor = androidx.compose.ui.graphics.Color.White

                                        // Corner brackets — decoration only (constant grey).
                                        // The square markers at the corners are the touch
                                        // targets, not the brackets themselves.
                                        // TOP-LEFT
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),
                                            end = androidx.compose.ui.geometry.Offset(bounds.left + cornerPx, bounds.top),
                                            strokeWidth = strokePx)
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),
                                            end = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top + cornerPx),
                                            strokeWidth = strokePx)
                                        // TOP-RIGHT
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, bounds.top),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right - cornerPx, bounds.top),
                                            strokeWidth = strokePx)
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, bounds.top),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right, bounds.top + cornerPx),
                                            strokeWidth = strokePx)
                                        // BOTTOM-LEFT
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom),
                                            end = androidx.compose.ui.geometry.Offset(bounds.left + cornerPx, bounds.bottom),
                                            strokeWidth = strokePx)
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom),
                                            end = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom - cornerPx),
                                            strokeWidth = strokePx)
                                        // BOTTOM-RIGHT
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right - cornerPx, bounds.bottom),
                                            strokeWidth = strokePx)
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom - cornerPx),
                                            strokeWidth = strokePx)

                                        // Dashed lines between the corner brackets —
                                        // decoration only (constant grey, always dashed).
                                        // TOP edge
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left + cornerPx, bounds.top),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right - cornerPx, bounds.top),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)
                                        // BOTTOM edge
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left + cornerPx, bounds.bottom),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right - cornerPx, bounds.bottom),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)
                                        // LEFT edge
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top + cornerPx),
                                            end = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom - cornerPx),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)
                                        // RIGHT edge
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, bounds.top + cornerPx),
                                            end = androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom - cornerPx),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)

                                        // Resize-handle markers — squares for corners,
                                        // circles for edges. These ARE the touch targets
                                        // (the 36 dp invisible Box layered on top is keyed
                                        // to each marker position). Active marker scales
                                        // up + flips to white as visible feedback.
                                        val markerPx = 12.dp.toPx()
                                        val activeMarkerPx = 18.dp.toPx()
                                        val markerPositions = listOf(
                                            androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),                                  // 0 TL
                                            androidx.compose.ui.geometry.Offset((bounds.left + bounds.right) / 2f, bounds.top),           // 1 T
                                            androidx.compose.ui.geometry.Offset(bounds.right, bounds.top),                                 // 2 TR
                                            androidx.compose.ui.geometry.Offset(bounds.right, (bounds.top + bounds.bottom) / 2f),          // 3 R
                                            androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom),                              // 4 BR
                                            androidx.compose.ui.geometry.Offset((bounds.left + bounds.right) / 2f, bounds.bottom),        // 5 B
                                            androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom),                               // 6 BL
                                            androidx.compose.ui.geometry.Offset(bounds.left, (bounds.top + bounds.bottom) / 2f)            // 7 L
                                        )
                                        markerPositions.forEachIndexed { i, p ->
                                            val isCorner = (i % 2 == 0)
                                            val isActive = i == activeIdx
                                            val mPx = if (isActive) activeMarkerPx else markerPx
                                            val mColor = if (isActive) highlightColor else overlayColor
                                            if (isCorner) {
                                                drawRect(
                                                    color = mColor,
                                                    topLeft = androidx.compose.ui.geometry.Offset(
                                                        p.x - mPx / 2f, p.y - mPx / 2f
                                                    ),
                                                    size = androidx.compose.ui.geometry.Size(mPx, mPx)
                                                )
                                            } else {
                                                drawCircle(
                                                    color = mColor,
                                                    radius = mPx / 2f,
                                                    center = p
                                                )
                                            }
                                        }

                                        // Crosshair: dashed lines from the icon
                                        // center extending all the way to the
                                        // screen edges (past the page padding).
                                        // Only RIGHT + ABOVE arms now — left and
                                        // below removed per user request.
                                        // Horizontal — right of icon
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(bounds.right, centerY),
                                            end = androidx.compose.ui.geometry.Offset(size.width + padHpx, centerY),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)
                                        // Vertical — above icon
                                        drawLine(overlayColor,
                                            start = androidx.compose.ui.geometry.Offset(centerX, -padVpx),
                                            end = androidx.compose.ui.geometry.Offset(centerX, bounds.top),
                                            strokeWidth = strokePx,
                                            pathEffect = dashEffect)

                                    }

                                    // (Pixel-coord labels removed — were just informational
                                    // distances of the icon's centre from the page edges.)

                                    // Resize handles — uniform 36 dp invisible touch zones
                                    // anchored on each of the 8 visible markers (square at
                                    // corners, circle at edges). The marker IS the visual
                                    // touch target. awaitEachGesture consumes the down
                                    // event immediately so the parent's swipe-up-to-open-
                                    // drawer gesture can't steal the touch and accidentally
                                    // open an app from the drawer.
                                    val pkgForResize = editingPackageName
                                    val iconCenterForResize = activeEditIconCenter
                                    if (pkgForResize != null && iconCenterForResize != null) {
                                        val handleTouchSizeDp = 36.dp
                                        val handleTouchSizePx = with(density) { handleTouchSizeDp.toPx() }
                                        // Editing icon's unscaled icon px — used by the
                                        // handle's drag math as the reference content size.
                                        val editingCust = appCustomizations.customizations[pkgForResize]
                                        val editingPerAppPct = editingCust?.iconSizePercent ?: iconSizePercent
                                        val editingIconPxPage = with(density) {
                                            (iconSizeDp * editingPerAppPct / iconSizePercent.toFloat()).dp.toPx()
                                        }
                                        // boundsPaddingPxOuter equivalent at page scope (12dp).
                                        // Used to back out the content dimensions from the
                                        // current bracket rect — needed by the drag math so
                                        // drag distance maps 1:1 to bracket-edge movement.
                                        val pagePadPx = with(density) { 12.dp.toPx() }
                                        // No 1.265× bump in edit mode — kept = 1.0 for the
                                        // handle's reference dimension math, matches editScaleOuter.
                                        val editScalePage = 1f
                                        // Editing icon's label measurement at PAGE scope so the
                                        // handle's drag loop can write activeEditBounds inline,
                                        // in the SAME frame as mult/shift change. Without this,
                                        // the LaunchedEffect path lags by one frame and the
                                        // bracket trails the icon during continuous resize.
                                        val editingTriple = detachedAppsForPage.find { it.first.packageName == pkgForResize }
                                        val editingHomeAppForBounds = editingTriple?.first
                                        val editingAppInfoForBounds = editingTriple?.second
                                        val editingLabelText = editingCust?.customLabel?.takeIf { it.isNotEmpty() }
                                            ?: editingAppInfoForBounds?.name ?: ""
                                        val editingLabelFontSize = editingCust?.iconTextSizePercent?.let { 12.sp * it / 100f }
                                            ?: gridAppNameFont
                                        val editingLabelFontFamily = editingCust?.labelFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: selectedFontFamily ?: androidx.compose.ui.text.font.FontFamily.Default
                                        val editingTextMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                                        val editingBodySmallStyle = MaterialTheme.typography.bodySmall
                                        val editingLabelMeasured = remember(editingLabelText, editingLabelFontSize, editingLabelFontFamily, editingBodySmallStyle) {
                                            editingTextMeasurer.measure(
                                                text = editingLabelText,
                                                style = editingBodySmallStyle.copy(
                                                    fontSize = editingLabelFontSize,
                                                    fontFamily = editingLabelFontFamily
                                                )
                                            )
                                        }
                                        val editingLabelHeightPx = editingLabelMeasured.size.height.toFloat()
                                        val editingLabelWidthPx = editingLabelMeasured.size.width.toFloat()
                                        // Text-as-icon footprint at page scope — mirror the per-icon
                                        // measurement so the resize-handle inline bounds writes
                                        // wrap a wide glyph like "morphe" instead of the square.
                                        val editingIconTextString = editingCust?.iconText?.takeIf { it.isNotBlank() }
                                        val editingIconTextSp = editingCust?.iconAsTextSizeSp ?: 28
                                        // Same fallback chain as OverlayAppContent — icon-text font
                                        // falls back to per-app label font, then global font.
                                        val editingPerAppLabelFontFamily = editingCust?.labelFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: selectedFontFamily ?: androidx.compose.ui.text.font.FontFamily.Default
                                        val editingIconTextFontFamily = editingCust?.iconTextFontId?.let { id ->
                                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                                        } ?: editingPerAppLabelFontFamily
                                        val editingIconTextMeasured = remember(editingIconTextString, editingIconTextSp, editingIconTextFontFamily) {
                                            editingIconTextString?.let { txt ->
                                                editingTextMeasurer.measure(
                                                    text = txt,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        fontSize = editingIconTextSp.sp,
                                                        fontFamily = editingIconTextFontFamily
                                                    )
                                                )
                                            }
                                        }
                                        val editingIconWidthPx = editingIconTextMeasured?.size?.width?.toFloat() ?: editingIconPxPage
                                        val editingIconHeightPx = editingIconTextMeasured?.size?.height?.toFloat() ?: editingIconPxPage
                                        // Text mode hides the label, so zero out the label area —
                                        // otherwise the bracket reserves space below the glyph for
                                        // a label that isn't rendered.
                                        val editingLabelOffsetPx = if (editingIconTextString != null) 0f
                                            else with(density) { gridIconTextSpacer.toPx() } + editingLabelHeightPx
                                        val editingCappedLabelWidth = if (editingIconTextString != null) 0f
                                            else kotlin.math.min(editingLabelWidthPx, cellSize.width.toFloat())
                                        // Per-handle outward direction PER AXIS. 0 = that axis
                                        // doesn't change on this handle (edge handles only
                                        // resize one dimension). Order: TL T TR R BR B BL L.
                                        val handleDirX = listOf(-1f, 0f, 1f, 1f, 1f, 0f, -1f, -1f)
                                        val handleDirY = listOf(-1f, -1f, -1f, 0f, 1f, 1f, 1f, 0f)
                                        // Anchor handle — the OPPOSITE side / corner that
                                        // stays pinned during resize.
                                        val handleAnchorX = listOf(1f, 0f, -1f, -1f, -1f, 0f, 1f, 1f)
                                        val handleAnchorY = listOf(1f, 1f, 1f, 0f, -1f, -1f, -1f, 0f)
                                        val handleCenters = listOf(
                                            androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),                                  // 0 TL
                                            androidx.compose.ui.geometry.Offset((bounds.left + bounds.right) / 2f, bounds.top),           // 1 T
                                            androidx.compose.ui.geometry.Offset(bounds.right, bounds.top),                                 // 2 TR
                                            androidx.compose.ui.geometry.Offset(bounds.right, (bounds.top + bounds.bottom) / 2f),          // 3 R
                                            androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom),                              // 4 BR
                                            androidx.compose.ui.geometry.Offset((bounds.left + bounds.right) / 2f, bounds.bottom),        // 5 B
                                            androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom),                               // 6 BL
                                            androidx.compose.ui.geometry.Offset(bounds.left, (bounds.top + bounds.bottom) / 2f)            // 7 L
                                        )
                                        handleCenters.forEachIndexed { idx, center ->
                                            Box(
                                                modifier = Modifier
                                                    .offset {
                                                        IntOffset(
                                                            (center.x - handleTouchSizePx / 2f).toInt(),
                                                            (center.y - handleTouchSizePx / 2f).toInt()
                                                        )
                                                    }
                                                    .size(handleTouchSizeDp)
                                                    .pointerInput(pkgForResize, idx) {
                                                        val dirX = handleDirX[idx]
                                                        val dirY = handleDirY[idx]
                                                        val anchorX = handleAnchorX[idx]
                                                        val anchorY = handleAnchorY[idx]
                                                        awaitEachGesture {
                                                            // Consume the down IMMEDIATELY so the
                                                            // home-screen swipe-up gesture (which
                                                            // opens the app drawer) and any other
                                                            // parent detector never sees this touch.
                                                            val down = awaitFirstDown(requireUnconsumed = false)
                                                            down.consume()
                                                            val initialMultX = activeResizeMultiplierX
                                                            val initialMultY = activeResizeMultiplierY
                                                            // Snapshot the stored stretch at drag
                                                            // start so the commit's final scale =
                                                            // initialStored * latestMult, with NO
                                                            // chance of double-applying any state
                                                            // that might already include the in-
                                                            // flight multiplier.
                                                            val custAtStart = appCustomizations.customizations[pkgForResize]
                                                            val initialStoredX = custAtStart?.detachedScaleX ?: 1f
                                                            val initialStoredY = custAtStart?.detachedScaleY ?: 1f
                                                            val initialDetachedX = custAtStart?.detachedX ?: 0f
                                                            val initialDetachedY = custAtStart?.detachedY ?: 0f
                                                            // Per-drag mult bounds so the in-flight
                                                            // visual matches what the commit clamp
                                                            // will store. Without this, dragging the
                                                            // icon past 5.0 stored×mult shows the
                                                            // larger visual mid-drag, then snaps
                                                            // back to 5.0 on release (the "warp").
                                                            val minScaleStored = 0.3f
                                                            val maxScaleStored = 5f
                                                            val multUpperX = (maxScaleStored / initialStoredX).coerceAtMost(3f)
                                                            val multLowerX = (minScaleStored / initialStoredX).coerceAtLeast(0.4f)
                                                            val multUpperY = (maxScaleStored / initialStoredY).coerceAtMost(3f)
                                                            val multLowerY = (minScaleStored / initialStoredY).coerceAtLeast(0.4f)
                                                            // FULL content dimensions at drag start
                                                            // (= what the bracket wraps, minus the
                                                            // 12 dp padding). Drag distance maps
                                                            // directly to bracket-edge motion: drag
                                                            // 50 px → bracket edge moves 50 px →
                                                            // content dimension grows 50 px. The
                                                            // resize ratio falls out from there.
                                                            //
                                                            // CRITICAL: read activeEditBounds fresh
                                                            // here instead of the outer-scope `bounds`
                                                            // val. pointerInput keys on (pkg, idx) so
                                                            // the suspend block is NOT restarted when
                                                            // activeEditBounds changes mid-edit — the
                                                            // outer `bounds` capture is stale on the
                                                            // 2nd+ resize. Reading the state directly
                                                            // here gets the current bracket size, so
                                                            // ratio math reflects the post-1st-resize
                                                            // bracket and the opposite edge truly
                                                            // holds still on subsequent drags.
                                                            val freshBounds = activeEditBounds
                                                            val initialContentX = if (freshBounds != null)
                                                                (freshBounds.right - freshBounds.left) - pagePadPx
                                                            else 0f
                                                            val initialContentY = if (freshBounds != null)
                                                                (freshBounds.bottom - freshBounds.top) - pagePadPx
                                                            else 0f
                                                            var accumulated = androidx.compose.ui.geometry.Offset.Zero
                                                            // Latest in-flight values tracked locally
                                                            // — commit uses these so we don't race a
                                                            // recomposition or snapshot boundary on
                                                            // the activeResize* state reads.
                                                            var latestMultX = initialMultX
                                                            var latestMultY = initialMultY
                                                            var latestShift = androidx.compose.ui.geometry.Offset.Zero
                                                            activeResizeHandle = idx
                                                            try {
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                                                    if (change.changedToUp()) {
                                                                        change.consume()
                                                                        val newScaleX = (initialStoredX * latestMultX).coerceIn(0.3f, 5f)
                                                                        val newScaleY = (initialStoredY * latestMultY).coerceIn(0.3f, 5f)
                                                                        val newDetX = initialDetachedX + latestShift.x
                                                                        val newDetY = initialDetachedY + latestShift.y
                                                                        val current = appCustomizations.customizations[pkgForResize]
                                                                        val updated = (current ?: AppCustomization()).copy(
                                                                            detachedScaleX = newScaleX,
                                                                            detachedScaleY = newScaleY,
                                                                            detachedX = newDetX,
                                                                            detachedY = newDetY
                                                                        )
                                                                        appCustomizations = setCustomization(
                                                                            context, appCustomizations, pkgForResize, updated
                                                                        )
                                                                        // INLINE bounds write using
                                                                        // the FINAL COMMITTED values
                                                                        // (post-clamp scale + new
                                                                        // detached pos). The last
                                                                        // onDrag's inline write used
                                                                        // pre-clamp values; this one
                                                                        // ensures bounds match what
                                                                        // the icon will render with
                                                                        // post-commit. Same snapshot
                                                                        // as the cust write, so no
                                                                        // frame gap, no ghost.
                                                                        val homeAppForBoundsCommit = editingHomeAppForBounds
                                                                        if (homeAppForBoundsCommit != null) {
                                                                            val combinedX = editScalePage * newScaleX
                                                                            val combinedY = editScalePage * newScaleY
                                                                            val effIconW = editingIconWidthPx * combinedX
                                                                            val effIconH = editingIconHeightPx * combinedY
                                                                            val effLabelOff = editingLabelOffsetPx * combinedY
                                                                            val effLabelW = editingCappedLabelWidth * combinedX
                                                                            val cellCYpx = newDetY + cellSize.height / 2f
                                                                            val cxPxC = newDetX + cellSize.width / 2f
                                                                            val cyPxC = cellCYpx - effLabelOff / 2f
                                                                            val widthPxC = kotlin.math.max(effIconW, effLabelW) + pagePadPx
                                                                            val vPadC = pagePadPx / 2f
                                                                            activeEditBounds = androidx.compose.ui.geometry.Rect(
                                                                                left = cxPxC - widthPxC / 2f,
                                                                                top = cyPxC - effIconH / 2f - vPadC,
                                                                                right = cxPxC + widthPxC / 2f,
                                                                                bottom = cyPxC + effIconH / 2f + effLabelOff + vPadC
                                                                            )
                                                                            activeEditIconCenter = androidx.compose.ui.geometry.Offset(cxPxC, cyPxC)
                                                                        }
                                                                        break
                                                                    }
                                                                    val delta = change.positionChange()
                                                                    if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                                                                        change.consume()
                                                                        accumulated += delta
                                                                        if (initialContentX <= 0f || initialContentY <= 0f) continue
                                                                        // ratio = 1 + drag / content
                                                                        // (drag-edge moves 1:1 with finger).
                                                                        val newMultX: Float = if (dirX != 0f) {
                                                                            val growthX = accumulated.x * dirX
                                                                            val ratioX = (initialContentX + growthX) / initialContentX
                                                                            (initialMultX * ratioX).coerceIn(multLowerX, multUpperX)
                                                                        } else initialMultX
                                                                        val newMultY: Float = if (dirY != 0f) {
                                                                            val growthY = accumulated.y * dirY
                                                                            val ratioY = (initialContentY + growthY) / initialContentY
                                                                            (initialMultY * ratioY).coerceIn(multLowerY, multUpperY)
                                                                        } else initialMultY
                                                                        latestMultX = newMultX
                                                                        latestMultY = newMultY
                                                                        activeResizeMultiplierX = newMultX
                                                                        activeResizeMultiplierY = newMultY
                                                                        // shift = -anchor * delta/2.
                                                                        // Total content grew by delta;
                                                                        // center moves by delta/2 to
                                                                        // keep the opposite edge pinned.
                                                                        val deltaContentX = initialContentX *
                                                                            ((newMultX / initialMultX) - 1f)
                                                                        val deltaContentY = initialContentY *
                                                                            ((newMultY / initialMultY) - 1f)
                                                                        latestShift = androidx.compose.ui.geometry.Offset(
                                                                            -anchorX * deltaContentX / 2f,
                                                                            -anchorY * deltaContentY / 2f
                                                                        )
                                                                        activeResizeShift = latestShift
                                                                        // INLINE bounds write — same
                                                                        // frame as mult/shift, no
                                                                        // LaunchedEffect lag, so the
                                                                        // bracket + crosshair stay
                                                                        // glued to the icon during
                                                                        // continuous resize. Uses
                                                                        // initialStored* (captured at
                                                                        // drag start) so we don't
                                                                        // race appCustomizations.
                                                                        val homeAppForBounds = editingHomeAppForBounds
                                                                        if (homeAppForBounds != null) {
                                                                            val combinedX = editScalePage * initialStoredX * newMultX
                                                                            val combinedY = editScalePage * initialStoredY * newMultY
                                                                            val effIconW = editingIconWidthPx * combinedX
                                                                            val effIconH = editingIconHeightPx * combinedY
                                                                            val effLabelOff = editingLabelOffsetPx * combinedY
                                                                            val effLabelW = editingCappedLabelWidth * combinedX
                                                                            val defaultBpX = (homeAppForBounds.position % gridColumns) * cellSize.width.toFloat()
                                                                            val defaultBpY = (homeAppForBounds.position / gridColumns) * cellSize.height.toFloat()
                                                                            val basePosX = (custAtStart?.detachedX ?: defaultBpX)
                                                                            val basePosY = (custAtStart?.detachedY ?: defaultBpY)
                                                                            val cellCenterYpx = basePosY + latestShift.y + cellSize.height / 2f
                                                                            val cxPx = basePosX + latestShift.x + cellSize.width / 2f
                                                                            val cyPx = cellCenterYpx - effLabelOff / 2f
                                                                            val widthPxN = kotlin.math.max(effIconW, effLabelW) + pagePadPx
                                                                            val vPadN = pagePadPx / 2f
                                                                            activeEditBounds = androidx.compose.ui.geometry.Rect(
                                                                                left = cxPx - widthPxN / 2f,
                                                                                top = cyPx - effIconH / 2f - vPadN,
                                                                                right = cxPx + widthPxN / 2f,
                                                                                bottom = cyPx + effIconH / 2f + effLabelOff + vPadN
                                                                            )
                                                                            activeEditIconCenter = androidx.compose.ui.geometry.Offset(cxPx, cyPx)
                                                                        }
                                                                    }
                                                                }
                                                            } finally {
                                                                activeResizeMultiplierX = 1f
                                                                activeResizeMultiplierY = 1f
                                                                activeResizeShift = androidx.compose.ui.geometry.Offset.Zero
                                                                activeResizeHandle = null
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
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

            // Dock bar at bottom — wrapped in a HorizontalPager when there are multiple
            // dock pages. Styled like the widget stack (chrome appears on swipe + dots).
            val dockPagerState = rememberPagerState(initialPage = 0, pageCount = { dockPagesCount })
            // Expose for drag-drop logic so new dock items use the current page.
            currentDockPage = dockPagerState.currentPage

            // Chrome (dim background + border) appears while swiping the dock pager,
            // identical animation profile to the widget stack chrome.
            val isDockSwiping = dockPagerState.isScrollInProgress
            var isDockSwipeLingering by remember { mutableStateOf(false) }
            LaunchedEffect(isDockSwiping) {
                if (isDockSwiping) {
                    isDockSwipeLingering = true
                } else if (isDockSwipeLingering) {
                    delay(1000)
                    isDockSwipeLingering = false
                }
            }
            val showDockChrome = dockPagesCount > 1 && (isDockSwiping || isDockSwipeLingering)
            val dockChromeAlpha by animateFloatAsState(
                targetValue = if (showDockChrome) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (showDockChrome) 120 else 400,
                    easing = FastOutSlowInEasing
                ),
                label = "dockChrome"
            )

            // Dot styling matches widget stack
            val dockDotBaseColor = getScrollbarColor(context)
            val dockDotIntensity = getScrollbarIntensity(context)
            val dockDotColor = remember(dockDotBaseColor, dockDotIntensity) {
                val base = Color(dockDotBaseColor)
                val factor = (dockDotIntensity / 100f).coerceIn(0f, 1f)
                Color(
                    red = base.red * factor,
                    green = base.green * factor,
                    blue = base.blue * factor,
                    alpha = base.alpha
                )
            }
            val dockDotSize = (screenWidthDp * 0.02f * getScrollbarWidthPercent(context) / 100f).dp

            if (isDockEnabled) Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { dockTopY = it.positionInRoot().y }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dockHoverCornerRadius))
                        .then(
                            if (dockChromeAlpha > 0f) Modifier
                                .background(Color.Black.copy(alpha = 0.4f * dockChromeAlpha))
                                .border(1.dp, Color(0xFF888888).copy(alpha = dockChromeAlpha), RoundedCornerShape(dockHoverCornerRadius))
                            else Modifier
                        )
                ) {
                HorizontalPager(
                    state = dockPagerState,
                    modifier = Modifier.fillMaxWidth(),
                    // Disable swipe when an item is being dragged so dock-page swipes
                    // don't fight with drag-and-drop.
                    userScrollEnabled = dockPagesCount > 1 && draggedItemIndex == null && !isDropAnimating && !externalDragActive && !isWidgetBeingDragged
                ) { dockPage ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gridHPadding, vertical = gridVPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                repeat(dockSlots) { slot ->
                    val dockApp = dockApps.find { it.position == slot && it.page == dockPage }
                    val appInfo = dockApp?.let { da ->
                        allAvailableApps.find { it.packageName == da.packageName }?.let { info ->
                            val cust = appCustomizations.customizations[da.packageName]
                            if (cust != null) info.copy(customization = cust) else info
                        }
                    }
                    val dockFolder = dockFolders.find { it.position == slot && it.page == dockPage }
                    // Apply per-app customizations so dock folder mini icons show custom icons/shapes/tints
                    val dockFolderPreviewApps = dockFolder?.appPackageNames?.filter { it.isNotEmpty() && it !in hiddenApps }?.take(4)?.mapNotNull { pkg ->
                        allAvailableApps.find { it.packageName == pkg }?.let { info ->
                            val cust = appCustomizations.customizations[pkg]
                            if (cust != null) info.copy(customization = cust) else info
                        }
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
                            folderCustomization = dockFolder?.let { appCustomizations.customizations["folder_${it.id}"] },
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
                                    launchApp(context, appInfo.packageName, appInfo.userSerial)
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
                                    // Hovering over dock - check if slot is valid (on visible page)
                                    val dockSlotApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                                    val dockSlotFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
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
                                        // All dock-to-dock ops happen on the currently visible page —
                                        // both source and target are slots within currentDockPage.
                                        val fromApp = dockApps.find { it.position == currentDraggedSlot && it.page == currentDockPage }
                                        val toApp = dockApps.find { it.position == targetDockSlot && it.page == currentDockPage }
                                        val toFolder = dockFolders.find { it.position == targetDockSlot && it.page == currentDockPage }
                                        val slotEmpty = toApp == null && toFolder == null

                                        if (isDraggingDockFolder && dockFolder != null && slotEmpty) {
                                            // Move dock folder to another dock slot
                                            val targetPos = dockPositions[targetDockSlot]
                                            targetOffset = if (targetPos != null) {
                                                Offset(targetPos.x - originalDockPos.x, targetPos.y - originalDockPos.y)
                                            } else Offset.Zero
                                            dropAction = {
                                                dockFolders = dockFolders.map { f ->
                                                    if (f.id == dockFolder.id) f.copy(position = targetDockSlot, page = currentDockPage)
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
                                                updatedDockApps.removeAll { it.position == currentDraggedSlot && it.page == currentDockPage }
                                                updatedDockApps.add(DockApp(fromApp.packageName, targetDockSlot, page = currentDockPage))
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
                                                    !((it.position == currentDraggedSlot || it.position == targetDockSlot) && it.page == currentDockPage)
                                                }
                                                val newDockFolder = DockFolder(
                                                    name = "Folder",
                                                    position = targetDockSlot,
                                                    appPackageNames = listOf(toApp.packageName, fromApp.packageName),
                                                    page = currentDockPage
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
                } // end repeat(dockSlots)
                } // end Row
                } // end HorizontalPager dockPage
                } // end inner chrome Box

                // Page dots overlay — only visible when there are multiple dock pages
                // and only fully opaque while swiping (matches widget stack behavior).
                if (dockPagesCount > 1 && dockChromeAlpha > 0f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .graphicsLayer { alpha = dockChromeAlpha },
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(dockPagesCount) { dotIndex ->
                            Box(
                                modifier = Modifier
                                    .size(dockDotSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (dockPagerState.currentPage == dotIndex)
                                            dockDotColor.copy(alpha = 0.9f)
                                        else dockDotColor.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            } // end outer dock Box
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
                            val folderOverlayInvalid = when {
                                hoveredDockSlot != null -> !isHoveredDockSlotValid
                                hoveredGridCell != null -> !isHoveredCellValid
                                else -> false
                            }
                            OverlayFolderContent(
                                context = context,
                                folderData = draggedFolderData!!,
                                folderCust = appCustomizations.customizations["folder_${draggedFolderData!!.id}"],
                                previewApps = draggedFolderPreviewApps,
                                iconSizeDp = iconSizeDp,
                                gridIconTextSpacer = gridIconTextSpacer,
                                gridAppNameFont = gridAppNameFont,
                                selectedFontFamily = selectedFontFamily,
                                textAlpha = textAlpha,
                                globalIconShape = globalIconShape,
                                globalIconBgColor = globalIconBgColor,
                                globalIconBgIntensity = globalIconBgIntensity,
                                isInvalid = folderOverlayInvalid,
                                showLabel = !isDropAnimating || !dropTargetIsDock
                            )
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
                            val folderOverlayInvalid = when {
                                hoveredDockSlot != null -> !isHoveredDockSlotValid
                                hoveredGridCell != null -> !isHoveredCellValid
                                else -> false
                            }
                            OverlayFolderContent(
                                context = context,
                                folderData = draggedFolderData!!,
                                folderCust = appCustomizations.customizations["folder_${draggedFolderData!!.id}"],
                                previewApps = draggedFolderPreviewApps,
                                iconSizeDp = iconSizeDp,
                                gridIconTextSpacer = gridIconTextSpacer,
                                gridAppNameFont = gridAppNameFont,
                                selectedFontFamily = selectedFontFamily,
                                textAlpha = textAlpha,
                                globalIconShape = globalIconShape,
                                globalIconBgColor = globalIconBgColor,
                                globalIconBgIntensity = globalIconBgIntensity,
                                isInvalid = folderOverlayInvalid,
                                showLabel = !isDropAnimating || !dropTargetIsDock
                            )
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
                        val app = updatedApps.find { it.page == pg && it.position == pos && isAttached(it) }
                        if (app != null) {
                            selectedPkgs.add(app.packageName)
                            updatedApps.removeAll { it.page == pg && it.position == pos && isAttached(it) }
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
    var openedFolderIconBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
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
        // Snapshot the actual icon bounds for the opened folder (home cells
        // only — dock folder bounds aren't tracked yet so they keep the
        // approximation). Falls back to null if the cell hasn't reported.
        openedFolderIconBounds = if (openHomeFolder!!.position >= 0)
            folderIconBoundsMap[openHomeFolder!!.position]
        else null
    }
    val folderAnimProgress by animateFloatAsState(
        // Match Lawnchair's SMOOTH (spring-based) folder animation. From
        // src/com/android/launcher3/folder/FolderSpringAnimatorSet.kt:
        //   STIFFNESS_SHAPE_POSITION = 380f   — for translation + scale
        //   DAMPING_SHAPE_POSITION   = 0.8f   — slight, natural bounce
        // The whole motion (popup + dim + everything driven by this
        // progress) settles via spring physics instead of a fixed tween,
        // so the open and close read as a single fluid arc.
        targetValue = if (openHomeFolder != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f,
            visibilityThreshold = 0.001f
        ),
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
                    escapeCloseAnim.animateTo(
                        0f,
                        // Match Lawnchair's spring-based close motion.
                        spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = 0.001f)
                    )
                    // Null lastOpenedFolder ONLY if the user has already let go
                    // of their finger (escapedToHomeGrid resets to false in
                    // onDragEnd). If still dragging, leave it alone — the
                    // DraggableGridCell that started the drag MUST stay in
                    // the composition tree, or its pointerInput coroutine
                    // gets cancelled and the drag silently dies mid-air,
                    // making it look like the system "dropped" the icon
                    // when the close animation finishes. onDragEnd nulls
                    // lastOpenedFolder itself when the real drop fires.
                    if (openHomeFolder == null && !escapedToHomeGrid) {
                        lastOpenedFolder = null
                    }
                }
            }
    }
    if (escapeCloseAnim.value > 0f) {
        // Use the same bounded-popup geometry as the open animation so the
        // close visual matches the new card style.
        val escDensity = LocalDensity.current
        val escConfig = LocalConfiguration.current
        val escScreenW = with(escDensity) { escConfig.screenWidthDp.dp.toPx() }
        val escScreenH = with(escDensity) { escConfig.screenHeightDp.dp.toPx() }
        val escMaxW = with(escDensity) { 320.dp.toPx() }
        val escPopupW = (escScreenW * 0.72f).coerceAtMost(escMaxW)
        val escTitleBarPx = with(escDensity) { 52.dp.toPx() }
        val escPopupH = escScreenH * 0.38f + escTitleBarPx
        val escSafe = with(escDensity) { 16.dp.toPx() }
        val escIconSidePx = with(escDensity) { iconSizeDp.dp.toPx() }
        val escCellCx = escapeCloseCellOrigin.x + escapeCloseCellSize.width / 2f
        val escCellCy = escapeCloseCellOrigin.y + escapeCloseCellSize.height / 2f
        val escIconCenterY = escCellCy - with(escDensity) { 8.dp.toPx() }
        val escIconTopPx = escIconCenterY - escIconSidePx / 2f
        val escIconBottomPx = escIconCenterY + escIconSidePx / 2f
        val escIconWidthPx = escIconSidePx
        val escIconHeightPx = escIconSidePx
        // Adjacent-to-icon placement, matching the open animation.
        val escGoesAbove = escIconTopPx - escPopupH - escSafe >= escSafe ||
            (escIconBottomPx + escPopupH + escSafe > escScreenH - escSafe)
        val escPopupY = if (escGoesAbove) {
            (escIconBottomPx - escPopupH).coerceAtLeast(escSafe)
        } else {
            escIconTopPx.coerceAtMost(escScreenH - escPopupH - escSafe)
        }
        val escCenteredLeft = escCellCx - escPopupW / 2f
        val escPopupX = escCenteredLeft.coerceIn(escSafe, (escScreenW - escPopupW - escSafe).coerceAtLeast(escSafe))
        // Clip-reveal rect in popup-local px (folder-icon rect → full popup),
        // matching the open animation's reveal.
        val escRevStartLeft = (escCellCx - escIconWidthPx / 2f) - escPopupX
        val escRevStartTop = (escIconCenterY - escIconHeightPx / 2f) - escPopupY
        val escRevStartRight = escRevStartLeft + escIconWidthPx
        val escRevStartBottom = escRevStartTop + escIconHeightPx
        val escRevStartRadiusPx = escIconWidthPx * 0.29f
        val escFinalRadiusPx = with(escDensity) { 20.dp.toPx() }
        Box(
            modifier = Modifier
                .offset { IntOffset(escPopupX.roundToInt(), escPopupY.roundToInt()) }
                .size(
                    width = with(escDensity) { escPopupW.toDp() },
                    height = with(escDensity) { escPopupH.toDp() }
                )
                .graphicsLayer { alpha = escapeCloseAnim.value }
                .drawWithContent {
                    val p = escapeCloseAnim.value.coerceIn(0f, 1f)
                    if (p >= 0.999f) { drawContent(); return@drawWithContent }
                    val l = escRevStartLeft + (0f - escRevStartLeft) * p
                    val t = escRevStartTop + (0f - escRevStartTop) * p
                    val r = escRevStartRight + (size.width - escRevStartRight) * p
                    val b = escRevStartBottom + (size.height - escRevStartBottom) * p
                    val rad = escRevStartRadiusPx + (escFinalRadiusPx - escRevStartRadiusPx) * p
                    val revealPath = Path().apply {
                        addRoundRect(RoundRect(l, t, r, b, CornerRadius(rad, rad)))
                    }
                    clipPath(revealPath) { this@drawWithContent.drawContent() }
                }
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    com.bearinmind.launcher314.ui.theme.LocalFolderBorderColor.current,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // No header — matches the title-bar-less popup. Just the
                // app grid silhouette so the close animation visual lines
                // up with the open animation.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
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
        // Re-derive the folder from LIVE homeFolders / dockFolders by ID rather
        // than using the captured open snapshot. openHomeFolder/lastOpenedFolder
        // are point-in-time copies that DON'T update when the underlying folder
        // changes (e.g. re-adding an escaped app), so the open folder rendered
        // stale until a full reload (leaving + returning to the home screen).
        // Falling back to the snapshot keeps it alive mid-close.
        // Use the FROZEN snapshot while an escape drag is active — re-deriving
        // live would change folder.appPackageNames the instant the app is
        // removed, flipping the folder grid's remember() keys mid-drag and
        // killing the dragged cell's gesture (it would auto-drop). Once the
        // drag ends (escapedToHomeGrid=false) use the LIVE folder so re-adds
        // and other changes show immediately.
        val folderSnapshot = openHomeFolder ?: lastOpenedFolder!!
        val folder = if (escapedToHomeGrid) folderSnapshot
            else liveFolder(folderSnapshot, homeFolders, dockFolders)
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

        // ── Bounded-popup placement (Lawnchair / Neo Launcher style) ──────
        // Card opens just above or below the tapped folder, ~85% of screen
        // width with a 400dp cap. Wallpaper + home grid stays visible behind
        // a 50% black dim. Tap-outside closes. transformOrigin points toward
        // the anchor so the scale-up reads as "growing out of the folder".
        val folderDensity = LocalDensity.current
        val folderConfig = LocalConfiguration.current
        val screenWpx = with(folderDensity) { folderConfig.screenWidthDp.dp.toPx() }
        val screenHpx = with(folderDensity) { folderConfig.screenHeightDp.dp.toPx() }
        // Narrower / shorter popup, closer to Lawnchair's compact card. The
        // wide 85%-screen version felt like a generic dialog floating over
        // the wallpaper; this size makes the popup feel like the folder
        // cell itself stretched outward. Height = grid area + title bar so
        // adding the title doesn't squish the apps. The user can drag-
        // resize the popup via the title-bar's "Resize" menu — saved
        // dimensions live in AppCustomization keyed by folder ID and
        // override the defaults below when present.
        val maxCardWpx = with(folderDensity) { 320.dp.toPx() }
        val defaultPopupWpx = (screenWpx * 0.72f).coerceAtMost(maxCardWpx)
        val titleBarPx = with(folderDensity) { 52.dp.toPx() }
        val defaultPopupHpx = screenHpx * 0.38f + titleBarPx
        val folderCust = appCustomizations.customizations[folder.id]
        var resizeWidthOverride by remember(folder.id) {
            mutableStateOf(folderCust?.folderPopupWidthPx?.toFloat())
        }
        var resizeHeightOverride by remember(folder.id) {
            mutableStateOf(folderCust?.folderPopupHeightPx?.toFloat())
        }
        // Per-folder grid overrides. Null while the user hasn't opted in;
        // the Resize panel seeds these from the global gridColumns/gridRows
        // on first open so the steppers start from a real value.
        var resizeColumnsOverride by remember(folder.id) {
            mutableStateOf(folderCust?.folderGridColumns)
        }
        var resizeRowsOverride by remember(folder.id) {
            mutableStateOf(folderCust?.folderGridRows)
        }
        var isResizingFolder by remember(folder.id) { mutableStateOf(false) }
        // Spring-driven resize enter/exit progress (declared here so the popup
        // card's static border can cross-fade out as the dashed resize outline
        // fades in — see the .border below).
        val resizeAnimProgress by animateFloatAsState(
            targetValue = if (isResizingFolder) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 380f,
                visibilityThreshold = 0.001f
            ),
            label = "folderResizeAnim"
        )
        // Effective grid dims used by the folder render loop. The Resize
        // session updates the overrides live; the loop reads these.
        val folderGridColumns = resizeColumnsOverride ?: gridColumns
        val folderGridRows = resizeRowsOverride ?: gridRows
        val minPopupWpx = with(folderDensity) { 200.dp.toPx() }
        val minPopupHpx = with(folderDensity) { 200.dp.toPx() }
        val maxPopupWpx = screenWpx - 2f * with(folderDensity) { 16.dp.toPx() }
        // Cap at HALF the screen height so the popup can always sit ABOVE or
        // BELOW the folder icon no matter where the icon is on screen — the
        // grow-from-icon animation only reads correctly when one full popup
        // height fits between the icon's edge and the matching screen edge.
        val maxPopupHpx = screenHpx * 0.5f
        val popupWpx = (resizeWidthOverride ?: defaultPopupWpx)
            .coerceIn(minPopupWpx, maxPopupWpx)
        val popupHpx = (resizeHeightOverride ?: defaultPopupHpx)
            .coerceIn(minPopupHpx, maxPopupHpx)

        // ── Auto-shrink folder icons so they never overlap (folder-only) ──
        // The grid cells split the popup evenly (weight 1f). Compute each
        // cell's px size from the popup content area, then cap the icon so it
        // fits its cell with margin. min() with the normal home icon size
        // means icons only ever get SMALLER than home — never bigger — so a
        // sparse folder still looks like the home screen, but a dense grid
        // (or a small / resized popup) flows the icons down instead of
        // overlapping. Home and dock are untouched; this is the folder only.
        val folderGridPadPx = with(folderDensity) { 16.dp.toPx() }   // 8dp each side
        val folderTitleBarPx2 = with(folderDensity) { 52.dp.toPx() }
        val folderContentWpx = (popupWpx - folderGridPadPx).coerceAtLeast(1f)
        val folderContentHpx = (popupHpx - folderTitleBarPx2 - folderGridPadPx).coerceAtLeast(1f)
        val folderCellWpx = folderContentWpx / folderGridColumns.coerceAtLeast(1)
        val folderCellHpx = folderContentHpx / folderGridRows.coerceAtLeast(1)
        // Icon must fit horizontally (≤ ~82% of cell width) AND leave room
        // below for the label (≤ ~58% of cell height).
        val folderFitIconPx = minOf(folderCellWpx * 0.82f, folderCellHpx * 0.58f)
        val baseFolderIconPx = with(folderDensity) { iconSizeDp.dp.toPx() }
        val folderIconPx = minOf(baseFolderIconPx, folderFitIconPx).coerceAtLeast(1f)
        val folderIconSizeDp = with(folderDensity) { folderIconPx.toDp() }.value.toInt()
            .coerceAtLeast(1)

        // Popup sits ADJACENT to the folder icon (above when there's room,
        // below otherwise) — the popup's near edge touches the icon's edge,
        // so the icon stays visible below/above the popup. Pivot is set at
        // that touching edge (and aligned X-wise with the icon center) so
        // the scale animation grows the popup OUT OF the folder icon.
        val safePx = with(folderDensity) { 16.dp.toPx() }
        val iconSidePx = with(folderDensity) { iconSizeDp.dp.toPx() }
        val cellCenterX = folderCellOrigin.x + folderCellOriginSize.width / 2f
        val cellCenterY = folderCellOrigin.y + folderCellOriginSize.height / 2f
        // Prefer the EXACT folder-icon bounds reported via
        // onFolderIconPositioned. Falls back to a cell + iconSizeDp
        // approximation if the bounds haven't been measured yet (first
        // open before any layout).
        val iconBounds = openedFolderIconBounds
        val iconCenterX = iconBounds?.center?.x ?: cellCenterX
        val iconCenterY = iconBounds?.center?.y ?: (cellCenterY - with(folderDensity) { 8.dp.toPx() })
        val iconTopPx = iconBounds?.top ?: (iconCenterY - iconSidePx / 2f)
        val iconBottomPx = iconBounds?.bottom ?: (iconCenterY + iconSidePx / 2f)
        // Icon WIDTH and HEIGHT in px — used to seed the popup's initial
        // scale so it starts the SAME size as the folder icon, then all
        // four edges expand outward (Lawnchair's ClipRevealData pattern,
        // but achieved with scaleX/scaleY + pivot at the icon center).
        val iconWidthPx = iconBounds?.width ?: iconSidePx
        val iconHeightPx = iconBounds?.height ?: iconSidePx
        // Popup sits ADJACENT to the folder icon — above when there's room,
        // below otherwise — fully covering the icon (the icon's far edge sits
        // flush with the popup's far edge). The reveal still grows from the
        // icon rect so it reads as "growing out of the folder".
        val popupGoesAbove = iconTopPx - popupHpx - safePx >= safePx ||
            (iconBottomPx + popupHpx + safePx > screenHpx - safePx)
        val popupY = if (popupGoesAbove) {
            (iconBottomPx - popupHpx).coerceAtLeast(safePx)
        } else {
            iconTopPx.coerceAtMost(screenHpx - popupHpx - safePx)
        }
        val centeredLeft = iconCenterX - popupWpx / 2f
        val popupX = centeredLeft.coerceIn(safePx, (screenWpx - popupWpx - safePx).coerceAtLeast(safePx))

        // ── Clip-reveal geometry (Launcher3 RoundedRectRevealOutlineProvider)
        // The popup is laid out at FULL size; a rounded-rect clip grows from
        // the folder-icon rect to the full popup, with the corner radius
        // easing from the icon's radius to the popup's. Content draws crisp
        // at final layout — no scale distortion. Rect is in popup-LOCAL px.
        val iconLeftLocal = (iconBounds?.left ?: (iconCenterX - iconWidthPx / 2f)) - popupX
        val iconTopLocal = (iconBounds?.top ?: (iconCenterY - iconHeightPx / 2f)) - popupY
        val revStartLeft = iconLeftLocal
        val revStartTop = iconTopLocal
        val revStartRight = iconLeftLocal + iconWidthPx
        val revStartBottom = iconTopLocal + iconHeightPx
        val revStartRadiusPx = iconWidthPx * 0.29f
        val finalRadiusPx = with(folderDensity) { 20.dp.toPx() }

        // ── Preview-icon morph (Launcher3 FolderAnimationManager): the up-to-
        // 4 preview icons shown in the CLOSED folder's 2x2 mini-grid fly to
        // their open-grid slots. miniCenters = each preview slot's center in
        // ROOT px; open cells 0..3 animate from here to their laid-out slot.
        val miniCenters: List<Offset> = if (iconBounds != null) {
            val ib = iconBounds
            val miniPad = ib.width * 0.12f
            val miniCell = (ib.width - miniPad * 2f) / 2f
            (0 until 4).map { k ->
                val col = k % 2; val row = k / 2
                Offset(
                    ib.left + miniPad + col * miniCell + miniCell / 2f,
                    ib.top + miniPad + row * miniCell + miniCell / 2f
                )
            }
        } else emptyList()
        // Scale relative to the FOLDER icon size (which may be auto-shrunk),
        // so the flying preview icon starts at roughly the closed mini-icon
        // size and lands at the actual in-folder icon size.
        val miniScale = if (iconBounds != null && folderIconPx > 0f)
            ((iconBounds.width * 0.38f) / folderIconPx).coerceIn(0.1f, 0.9f) else 0.4f

        // Invisible tap-catcher backdrop — Launcher3 / Lawnchair / Neo do
        // NOT dim or scrim the wallpaper when a folder opens (the opaque
        // folder card carries the contrast). So no .background here; the Box
        // exists only to catch tap-outside-to-close (and commit a pending
        // name edit first, mirroring the header's tap-to-close).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isFolderNameEditing) {
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
                    } else {
                        // If closing while in resize mode, commit the in-flight
                        // resize and exit resize mode so the resize outline +
                        // panel play their exit animation IN SYNC with the
                        // folder collapsing — one homogenous close, not the
                        // outline snapping away while the popup shrinks.
                        if (isResizingFolder) {
                            val newCust = (appCustomizations.customizations[folder.id]
                                ?: AppCustomization()).copy(
                                folderPopupWidthPx = resizeWidthOverride?.roundToInt(),
                                folderPopupHeightPx = resizeHeightOverride?.roundToInt(),
                                folderGridColumns = resizeColumnsOverride,
                                folderGridRows = resizeRowsOverride
                            )
                            appCustomizations = setCustomization(
                                context, appCustomizations, folder.id, newCust
                            )
                            isResizingFolder = false
                        }
                        openHomeFolder = null
                    }
                }
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(popupX.roundToInt(), popupY.roundToInt()) }
                .size(
                    width = with(folderDensity) { popupWpx.toDp() },
                    height = with(folderDensity) { popupHpx.toDp() }
                )
                // Escape-drag: keep the popup at full layout but invisible —
                // the home-grid overlay renders the dragged icon instead.
                // No scale (scale would distort child pointer coords).
                .graphicsLayer { alpha = if (escapedToHomeGrid) 0f else 1f }
                // Clip-reveal (Launcher3 RoundedRectRevealOutlineProvider):
                // the content is laid out full-size and drawn crisp, then
                // clipped to a rounded rect that grows from the folder-icon
                // rect to the full popup. drawWithContent sits BEFORE the
                // .clip/.background below so its drawContent() includes the
                // background + children, all clipped by the reveal path.
                .drawWithContent {
                    if (escapedToHomeGrid) { drawContent(); return@drawWithContent }
                    val p = folderAnimProgress.coerceIn(0f, 1f)
                    if (p >= 0.999f) { drawContent(); return@drawWithContent }
                    val l = revStartLeft + (0f - revStartLeft) * p
                    val t = revStartTop + (0f - revStartTop) * p
                    val r = revStartRight + (size.width - revStartRight) * p
                    val b = revStartBottom + (size.height - revStartBottom) * p
                    val rad = revStartRadiusPx + (finalRadiusPx - revStartRadiusPx) * p
                    val revealPath = Path().apply {
                        addRoundRect(RoundRect(l, t, r, b, CornerRadius(rad, rad)))
                    }
                    clipPath(revealPath) { this@drawWithContent.drawContent() }
                }
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.background)
                // Same 1dp outline as the closed folder icon on the home grid
                // (LocalFolderBorderColor). It sits after .background but
                // before .onGloballyPositioned, so it's part of the content
                // the reveal clip wipes open — the outline reveals with the card.
                // Fades OUT as the dashed resize outline (FolderResizeOverlay)
                // fades in, so the two never overlap / double up.
                .border(
                    1.dp,
                    com.bearinmind.launcher314.ui.theme.LocalFolderBorderColor.current.let {
                        // coerceIn: the spring overshoots past 1.0, so
                        // (1 - progress) can go slightly negative → negative
                        // alpha → Color.copy throws. Clamp to [0,1].
                        it.copy(alpha = (it.alpha * (1f - resizeAnimProgress)).coerceIn(0f, 1f))
                    },
                    RoundedCornerShape(20.dp)
                )
                .onGloballyPositioned { folderOverlayRootPos = it.positionInRoot() }
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact title bar — folder name (tap-to-edit) on the left,
            // overflow menu (3-dot) on the right with "Remove". Same bg as
            // the popup card so it reads as one continuous surface.
            // folderHeaderBottomY captures the bar's bottom so dragging an
            // app upward past it pulls the app out of the folder.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    // Animate the title + menu in with the same fade/scale as
                    // the app icons so the whole header isn't static while the
                    // grid flies out. Identity at p=1 so it never interferes
                    // once the folder is open. graphicsLayer is draw-only, so
                    // folderHeaderBottomY (measured below) stays accurate for
                    // escape-drag detection.
                    .graphicsLayer {
                        val mp = folderAnimProgress.coerceIn(0f, 1f)
                        if (mp < 0.999f && !escapedToHomeGrid) {
                            val a = ((mp - 0.12f) / 0.55f).coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.8f + 0.2f * a
                            scaleX = s; scaleY = s
                        }
                    }
                    .onGloballyPositioned { coords ->
                        folderHeaderBottomY = coords.positionInRoot().y + coords.size.height
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isFolderNameEditing) {
                    BasicTextField(
                        value = editedFolderName,
                        onValueChange = { editedFolderName = it },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
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
                            .widthIn(min = 100.dp, max = 240.dp),
                        decorationBox = { innerTextField -> innerTextField() }
                    )
                } else {
                    Text(
                        text = folder.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 56.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                editedFolderName = TextFieldValue(folder.name, TextRange(folder.name.length))
                                isFolderNameEditing = true
                            }
                    )
                }
                var headerMenuOpen by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                ) {
                    IconButton(onClick = { headerMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Folder options",
                            tint = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current
                        )
                    }
                    DropdownMenu(
                        expanded = headerMenuOpen,
                        onDismissRequest = { headerMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isResizingFolder) "Done resizing" else "Resize") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isResizingFolder) Icons.Outlined.Check
                                        else Icons.Outlined.AspectRatio,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                headerMenuOpen = false
                                if (isResizingFolder) {
                                    // Commit the in-flight overrides to the
                                    // folder's customization so they persist.
                                    val newCust = (appCustomizations.customizations[folder.id]
                                        ?: AppCustomization()).copy(
                                        folderPopupWidthPx = resizeWidthOverride?.roundToInt(),
                                        folderPopupHeightPx = resizeHeightOverride?.roundToInt(),
                                        folderGridColumns = resizeColumnsOverride,
                                        folderGridRows = resizeRowsOverride
                                    )
                                    appCustomizations = setCustomization(
                                        context, appCustomizations, folder.id, newCust
                                    )
                                    isResizingFolder = false
                                } else {
                                    // Start resize mode — seed the overrides
                                    // with the current values so steppers and
                                    // drag handles begin from the visible state.
                                    if (resizeWidthOverride == null) resizeWidthOverride = popupWpx
                                    if (resizeHeightOverride == null) resizeHeightOverride = popupHpx
                                    if (resizeColumnsOverride == null) resizeColumnsOverride = gridColumns
                                    if (resizeRowsOverride == null) resizeRowsOverride = gridRows
                                    isResizingFolder = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                headerMenuOpen = false
                                showFolderDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // Apps area — fills the rest of the popup below the title bar.
            // No own background; the outer popup Box draws it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                // Resolve cell map to HomeAppInfo for rendering. Reflow any
                // app whose stored index lands OUTSIDE the visible grid into
                // the first free in-grid cell — otherwise an app that ended up
                // at a high index (e.g. after escape-drag out then re-add left
                // a gap, or after the grid was shrunk) would be real in
                // appPackageNames but never rendered, so it shows in the closed
                // 2x2 preview (which compacts) yet vanishes when the folder is
                // opened. In-grid gaps are preserved so drag-reorder still works.
                val folderCellCount = (folderGridColumns * folderGridRows).coerceAtLeast(1)
                val folderCellAppMap = remember(folderCellMap, allAvailableApps, folderCellCount) {
                    reflowFolderCells(folderCellMap, allAvailableApps, folderCellCount)
                }
                val isDraggingInFolder = draggedPkg != null && !isFolderDropAnimating

                Column(modifier = Modifier.fillMaxSize().graphicsLayer { clip = false }) {
                    for (fRow in 0 until folderGridRows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer { clip = false }
                        ) {
                            for (fCol in 0 until folderGridColumns) {
                                val cellIdx = fRow * folderGridColumns + fCol
                                // Keep the gesture-owning cell rendering as an App
                                // for the whole drag (see resolveFolderCellApp) so
                                // its pointerInput isn't disposed mid-drag.
                                val cellApp = resolveFolderCellApp(folderCellAppMap, cellIdx, draggedPkg, draggedFolderCellIdx, allAvailableApps)
                                val isDragged = cellApp != null && draggedPkg == cellApp.packageName

                                val folderGridCell: HomeGridCell = if (cellApp != null) {
                                    HomeGridCell.App(cellApp, cellIdx)
                                } else {
                                    HomeGridCell.Empty
                                }

                                // Open/close morph for this cell. Preview
                                // slots (0..3, with an app) fly from the
                                // closed folder's 2x2 mini-grid to this slot
                                // (Launcher3 preview-item → content morph);
                                // the rest fade in with a slight stagger.
                                // Identity at p=1 so it never interferes with
                                // drag / resize once the folder is open.
                                val morphCellPos = folderCellPositions[cellIdx]
                                val isPreviewSlot = cellApp != null && cellIdx < miniCenters.size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            clip = false
                                            val mp = folderAnimProgress.coerceIn(0f, 1f)
                                            if (mp < 0.999f && !escapedToHomeGrid) {
                                                if (isPreviewSlot && morphCellPos != null &&
                                                    folderCellSize.width > 0) {
                                                    val cx = morphCellPos.x + folderCellSize.width / 2f
                                                    val cy = morphCellPos.y + folderCellSize.height / 2f
                                                    translationX = (miniCenters[cellIdx].x - cx) * (1f - mp)
                                                    translationY = (miniCenters[cellIdx].y - cy) * (1f - mp)
                                                    val s = miniScale + (1f - miniScale) * mp
                                                    scaleX = s; scaleY = s
                                                    alpha = ((mp - 0.05f) / 0.45f).coerceIn(0f, 1f)
                                                } else if (cellApp != null) {
                                                    val delay = (0.22f + 0.03f * cellIdx).coerceAtMost(0.6f)
                                                    val a = ((mp - delay) / (1f - delay)).coerceIn(0f, 1f)
                                                    alpha = a
                                                    val s = 0.75f + 0.25f * a
                                                    scaleX = s; scaleY = s
                                                }
                                            }
                                        }
                                ) {
                                    DraggableGridCell(
                                        cell = folderGridCell,
                                        index = cellIdx,
                                        iconSize = folderIconSizeDp,
                                        gridColumns = folderGridColumns,
                                        gridRows = folderGridRows,
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

                                                // Escape to the home grid as soon as the dragged center
                                                // leaves the popup rect on ANY side (above header bar,
                                                // below the popup, or off either side). Matches
                                                // Lawnchair / Neo Launcher feel — drag anywhere outside
                                                // the card and the folder closes back to the home grid.
                                                val outsidePopup =
                                                    dragCenter.y < folderHeaderBottomY ||
                                                    dragCenter.y > popupY + popupHpx ||
                                                    dragCenter.x < popupX ||
                                                    dragCenter.x > popupX + popupWpx
                                                if (outsidePopup && cellApp != null) {
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
                                                                    dockApps = dockApps + DockApp(remainingPkg, updatedDF.position, page = updatedDF.page)
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
                                                launchApp(context, cellApp.packageName, cellApp.userSerial)
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
                                                            dockApps = dockApps + DockApp(remainingPkg, updatedDF.position, page = updatedDF.page)
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
                        // Use folderIconSizeDp (the auto-shrunk in-folder size),
                        // NOT iconSizeDp — otherwise the picked-up icon renders
                        // at the full home size while the folder cells are
                        // smaller, so it looks oversized / squished against the
                        // cell. Matching the size keeps the drag icon 1:1 with
                        // the grid it came from.
                        OverlayAppContent(context, draggedApp, folderIconSizeDp, iconSizePercent, gridIconTextSpacer, gridAppNameFont, selectedFontFamily, overlayTextAlpha, globalIconShape, showLabel = true, globalIconBgColor = globalIconBgColor)
                    }
                }
            }
        }

        } // end wrapper Box

        // Widget-style resize overlay (see FolderResizeOverlay). Rendered as
        // a SIBLING of the popup Box (not a child) because the popup uses
        // .clip() which propagates to children's pointer hit-testing — any
        // edge/corner handle sticking OUT of the popup's rounded rect would
        // never receive touches if it were inside. Positions itself at the
        // popup's offset and size; outline + handles are drawn inside.
        //
        // Spring-driven enter/exit so the outline visually grows out of /
        // collapses back into the popup card — same spring parameters as
        // the folder open/close anim (Lawnchair's FolderSpringAnimatorSet).
        // Resize UI visibility tracks BOTH the resize enter/exit spring AND the
        // folder open/close progress: min() means when the folder is closing
        // (folderAnimProgress 1→0) the resize outline + panel collapse / fade
        // into the folder in lockstep with the popup — one homogenous close.
        // During a normal resize the folder is fully open (folderAnimProgress=1)
        // so this is just resizeAnimProgress.
        val resizeUiProgress = minOf(
            resizeAnimProgress,
            folderAnimProgress.coerceIn(0f, 1f)
        )
        if ((isResizingFolder || resizeAnimProgress > 0.001f) && resizeUiProgress > 0.001f) {
            FolderResizeOverlay(
                folderId = folder.id,
                popupOffsetXpx = popupX,
                popupOffsetYpx = popupY,
                popupWidthPx = popupWpx,
                popupHeightPx = popupHpx,
                minWidthPx = minPopupWpx,
                maxWidthPx = maxPopupWpx,
                minHeightPx = minPopupHpx,
                maxHeightPx = maxPopupHpx,
                onWidthChange = { resizeWidthOverride = it },
                onHeightChange = { resizeHeightOverride = it },
                progress = resizeUiProgress,
                interactive = isResizingFolder,
                // Folder-icon rect (root px) so the resize outline collapses
                // INTO the icon on close, matching the popup's clip-reveal.
                iconLeftPx = iconBounds?.left ?: (iconCenterX - iconWidthPx / 2f),
                iconTopPx = iconBounds?.top ?: (iconCenterY - iconHeightPx / 2f),
                iconRightPx = iconBounds?.right ?: (iconCenterX + iconWidthPx / 2f),
                iconBottomPx = iconBounds?.bottom ?: (iconCenterY + iconHeightPx / 2f)
            )
            // Control panel — sits at the top of the screen with steppers
            // for rows / columns + Reset / Done buttons. Drag handles
            // remain active in parallel so pixel-size and grid can both
            // be tuned during the same Resize session.
            FolderResizePanel(
                currentColumns = resizeColumnsOverride ?: gridColumns,
                currentRows = resizeRowsOverride ?: gridRows,
                progress = resizeUiProgress,
                interactive = isResizingFolder,
                // Popup in the TOP half → panel at the BOTTOM (opposite side),
                // so the panel never sits on top of the folder card.
                anchorBottom = (popupY + popupHpx / 2f) < screenHpx / 2f,
                onColumnsChange = { resizeColumnsOverride = it },
                onRowsChange = { resizeRowsOverride = it },
                onReset = {
                    resizeWidthOverride = null
                    resizeHeightOverride = null
                    resizeColumnsOverride = gridColumns
                    resizeRowsOverride = gridRows
                    val cleared = (appCustomizations.customizations[folder.id]
                        ?: AppCustomization()).copy(
                        folderPopupWidthPx = null,
                        folderPopupHeightPx = null,
                        folderGridColumns = null,
                        folderGridRows = null
                    )
                    appCustomizations = setCustomization(
                        context, appCustomizations, folder.id, cleared
                    )
                },
                onDone = {
                    val newCust = (appCustomizations.customizations[folder.id]
                        ?: AppCustomization()).copy(
                        folderPopupWidthPx = resizeWidthOverride?.roundToInt(),
                        folderPopupHeightPx = resizeHeightOverride?.roundToInt(),
                        folderGridColumns = resizeColumnsOverride,
                        folderGridRows = resizeRowsOverride
                    )
                    appCustomizations = setCustomization(
                        context, appCustomizations, folder.id, newCust
                    )
                    isResizingFolder = false
                }
            )
        }

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
                    // Wallpaper option — shows a chooser dialog (Cancel /
                    // Default / Custom) so the user can pick between the
                    // system picker and our custom editor.
                    DropdownMenuItem(
                        text = { Text("Wallpaper") },
                        onClick = {
                            showLauncherSettingsMenu = false
                            showWallpaperPickDialog = true
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
                                try {
                                    delay(350) // Match dot animation duration (300ms) + margin
                                } finally {
                                    addingLastDot = false
                                }
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
                                // Capture which page the user actually wants
                                // gone — previously this code only ever
                                // decremented totalPages, which removes the
                                // LAST page regardless of where the user is,
                                // so removing a middle page silently did
                                // nothing visible (apps still rendered at
                                // their original page index).
                                val pageToRemove = pagerState.currentPage
                                val targetAfter = (pageToRemove - 1).coerceAtLeast(0)
                                dropScope.launch {
                                    try {
                                        pagerState.animateScrollToPage(targetAfter)
                                        delay(350) // Let dot fade-out animation complete (300ms + margin)

                                        // 1) Strip every widget that lived on the
                                        //    removed page (deletes the host ID +
                                        //    cached view for each).
                                        val widgetsBefore = WidgetManager.loadPlacedWidgets(context)
                                        widgetsBefore
                                            .filter { it.page == pageToRemove }
                                            .forEach { WidgetManager.removePlacedWidget(context, it.appWidgetId) }
                                        // 2) Shift any widget on a later page
                                        //    down by 1 so its index lines up with
                                        //    the new pager layout.
                                        val widgetsShifted = WidgetManager.loadPlacedWidgets(context).map {
                                            if (it.page > pageToRemove) it.copy(page = it.page - 1) else it
                                        }
                                        WidgetManager.savePlacedWidgets(context, widgetsShifted)
                                        placedWidgets = widgetsShifted

                                        // 3) Apps + home folders: filter and shift
                                        //    in one pass, then persist.
                                        val newApps = homeApps
                                            .filter { it.page != pageToRemove }
                                            .map { if (it.page > pageToRemove) it.copy(page = it.page - 1) else it }
                                        val newFolders = homeFolders
                                            .filter { it.page != pageToRemove }
                                            .map { if (it.page > pageToRemove) it.copy(page = it.page - 1) else it }
                                        homeApps = newApps
                                        homeFolders = newFolders
                                        saveHomeScreenData(
                                            context,
                                            HomeScreenData(
                                                apps = newApps,
                                                dockApps = dockApps,
                                                folders = newFolders,
                                                dockFolders = dockFolders
                                            )
                                        )

                                        // 4) Drop the page count + persist.
                                        totalPages -= 1
                                        prefs.edit().putInt("launcher_total_pages", totalPages).apply()
                                    } finally {
                                        // Always clear the animation flag so
                                        // the last dot doesn't stay invisible
                                        // if something inside the try threw
                                        // (e.g. animateScrollToPage canceled).
                                        removingLastDot = false
                                    }
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

        // "Pick a new wallpaper" chooser. Mirrors the DeviceAudioEQ Reset
        // dialog styling: 1E1E1E rounded surface, light/medium grey title +
        // body, 1px divider, three equal-width OutlinedButtons across the
        // bottom (Cancel / Default / Custom).
        if (showWallpaperPickDialog) {
            Dialog(onDismissRequest = { showWallpaperPickDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 24.dp, top = 20.dp, end = 24.dp, bottom = 16.dp
                        )
                    ) {
                        Text(
                            text = "Pick a new wallpaper",
                            color = Color(0xFFE2E2E2),
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Pick a new wallpaper using a default system picker or a custom picker with multiple effects",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF444444))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { showWallpaperPickDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF444444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF9A9A),
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) { Text("Cancel") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    showWallpaperPickDialog = false
                                    openWallpaperPicker(context)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF444444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFDDDDDD),
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) { Text("Default") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    showWallpaperPickDialog = false
                                    pickCustomWallpaper.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF444444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFDDDDDD),
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) { Text("Custom") }
                        }
                    }
                }
            }
        }

        // Wallpaper editor — opens after the user picks an image via the
        // Custom path. If the editor was previously dismissed via Preview,
        // restore the in-flight edit (rather than re-reading saved prefs).
        // Consume the pending value so a fresh open later starts from prefs.
        customWallpaperSourcePath?.let { path ->
            val sourceFile = remember(path) { File(path) }
            if (sourceFile.exists()) {
                val resume = WallpaperPreviewBus.pendingResumeEdit
                val initialEdit = remember(path, resume) {
                    if (resume != null) {
                        WallpaperPreviewBus.pendingResumeEdit = null
                        resume
                    } else getDeviceWallpaperEdit(context)
                }
                WallpaperEditorScreen(
                    sourceFile = sourceFile,
                    initialEdit = initialEdit,
                    onDismiss = { customWallpaperSourcePath = null },
                    onApplied = {
                        customWallpaperSourcePath = null
                        setWallpaperMode(context, WALLPAPER_MODE_DEVICE)
                    },
                    // Editor dismisses itself after setting the preview
                    // bus; just close our local state so the preview backdrop
                    // (rendered by LauncherWithDrawer) becomes visible. The
                    // auto-reopen LaunchedEffect above brings the editor back
                    // when the user taps Exit on the preview overlay.
                    onRequestPreviewLauncher = { customWallpaperSourcePath = null }
                )
            } else {
                customWallpaperSourcePath = null
            }
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
            if (!com.bearinmind.launcher314.ui.theme.LocalHideIconText.current) {
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
