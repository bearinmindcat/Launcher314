@file:OptIn(ExperimentalMaterial3Api::class)

package com.bearinmind.launcher314.ui.settings

import android.content.Context
import com.bearinmind.launcher314.data.getHomeGridSize
import com.bearinmind.launcher314.data.setHomeGridSize
import com.bearinmind.launcher314.data.getHomeGridRows
import com.bearinmind.launcher314.data.setHomeGridRows
import com.bearinmind.launcher314.data.getHomeIconSizePercent
import com.bearinmind.launcher314.data.setHomeIconSizePercent
import com.bearinmind.launcher314.data.getDockColumns
import com.bearinmind.launcher314.data.setDockColumns
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import com.bearinmind.launcher314.ui.components.ThumbDragVerticalSlider
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.LazyGridScrollbar
import com.bearinmind.launcher314.ui.components.VerticalIconSizeSlider
import com.bearinmind.launcher314.ui.components.BottomColumnsSlider
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.bearinmind.launcher314.helpers.FontManager
import com.bearinmind.launcher314.helpers.getDrawerTransparency
import com.bearinmind.launcher314.helpers.setDrawerTransparency
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import com.bearinmind.launcher314.ui.widgets.WidgetManager
import com.bearinmind.launcher314.data.getGridSize
import com.bearinmind.launcher314.data.setGridSize
import com.bearinmind.launcher314.data.getDrawerIconSizePercent
import com.bearinmind.launcher314.data.setDrawerIconSizePercent
import com.bearinmind.launcher314.data.getSizeLinked
import com.bearinmind.launcher314.data.setSizeLinked
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.getScrollbarHeightPercent
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarIntensity
import com.bearinmind.launcher314.data.getDrawerGridRows
import com.bearinmind.launcher314.data.setDrawerGridRows
import com.bearinmind.launcher314.data.getDrawerPagedMode
import com.bearinmind.launcher314.data.setDrawerPagedMode
import com.bearinmind.launcher314.data.getIconTextSizePercent
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Wallpaper

// Calculate linked icon size percent from grid size (inverse relationship)
// At default: 100% * 4 cols = 400. So linkedIconPercent = 400 / gridSize
// Valid snap values for the grid size slider
private val validGridSizes = listOf(3, 4, 5, 6, 7)
// Valid linked icon percent values (one per grid size): 3→133, 4→100, 5→80, 6→67
private val validLinkedIconPercents = listOf(67, 80, 100, 133)

fun calculateLinkedIconPercent(gridSize: Int): Int {
    val rawValue = (400f / gridSize).roundToInt()
    return rawValue.coerceIn(50, 125)
}

// Calculate linked grid size from icon percent
fun calculateLinkedGridSize(iconPercent: Int): Int {
    val rawValue = (400f / iconPercent).roundToInt()
    // Snap to nearest valid grid size
    return validGridSizes.minByOrNull { kotlin.math.abs(it - rawValue) } ?: rawValue.coerceIn(3, 7)
}

// Preview data classes
data class PreviewAppInfo(
    val name: String,
    val packageName: String,
    val iconPath: String
)

@Serializable
data class PreviewFolder(
    val id: String,
    val name: String,
    val appPackageNames: List<String> = emptyList()
)

@Serializable
data class PreviewDrawerData(
    val folders: List<PreviewFolder> = emptyList()
)

// Sealed class to represent items in the preview grid
sealed class PreviewItem {
    data class FolderItem(val folder: PreviewFolder, val appIcons: List<String>) : PreviewItem()
    data class AppItem(val app: PreviewAppInfo) : PreviewItem()
}

/**
 * Complete preview grid section with sliders and controls
 */
