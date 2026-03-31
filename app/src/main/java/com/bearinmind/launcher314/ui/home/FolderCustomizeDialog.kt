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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearinmind.launcher314.ui.settings.FontsScreen
import android.content.Context
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.ui.res.painterResource
import com.bearinmind.launcher314.R
import com.bearinmind.launcher314.data.AppCustomization
import com.bearinmind.launcher314.helpers.IconShapes
import com.bearinmind.launcher314.helpers.getIconShape
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
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
fun FolderCustomizeDialog(
    context: Context,
    folderName: String,
    folderId: String,
    currentCustomization: AppCustomization?,
    globalIconSizePercent: Int = 100,
    globalIconTextSizePercent: Int = 100,
    iconSizeOverflowThreshold: Float = 125f,
    globalIconShape: String? = null,
    globalIconBgColor: Int? = null,
    globalIconBgIntensity: Int = 100,
    previewAppIcons: List<String> = emptyList(),
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
    var selectedSizePercent by remember { mutableStateOf(
        (currentCustomization?.iconSizePercent ?: globalIconSizePercent).toFloat().coerceIn(SliderConfigs.perAppIconSize.minValue, SliderConfigs.perAppIconSize.maxValue)
    ) }
    var selectedShapeExp by remember { mutableStateOf(currentCustomization?.iconShapeExp) }
    var selectedTextSizePercent by remember { mutableStateOf(
        (currentCustomization?.iconTextSizePercent ?: globalIconTextSizePercent).toFloat().coerceIn(SliderConfigs.iconTextSize.minValue, SliderConfigs.iconTextSize.maxValue)
    ) }
    var selectedFontId by remember { mutableStateOf(currentCustomization?.labelFontId) }
    var showFontScreen by remember { mutableStateOf(false) }

    var expandedSection by remember { mutableStateOf(0) } // 0=none, 1=shape, 2=tint, 3=size, 4=label

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
                // Live preview — folder icon with label
                val previewLabel = if (customLabel.isNotBlank()) customLabel else folderName
                val previewLabelAlpha by animateFloatAsState(
                    targetValue = if (hideLabel) 0f else 1f,
                    animationSpec = tween(durationMillis = 250),
                    label = "previewLabelAlpha"
                )
                val effectiveShapeName = selectedShapeExp ?: globalIconShape
                val previewSizeScale = selectedSizePercent / globalIconSizePercent.toFloat()
                val folderClipShape = if (effectiveShapeName != null) getIconShape(effectiveShapeName) else RoundedCornerShape(12.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val folderBoxSize = (48 * previewSizeScale).dp
                    val borderColor = if (selectedTintColor != null) {
                        Color(selectedTintColor!!).copy(alpha = (tintIntensity / 100f).coerceIn(0f, 1f))
                    } else if (globalIconBgColor != null) {
                        Color(globalIconBgColor).copy(alpha = (globalIconBgIntensity / 100f).coerceIn(0f, 1f))
                    } else Color.White.copy(alpha = 0.3f)
                    val resolvedFolderClip = folderClipShape ?: RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier.size(folderBoxSize),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background layer
                        Box(modifier = Modifier.matchParentSize().background(Color(0xFF1A1A1A), resolvedFolderClip))
                        // Content layer — inset by border width and clipped so icons stay inside outline
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(1.dp)
                                .graphicsLayer { clip = true; shape = resolvedFolderClip },
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewAppIcons.isNotEmpty()) {
                                val contentSize = folderBoxSize - 2.dp // account for border inset
                                val padding = contentSize * 0.12f
                                val spacing = contentSize * 0.05f
                                val miniIconSize = (contentSize - padding * 2 - spacing) / 2
                                val miniClip = if (globalIconShape != null) getIconShape(globalIconShape) ?: RoundedCornerShape(miniIconSize * 0.2f) else RoundedCornerShape(miniIconSize * 0.2f)
                                Column(
                                    modifier = Modifier.padding(padding),
                                    verticalArrangement = Arrangement.spacedBy(spacing)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                        previewAppIcons.getOrNull(0)?.let { path ->
                                            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(miniIconSize).clip(miniClip))
                                        } ?: Spacer(Modifier.size(miniIconSize))
                                        previewAppIcons.getOrNull(1)?.let { path ->
                                            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(miniIconSize).clip(miniClip))
                                        } ?: Spacer(Modifier.size(miniIconSize))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                        previewAppIcons.getOrNull(2)?.let { path ->
                                            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(miniIconSize).clip(miniClip))
                                        } ?: Spacer(Modifier.size(miniIconSize))
                                        previewAppIcons.getOrNull(3)?.let { path ->
                                            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(miniIconSize).clip(miniClip))
                                        } ?: Spacer(Modifier.size(miniIconSize))
                                    }
                                }
                            }
                        }
                        // Border overlay — on top
                        Box(modifier = Modifier.matchParentSize().border(1.dp, borderColor, resolvedFolderClip))
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    val previewFontFamily = if (selectedFontId != null) {
                        FontManager.bundledFonts.find { it.id == selectedFontId }?.fontFamily
                            ?: FontManager.getImportedFonts(context).find { it.id == selectedFontId }?.fontFamily
                            ?: FontFamily.Default
                    } else FontFamily.Default
                    val previewTextSize = (selectedTextSizePercent / 100f * 12f).sp

                    Text(
                        text = previewLabel,
                        fontSize = previewTextSize,
                        fontFamily = previewFontFamily,
                        color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = previewLabelAlpha }
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Section buttons — Shape, Tint, Size, Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    val iconSize = Modifier.size(24.dp)
                    val selectedBg = Color.White.copy(alpha = 0.1f)
                    val selectedShape = RoundedCornerShape(12.dp)

                    // Shape button
                    val shapeColor = when {
                        selectedShapeExp != null -> Color.White
                        expandedSection == 1 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable { expandedSection = if (expandedSection == 1) 0 else 1 }
                            .then(if (expandedSection == 1) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Category, contentDescription = "Shape", modifier = iconSize, tint = shapeColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Shape", color = shapeColor, fontSize = 11.sp)
                    }

                    // Tint button
                    val tintColor = when {
                        selectedTintColor != null -> Color.White
                        expandedSection == 2 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable { expandedSection = if (expandedSection == 2) 0 else 2 }
                            .then(if (expandedSection == 2) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Outline", modifier = iconSize, tint = tintColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Outline", color = tintColor, fontSize = 11.sp)
                    }

                    // Size button
                    val sizeColor = when {
                        selectedSizePercent.roundToInt() != globalIconSizePercent -> Color.White
                        expandedSection == 3 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape).clickable { expandedSection = if (expandedSection == 3) 0 else 3 }
                            .then(if (expandedSection == 3) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(painterResource(id = R.drawable.ic_resize), contentDescription = "Size", modifier = iconSize, tint = sizeColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Size", color = sizeColor, fontSize = 11.sp)
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
                            .clip(selectedShape).clickable { expandedSection = if (expandedSection == 4) 0 else 4 }
                            .then(if (expandedSection == 4) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(painterResource(id = R.drawable.ic_label), contentDescription = "Label", modifier = iconSize, tint = labelColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Label", color = labelColor, fontSize = 11.sp)
                    }
                }

                // Expandable section content
                AnimatedContent(
                    targetState = expandedSection,
                    transitionSpec = {
                        (expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                            .togetherWith(shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "folderSectionContent"
                ) { section ->
                    when (section) {
                        1 -> {
                            // Shape options
                            Column(
                                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val aospShapes = listOf(IconShapes.CIRCLE, IconShapes.ROUNDED_SQUARE, IconShapes.SQUIRCLE, IconShapes.TEARDROP)
                                    // Default (no override)
                                    Box(
                                        modifier = Modifier.size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .then(if (selectedShapeExp == null) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                                            .clickable { selectedShapeExp = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "Default", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                    aospShapes.forEach { shape ->
                                        val isSelected = selectedShapeExp == shape
                                        Box(
                                            modifier = Modifier.size(28.dp)
                                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                                                .clickable { selectedShapeExp = shape },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier.size(22.dp)
                                                    .then(getIconShape(shape)?.let { Modifier.clip(it) } ?: Modifier)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                                Text("Folder Shape", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                        2 -> {
                            // Tint options
                            Column(
                                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
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
                                                modifier = Modifier.size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (colorLong != null) Color(colorLong) else Color.White.copy(alpha = 0.1f))
                                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                                                    .clickable { selectedTintColor = colorLong },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (colorLong == null) {
                                                    Icon(Icons.Outlined.Clear, contentDescription = "No tint", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
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
                            }
                        }
                        3 -> {
                            // Size options
                            Column(
                                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
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
                                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customLabel,
                                    onValueChange = { customLabel = it },
                                    placeholder = { Text(folderName, color = Color.White.copy(alpha = 0.3f)) },
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

                                Button(
                                    onClick = { showFontScreen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = selectedFontName, fontFamily = selectedFontFamily, maxLines = 1)
                                }

                                Text("Font", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Hide Label", color = Color.White.copy(alpha = 0.87f), fontSize = 14.sp)
                                    Switch(checked = hideLabel, onCheckedChange = { hideLabel = it })
                                }
                            }
                        }
                        else -> Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Buttons
                val btnPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = btnPadding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Cancel", fontSize = 13.sp, maxLines = 1) }

                    // Reset
                    OutlinedButton(
                        onClick = { onReset(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = btnPadding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A))
                    ) { Text("Reset", fontSize = 13.sp, maxLines = 1) }

                    Button(
                        onClick = {
                            val sizeInt = selectedSizePercent.roundToInt()
                            val intensityInt = tintIntensity.roundToInt()
                            val newCustomization = AppCustomization(
                                customLabel = customLabel.takeIf { it.isNotBlank() },
                                hideLabel = hideLabel,
                                iconTintColor = selectedTintColor,
                                iconTintBlendMode = if (selectedTintColor != null) "SrcAtop" else null,
                                iconTintIntensity = if (selectedTintColor != null) intensityInt.takeIf { it != 100 } else null,
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
                    ) { Text("Save", fontSize = 13.sp, maxLines = 1) }
                }
            }
        }
    }

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
