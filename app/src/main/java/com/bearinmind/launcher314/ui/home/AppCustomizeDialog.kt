package com.bearinmind.launcher314.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import com.bearinmind.launcher314.helpers.FontManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearinmind.launcher314.ui.settings.FontsScreen
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import com.bearinmind.launcher314.R
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.AppCustomization
import com.bearinmind.launcher314.data.HomeAppInfo
import com.bearinmind.launcher314.data.getCustomIconsDir
import com.bearinmind.launcher314.data.saveBitmapToFile
import com.bearinmind.launcher314.helpers.IconShapes
import com.bearinmind.launcher314.helpers.deleteBgTintedIcon
import com.bearinmind.launcher314.helpers.deleteShapedBgTintedIcon
import com.bearinmind.launcher314.helpers.deleteShapedIcon
import com.bearinmind.launcher314.helpers.generateBgTintedIcon
import com.bearinmind.launcher314.helpers.generateShapedBgTintedIcon
import com.bearinmind.launcher314.helpers.generateShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateGlobalShapedIcon
import com.bearinmind.launcher314.helpers.getOrGenerateBgColorShapedIcon
import com.bearinmind.launcher314.helpers.getIconShape
import com.bearinmind.launcher314.helpers.getShapedExpDir
import com.bearinmind.launcher314.helpers.parseBlendMode
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Small chip used by the Icon sub-section's three-mode selector
 *  (Use customized / Use original / Pick image). Selected chips get a
 *  brighter background; "Pick image" passes selected=false because it's
 *  an action, not a persistent mode. */
@Composable
private fun IconModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Color.White.copy(alpha = if (selected) 0.22f else 0.1f),
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (selected) 1f else 0.7f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Upward-pointing tail drawn in the SAME color as the panel surface, so it
 *  reads as the panel growing a pointer toward the selected card — same
 *  pattern AnimatedPopup.kt uses for the long-press menu's arrow. */
@Composable
private fun PanelTail(color: Color) {
    Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}

/** Default size (sp) for the text-as-icon glyph when the user hasn't set
 *  one explicitly. Picked so a typical word ("Morphe", "App") reads as a
 *  prominent icon without overrunning the cell at default icon sizes. */
private const val DEFAULT_ICON_TEXT_SIZE_SP = 28

private val PRESET_COLORS = listOf(
    null to "None",
    0xFFFFFFFF to "White",
    0xFFEF9A9A to "Red",
    0xFFA5D6A7 to "Green",
    0xFF90CAF9 to "Blue",
    0xFFFFF59D to "Yellow",
    0xFFFFCC80 to "Orange",
    0xFFCE93D8 to "Purple",
    0xFF9FA8DA to "Indigo"
)