@Composable
fun AppDrawerPreviewSection(
    onPreviewDrawer: () -> Unit,
    onGridSizeChanged: (Int) -> Unit = {},
    // Optional scrollbar overrides for real-time preview updates
    scrollbarWidthOverride: Int? = null,
    scrollbarHeightOverride: Int? = null,
    scrollbarColorOverride: Int? = null,
    scrollbarIntensityOverride: Int? = null,
    iconTextSizeOverride: Int? = null,
    sharedIconSize: Float? = null,
    onSharedIconSizeChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for the sliders - will be synced with SharedPreferences
    var currentGridSize by remember { mutableFloatStateOf(getGridSize(context).toFloat()) }
    var currentIconSizePercent by remember { mutableFloatStateOf(getDrawerIconSizePercent(context).toFloat()) }
    // var isLinked by remember { mutableStateOf(getSizeLinked(context)) }
    val isLinked = false // Link button hidden — keep variable for minimal code changes
    var drawerTransparency by remember { mutableFloatStateOf(getDrawerTransparency(context).toFloat()) }
    var drawerGridRows by remember { mutableFloatStateOf(getDrawerGridRows(context).toFloat()) }
    var isPagedMode by remember { mutableStateOf(getDrawerPagedMode(context)) }
    var selectedFontFamily by remember { mutableStateOf(FontManager.getSelectedFontFamily(context)) }

    // Sync icon size from shared state (updated by either section's slider)
    LaunchedEffect(sharedIconSize) {
        if (sharedIconSize != null && sharedIconSize != currentIconSizePercent) {
            currentIconSizePercent = sharedIconSize
            setDrawerIconSizePercent(context, sharedIconSize.roundToInt())
        }
    }

    // Scrollbar settings (percentages) - use overrides if provided, otherwise load from SharedPreferences
    val scrollbarWidthPercent = scrollbarWidthOverride ?: remember { getScrollbarWidthPercent(context) }
    val scrollbarHeightPercent = scrollbarHeightOverride ?: remember { getScrollbarHeightPercent(context) }
    val scrollbarColor = scrollbarColorOverride ?: remember { getScrollbarColor(context) }
    val scrollbarIntensity = scrollbarIntensityOverride ?: remember { getScrollbarIntensity(context) }

    // Refresh from SharedPreferences when screen becomes RESUMED
    // This works with Compose Navigation because NavBackStackEntry has its own lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-read all values from SharedPreferences
                currentGridSize = getGridSize(context).toFloat()
                currentIconSizePercent = getDrawerIconSizePercent(context).toFloat()
                drawerTransparency = getDrawerTransparency(context).toFloat()
                drawerGridRows = getDrawerGridRows(context).toFloat()
                isPagedMode = getDrawerPagedMode(context)
                selectedFontFamily = FontManager.getSelectedFontFamily(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load apps and folders for preview
    var previewApps by remember { mutableStateOf<List<PreviewAppInfo>>(emptyList()) }
    var previewFolders by remember { mutableStateOf<List<PreviewFolder>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            previewApps = loadPreviewApps(context)
            previewFolders = loadPreviewFolders(context)
        }
    }

    // Load wallpaper for preview - try actual wallpaper first, fall back to built-in
    var wallpaperDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val loadedDrawable = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        wallpaperManager.getDrawable(android.app.WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.drawable
                    }
                } catch (e: SecurityException) {
                    null
                } ?: wallpaperManager.builtInDrawable
                withContext(Dispatchers.Main) {
                    wallpaperDrawable = loadedDrawable
                }
            } catch (e: Exception) {
                // Wallpaper access failed
            }
        }
    }

    // Build the preview items list (folders first, then apps not in folders)
    val previewItems by remember(previewApps, previewFolders) {
        derivedStateOf {
            val appsInFolders = previewFolders.flatMap { it.appPackageNames.filter { pkg -> pkg.isNotEmpty() } }.toSet()
            val items = mutableListOf<PreviewItem>()

            // Add folders first
            previewFolders.forEach { folder ->
                val folderAppIcons = folder.appPackageNames
                    .filter { it.isNotEmpty() }
                    .mapNotNull { pkgName -> previewApps.find { it.packageName == pkgName }?.iconPath }
                    .take(4)
                items.add(PreviewItem.FolderItem(folder, folderAppIcons))
            }

            // Add apps that are NOT in folders
            previewApps
                .filter { it.packageName !in appsInFolders }
                .forEach { app ->
                    items.add(PreviewItem.AppItem(app))
                }

            items.toList()
        }
    }

    // Calculate the preview height based on screen
    val configuration = LocalConfiguration.current
    val previewHeight = (configuration.screenHeightDp * 0.4f).dp

    // Compute UNIVERSAL overflow threshold — min of drawer and home screen thresholds
    // Icon formula: screenWidth / 4 * 0.55 * pct/100
    val universalOverflowThreshold = run {
        val swDp = configuration.screenWidthDp.toFloat()
        val shDp = configuration.screenHeightDp.toFloat()
        val iconRef = swDp / 4f * 0.55f  // icon dp at 100%

        // Drawer threshold
        val gridSizeInt = currentGridSize.roundToInt()
        val drawerHPad = if (isPagedMode) 16f else 28f
        val drawerCellWidth = (swDp - drawerHPad) / gridSizeInt
        val drawerThreshold = ((drawerCellWidth - 16f) / iconRef * 100f).coerceIn(50f, 125f)

        // Home screen threshold (read stored settings)
        val homeGridCols = getHomeGridSize(context)
        val homeGridRows = getHomeGridRows(context)
        val homeHPad = swDp * 0.044f
        val homeCellWidth = (swDp - homeHPad * 2) / homeGridCols
        val homeCellBasis = minOf(homeCellWidth, (shDp - 76f - swDp * 0.022f * 2) / homeGridRows)
        val homeMarkerPadding = homeCellBasis * 0.073f * 2f
        val homeThreshold = ((homeCellWidth - homeMarkerPadding) / iconRef * 100f).coerceIn(50f, 125f)

        minOf(drawerThreshold, homeThreshold)
    }

    // Auto-snap icon size down when threshold drops below current value
    LaunchedEffect(universalOverflowThreshold) {
        if (currentIconSizePercent > universalOverflowThreshold) {
            // Snap to highest usable value
            val snapTicks = if (isLinked) listOf(67, 80, 100, 133).filter { it <= 125 } else (50..125 step 5).toList()
            val maxSnap = snapTicks.filter { it.toFloat() <= universalOverflowThreshold }.maxOrNull()?.toFloat() ?: 50f
            currentIconSizePercent = maxSnap
            setDrawerIconSizePercent(context, maxSnap.roundToInt())
            setHomeIconSizePercent(context, maxSnap.roundToInt())
            onSharedIconSizeChanged(maxSnap)
        }
    }

    // Preview section with sliders - grid layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top row: Preview + Icon slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Preview - shifted right to match columns slider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                RealAppDrawerPreview(
                    items = previewItems,
                    gridSize = currentGridSize.roundToInt(),
                    iconSizePercent = currentIconSizePercent.roundToInt(),
                    transparency = drawerTransparency.roundToInt(),
                    wallpaperDrawable = wallpaperDrawable,
                    scrollbarWidthPercent = scrollbarWidthPercent,
                    scrollbarHeightPercent = scrollbarHeightPercent,
                    scrollbarColor = scrollbarColor,
                    scrollbarIntensity = scrollbarIntensity,
                    drawerGridRows = drawerGridRows.roundToInt(),
                    isPagedMode = isPagedMode,
                    iconTextSizePercent = iconTextSizeOverride ?: getIconTextSizePercent(LocalContext.current),
                    labelFontFamily = selectedFontFamily,
                    onPlayClick = onPreviewDrawer
                )
            }

            // Vertical Icon Size Slider (50-125, red zone above overflow threshold)
            VerticalIconSizeSlider(
                currentSize = currentIconSizePercent,
                sliderHeight = previewHeight + 16.dp,
                isLinked = isLinked,
                overflowThreshold = universalOverflowThreshold,
                onSizeChange = { newSize ->
                    currentIconSizePercent = newSize
                    setDrawerIconSizePercent(context, newSize.roundToInt())
                    setHomeIconSizePercent(context, newSize.roundToInt())
                    onSharedIconSizeChanged(newSize)
                    // If linked, update grid size to match
                    if (isLinked) {
                        val newGridSize = calculateLinkedGridSize(newSize.roundToInt()).toFloat()
                        if (newGridSize != currentGridSize) {
                            currentGridSize = newGridSize
                            setGridSize(context, newGridSize.roundToInt())
                        }
                    }
                },
                onSizeChangeFinished = {
                    setDrawerIconSizePercent(context, currentIconSizePercent.roundToInt())
                    setHomeIconSizePercent(context, currentIconSizePercent.roundToInt())
                    if (isLinked) {
                        val size = currentGridSize.roundToInt()
                        setGridSize(context, size)
                        onGridSizeChanged(size)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom row: Columns slider + Link button at intersection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top // Align at top so link button sits at slider level
        ) {
            // Columns slider - shifted right to center under preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                BottomColumnsSlider(
                    currentSize = currentGridSize,
                    onSizeChange = { newSize ->
                        currentGridSize = newSize
                        setGridSize(context, newSize.roundToInt())
                        // If linked, update icon size to match
                        if (isLinked) {
                            val newIconPercent = calculateLinkedIconPercent(newSize.roundToInt()).toFloat()
                            if (newIconPercent != currentIconSizePercent) {
                                currentIconSizePercent = newIconPercent
                                setDrawerIconSizePercent(context, newIconPercent.roundToInt())
                            }
                        }
                    },
                    onSizeChangeFinished = {
                        val size = currentGridSize.roundToInt()
                        setGridSize(context, size)
                        onGridSizeChanged(size)
                        if (isLinked) {
                            setDrawerIconSizePercent(context, currentIconSizePercent.roundToInt())
                        }
                    }
                )
            }

            // Link button — commented out, kept for future use
//            Box(
//                modifier = Modifier
//                    .width(72.dp)
//                    .height(48.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                IconButton(
//                    onClick = {
//                        isLinked = !isLinked
//                        setSizeLinked(context, isLinked)
//                        // If just linked, sync icon size to current grid size
//                        if (isLinked) {
//                            val linkedPercent = calculateLinkedIconPercent(currentGridSize.roundToInt()).toFloat()
//                            currentIconSizePercent = linkedPercent
//                            setDrawerIconSizePercent(context, linkedPercent.roundToInt())
//                        }
//                    },
//                    modifier = Modifier.offset(x = 10.dp)
//                ) {
//                    Icon(
//                        imageVector = if (isLinked) Icons.Filled.Link else Icons.Filled.LinkOff,
//                        contentDescription = if (isLinked) "Linked" else "Unlinked",
//                        tint = if (isLinked)
//                            MaterialTheme.colorScheme.primary
//                        else
//                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
//                    )
//                }
//            }
            // Spacer to maintain layout alignment with rows slider row
            Spacer(modifier = Modifier.width(72.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rows slider + Paged mode checkbox (same layout as columns slider + link button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Rows slider (interaction disabled when paged mode is off, always full visibility)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                DrawerRowsSlider(
                    currentSize = drawerGridRows,
                    enabled = isPagedMode,
                    onSizeChange = { newSize ->
                        drawerGridRows = newSize
                        setDrawerGridRows(context, newSize.roundToInt())
                    },
                    onSizeChangeFinished = {
                        setDrawerGridRows(context, drawerGridRows.roundToInt())
                    }
                )
            }

            // Paged mode checkbox - same container as link icon (72dp wide, 48dp tall)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isPagedMode,
                    onCheckedChange = { checked ->
                        isPagedMode = checked
                        setDrawerPagedMode(context, checked)
                    },
                    modifier = Modifier.offset(x = 10.dp), // Same offset as link icon
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transparency slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp) // Match columns slider layout (72dp + 4dp spacing)
        ) {
            DrawerTransparencySlider(
                currentTransparency = drawerTransparency,
                onTransparencyChange = { newValue ->
                    drawerTransparency = newValue
                    setDrawerTransparency(context, newValue.roundToInt())
                },
                onTransparencyChangeFinished = {
                    setDrawerTransparency(context, drawerTransparency.roundToInt())
                }
            )
        }
    }
}

