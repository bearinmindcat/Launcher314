package com.bearinmind.launcher314.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset as GeoOffset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PanoramaFishEye
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearinmind.launcher314.data.DeviceWallpaperEdit
import com.bearinmind.launcher314.data.WALLPAPER_MODE_DEVICE
import com.bearinmind.launcher314.data.WP_FILTER_NONE
import com.bearinmind.launcher314.data.bumpWallpaperCacheVersion
import com.bearinmind.launcher314.data.getWallpaperDim
import com.bearinmind.launcher314.data.setDeviceWallpaperEdit
import com.bearinmind.launcher314.data.setWallpaperDim
import com.bearinmind.launcher314.data.setWallpaperMode
import com.bearinmind.launcher314.helpers.WallpaperHelper
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WallpaperEditorScreen(
    sourceFile: File,
    initialEdit: DeviceWallpaperEdit,
    onDismiss: () -> Unit,
    onApplied: () -> Unit,
    /**
     * Invoked when the user taps the top-bar Preview button. Caller should
     * navigate out of the Settings screen so the launcher's real home-screen
     * chrome becomes visible; the wallpaper backdrop under it will reflect
     * the in-progress edit via `WallpaperPreviewBus`. The editor itself is
     * dismissed by the Preview-button handler right before this fires, and
     * Settings will auto-reopen the editor (with all state restored) when
     * the user navigates back.
     */
    onRequestPreviewLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val sourceBitmap = remember(sourceFile.absolutePath) {
        if (sourceFile.exists()) {
            BitmapFactory.decodeFile(sourceFile.absolutePath, BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            })
        } else null
    }
    LaunchedEffect(sourceBitmap) { if (sourceBitmap == null) onDismiss() }

    // All edit state
    var scale by remember { mutableFloatStateOf(initialEdit.scale.coerceIn(0.5f, 5f)) }
    var offsetX by remember { mutableFloatStateOf(initialEdit.offsetX) }
    var offsetY by remember { mutableFloatStateOf(initialEdit.offsetY) }
    var rotation by remember { mutableIntStateOf(initialEdit.rotationDegrees) }
    var flipH by remember { mutableStateOf(initialEdit.flipH) }
    var flipV by remember { mutableStateOf(initialEdit.flipV) }
    var cropL by remember { mutableFloatStateOf(initialEdit.cropLeft) }
    var cropT by remember { mutableFloatStateOf(initialEdit.cropTop) }
    var cropR by remember { mutableFloatStateOf(initialEdit.cropRight) }
    var cropB by remember { mutableFloatStateOf(initialEdit.cropBottom) }
    var brightness by remember { mutableIntStateOf(initialEdit.brightness) }
    var contrast by remember { mutableIntStateOf(initialEdit.contrast) }
    var saturation by remember { mutableIntStateOf(initialEdit.saturation) }
    var exposure by remember { mutableIntStateOf(initialEdit.exposure) }
    var highlights by remember { mutableIntStateOf(initialEdit.highlights) }
    var shadows by remember { mutableIntStateOf(initialEdit.shadows) }
    var tint by remember { mutableIntStateOf(initialEdit.tint) }
    var temperature by remember { mutableIntStateOf(initialEdit.temperature) }
    var sharpness by remember { mutableIntStateOf(initialEdit.sharpness) }
    var blur by remember { mutableIntStateOf(initialEdit.blur) }
    var vignette by remember { mutableIntStateOf(initialEdit.vignette) }
    var filter by remember { mutableStateOf(initialEdit.filter) }
    // Launcher dim — separate prefs key (not part of DeviceWallpaperEdit) since
    // it's a launcher overlay applied on top of the wallpaper at render time,
    // not a property of the bake. Writes through to SharedPreferences on every
    // change so the launcher's reactive read picks it up live in Preview.
    var dimEffect by remember { mutableIntStateOf(getWallpaperDim(context)) }
    var isApplying by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Undo / redo stack. Every "significant" action (button press, chip tap, slider
    // release, crop drag end) pushes the current edit state snapshot. Undo/Redo
    // walk the index back/forward; forward history is trimmed when a new action
    // happens mid-history.
    val history = remember { mutableStateListOf(initialEdit) }
    var historyIdx by remember { mutableIntStateOf(0) }
    val canUndo by remember { derivedStateOf { historyIdx > 0 } }
    val canRedo by remember { derivedStateOf { historyIdx < history.size - 1 } }

    fun snapshotEdit() = DeviceWallpaperEdit(
        scale = scale, offsetX = offsetX, offsetY = offsetY,
        rotationDegrees = rotation, flipH = flipH, flipV = flipV,
        cropLeft = cropL, cropTop = cropT, cropRight = cropR, cropBottom = cropB,
        brightness = brightness, contrast = contrast, saturation = saturation,
        exposure = exposure,
        highlights = highlights, shadows = shadows,
        tint = tint, temperature = temperature,
        sharpness = sharpness,
        blur = blur, vignette = vignette, filter = filter
    )

    fun commitEdit() {
        val snap = snapshotEdit()
        if (snap == history.getOrNull(historyIdx)) return
        // Trim forward history, append, advance index. Cap at 50 entries.
        while (history.size > historyIdx + 1) history.removeAt(history.size - 1)
        history.add(snap)
        if (history.size > 50) history.removeAt(0) else historyIdx++
    }

    fun applyEditState(e: DeviceWallpaperEdit) {
        scale = e.scale.coerceIn(0.5f, 5f)
        offsetX = e.offsetX; offsetY = e.offsetY
        rotation = e.rotationDegrees
        flipH = e.flipH; flipV = e.flipV
        cropL = e.cropLeft; cropT = e.cropTop; cropR = e.cropRight; cropB = e.cropBottom
        brightness = e.brightness; contrast = e.contrast; saturation = e.saturation
        exposure = e.exposure
        highlights = e.highlights; shadows = e.shadows
        tint = e.tint; temperature = e.temperature
        sharpness = e.sharpness
        blur = e.blur; vignette = e.vignette; filter = e.filter
    }

    fun doUndo() {
        if (canUndo) { historyIdx--; applyEditState(history[historyIdx]) }
    }
    fun doRedo() {
        if (canRedo) { historyIdx++; applyEditState(history[historyIdx]) }
    }
    fun doReset() {
        applyEditState(DeviceWallpaperEdit())
        commitEdit()
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val deviceAspect = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()

    // Combined ColorFilter for live preview. Everything here is a 4×5
    // ColorMatrix so drag stays at 60 fps. Drift vs. bake:
    //   EXACT  : Brightness, Temperature (Helland gains), Tint, Saturation, Filter
    //   APPROX : Exposure (skips linearization — slightly gentler than bake),
    //            Contrast (128 pivot instead of image-mean pivot — close at
    //            mid values, diverges on very dark or bright images),
    //            Highlights/Shadows (global contrast skew — bake uses
    //            smoothstep + Y/(1-Y) masks so only top/bottom 1/3 of tones
    //            move). Sharpness is bake-only (needs a blurred copy).
    val previewColorFilter = remember(
        brightness, contrast, saturation, filter,
        exposure, highlights, shadows, tint, temperature
    ) {
        // All effect sliders are now -100..+100 with 0 = neutral. `amt(v) = v/100f`
        // maps directly to a -1..+1 strength.
        fun amt(v: Int): Float = v / 100f
        val cm = android.graphics.ColorMatrix()
        if (brightness != 0) {
            val b = amt(brightness) * 127f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (exposure != 0) {
            val e = Math.pow(2.0, amt(exposure).toDouble()).toFloat()
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                e, 0f, 0f, 0f, 0f,
                0f, e, 0f, 0f, 0f,
                0f, 0f, e, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (temperature != 0) {
            // Planckian-locus Kelvin gains (Tanner Helland) — exact match to
            // the bake pipeline, so Temperature preview and final output agree.
            val gains = com.bearinmind.launcher314.helpers.WallpaperHelper.computeKelvinGains(amt(temperature))
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                gains[0], 0f, 0f, 0f, 0f,
                0f, gains[1], 0f, 0f, 0f,
                0f, 0f, gains[2], 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (tint != 0) {
            // YIQ Q-axis shift (green ↔ magenta) — matches GPUImage's white-
            // balance tint. Previously used the I-axis coefficients which
            // move orange↔cyan and duplicated Temperature's behavior; that
            // was a bug.
            val t = amt(tint) * 30f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0.621f * t,
                0f, 1f, 0f, 0f, -0.647f * t,
                0f, 0f, 1f, 0f, 1.702f * t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // Highlights/shadows PREVIEW approximation: global contrast skew biased
        // toward either end. Not identical to the per-pixel luminance mask at
        // bake time, but close enough visually for the user to see the slider
        // taking effect in real time.
        if (highlights != 0) {
            val h = amt(highlights) * 0.30f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f + h, 0f, 0f, 0f, -h * 128f,
                0f, 1f + h, 0f, 0f, -h * 128f,
                0f, 0f, 1f + h, 0f, -h * 128f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (shadows != 0) {
            val s = amt(shadows) * 0.30f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f - s, 0f, 0f, 0f, s * 128f,
                0f, 1f - s, 0f, 0f, s * 128f,
                0f, 0f, 1f - s, 0f, s * 128f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (contrast != 0) {
            val c = 1f + amt(contrast)
            val t = 128f * (1f - c)
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (saturation != 0) {
            val sat = android.graphics.ColorMatrix()
            // -100 -> 0% (grayscale), 0 -> 100% (identity), +100 -> 200%.
            sat.setSaturation((1f + amt(saturation)).coerceAtLeast(0f))
            cm.postConcat(sat)
        }
        // Named preset filters (Mono / Sepia / Warm / Cool / Vivid / Faded
        // / Polaroid / Kodachrome / Vintage / Teal&Orange / Night / Invert)
        // all resolve through the shared WallpaperFilters catalog — identical
        // math to the bake path so preview ↔ final output match.
        com.bearinmind.launcher314.helpers.WallpaperFilters.matrixFor(filter)?.let {
            cm.postConcat(android.graphics.ColorMatrix(it))
        }
        ColorFilter.colorMatrix(ColorMatrix(cm.array))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            // Required for windowInsetsPadding(navigationBars) below to actually
            // resolve to a non-zero value. With the default (true), Compose treats
            // the nav-bar inset as already consumed by the dialog window — but the
            // dialog visually still draws under the nav bar, hiding our IconRow.
            decorFitsSystemWindows = false
        )
    ) {
        // Match what the launcher Activity does in NavigationHelper.kt: make both
        // system bars fully TRANSPARENT and disable contrast enforcement. Setting
        // the bars to #0F0F0F directly doesn't work — on API 35+ `statusBarColor`
        // is deprecated/ignored, and Samsung One UI paints its own scrim on top
        // regardless. With transparent bars + no scrim, our Box's #0F0F0F
        // background shows through uniformly behind the system icons.
        val localView = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(Unit) {
            val dialogWindow = (localView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            val prevStatus = dialogWindow?.statusBarColor
            val prevNav = dialogWindow?.navigationBarColor
            val controller = dialogWindow?.let { androidx.core.view.WindowCompat.getInsetsController(it, localView) }
            val prevLightStatus = controller?.isAppearanceLightStatusBars
            val prevLightNav = controller?.isAppearanceLightNavigationBars
            val prevStatusContrast = if (Build.VERSION.SDK_INT >= 29) dialogWindow?.isStatusBarContrastEnforced else null
            val prevNavContrast = if (Build.VERSION.SDK_INT >= 29) dialogWindow?.isNavigationBarContrastEnforced else null

            // Force the Compose Dialog's window to be truly edge-to-edge AND clear
            // the `FLAG_DIM_BEHIND` flag — that flag is what actually causes the
            // "darker" scrim above the status bar + below the nav bar, because the
            // dim is applied to the region behind the dialog and the system bars
            // sit on top of it. Clearing DIM_BEHIND removes the scrim entirely.
            if (dialogWindow != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
                )
                dialogWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                dialogWindow.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // Also set dim amount to 0 just to be extra safe
                dialogWindow.setDimAmount(0f)
            }
            dialogWindow?.statusBarColor = android.graphics.Color.TRANSPARENT
            dialogWindow?.navigationBarColor = android.graphics.Color.TRANSPARENT
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
            if (Build.VERSION.SDK_INT >= 29 && dialogWindow != null) {
                dialogWindow.isStatusBarContrastEnforced = false
                dialogWindow.isNavigationBarContrastEnforced = false
            }
            onDispose {
                if (dialogWindow != null) {
                    if (prevStatus != null) dialogWindow.statusBarColor = prevStatus
                    if (prevNav != null) dialogWindow.navigationBarColor = prevNav
                    if (Build.VERSION.SDK_INT >= 29) {
                        if (prevStatusContrast != null) dialogWindow.isStatusBarContrastEnforced = prevStatusContrast
                        if (prevNavContrast != null) dialogWindow.isNavigationBarContrastEnforced = prevNavContrast
                    }
                }
                if (controller != null) {
                    if (prevLightStatus != null) controller.isAppearanceLightStatusBars = prevLightStatus
                    if (prevLightNav != null) controller.isAppearanceLightNavigationBars = prevLightNav
                }
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
        ) {
            // IconRow is a normal Column child (sized to its own height). The Column
            // applies a fixed bottom padding so the IconRow sits above the 3-button
            // nav bar. WindowInsets / view.rootWindowInsets both return 0 inside
            // this Dialog, so we hardcode 56dp — the standard 3-button nav bar
            // height on Samsung One UI / stock Android.
            val pinnedIconRowHeight = 88.dp
            val navBarPaddingDp = 96.dp
            // Active editing category — hoisted so the IconRow's CategoryIcons and
            // the Crossfade control above can share the same state.
            var activeCategoryShared by remember { mutableStateOf("brightness") }
            // Top-level section the user is editing: "crop" (geometric tools),
            // "filters" (named preset filters), "effects" (scalar sliders).
            // Defaults to Crop so "Edit again…" opens with the crop rectangle
            // ready to drag — the most common first step.
            var currentSection by remember { mutableStateOf("crop") }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = navBarPaddingDp)
            ) {
                // Top action bar: four boxed outlined buttons matching the DeviceAudioEQ
                // pattern (Material3 OutlinedButton with cornerRadius 12dp). Undo / Redo
                // walk the edit history; Reset clears to defaults; Save opens the target
                // selection AlertDialog.
                var saveDialogOpen by remember { mutableStateOf(false) }
                var saveTarget by remember { mutableIntStateOf(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT — icon-only Undo / Redo (Spectrum-style curved arrows)
                    IconOnlyTopButton(
                        painter = androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_undo_spectrum),
                        contentDesc = "Undo",
                        enabled = canUndo,
                        onClick = { doUndo() }
                    )
                    IconOnlyTopButton(
                        painter = androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_redo_spectrum),
                        contentDesc = "Redo",
                        enabled = canRedo,
                        onClick = { doRedo() }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // RIGHT — text-only Reset (red) / Save
                    TextOnlyTopButton(
                        label = "Reset",
                        textColor = Color(0xFFE53935),
                        onClick = { doReset() }
                    )
                    TextOnlyTopButton(
                        label = "Preview",
                        onClick = {
                            if (sourceBitmap != null) {
                                val snap = snapshotEdit()
                                // Stash snapshot + source for the launcher's
                                // backdrop renderer, plus a pending-resume
                                // copy so Settings can auto-reopen the editor
                                // with identical state when the user exits
                                // the preview.
                                com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview =
                                    com.bearinmind.launcher314.data.WallpaperPreviewBus.PreviewEntry(
                                        sourceBitmap = sourceBitmap,
                                        edit = snap,
                                        colorFilter = previewColorFilter
                                    )
                                com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit = snap
                                // Close editor + navigate away from Settings to
                                // the launcher's real home screen.
                                onRequestPreviewLauncher()
                                onDismiss()
                            }
                        }
                    )
                    // (Apply button removed — save happens from the
                    // launcher-preview's Apply pill after tapping Preview.)
                }

                if (saveDialogOpen) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { saveDialogOpen = false },
                        title = { Text("Save as wallpaper?") },
                        text = {
                            Column {
                                Text(
                                    "Applies the edited image as your Android wallpaper.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("Apply to:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = saveTarget == WallpaperManager.FLAG_SYSTEM,
                                        onClick = { saveTarget = WallpaperManager.FLAG_SYSTEM },
                                        label = { Text("Home") }
                                    )
                                    FilterChip(
                                        selected = saveTarget == WallpaperManager.FLAG_LOCK,
                                        onClick = { saveTarget = WallpaperManager.FLAG_LOCK },
                                        label = { Text("Lock") }
                                    )
                                    FilterChip(
                                        selected = saveTarget == (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
                                        onClick = { saveTarget = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK },
                                        label = { Text("Both") }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                saveDialogOpen = false
                                applyEdited(
                                    context = context,
                                    source = sourceBitmap ?: return@TextButton,
                                    edit = snapshotEdit(),
                                    target = saveTarget,
                                    scope = scope,
                                    setApplying = { isApplying = it },
                                    setStatus = { statusMessage = it },
                                    onSuccess = { onApplied(); onDismiss() }
                                )
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                saveDialogOpen = false
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (sourceBitmap != null) {
                    // Active editing category — read from the hoisted state so the
                    // pinned IconRow (sibling outside this Column) can stay in sync.
                    val activeCategory = activeCategoryShared
                    val isCropMode = currentSection == "crop"
                    // "Live" crop handles manipulated by the CropOverlay —
                    // always expressed in 0..1 fractional coords of the
                    // CURRENTLY displayed (already-committed-crop) bitmap.
                    // Tapping Save composes these values into the persisted
                    // source-space crop (cropL/T/R/B on DeviceWallpaperEdit)
                    // and resets the editable rect to full-edges so the user
                    // can keep cropping on the new, tighter image.
                    var editCropL by remember { mutableFloatStateOf(0f) }
                    var editCropT by remember { mutableFloatStateOf(0f) }
                    var editCropR by remember { mutableFloatStateOf(1f) }
                    var editCropB by remember { mutableFloatStateOf(1f) }
                    // Save-animation machinery. Sequential three-phase
                    // transition when the user taps Save in the crop section:
                    //   phase 1 (crop)    : CropOverlay handles fade out
                    //   phase 2 (enlarge) : image zooms into the pre-save
                    //                       editCrop region so it fills the
                    //                       preview; at the end of the
                    //                       zoom we commit the composed
                    //                       crop and swap to the new
                    //                       editorDisplayBitmap
                    //   phase 3 (return)  : CropOverlay re-appears at the
                    //                       new edges so the user can
                    //                       crop further
                    val saveAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                    var saveAnimPhase by remember { mutableIntStateOf(0) }
                    var preSaveEcL by remember { mutableFloatStateOf(0f) }
                    var preSaveEcT by remember { mutableFloatStateOf(0f) }
                    var preSaveEcR by remember { mutableFloatStateOf(1f) }
                    var preSaveEcB by remember { mutableFloatStateOf(1f) }
                    var previewSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
                    // When the save animation hits phase 3 we drive the
                    // preview-box aspect ratio from the old bitmap aspect to
                    // the new one over the same ~280 ms as the overlay
                    // fade-in, so the box border smoothly morphs instead of
                    // snapping to the new aspect when the bitmap swaps.
                    var aspectOverride by remember { mutableStateOf<Float?>(null) }
                    // Reset editable rect whenever the user re-enters the Crop
                    // section (so it doesn't retain stale mid-drag values
                    // from a previous editing pass).
                    LaunchedEffect(currentSection) {
                        if (currentSection == "crop") {
                            editCropL = 0f; editCropT = 0f
                            editCropR = 1f; editCropB = 1f
                        }
                    }

                    // Preview takes ALL remaining space after the intrinsic Controls
                    // and IconRow heights. Crop mode just shrinks Controls (no slider
                    // shown) which automatically grows Preview — no weight animation
                    // needed. Keeps the IconRow at a stable Y near the bottom.

                    // ---- Scaled phone-shaped preview ----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Preview uses the image's natural aspect ratio so the crop handles
                        // always sit on the real image bounds — crop is embedded, not a
                        // separate toggled mode. The crop overlay lives in a sibling Box so
                        // its outline draws ON TOP of the image's border instead of being
                        // clipped inside.
                        // When NOT in crop mode and the user has trimmed the
                        // image, show the cropped sub-bitmap at the preview's
                        // native aspect ratio — so saving the crop actually
                        // visually "commits" (the preview now displays only
                        // what will become the wallpaper). In crop mode the
                        // FULL source is used so handles can reach every edge.
                        val cropInset = cropL > 0.001f || cropT > 0.001f ||
                            cropR < 0.999f || cropB < 0.999f
                        // Preview always renders the already-committed crop
                        // sub-bitmap. The live-editing rectangle (editCrop
                        // state) draws on top via the CropOverlay handles.
                        val editorDisplayBitmap = remember(
                            sourceBitmap, cropL, cropT, cropR, cropB
                        ) {
                            if (!cropInset) sourceBitmap
                            else {
                                val l = (cropL * sourceBitmap.width).toInt().coerceIn(0, sourceBitmap.width - 1)
                                val t = (cropT * sourceBitmap.height).toInt().coerceIn(0, sourceBitmap.height - 1)
                                val r = (cropR * sourceBitmap.width).toInt().coerceIn(l + 1, sourceBitmap.width)
                                val b = (cropB * sourceBitmap.height).toInt().coerceIn(t + 1, sourceBitmap.height)
                                android.graphics.Bitmap.createBitmap(sourceBitmap, l, t, r - l, b - t)
                            }
                        }
                        val previewAspect = aspectOverride
                            ?: (editorDisplayBitmap.width.toFloat() / editorDisplayBitmap.height).toFloat()
                        // Force the preview to fill the available HEIGHT and then derive the
                        // narrower width from the image's aspect ratio — this keeps the box
                        // exactly the image's shape, no dark extension on the sides for tall
                        // portrait images.
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(previewAspect)
                                // Clip to the preview box so `BlurEffect`
                                // (which paints a few extra pixels past the
                                // layout bounds) can't bleed the blurred
                                // halo outside the framed area.
                                .clip(androidx.compose.ui.graphics.RectangleShape)
                                .border(1.dp, Color(0xFF555555)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { previewSizePx = it.size }
                                    // Shared zoom layer: during phase 2 of
                                    // the save animation, this graphicsLayer
                                    // scales the image + crop-overlay + dim
                                    // bands together. Pivot (0.5, 0.5) with
                                    // translation pans the pre-save editCrop
                                    // centre to the preview centre as scale
                                    // grows from 1 to 1/ecW · 1/ecH.
                                    .graphicsLayer {
                                        if (saveAnimPhase == 2) {
                                            val ecW = (preSaveEcR - preSaveEcL).coerceAtLeast(0.02f)
                                            val ecH = (preSaveEcB - preSaveEcT).coerceAtLeast(0.02f)
                                            val ecCx = (preSaveEcL + preSaveEcR) / 2f
                                            val ecCy = (preSaveEcT + preSaveEcB) / 2f
                                            // Uniform scale — no distortion.
                                            // The dimension that matches the
                                            // editCrop's longer side fills
                                            // the preview; the shorter side
                                            // letterboxes into the dim bands
                                            // (already editor-bg coloured),
                                            // so when the preview box later
                                            // morphs to the new aspect the
                                            // letterbox becomes outside-box
                                            // bg — seamless.
                                            val sT = 1f / kotlin.math.max(ecW, ecH)
                                            val p = saveAnim.value
                                            val s = 1f + p * (sT - 1f)
                                            scaleX = s
                                            scaleY = s
                                            translationX = p * -(ecCx - 0.5f) * previewSizePx.width * sT
                                            translationY = p * -(ecCy - 0.5f) * previewSizePx.height * sT
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            val blurMod = if (blur > 0 && Build.VERSION.SDK_INT >= 31) {
                                Modifier.graphicsLayer {
                                    renderEffect = BlurEffect(
                                        radiusX = with(density) { (blur / 100f * 25f).dp.toPx() },
                                        radiusY = with(density) { (blur / 100f * 25f).dp.toPx() },
                                        edgeTreatment = TileMode.Clamp
                                    )
                                }
                            } else Modifier

                            // Full image rendered with FillBounds so it fills the preview box
                            // exactly (the box's aspect ratio already matches the image's, so
                            // no distortion). FillBounds avoids the tiny rounding-letterbox
                            // that ContentScale.Fit produced — the crop outline now starts
                            // flush with the image edges.
                            // Image holds ONLY the user's rotate / scale /
                            // flip / translate. The save-animation zoom
                            // lives on the parent Box (above) so the crop
                            // overlay + dim bands zoom in lockstep with the
                            // image, otherwise the outline would detach
                            // from the image during the enlarge phase.
                            Image(
                                bitmap = editorDisplayBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                colorFilter = previewColorFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = rotation.toFloat()
                                        scaleX = scale * (if (flipH) -1f else 1f)
                                        scaleY = scale * (if (flipV) -1f else 1f)
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                                    .then(blurMod)
                            )

                            // Crop overlay — only visible when the Crop category is the
                            // active editing mode. Other modes (Brightness, Contrast, etc.)
                            // hide the outline/handles so the user sees the clean image.
                            // CropOverlay now manipulates the LIVE edit
                            // rectangle (editCrop), which is 0..1 on the
                            // already-cropped displayed sub-bitmap. Save
                            // composes it into cropL/T/R/B.
                            if (isCropMode) {
                                // Per-phase overlay look:
                                //  phase 0 (idle)    : dim 0.55, outline 1.0
                                //  phase 1 (crop)    : dim 0.55 → 1.0 (the
                                //                      uncropped area fades
                                //                      to fully black) while
                                //                      outline stays visible
                                //  phase 2 (enlarge) : dim 1.0, outline 1.0
                                //                      (grey outline rides
                                //                      the zoom with the
                                //                      image)
                                //  phase 3 (return)  : dim back to 0.55,
                                //                      outline fades in from
                                //                      0 as handles return
                                //                      at the new edges
                                val dimDefault = Color.Black.copy(alpha = 0.55f)
                                val editorBg = Color(0xFF121212)
                                val dimColorNow = when (saveAnimPhase) {
                                    1 -> androidx.compose.ui.graphics.lerp(
                                        dimDefault, editorBg, saveAnim.value
                                    )
                                    2 -> editorBg
                                    3 -> dimDefault
                                    else -> dimDefault
                                }
                                val outlineNow = when (saveAnimPhase) {
                                    1 -> 1f       // stays visible during crop fade
                                    2 -> 1f       // stays visible during enlarge
                                    3 -> saveAnim.value
                                    else -> 1f
                                }
                                CropOverlay(
                                    cropL = editCropL, cropT = editCropT,
                                    cropR = editCropR, cropB = editCropB,
                                    onCropChange = { l, t, r, b ->
                                        editCropL = l.coerceIn(0f, 1f)
                                        editCropT = t.coerceIn(0f, 1f)
                                        editCropR = r.coerceIn(0f, 1f)
                                        editCropB = b.coerceIn(0f, 1f)
                                    },
                                    onCommit = { /* no-op; Save button commits */ },
                                    dimColor = dimColorNow,
                                    outlineAlpha = outlineNow
                                )
                            }

                            // Crop-mode "Save" button — same OutlinedButton
                            // style as the top-bar Reset / Preview / Apply
                            // (12 dp rounded corners, 1 dp #888 border, 16 dp
                            // horizontal / 8 dp vertical content padding,
                            // labelLarge text). Semi-opaque black fill so it
                            // stays readable over any wallpaper image.
                            if (isCropMode && saveAnimPhase == 0) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            // Snapshot pre-save edit rectangle
                                            // so the zoom phase knows where to
                                            // zoom into.
                                            preSaveEcL = editCropL
                                            preSaveEcT = editCropT
                                            preSaveEcR = editCropR
                                            preSaveEcB = editCropB
                                            // Phase 1 — CROP: dim bands fade
                                            // toward full black (uncropped
                                            // portion disappears). ~400 ms.
                                            saveAnimPhase = 1
                                            saveAnim.snapTo(0f)
                                            saveAnim.animateTo(
                                                1f,
                                                androidx.compose.animation.core.tween(
                                                    durationMillis = 400,
                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                )
                                            )
                                            // Phase 2 — ENLARGE: image + grey
                                            // outline zoom together to match
                                            // the new bounds. ~600 ms.
                                            saveAnimPhase = 2
                                            saveAnim.snapTo(0f)
                                            saveAnim.animateTo(
                                                1f,
                                                androidx.compose.animation.core.tween(
                                                    durationMillis = 600,
                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                )
                                            )
                                            // Commit: compose editCrop into
                                            // cropL/T/R/B (source-space) and
                                            // reset editCrop to edges of the
                                            // new sub-bitmap.
                                            val w = cropR - cropL
                                            val h = cropB - cropT
                                            val newL = (cropL + preSaveEcL * w).coerceIn(0f, 1f)
                                            val newR = (cropL + preSaveEcR * w).coerceIn(newL + 0.01f, 1f)
                                            val newT = (cropT + preSaveEcT * h).coerceIn(0f, 1f)
                                            val newB = (cropT + preSaveEcB * h).coerceIn(newT + 0.01f, 1f)
                                            cropL = newL; cropR = newR
                                            cropT = newT; cropB = newB
                                            editCropL = 0f; editCropT = 0f
                                            editCropR = 1f; editCropB = 1f
                                            commitEdit()
                                            // Phase 3 — RETURN: outline fades
                                            // back in at the new edges AND
                                            // the preview-box aspect morphs
                                            // from the pre-save aspect to the
                                            // post-commit aspect so the box
                                            // border glides into place
                                            // instead of snapping. ~280 ms.
                                            val preAspect = sourceBitmap.width.toFloat() *
                                                (w) / (sourceBitmap.height * (h))
                                            val postAspect = sourceBitmap.width.toFloat() *
                                                (newR - newL) / (sourceBitmap.height * (newB - newT))
                                            aspectOverride = preAspect
                                            saveAnimPhase = 3
                                            saveAnim.snapTo(0f)
                                            launch {
                                                androidx.compose.animation.core.animate(
                                                    initialValue = preAspect,
                                                    targetValue = postAspect,
                                                    animationSpec = androidx.compose.animation.core.tween(
                                                        durationMillis = 280,
                                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                    )
                                                ) { value, _ -> aspectOverride = value }
                                            }
                                            saveAnim.animateTo(
                                                1f,
                                                androidx.compose.animation.core.tween(
                                                    durationMillis = 280,
                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                )
                                            )
                                            aspectOverride = null
                                            saveAnimPhase = 0
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF888888)),
                                    colors = androidx.compose.material3.ButtonDefaults
                                        .outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                ) {
                                    Text(
                                        "Save",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            // Launcher dim preview overlay — mirrors the
                            // black-alpha overlay that LauncherWithDrawer
                            // applies on top of the wallpaper at render time
                            // (see `wallpaperDimPercent` there). Drawn here
                            // so dragging the Dim slider gives immediate
                            // feedback inside the editor preview.
                            if (dimEffect > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = (dimEffect / 100f).coerceIn(0f, 1f)))
                                )
                            }

                            // Vignette preview overlay
                            if (vignette > 0) {
                                val alphaMax = (vignette / 100f * 0.85f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = alphaMax * 0.4f),
                                                    Color.Black.copy(alpha = alphaMax)
                                                )
                                            )
                                        )
                                )
                            }

                            // (floating save button removed — save now lives in the top action bar)

                            // Floating transform row — only shown in Crop mode. Uses
                            // AnimatedVisibility(fadeIn/fadeOut) so the whole group
                            // smoothly appears/disappears when switching in/out of
                            // crop mode. All four icons live in ONE rounded-box
                            // container (not four separate boxes) matching the top
                            // bar's 12dp rounded-rect style.
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isCropMode,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 220)
                                ),
                                exit = androidx.compose.animation.fadeOut(
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180)
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF888888),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FloatingTransformIconPainter(
                                        painter = androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_rotate_left_spectrum),
                                        contentDesc = "Rotate left"
                                    ) { rotation = ((rotation - 90) + 360) % 360; commitEdit() }
                                    FloatingTransformIconPainter(
                                        painter = androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_rotate_right_spectrum),
                                        contentDesc = "Rotate right"
                                    ) { rotation = (rotation + 90) % 360; commitEdit() }
                                    FloatingTransformIcon(
                                        icon = Icons.Outlined.SwapHoriz,
                                        contentDesc = "Flip horizontal",
                                        selected = flipH
                                    ) { flipH = !flipH; commitEdit() }
                                    FloatingTransformIcon(
                                        icon = Icons.Outlined.SwapVert,
                                        contentDesc = "Flip vertical",
                                        selected = flipV
                                    ) { flipV = !flipV; commitEdit() }
                                }
                            }
                        }
                        } // end inner box
                    }

                    // ---- Controls — category icon picker + active control ----
                    // One category is selected at a time (Samsung-Photos-style). The picked
                    // category's control (slider or chips) is shown above its icon row. Crop
                    // mode shows no popup control — only the (now larger) preview + outline.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            // Animate the Column's height when swapping between crop
                            // mode (no slider → collapses) and other modes (slider +
                            // label → ~108dp). That lets the Preview above (weight 1f)
                            // grow / shrink smoothly instead of snapping.
                            .animateContentSize(
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 300,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // (removed top Spacer so the label + slider sit as close
                        // as possible to the IconRow beneath the Crossfade block.)

                        // Crossfade smoothly transitions the slider between effects when
                        // the active one changes. Crop and Filters sections render an
                        // empty placeholder (no scalar to drive) so the surrounding
                        // layout collapses cleanly via animateContentSize.
                        androidx.compose.animation.Crossfade(
                            targetState = if (currentSection == "effects") activeCategory else "__nonslider__",
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 220,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            label = "categoryControl"
                        ) { cat ->
                            if (cat == "__nonslider__" || cat == "crop") {
                                Spacer(Modifier.height(0.dp))
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    // Slider only — the category name lives
                                    // under each icon in the pager below so
                                    // users can see every category's name at
                                    // once, not just the selected one.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 28.dp)
                                            .height(64.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        // All -50..+50 effects reuse wallpaperBrightness (bipolar).
                                        // Sharpness is 0..100 (wallpaperPercent). Saturation stays
                                        // on its -100..+100 config.
                                        when (cat) {
                                            "brightness" -> ThumbDragHorizontalSlider(
                                                currentValue = brightness.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { brightness = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { brightness = 0; commitEdit() }
                                            )
                                            "exposure" -> ThumbDragHorizontalSlider(
                                                currentValue = exposure.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { exposure = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { exposure = 0; commitEdit() }
                                            )
                                            "contrast" -> ThumbDragHorizontalSlider(
                                                currentValue = contrast.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { contrast = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { contrast = 0; commitEdit() }
                                            )
                                            "highlights" -> ThumbDragHorizontalSlider(
                                                currentValue = highlights.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { highlights = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { highlights = 0; commitEdit() }
                                            )
                                            "shadows" -> ThumbDragHorizontalSlider(
                                                currentValue = shadows.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { shadows = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { shadows = 0; commitEdit() }
                                            )
                                            "saturation" -> ThumbDragHorizontalSlider(
                                                currentValue = saturation.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { saturation = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { saturation = 0; commitEdit() }
                                            )
                                            "tint" -> ThumbDragHorizontalSlider(
                                                currentValue = tint.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { tint = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { tint = 0; commitEdit() }
                                            )
                                            "temperature" -> ThumbDragHorizontalSlider(
                                                currentValue = temperature.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { temperature = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { temperature = 0; commitEdit() }
                                            )
                                            "sharpness" -> ThumbDragHorizontalSlider(
                                                currentValue = sharpness.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { sharpness = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { sharpness = 0; commitEdit() }
                                            )
                                            "blur" -> ThumbDragHorizontalSlider(
                                                currentValue = blur.toFloat(),
                                                config = SliderConfigs.wallpaperPercent,
                                                onValueChange = { blur = it.toInt() },
                                                onValueChangeFinished = { commitEdit() },
                                                onDoubleTap = { blur = 0; commitEdit() }
                                            )
                                            "dim" -> ThumbDragHorizontalSlider(
                                                currentValue = dimEffect.toFloat(),
                                                config = SliderConfigs.wallpaperPercent,
                                                onValueChange = {
                                                    dimEffect = it.toInt()
                                                    setWallpaperDim(context, dimEffect)
                                                },
                                                onValueChangeFinished = {
                                                    setWallpaperDim(context, dimEffect)
                                                },
                                                onDoubleTap = {
                                                    dimEffect = 0
                                                    setWallpaperDim(context, 0)
                                                }
                                            )
                                        }
                                    }
                                    // (label is no longer shown here — each
                                    // icon in the pager below carries its own
                                    // name underneath it.)
                                }
                            }
                        }

                        statusMessage?.let {
                            Text(it, color = Color(0xFFFF6B6B))
                        }
                    }

                    // Category picker — HorizontalPager. User swipes left/right to
                    // slide through categories; centered page is the active one,
                    // scales up and full-alpha while neighbors shrink + fade.
                    // Tapping a neighbor animates to center on that page.
                    //
                    // Contents depend on `currentSection`: Effects = 9 scalar
                    // sliders, Filters = 4 preset filters, Crop = 5 transform
                    // actions. Crop's floating toolbar on the preview is still
                    // there too (rotate / flip redundancy is fine).
                    val categoryKeys = remember(currentSection) {
                        when (currentSection) {
                            "crop" -> listOf("rotate_left", "rotate_right", "flip_h", "flip_v", "reset_crop")
                            "filters" -> listOf(
                                "filter_none", "filter_mono", "filter_sepia",
                                "filter_warm", "filter_cool", "filter_vivid", "filter_faded",
                                "filter_polaroid", "filter_kodachrome", "filter_vintage",
                                "filter_teal_orange", "filter_night"
                            )
                            else -> listOf(
                                "brightness", "exposure", "contrast",
                                "highlights", "shadows", "saturation", "tint", "temperature",
                                "sharpness", "blur", "dim"
                            )
                        }
                    }
                    // Cropped source bitmap used by the Filters-section
                    // thumbnails. Recomputes only when the crop rectangle
                    // changes, not per-recomposition. Samsung-Photos-style:
                    // each filter icon shows the actual image with that
                    // filter applied.
                    val filterThumbnailSrc = remember(sourceBitmap, cropL, cropT, cropR, cropB) {
                        val inset = cropL > 0.001f || cropT > 0.001f ||
                            cropR < 0.999f || cropB < 0.999f
                        if (inset) {
                            val l = (cropL * sourceBitmap.width).toInt().coerceIn(0, sourceBitmap.width - 1)
                            val t = (cropT * sourceBitmap.height).toInt().coerceIn(0, sourceBitmap.height - 1)
                            val r = (cropR * sourceBitmap.width).toInt().coerceIn(l + 1, sourceBitmap.width)
                            val b = (cropB * sourceBitmap.height).toInt().coerceIn(t + 1, sourceBitmap.height)
                            android.graphics.Bitmap.createBitmap(sourceBitmap, l, t, r - l, b - t)
                        } else sourceBitmap
                    }
                    // ColorMatrix objects for every preset filter thumbnail.
                    // Remember once; all entries resolve through the shared
                    // WallpaperFilters catalog so preview + bake match exactly.
                    val filterThumbnailMatrices = remember {
                        mapOf(
                            "filter_mono" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.MONO),
                            "filter_sepia" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.SEPIA),
                            "filter_warm" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.WARM),
                            "filter_cool" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.COOL),
                            "filter_vivid" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.VIVID),
                            "filter_faded" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.FADED),
                            "filter_polaroid" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.POLAROID),
                            "filter_kodachrome" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.KODACHROME),
                            "filter_vintage" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.VINTAGE),
                            "filter_teal_orange" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.TEAL_ORANGE),
                            "filter_night" to androidx.compose.ui.graphics.ColorMatrix(com.bearinmind.launcher314.helpers.WallpaperFilters.NIGHT)
                        )
                    }
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = categoryKeys.indexOf(activeCategoryShared).coerceAtLeast(0),
                        pageCount = { categoryKeys.size }
                    )
                    // When the user taps a different section tab, reset the
                    // pager to page 0 so the first item of the new section is
                    // centered.
                    LaunchedEffect(currentSection) {
                        pagerState.scrollToPage(0)
                    }
                    val pickerHaptics = com.bearinmind.launcher314.helpers.rememberHapticFeedback()
                    // `visualPage` = whichever icon has the smallest |offset|
                    // from center (i.e. the biggest / currently-outlined one).
                    // Updated instantly as the user drags, even through
                    // offset-0.5 flips, so the outline/scale/label ALWAYS
                    // track the visually-dominant icon.
                    // Coerce via the LIVE pager count, not `categoryKeys.size`
                    // captured at first composition. Without this, swipes past
                    // index-8 in a 12-entry section (Filters) clamped back to
                    // 8 because the original derivedStateOf closed over the
                    // Effects-section size (9).
                    val visualPage by remember {
                        derivedStateOf {
                            val maxIdx = (pagerState.pageCount - 1).coerceAtLeast(0)
                            kotlin.math.round(
                                pagerState.currentPage + pagerState.currentPageOffsetFraction
                            ).toInt().coerceIn(0, maxIdx)
                        }
                    }
                    // Haptic tick + slider sync fire when visualPage changes.
                    // Skip the first composition so opening the editor doesn't
                    // vibrate. activeCategoryShared only updates while the
                    // Effects section is active (that's the only section where
                    // the centered icon drives which slider is shown).
                    var hapticPrimed by remember { mutableStateOf(false) }
                    LaunchedEffect(visualPage, currentSection) {
                        if (hapticPrimed) {
                            pickerHaptics.performTextHandleMove()
                        } else {
                            hapticPrimed = true
                        }
                        if (visualPage >= categoryKeys.size) return@LaunchedEffect
                        when (currentSection) {
                            "effects" -> activeCategoryShared = categoryKeys[visualPage]
                            "filters" -> {
                                // Live filter preview: update `filter` immediately as the
                                // pager centers a new thumbnail, so the big preview image
                                // above reflects the new look without needing a tap.
                                filter = filterKeyToString(categoryKeys[visualPage])
                            }
                        }
                    }
                    // Debounced commit: swiping through several filters creates
                    // one undo-history entry (the final one), not one per filter.
                    LaunchedEffect(filter) {
                        kotlinx.coroutines.delay(350)
                        commitEdit()
                    }
                    // (No post-settle correction.) The pager's own fling
                    // animation lands the page exactly once; any second
                    // animation on top of that reads as a visible "jump back"
                    // after the user's eye has already committed to the
                    // landing icon. Trade-off: nudges may occasionally snap
                    // one icon past where the finger was — acceptable because
                    // the single-animation path is perceptually smoother than
                    // any correction scheme that requires a second pass.
                    // Hide the pager in Crop mode — the rotate / flip / reset
                    // controls already live on the preview's floating toolbar,
                    // so duplicating them as pager icons is redundant. The
                    // AnimatedVisibility collapses the height smoothly instead
                    // of the pager snapping away, matching the feel of the old
                    // slider-collapse when Crop was just an Effects entry.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentSection != "crop",
                        enter = androidx.compose.animation.expandVertically(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 280,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
                        ),
                        exit = androidx.compose.animation.shrinkVertically(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 280,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180)
                        )
                    ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pinnedIconRowHeight)
                    ) {
                        // Each cell slot = maxWidth / 5 so that 5 cells (center +
                        // 2 on each side) span the FULL width of the container —
                        // same visible count as before but distributed edge to
                        // edge instead of bunched into a narrow center strip.
                        // The icon inside each cell stays at its intrinsic 56dp
                        // and is centered by the Column's CenterHorizontally.
                        val itemWidth = this.maxWidth / 5
                        val sidePadding = itemWidth * 2
                        // Let release velocity carry the pager through however
                        // many pages Compose's spline predicts, capped at the
                        // full category count so the HARDEST flick goes from
                        // crop (0) → sharpness (9) or vice-versa in a single
                        // fling. Gentle drag → ±1, medium flick → a handful,
                        // max swipe → full 11-page traversal. One animation,
                        // no second-pass "jump back."
                        val maxPageDistance = categoryKeys.size - 1
                        val velocitySnap = remember(maxPageDistance) {
                            object : androidx.compose.foundation.pager.PagerSnapDistance {
                                override fun calculateTargetPage(
                                    startPage: Int,
                                    suggestedTargetPage: Int,
                                    velocity: Float,
                                    pageSize: Int,
                                    pageSpacing: Int
                                ): Int = suggestedTargetPage.coerceIn(
                                    startPage - maxPageDistance,
                                    startPage + maxPageDistance
                                )
                            }
                        }
                        // Smooth settle: tween with FastOutSlowInEasing gives
                        // a fluid deceleration at the end of a fling or the
                        // short animation after a slow drag. 400ms is long
                        // enough to feel unhurried but snappy — the decay
                        // portion (handled internally by Compose's spline) is
                        // untouched, so fast flings still decelerate in their
                        // own natural physics curve before this easing kicks
                        // in for the final lock-on.
                        val flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = velocitySnap,
                            snapPositionalThreshold = 0.5f,
                            snapAnimationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 400,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(itemWidth),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sidePadding),
                            pageSpacing = 0.dp,
                            flingBehavior = flingBehavior,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val key = categoryKeys[page]
                            // Signed distance of this page from the centered one
                            // — used to scale + fade neighbors.
                            val pageOffset = kotlin.math.abs(
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).coerceIn(0f, 1f)
                            // Slightly smaller than before: center 1.15, edge 0.85
                            // (was 1.25 / 0.85). Gives effect icons a bit more
                            // breathing room.
                            val iconScale = 1.15f - pageOffset * 0.30f
                            val iconAlpha = 1f - pageOffset * 0.45f
                            val isSelected = page == visualPage
                            val activated = when (currentSection) {
                                "filters" -> filter == filterKeyToString(key)
                                "crop" -> when (key) {
                                    // Flip toggles highlight when flipped; rotate / reset are actions only.
                                    "flip_h" -> flipH
                                    "flip_v" -> flipV
                                    else -> false
                                }
                                else -> when (key) {
                                    "brightness" -> brightness != 0
                                    "exposure" -> exposure != 0
                                    "contrast" -> contrast != 0
                                    "highlights" -> highlights != 0
                                    "shadows" -> shadows != 0
                                    "saturation" -> saturation != 0
                                    "tint" -> tint != 0
                                    "temperature" -> temperature != 0
                                    "sharpness" -> sharpness != 0
                                    "blur" -> blur != 0
                                    "dim" -> dimEffect != 0
                                    else -> false
                                }
                            }
                            val onTap: () -> Unit = when (currentSection) {
                                "crop" -> ({
                                    when (key) {
                                        "rotate_left" -> rotation = ((rotation - 90) + 360) % 360
                                        "rotate_right" -> rotation = (rotation + 90) % 360
                                        "flip_h" -> flipH = !flipH
                                        "flip_v" -> flipV = !flipV
                                        "reset_crop" -> {
                                            cropL = 0f; cropT = 0f; cropR = 1f; cropB = 1f
                                            rotation = 0; flipH = false; flipV = false
                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                        }
                                    }
                                    commitEdit()
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                })
                                "filters" -> ({
                                    filter = filterKeyToString(key)
                                    commitEdit()
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                })
                                else -> ({ scope.launch { pagerState.animateScrollToPage(page) } })
                            }
                            // Double-tap: effects reset that effect; filters
                            // reset to None; crop icons have no double-tap
                            // action (single-tap already applies).
                            val onDoubleTap: () -> Unit = when (currentSection) {
                                "filters" -> ({ filter = WP_FILTER_NONE; commitEdit() })
                                "crop" -> ({}) // no-op; single-tap already performs the action
                                else -> ({
                                    when (key) {
                                        "brightness" -> brightness = 0
                                        "exposure" -> exposure = 0
                                        "contrast" -> contrast = 0
                                        "highlights" -> highlights = 0
                                        "shadows" -> shadows = 0
                                        "saturation" -> saturation = 0
                                        "tint" -> tint = 0
                                        "temperature" -> temperature = 0
                                        "sharpness" -> sharpness = 0
                                        "blur" -> blur = 0
                                        "dim" -> {
                                            dimEffect = 0
                                            setWallpaperDim(context, 0)
                                        }
                                    }
                                    commitEdit()
                                })
                            }
                            // Value indicator (shown inside the outlined selected
                            // icon for effects). Filters and crop have no scalar
                            // to show.
                            val displayValue = if (currentSection == "effects") when (key) {
                                "brightness" -> brightness
                                "exposure" -> exposure
                                "contrast" -> contrast
                                "highlights" -> highlights
                                "shadows" -> shadows
                                "saturation" -> saturation
                                "tint" -> tint
                                "temperature" -> temperature
                                "sharpness" -> sharpness
                                "blur" -> blur
                                "dim" -> dimEffect
                                else -> 0
                            } else 0
                            val categoryName = when (key) {
                                // effects
                                "brightness" -> "Brightness"
                                "exposure" -> "Exposure"
                                "contrast" -> "Contrast"
                                "highlights" -> "Highlights"
                                "shadows" -> "Shadows"
                                "saturation" -> "Saturation"
                                "tint" -> "Tint"
                                "temperature" -> "Temperature"
                                "sharpness" -> "Sharpness"
                                "blur" -> "Blur"
                                "dim" -> "Dim"
                                // filters
                                "filter_none" -> "Original"
                                "filter_mono" -> "Mono"
                                "filter_sepia" -> "Sepia"
                                "filter_warm" -> "Warm"
                                "filter_cool" -> "Cool"
                                "filter_vivid" -> "Vivid"
                                "filter_faded" -> "Faded"
                                "filter_polaroid" -> "Polaroid"
                                "filter_kodachrome" -> "Kodachrome"
                                "filter_vintage" -> "Vintage"
                                "filter_teal_orange" -> "Teal & Orange"
                                "filter_night" -> "Night"
                                // crop
                                "rotate_left" -> "Rotate left"
                                "rotate_right" -> "Rotate right"
                                "flip_h" -> "Flip H"
                                "flip_v" -> "Flip V"
                                "reset_crop" -> "Reset"
                                else -> ""
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        alpha = iconAlpha
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                            ) {
                                when (key) {
                                    // --- effects ---
                                    "brightness" -> CategoryIcon(Icons.Outlined.LightMode, "Brightness", isSelected, activated, displayValue, onTap, onDoubleTap)
                                    "exposure" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_contrast_square_spectrum),
                                        "Exposure", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "contrast" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_contrast_spectrum),
                                        "Contrast", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "highlights" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_tonality_2_spectrum),
                                        "Highlights", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "shadows" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_tonality_spectrum),
                                        "Shadows", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "saturation" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_saturation_levels),
                                        "Saturation", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "tint" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_opacity_spectrum),
                                        "Tint", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "temperature" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_thermometer_spectrum),
                                        "Temperature", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "sharpness" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_triangle_rounded_tonality),
                                        "Sharpness", isSelected, activated, displayValue, onTap, onDoubleTap
                                    )
                                    "blur" -> CategoryIcon(Icons.Outlined.BlurOn, "Blur", isSelected, activated, displayValue, onTap, onDoubleTap)
                                    "dim" -> CategoryIcon(Icons.Outlined.Brightness4, "Dim", isSelected, activated, displayValue, onTap, onDoubleTap)
                                    // --- filters (live thumbnails of the cropped source with each filter applied) ---
                                    "filter_none",
                                    "filter_mono",
                                    "filter_sepia",
                                    "filter_warm",
                                    "filter_cool",
                                    "filter_vivid",
                                    "filter_faded",
                                    "filter_polaroid",
                                    "filter_kodachrome",
                                    "filter_vintage",
                                    "filter_teal_orange",
                                    "filter_night" -> FilterThumbnail(
                                        thumbnail = filterThumbnailSrc,
                                        filterMatrix = filterThumbnailMatrices[key], // null for "filter_none"
                                        selected = isSelected,
                                        activated = activated,
                                        contentDesc = categoryName,
                                        onClick = onTap,
                                        onDoubleClick = onDoubleTap
                                    )
                                    // --- crop actions ---
                                    "rotate_left" -> CategoryIcon(Icons.Outlined.Rotate90DegreesCcw, "Rotate left", isSelected, activated, 0, onTap, null)
                                    "rotate_right" -> CategoryIcon(Icons.Outlined.Rotate90DegreesCw, "Rotate right", isSelected, activated, 0, onTap, null)
                                    "flip_h" -> CategoryIcon(Icons.Outlined.SwapHoriz, "Flip H", isSelected, activated, 0, onTap, null)
                                    "flip_v" -> CategoryIcon(Icons.Outlined.SwapVert, "Flip V", isSelected, activated, 0, onTap, null)
                                    "reset_crop" -> CategoryIcon(Icons.Outlined.RestartAlt, "Reset", isSelected, activated, 0, onTap, null)
                                }
                                Text(
                                    text = categoryName,
                                    fontSize = 8.sp,
                                    color = Color(0xFFAAAAAA),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    }

                    // Permanent section picker — Crop / Filters / Effects.
                    // Sits below the pager and switches what the pager + slider
                    // display. Tapping a tab resets the pager to page 0.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "crop" to "Crop",
                            "filters" to "Filters",
                            "effects" to "Effects"
                        ).forEach { (sectionKey, sectionLabel) ->
                            val sectionSelected = currentSection == sectionKey
                            // Only the background fill changes with selection;
                            // the outline and text stay at a constant intensity
                            // so the selected tab doesn't visually "brighten."
                            val bg by androidx.compose.animation.animateColorAsState(
                                targetValue = if (sectionSelected) Color(0xFF2E2E2E) else Color.Transparent,
                                animationSpec = androidx.compose.animation.core.tween(200),
                                label = "sectionBg"
                            )
                            val sectionShape = RoundedCornerShape(8.dp)
                            // Each tab gets exactly 1/3 of the row via weight(1f)
                            // so "Filters" sits at the row's geometric center
                            // regardless of label width, and "Crop" / "Effects"
                            // land symmetrically on either side. The inner tab
                            // box is centered within its slot so label widths
                            // never skew the group's balance.
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(sectionShape)
                                        .background(bg)
                                        .border(width = 1.dp, color = Color(0xFF888888), shape = sectionShape)
                                        .clickable { currentSection = sectionKey }
                                        .padding(horizontal = 18.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sectionLabel,
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 13.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isApplying) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

        }
    }
}

