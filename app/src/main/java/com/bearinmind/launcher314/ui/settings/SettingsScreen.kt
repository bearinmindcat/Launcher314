package com.bearinmind.launcher314.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.setScrollbarWidthPercent
import com.bearinmind.launcher314.data.getScrollbarHeightPercent
import com.bearinmind.launcher314.data.setScrollbarHeightPercent
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.setScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarIntensity
import com.bearinmind.launcher314.data.setScrollbarIntensity
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.CollapsibleSection
import com.bearinmind.launcher314.data.getIconTextSizePercent
import com.bearinmind.launcher314.data.setIconTextSizePercent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.border
import kotlin.math.roundToInt
import com.bearinmind.launcher314.services.AppDrawerAccessibilityService
import com.bearinmind.launcher314.services.AppDrawerTileService
import com.bearinmind.launcher314.data.LauncherUtils
import com.bearinmind.launcher314.services.NotificationDrawerAction
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.bearinmind.launcher314.helpers.FontManager
import com.bearinmind.launcher314.helpers.IconPackManager
import com.bearinmind.launcher314.data.getSelectedFont
import com.bearinmind.launcher314.data.getSettingsSelectedTab
import com.bearinmind.launcher314.data.setSettingsSelectedTab
import com.bearinmind.launcher314.data.getDrawerIconSizePercent
import com.bearinmind.launcher314.data.getGlobalIconShape
import com.bearinmind.launcher314.data.getGlobalIconBgColor
import com.bearinmind.launcher314.data.setGlobalIconBgColor
import com.bearinmind.launcher314.data.getDoubleTapLockEnabled
import com.bearinmind.launcher314.data.setDoubleTapLockEnabled
import com.bearinmind.launcher314.data.getReverseDrawerSearchBar
import com.bearinmind.launcher314.data.getAutoOpenKeyboard
import com.bearinmind.launcher314.data.setAutoOpenKeyboard
import com.bearinmind.launcher314.data.setReverseDrawerSearchBar
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.generateShapedIconBitmap
import com.bearinmind.launcher314.data.getGlobalIconBgIntensity
import com.bearinmind.launcher314.data.setGlobalIconBgIntensity
import com.bearinmind.launcher314.data.setGlobalIconShape
import com.bearinmind.launcher314.helpers.clearGlobalShapedIcons
import com.bearinmind.launcher314.helpers.clearBgColorShapedIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Clear
import com.bearinmind.launcher314.helpers.IconShapes
import com.bearinmind.launcher314.helpers.getIconShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onClearData: () -> Unit = {},
    onExportData: (String) -> Unit = {},
    onImportData: (String) -> Boolean = { false },
    onPreviewDrawer: () -> Unit = {},
    onPreviewLauncher: () -> Unit = {},
    onGridSizeChanged: (Int) -> Unit = {},
    onFontsClick: () -> Unit = {},
    onIconPacksClick: () -> Unit = {},
    onHideAppsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Tab selection state: 0 = App Drawer, 1 = Home Screen (persisted)
    var selectedTab by remember { mutableIntStateOf(getSettingsSelectedTab(context)) }

    // Scrollbar settings state (percentage-based for proportional scaling)
    var scrollbarWidth by remember { mutableFloatStateOf(getScrollbarWidthPercent(context).toFloat()) }
    var scrollbarHeight by remember { mutableFloatStateOf(getScrollbarHeightPercent(context).toFloat()) }
    var scrollbarColor by remember { mutableIntStateOf(getScrollbarColor(context)) }
    var scrollbarIntensity by remember { mutableFloatStateOf(getScrollbarIntensity(context).toFloat()) }

    // Collapsible section states
    var launcherPreviewExpanded by remember { mutableStateOf(true) }
    var iconTextSectionExpanded by remember { mutableStateOf(true) }
    var scrollbarSectionExpanded by remember { mutableStateOf(true) }
    var visibilitySettingsExpanded by remember { mutableStateOf(true) }

    // Icon text size state (shared between home screen and app drawer)
    var iconTextSizePercent by remember { mutableFloatStateOf(getIconTextSizePercent(context).toFloat()) }

    // Icon size state (shared between home screen and app drawer previews)
    var sharedIconSizePercent by remember { mutableFloatStateOf(getDrawerIconSizePercent(context).toFloat()) }
    var globalIconShape by remember { mutableStateOf(getGlobalIconShape(context)) }
    var globalIconBgColor by remember { mutableStateOf(getGlobalIconBgColor(context)) }
    var globalIconBgIntensity by remember { mutableStateOf(getGlobalIconBgIntensity(context)) }

    // Accessibility service state - reflects actual system state
    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(AppDrawerAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    // Quick Settings Tile state
    var quickSettingsTileEnabled by remember { mutableStateOf(AppDrawerTileService.isEnabled(context)) }

    // Notification Action state
    var notificationEnabled by remember { mutableStateOf(NotificationDrawerAction.isEnabled(context)) }

    // Launcher state - reflects actual system state (is app the default launcher?)
    var isDefaultLauncher by remember { mutableStateOf(LauncherUtils.isDefaultLauncher(context)) }

    // Get lifecycle owner for observing resume events
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh states when returning to this screen (checks actual system state)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check actual system state for accessibility service
                isAccessibilityServiceEnabled = AppDrawerAccessibilityService.isAccessibilityServiceEnabled(context)

                // Check actual system state for default launcher
                isDefaultLauncher = LauncherUtils.isDefaultLauncher(context)

                // Sync internal settings with actual system state
                AppDrawerAccessibilityService.setEnabled(context, isAccessibilityServiceEnabled)
                LauncherUtils.setEnabled(context, isDefaultLauncher)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // App Drawer / Home Screen Settings Section
            CollapsibleSection(
                title = "Launcher Preview & Customization",
                expanded = launcherPreviewExpanded,
                onToggle = { launcherPreviewExpanded = !launcherPreviewExpanded }
            ) {
                // Tab buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // App Drawer tab button
                    Button(
                        onClick = { selectedTab = 0; setSettingsSelectedTab(context, 0) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTab == 0)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("App Drawer")
                    }

                    // Home Screen tab button
                    Button(
                        onClick = { selectedTab = 1; setSettingsSelectedTab(context, 1) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTab == 1)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Home Screen")
                    }
                }

                // Both previews always composed (pre-rendered) — only the active one is visible
                // App Drawer preview
                Box(
                    modifier = if (selectedTab == 0) Modifier
                    else Modifier.requiredHeight(0.dp).clipToBounds()
                ) {
                    AppDrawerPreviewSection(
                        onPreviewDrawer = onPreviewDrawer,
                        onGridSizeChanged = onGridSizeChanged,
                        scrollbarWidthOverride = scrollbarWidth.roundToInt(),
                        scrollbarHeightOverride = scrollbarHeight.roundToInt(),
                        scrollbarColorOverride = scrollbarColor,
                        scrollbarIntensityOverride = scrollbarIntensity.roundToInt(),
                        iconTextSizeOverride = iconTextSizePercent.roundToInt(),
                        sharedIconSize = sharedIconSizePercent,
                        onSharedIconSizeChanged = { sharedIconSizePercent = it },
                        iconShapeOverride = globalIconShape,
                        iconBgColorOverride = globalIconBgColor,
                        iconBgIntensityOverride = globalIconBgIntensity
                    )
                }

                // Home Screen preview
                Box(
                    modifier = if (selectedTab == 1) Modifier
                    else Modifier.requiredHeight(0.dp).clipToBounds()
                ) {
                    HomeScreenPreviewSection(
                        onPreviewLauncher = {
                            onPreviewLauncher()
                        },
                        iconTextSizeOverride = iconTextSizePercent.roundToInt(),
                        sharedIconSize = sharedIconSizePercent,
                        onSharedIconSizeChanged = { sharedIconSizePercent = it },
                        iconShapeOverride = globalIconShape,
                        iconBgColorOverride = globalIconBgColor,
                        iconBgIntensityOverride = globalIconBgIntensity
                    )
                }

                // Shared settings sections (same for both tabs)
                Divider(color = Color.Gray.copy(alpha = 0.2f))

                CollapsibleSection(
                    title = "Icon Personalization",
                    expanded = iconTextSectionExpanded,
                    onToggle = { iconTextSectionExpanded = !iconTextSectionExpanded }
                ) {
                    IconTextPersonalizationCard(
                        textSizePercent = iconTextSizePercent,
                        onTextSizeChange = { iconTextSizePercent = it },
                        onFontsClick = onFontsClick,
                        onIconPacksClick = onIconPacksClick,
                        globalIconShape = globalIconShape,
                        onGlobalIconShapeChanged = { shape ->
                            globalIconShape = shape
                            setGlobalIconShape(context, shape)
                            clearGlobalShapedIcons(context)
                            clearBgColorShapedIcons(context)
                        },
                        globalIconBgColor = globalIconBgColor,
                        onGlobalIconBgColorChanged = { color ->
                            globalIconBgColor = color
                            setGlobalIconBgColor(context, color)
                            clearBgColorShapedIcons(context)
                        },
                        globalIconBgIntensity = globalIconBgIntensity,
                        onGlobalIconBgIntensityChanged = { intensity ->
                            globalIconBgIntensity = intensity
                            setGlobalIconBgIntensity(context, intensity)
                            clearBgColorShapedIcons(context)
                        }
                    )
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                CollapsibleSection(
                    title = "Scroll Bar / Navigation",
                    expanded = scrollbarSectionExpanded,
                    onToggle = { scrollbarSectionExpanded = !scrollbarSectionExpanded }
                ) {
                    ScrollbarPersonalizationCard(
                        scrollbarWidth = scrollbarWidth,
                        scrollbarHeight = scrollbarHeight,
                        scrollbarColor = scrollbarColor,
                        scrollbarIntensity = scrollbarIntensity,
                        onWidthChange = { scrollbarWidth = it },
                        onHeightChange = { scrollbarHeight = it },
                        onColorChange = { scrollbarColor = it },
                        onIntensityChange = { scrollbarIntensity = it }
                    )
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                var wallpaperSectionExpanded by remember { mutableStateOf(false) }
                CollapsibleSection(
                    title = "Wallpaper",
                    expanded = wallpaperSectionExpanded,
                    onToggle = { wallpaperSectionExpanded = !wallpaperSectionExpanded }
                ) {
                    WallpaperPersonalizationCard()
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // Visibility Settings Section - always open (no collapsible triangle for now)
            SettingsSection(title = "Visibility Settings") {
                // // Accessibility Shortcut - toggle opens Android accessibility settings
                // SettingsToggleItem(
                //     title = "Accessibility Shortcut",
                //     subtitle = if (isAccessibilityServiceEnabled)
                //         "Tap accessibility button to open drawer"
                //     else
                //         "Tap to enable in Android settings",
                //     checked = isAccessibilityServiceEnabled,
                //     onCheckedChange = {
                //         AppDrawerAccessibilityService.openAccessibilitySettings(context)
                //     }
                // )

                // // Quick Settings Tile toggle
                // SettingsToggleItem(
                //     title = "Quick Settings Tile",
                //     subtitle = if (quickSettingsTileEnabled)
                //         "Add tile from Quick Settings edit menu"
                //     else
                //         "Show tile in Quick Settings panel",
                //     checked = quickSettingsTileEnabled,
                //     onCheckedChange = { checked ->
                //         quickSettingsTileEnabled = checked
                //         AppDrawerTileService.setEnabled(context, checked)
                //     }
                // )

                // // Notification Shortcut toggle
                // SettingsToggleItem(
                //     title = "Notification Shortcut",
                //     subtitle = if (notificationEnabled)
                //         "Persistent notification in shade"
                //     else
                //         "Show notification to open drawer",
                //     checked = notificationEnabled,
                //     onCheckedChange = { checked ->
                //         notificationEnabled = checked
                //         NotificationDrawerAction.setEnabled(context, checked)
                //     }
                // )

                // Enable Launcher toggle - reflects actual system state
                SettingsToggleItem(
                    title = "Enable Launcher",
                    subtitle = if (isDefaultLauncher)
                        "Swipe up on home screen for apps"
                    else
                        "Tap to set as default launcher",
                    checked = isDefaultLauncher,
                    onCheckedChange = {
                        LauncherUtils.openDefaultLauncherSettings(context)
                    }
                )

                // Reverse drawer search bar toggle
                var reverseSearchBar by remember { mutableStateOf(getReverseDrawerSearchBar(context)) }
                SettingsToggleItem(
                    title = "Reverse drawer search bar",
                    subtitle = "Moves drawer search bar to the bottom",
                    checked = reverseSearchBar,
                    onCheckedChange = {
                        reverseSearchBar = !reverseSearchBar
                        setReverseDrawerSearchBar(context, reverseSearchBar)
                    }
                )

                // Auto open keyboard toggle
                var autoOpenKeyboard by remember { mutableStateOf(getAutoOpenKeyboard(context)) }
                SettingsToggleItem(
                    title = "Auto open keyboard",
                    subtitle = "Automatically opens keyboard in app drawer",
                    checked = autoOpenKeyboard,
                    onCheckedChange = {
                        autoOpenKeyboard = !autoOpenKeyboard
                        setAutoOpenKeyboard(context, autoOpenKeyboard)
                    }
                )

                // Double-tap to lock screen toggle
                var doubleTapLockEnabled by remember { mutableStateOf(getDoubleTapLockEnabled(context)) }
                var isServiceEnabled by remember { mutableStateOf(AppDrawerAccessibilityService.isAccessibilityServiceEnabled(context)) }

                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(1000)
                        val newState = AppDrawerAccessibilityService.isAccessibilityServiceEnabled(context)
                        if (newState != isServiceEnabled) {
                            isServiceEnabled = newState
                            if (newState) {
                                doubleTapLockEnabled = true
                                setDoubleTapLockEnabled(context, true)
                            } else {
                                doubleTapLockEnabled = false
                                setDoubleTapLockEnabled(context, false)
                            }
                        }
                    }
                }

                SettingsToggleItem(
                    title = "Double-tap to lock screen",
                    subtitle = if (doubleTapLockEnabled && isServiceEnabled)
                        "Double-tap anywhere on home screen to lock"
                    else
                        "Enable feature in accessibility settings",
                    checked = doubleTapLockEnabled && isServiceEnabled,
                    onCheckedChange = {
                        if (!isServiceEnabled) {
                            AppDrawerAccessibilityService.openAccessibilitySettings(context)
                        } else {
                            doubleTapLockEnabled = !doubleTapLockEnabled
                            setDoubleTapLockEnabled(context, doubleTapLockEnabled)
                        }
                    }
                )

                // Swipe down for notifications/quick settings
                var swipeDownEnabled by remember { mutableStateOf(com.bearinmind.launcher314.data.getSwipeDownNotifications(context)) }
                var swipeDownMode by remember { mutableIntStateOf(com.bearinmind.launcher314.data.getSwipeDownMode(context)) }
                var showSwipeDownDropdown by remember { mutableStateOf(false) }
                val swipeDownModeLabel = if (swipeDownMode == 0) "Notifications" else "Quick Settings"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable {
                            swipeDownEnabled = !swipeDownEnabled
                            com.bearinmind.launcher314.data.setSwipeDownNotifications(context, swipeDownEnabled)
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Swipe down for ",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box {
                                val triangleRotation by animateFloatAsState(
                                    targetValue = if (showSwipeDownDropdown) -90f else 0f,
                                    label = "swipeDownTriangle"
                                )
                                Row(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = androidx.compose.material.ripple.rememberRipple()
                                        ) { showSwipeDownDropdown = !showSwipeDownDropdown }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = swipeDownModeLabel,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Canvas(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .rotate(triangleRotation)
                                    ) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width / 2, size.height * 0.8f)
                                            lineTo(size.width * 0.15f, size.height * 0.2f)
                                            lineTo(size.width * 0.85f, size.height * 0.2f)
                                            close()
                                        }
                                        drawPath(path, color = androidx.compose.ui.graphics.Color.Gray)
                                    }
                                }
                                DropdownMenu(
                                    expanded = showSwipeDownDropdown,
                                    onDismissRequest = { showSwipeDownDropdown = false },
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Notifications") },
                                        onClick = {
                                            swipeDownMode = 0
                                            com.bearinmind.launcher314.data.setSwipeDownMode(context, 0)
                                            showSwipeDownDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Quick Settings") },
                                        onClick = {
                                            swipeDownMode = 1
                                            com.bearinmind.launcher314.data.setSwipeDownMode(context, 1)
                                            showSwipeDownDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Swipe down on home screen to access",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = swipeDownEnabled,
                        onCheckedChange = {
                            swipeDownEnabled = it
                            com.bearinmind.launcher314.data.setSwipeDownNotifications(context, it)
                        }
                    )
                }

                // Hide apps from launcher
                val hiddenAppCount = com.bearinmind.launcher314.data.getHiddenApps(context).size
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onHideAppsClick() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hide apps from launcher",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (hiddenAppCount > 0) "$hiddenAppCount apps hidden" else "No apps hidden",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // Developer Information Section
            SettingsSection(title = "Development Information") {
                SettingsClickableItem(
                    title = "Version",
                    subtitle = "v0.0.9-beta",
                    onClick = { }
                )
                SettingsClickableItem(
                    title = "Bugs & Feature Requests",
                    subtitle = "Report bugs or leave feature requests on my github or reddit posts :)",
                    onClick = { }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bearinmindcat/Launcher314"))
                            try { context.startActivity(intent) } catch (_: Exception) { }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("GitHub")
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/fossdroid/comments/1rlpxux/just_another_launcher_app_i_made_with_easy/"))
                            try { context.startActivity(intent) } catch (_: Exception) { }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reddit")
                    }
                }
            }
        }
    }


}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // Minimum height for consistent touch targets
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled) textColor else textColor.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = if (enabled) textColor.copy(alpha = 0.6f) else textColor.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SettingsLargeItem(
    title: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            color = textColor
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // Minimum height for consistent touch targets
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Card with text size slider for icon labels.
 * Same layout pattern as ScrollbarPersonalizationCard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconTextPersonalizationCard(
    textSizePercent: Float,
    onTextSizeChange: (Float) -> Unit,
    onFontsClick: () -> Unit = {},
    onIconPacksClick: () -> Unit = {},
    globalIconShape: String? = null,
    onGlobalIconShapeChanged: (String?) -> Unit = {},
    globalIconBgColor: Int? = null,
    onGlobalIconBgColorChanged: (Int?) -> Unit = {},
    globalIconBgIntensity: Int = 100,
    onGlobalIconBgIntensityChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    // Re-read on every recomposition so it updates when returning from FontsScreen/IconPacksScreen
    val selectedFontId = getSelectedFont(context)
    val selectedFontName = FontManager.getSelectedFontName(context)
    val selectedFontFamily = FontManager.getSelectedFontFamily(context)
    val selectedIconPackName = IconPackManager.getSelectedIconPackName(context)
    val selectedIconPackIconPath = IconPackManager.getSelectedIconPackIconPath(context)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Text size slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            ThumbDragHorizontalSlider(
                currentValue = textSizePercent,
                config = SliderConfigs.iconTextSize,
                onValueChange = { newSize ->
                    onTextSizeChange(newSize)
                    setIconTextSizePercent(context, newSize.roundToInt())
                },
                onValueChangeFinished = {
                    setIconTextSizePercent(context, textSizePercent.roundToInt())
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Font selection button — aligned with slider track (same padding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            Button(
                onClick = onFontsClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = selectedFontName,
                    fontFamily = selectedFontFamily ?: FontFamily.Default
                )
            }
        }

        // "Font" label centered below button, matching slider label style
        Text(
            text = "Font",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp, top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Text Color intensity slider + color row
        var localTextColor by remember { mutableStateOf(com.bearinmind.launcher314.data.getGlobalTextColor(context)) }
        var localTextIntensity by remember { mutableIntStateOf(com.bearinmind.launcher314.data.getGlobalTextColorIntensity(context)) }
        run {
            val textIntensityEnabled = localTextColor != null

            // Intensity slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 76.dp)
            ) {
                ThumbDragHorizontalSlider(
                    currentValue = localTextIntensity.toFloat(),
                    config = SliderConfigs.colorIntensity,
                    enabled = textIntensityEnabled,
                    onValueChange = { localTextIntensity = it.toInt() },
                    onValueChangeFinished = { com.bearinmind.launcher314.data.setGlobalTextColorIntensity(context, localTextIntensity) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val textColors = listOf(
                null to "—",
                0xFFFFFFFF.toInt() to "White",
                0xFFEF9A9A.toInt() to "Red",
                0xFFA5D6A7.toInt() to "Green",
                0xFF90CAF9.toInt() to "Blue",
                0xFFFFF59D.toInt() to "Yellow",
                0xFFFFCC80.toInt() to "Orange",
                0xFFCE93D8.toInt() to "Purple",
                0xFF9FA8DA.toInt() to "Indigo"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 76.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                textColors.forEach { (color, _) ->
                    val isSelected = localTextColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (color != null) Modifier.background(Color(color))
                                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable {
                                localTextColor = color
                                com.bearinmind.launcher314.data.setGlobalTextColor(context, color)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == null) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Default text color",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Text Color",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 76.dp, top = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Icon Pack selection button — same layout as font button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp)
        ) {
            Button(
                onClick = onIconPacksClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (selectedIconPackIconPath != null) {
                    AsyncImage(
                        model = java.io.File(selectedIconPackIconPath),
                        contentDescription = selectedIconPackName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = selectedIconPackName)
            }
        }

        // "Icon Pack" label centered below button
        Text(
            text = "Icon Pack",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp, top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Icon Shape + Background Color + Intensity — with live icon preview on the right
        val aospShapes = listOf(IconShapes.CIRCLE, IconShapes.ROUNDED_SQUARE, IconShapes.SQUIRCLE, IconShapes.TEARDROP)
        val iconPreviewContext = LocalContext.current
        val ownPackageName = iconPreviewContext.packageName

        // Local intensity for smooth dragging without regenerating on every frame
        var localIntensity by remember(globalIconBgIntensity) { mutableStateOf(globalIconBgIntensity) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left side — controls
            Column(modifier = Modifier.weight(1f)) {
                // Shape row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val isDefault = globalIconShape == null
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isDefault) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable { onGlobalIconShapeChanged(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "No shape",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    aospShapes.forEach { shapeName ->
                        val isSelected = globalIconShape == shapeName
                        val clipShape = getIconShape(shapeName)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .clickable { onGlobalIconShapeChanged(shapeName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .then(
                                        if (clipShape != null) Modifier.clip(clipShape)
                                        else Modifier
                                    )
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                Text(
                    text = "Icon Shape",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp),
                    textAlign = TextAlign.Center
                )

                // Icon color intensity — always visible, disabled when no shape or no bg color
                run {
                    Spacer(modifier = Modifier.height(12.dp))

                    val intensityEnabled = globalIconShape != null && globalIconBgColor != null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    ) {
                        ThumbDragHorizontalSlider(
                            currentValue = localIntensity.toFloat(),
                            config = SliderConfigs.iconColorIntensity,
                            enabled = intensityEnabled,
                            onValueChange = { localIntensity = it.toInt() },
                            onValueChangeFinished = { onGlobalIconBgIntensityChanged(localIntensity) }
                        )
                    }

                    Text(
                        text = "Icon Color Intensity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (intensityEnabled) 0.7f else 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Background color row — always visible, disabled when no shape
                run {
                    Spacer(modifier = Modifier.height(12.dp))

                    val bgColorEnabled = globalIconShape != null
                    val iconBgColors = listOf(
                        null to "—",
                        0xFFFFFFFF.toInt() to "White",
                        0xFFEF9A9A.toInt() to "Red",
                        0xFFA5D6A7.toInt() to "Green",
                        0xFF90CAF9.toInt() to "Blue",
                        0xFFFFF59D.toInt() to "Yellow",
                        0xFFFFCC80.toInt() to "Orange",
                        0xFFCE93D8.toInt() to "Purple",
                        0xFF9FA8DA.toInt() to "Indigo"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .then(if (!bgColorEnabled) Modifier.graphicsLayer { alpha = 0.4f } else Modifier),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        iconBgColors.forEach { (color, label) ->
                            val isSelected = globalIconBgColor == color
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (color != null) Modifier.background(Color(color))
                                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                        else Modifier
                                    )
                                    .then(if (bgColorEnabled) Modifier.clickable { onGlobalIconBgColorChanged(color) } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (color == null) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "No background color",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Icon Background Color",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (bgColorEnabled) 0.7f else 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } // end left Column

            // Right side — live icon preview with label
            Column(
                modifier = Modifier
                    .width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val previewBitmap = remember(globalIconShape, globalIconBgColor, localIntensity) {
                    try {
                        if (globalIconShape != null) {
                            generateShapedIconBitmap(
                                iconPreviewContext, ownPackageName, globalIconShape,
                                bgColor = globalIconBgColor,
                                intensity = localIntensity
                            ).asImageBitmap()
                        } else null
                    } catch (_: Exception) { null }
                }
                val clipShape = getIconShape(globalIconShape)
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Icon preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(48.dp)
                            .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier)
                    )
                } else {
                    val rawIconDrawable = remember {
                        try { iconPreviewContext.packageManager.getApplicationIcon(ownPackageName) } catch (_: Exception) { null }
                    }
                    if (rawIconDrawable != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = rawIconDrawable),
                            contentDescription = "Icon preview",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                val appLabel = remember {
                    try {
                        val appInfo = iconPreviewContext.packageManager.getApplicationInfo(ownPackageName, 0)
                        iconPreviewContext.packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) { "App" }
                }
                val previewTextSize = (textSizePercent / 100f * 12f).sp
                val previewTextIntensity = localTextIntensity / 100f
                val previewTextColor = if (localTextColor != null) {
                    val b = Color(localTextColor!!)
                    Color(b.red * previewTextIntensity, b.green * previewTextIntensity, b.blue * previewTextIntensity, b.alpha)
                } else Color.White
                Text(
                    text = appLabel,
                    fontSize = previewTextSize,
                    fontFamily = selectedFontFamily ?: FontFamily.Default,
                    color = previewTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } // end Row
    }
}

// Scrollbar color options - Muted colors from calendar bubbles
private val scrollbarColors = listOf(
    0xFFFFFFFF.toInt() to "White",
    0xFFEF9A9A.toInt() to "Red",
    0xFFA5D6A7.toInt() to "Green",
    0xFF90CAF9.toInt() to "Blue",
    0xFFFFF59D.toInt() to "Yellow",
    0xFFFFCC80.toInt() to "Orange",
    0xFFCE93D8.toInt() to "Purple",
    0xFF9FA8DA.toInt() to "Indigo"
)

@Composable
fun ScrollbarPersonalizationCard(
    scrollbarWidth: Float,
    scrollbarHeight: Float,
    scrollbarColor: Int,
    scrollbarIntensity: Float,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    val context = LocalContext.current

    // Apply intensity to color (0% = black, 100% = original color)
    fun adjustColorIntensity(color: Int, intensity: Float): Color {
        val baseColor = Color(color)
        val factor = (intensity / 100f).coerceIn(0f, 1f)
        return Color(
            red = baseColor.red * factor,
            green = baseColor.green * factor,
            blue = baseColor.blue * factor,
            alpha = baseColor.alpha
        )
    }

    // Container matching AppDrawerPreviewSection layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Row with sliders on left, mini preview on right (matching preview + icon slider layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left side - Sliders with same padding as columns slider
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                // Width slider (percentage of screen width)
                ThumbDragHorizontalSlider(
                    currentValue = scrollbarWidth,
                    config = SliderConfigs.scrollbarWidth,
                    onValueChange = { newWidth ->
                        onWidthChange(newWidth)
                        setScrollbarWidthPercent(context, newWidth.roundToInt())
                    },
                    onValueChangeFinished = {
                        setScrollbarWidthPercent(context, scrollbarWidth.roundToInt())
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Height slider (percentage of screen height)
                ThumbDragHorizontalSlider(
                    currentValue = scrollbarHeight,
                    config = SliderConfigs.scrollbarHeight,
                    onValueChange = { newHeight ->
                        onHeightChange(newHeight)
                        setScrollbarHeightPercent(context, newHeight.roundToInt())
                    },
                    onValueChangeFinished = {
                        setScrollbarHeightPercent(context, scrollbarHeight.roundToInt())
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color Intensity slider
                ThumbDragHorizontalSlider(
                    currentValue = scrollbarIntensity,
                    config = SliderConfigs.colorIntensity,
                    onValueChange = { newIntensity ->
                        onIntensityChange(newIntensity)
                        setScrollbarIntensity(context, newIntensity.roundToInt())
                    },
                    onValueChangeFinished = {
                        setScrollbarIntensity(context, scrollbarIntensity.roundToInt())
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color selection - cubes with label below (affected by intensity)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    scrollbarColors.forEach { (color, _) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(adjustColorIntensity(color, scrollbarIntensity))
                                .then(
                                    if (scrollbarColor == color) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    onColorChange(color)
                                    setScrollbarColor(context, color)
                                }
                        )
                    }
                }
                // Color label below cubes
                Text(
                    text = "Color",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Right side - Scrollbar preview
            ScrollbarPreview(
                scrollbarWidth = scrollbarWidth,
                scrollbarHeight = scrollbarHeight,
                adjustedColor = adjustColorIntensity(scrollbarColor, scrollbarIntensity)
            )
        }
    }
}

/**
 * Simple scrollbar preview - just shows the scrollbar thumb centered in a 72dp area
 * Fixed at 72dp width to match the link button area (ensuring slider X coordinates match)
 * scrollbarWidth/Height are percentages that get converted to dp for display
 */
@Composable
private fun ScrollbarPreview(
    scrollbarWidth: Float,
    scrollbarHeight: Float,
    adjustedColor: Color
) {
    // Convert percentage to dp for preview
    val configuration = LocalConfiguration.current
    val widthDp = (configuration.screenWidthDp * 0.02f * scrollbarWidth / 100f)
    val heightDp = (configuration.screenHeightDp * 0.20f * scrollbarHeight / 100f)

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Navigation dots - above the scrollbar, same X center offset
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 8.dp)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(widthDp.dp)
                    .clip(CircleShape)
                    .background(adjustedColor.copy(alpha = 0.9f))
            )
            Box(
                modifier = Modifier
                    .size(widthDp.dp)
                    .clip(CircleShape)
                    .background(adjustedColor.copy(alpha = 0.3f))
            )
        }

        // Scrollbar thumb preview - original position
        Box(
            modifier = Modifier
                .offset(x = 8.dp, y = 56.dp)
                .width(widthDp.dp)
                .height((heightDp * 0.5f).dp)
                .clip(RoundedCornerShape(widthDp.dp / 2))
                .background(adjustedColor.copy(alpha = 0.7f))
        )
    }
}