@Composable
fun AppCustomizeDialog(
    context: Context,
    appInfo: HomeAppInfo,
    currentCustomization: AppCustomization?,
    globalIconSizePercent: Int = 100,
    globalIconTextSizePercent: Int = 100,
    iconSizeOverflowThreshold: Float = 125f,
    globalIconShape: String? = null,
    globalIconBgColor: Int? = null,
    onSave: (AppCustomization) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var customLabel by remember { mutableStateOf(currentCustomization?.customLabel ?: "") }
    var hideLabel by remember { mutableStateOf(currentCustomization?.hideLabel ?: false) }
    var selectedTintColor by remember { mutableStateOf(currentCustomization?.iconTintColor) }
    var tintIntensity by remember { mutableStateOf(
        (currentCustomization?.iconTintIntensity ?: 100).toFloat()
    ) }
    // Per-app icon size uses absolute percentage (same scale as global slider)
    var selectedSizePercent by remember { mutableStateOf(
        (currentCustomization?.iconSizePercent ?: globalIconSizePercent).toFloat().coerceIn(SliderConfigs.perAppIconSize.minValue, SliderConfigs.perAppIconSize.maxValue)
    ) }
    var tintBackgroundOnly by remember { mutableStateOf(currentCustomization?.iconTintBackgroundOnly ?: false) }
    // Issue #48 — Experimental: detach the icon from the grid.
    var detachedFromGrid by remember { mutableStateOf(currentCustomization?.detachedFromGrid ?: false) }
    // Text-as-icon (replaces the image when non-blank).
    var iconText by remember { mutableStateOf(currentCustomization?.iconText ?: "") }
    var iconTextColor by remember { mutableStateOf(currentCustomization?.iconTextColor) }
    var iconTextColorIntensity by remember { mutableStateOf(
        (currentCustomization?.iconTextColorIntensity ?: 100).toFloat()
    ) }
    var iconTextFontId by remember { mutableStateOf(currentCustomization?.iconTextFontId) }
    // Null = auto (renderer picks ~0.55× icon size). Non-null = direct SP
    // override the user typed into the popup.
    var iconAsTextSizeSp by remember { mutableStateOf(currentCustomization?.iconAsTextSizeSp) }
    var showIconTextSizePicker by remember { mutableStateOf(false) }
    // Icon "mode" in the Experimental section. Mutually exclusive with text
    // mode (iconText non-blank). When true the rendered icon falls back to
    // the app's own launcher icon, bypassing every image-side override.
    var useOriginalIcon by remember { mutableStateOf(currentCustomization?.useOriginalIcon ?: false) }
    // 0 = no sub-section open (only Detach toggle + cards visible), 1 = icon
    // section selected (just highlights the Icon card — picker fires on tap),
    // 2 = text section selected (shows text field + color + intensity + font).
    var expSubSection by remember { mutableStateOf(
        if (!currentCustomization?.iconText.isNullOrBlank()) 2 else 0
    ) }
    // Separate FocusRequester for the icon-text font screen (so the Label
    // section's font picker doesn't get accidentally re-purposed).
    var showIconTextFontScreen by remember { mutableStateOf(false) }
    var selectedShapeExp by remember { mutableStateOf(currentCustomization?.iconShapeExp) }
    var customIconPath by remember { mutableStateOf(currentCustomization?.customIconPath) }
    var selectedTextSizePercent by remember { mutableStateOf(
        (currentCustomization?.iconTextSizePercent ?: globalIconTextSizePercent).toFloat().coerceIn(SliderConfigs.iconTextSize.minValue, SliderConfigs.iconTextSize.maxValue)
    ) }
    var selectedFontId by remember { mutableStateOf(currentCustomization?.labelFontId) }
    var selectedLabelColor by remember { mutableStateOf(currentCustomization?.labelColor) }
    var labelColorIntensity by remember { mutableStateOf(
        (currentCustomization?.labelColorIntensity ?: 100).toFloat()
    ) }
    var selectedIconPackName by remember { mutableStateOf(currentCustomization?.customIconPackName) }
    var showFontScreen by remember { mutableStateOf(false) }
    var showIconPackBrowser by remember { mutableStateOf(false) }

    // Version counter to bust Coil cache when the same file path is overwritten
    var customIconVersion by remember { mutableStateOf(0) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                // Read EXIF orientation
                val exifStream = context.contentResolver.openInputStream(uri)
                val rotation = if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }.also { exifStream.close() }
                } else 0f

                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (original != null) {
                    // Apply EXIF rotation
                    val rotated = if (rotation != 0f) {
                        val matrix = Matrix().apply { postRotate(rotation) }
                        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                    } else original

                    // Center-crop to square, scale to 192x192
                    val size = minOf(rotated.width, rotated.height)
                    val x = (rotated.width - size) / 2
                    val y = (rotated.height - size) / 2
                    val cropped = Bitmap.createBitmap(rotated, x, y, size, size)
                    val scaled = Bitmap.createScaledBitmap(cropped, 192, 192, true)
                    val outFile = File(getCustomIconsDir(context), "${appInfo.packageName}.png")
                    saveBitmapToFile(scaled, outFile)
                    customIconPath = outFile.absolutePath
                    selectedIconPackName = null
                    customIconVersion++
                    // Picking an image switches the icon to "customized"
                    // and clears text-mode so the two never both apply.
                    useOriginalIcon = false
                    iconText = ""
                    if (cropped !== rotated) cropped.recycle()
                    if (rotated !== original) rotated.recycle()
                    scaled.recycle()
                    original.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    // Which expandable section is open (mutually exclusive)
    var expandedSection by remember { mutableStateOf(0) } // 0=none, 1=shape(EXP), 2=tint, 3=size, 4=label, 5=experimental

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live preview
                val previewLabel = if (customLabel.isNotBlank()) customLabel else appInfo.name
                val previewLabelAlpha by animateFloatAsState(
                    targetValue = if (hideLabel) 0f else 1f,
                    animationSpec = tween(durationMillis = 250),
                    label = "previewLabelAlpha"
                )
                @Suppress("UNUSED_VARIABLE")
                val previewShape: Shape? = null
                // Preview at the chosen per-app size, unless "Detach from
                // grid" is on — then it sits at 1.0 so the icon stays
                // anchored at the default size while the user picks shape /
                // tint / size sliders for the detached layout.
                val rawPreviewSizeScale = selectedSizePercent / globalIconSizePercent.toFloat()
                val previewSizeScale = if (detachedFromGrid) 1f else rawPreviewSizeScale

                // Effective shape: per-app overrides global
                val effectiveShapeName = selectedShapeExp ?: globalIconShape

                // Resolve current icon path — picks up icon_pack_cache changes.
                // While "Detach from grid" is ON, force the ORIGINAL app
                // icon into the preview across EVERY tab (shape / tint /
                // size / label / exp). Detached icons live on the page
                // freely and the user wants the unmodified original as the
                // reference while they're positioning / sizing one — the
                // shape / tint / custom icon overrides muddy that.
                val showOriginalForDetach = detachedFromGrid || useOriginalIcon
                val currentIconPath = remember(customIconVersion, selectedIconPackName, customIconPath, showOriginalForDetach) {
                    if (showOriginalForDetach) appInfo.iconPath
                    else if (customIconPath != null) customIconPath!!
                    else {
                        val packCache = File(context.cacheDir, "icon_pack_cache/${appInfo.packageName}.png")
                        if (packCache.exists()) packCache.absolutePath
                        else appInfo.iconPath
                    }
                }

                // Generate shaped EXP preview bitmap (plain shape, no tint baked in)
                val shapeExpPreviewBitmap = remember(effectiveShapeName, globalIconBgColor, customIconVersion, selectedIconPackName) {
                    effectiveShapeName?.let {
                        try {
                            if (globalIconBgColor != null) {
                                val path = getOrGenerateBgColorShapedIcon(context, appInfo.packageName, it, globalIconBgColor)
                                BitmapFactory.decodeFile(path)?.asImageBitmap()
                            } else {
                                val path = getOrGenerateGlobalShapedIcon(context, appInfo.packageName, it)
                                BitmapFactory.decodeFile(path)?.asImageBitmap()
                            }
                        } catch (_: Exception) { null }
                    }
                }

                // Shaped + bg-only tint combined bitmap
                val shapedBgTintPreviewBitmap = remember(effectiveShapeName, selectedTintColor, tintIntensity, tintBackgroundOnly, customIconVersion, selectedIconPackName) {
                    if (effectiveShapeName != null && tintBackgroundOnly && selectedTintColor != null) {
                        try {
                            val path = generateShapedBgTintedIcon(context, appInfo.packageName, effectiveShapeName, selectedTintColor!!.toInt(), tintIntensity / 100f)
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    } else null
                }

                // Background-only tint (no shape): generate a bitmap with tinted bg + untinted fg
                val bgTintPreviewBitmap = remember(selectedTintColor, tintIntensity, tintBackgroundOnly, effectiveShapeName, customIconVersion, selectedIconPackName) {
                    if (tintBackgroundOnly && selectedTintColor != null && effectiveShapeName == null) {
                        try {
                            val path = generateBgTintedIcon(context, appInfo.packageName, selectedTintColor!!.toInt(), tintIntensity / 100f)
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    } else null
                }

                // Runtime tint filter for regular (non-bg-only) tint
                val previewTint = if (tintBackgroundOnly) null else selectedTintColor?.let { colorLong ->
                    val intensityAlpha = tintIntensity / 100f
                    ColorFilter.tint(
                        Color(colorLong.toInt()).copy(alpha = intensityAlpha),
                        parseBlendMode("SrcAtop")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    // Centered icon + label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val customIconFile = customIconPath?.let { File(it) }
                        val customIconClipShape = selectedShapeExp?.let { getIconShape(it) }
                        if (showOriginalForDetach) {
                            // Force the original (unmodified) icon in Exp tab.
                            AsyncImage(
                                model = File(appInfo.iconPath),
                                contentDescription = appInfo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        } else if (customIconFile != null && customIconFile.exists()) {
                            // Use version key to force reload when same file path is overwritten
                            val iconBitmap = remember(customIconPath, customIconVersion) {
                                BitmapFactory.decodeFile(customIconFile.absolutePath)?.asImageBitmap()
                            }
                            if (iconBitmap != null) androidx.compose.foundation.Image(
                                bitmap = iconBitmap,
                                contentDescription = appInfo.name,
                                contentScale = if (customIconClipShape != null) ContentScale.Crop else ContentScale.Fit,
                                colorFilter = previewTint,
                                modifier = Modifier
                                    .size(64.dp)
                                    .then(if (customIconClipShape != null) Modifier.clip(customIconClipShape) else Modifier)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        } else if (shapedBgTintPreviewBitmap != null) {
                            // Shape + bg-only tint combined
                            androidx.compose.foundation.Image(
                                bitmap = shapedBgTintPreviewBitmap,
                                contentDescription = appInfo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        } else if (shapeExpPreviewBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = shapeExpPreviewBitmap,
                                contentDescription = appInfo.name,
                                contentScale = ContentScale.Fit,
                                colorFilter = previewTint,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        } else if (bgTintPreviewBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bgTintPreviewBitmap,
                                contentDescription = appInfo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        } else {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(File(currentIconPath))
                                    .memoryCacheKey("${currentIconPath}_${customIconVersion}")
                                    .diskCacheKey("${currentIconPath}_${customIconVersion}")
                                    .build(),
                                contentDescription = appInfo.name,
                                contentScale = if (previewShape != null) ContentScale.Crop else ContentScale.Fit,
                                colorFilter = previewTint,
                                modifier = Modifier
                                    .size(64.dp)
                                    .then(if (previewShape != null) Modifier.clip(previewShape) else Modifier)
                                    .graphicsLayer {
                                        scaleX = previewSizeScale
                                        scaleY = previewSizeScale
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val previewFontFamily = selectedFontId?.let { id ->
                            FontManager.bundledFonts.find { it.id == id }?.fontFamily
                                ?: FontManager.getImportedFonts(context).find { it.id == id }?.fontFamily
                        } ?: FontFamily.Default
                        Text(
                            text = previewLabel,
                            color = Color.White,
                            fontSize = 12.sp * selectedTextSizePercent / 100f,
                            fontFamily = previewFontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer { alpha = previewLabelAlpha }
                        )
                    }

                    // Overlay buttons: Y aligned with icon center (16dp padding + 32dp = 48dp)
                    // Top padding 34dp so button (28dp) center lands at 34+14=48dp
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 34.dp)
                    ) {
                        // Add photo button (right side)
                        Column(
                            modifier = Modifier
                                .align(androidx.compose.ui.BiasAlignment(0.75f, -1f))
                                .defaultMinSize(minWidth = 36.dp)
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add_photo),
                                contentDescription = "Change icon",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Icon",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                        }

                        // Reset icon button (left side, mirrored position)
                        Column(
                            modifier = Modifier
                                .align(androidx.compose.ui.BiasAlignment(-0.75f, -1f))
                                .background(
                                    Color(0xFFFF6B6B).copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Reset all customizations to defaults
                                    customLabel = ""
                                    hideLabel = false
                                    selectedShapeExp = null
                                    selectedTintColor = null
                                    tintIntensity = 100f
                                    tintBackgroundOnly = false
                                    selectedSizePercent = globalIconSizePercent.toFloat().coerceIn(SliderConfigs.perAppIconSize.minValue, SliderConfigs.perAppIconSize.maxValue)
                                    customIconPath = null
                                    selectedIconPackName = null
                                    selectedTextSizePercent = globalIconTextSizePercent.toFloat().coerceIn(SliderConfigs.iconTextSize.minValue, SliderConfigs.iconTextSize.maxValue)
                                    selectedFontId = null
                                    val iconFile = File(getCustomIconsDir(context), "${appInfo.packageName}.png")
                                    if (iconFile.exists()) iconFile.delete()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_reset_icon),
                                contentDescription = "Reset to default",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reset",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Shape, Color, Size & Label icon buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    val iconSize = Modifier.size(24.dp)

                    val selectedBg = Color.White.copy(alpha = 0.1f)
                    val selectedShape = RoundedCornerShape(12.dp)

                    // Shape button (uses EXP/adaptive masking)
                    val shapeColor = when {
                        selectedShapeExp != null -> Color.White
                        expandedSection == 1 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable {
                                expandedSection = if (expandedSection == 1) 0 else 1
                            }
                            .then(if (expandedSection == 1) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = "Icon Shape",
                            modifier = iconSize,
                            tint = shapeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shape",
                            color = shapeColor,
                            fontSize = 11.sp
                        )
                    }

                    // Color tint button
                    val tintColor = when {
                        selectedTintColor != null -> Color.White
                        expandedSection == 2 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable {
                                expandedSection = if (expandedSection == 2) 0 else 2
                            }
                            .then(if (expandedSection == 2) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = "Icon Color Tint",
                            modifier = iconSize,
                            tint = tintColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tint",
                            color = tintColor,
                            fontSize = 11.sp
                        )
                    }

                    // Size button (custom resize icon)
                    val sizeColor = when {
                        selectedSizePercent.roundToInt() != globalIconSizePercent -> Color.White
                        expandedSection == 3 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable {
                                expandedSection = if (expandedSection == 3) 0 else 3
                            }
                            .then(if (expandedSection == 3) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_resize),
                            contentDescription = "Icon Size",
                            modifier = iconSize,
                            tint = sizeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size",
                            color = sizeColor,
                            fontSize = 11.sp
                        )
                    }

                    // Label button
                    val labelColor = when {
                        customLabel.isNotBlank() || hideLabel || selectedTextSizePercent.roundToInt() != globalIconTextSizePercent || selectedFontId != null -> Color.White
                        expandedSection == 4 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable {
                                expandedSection = if (expandedSection == 4) 0 else 4
                            }
                            .then(if (expandedSection == 4) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_label),
                            contentDescription = "Label",
                            modifier = iconSize,
                            tint = labelColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Label",
                            color = labelColor,
                            fontSize = 11.sp
                        )
                    }

                    // Experimental button — placeholder section for upcoming
                    // Total-Launcher-style "free-floating icon" features. Lives
                    // alongside the other customization tabs so the icon
                    // long-press menu stays clean. Issue #48.
                    val experimentalColor = when {
                        expandedSection == 5 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable {
                                expandedSection = if (expandedSection == 5) 0 else 5
                            }
                            .then(if (expandedSection == 5) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_experiment),
                            contentDescription = "Experimental",
                            modifier = iconSize,
                            tint = experimentalColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exp.",
                            color = experimentalColor,
                            fontSize = 11.sp
                        )
                    }

                }

                // Expandable section content (single AnimatedContent for smooth transitions)
                AnimatedContent(
                    targetState = expandedSection,
                    transitionSpec = {
                        (expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                            .togetherWith(shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "sectionContent"
                ) { section ->
                    when (section) {
                        1 -> {
                            // Shape options (EXP adaptive icon masking)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val aospShapes = listOf(IconShapes.CIRCLE, IconShapes.ROUNDED_SQUARE, IconShapes.SQUIRCLE, IconShapes.TEARDROP)
                                    ShapeOptionBox(
                                        isSelected = selectedShapeExp == null,
                                        clipShape = null,
                                        onClick = { selectedShapeExp = null },
                                        showClearIcon = true
                                    )

                                    aospShapes.forEach { shape ->
                                        ShapeOptionBox(
                                            isSelected = selectedShapeExp == shape,
                                            clipShape = getIconShape(shape),
                                            onClick = { selectedShapeExp = shape }
                                        )
                                    }
                                }

                                Text(
                                    "Icon Shape",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        2 -> {
                            // Color tint options
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val chunked = PRESET_COLORS.chunked(5)
                                chunked.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                    ) {
                                        row.forEach { (colorLong, _) ->
                                            val isSelected = selectedTintColor == colorLong
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (colorLong != null) Color(colorLong)
                                                        else Color.White.copy(alpha = 0.1f)
                                                    )
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.border(
                                                                width = 2.dp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                        } else Modifier
                                                    )
                                                    .clickable { selectedTintColor = colorLong },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (colorLong == null) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Clear,
                                                        contentDescription = "No tint",
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(0.dp))
                                ThumbDragHorizontalSlider(
                                    currentValue = tintIntensity,
                                    config = SliderConfigs.tintIntensity,
                                    onValueChange = { tintIntensity = it },
                                    onValueChangeFinished = {}
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Background Only",
                                        color = Color.White.copy(alpha = 0.87f),
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = tintBackgroundOnly,
                                        onCheckedChange = { tintBackgroundOnly = it }
                                    )
                                }

                                // Icon Pack button — shows per-app pack if set, else global pack
                                val displayPackName = selectedIconPackName
                                    ?: com.bearinmind.launcher314.helpers.IconPackManager.getSelectedIconPackName(context)
                                val installedPacks = remember {
                                    com.bearinmind.launcher314.helpers.IconPackManager.getInstalledIconPacks(context)
                                }
                                // Find icon path: match per-app pack name to installed packs, else use global
                                val displayPackIconPath = remember(selectedIconPackName) {
                                    if (selectedIconPackName != null) {
                                        installedPacks.find { it.displayName == selectedIconPackName }?.iconPath
                                    } else {
                                        com.bearinmind.launcher314.helpers.IconPackManager.getSelectedIconPackIconPath(context)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Button(
                                        onClick = { showIconPackBrowser = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.05f),
                                            contentColor = Color.White.copy(alpha = 0.87f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (displayPackIconPath != null) {
                                            AsyncImage(
                                                model = File(displayPackIconPath),
                                                contentDescription = displayPackName,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = displayPackName)
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Size options
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThumbDragHorizontalSlider(
                                    currentValue = selectedSizePercent,
                                    config = SliderConfigs.perAppIconSize,
                                    overflowThreshold = iconSizeOverflowThreshold,
                                    onValueChange = { selectedSizePercent = it },
                                    onValueChangeFinished = {}
                                )
                            }
                        }
                        4 -> {
                            // Label customization
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customLabel,
                                    onValueChange = { customLabel = it },
                                    placeholder = { Text(appInfo.name, color = Color.White.copy(alpha = 0.3f)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        cursorColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val leadingFontFamily = if (selectedFontId != null) {
                                                FontManager.bundledFonts.find { it.id == selectedFontId }?.fontFamily
                                                    ?: FontManager.getImportedFonts(context).find { it.id == selectedFontId }?.fontFamily
                                                    ?: FontManager.getSelectedFontFamily(context) ?: FontFamily.Default
                                            } else FontManager.getSelectedFontFamily(context) ?: FontFamily.Default
                                            IconButton(onClick = { showFontScreen = true }) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Aa", fontSize = 14.sp, fontFamily = leadingFontFamily,
                                                        color = if (selectedFontId != null) Color.White else Color.White.copy(alpha = 0.4f),
                                                        maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                                                        modifier = Modifier.requiredHeight(18.dp).wrapContentHeight(Alignment.CenterVertically, unbounded = true))
                                                    Text("Font", fontSize = 8.sp, color = if (selectedFontId != null) Color.White else Color.White.copy(alpha = 0.4f), fontFamily = leadingFontFamily)
                                                }
                                            }
                                            Box(modifier = Modifier.width(1.dp).height(56.dp).background(Color.White.copy(alpha = 0.2f)))
                                        }
                                    },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.width(1.dp).height(56.dp).background(Color.White.copy(alpha = 0.2f)))
                                            IconButton(onClick = { hideLabel = !hideLabel }) {
                                                Icon(
                                                    painter = painterResource(id = if (hideLabel) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                                    contentDescription = if (hideLabel) "Show label" else "Hide label",
                                                    tint = if (hideLabel) Color.White.copy(alpha = 0.4f) else Color.White
                                                )
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(0.dp))
                                ThumbDragHorizontalSlider(
                                    currentValue = selectedTextSizePercent,
                                    config = SliderConfigs.iconTextSize,
                                    onValueChange = { selectedTextSizePercent = it },
                                    onValueChangeFinished = {}
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Label color picker
                                val labelColors = PRESET_COLORS
                                val chunkedLabelColors = labelColors.chunked(5)
                                chunkedLabelColors.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                    ) {
                                        row.forEach { (colorLong, _) ->
                                            val isSelected = selectedLabelColor == colorLong
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (colorLong != null) Color(colorLong)
                                                        else Color.White.copy(alpha = 0.1f)
                                                    )
                                                    .then(
                                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                                        else Modifier
                                                    )
                                                    .clickable { selectedLabelColor = colorLong },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (colorLong == null) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Clear,
                                                        contentDescription = "Default color",
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(0.dp))

                                ThumbDragHorizontalSlider(
                                    currentValue = labelColorIntensity,
                                    config = SliderConfigs.tintIntensity,
                                    onValueChange = { labelColorIntensity = it },
                                    onValueChangeFinished = {}
                                )
                            }
                        }
                        5 -> {
                            // Experimental section — Total-Launcher-style options
                            // for this app. Detach toggle (Issue #48), an icon
                            // picker that reuses the same PickVisualMedia flow
                            // as the top-right preview button, and a "text as
                            // icon" field so the app can render a glyph / word
                            // instead of an image — still clickable, draggable,
                            // resizable like a normal icon.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Detach from grid",
                                        color = Color.White.copy(alpha = 0.87f),
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = detachedFromGrid,
                                        onCheckedChange = { detachedFromGrid = it }
                                    )
                                }
                                // "Change icon" — Icon card launches the picker
                                // (same path the top-right preview button uses),
                                // Text card focuses the text-as-icon field below.
                                // Cards mirror the small chip styling used by the
                                // popup-menu "Icon" button at the top of the dialog.
                                Text(
                                    text = "Change icon",
                                    color = Color.White.copy(alpha = 0.87f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val iconTextFocus = remember { androidx.compose.ui.focus.FocusRequester() }
                                val iconSelected = expSubSection == 1
                                val textSelected = expSubSection == 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Icon card
                                    Column(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .background(
                                                Color.White.copy(alpha = if (iconSelected) 0.22f else 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                // Just switch to the Icon sub-section;
                                                // the picker is now launched explicitly
                                                // via the "Pick image" chip in the panel
                                                // below, alongside "Use customized" /
                                                // "Use original".
                                                expSubSection = 1
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_add_photo),
                                            contentDescription = "Change icon",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White.copy(alpha = if (iconSelected) 1f else 0.7f)
                                        )
                                        Text(
                                            text = "Icon",
                                            color = Color.White.copy(alpha = if (iconSelected) 1f else 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    // Text card
                                    Column(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .background(
                                                Color.White.copy(alpha = if (textSelected) 0.22f else 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                // Just switch sections — the LaunchedEffect
                                                // below auto-focuses the field once it's
                                                // actually in composition. Calling
                                                // requestFocus() here directly crashes with
                                                // "FocusRequester is not initialized"
                                                // because the field doesn't exist yet on
                                                // this frame.
                                                expSubSection = 2
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.TextFields,
                                            contentDescription = "Use text",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White.copy(alpha = if (textSelected) 1f else 0.7f)
                                        )
                                        Text(
                                            text = "Text",
                                            color = Color.White.copy(alpha = if (textSelected) 1f else 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                // (No auto-focus on the field — auto-focusing
                                // would show the 2dp "focused" border which
                                // reads as a thicker outline than the Label
                                // section's field next to it. The user can
                                // tap the field to focus.)

                                // Icon sub-section panel — visible when Icon
                                // card is selected. Three chips: customized /
                                // original / pick. The first two are radio-
                                // style (one stays selected); "Pick image"
                                // is an action that fires the picker.
                                if (expSubSection == 1) {
                                    val panelBg = Color.White.copy(alpha = 0.05f)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                        ) {
                                            Box(
                                                modifier = Modifier.width(64.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                PanelTail(color = panelBg)
                                            }
                                            Spacer(modifier = Modifier.width(64.dp))
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(panelBg, RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 3 chips stacked top-to-bottom:
                                            //   Use customized / Use original / Pick image.
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconModeChip(
                                                    label = "Use customized",
                                                    selected = !useOriginalIcon,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    useOriginalIcon = false
                                                    iconText = ""
                                                }
                                                IconModeChip(
                                                    label = "Use original",
                                                    selected = useOriginalIcon,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    useOriginalIcon = true
                                                    iconText = ""
                                                }
                                                IconModeChip(
                                                    label = "Pick image",
                                                    selected = false,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    imagePickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Text sub-section panel — only visible when the
                                // Text card is selected. Pick text + color + tint
                                // intensity + font, mirroring the Label section's
                                // controls but writing to the iconText* fields.
                                //
                                // The container holds a small triangle "tail" row
                                // (drawn in the SAME color as the panel) sitting
                                // right above the panel — like the long-press
                                // popup's arrow in AnimatedPopup.kt. No vertical
                                // spacing between tail and panel so they read as
                                // one continuous surface.
                                if (expSubSection == 2) {
                                    val panelBg = Color.White.copy(alpha = 0.05f)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Tail row — mirrors the cards-row layout
                                        // (64dp slots + 10dp gap, centered) so the
                                        // triangle lands directly under the Text
                                        // card.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                        ) {
                                            Spacer(modifier = Modifier.width(64.dp))
                                            Box(
                                                modifier = Modifier.width(64.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                PanelTail(color = panelBg)
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(panelBg, RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = iconText,
                                                onValueChange = { iconText = it },
                                                placeholder = { Text("Enter text", color = Color.White.copy(alpha = 0.3f)) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(iconTextFocus),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                    cursorColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                leadingIcon = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val leadingFontFamily = if (iconTextFontId != null) {
                                                            FontManager.bundledFonts.find { it.id == iconTextFontId }?.fontFamily
                                                                ?: FontManager.getImportedFonts(context).find { it.id == iconTextFontId }?.fontFamily
                                                                ?: FontManager.getSelectedFontFamily(context) ?: FontFamily.Default
                                                        } else FontManager.getSelectedFontFamily(context) ?: FontFamily.Default
                                                        IconButton(onClick = { showIconTextFontScreen = true }) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("Aa", fontSize = 14.sp, fontFamily = leadingFontFamily,
                                                                    color = if (iconTextFontId != null) Color.White else Color.White.copy(alpha = 0.4f),
                                                                    maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                                                                    modifier = Modifier.requiredHeight(18.dp).wrapContentHeight(Alignment.CenterVertically, unbounded = true))
                                                                Text("Font", fontSize = 8.sp, color = if (iconTextFontId != null) Color.White else Color.White.copy(alpha = 0.4f), fontFamily = leadingFontFamily)
                                                            }
                                                        }
                                                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(Color.White.copy(alpha = 0.2f)))
                                                    }
                                                },
                                                trailingIcon = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(Color.White.copy(alpha = 0.2f)))
                                                        IconButton(onClick = { showIconTextSizePicker = true }) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = (iconAsTextSizeSp ?: DEFAULT_ICON_TEXT_SIZE_SP).toString(),
                                                                    fontSize = 12.sp,
                                                                    color = if (iconAsTextSizeSp != null) Color.White else Color.White.copy(alpha = 0.7f),
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    overflow = TextOverflow.Visible,
                                                                    modifier = Modifier
                                                                        .requiredHeight(18.dp)
                                                                        .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
                                                                )
                                                                Text(
                                                                    "Size",
                                                                    fontSize = 8.sp,
                                                                    color = if (iconAsTextSizeSp != null) Color.White else Color.White.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                            // Color picker — same preset list as the label.
                                            PRESET_COLORS.chunked(5).forEach { rowColors ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                                ) {
                                                    rowColors.forEach { (colorLong, _) ->
                                                        val isSelected = iconTextColor == colorLong
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                    if (colorLong != null) Color(colorLong)
                                                                    else Color.White.copy(alpha = 0.1f)
                                                                )
                                                                .then(
                                                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                                                    else Modifier
                                                                )
                                                                .clickable { iconTextColor = colorLong },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (colorLong == null) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Clear,
                                                                    contentDescription = "Default color",
                                                                    tint = Color.White.copy(alpha = 0.5f),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            ThumbDragHorizontalSlider(
                                                currentValue = iconTextColorIntensity,
                                                config = SliderConfigs.tintIntensity,
                                                onValueChange = { iconTextColorIntensity = it },
                                                onValueChangeFinished = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // No section expanded — empty spacer (0 height)
                            Spacer(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Buttons
                val btnPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = btnPadding,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel", fontSize = 13.sp, maxLines = 1)
                    }

                    // Save
                    Button(
                        onClick = {
                            // Generate shaped EXP icon if selected
                            if (selectedShapeExp != null) {
                                try {
                                    generateShapedIcon(context, appInfo.packageName, selectedShapeExp!!)
                                } catch (_: Exception) {}
                            } else {
                                deleteShapedIcon(context, appInfo.packageName)
                            }

                            // Generate bg-tinted icon if background-only tint
                            if (tintBackgroundOnly && selectedTintColor != null) {
                                try {
                                    generateBgTintedIcon(context, appInfo.packageName, selectedTintColor!!.toInt(), tintIntensity / 100f)
                                } catch (_: Exception) {}
                                // Also generate combined shaped+bg-tinted if shape is active
                                if (selectedShapeExp != null) {
                                    try {
                                        generateShapedBgTintedIcon(context, appInfo.packageName, selectedShapeExp!!, selectedTintColor!!.toInt(), tintIntensity / 100f)
                                    } catch (_: Exception) {}
                                } else {
                                    deleteShapedBgTintedIcon(context, appInfo.packageName)
                                }
                            } else {
                                deleteBgTintedIcon(context, appInfo.packageName)
                                deleteShapedBgTintedIcon(context, appInfo.packageName)
                            }

                            val sizeInt = selectedSizePercent.roundToInt()
                            val intensityInt = tintIntensity.roundToInt()
                            val newCustomization = AppCustomization(
                                customLabel = customLabel.takeIf { it.isNotBlank() },
                                hideLabel = hideLabel,
                                customIconPath = customIconPath,
                                iconTintColor = selectedTintColor,
                                iconTintBlendMode = if (selectedTintColor != null) "SrcAtop" else null,
                                iconTintIntensity = if (selectedTintColor != null) intensityInt.takeIf { it != 100 } else null,
                                iconTintBackgroundOnly = tintBackgroundOnly,
                                iconShape = null,
                                iconShapeExp = selectedShapeExp,
                                iconSizePercent = sizeInt.takeIf { it != globalIconSizePercent },
                                iconTextSizePercent = selectedTextSizePercent.roundToInt().takeIf { it != globalIconTextSizePercent },
                                labelFontId = selectedFontId,
                                labelColor = selectedLabelColor,
                                labelColorIntensity = if (selectedLabelColor != null) labelColorIntensity.roundToInt().takeIf { it != 100 } else null,
                                customIconPackName = selectedIconPackName,
                                detachedFromGrid = detachedFromGrid,
                                // Preserve existing position if toggling on/off mid-edit — null
                                // only when the user explicitly re-attaches and we want to
                                // forget the saved coords. For now keep them so toggling back
                                // on returns the icon to its previous free position.
                                detachedX = currentCustomization?.detachedX,
                                detachedY = currentCustomization?.detachedY,
                                iconText = iconText.takeIf { it.isNotBlank() },
                                iconTextColor = iconTextColor,
                                iconTextColorIntensity = if (iconTextColor != null)
                                    iconTextColorIntensity.roundToInt().takeIf { it != 100 } else null,
                                iconTextFontId = iconTextFontId,
                                iconAsTextSizeSp = iconAsTextSizeSp,
                                useOriginalIcon = useOriginalIcon
                            )
                            onSave(newCustomization)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = btnPadding
                    ) {
                        Text("Save", fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    // Font picker fullscreen overlay
    if (showFontScreen) {
        Dialog(
            onDismissRequest = { showFontScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FontsScreen(
                onBack = { showFontScreen = false },
                onFontSelected = { fontId ->
                    selectedFontId = fontId
                    showFontScreen = false
                },
                initialFontId = selectedFontId
            )
        }
    }

    // Text-as-icon SIZE picker — styled like the DeviceAudioEQ
    // "Save Custom Preset" dialog: charcoal #252525 surface, 1dp stroke
    // #333333, 16dp corners, light-grey title, single bordered text field,
    // thin divider, two outlined buttons (Cancel salmon, OK light grey).
    if (showIconTextSizePicker) {
        var pendingSize by remember(showIconTextSizePicker) {
            mutableStateOf((iconAsTextSizeSp ?: DEFAULT_ICON_TEXT_SIZE_SP).toString())
        }
        Dialog(onDismissRequest = { showIconTextSizePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF252525),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Text size",
                        color = Color(0xFFE2E2E2),
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = pendingSize,
                        onValueChange = { new ->
                            // Allow only digits, up to 3 chars (max 999 sp).
                            if (new.all { it.isDigit() } && new.length <= 3) pendingSize = new
                        },
                        placeholder = {
                            Text(
                                "e.g. 30",
                                color = Color(0xFF888888),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF555555),
                            unfocusedBorderColor = Color(0xFF555555),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.White),
                        trailingIcon = {
                            Text(
                                "sp",
                                color = Color(0xFF888888),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF444444))
                            .padding(bottom = 0.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showIconTextSizePicker = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFEF9A9A)
                            )
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                iconAsTextSizeSp = pendingSize.toIntOrNull()?.takeIf { it in 1..999 }
                                showIconTextSizePicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFDDDDDD)
                            )
                        ) {
                            Text("OK", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Icon-text font picker — separate from the label font so the user can
    // style text-as-icon independently of the label below.
    if (showIconTextFontScreen) {
        Dialog(
            onDismissRequest = { showIconTextFontScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FontsScreen(
                onBack = { showIconTextFontScreen = false },
                onFontSelected = { fontId ->
                    iconTextFontId = fontId
                    showIconTextFontScreen = false
                },
                initialFontId = iconTextFontId
            )
        }
    }

    // Icon pack picker — select a pack and apply its icon to THIS app only
    if (showIconPackBrowser) {
        Dialog(
            onDismissRequest = { showIconPackBrowser = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PerAppIconPackPicker(
                context = context,
                packageName = appInfo.packageName,
                onIconApplied = { path, packName ->
                    customIconPath = path.ifEmpty { null }
                    selectedIconPackName = packName
                    customIconVersion++
                    showIconPackBrowser = false
                },
                onDismiss = { showIconPackBrowser = false }
            )
        }
    }
}

/**
 * Per-app icon pack picker — same UI as the settings IconPacksScreen,
 * but selecting a pack applies its icon to only the specified app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerAppIconPackPicker(
    context: Context,
    packageName: String,
    onIconApplied: (path: String, packName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }

    val installedPacks = remember { com.bearinmind.launcher314.helpers.IconPackManager.getInstalledIconPacks(context) }
    val filteredPacks = remember(searchQuery, installedPacks) {
        if (searchQuery.isBlank()) installedPacks
        else installedPacks.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }
    val showSystemIcons = searchQuery.isBlank() ||
        "system icons".contains(searchQuery, ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search bar — exact same style as IconPacksScreen
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search Icon Packs", color = Color.White.copy(alpha = 0.6f))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search"
                            )
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

            // Pack list — exact same layout as IconPacksScreen
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
            ) {
                // System Icons (default) — reset to system icon for this app
                if (showSystemIcons) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable {
                                    // Delete custom icon and all cached variants
                                    val iconFile = File(getCustomIconsDir(context), "$packageName.png")
                                    if (iconFile.exists()) iconFile.delete()
                                    // Delete icon pack cache for this app so it falls back to system
                                    val packCacheFile = File(context.cacheDir, "icon_pack_cache/$packageName.png")
                                    if (packCacheFile.exists()) packCacheFile.delete()
                                    // Clear shaped caches
                                    listOf("global_shaped_icons", "bg_color_shaped_icons", "shaped_exp_icons",
                                        "shaped_bg_tinted_icons", "bg_tinted_icons", "foreground_icons").forEach { dir ->
                                        File(context.filesDir, dir).listFiles()?.filter {
                                            it.name.startsWith(packageName)
                                        }?.forEach { it.delete() }
                                    }
                                    File(context.cacheDir, "app_icons").listFiles()?.filter {
                                        it.name.startsWith(packageName)
                                    }?.forEach { it.delete() }
                                    android.widget.Toast.makeText(context, "Reset to system icon", android.widget.Toast.LENGTH_SHORT).show()
                                    onIconApplied("", null)
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = Color(0xFF3A3A3A),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = "System Icons (Default)",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.87f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }

                items(filteredPacks.size) { index ->
                    val pack = filteredPacks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                isLoading = true
                                loadingMessage = "Applying ${pack.displayName}..."
                                scope.launch(Dispatchers.IO) {
                                    try {
                                    val pm = context.packageManager

                                    // Use EXACTLY the same approach as global cacheIconPackIcons:
                                    // just call it for this single package
                                    val appFilterMap = com.bearinmind.launcher314.helpers.IconPackManager.parseAppFilter(context, pack.packageName)
                                    val iconPackResources = pm.getResourcesForApplication(pack.packageName)

                                    // Query all launcher activities (same as cacheIconPackIcons)
                                    val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                                    }
                                    val resolveInfoList = pm.queryIntentActivities(launchIntent, 0)

                                    // Find ALL activities for this package (some apps have multiple)
                                    val matchingActivities = resolveInfoList.filter {
                                        it.activityInfo.packageName == packageName
                                    }

                                    var found = false
                                    for (ri in matchingActivities) {
                                        val activityInfo = ri.activityInfo
                                        val activityName = activityInfo.name
                                        val componentInfo = "ComponentInfo{$packageName/$activityName}"

                                        // Try exact match first, then prefix match
                                        var drawableName = appFilterMap[componentInfo]
                                        if (drawableName == null) {
                                            val prefix = "ComponentInfo{$packageName/"
                                            val matchKey = appFilterMap.keys.firstOrNull { it.startsWith(prefix) }
                                            if (matchKey != null) drawableName = appFilterMap[matchKey]
                                        }

                                        if (drawableName == null) continue

                                        val drawableResId = iconPackResources.getIdentifier(drawableName, "drawable", pack.packageName)
                                        if (drawableResId == 0) continue

                                        @Suppress("DEPRECATION")
                                        val drawable = iconPackResources.getDrawable(drawableResId, null) ?: continue

                                        val bitmap = com.bearinmind.launcher314.data.drawableToBitmap(drawable)

                                        // Save to icon_pack_cache — exact same location as global cacheIconPackIcons
                                        val cacheDir = File(context.cacheDir, "icon_pack_cache")
                                        if (!cacheDir.exists()) cacheDir.mkdirs()
                                        val cacheFile = File(cacheDir, "$packageName.png")
                                        com.bearinmind.launcher314.data.saveBitmapToFile(bitmap, cacheFile)
                                        bitmap.recycle()

                                        // Remove custom icon so hasCustomIcon=false
                                        val customFile = File(getCustomIconsDir(context), "$packageName.png")
                                        if (customFile.exists()) customFile.delete()

                                        // Clear ALL derived caches for this package so they regenerate
                                        listOf("app_icons").forEach { dir ->
                                            File(context.cacheDir, dir).listFiles()?.filter {
                                                it.name.startsWith(packageName)
                                            }?.forEach { it.delete() }
                                        }
                                        listOf("global_shaped_icons", "bg_color_shaped_icons",
                                            "shaped_exp_icons", "shaped_bg_tinted_icons",
                                            "bg_tinted_icons", "foreground_icons").forEach { dir ->
                                            File(context.filesDir, dir).listFiles()?.filter {
                                                it.name.startsWith(packageName)
                                            }?.forEach { it.delete() }
                                        }

                                        found = true
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            android.widget.Toast.makeText(context, "Applied $drawableName from ${pack.displayName}", android.widget.Toast.LENGTH_SHORT).show()
                                            onIconApplied("", pack.displayName)
                                        }
                                        break
                                    }

                                    if (found) return@launch
                                    } catch (e: Exception) {
                                        android.util.Log.e("PerAppIconPack", "Error: ${e.message}", e)
                                    }

                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "No icon found for this app in ${pack.displayName}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color(0xFF3A3A3A),
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = pack.displayName,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.87f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Icon pack preview icon (tappable to open the app)
                        if (pack.iconPath.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pack.packageName)
                                        if (launchIntent != null) context.startActivity(launchIntent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = File(pack.iconPath),
                                    contentDescription = "Open ${pack.displayName}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(40.dp)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }

                if (installedPacks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No icon packs installed.\nInstall icon packs from the Play Store or F-Droid that support ADW/Nova launcher themes.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingMessage, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Find drawable name for a package from appfilter map with fallback matching */
private fun findDrawableForApp(
    appFilterMap: Map<String, String>,
    pkgName: String,
    activityName: String
): String? {
    // Exact ComponentInfo match
    val componentInfo = "ComponentInfo{$pkgName/$activityName}"
    appFilterMap[componentInfo]?.let { return it }
    // Package-name prefix match
    val prefix = "ComponentInfo{$pkgName/"
    appFilterMap.keys.firstOrNull { it.startsWith(prefix) }?.let {
        return appFilterMap[it]
    }
    return null
}

@Composable
private fun ShapeOptionBox(
    isSelected: Boolean,
    clipShape: Shape?,
    onClick: () -> Unit,
    label: String? = null,
    showClearIcon: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showClearIcon) {
            Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = "None",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        } else if (label != null) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (clipShape != null) Modifier.clip(clipShape)
                        else Modifier
                    )
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}