/**
 * Crop overlay — dim area outside crop rect, widget-resize-style dashed outline
 * (rounded corner arcs + straight dashed edges), rule-of-thirds grid.
 *
 * Draggable: all four EDGES and all four CORNERS — matches the widget-resize
 * gesture model. Edges drag one axis; corners drag both. The outline itself IS
 * the visual — no separate handle boxes. The active edge/corner highlights
 * WHITE while dragging (again matching widget-resize).
 */
@Composable
private fun BoxScope.CropOverlay(
    cropL: Float, cropT: Float, cropR: Float, cropB: Float,
    onCropChange: (Float, Float, Float, Float) -> Unit,
    onCommit: () -> Unit = {},
    /** Final colour (alpha included) for the four bands OUTSIDE the crop
     *  rectangle. Default is the standard translucent dark dim; the editor's
     *  save-animation fades this to the editor background colour during
     *  phase 1 so the uncropped area blends seamlessly with the surrounding
     *  chrome before the image zooms. */
    dimColor: Color = Color.Black.copy(alpha = 0.55f),
    /** Alpha on the Canvas that draws the dashed outline, corner L-markers
     *  and rule-of-thirds grid. Stays 1.0 normally so the outline is visible
     *  during the zoom phase ("enlarges the image + grey outline"), but the
     *  editor drops it during phase 3 to bring handles back in smoothly. */
    outlineAlpha: Float = 1f
) {
    val density = LocalDensity.current
    var boxWidthPx by remember { mutableFloatStateOf(1f) }
    var boxHeightPx by remember { mutableFloatStateOf(1f) }
    var activeHandle by remember { mutableStateOf<String?>(null) }

    // rememberUpdatedState so pointer-input lambdas (captured once with key=Unit)
    // always read the CURRENT crop/box values — without this the handles appear
    // to "stick" because the lambda uses stale leftPx/topPx etc.
    val latestL by rememberUpdatedState(cropL)
    val latestT by rememberUpdatedState(cropT)
    val latestR by rememberUpdatedState(cropR)
    val latestB by rememberUpdatedState(cropB)
    val latestW by rememberUpdatedState(boxWidthPx)
    val latestH by rememberUpdatedState(boxHeightPx)
    val onChange by rememberUpdatedState(onCropChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                boxWidthPx = it.size.width.toFloat().coerceAtLeast(1f)
                boxHeightPx = it.size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        val leftPx = cropL * boxWidthPx
        val topPx = cropT * boxHeightPx
        val rightPx = cropR * boxWidthPx
        val bottomPx = cropB * boxHeightPx
        val dim = dimColor

        // Dim bands outside the crop rect
        Box(modifier = Modifier.offset { IntOffset(0, 0) }.size(
            width = with(density) { boxWidthPx.toDp() },
            height = with(density) { topPx.toDp() }).background(dim))
        Box(modifier = Modifier.offset { IntOffset(0, bottomPx.toInt()) }.size(
            width = with(density) { boxWidthPx.toDp() },
            height = with(density) { (boxHeightPx - bottomPx).toDp() }).background(dim))
        Box(modifier = Modifier.offset { IntOffset(0, topPx.toInt()) }.size(
            width = with(density) { leftPx.toDp() },
            height = with(density) { (bottomPx - topPx).toDp() }).background(dim))
        Box(modifier = Modifier.offset { IntOffset(rightPx.toInt(), topPx.toInt()) }.size(
            width = with(density) { (boxWidthPx - rightPx).toDp() },
            height = with(density) { (bottomPx - topPx).toDp() }).background(dim))

        // Outline — faithful copy of WidgetResize.kt's visual language.
        val topActive = activeHandle == "top" || activeHandle == "topLeft" || activeHandle == "topRight"
        val bottomActive = activeHandle == "bottom" || activeHandle == "bottomLeft" || activeHandle == "bottomRight"
        val leftActive = activeHandle == "left" || activeHandle == "topLeft" || activeHandle == "bottomLeft"
        val rightActive = activeHandle == "right" || activeHandle == "topRight" || activeHandle == "bottomRight"

        Canvas(
            modifier = Modifier
                .offset { IntOffset(leftPx.toInt(), topPx.toInt()) }
                .size(
                    width = with(density) { (rightPx - leftPx).toDp() },
                    height = with(density) { (bottomPx - topPx).toDp() }
                )
                .graphicsLayer { this.alpha = outlineAlpha.coerceIn(0f, 1f) }
        ) {
            val strokePx = with(density) { 2.dp.toPx() }
            val cornerLenPx = with(density) { 18.dp.toPx() }  // length of solid corner marker
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val grey = Color(0xFF666666)
            val white = Color.White
            val offset = strokePx / 2
            val w = size.width; val h = size.height

            // SHARP, dashed rectangle edges (no rounded corners). Colour turns white
            // while that edge is actively being dragged.
            drawLine(if (topActive) white else grey,
                GeoOffset(0f, offset), GeoOffset(w, offset),
                strokeWidth = strokePx, pathEffect = dash)
            drawLine(if (rightActive) white else grey,
                GeoOffset(w - offset, 0f), GeoOffset(w - offset, h),
                strokeWidth = strokePx, pathEffect = dash)
            drawLine(if (bottomActive) white else grey,
                GeoOffset(0f, h - offset), GeoOffset(w, h - offset),
                strokeWidth = strokePx, pathEffect = dash)
            drawLine(if (leftActive) white else grey,
                GeoOffset(offset, 0f), GeoOffset(offset, h),
                strokeWidth = strokePx, pathEffect = dash)

            // SOLID corner markers — short L-shapes at each corner, drawn thicker to
            // look like crisp "lined corners". Highlighted white when either adjacent
            // edge is active. cornerOff insets by HALF the thicker stroke so the
            // whole marker stays inside the Canvas bounds (previously the extra
            // thickness bled outside by ~2px — visible as the crop sitting outside
            // the image).
            val cornerStrokePx = strokePx * 1.6f
            val cornerOff = cornerStrokePx / 2f
            fun cornerColor(a: Boolean, b: Boolean) = if (a || b) white else grey

            // Top-left L
            val tl = cornerColor(leftActive, topActive)
            drawLine(tl, GeoOffset(0f, cornerOff), GeoOffset(cornerLenPx, cornerOff), strokeWidth = cornerStrokePx)
            drawLine(tl, GeoOffset(cornerOff, 0f), GeoOffset(cornerOff, cornerLenPx), strokeWidth = cornerStrokePx)
            // Top-right L
            val tr = cornerColor(topActive, rightActive)
            drawLine(tr, GeoOffset(w - cornerLenPx, cornerOff), GeoOffset(w, cornerOff), strokeWidth = cornerStrokePx)
            drawLine(tr, GeoOffset(w - cornerOff, 0f), GeoOffset(w - cornerOff, cornerLenPx), strokeWidth = cornerStrokePx)
            // Bottom-right L
            val br = cornerColor(rightActive, bottomActive)
            drawLine(br, GeoOffset(w - cornerLenPx, h - cornerOff), GeoOffset(w, h - cornerOff), strokeWidth = cornerStrokePx)
            drawLine(br, GeoOffset(w - cornerOff, h - cornerLenPx), GeoOffset(w - cornerOff, h), strokeWidth = cornerStrokePx)
            // Bottom-left L
            val bl = cornerColor(bottomActive, leftActive)
            drawLine(bl, GeoOffset(0f, h - cornerOff), GeoOffset(cornerLenPx, h - cornerOff), strokeWidth = cornerStrokePx)
            drawLine(bl, GeoOffset(cornerOff, h - cornerLenPx), GeoOffset(cornerOff, h), strokeWidth = cornerStrokePx)

            // Rule-of-thirds grid inside (lighter, always grey)
            val gridColor = Color.White.copy(alpha = 0.25f)
            drawLine(gridColor, GeoOffset(w / 3f, 0f), GeoOffset(w / 3f, h), strokeWidth = 1f)
            drawLine(gridColor, GeoOffset(2 * w / 3f, 0f), GeoOffset(2 * w / 3f, h), strokeWidth = 1f)
            drawLine(gridColor, GeoOffset(0f, h / 3f), GeoOffset(w, h / 3f), strokeWidth = 1f)
            drawLine(gridColor, GeoOffset(0f, 2 * h / 3f), GeoOffset(w, 2 * h / 3f), strokeWidth = 1f)
        }

        // (Save chip moved OUT of the CropOverlay — it's now pinned to the
        // preview box so it stays in a fixed position instead of floating
        // with the crop rectangle as the user drags handles around.)

        // Invisible drag zones — same pattern as widget-resize: 32dp edges + 48dp
        // corners. Zones are declared EDGES FIRST then corners so corners end up
        // on top in the pointer-input dispatch order at overlapping positions.
        val edgeDragPx = with(density) { 32.dp.toPx() }
        val cornerDragPx = with(density) { 48.dp.toPx() }
        val halfEdge = edgeDragPx / 2f
        val halfCorner = cornerDragPx / 2f
        val rectWPx = rightPx - leftPx
        val rectHPx = bottomPx - topPx

        // TOP edge (between the two top corners)
        DragZone(
            xPx = leftPx + halfCorner,
            yPx = topPx - halfEdge,
            widthPx = (rectWPx - cornerDragPx).coerceAtLeast(0f),
            heightPx = edgeDragPx,
            onStart = { activeHandle = "top" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { _, dy ->
                val newT = ((latestT * latestH + dy) / latestH).coerceIn(0f, latestB - 0.05f)
                onChange(latestL, newT, latestR, latestB)
            }
        )
        // BOTTOM edge
        DragZone(
            xPx = leftPx + halfCorner,
            yPx = bottomPx - halfEdge,
            widthPx = (rectWPx - cornerDragPx).coerceAtLeast(0f),
            heightPx = edgeDragPx,
            onStart = { activeHandle = "bottom" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { _, dy ->
                val newB = ((latestB * latestH + dy) / latestH).coerceIn(latestT + 0.05f, 1f)
                onChange(latestL, latestT, latestR, newB)
            }
        )
        // LEFT edge
        DragZone(
            xPx = leftPx - halfEdge,
            yPx = topPx + halfCorner,
            widthPx = edgeDragPx,
            heightPx = (rectHPx - cornerDragPx).coerceAtLeast(0f),
            onStart = { activeHandle = "left" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, _ ->
                val newL = ((latestL * latestW + dx) / latestW).coerceIn(0f, latestR - 0.05f)
                onChange(newL, latestT, latestR, latestB)
            }
        )
        // RIGHT edge
        DragZone(
            xPx = rightPx - halfEdge,
            yPx = topPx + halfCorner,
            widthPx = edgeDragPx,
            heightPx = (rectHPx - cornerDragPx).coerceAtLeast(0f),
            onStart = { activeHandle = "right" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, _ ->
                val newR = ((latestR * latestW + dx) / latestW).coerceIn(latestL + 0.05f, 1f)
                onChange(latestL, latestT, newR, latestB)
            }
        )
        // TOP-LEFT corner
        DragZone(
            xPx = leftPx - halfCorner, yPx = topPx - halfCorner,
            widthPx = cornerDragPx, heightPx = cornerDragPx,
            onStart = { activeHandle = "topLeft" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, dy ->
                val newL = ((latestL * latestW + dx) / latestW).coerceIn(0f, latestR - 0.05f)
                val newT = ((latestT * latestH + dy) / latestH).coerceIn(0f, latestB - 0.05f)
                onChange(newL, newT, latestR, latestB)
            }
        )
        // TOP-RIGHT corner
        DragZone(
            xPx = rightPx - halfCorner, yPx = topPx - halfCorner,
            widthPx = cornerDragPx, heightPx = cornerDragPx,
            onStart = { activeHandle = "topRight" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, dy ->
                val newR = ((latestR * latestW + dx) / latestW).coerceIn(latestL + 0.05f, 1f)
                val newT = ((latestT * latestH + dy) / latestH).coerceIn(0f, latestB - 0.05f)
                onChange(latestL, newT, newR, latestB)
            }
        )
        // BOTTOM-LEFT corner
        DragZone(
            xPx = leftPx - halfCorner, yPx = bottomPx - halfCorner,
            widthPx = cornerDragPx, heightPx = cornerDragPx,
            onStart = { activeHandle = "bottomLeft" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, dy ->
                val newL = ((latestL * latestW + dx) / latestW).coerceIn(0f, latestR - 0.05f)
                val newB = ((latestB * latestH + dy) / latestH).coerceIn(latestT + 0.05f, 1f)
                onChange(newL, latestT, latestR, newB)
            }
        )
        // BOTTOM-RIGHT corner
        DragZone(
            xPx = rightPx - halfCorner, yPx = bottomPx - halfCorner,
            widthPx = cornerDragPx, heightPx = cornerDragPx,
            onStart = { activeHandle = "bottomRight" },
            onEnd = { activeHandle = null; onCommit() },
            onDrag = { dx, dy ->
                val newR = ((latestR * latestW + dx) / latestW).coerceIn(latestL + 0.05f, 1f)
                val newB = ((latestB * latestH + dy) / latestH).coerceIn(latestT + 0.05f, 1f)
                onChange(latestL, latestT, newR, newB)
            }
        )
    }
}

/** Invisible positioned drag zone. Uses rememberUpdatedState so stale captures don't stick. */
@Composable
private fun BoxScope.DragZone(
    xPx: Float, yPx: Float, widthPx: Float, heightPx: Float,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    if (widthPx <= 0f || heightPx <= 0f) return
    val density = LocalDensity.current
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnEnd by rememberUpdatedState(onEnd)
    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.toInt(), yPx.toInt()) }
            .size(
                width = with(density) { widthPx.toDp() },
                height = with(density) { heightPx.toDp() }
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentOnStart() },
                    onDragEnd = { currentOnEnd() },
                    onDragCancel = { currentOnEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
    )
}

