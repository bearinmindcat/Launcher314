package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bearinmind.launcher314.data.getWidgetRoundedCornersEnabled
import com.bearinmind.launcher314.data.setWidgetRoundedCornersEnabled
import com.bearinmind.launcher314.data.getWidgetCornerRadiusPercent
import com.bearinmind.launcher314.data.setWidgetCornerRadiusPercent
import com.bearinmind.launcher314.data.WIDGET_MAX_CORNER_RADIUS_DP
import androidx.compose.ui.graphics.graphicsLayer
import com.bearinmind.launcher314.ui.components.AnimatedPopup
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import com.bearinmind.launcher314.ui.components.SliderConfigs

/**
 * Data class representing a widget for display
 */
data class WidgetInfo(
    val providerInfo: AppWidgetProviderInfo,
    val appName: String,
    val label: String,
    val appIcon: Bitmap?,
    val previewImage: Bitmap?,
    val cellWidth: Int,
    val cellHeight: Int
)

/**
 * Data class for grouped widgets by app
 */
data class AppWidgetGroup(
    val appName: String,
    val appIcon: Bitmap?,
    val packageName: String,
    val widgets: List<WidgetInfo>
)

/**
 * WidgetsScreen - Shows available widgets grouped by app with previews
 * Matches AppDrawerScreen styling with search bar and collapsible sections
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WidgetsScreen(
    onBack: () -> Unit = {},
    onWidgetSelected: (WidgetInfo) -> Unit = {},
    gridColumns: Int = 4,
    gridRows: Int = 5,
    getOccupiedCells: () -> Set<Int> = { emptySet() }
) {
    val context = LocalContext.current
    var appGroups by remember { mutableStateOf<List<AppWidgetGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Settings popup state
    var showMenu by remember { mutableStateOf(false) }
    var widgetRoundedCornersEnabled by remember { mutableStateOf(getWidgetRoundedCornersEnabled(context)) }
    var widgetCornerRadius by remember { mutableFloatStateOf(getWidgetCornerRadiusPercent(context).toFloat()) }

    // Track expanded state for each app group
    var expandedApps by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Widget confirmation dialog state
    var selectedWidget by remember { mutableStateOf<WidgetInfo?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var noSpaceError by remember { mutableStateOf(false) }

    // Load available widgets
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            appGroups = loadWidgetsGroupedByApp(context)
            // Start with all apps collapsed - user can expand as needed
            expandedApps = emptySet()
            isLoading = false
        }
    }

    // Filter app groups based on search query
    val filteredAppGroups by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                appGroups
            } else {
                appGroups.filter { group ->
                    group.appName.contains(searchQuery, ignoreCase = true) ||
                    group.widgets.any { it.label.contains(searchQuery, ignoreCase = true) }
                }
            }
        }
    }

    // Helper function to find first available position for a widget
    fun findAvailablePosition(widgetCols: Int, widgetRows: Int): Pair<Int, Int>? {
        val occupiedCells = getOccupiedCells()
        val totalCells = gridColumns * gridRows

        // Try each possible starting position
        for (startRow in 0 until gridRows) {
            for (startCol in 0 until gridColumns) {
                // Check if widget fits at this position
                if (startCol + widgetCols > gridColumns) continue
                if (startRow + widgetRows > gridRows) continue

                // Check if all cells for this widget are available
                var allCellsAvailable = true
                for (row in startRow until startRow + widgetRows) {
                    for (col in startCol until startCol + widgetCols) {
                        val cellIndex = row * gridColumns + col
                        if (occupiedCells.contains(cellIndex)) {
                            allCellsAvailable = false
                            break
                        }
                    }
                    if (!allCellsAvailable) break
                }

                if (allCellsAvailable) {
                    return Pair(startCol, startRow)
                }
            }
        }
        return null // No available position found
    }

    // Handle widget long-press - show confirmation dialog
    fun handleWidgetLongPress(widget: WidgetInfo) {
        selectedWidget = widget
        // Check if there's space available
        val availablePos = findAvailablePosition(widget.cellWidth, widget.cellHeight)
        if (availablePos != null) {
            noSpaceError = false
            showAddDialog = true
        } else {
            noSpaceError = true
            showAddDialog = true
        }
    }

    // Solid dark background for widgets screen
    val widgetsBackground = Color(0xFF121212)

    // Add widget confirmation dialog
    if (showAddDialog && selectedWidget != null) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                selectedWidget = null
                noSpaceError = false
            },
            title = {
                Text(
                    text = if (noSpaceError) "Not Enough Space" else "Add Widget",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (noSpaceError) {
                    Text(
                        text = "There isn't enough space on the home screen for this widget (${selectedWidget!!.cellWidth} × ${selectedWidget!!.cellHeight}). Remove some apps or widgets to make room."
                    )
                } else {
                    Column {
                        Text(
                            text = "Add \"${selectedWidget!!.label}\" to your home screen?",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Size: ${selectedWidget!!.cellWidth} × ${selectedWidget!!.cellHeight} cells",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (!noSpaceError) {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            selectedWidget?.let { widget ->
                                onWidgetSelected(widget)
                            }
                            selectedWidget = null
                        }
                    ) {
                        Text("Add to Home")
                    }
                } else {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            selectedWidget = null
                            noSpaceError = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (!noSpaceError) {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            selectedWidget = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(widgetsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search bar (matching app drawer style)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search Widgets", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                        // 3-dot menu button
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }
                            AnimatedPopup(
                                visible = showMenu,
                                onDismissRequest = { showMenu = false },
                                gapDp = 4
                            ) {
                                // Rounded corners toggle
                                DropdownMenuItem(
                                    text = { Text("Rounded corners") },
                                    onClick = {
                                        widgetRoundedCornersEnabled = !widgetRoundedCornersEnabled
                                        setWidgetRoundedCornersEnabled(context, widgetRoundedCornersEnabled)
                                        WidgetManager.refreshAllWidgetCorners(context)
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = widgetRoundedCornersEnabled,
                                            onCheckedChange = {
                                                widgetRoundedCornersEnabled = it
                                                setWidgetRoundedCornersEnabled(context, it)
                                                WidgetManager.refreshAllWidgetCorners(context)
                                            },
                                            modifier = Modifier
                                                .height(20.dp)
                                                .graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
                                        )
                                    }
                                )

                                // Corner radius slider (only when enabled)
                                if (widgetRoundedCornersEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .padding(bottom = 4.dp)
                                    ) {
                                        ThumbDragHorizontalSlider(
                                            currentValue = widgetCornerRadius,
                                            config = SliderConfigs.cornerRoundness,
                                            onValueChange = { newVal ->
                                                widgetCornerRadius = newVal
                                                setWidgetCornerRadiusPercent(context, newVal.roundToInt())
                                            },
                                            onValueChangeFinished = {
                                                WidgetManager.refreshAllWidgetCorners(context)
                                            }
                                        )
                                    }
                                }
                            }
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
            } else if (filteredAppGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No widgets found" else "No widgets available",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Install apps with widgets to see them here",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val density = LocalDensity.current
                val cornerRadiusPx = if (widgetRoundedCornersEnabled) {
                    with(density) { (widgetCornerRadius / 100f * WIDGET_MAX_CORNER_RADIUS_DP).dp.toPx() }
                } else 0f

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    items(filteredAppGroups) { appGroup ->
                        WidgetAppSection(
                            appGroup = appGroup,
                            isExpanded = expandedApps.contains(appGroup.packageName),
                            cornerRadiusPx = cornerRadiusPx,
                            onToggleExpand = {
                                expandedApps = if (expandedApps.contains(appGroup.packageName)) {
                                    expandedApps - appGroup.packageName
                                } else {
                                    expandedApps + appGroup.packageName
                                }
                            },
                            onWidgetSelected = { widget -> handleWidgetLongPress(widget) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collapsible section for an app's widgets (matching settings dropdown style)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetAppSection(
    appGroup: AppWidgetGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onWidgetSelected: (WidgetInfo) -> Unit = {},
    cornerRadiusPx: Float = 0f
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) -90f else 0f,
        label = "triangle_rotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // App header row (clickable to expand/collapse) - matching CollapsibleSection style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White),
                    onClick = onToggleExpand
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App icon (no background)
            appGroup.appIcon?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appGroup.appName,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // App name
            Text(
                text = appGroup.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.87f),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Triangle indicator (matching CollapsibleSection)
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .rotate(rotationAngle)
            ) {
                val path = Path().apply {
                    // Triangle pointing down (will rotate to point right when expanded)
                    moveTo(size.width / 2, size.height * 0.8f)
                    lineTo(size.width * 0.15f, size.height * 0.2f)
                    lineTo(size.width * 0.85f, size.height * 0.2f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.Gray,
                    style = Fill
                )
            }
        }

        // Widgets grid (shown when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appGroup.widgets) { widget ->
                    WidgetPreviewCard(
                        widget = widget,
                        cornerRadiusPx = cornerRadiusPx,
                        onLongPress = { onWidgetSelected(widget) }
                    )
                }
            }
        }

        // Divider between apps (extends edge-to-edge)
        Divider(
            color = Color.White.copy(alpha = 0.1f)
        )
    }
}

/**
 * Widget preview card showing the widget preview and cell size
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetPreviewCard(
    widget: WidgetInfo,
    cornerRadiusPx: Float = 0f,
    onLongPress: () -> Unit = {}
) {
    val cardWidth = 140.dp
    val previewHeight = 100.dp
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Pre-round the preview bitmap so corners are clipped without morphing
    val displayPreview = remember(widget.previewImage, cornerRadiusPx) {
        val src = widget.previewImage ?: return@remember null
        if (cornerRadiusPx > 0f) roundBitmapCorners(src, cornerRadiusPx) else src
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(
                onClick = { /* Tap does nothing for now */ },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Widget preview image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight),
            contentAlignment = Alignment.Center
        ) {
            if (displayPreview != null) {
                Image(
                    bitmap = displayPreview.asImageBitmap(),
                    contentDescription = widget.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (widget.appIcon != null) {
                // Fallback to app icon if no preview
                Image(
                    bitmap = widget.appIcon.asImageBitmap(),
                    contentDescription = widget.label,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Widget name
        Text(
            text = widget.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.87f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Cell size label (e.g., "4 x 2")
        Text(
            text = "${widget.cellWidth} x ${widget.cellHeight}",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Load all available widgets grouped by app
 *
 * Widget cell size calculation based on Fossify Launcher:
 * - On Android S+ (API 31+), use targetCellWidth/targetCellHeight if available
 * - Otherwise, calculate from minWidth/minHeight using formula: ceil((dpValue - 30) / 70)
 * - minWidth/minHeight are ALWAYS in pixels on all Android versions
 */
private fun loadWidgetsGroupedByApp(context: Context): List<AppWidgetGroup> {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val packageManager = context.packageManager
    val density = context.resources.displayMetrics.density
    val densityDpi = context.resources.displayMetrics.densityDpi

    val widgetsByPackage = mutableMapOf<String, MutableList<WidgetInfo>>()
    val appIcons = mutableMapOf<String, Bitmap?>()
    val appNames = mutableMapOf<String, String>()

    appWidgetManager.installedProviders.forEach { providerInfo ->
        try {
            val packageName = providerInfo.provider.packageName
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val appName = packageManager.getApplicationLabel(appInfo).toString()

            // Get app icon (cache per package)
            if (!appIcons.containsKey(packageName)) {
                appIcons[packageName] = try {
                    val drawable = packageManager.getApplicationIcon(packageName)
                    drawableToBitmap(drawable)
                } catch (e: Exception) {
                    null
                }
                appNames[packageName] = appName
            }

            // Get widget preview image using correct densityDpi (320, 480, etc.)
            val previewImage = try {
                val previewDrawable = providerInfo.loadPreviewImage(context, densityDpi)
                    ?: run {
                        // Fallback to icon if no preview available
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            providerInfo.loadIcon(context, densityDpi)
                        } else {
                            null
                        }
                    }
                previewDrawable?.let { drawableToBitmap(it) }
            } catch (e: Exception) {
                null
            }

            // Calculate cell size using Fossify's approach:
            // 1. On Android S+ (API 31), prefer targetCellWidth/targetCellHeight if non-zero
            // 2. Otherwise calculate from minWidth/minHeight (always in pixels)
            val (cellWidth, cellHeight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val targetWidth = providerInfo.targetCellWidth
                val targetHeight = providerInfo.targetCellHeight
                if (targetWidth > 0 && targetHeight > 0) {
                    // Use target cell dimensions directly (most accurate)
                    Pair(targetWidth, targetHeight)
                } else {
                    // Calculate from pixel dimensions
                    Pair(
                        calculateCellCount(providerInfo.minWidth, density),
                        calculateCellCount(providerInfo.minHeight, density)
                    )
                }
            } else {
                // Pre-Android S: calculate from pixel dimensions
                Pair(
                    calculateCellCount(providerInfo.minWidth, density),
                    calculateCellCount(providerInfo.minHeight, density)
                )
            }

            val widgetLabel = providerInfo.loadLabel(packageManager)

            val widgetInfo = WidgetInfo(
                providerInfo = providerInfo,
                appName = appName,
                label = widgetLabel,
                appIcon = appIcons[packageName],
                previewImage = previewImage,
                cellWidth = cellWidth,
                cellHeight = cellHeight
            )

            widgetsByPackage.getOrPut(packageName) { mutableListOf() }.add(widgetInfo)

        } catch (e: Exception) {
            // Skip widgets that fail to load
        }
    }

    // Convert to AppWidgetGroup list, sorted by app name
    return widgetsByPackage.map { (packageName, widgets) ->
        AppWidgetGroup(
            appName = appNames[packageName] ?: packageName,
            appIcon = appIcons[packageName],
            packageName = packageName,
            widgets = widgets.sortedBy { it.label }
        )
    }.sortedBy { it.appName }
}

/**
 * Calculate number of cells needed for a widget dimension.
 * Based on Fossify Launcher formula: ceil((dpValue - 30) / 70)
 * where 30dp is padding and 70dp is standard cell size.
 *
 * @param sizeInPixels The widget dimension in pixels
 * @param density The display density for dp conversion
 * @return Number of cells needed (minimum 1)
 */
private fun calculateCellCount(sizeInPixels: Int, density: Float): Int {
    val sizeInDp = sizeInPixels / density
    val cells = kotlin.math.ceil((sizeInDp - 30) / 70.0).toInt()
    return maxOf(cells, 1)
}

/**
 * Returns a new bitmap with rounded corners applied via PorterDuff masking.
 * The source bitmap is drawn at its original aspect ratio — no stretching.
 */
private fun roundBitmapCorners(source: Bitmap, radiusPx: Float): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())

    // Draw rounded rect as mask
    canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

    // Draw source bitmap using SRC_IN so only the masked area shows
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(source, 0f, 0f, paint)

    return output
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
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