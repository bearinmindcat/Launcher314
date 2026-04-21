package com.bearinmind.launcher314.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
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
import com.bearinmind.launcher314.data.WP_FILTER_GRAYSCALE
import com.bearinmind.launcher314.data.WP_FILTER_INVERT
import com.bearinmind.launcher314.data.WP_FILTER_NONE
import com.bearinmind.launcher314.data.WP_FILTER_SEPIA
import com.bearinmind.launcher314.data.bumpWallpaperCacheVersion
import com.bearinmind.launcher314.data.setDeviceWallpaperEdit
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
    onApplied: () -> Unit
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
    var lightBalance by remember { mutableIntStateOf(initialEdit.lightBalance) }
    var exposure by remember { mutableIntStateOf(initialEdit.exposure) }
    var highlights by remember { mutableIntStateOf(initialEdit.highlights) }
    var shadows by remember { mutableIntStateOf(initialEdit.shadows) }
    var tint by remember { mutableIntStateOf(initialEdit.tint) }
    var temperature by remember { mutableIntStateOf(initialEdit.temperature) }
    var sharpness by remember { mutableIntStateOf(initialEdit.sharpness) }
    var definition by remember { mutableIntStateOf(initialEdit.definition) }
    var blur by remember { mutableIntStateOf(initialEdit.blur) }
    var vignette by remember { mutableIntStateOf(initialEdit.vignette) }
    var filter by remember { mutableStateOf(initialEdit.filter) }
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
        lightBalance = lightBalance, exposure = exposure,
        highlights = highlights, shadows = shadows,
        tint = tint, temperature = temperature,
        sharpness = sharpness, definition = definition,
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
        lightBalance = e.lightBalance; exposure = e.exposure
        highlights = e.highlights; shadows = e.shadows
        tint = e.tint; temperature = e.temperature
        sharpness = e.sharpness; definition = e.definition
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

    // Combined ColorFilter for live preview. Effects that fit cleanly into a
    // 4x5 ColorMatrix are applied here (brightness, exposure, lightBalance,
    // temperature, tint, contrast, saturation, definition, filter). Tonal
    // effects that need per-pixel curves (highlights, shadows) use rough
    // matrix approximations for preview; the bake-time renderer applies the
    // real curves. Sharpness is bake-only (needs convolution).
    val previewColorFilter = remember(
        brightness, contrast, saturation, filter,
        lightBalance, exposure, highlights, shadows, tint, temperature, definition
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
        // lightBalance applies a histogram-driven cubic LUT (Apple Brilliance
        // patent). Not expressible as a 4x5 ColorMatrix so preview skips it;
        // baked output (see WallpaperHelper.applyLightBalance) reflects it.
        if (temperature != 0) {
            val t = amt(temperature) * 40f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, t,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (tint != 0) {
            val t = amt(tint) * 30f
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, t * 0.5f,
                0f, 1f, 0f, 0f, -t,
                0f, 0f, 1f, 0f, t * 0.5f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // Highlights + shadows are luminance-masked tone curves that can't be
        // expressed as a 4x5 ColorMatrix — they're applied per-pixel at bake
        // time only (WallpaperHelper.applyTonalAndSharpness). Live preview
        // therefore doesn't reflect them; only the saved wallpaper does.
        if (definition != 0) {
            val c = 1f + amt(definition) * 0.5f
            val t = 128f * (1f - c)
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
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
        when (filter) {
            WP_FILTER_GRAYSCALE -> { val g = android.graphics.ColorMatrix(); g.setSaturation(0f); cm.postConcat(g) }
            WP_FILTER_SEPIA -> cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )))
            WP_FILTER_INVERT -> cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
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
            val pinnedIconRowHeight = 76.dp
            val navBarPaddingDp = 96.dp
            // Active editing category — hoisted so the IconRow's CategoryIcons and
            // the Crossfade control above can share the same state.
            var activeCategoryShared by remember { mutableStateOf("brightness") }
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
                        onClick = { saveDialogOpen = true }
                    )
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
                    val isCropMode = activeCategory == "crop"

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
                        val previewAspect = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()
                        // Force the preview to fill the available HEIGHT and then derive the
                        // narrower width from the image's aspect ratio — this keeps the box
                        // exactly the image's shape, no dark extension on the sides for tall
                        // portrait images.
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(previewAspect)
                                .border(1.dp, Color(0xFF555555)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
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
                            Image(
                                bitmap = sourceBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
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
                            if (isCropMode) {
                                CropOverlay(
                                    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB,
                                    onCropChange = { l, t, r, b ->
                                        cropL = l.coerceIn(0f, 1f)
                                        cropT = t.coerceIn(0f, 1f)
                                        cropR = r.coerceIn(0f, 1f)
                                        cropB = b.coerceIn(0f, 1f)
                                    },
                                    onCommit = { commitEdit() }
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

                        // Crossfade smoothly transitions the label + control between categories
                        // when the active one changes. Crop mode renders an empty placeholder
                        // so the surrounding layout collapses cleanly.
                        androidx.compose.animation.Crossfade(
                            targetState = activeCategory,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 220,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            label = "categoryControl"
                        ) { cat ->
                            if (cat == "crop") {
                                Spacer(Modifier.height(0.dp))
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    val categoryLabel = when (cat) {
                                        "light_balance" -> "Light balance"
                                        "brightness" -> "Brightness"
                                        "exposure" -> "Exposure"
                                        "contrast" -> "Contrast"
                                        "highlights" -> "Highlights"
                                        "shadows" -> "Shadows"
                                        "saturation" -> "Saturation"
                                        "tint" -> "Tint"
                                        "temperature" -> "Temperature"
                                        "sharpness" -> "Sharpness"
                                        "definition" -> "Definition"
                                        else -> ""
                                    }
                                    Text(
                                        categoryLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    // Height sized to match the slider's own vertical
                                    // footprint (~72dp = 48dp track + ~16dp tick labels
                                    // + a little breathing room). Any bigger and we get
                                    // empty space above the slider that visually adds
                                    // a gap between the category name and the track.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 28.dp)
                                            .height(64.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        // All -50..+50 effects reuse wallpaperBrightness (bipolar).
                                        // Sharpness is 0..100 (wallpaperPercent). Saturation stays
                                        // on its -100..+100 config.
                                        when (cat) {
                                            "light_balance" -> ThumbDragHorizontalSlider(
                                                currentValue = lightBalance.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { lightBalance = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "brightness" -> ThumbDragHorizontalSlider(
                                                currentValue = brightness.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { brightness = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "exposure" -> ThumbDragHorizontalSlider(
                                                currentValue = exposure.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { exposure = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "contrast" -> ThumbDragHorizontalSlider(
                                                currentValue = contrast.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { contrast = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "highlights" -> ThumbDragHorizontalSlider(
                                                currentValue = highlights.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { highlights = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "shadows" -> ThumbDragHorizontalSlider(
                                                currentValue = shadows.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { shadows = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "saturation" -> ThumbDragHorizontalSlider(
                                                currentValue = saturation.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { saturation = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "tint" -> ThumbDragHorizontalSlider(
                                                currentValue = tint.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { tint = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "temperature" -> ThumbDragHorizontalSlider(
                                                currentValue = temperature.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { temperature = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "sharpness" -> ThumbDragHorizontalSlider(
                                                currentValue = sharpness.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { sharpness = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "definition" -> ThumbDragHorizontalSlider(
                                                currentValue = definition.toFloat(),
                                                config = SliderConfigs.wallpaperEffect,
                                                onValueChange = { definition = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        statusMessage?.let {
                            Text(it, color = Color(0xFFFF6B6B))
                        }
                    }

                    // Category picker — HorizontalPager. User swipes left/right to
                    // slide through categories; centered page is the active one,
                    // scales up (1.3x) and full-alpha while neighbors shrink + fade.
                    // Tapping a neighbor animates to center on that page.
                    val categoryKeys = listOf(
                        "crop", "light_balance", "brightness", "exposure", "contrast",
                        "highlights", "shadows", "saturation", "tint", "temperature",
                        "sharpness", "definition"
                    )
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = categoryKeys.indexOf(activeCategoryShared).coerceAtLeast(0),
                        pageCount = { categoryKeys.size }
                    )
                    val pickerHaptics = com.bearinmind.launcher314.helpers.rememberHapticFeedback()
                    // `visualPage` tracks whichever icon is visually centered
                    // (smallest |offset|). Using this instead of `currentPage`
                    // keeps the "selected" highlight (outline + fill + scale)
                    // lined up with the icon that's actually biggest on screen
                    // during drags and flings, so the pager never looks like
                    // it's snapping to a different icon than you chose.
                    val visualPage by remember {
                        derivedStateOf {
                            val raw = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            raw.toDouble().let { kotlin.math.round(it).toInt() }
                                .coerceIn(0, categoryKeys.size - 1)
                        }
                    }
                    // Skip the first emission (initial composition) so opening
                    // the editor doesn't trigger a vibration. Subsequent page
                    // changes — drag, fling-snap, or programmatic — get a soft
                    // tick to confirm the new selection.
                    var hapticPrimed by remember { mutableStateOf(false) }
                    LaunchedEffect(visualPage) {
                        if (hapticPrimed) {
                            pickerHaptics.performTextHandleMove()
                        } else {
                            hapticPrimed = true
                        }
                        activeCategoryShared = categoryKeys[visualPage]
                    }
                    val cropActivated = cropL != 0f || cropT != 0f || cropR != 1f || cropB != 1f ||
                        rotation != 0 || flipH || flipV || scale != 1f || offsetX != 0f || offsetY != 0f
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pinnedIconRowHeight)
                    ) {
                        val itemWidth = 56.dp
                        // Center the current page by padding each side with
                        // (container - item) / 2.
                        val sidePadding = ((this.maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
                        // threshold = 0.5 is "round to nearest": whatever icon
                        // is visually centered when the finger lifts is where
                        // the pager snaps. Combined with visualPage-driven
                        // selection above, this means the icon that LOOKS
                        // biggest is always the one that ends up selected.
                        // atMost(1) + non-bouncy spring keep each swipe
                        // unambiguous (exactly one page advance, settles
                        // quickly without overshoot).
                        val flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = androidx.compose.foundation.pager.PagerSnapDistance.atMost(1),
                            snapPositionalThreshold = 0.5f,
                            snapAnimationSpec = androidx.compose.animation.core.spring(
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
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
                            // — used to scale + fade neighbors. coerceIn(0..1)
                            // caps pages >=1 away at the minimum scale.
                            val pageOffset = kotlin.math.abs(
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).coerceIn(0f, 1f)
                            val iconScale = 1.25f - pageOffset * 0.40f  // center 1.25, edge 0.85
                            val iconAlpha = 1f - pageOffset * 0.45f     // center 1.00, edge 0.55
                            val activated = when (key) {
                                "crop" -> cropActivated
                                "light_balance" -> lightBalance != 0
                                "brightness" -> brightness != 0
                                "exposure" -> exposure != 0
                                "contrast" -> contrast != 0
                                "highlights" -> highlights != 0
                                "shadows" -> shadows != 0
                                "saturation" -> saturation != 0
                                "tint" -> tint != 0
                                "temperature" -> temperature != 0
                                "sharpness" -> sharpness != 0
                                "definition" -> definition != 0
                                else -> false
                            }
                            val isSelected = page == visualPage
                            val onTap: () -> Unit = {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        alpha = iconAlpha
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Crop has no scalar slider, so just feed 50 (no indicator).
                                val displayValue = when (key) {
                                    "light_balance" -> lightBalance
                                    "brightness" -> brightness
                                    "exposure" -> exposure
                                    "contrast" -> contrast
                                    "highlights" -> highlights
                                    "shadows" -> shadows
                                    "saturation" -> saturation
                                    "tint" -> tint
                                    "temperature" -> temperature
                                    "sharpness" -> sharpness
                                    "definition" -> definition
                                    else -> 50
                                }
                                when (key) {
                                    "crop" -> CategoryIcon(Icons.Outlined.Crop, "Crop", isSelected, activated, displayValue, onTap)
                                    "light_balance" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_lightbulb_2_spectrum),
                                        "Light balance", isSelected, activated, displayValue, onTap
                                    )
                                    "brightness" -> CategoryIcon(Icons.Outlined.LightMode, "Brightness", isSelected, activated, displayValue, onTap)
                                    "exposure" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_contrast_square_spectrum),
                                        "Exposure", isSelected, activated, displayValue, onTap
                                    )
                                    "contrast" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_contrast_spectrum),
                                        "Contrast", isSelected, activated, displayValue, onTap
                                    )
                                    "highlights" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_tonality_2_spectrum),
                                        "Highlights", isSelected, activated, displayValue, onTap
                                    )
                                    "shadows" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_tonality_spectrum),
                                        "Shadows", isSelected, activated, displayValue, onTap
                                    )
                                    "saturation" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_saturation_levels),
                                        "Saturation", isSelected, activated, displayValue, onTap
                                    )
                                    "tint" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_opacity_spectrum),
                                        "Tint", isSelected, activated, displayValue, onTap
                                    )
                                    "temperature" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_thermometer_spectrum),
                                        "Temperature", isSelected, activated, displayValue, onTap
                                    )
                                    "sharpness" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_triangle_rounded_tonality),
                                        "Sharpness", isSelected, activated, displayValue, onTap
                                    )
                                    "definition" -> CategoryIconPainter(
                                        androidx.compose.ui.res.painterResource(com.bearinmind.launcher314.R.drawable.ic_triangle_rounded_levels),
                                        "Definition", isSelected, activated, displayValue, onTap
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
    onCommit: () -> Unit = {}
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
        val dim = Color.Black.copy(alpha = 0.55f)

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
    onClick: () -> Unit
) {
    CategoryIconBox(selected, activated, value, onClick) {
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
    onClick: () -> Unit
) {
    CategoryIconBox(selected, activated, value, onClick) {
        Icon(
            painter = painter,
            contentDescription = contentDesc,
            modifier = Modifier.size(22.dp),
            tint = Color.White
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
@Composable
private fun CategoryIconBox(
    selected: Boolean,
    activated: Boolean,
    value: Int,
    onClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    // Activated (value != 0) → pure white outline so touched effects stand out
    // clearly against untouched ones as you scroll past. Selected keeps the
    // pure-white outline but gets a thicker stroke + brighter fill below so
    // it's still obviously the current cell.
    val outlineColor = when {
        activated -> Color.White
        else -> Color(0xFF444444)
    }
    val outlineWidth = if (selected) 1.5.dp else 1.dp
    val iconAlpha = if (activated) 1f else 0.4f
    // Slightly brighter fill on the selected cell so it's more obvious which
    // cell is currently being edited. Non-selected activated cells use a
    // dimmer fill so their value preview is still visible without competing.
    val selectedFill = Color(0xFF5A5A5A)
    val unselectedActivatedFill = Color(0xFF2E2E2E)
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
                        color = if (selected) selectedFill else unselectedActivatedFill,
                        topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(fillW, size.height)
                    )
                }
            }
            .border(width = outlineWidth, color = outlineColor, shape = shape)
            .clickable(onClick = onClick),
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

private fun applyEdited(
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
                lightBalance = edit.lightBalance,
                exposure = edit.exposure,
                highlights = edit.highlights,
                shadows = edit.shadows,
                tint = edit.tint,
                temperature = edit.temperature,
                sharpness = edit.sharpness,
                definition = edit.definition
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