/**
 * Icon-only variant — used for Undo / Redo on the LEFT of the top bar. Takes a
 * Painter so we can use the Spectrum drawable resources the user supplied.
 */
@Composable
private fun IconOnlyTopButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDesc: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        border = BorderStroke(1.dp, if (enabled) Color(0xFF888888) else Color(0xFF3A3A3A))
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDesc,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) Color.White else Color(0xFF555555)
        )
    }
}

/**
 * Text-only variant — used for Reset / Save on the RIGHT of the top bar. Optional
 * [textColor] override lets Reset be red while Save stays white.
 */
@Composable
private fun TextOnlyTopButton(
    label: String,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, if (enabled) Color(0xFF888888) else Color(0xFF3A3A3A))
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) textColor else Color(0xFF555555)
        )
    }
}

/**
 * Category picker icon — used in the bottom row to switch between editing
 * categories (Crop, Brightness, Contrast, Saturation, Filter, Blur, Vignette).
 * Selected state shows a filled white circle with the icon inverted black,
 * matching Samsung-Photos' selected-category visual.
 */
@Composable
private fun CategoryIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    selected: Boolean,
    activated: Boolean = true,
    value: Int = 50,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null
) {
    CategoryIconBox(selected, activated, value, onClick, onDoubleClick) {
        Icon(
            icon, contentDescription = contentDesc,
            modifier = Modifier.size(22.dp),
            tint = Color.White
        )
    }
}

