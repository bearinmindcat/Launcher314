package com.bearinmind.launcher314

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bearinmind.launcher314.helpers.applyTransparentNavigation
import com.bearinmind.launcher314.data.getHomeGridSize
import com.bearinmind.launcher314.data.getHomeGridRows
import com.bearinmind.launcher314.ui.widgets.WidgetManager
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import com.bearinmind.launcher314.data.HomeScreenApp
import com.bearinmind.launcher314.data.HomeScreenData
import kotlinx.serialization.json.Json
import java.io.File
import com.bearinmind.launcher314.ui.home.LauncherScreen
import com.bearinmind.launcher314.data.LauncherUtils
import com.bearinmind.launcher314.ui.home.LauncherWithDrawer
import com.bearinmind.launcher314.ui.drawer.AppDrawerScreen
import com.bearinmind.launcher314.ui.settings.SettingsScreen
import com.bearinmind.launcher314.ui.widgets.WidgetInfo
import com.bearinmind.launcher314.ui.widgets.WidgetsScreen
import com.bearinmind.launcher314.ui.settings.FontsScreen
import com.bearinmind.launcher314.ui.settings.HideAppsScreen
import com.bearinmind.launcher314.ui.settings.IconPacksScreen
import com.bearinmind.launcher314.ui.theme.Launcher314Theme

class MainActivity : ComponentActivity() {
    private var isLauncherMode = false

    // Observable counter that triggers home screen refresh when widgets are added
    var widgetAddedTrigger = mutableIntStateOf(0)
        private set

    // Observable trigger: incremented when home button is pressed (signals Compose to go home)
    var homeButtonTrigger = mutableIntStateOf(0)
        private set

    // Observable trigger: incremented when accessibility service requests drawer open
    var openDrawerTrigger = mutableIntStateOf(0)
        private set

    // Pending widget info for binding
    private var pendingWidgetInfo: WidgetInfo? = null
    private var pendingWidgetId: Int = -1

    // Callback to navigate to widgets screen after permission is granted
    private var onWidgetPermissionGranted: (() -> Unit)? = null

    // Activity result launcher for initial widget permission check (when clicking Widgets button)
    private val widgetPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Clean up the test widget ID
        if (pendingWidgetId != -1) {
            WidgetManager.deleteWidgetId(pendingWidgetId)
            pendingWidgetId = -1
        }