/**
 * Horizontal transparency slider for App Drawer (0% to 100%)
 * Snaps to 5% increments with tick marks at every 5%
 */
@Composable
private fun DrawerTransparencySlider(
    currentTransparency: Float,
    onTransparencyChange: (Float) -> Unit,
    onTransparencyChangeFinished: () -> Unit
) {
    ThumbDragHorizontalSlider(
        currentValue = currentTransparency,
        config = SliderConfigs.drawerTransparency,
        onValueChange = onTransparencyChange,
        onValueChangeFinished = onTransparencyChangeFinished
    )
}

/**
 * Horizontal rows slider for App Drawer paged mode (4 to 7 rows)
 */
@Composable
private fun DrawerRowsSlider(
    currentSize: Float,
    enabled: Boolean = true,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    ThumbDragHorizontalSlider(
        currentValue = currentSize,
        config = SliderConfigs.drawerRows,
        onValueChange = onSizeChange,
        onValueChangeFinished = onSizeChangeFinished,
        enabled = enabled
    )
}

@Composable
private fun RealAppDrawerPreview(
    items: List<PreviewItem>,
    gridSize: Int,
    iconSizePercent: Int = 100,
    transparency: Int = 100,
    wallpaperDrawable: android.graphics.drawable.Drawable? = null,
    scrollbarWidthPercent: Int = 100,
    scrollbarHeightPercent: Int = 100,
    scrollbarColor: Int = 0xFFFFFFFF.toInt(),
    scrollbarIntensity: Int = 100,
    drawerGridRows: Int = 6,
    isPagedMode: Boolean = false,
    iconTextSizePercent: Int = 100,
    labelFontFamily: FontFamily? = null,
    onPlayClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val scaleFactor = 0.4f
    val previewWidth = screenWidth * scaleFactor
    val previewHeight = screenHeight * scaleFactor

    // Compute icon dp from percentage using fixed reference (screenWidth / 4)
    // Uses reference column count of 4 so icon size is consistent across screens
    val iconSize = (configuration.screenWidthDp.toFloat() / 4f * 0.55f * iconSizePercent / 100f).toInt()
    val baseIconSize = iconSize.dp * scaleFactor
    val baseFontSize = 12.sp * scaleFactor * iconTextSizePercent / 100f
    val baseSpacing = 8.dp * scaleFactor
    val basePadding = 8.dp * scaleFactor
    val statusBarHeight = 24.dp * scaleFactor
    val searchBarHeight = 48.dp * scaleFactor
    val navBarHeight = 20.dp * scaleFactor
    val tinyIconSize = 10.dp * scaleFactor
    val smallIconSize = 14.dp * scaleFactor

    // Compute scrollbar dp from percentage (100% = 2% screen width / 20% screen height)
    val scrollbarWidth = (configuration.screenWidthDp * 0.02f * scrollbarWidthPercent / 100f).toInt()
    val scrollbarHeight = (configuration.screenHeightDp * 0.20f * scrollbarHeightPercent / 100f).toInt()
    // Scaled scrollbar dimensions (scaled down extra for accurate visual representation)
    val scrollbarScaleFactor = scaleFactor * 0.6f  // Extra scale for visual accuracy
    val scaledScrollbarWidth = (scrollbarWidth * scrollbarScaleFactor).dp
    val scaledScrollbarHeight = (scrollbarHeight * scrollbarScaleFactor).dp
    val scrollbarComposeColor = Color(scrollbarColor)

    // Intensity-adjusted color for navigation dots
    val dotColor = remember(scrollbarColor, scrollbarIntensity) {
        val base = Color(scrollbarColor)
        val factor = (scrollbarIntensity / 100f).coerceIn(0f, 1f)
        Color(
            red = base.red * factor,
            green = base.green * factor,
            blue = base.blue * factor,
            alpha = base.alpha
        )
    }

    // Calculate background color with transparency
    // 0% = fully opaque (no transparency, show black background)
    // 100% = fully transparent (show wallpaper)
    val backgroundAlpha = (100 - transparency) / 100f
    val backgroundColor = Color(0xFF121212).copy(alpha = backgroundAlpha)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview container
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onPlayClick() }
        ) {
            // Wallpaper layer (behind everything)
            if (wallpaperDrawable != null) {
                val painter = rememberDrawablePainter(drawable = wallpaperDrawable)
                Image(
                    painter = painter,
                    contentDescription = "Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback dark background if no wallpaper
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                )
            }

            // Semi-transparent overlay (controlled by transparency slider)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )

            // Grid state for scrollbar (declared at this level so scrollbar can access it)
            val gridState = rememberLazyGridState()

            Column(modifier = Modifier.fillMaxSize()) {
                // Status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .padding(horizontal = basePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12:00",
                        fontSize = (8.sp * scaleFactor),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SignalCellular4Bar,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Filled.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(searchBarHeight)
                        .padding(horizontal = basePadding, vertical = 2.dp)
                        .clip(RoundedCornerShape(searchBarHeight / 2))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = basePadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(basePadding / 2)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(smallIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Search",
                            fontSize = (9.sp * scaleFactor),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(smallIconSize),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // App grid with scrollbar
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (isPagedMode) {
                    // Paged mode: fixed grid with drawerGridRows rows
                    val itemsPerPage = gridSize * drawerGridRows
                    val pageItems = items.take(itemsPerPage) // Show first page in preview

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = basePadding),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (row in 0 until drawerGridRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                for (col in 0 until gridSize) {
                                    val itemIndex = row * gridSize + col
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (itemIndex < pageItems.size) {
                                            when (val item = pageItems[itemIndex]) {
                                                is PreviewItem.FolderItem -> ScaledPreviewFolderItem(
                                                    folder = item.folder,
                                                    appIcons = item.appIcons,
                                                    iconSize = baseIconSize,
                                                    fontSize = baseFontSize,
                                                    fontFamily = labelFontFamily
                                                )
                                                is PreviewItem.AppItem -> ScaledPreviewAppItem(
                                                    app = item.app,
                                                    iconSize = baseIconSize,
                                                    fontSize = baseFontSize,
                                                    fontFamily = labelFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Page indicator dots (matching actual app drawer)
                    val totalPages = if (items.isEmpty()) 1
                        else (items.size + itemsPerPage - 1) / itemsPerPage
                    if (totalPages > 1) {
                        // Fixed height container so dot size changes don't affect grid icon sizes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((scrollbarWidth.dp + 4.dp) * scaleFactor)
                                .padding(bottom = basePadding / 2),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp * scaleFactor, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(totalPages) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(scrollbarWidth.dp * scaleFactor)
                                            .clip(CircleShape)
                                            .background(
                                                if (index == 0)
                                                    dotColor.copy(alpha = 0.9f)
                                                else
                                                    dotColor.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Scroll mode: LazyVerticalGrid
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridSize),
                        contentPadding = PaddingValues(
                            start = basePadding,
                            end = basePadding * 2.5f,
                            top = basePadding * 2,
                            bottom = basePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(baseSpacing / 2),
                        verticalArrangement = Arrangement.spacedBy(baseSpacing),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        userScrollEnabled = true
                    ) {
                        items(
                            items = items,
                            key = { item ->
                                when (item) {
                                    is PreviewItem.FolderItem -> "folder_${item.folder.id}"
                                    is PreviewItem.AppItem -> "app_${item.app.packageName}"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is PreviewItem.FolderItem -> ScaledPreviewFolderItem(
                                    folder = item.folder,
                                    appIcons = item.appIcons,
                                    iconSize = baseIconSize,
                                    fontSize = baseFontSize,
                                    fontFamily = labelFontFamily
                                )
                                is PreviewItem.AppItem -> ScaledPreviewAppItem(
                                    app = item.app,
                                    iconSize = baseIconSize,
                                    fontSize = baseFontSize,
                                    fontFamily = labelFontFamily
                                )
                            }
                        }
                    }
                }

                // Navigation bar area (transparent background, visible gesture bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(navBarHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // Gesture bar indicator (visible)
                    Box(
                        modifier = Modifier
                            .width(previewWidth * 0.3f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }

            // Scrollbar overlay on the right edge (matching actual app drawer - no track, hidden in paged mode)
            if (items.isNotEmpty() && !isPagedMode) {
                LazyGridScrollbar(
                    gridState = gridState,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .padding(top = statusBarHeight + searchBarHeight, bottom = navBarHeight + 2.dp),
                    thumbColor = scrollbarComposeColor.copy(alpha = 0.5f),
                    thumbSelectedColor = scrollbarComposeColor.copy(alpha = 0.8f),
                    trackColor = Color.Transparent,  // No track, just thumb
                    thumbWidth = scaledScrollbarWidth,
                    thumbMinHeight = scaledScrollbarHeight,
                    scrollbarPadding = 0.dp,
                    alwaysShow = true
                )
            }

            // Play button overlay in center
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open App Drawer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

    }
}

@Composable
private fun ScaledPreviewFolderItem(
    folder: PreviewFolder,
    appIcons: List<String>,
    iconSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily? = null,
    showPlusMarker: Boolean = false,
    scaleFactor: Float = 0.4f
) {
    val miniIconSize = iconSize / 2.2f
    val markerHalfSize = 6.dp * scaleFactor

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcons.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.5.dp)) {
                            appIcons.getOrNull(0)?.let { iconPath ->
                                AsyncImage(
                                    model = File(iconPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            appIcons.getOrNull(1)?.let { iconPath ->
                                AsyncImage(
                                    model = File(iconPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(0.5.dp)) {
                            appIcons.getOrNull(2)?.let { iconPath ->
                                AsyncImage(
                                    model = File(iconPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            appIcons.getOrNull(3)?.let { iconPath ->
                                AsyncImage(
                                    model = File(iconPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = folder.name,
                fontSize = fontSize,
                fontFamily = fontFamily ?: FontFamily.Default,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // "+" marker at bottom-right corner (always visible in preview)
        if (showPlusMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = markerHalfSize, y = markerHalfSize)
                    .size(12.dp * scaleFactor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = (10.sp * scaleFactor),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ScaledPreviewAppItem(
    app: PreviewAppInfo,
    iconSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily? = null,
    showPlusMarker: Boolean = false,
    scaleFactor: Float = 0.4f
) {
    val markerHalfSize = 6.dp * scaleFactor

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = File(app.iconPath),
                contentDescription = app.name,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = app.name,
                fontSize = fontSize,
                fontFamily = fontFamily ?: FontFamily.Default,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // "+" marker at bottom-right corner (always visible in preview)
        if (showPlusMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = markerHalfSize, y = markerHalfSize)
                    .size(12.dp * scaleFactor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = (10.sp * scaleFactor),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Load folders from drawer_data.json
fun loadPreviewFolders(context: Context): List<PreviewFolder> {
    return try {
        val file = File(context.filesDir, "drawer_data.json")
        if (file.exists()) {
            val data = Json.decodeFromString<PreviewDrawerData>(file.readText())
            data.folders
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Load apps for preview
fun loadPreviewApps(context: Context): List<PreviewAppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val iconsDir = File(context.cacheDir, "app_icons")
    if (!iconsDir.exists()) {
        iconsDir.mkdirs()
    }

    val resolveInfoList = packageManager.queryIntentActivities(intent, 0)

    return resolveInfoList
        .mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val iconFile = File(iconsDir, "$packageName.png")

                if (!iconFile.exists()) {
                    val drawable = packageManager.getApplicationIcon(packageName)
                    val bitmap = drawableToBitmapPreview(drawable)
                    saveBitmapToFilePreview(bitmap, iconFile)
                    bitmap.recycle()
                }

                PreviewAppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    iconPath = iconFile.absolutePath
                )
            } catch (e: Exception) {
                null
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.name.lowercase() }
}

private fun drawableToBitmapPreview(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun saveBitmapToFilePreview(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

// ============================================================================
// HOME SCREEN PREVIEW SECTION
// ============================================================================

/**
 * Check if we have permission to read wallpaper.
 * On Android 13+ (API 33), we need READ_MEDIA_IMAGES.
 * On older versions, we need READ_EXTERNAL_STORAGE.
 */
fun checkWallpaperPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Get the appropriate permission to request for wallpaper access.
 */
fun getWallpaperPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

// Home screen prefs imported from com.bearinmind.launcher314.data.HomeScreenPrefs

// Data classes for home screen apps (matching LauncherScreen)
@Serializable
data class HomeScreenAppData(
    val packageName: String,
    val position: Int
)

@Serializable
data class HomeDockAppData(
    val packageName: String,
    val position: Int
)

@Serializable
data class HomeFolderData(
    val id: String = "",
    val name: String = "",
    val position: Int = 0,
    val page: Int = 0,
    val appPackageNames: List<String> = emptyList()
)

@Serializable
data class HomeScreenDataFile(
    val apps: List<HomeScreenAppData> = emptyList(),
    val dockApps: List<HomeDockAppData> = emptyList(),
    val folders: List<HomeFolderData> = emptyList()
)

// Sealed class for preview grid cell content
sealed class HomePreviewCell {
    data class App(val app: PreviewAppInfo) : HomePreviewCell()
    data class Folder(val folder: HomeFolderData, val previewApps: List<PreviewAppInfo>) : HomePreviewCell()
    data class WidgetOrigin(val widget: PlacedWidget) : HomePreviewCell()
    object WidgetSpan : HomePreviewCell()  // cells occupied by a multi-cell widget (not origin)
}

// Json instance that tolerates extra fields (page, etc.) from the actual launcher data
private val previewJson = Json { ignoreUnknownKeys = true }

// Load home screen apps for preview
fun loadHomeScreenApps(context: Context): List<HomeScreenAppData> {
    return try {
        val file = File(context.filesDir, "home_screen_data.json")
        if (file.exists()) {
            previewJson.decodeFromString<HomeScreenDataFile>(file.readText()).apps
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Load dock apps for preview
fun loadDockApps(context: Context): List<HomeDockAppData> {
    return try {
        val file = File(context.filesDir, "home_screen_data.json")
        if (file.exists()) {
            previewJson.decodeFromString<HomeScreenDataFile>(file.readText()).dockApps
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Load home folders for preview
fun loadHomeFolders(context: Context): List<HomeFolderData> {
    return try {
        val file = File(context.filesDir, "home_screen_data.json")
        if (file.exists()) {
            previewJson.decodeFromString<HomeScreenDataFile>(file.readText()).folders
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Load placed widgets directly from SharedPreferences (bypasses WidgetManager init)
fun loadPreviewWidgets(context: Context): List<PlacedWidget> {
    return try {
        val prefs = context.getSharedPreferences("launcher_widgets", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("placed_widgets", null) ?: return emptyList()
        previewJson.decodeFromString<List<PlacedWidget>>(jsonString)
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Home Screen preview section with sliders for grid and icon size
 * Same style as App Drawer preview with track sliders
 */
@Composable
fun HomeScreenPreviewSection(
    onPreviewLauncher: () -> Unit = {},
    iconTextSizeOverride: Int? = null,
    sharedIconSize: Float? = null,
    onSharedIconSizeChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Grid settings (columns x rows), dock columns, and icon size
    var gridColumns by remember { mutableFloatStateOf(getHomeGridSize(context).toFloat()) }
    var gridRows by remember { mutableFloatStateOf(getHomeGridRows(context).toFloat()) }
    var dockColumns by remember { mutableFloatStateOf(getDockColumns(context).toFloat()) }
    var iconSizePercent by remember { mutableFloatStateOf(getHomeIconSizePercent(context).toFloat()) }
    var selectedFontFamily by remember { mutableStateOf(FontManager.getSelectedFontFamily(context)) }

    // Sync icon size from shared state (updated by either section's slider)
    LaunchedEffect(sharedIconSize) {
        if (sharedIconSize != null && sharedIconSize != iconSizePercent) {
            iconSizePercent = sharedIconSize
            setHomeIconSizePercent(context, sharedIconSize.roundToInt())
        }
    }

    // Load home screen apps, dock apps, folders, and widgets for preview
    var homeScreenApps by remember { mutableStateOf<List<HomeScreenAppData>>(emptyList()) }
    var dockApps by remember { mutableStateOf<List<HomeDockAppData>>(emptyList()) }
    var allApps by remember { mutableStateOf<List<PreviewAppInfo>>(emptyList()) }
    var homeFolders by remember { mutableStateOf<List<HomeFolderData>>(emptyList()) }
    var placedWidgets by remember { mutableStateOf<List<PlacedWidget>>(emptyList()) }

    // Widget bitmaps - kept outside key(iconSizePercent) so they persist across icon size changes
    val widgetBitmaps = remember { mutableStateMapOf<Int, android.graphics.Bitmap>() }

    // Load wallpaper for preview - try actual wallpaper first, fall back to built-in
    var wallpaperDrawable by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val loadedDrawable = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        wallpaperManager.getDrawable(android.app.WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.drawable
                    }
                } catch (e: SecurityException) {
                    null
                } ?: wallpaperManager.builtInDrawable
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    wallpaperDrawable = loadedDrawable
                }
            } catch (e: Exception) {
                // Wallpaper access failed
            }
        }
    }

    // Refresh from SharedPreferences when screen becomes RESUMED
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                gridColumns = getHomeGridSize(context).toFloat()
                gridRows = getHomeGridRows(context).toFloat()
                dockColumns = getDockColumns(context).toFloat()
                iconSizePercent = getHomeIconSizePercent(context).toFloat()
                selectedFontFamily = FontManager.getSelectedFontFamily(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load apps, folders, and widgets
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            homeScreenApps = loadHomeScreenApps(context)
            dockApps = loadDockApps(context)
            allApps = loadPreviewApps(context)
            homeFolders = loadHomeFolders(context)
            placedWidgets = WidgetManager.loadPlacedWidgets(context)
        }
    }

    // Calculate the preview height based on screen
    val configuration = LocalConfiguration.current
    val previewHeight = (configuration.screenHeightDp * 0.4f).dp

    // Compute UNIVERSAL overflow threshold — min of home screen and drawer thresholds
    // Icon formula: screenWidth / 4 * 0.55 * pct/100
    val screenWidthDpVal = configuration.screenWidthDp.toFloat()
    val screenHeightDpVal = configuration.screenHeightDp.toFloat()
    val iconRef = screenWidthDpVal / 4f * 0.55f  // icon dp at 100%

    val universalOverflowThreshold = run {
        // Home screen threshold
        val hPad = screenWidthDpVal * 0.044f
        val homeCellWidth = (screenWidthDpVal - hPad * 2) / gridColumns.roundToInt()
        val cellBasis = minOf(homeCellWidth, (screenHeightDpVal - 76f - screenWidthDpVal * 0.022f * 2) / gridRows.roundToInt())
        val markerPadding = cellBasis * 0.073f * 2f
        val homeThreshold = ((homeCellWidth - markerPadding) / iconRef * 100f).coerceIn(50f, 125f)

        // Drawer threshold (read stored settings)
        val drawerGridSize = getGridSize(context)
        val drawerPaged = getDrawerPagedMode(context)
        val drawerHPad = if (drawerPaged) 16f else 28f
        val drawerCellWidth = (screenWidthDpVal - drawerHPad) / drawerGridSize
        val drawerThreshold = ((drawerCellWidth - 16f) / iconRef * 100f).coerceIn(50f, 125f)

        minOf(homeThreshold, drawerThreshold)
    }

    // Auto-snap icon size down when threshold drops below current value
    LaunchedEffect(universalOverflowThreshold) {
        if (iconSizePercent > universalOverflowThreshold) {
            val snapTicks = (50..125 step 5).toList()
            val maxSnap = snapTicks.filter { it.toFloat() <= universalOverflowThreshold }.maxOrNull()?.toFloat() ?: 50f
            iconSizePercent = maxSnap
            setHomeIconSizePercent(context, maxSnap.roundToInt())
            setDrawerIconSizePercent(context, maxSnap.roundToInt())
            onSharedIconSizeChanged(maxSnap)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top row: Preview + Icon size slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Preview - shifted right to match columns slider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                    HomeScreenPreview(
                        gridColumns = gridColumns.roundToInt(),
                        gridRows = gridRows.roundToInt(),
                        dockColumns = dockColumns.roundToInt(),
                        iconSizePercent = iconSizePercent.roundToInt(),
                        iconTextSizePercent = iconTextSizeOverride ?: getIconTextSizePercent(context),
                        homeScreenApps = homeScreenApps,
                        dockApps = dockApps,
                        allApps = allApps,
                        homeFolders = homeFolders,
                        placedWidgets = placedWidgets,
                        widgetBitmaps = widgetBitmaps,
                        labelFontFamily = selectedFontFamily,
                        wallpaperDrawable = wallpaperDrawable,
                        onPlayClick = onPreviewLauncher
                    )
            }

            // Vertical Icon Size Slider (50-125, red zone above overflow threshold)
            HomeVerticalIconSizeSlider(
                currentSize = iconSizePercent,
                sliderHeight = previewHeight + 16.dp,
                overflowThreshold = universalOverflowThreshold,
                onSizeChange = { newSize ->
                    iconSizePercent = newSize
                    setHomeIconSizePercent(context, newSize.roundToInt())
                    setDrawerIconSizePercent(context, newSize.roundToInt())
                    onSharedIconSizeChanged(newSize)
                },
                onSizeChangeFinished = {
                    setHomeIconSizePercent(context, iconSizePercent.roundToInt())
                    setDrawerIconSizePercent(context, iconSizePercent.roundToInt())
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom: Columns slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            HomeColumnsSlider(
                currentSize = gridColumns,
                onSizeChange = { newSize ->
                    gridColumns = newSize
                    setHomeGridSize(context, newSize.roundToInt())
                },
                onSizeChangeFinished = {
                    setHomeGridSize(context, gridColumns.roundToInt())
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Rows slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            HomeRowsSlider(
                currentSize = gridRows,
                onSizeChange = { newSize ->
                    gridRows = newSize
                    setHomeGridRows(context, newSize.roundToInt())
                },
                onSizeChangeFinished = {
                    setHomeGridRows(context, gridRows.roundToInt())
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dock Columns slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            HomeDockColumnsSlider(
                currentSize = dockColumns,
                onSizeChange = { newSize ->
                    dockColumns = newSize
                    setDockColumns(context, newSize.roundToInt())
                },
                onSizeChangeFinished = {
                    setDockColumns(context, dockColumns.roundToInt())
                }
            )
        }
    }
}

/**
 * Vertical icon size slider for Home Screen (similar to App Drawer)
 * Now includes minor tick marks at every 2 increments and snaps to them
 */
@Composable
private fun HomeVerticalIconSizeSlider(
    currentSize: Float,
    sliderHeight: Dp,
    overflowThreshold: Float = 125f,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    // Always 50-125 range, with overflow threshold for visual warning
    val config = SliderConfigs.iconSizePercent
    ThumbDragVerticalSlider(
        currentValue = currentSize,
        sliderHeight = sliderHeight,
        config = config,
        onValueChange = onSizeChange,
        onValueChangeFinished = onSizeChangeFinished,
        overflowThreshold = overflowThreshold
    )
}

/**
 * Horizontal columns slider for Home Screen
 */
@Composable
private fun HomeColumnsSlider(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    ThumbDragHorizontalSlider(
        currentValue = currentSize,
        config = SliderConfigs.gridColumns,
        onValueChange = onSizeChange,
        onValueChangeFinished = onSizeChangeFinished
    )
}

/**
 * Horizontal rows slider for Home Screen
 */
@Composable
private fun HomeRowsSlider(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    ThumbDragHorizontalSlider(
        currentValue = currentSize,
        config = SliderConfigs.gridRows,
        onValueChange = onSizeChange,
        onValueChangeFinished = onSizeChangeFinished
    )
}

/**
 * Horizontal dock columns slider for Home Screen
 */
@Composable
private fun HomeDockColumnsSlider(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit
) {
    ThumbDragHorizontalSlider(
        currentValue = currentSize,
        config = SliderConfigs.dockColumns,
        onValueChange = onSizeChange,
        onValueChangeFinished = onSizeChangeFinished
    )
}

@Composable
private fun HomeScreenPreview(
    gridColumns: Int,
    gridRows: Int,
    dockColumns: Int = 5,
    iconSizePercent: Int = 100,
    iconTextSizePercent: Int = 100,
    homeScreenApps: List<HomeScreenAppData>,
    dockApps: List<HomeDockAppData>,
    allApps: List<PreviewAppInfo>,
    homeFolders: List<HomeFolderData> = emptyList(),
    placedWidgets: List<PlacedWidget> = emptyList(),
    widgetBitmaps: MutableMap<Int, android.graphics.Bitmap> = mutableMapOf(),
    labelFontFamily: FontFamily? = null,
    wallpaperDrawable: Drawable? = null,
    onPlayClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val scaleFactor = 0.4f
    val previewWidth = screenWidth * scaleFactor
    val previewHeight = screenHeight * scaleFactor

    // Match actual launcher proportional sizing system
    val screenWidthDpVal = configuration.screenWidthDp.toFloat()
    val screenHeightDpVal = configuration.screenHeightDp.toFloat()
    val iconSizeDp = (screenWidthDpVal / 4f * 0.55f * iconSizePercent / 100f)

    // Proportional padding matching actual launcher (LauncherScreen.kt)
    val gridHPaddingPreview = (screenWidthDpVal * 0.044f).dp * scaleFactor
    val gridVPaddingPreview = (screenWidthDpVal * 0.022f).dp * scaleFactor

    // Cell basis for proportional sizing (same formula as actual launcher)
    val gridCellWidth = (screenWidthDpVal - screenWidthDpVal * 0.044f * 2) / gridColumns
    val gridCellHeight = (screenHeightDpVal - 76f - screenWidthDpVal * 0.022f * 2) / gridRows
    val gridCellBasis = minOf(gridCellWidth, gridCellHeight)

    val statusBarHeight = 24.dp * scaleFactor
    val statusBarPadding = 8.dp * scaleFactor  // Match app drawer preview
    val navBarHeight = 20.dp * scaleFactor
    val tinyIconSize = 10.dp * scaleFactor
    val baseIconSize = iconSizeDp.dp * scaleFactor
    val dockIconSize = iconSizeDp.dp * scaleFactor

    // Proportional sizes matching actual launcher
    val markerHalfSize = (gridCellBasis * 0.073f).dp * scaleFactor
    val plusMarkerSize = (gridCellBasis * 0.146f).dp * scaleFactor
    val plusMarkerFont = (gridCellBasis * 0.122f).sp * scaleFactor
    val baseFontSize = 12.sp * scaleFactor * iconTextSizePercent / 100f
    val iconTextSpacer = 4.dp * scaleFactor

    // Build grid cells from home screen apps, folders, and widgets
    val totalCells = gridColumns * gridRows
    val gridCells = remember(homeScreenApps, allApps, homeFolders, placedWidgets, totalCells, gridColumns) {
        val cells = MutableList<HomePreviewCell?>(totalCells) { null }

        // Place widgets first (they span multiple cells)
        placedWidgets.forEach { widget ->
            val originPos = widget.startRow * gridColumns + widget.startColumn
            if (originPos in 0 until totalCells) {
                cells[originPos] = HomePreviewCell.WidgetOrigin(widget)
                // Mark spanned cells
                for (r in widget.startRow until (widget.startRow + widget.rowSpan).coerceAtMost(gridRows)) {
                    for (c in widget.startColumn until (widget.startColumn + widget.columnSpan).coerceAtMost(gridColumns)) {
                        val spanPos = r * gridColumns + c
                        if (spanPos != originPos && spanPos in 0 until totalCells) {
                            cells[spanPos] = HomePreviewCell.WidgetSpan
                        }
                    }
                }
            }
        }

        // Place folders (page 0 only for preview)
        homeFolders.filter { it.page == 0 }.forEach { folder ->
            if (folder.position < totalCells && cells[folder.position] == null) {
                val folderApps = folder.appPackageNames.filter { it.isNotEmpty() }.mapNotNull { pkgName ->
                    allApps.find { it.packageName == pkgName }
                }
                cells[folder.position] = HomePreviewCell.Folder(folder, folderApps)
            }
        }

        // Place apps (page 0 only for preview)
        homeScreenApps.forEach { homeApp ->
            if (homeApp.position < totalCells && cells[homeApp.position] == null) {
                allApps.find { it.packageName == homeApp.packageName }?.let { appInfo ->
                    cells[homeApp.position] = HomePreviewCell.App(appInfo)
                }
            }
        }
        cells.toList()
    }

    // Build dock cells from dockColumns parameter
    val dockCells = remember(dockApps, allApps, dockColumns) {
        val cells = MutableList<PreviewAppInfo?>(dockColumns) { null }
        dockApps.forEach { dockApp ->
            if (dockApp.position < dockColumns) {
                allApps.find { it.packageName == dockApp.packageName }?.let { appInfo ->
                    cells[dockApp.position] = appInfo
                }
            }
        }
        cells.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onPlayClick() }
        ) {
            // Wallpaper background
            if (wallpaperDrawable != null) {
                val painter = rememberDrawablePainter(drawable = wallpaperDrawable)
                Image(
                    painter = painter,
                    contentDescription = "Wallpaper",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback dark background (matches actual launcher)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .padding(horizontal = statusBarPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12:00",
                        fontSize = (8.sp * scaleFactor),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SignalCellular4Bar,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Filled.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(tinyIconSize),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // App grid area + dock - all in one weighted Column so dock row = grid row height
                // Page indicator dots setup (computed here so the fixed-height Box is inside the weighted Column)
                val previewDotContext = LocalContext.current
                val previewDotBaseColor = remember { getScrollbarColor(previewDotContext) }
                val previewDotIntensity = remember { getScrollbarIntensity(previewDotContext) }
                val previewDotWidthPct = remember { getScrollbarWidthPercent(previewDotContext) }
                val previewDotWidthDp = (LocalConfiguration.current.screenWidthDp * 0.02f * previewDotWidthPct / 100f).toInt()
                val previewDotColor = remember(previewDotBaseColor, previewDotIntensity) {
                    val base = Color(previewDotBaseColor)
                    val factor = (previewDotIntensity / 100f).coerceIn(0f, 1f)
                    Color(
                        red = base.red * factor,
                        green = base.green * factor,
                        blue = base.blue * factor,
                        alpha = base.alpha
                    )
                }
                val previewTriangleSize = (previewDotWidthDp + 2).dp * scaleFactor

                Box(
                    modifier = Modifier
                        .weight(gridRows.toFloat())
                        .fillMaxWidth()
                        .padding(horizontal = gridHPaddingPreview, vertical = gridVPaddingPreview)
                ) {
                    // Grid rows in Column so weight(1f) works on rows
                    Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until gridRows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            for (column in 0 until gridColumns) {
                                val index = row * gridColumns + column
                                val cell = gridCells.getOrNull(index)
                                val showBottomRightMarker = (row < gridRows - 1) && (column < gridColumns - 1)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (cell) {
                                        is HomePreviewCell.App -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(unbounded = true)
                                                    .graphicsLayer { clip = false }
                                                    .padding(markerHalfSize)
                                            ) {
                                                AsyncImage(
                                                    model = File(cell.app.iconPath),
                                                    contentDescription = cell.app.name,
                                                    modifier = Modifier
                                                        .requiredSize(baseIconSize)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Spacer(modifier = Modifier.height(iconTextSpacer))
                                                Text(
                                                    text = cell.app.name,
                                                    fontSize = baseFontSize,
                                                    fontFamily = labelFontFamily,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    style = LocalTextStyle.current.copy(
                                                        shadow = Shadow(
                                                            color = Color.Black,
                                                            offset = Offset(1f, 1f),
                                                            blurRadius = 2f
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                        is HomePreviewCell.Folder -> {
                                            // Folder: 2x2 mini-icon grid in a dark rounded box + name
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(unbounded = true)
                                                    .graphicsLayer { clip = false }
                                                    .padding(markerHalfSize)
                                            ) {
                                                val folderBoxSize = baseIconSize
                                                val folderCornerRadius = baseIconSize * 0.29f
                                                Box(
                                                    modifier = Modifier
                                                        .requiredSize(folderBoxSize)
                                                        .clip(RoundedCornerShape(folderCornerRadius))
                                                        .background(Color(0xFF1A1A1A)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (cell.previewApps.isNotEmpty()) {
                                                        val fPad = folderBoxSize * 0.08f
                                                        val fSpacing = folderBoxSize * 0.04f
                                                        val miniIcon = (folderBoxSize - fPad * 2 - fSpacing) / 2
                                                        Column(
                                                            modifier = Modifier.padding(fPad),
                                                            verticalArrangement = Arrangement.spacedBy(fSpacing)
                                                        ) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(fSpacing)) {
                                                                cell.previewApps.getOrNull(0)?.let { a ->
                                                                    AsyncImage(
                                                                        model = File(a.iconPath),
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Fit,
                                                                        modifier = Modifier
                                                                            .size(miniIcon)
                                                                            .clip(RoundedCornerShape(miniIcon * 0.2f))
                                                                    )
                                                                } ?: Spacer(Modifier.size(miniIcon))
                                                                cell.previewApps.getOrNull(1)?.let { a ->
                                                                    AsyncImage(
                                                                        model = File(a.iconPath),
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Fit,
                                                                        modifier = Modifier
                                                                            .size(miniIcon)
                                                                            .clip(RoundedCornerShape(miniIcon * 0.2f))
                                                                    )
                                                                } ?: Spacer(Modifier.size(miniIcon))
                                                            }
                                                            Row(horizontalArrangement = Arrangement.spacedBy(fSpacing)) {
                                                                cell.previewApps.getOrNull(2)?.let { a ->
                                                                    AsyncImage(
                                                                        model = File(a.iconPath),
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Fit,
                                                                        modifier = Modifier
                                                                            .size(miniIcon)
                                                                            .clip(RoundedCornerShape(miniIcon * 0.2f))
                                                                    )
                                                                } ?: Spacer(Modifier.size(miniIcon))
                                                                cell.previewApps.getOrNull(3)?.let { a ->
                                                                    AsyncImage(
                                                                        model = File(a.iconPath),
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Fit,
                                                                        modifier = Modifier
                                                                            .size(miniIcon)
                                                                            .clip(RoundedCornerShape(miniIcon * 0.2f))
                                                                    )
                                                                } ?: Spacer(Modifier.size(miniIcon))
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(iconTextSpacer))
                                                Text(
                                                    text = cell.folder.name,
                                                    fontSize = baseFontSize,
                                                    fontFamily = labelFontFamily,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    style = LocalTextStyle.current.copy(
                                                        shadow = Shadow(
                                                            color = Color.Black,
                                                            offset = Offset(1f, 1f),
                                                            blurRadius = 2f
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                        is HomePreviewCell.WidgetOrigin,
                                        is HomePreviewCell.WidgetSpan -> {
                                            // Empty - bitmap overlay handles widget rendering
                                        }
                                        null -> { /* empty cell */ }
                                    }

                                    // "+" marker at cell intersection
                                    if (showBottomRightMarker) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = markerHalfSize, y = markerHalfSize)
                                                .size(plusMarkerSize),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+",
                                                fontSize = plusMarkerFont,
                                                color = Color.White.copy(alpha = 0.5f),
                                                style = LocalTextStyle.current.copy(
                                                    shadow = Shadow(
                                                        color = Color.Black,
                                                        offset = Offset(0.5f, 0.5f),
                                                        blurRadius = 1f
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } // end inner grid Column

                    // Widget overlay - render widgets as bitmap snapshots on top of grid
                    // Creates the widget view at full screen size, draws it to a Bitmap,
                    // then displays the Bitmap as a scaled Image. This avoids the layout
                    // overflow issues of AndroidView + graphicsLayer.
                    if (placedWidgets.isNotEmpty()) {
                        // Pre-compute widget bitmaps using screen config (works even when preview is hidden)
                        val widgetConfig = LocalConfiguration.current
                        val widgetScreenWidthDp = widgetConfig.screenWidthDp.toFloat()
                        val widgetScreenHeightDp = widgetConfig.screenHeightDp.toFloat()
                        val widgetGridHPad = widgetScreenWidthDp * 0.044f
                        val widgetGridVPad = widgetScreenWidthDp * 0.022f
                        val widgetGridWidthDp = widgetScreenWidthDp - widgetGridHPad * 2
                        val widgetGridHeightDp = widgetScreenHeightDp - 76f - widgetGridVPad * 2
                        val widgetCellWDp = widgetGridWidthDp / gridColumns
                        val widgetCellHDp = widgetGridHeightDp / gridRows

                        // Use bitmaps from the parent (persists across icon size changes)
                        val widgetContext = LocalContext.current

                        placedWidgets.forEach { widget ->
                            LaunchedEffect(widget.appWidgetId) {
                                if (widgetBitmaps.containsKey(widget.appWidgetId)) return@LaunchedEffect
                                try {
                                    val host = WidgetManager.getAppWidgetHost()
                                    val mgr = android.appwidget.AppWidgetManager.getInstance(widgetContext)
                                    val info = mgr.getAppWidgetInfo(widget.appWidgetId)
                                    if (host != null && info != null) {
                                        val widgetView = host.createView(widgetContext, widget.appWidgetId, info)
                                        delay(500)
                                        val metrics = widgetContext.resources.displayMetrics
                                        // Compute full-screen pixel dimensions from screen config
                                        val fullWidthPx = (widgetCellWDp * widget.columnSpan * metrics.density).toInt()
                                        val fullHeightPx = (widgetCellHDp * widget.rowSpan * metrics.density).toInt()
                                        if (fullWidthPx > 0 && fullHeightPx > 0) {
                                            widgetView.measure(
                                                android.view.View.MeasureSpec.makeMeasureSpec(fullWidthPx, android.view.View.MeasureSpec.EXACTLY),
                                                android.view.View.MeasureSpec.makeMeasureSpec(fullHeightPx, android.view.View.MeasureSpec.EXACTLY)
                                            )
                                            widgetView.layout(0, 0, fullWidthPx, fullHeightPx)
                                            val bmp = Bitmap.createBitmap(fullWidthPx, fullHeightPx, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bmp)
                                            widgetView.draw(canvas)
                                            widgetBitmaps[widget.appWidgetId] = bmp
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        // Render bitmaps at correct positions using BoxWithConstraints
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cellW = maxWidth / gridColumns
                            val cellH = maxHeight / gridRows

                            placedWidgets.forEach { widget ->
                                key(widget.appWidgetId) {
                                    val x = cellW * widget.startColumn
                                    val y = cellH * widget.startRow
                                    val w = cellW * widget.columnSpan
                                    val h = cellH * widget.rowSpan

                                    Box(
                                        modifier = Modifier
                                            .offset(x = x, y = y)
                                            .width(w)
                                            .height(h)
                                            .clip(RoundedCornerShape(4.dp * scaleFactor))
                                    ) {
                                        val bmp = widgetBitmaps[widget.appWidgetId]
                                        if (bmp != null && !bmp.isRecycled) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Widget",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.FillBounds
                                            )
                                        } else {
                                            // Placeholder while bitmap loads
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp * scaleFactor)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Widget",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = baseFontSize
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // end grid area Box

                    // Page indicator dots (like the actual launcher, using scrollbar personalization)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((previewDotWidthDp.dp + 8.dp) * scaleFactor),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home page: rounded triangle
                            Canvas(modifier = Modifier.size(previewTriangleSize)) {
                                val w = size.width
                                val h = size.height
                                val top = Offset(w / 2f, 0f)
                                val bl = Offset(0f, h)
                                val br = Offset(w, h)
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
                                drawPath(path, previewDotColor.copy(alpha = 0.9f))
                            }
                        }
                    }

                    // Dock row - same weight(1f) as grid rows so dock icons match grid icon size
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = gridHPaddingPreview),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockCells.forEach { app ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app != null) {
                                    AsyncImage(
                                        model = File(app.iconPath),
                                        contentDescription = app.name,
                                        modifier = Modifier
                                            .requiredSize(dockIconSize)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    // Navigation bar (same style as App Drawer preview)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(navBarHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(previewWidth * 0.3f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                    }
                } // end inner content Column

            // Play button overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open Home Screen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } // end Preview Box
    }
}
