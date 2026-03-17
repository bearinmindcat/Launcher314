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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var selectedShapeExp by remember { mutableStateOf(currentCustomization?.iconShapeExp) }
    var customIconPath by remember { mutableStateOf(currentCustomization?.customIconPath) }
    var selectedTextSizePercent by remember { mutableStateOf(
        (currentCustomization?.iconTextSizePercent ?: globalIconTextSizePercent).toFloat().coerceIn(SliderConfigs.iconTextSize.minValue, SliderConfigs.iconTextSize.maxValue)
    ) }
    var selectedFontId by remember { mutableStateOf(currentCustomization?.labelFontId) }
    var showFontScreen by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (original != null) {
                    // Center-crop to square, scale to 192x192
                    val size = minOf(original.width, original.height)
                    val x = (original.width - size) / 2
                    val y = (original.height - size) / 2
                    val cropped = Bitmap.createBitmap(original, x, y, size, size)
                    val scaled = Bitmap.createScaledBitmap(cropped, 192, 192, true)
                    val outFile = File(getCustomIconsDir(context), "${appInfo.packageName}.png")
                    saveBitmapToFile(scaled, outFile)
                    customIconPath = outFile.absolutePath
                    if (cropped !== original) cropped.recycle()
                    scaled.recycle()
                    original.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    // Which expandable section is open (mutually exclusive)
    var expandedSection by remember { mutableStateOf(0) } // 0=none, 1=shape(EXP), 2=tint, 3=size, 4=icon

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
                val previewSizeScale = selectedSizePercent / globalIconSizePercent.toFloat()

                // Effective shape: per-app overrides global
                val effectiveShapeName = selectedShapeExp ?: globalIconShape

                // Generate shaped EXP preview bitmap (plain shape, no tint baked in)
                val shapeExpPreviewBitmap = remember(effectiveShapeName, globalIconBgColor) {
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
                val shapedBgTintPreviewBitmap = remember(effectiveShapeName, selectedTintColor, tintIntensity, tintBackgroundOnly) {
                    if (effectiveShapeName != null && tintBackgroundOnly && selectedTintColor != null) {
                        try {
                            val path = generateShapedBgTintedIcon(context, appInfo.packageName, effectiveShapeName, selectedTintColor!!.toInt(), tintIntensity / 100f)
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    } else null
                }

                // Background-only tint (no shape): generate a bitmap with tinted bg + untinted fg
                val bgTintPreviewBitmap = remember(selectedTintColor, tintIntensity, tintBackgroundOnly, effectiveShapeName) {
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
                        if (customIconFile != null && customIconFile.exists()) {
                            AsyncImage(
                                model = customIconFile,
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
                                model = File(appInfo.iconPath),
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
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(0.dp))
                                ThumbDragHorizontalSlider(
                                    currentValue = selectedTextSizePercent,
                                    config = SliderConfigs.iconTextSize,
                                    onValueChange = { selectedTextSizePercent = it },
                                    onValueChangeFinished = {}
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val selectedFontName = if (selectedFontId != null) {
                                    FontManager.bundledFonts.find { it.id == selectedFontId }?.displayName
                                        ?: FontManager.getImportedFonts(context).find { it.id == selectedFontId }?.displayName
                                        ?: "System Font (Default)"
                                } else "System Font (Default)"
                                val selectedFontFamily = if (selectedFontId != null) {
                                    FontManager.bundledFonts.find { it.id == selectedFontId }?.fontFamily
                                        ?: FontManager.getImportedFonts(context).find { it.id == selectedFontId }?.fontFamily
                                        ?: FontFamily.Default
                                } else FontFamily.Default

                                // Font button — opens full font picker screen
                                Button(
                                    onClick = { showFontScreen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = selectedFontName,
                                        fontFamily = selectedFontFamily,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = "Font",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Hide Label",
                                        color = Color.White.copy(alpha = 0.87f),
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = hideLabel,
                                        onCheckedChange = { hideLabel = it }
                                    )
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
                                labelFontId = selectedFontId
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