/** Painter variant of CategoryIcon for drawable-resource icons (Spectrum set). */
@Composable
private fun CategoryIconPainter(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDesc: String,
    selected: Boolean,
    activated: Boolean = true,
    value: Int = 50,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null
) {
    CategoryIconBox(selected, activated, value, onClick, onDoubleClick) {
        Icon(
            painter = painter,
            contentDescription = contentDesc,
            modifier = Modifier.size(22.dp),
            tint = Color.White
        )
    }
}

/** Maps a pager filter-cell key (e.g. "filter_warm") to the string constant stored in prefs. */
private fun filterKeyToString(key: String): String = when (key) {
    "filter_mono" -> com.bearinmind.launcher314.data.WP_FILTER_GRAYSCALE
    "filter_sepia" -> com.bearinmind.launcher314.data.WP_FILTER_SEPIA
    "filter_invert" -> com.bearinmind.launcher314.data.WP_FILTER_INVERT
    "filter_warm" -> com.bearinmind.launcher314.data.WP_FILTER_WARM
    "filter_cool" -> com.bearinmind.launcher314.data.WP_FILTER_COOL
    "filter_vivid" -> com.bearinmind.launcher314.data.WP_FILTER_VIVID
    "filter_faded" -> com.bearinmind.launcher314.data.WP_FILTER_FADED
    "filter_polaroid" -> com.bearinmind.launcher314.data.WP_FILTER_POLAROID
    "filter_kodachrome" -> com.bearinmind.launcher314.data.WP_FILTER_KODACHROME
    "filter_vintage" -> com.bearinmind.launcher314.data.WP_FILTER_VINTAGE
    "filter_teal_orange" -> com.bearinmind.launcher314.data.WP_FILTER_TEAL_ORANGE
    "filter_night" -> com.bearinmind.launcher314.data.WP_FILTER_NIGHT
    else -> com.bearinmind.launcher314.data.WP_FILTER_NONE
}