        if (result.resultCode == RESULT_OK) {
            // Permission granted, navigate to widgets screen
            onWidgetPermissionGranted?.invoke()
            onWidgetPermissionGranted = null
        } else {
            Toast.makeText(this, "Widget permission required to add widgets", Toast.LENGTH_SHORT).show()
            onWidgetPermissionGranted = null
        }
    }

    // Activity result launcher for widget binding permission
    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Widget binding was successful, now check if configuration is needed
            pendingWidgetInfo?.let { widget ->
                if (WidgetManager.needsConfiguration(widget.providerInfo)) {
                    // Launch configuration activity via AppWidgetHost (has permission for non-exported activities)
                    launchWidgetConfigure(pendingWidgetId)
                } else {
                    addWidgetToHomeScreen(widget)
                }
            }
        } else {
            // User denied binding permission
            WidgetManager.deleteWidgetId(pendingWidgetId)
            pendingWidgetId = -1
            pendingWidgetInfo = null
            Toast.makeText(this, "Widget permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CONFIGURE_APPWIDGET = 9004
    }

    // Launch widget configure activity via AppWidgetHost (has permission for non-exported activities)
    private fun launchWidgetConfigure(appWidgetId: Int) {
        try {
            WidgetManager.getAppWidgetHost()?.startAppWidgetConfigureActivityForResult(
                this, appWidgetId, 0, REQUEST_CONFIGURE_APPWIDGET, null
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to launch widget configure", e)
            // If configure fails, still try to add the widget (some widgets work without config)
            pendingWidgetInfo?.let { addWidgetToHomeScreen(it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_APPWIDGET) {
            android.util.Log.d("MainActivity", "Widget configure result: resultCode=$resultCode (OK=${RESULT_OK}, CANCELED=${RESULT_CANCELED})")
            if (resultCode == RESULT_OK) {
                pendingWidgetInfo?.let { widget ->
                    addWidgetToHomeScreen(widget)
                }
            } else {
                // Some widgets return CANCELED even when they auto-configure during binding.
                // Try to add the widget anyway — if it's already bound, it may render fine.
                val manager = WidgetManager.getAppWidgetManager()
                val info = manager?.getAppWidgetInfo(pendingWidgetId)
                if (info != null) {
                    // Widget is still bound and has valid provider info — add it
                    android.util.Log.d("MainActivity", "Widget configure cancelled but widget is bound, adding anyway")
                    pendingWidgetInfo?.let { widget ->
                        addWidgetToHomeScreen(widget)
                    }
                } else {
                    // Widget is truly invalid — clean up
                    WidgetManager.deleteWidgetId(pendingWidgetId)
                    pendingWidgetId = -1
                    pendingWidgetInfo = null
                    Toast.makeText(this, "Widget configuration cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addWidgetToHomeScreen(widget: WidgetInfo) {
        val gridColumns = getHomeGridSize(this)
        val gridRows = getHomeGridRows(this)

        // Find first available position for the widget
        val availablePos = findAvailablePositionForWidget(widget.cellWidth, widget.cellHeight, gridColumns, gridRows)

        if (availablePos == null) {
            Toast.makeText(this, "Not enough space for widget", Toast.LENGTH_SHORT).show()
            WidgetManager.deleteWidgetId(pendingWidgetId)
            pendingWidgetId = -1
            pendingWidgetInfo = null
            return
        }

        // Add the widget to the home screen using Einstein-style grid model
        val placedWidget = PlacedWidget(
            appWidgetId = pendingWidgetId,
            packageName = widget.providerInfo.provider.packageName,
            className = widget.providerInfo.provider.className,
            startColumn = availablePos.first,
            startRow = availablePos.second,
            columnSpan = widget.cellWidth,
            rowSpan = widget.cellHeight
        )
        WidgetManager.addPlacedWidget(this, placedWidget)

        // Restart host listener so it picks up the newly bound widget's RemoteViews
        WidgetManager.stopListening()
        WidgetManager.startListening()

        Toast.makeText(this, "Widget \"${widget.label}\" added!", Toast.LENGTH_SHORT).show()

        // Trigger home screen refresh so the new widget renders
        widgetAddedTrigger.intValue++

        // Clear pending state
        pendingWidgetId = -1
        pendingWidgetInfo = null
    }

    /**
     * Find the first available position for a widget on the grid.
     * Returns Pair(column, row) or null if no space available.
     */
    private fun findAvailablePositionForWidget(widgetCols: Int, widgetRows: Int, gridColumns: Int, gridRows: Int): Pair<Int, Int>? {
        val occupiedCells = getOccupiedCells(gridColumns)

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
        return null
    }

    /**
     * Get all occupied cell indices on the home screen grid.
     */
    fun getOccupiedCells(gridColumns: Int, page: Int = 0): Set<Int> {
        val occupiedCells = mutableSetOf<Int>()

        // Get occupied cells from placed apps on this page
        val homeScreenData = loadHomeScreenData()
        homeScreenData.apps.filter { it.page == page }.forEach { app ->
            occupiedCells.add(app.position)
        }

        // Get occupied cells from placed folders on this page
        homeScreenData.folders.filter { it.page == page }.forEach { folder ->
            occupiedCells.add(folder.position)
        }

        // Get occupied cells from placed widgets on this page (only primary widget per stack)
        val placedWidgets = WidgetManager.loadPlacedWidgets(this)
        val seenStacks = mutableSetOf<String>()
        placedWidgets.filter { it.page == page }.filter { w ->
            val sid = w.stackId
            if (sid == null) true
            else if (seenStacks.contains(sid)) false
            else { seenStacks.add(sid); true }
        }.forEach { widget ->
            for (row in widget.startRow until widget.startRow + widget.rowSpan) {
                for (col in widget.startColumn until widget.startColumn + widget.columnSpan) {
                    val cellIndex = row * gridColumns + col
                    occupiedCells.add(cellIndex)
                }
            }
        }

        return occupiedCells
    }

    // loadHomeScreenData() uses shared function from data/HomeScreenStorage.kt
    private fun loadHomeScreenData(): HomeScreenData = com.bearinmind.launcher314.data.loadHomeScreenData(this)

    /**
     * Check if widget permission is granted and request it if needed.
     * Returns true if permission is already granted (caller should proceed immediately).
     * Returns false if permission dialog was shown (caller should wait for callback).
     */
    fun checkWidgetPermissionAndNavigate(onPermissionGranted: () -> Unit): Boolean {
        val appWidgetManager = WidgetManager.getAppWidgetManager() ?: return true

        // Get any available widget provider to test permission
        val providers = appWidgetManager.installedProviders
        if (providers.isEmpty()) {
            // No widgets available, just proceed
            onPermissionGranted()
            return true
        }

        // Allocate a test widget ID
        pendingWidgetId = WidgetManager.allocateWidgetId()
        if (pendingWidgetId == -1) {
            // Failed to allocate, just proceed
            onPermissionGranted()
            return true
        }

        // Try to bind to the first provider to check if we have permission
        val testProvider = providers.first()
        val hasPermission = WidgetManager.bindWidget(this, pendingWidgetId, testProvider)

        if (hasPermission) {
            // We have permission, clean up and proceed
            WidgetManager.deleteWidgetId(pendingWidgetId)
            pendingWidgetId = -1
            onPermissionGranted()
            return true
        } else {
            // Need to request permission
            onWidgetPermissionGranted = onPermissionGranted
            val bindIntent = WidgetManager.createBindIntent(pendingWidgetId, testProvider)
            widgetPermissionLauncher.launch(bindIntent)
            return false
        }
    }

    fun onWidgetSelectedFromPicker(widget: WidgetInfo) {
        // Allocate a new widget ID
        pendingWidgetId = WidgetManager.allocateWidgetId()
        if (pendingWidgetId == -1) {
            Toast.makeText(this, "Failed to allocate widget ID", Toast.LENGTH_SHORT).show()
            return
        }

        pendingWidgetInfo = widget

        // Try to bind the widget
        val bound = WidgetManager.bindWidget(this, pendingWidgetId, widget.providerInfo)

        if (bound) {
            // Binding succeeded without permission prompt
            if (WidgetManager.needsConfiguration(widget.providerInfo)) {
                // Launch configuration activity via AppWidgetHost (has permission for non-exported activities)
                launchWidgetConfigure(pendingWidgetId)
            } else {
                addWidgetToHomeScreen(widget)
            }
        } else {
            // Need to request binding permission
            val bindIntent = WidgetManager.createBindIntent(pendingWidgetId, widget.providerInfo)
            bindWidgetLauncher.launch(bindIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isHomeLaunch = intent?.categories?.contains(Intent.CATEGORY_HOME) == true
        val navigateTo = intent?.getStringExtra("navigate_to")

        // Determine launcher mode:
        // - If launcher is enabled AND this is a home launch → launcher mode
        // - If launcher is enabled AND opened from app icon → also launcher mode
        //   (real launchers like Lawnchair/Fossify/Yagni always show the launcher when enabled)
        // - Preview modes → launcher mode
        val launcherEnabled = LauncherUtils.isEnabled(this)
        isLauncherMode = (launcherEnabled && (isHomeLaunch || navigateTo == null)) ||
                         navigateTo == "launcher_preview" ||
                         navigateTo == "launcher_preview_drawer"

        // Apply wallpaper theme for launcher mode BEFORE super.onCreate
        if (isLauncherMode) {
            setTheme(R.style.Theme_Launcher314_Launcher)
        }

        super.onCreate(savedInstanceState)

        // Handle pin shortcut request (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            handlePinShortcutRequest(intent)
        }

        // Initialize widget manager
        WidgetManager.init(this)

        // Make navigation bar fully transparent — always enable edge-to-edge
        // so WindowInsets padding works consistently on all screens
        applyTransparentNavigation(this)

        // Determine start destination
        val startDestination = when {
            navigateTo == "app_drawer" -> "app_drawer"
            navigateTo == "launcher_preview" -> "launcher"
            navigateTo == "launcher_preview_drawer" -> "launcher"
            isLauncherMode -> "launcher"
            else -> "settings"
        }

        // Whether to auto-open the drawer
        val openDrawerOnStart = navigateTo == "launcher_preview_drawer"

        setContent {
            Launcher314Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isLauncherMode)
                        androidx.compose.ui.graphics.Color.Transparent
                    else
                        MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        startDestination = startDestination,
                        isLauncherMode = isLauncherMode,
                        openDrawerOnStart = openDrawerOnStart
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for widget updates
        WidgetManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening for widget updates
        WidgetManager.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val isHomeLaunch = intent.categories?.contains(Intent.CATEGORY_HOME) == true
        val navigateTo = intent.getStringExtra("navigate_to")
        android.util.Log.d("MainActivity", "onNewIntent: isHomeLaunch=$isHomeLaunch, navigateTo=$navigateTo, isLauncherMode=$isLauncherMode, categories=${intent.categories}")

        // Handle pin shortcut request (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (handlePinShortcutRequest(intent)) return
        }

        when {
            // Home button pressed while already in launcher mode:
            // Signal Compose to return to home screen and close drawer
            isHomeLaunch && isLauncherMode -> {
                homeButtonTrigger.intValue++
            }
            // Home button pressed but activity is in non-launcher mode:
            // Restart fresh so onCreate picks up launcher mode with correct theme.
            // Uses CLEAR_TASK to avoid NavController restoring stale back stack.
            isHomeLaunch && !isLauncherMode -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
            // Accessibility service requesting drawer open (already in launcher mode)
            navigateTo == "app_drawer" && isLauncherMode -> {
                openDrawerTrigger.intValue++
            }
            // Any navigate_to intent that needs a mode switch (preview, app_drawer from non-launcher):
            // Start fresh activity to avoid NavController restoring previous mode's back stack
            // (which causes settings to overlap behind the transparent launcher).
            navigateTo != null -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("navigate_to", navigateTo)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
        }
    }

    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.O)
    private fun handlePinShortcutRequest(intent: Intent?): Boolean {
        if (intent == null) return false
        val launcherApps = getSystemService(android.content.pm.LauncherApps::class.java) ?: return false

        val request = try {
            launcherApps.getPinItemRequest(intent)
        } catch (_: Exception) {
            null
        } ?: return false

        if (!request.isValid) return false

        val shortcutInfo = request.shortcutInfo ?: return false
        val shortcutId = "shortcut_${System.currentTimeMillis()}"
        val iconsDir = java.io.File(filesDir, "shortcut_icons")
        if (!iconsDir.exists()) iconsDir.mkdirs()

        // Save shortcut metadata
        val name = shortcutInfo.shortLabel?.toString() ?: shortcutInfo.longLabel?.toString() ?: "Shortcut"
        // Try getIntent first, fall back to getIntents (plural)
        val launchIntent = try {
            shortcutInfo.intent ?: shortcutInfo.getIntents()?.lastOrNull()
        } catch (_: Exception) { null }

        val metaFile = java.io.File(iconsDir, "$shortcutId.meta")
        if (launchIntent != null) {
            metaFile.writeText("$name\n${launchIntent.toUri(Intent.URI_INTENT_SCHEME)}")
        } else {
            // Even without an intent, save the shortcut with package info so we can try to launch it
            val pkg = shortcutInfo.`package`
            val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = pkg
            }
            metaFile.writeText("$name\n${fallbackIntent.toUri(Intent.URI_INTENT_SCHEME)}")
        }

        // Save icon — try to get the shortcut's actual icon via LauncherApps
        try {
            val launcherAppsForIcon = getSystemService(android.content.pm.LauncherApps::class.java)
            val iconDrawable = launcherAppsForIcon?.getShortcutIconDrawable(shortcutInfo, resources.displayMetrics.densityDpi)
            val bitmap = if (iconDrawable != null) {
                com.bearinmind.launcher314.data.drawableToBitmap(iconDrawable)
            } else {
                val fallbackIcon = try {
                    packageManager.getApplicationIcon(shortcutInfo.`package`)
                } catch (_: Exception) { null }
                if (fallbackIcon != null) com.bearinmind.launcher314.data.drawableToBitmap(fallbackIcon) else null
            }
            if (bitmap != null) {
                val iconFile = java.io.File(iconsDir, "$shortcutId.png")
                com.bearinmind.launcher314.data.saveBitmapToFile(bitmap, iconFile)
                bitmap.recycle()
            }
        } catch (_: Exception) {}

        // Add to home screen — find first truly empty cell across all pages
        val data = com.bearinmind.launcher314.data.loadHomeScreenData(this)
        val prefs = applicationContext.getSharedPreferences("app_drawer_settings", MODE_PRIVATE)
        val gridColumns = prefs.getInt("home_grid_columns", 4)
        val gridRows = prefs.getInt("home_grid_rows", 5)
        val totalCells = gridColumns * gridRows
        val placedWidgets = com.bearinmind.launcher314.ui.widgets.WidgetManager.loadPlacedWidgets(this)

        // Try each page starting from 0
        var targetPage = 0
        var targetPosition = 0
        var found = false
        for (page in 0..10) {
            val occupiedByApps = data.apps.filter { it.page == page }.map { it.position }.toSet()
            val occupiedByFolders = data.folders.filter { it.page == page }.map { it.position }.toSet()
            val occupiedByWidgets = mutableSetOf<Int>()
            placedWidgets.filter { it.page == page }.forEach { widget ->
                for (r in widget.startRow until (widget.startRow + widget.rowSpan)) {
                    for (c in widget.startColumn until (widget.startColumn + widget.columnSpan)) {
                        occupiedByWidgets.add(r * gridColumns + c)
                    }
                }
            }
            val allOccupied = occupiedByApps + occupiedByFolders + occupiedByWidgets
            val empty = (0 until totalCells).firstOrNull { it !in allOccupied }
            if (empty != null) {
                targetPage = page
                targetPosition = empty
                found = true
                break
            }
        }
        if (!found) {
            targetPage = 0
            targetPosition = 0
        }

        val newApp = com.bearinmind.launcher314.data.HomeScreenApp(
            packageName = shortcutId,
            position = targetPosition,
            page = targetPage
        )
        val updatedData = data.copy(apps = data.apps + newApp)
        com.bearinmind.launcher314.data.saveHomeScreenData(this, updatedData)

        // Accept the pin request
        request.accept()

        android.widget.Toast.makeText(this, "\"$name\" added to home screen", android.widget.Toast.LENGTH_SHORT).show()

        // Restart the activity to force a full home screen reload
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(restartIntent)

        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    startDestination: String = "settings",
    isLauncherMode: Boolean = false,
    openDrawerOnStart: Boolean = false
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Get activity reference for permission check
    val activity = context as? MainActivity

    // When home button is pressed (singleTask delivers via onNewIntent),
    // pop back to the launcher route so settings/widgets/fonts are dismissed
    val homeButtonTrigger = activity?.homeButtonTrigger?.intValue ?: 0
    val openDrawerTrigger = activity?.openDrawerTrigger?.intValue ?: 0
    LaunchedEffect(homeButtonTrigger) {
        if (homeButtonTrigger > 0) {
            navController.popBackStack("launcher", inclusive = false)
        }
    }

    // For launcher mode, don't use Scaffold to avoid any background
    // Use the combined LauncherWithDrawer that has integrated swipe gesture
    if (isLauncherMode && startDestination == "launcher") {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("launcher") {
                // Combined launcher + drawer with swipe gesture
                LauncherWithDrawer(
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onWidgetsClick = {
                        // Check permission before navigating to widgets
                        activity?.checkWidgetPermissionAndNavigate {
                            navController.navigate("widgets")
                        } ?: navController.navigate("widgets")
                    },
                    openDrawerOnStart = openDrawerOnStart,
                    widgetRefreshTrigger = activity?.widgetAddedTrigger?.intValue ?: 0,
                    homeButtonTrigger = homeButtonTrigger,
                    openDrawerTrigger = openDrawerTrigger
                )
            }
            composable("widgets") {
                // Widgets screen handles its own styling (matches app drawer)
                val activity = context as? MainActivity
                val gridColumns = getHomeGridSize(context)
                val gridRows = getHomeGridRows(context)
                WidgetsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onWidgetSelected = { widget ->
                        activity?.onWidgetSelectedFromPicker(widget)
                        navController.popBackStack() // Go back to launcher after selection
                    },
                    gridColumns = gridColumns,
                    gridRows = gridRows,
                    getOccupiedCells = {
                        activity?.getOccupiedCells(gridColumns) ?: emptySet()
                    }
                )
            }
            composable("settings") {
                        val settingsAct = context as? MainActivity
                        androidx.compose.runtime.DisposableEffect(Unit) {
                            onDispose {
                                settingsAct?.widgetAddedTrigger?.intValue = (settingsAct?.widgetAddedTrigger?.intValue ?: 0) + 1
                            }
                        }
                        SettingsScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            onClearData = { },
                            onExportData = { },
                            onImportData = { false },
                            onPreviewDrawer = {
                                android.util.Log.d("MainActivity", "Preview drawer clicked (launcher mode), popping to launcher + opening drawer")
                                val popped = navController.popBackStack("launcher", inclusive = false)
                                android.util.Log.d("MainActivity", "popBackStack result: $popped")
                                if (popped) {
                                    activity?.openDrawerTrigger?.let { it.intValue++ }
                                }
                            },
                            onPreviewLauncher = {
                                android.util.Log.d("MainActivity", "Preview launcher clicked (launcher mode), popping to launcher")
                                val popped = navController.popBackStack("launcher", inclusive = false)
                                android.util.Log.d("MainActivity", "popBackStack result: $popped")
                            },
                            onFontsClick = {
                                navController.navigate("fonts")
                            },
                            onIconPacksClick = {
                                navController.navigate("icon_packs")
                            },
                            onHideAppsClick = {
                                navController.navigate("hide_apps")
                            }
                        )
            }
            composable("fonts") {
                FontsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("icon_packs") {
                IconPacksScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("hide_apps") {
                HideAppsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
                composable("launcher") {
                    LauncherScreen(
                        onOpenAppDrawer = {
                            navController.navigate("app_drawer")
                        },
                        onOpenSettings = {
                            navController.navigate("settings")
                        },
                        onOpenWidgets = {
                            // Check permission before navigating to widgets
                            activity?.checkWidgetPermissionAndNavigate {
                                navController.navigate("widgets")
                            } ?: navController.navigate("widgets")
                        }
                    )
                }
                composable("widgets") {
                    val activity = context as? MainActivity
                    val gridColumns = getHomeGridSize(context)
                    val gridRows = getHomeGridRows(context)
                    WidgetsScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                        onWidgetSelected = { widget ->
                            activity?.onWidgetSelectedFromPicker(widget)
                            navController.popBackStack()
                        },
                        gridColumns = gridColumns,
                        gridRows = gridRows,
                        getOccupiedCells = {
                            activity?.getOccupiedCells(gridColumns) ?: emptySet()
                        }
                    )
                }
                composable("app_drawer") {
                    AppDrawerScreen(
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
                composable("settings") {
                    val settingsAct = context as? MainActivity
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose {
                            settingsAct?.widgetAddedTrigger?.intValue = (settingsAct?.widgetAddedTrigger?.intValue ?: 0) + 1
                        }
                    }
                    SettingsScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                        onClearData = { },
                        onExportData = { },
                        onImportData = { false },
                        onPreviewDrawer = {
                            android.util.Log.d("MainActivity", "Preview drawer clicked (non-launcher mode), startActivity")
                            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                putExtra("navigate_to", "launcher_preview_drawer")
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        onPreviewLauncher = {
                            android.util.Log.d("MainActivity", "Preview launcher clicked (non-launcher mode), startActivity")
                            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                putExtra("navigate_to", "launcher_preview")
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        onFontsClick = {
                            navController.navigate("fonts")
                        },
                        onIconPacksClick = {
                            navController.navigate("icon_packs")
                        },
                        onHideAppsClick = {
                            navController.navigate("hide_apps")
                        }
                    )
                }
                composable("fonts") {
                    FontsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("icon_packs") {
                    IconPacksScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("hide_apps") {
                    HideAppsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
    }
}
