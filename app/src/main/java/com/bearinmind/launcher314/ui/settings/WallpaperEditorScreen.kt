package com.bearinmind.launcher314.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AutoAwesome
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Combined ColorFilter for live preview
    val previewColorFilter = remember(brightness, contrast, saturation, filter) {
        val cm = android.graphics.ColorMatrix()
        if (brightness != 0) {
            val b = (brightness / 50f * 127f)
            cm.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (contrast != 0) {
            val c = 1f + contrast / 100f
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
            sat.setSaturation((1f + saturation / 100f).coerceAtLeast(0f))
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
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
        ) {
            // IconRow is a normal Column child (sized to its own height). The Column
            // applies a fixed bottom padding so the IconRow sits above the 3-button
            // nav bar. WindowInsets / view.rootWindowInsets both return 0 inside
            // this Dialog, so we hardcode 56dp — the standard 3-button nav bar
            // height on Samsung One UI / stock Android.
            val pinnedIconRowHeight = 76.dp
            val navBarPaddingDp = 56.dp
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
                        label = "Save",
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

                            // Crop overlay — sibling of the Image inside the inner box so
                            // they share the exact same parent dimensions (the inner box's
                            // fillMaxSize). This guarantees the crop outline aligns precisely
                            // with the image bounds where the outer border sits.
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

                            // Floating transform row — overlaid on the bottom-center of the
                            // image preview, semi-transparent pill background. Replaces the
                            // old "Transform" section in the controls list.
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FloatingTransformIcon(
                                    icon = Icons.Outlined.Rotate90DegreesCcw,
                                    contentDesc = "Rotate left"
                                ) { rotation = ((rotation - 90) + 360) % 360; commitEdit() }
                                FloatingTransformIcon(
                                    icon = Icons.Outlined.Rotate90DegreesCw,
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
                        } // end inner box
                    }

                    // ---- Controls — category icon picker + active control ----
                    // One category is selected at a time (Samsung-Photos-style). The picked
                    // category's control (slider or chips) is shown above its icon row. Crop
                    // mode shows no popup control — only the (now larger) preview + outline.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))

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
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val categoryLabel = when (cat) {
                                        "brightness" -> "Brightness"
                                        "contrast" -> "Contrast"
                                        "saturation" -> "Saturation"
                                        "filter" -> "Filter"
                                        "blur" -> "Blur"
                                        "vignette" -> "Vignette"
                                        else -> ""
                                    }
                                    Text(
                                        categoryLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    // Height bumped to 92dp so the slider's tick number
                                    // labels + caption text below the track render
                                    // INSIDE this box (slider is ~80–90dp tall) instead
                                    // of overflowing onto the IconRow underneath.
                                    Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                                        when (cat) {
                                            "brightness" -> ThumbDragHorizontalSlider(
                                                currentValue = brightness.toFloat(),
                                                config = SliderConfigs.wallpaperBrightness,
                                                onValueChange = { brightness = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "contrast" -> ThumbDragHorizontalSlider(
                                                currentValue = contrast.toFloat(),
                                                config = SliderConfigs.wallpaperBrightness,
                                                onValueChange = { contrast = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "saturation" -> ThumbDragHorizontalSlider(
                                                currentValue = saturation.toFloat(),
                                                config = SliderConfigs.wallpaperSaturation,
                                                onValueChange = { saturation = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "filter" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(selected = filter == WP_FILTER_NONE,
                                                    onClick = { filter = WP_FILTER_NONE; commitEdit() }, label = { Text("None") })
                                                FilterChip(selected = filter == WP_FILTER_GRAYSCALE,
                                                    onClick = { filter = WP_FILTER_GRAYSCALE; commitEdit() }, label = { Text("B&W") })
                                                FilterChip(selected = filter == WP_FILTER_SEPIA,
                                                    onClick = { filter = WP_FILTER_SEPIA; commitEdit() }, label = { Text("Sepia") })
                                                FilterChip(selected = filter == WP_FILTER_INVERT,
                                                    onClick = { filter = WP_FILTER_INVERT; commitEdit() }, label = { Text("Invert") })
                                            }
                                            "blur" -> ThumbDragHorizontalSlider(
                                                currentValue = blur.toFloat(),
                                                config = SliderConfigs.wallpaperPercent,
                                                onValueChange = { blur = it.toInt() },
                                                onValueChangeFinished = { commitEdit() }
                                            )
                                            "vignette" -> ThumbDragHorizontalSlider(
                                                currentValue = vignette.toFloat(),
                                                config = SliderConfigs.wallpaperPercent,
                                                onValueChange = { vignette = it.toInt() },
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

                    // Category icon picker — placed RIGHT below the Controls column so
                    // it sits directly under the brightness slider (no big gap). The
                    // Column's navigationBarsPadding above keeps it clear of the nav bar.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pinnedIconRowHeight)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(Icons.Outlined.Crop, "Crop",
                            selected = activeCategoryShared == "crop") { activeCategoryShared = "crop" }
                        CategoryIcon(Icons.Outlined.LightMode, "Brightness",
                            selected = activeCategoryShared == "brightness") { activeCategoryShared = "brightness" }
                        CategoryIcon(Icons.Outlined.Contrast, "Contrast",
                            selected = activeCategoryShared == "contrast") { activeCategoryShared = "contrast" }
                        CategoryIcon(Icons.Outlined.InvertColors, "Saturation",
                            selected = activeCategoryShared == "saturation") { activeCategoryShared = "saturation" }
                        CategoryIcon(Icons.Outlined.AutoAwesome, "Filter",
                            selected = activeCategoryShared == "filter") { activeCategoryShared = "filter" }
                        CategoryIcon(Icons.Outlined.BlurOn, "Blur",
                            selected = activeCategoryShared == "blur") { activeCategoryShared = "blur" }
                        CategoryIcon(Icons.Outlined.PanoramaFishEye, "Vignette",
                            selected = activeCategoryShared == "vignette") { activeCategoryShared = "vignette" }
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Color(0xFF555555),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = contentDesc,
            modifier = Modifier.size(22.dp),
            tint = if (selected) Color.Black else Color.White
        )
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
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = contentDesc,
            modifier = Modifier.size(20.dp),
            tint = if (selected) Color(0xFF64B5F6) else Color.White
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
                edit.blur, edit.vignette, edit.filter
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