/**
 * Samsung-Photos-style filter thumbnail: a rounded-square mini-preview of the
 * source image with the filter's ColorMatrix applied. Replaces the generic
 * icon in the pager when the Filters section is active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilterThumbnail(
    thumbnail: android.graphics.Bitmap,
    filterMatrix: androidx.compose.ui.graphics.ColorMatrix?,
    selected: Boolean,
    activated: Boolean,
    contentDesc: String,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val targetOutline = if (activated) Color.White else Color(0xFF444444)
    val outlineColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetOutline,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "thumbOutline"
    )
    val outlineWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "thumbOutlineWidth"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .border(width = outlineWidth, color = outlineColor, shape = shape)
            .then(
                if (onDoubleClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onDoubleClick = onDoubleClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = contentDesc,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            colorFilter = filterMatrix?.let { ColorFilter.colorMatrix(it) }
        )
    }
}

/**
 * Shared box-with-progress-arc render used by both icon variants. When the
 * cell is selected AND activated, the glyph is replaced with the current
 * slider value (delta from the 50 neutral) and a circular arc is drawn just
 * inside the rounded-rect border to show how far the value has moved from
 * default — like the Samsung Photos editor's indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryIconBox(
    selected: Boolean,
    activated: Boolean,
    value: Int,
    onClick: () -> Unit = {},
    onDoubleClick: (() -> Unit)? = null,
    iconContent: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    // Animated colors/widths so transitions between selected/unselected and
    // activated/unactivated states don't snap — they interpolate across a
    // short tween. Keeps the pager feel fluid when flipping pages.
    val targetOutlineColor = if (activated) Color.White else Color(0xFF444444)
    val outlineColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetOutlineColor,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "outlineColor"
    )
    val outlineWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "outlineWidth"
    )
    val iconAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (activated) 1f else 0.4f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "iconAlpha"
    )
    // Fill colors and active state — also tweened so the value-preview shade
    // shifts gently when crossing the selected/unselected boundary.
    val selectedFill = Color(0xFF5A5A5A)
    val unselectedActivatedFill = Color(0xFF2E2E2E)
    val fillColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedFill else unselectedActivatedFill,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "fillColor"
    )
    val showValueIndicator = selected && activated
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .drawBehind {
                // Draw value fill for ANY activated cell (selected or not) so
                // the user can see at a glance where each effect sits before
                // they navigate to it. 0 → no fill. Positive values fill from
                // the LEFT edge; negative fill from the RIGHT edge.
                if (activated && value != 0) {
                    val frac = (kotlin.math.abs(value) / 100f).coerceIn(0f, 1f)
                    val fillW = size.width * frac
                    val left = if (value >= 0) 0f else size.width - fillW
                    drawRect(
                        color = fillColor,
                        topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(fillW, size.height)
                    )
                }
            }
            .border(width = outlineWidth, color = outlineColor, shape = shape)
            .then(
                if (onDoubleClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onDoubleClick = onDoubleClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showValueIndicator) {
            // Show signed slider value (e.g. "+29" / "-12" / "0").
            val text = when {
                value > 0 -> "+$value"
                else -> value.toString()
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        } else {
            Box(modifier = Modifier.graphicsLayer { alpha = iconAlpha }) {
                iconContent()
            }
        }
    }
}

/**
 * Tiny circular icon button used inside the floating transform row over the
 * image preview. Selected state turns the icon blue (used for flip toggles).
 */
@Composable
private fun FloatingTransformIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    // Inner-slot icon button — no outer border (the whole transform Row has one
    // shared rounded box around all four icons). When a flip toggle is active,
    // the icon cell gets a subtle white highlight so the user sees the state.
    val innerShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(innerShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = contentDesc,
            modifier = Modifier.size(20.dp),
            tint = if (selected) Color.Black else Color.White
        )
    }
}

/** Painter variant for custom vector drawables (Spectrum rotate icons). */
@Composable
private fun FloatingTransformIconPainter(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDesc: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val innerShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(innerShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDesc,
            modifier = Modifier.size(20.dp),
            tint = if (selected) Color.Black else Color.White
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFFAAAAAA),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float? = null,
    minTxt: String? = null,
    maxTxt: String? = null,
    slider: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
            if (value != null) {
                Text(String.format("%.1f", value), style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA))
            }
        }
        slider()
    }
}

@Composable
private fun EditorIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color(0xFF3A3A3A))
    OutlinedButton(
        onClick = onClick,
        border = border,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else Color.White)
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun ApplyButton(
    label: String,
    modifier: Modifier = Modifier,
    isApplying: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isApplying,
        modifier = modifier
    ) { Text(label) }
}

private fun currentEdit(
    scale: Float, offsetX: Float, offsetY: Float,
    rotation: Int, flipH: Boolean, flipV: Boolean,
    cropL: Float, cropT: Float, cropR: Float, cropB: Float,
    brightness: Int, contrast: Int, saturation: Int,
    blur: Int, vignette: Int, filter: String
) = DeviceWallpaperEdit(
    scale = scale, offsetX = offsetX, offsetY = offsetY,
    rotationDegrees = rotation, flipH = flipH, flipV = flipV,
    cropLeft = cropL, cropTop = cropT, cropRight = cropR, cropBottom = cropB,
    brightness = brightness, contrast = contrast, saturation = saturation,
    blur = blur, vignette = vignette, filter = filter
)

internal fun applyEdited(
    context: android.content.Context,
    source: android.graphics.Bitmap,
    edit: DeviceWallpaperEdit,
    target: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    setApplying: (Boolean) -> Unit,
    setStatus: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    setApplying(true)
    setStatus(null)
    scope.launch {
        val ok = withContext(Dispatchers.IO) {
            val dm = context.resources.displayMetrics
            val outW = dm.widthPixels
            val outH = dm.heightPixels
            val rendered = WallpaperHelper.renderEditedBitmap(
                context, source, outW, outH,
                edit.scale, edit.offsetX, edit.offsetY,
                edit.rotationDegrees, edit.flipH, edit.flipV,
                edit.cropLeft, edit.cropTop, edit.cropRight, edit.cropBottom,
                edit.brightness, edit.contrast, edit.saturation,
                edit.blur, edit.vignette, edit.filter,
                exposure = edit.exposure,
                highlights = edit.highlights,
                shadows = edit.shadows,
                tint = edit.tint,
                temperature = edit.temperature,
                sharpness = edit.sharpness
            )
            val applied = WallpaperHelper.applyAsSystemWallpaper(context, rendered, target)
            if (rendered !== source) rendered.recycle()
            applied
        }
        setApplying(false)
        if (ok) {
            setWallpaperMode(context, WALLPAPER_MODE_DEVICE)
            setDeviceWallpaperEdit(context, edit)
            bumpWallpaperCacheVersion(context)
            onSuccess()
        } else {
            setStatus("Failed to apply wallpaper. Check permissions.")
        }
    }
}
