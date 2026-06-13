package com.bearinmind.launcher314.ui.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bearinmind.launcher314.R
import com.bearinmind.launcher314.data.WIDGET_MAX_PADDING_DP
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlin.math.roundToInt

/**
 * Per-widget customize popup — same visual style as the per-app
 * AppCustomizeDialog: live preview header, a row of section chips
 * (Widget / Stack) that expand their options, and a Cancel / Save
 * row. Holds local state and only commits on Save; double-tapping
 * a slider resets it to the global value (= follow the global
 * setting, stored as null on the widget).
 */
@Composable
fun WidgetCustomizeDialog(
    initialFontScalePercent: Int?,
    initialPaddingPercent: Int?,
    globalFontScalePercent: Int,
    globalPaddingPercent: Int,
    initialCornerRadiusPercent: Int?,
    // EFFECTIVE global roundness (0 when the global toggle is off) — used as
    // the fallback display value and the double-tap reset target.
    globalCornerRadiusPercent: Int,
    onSave: (fontScalePercent: Int?, paddingPercent: Int?, cornerRadiusPercent: Int?) -> Unit,
    onDismiss: () -> Unit,
    // Stack section — only rendered for stacked widgets (isStack). The
    // toggle gates auto-advance; the slider sets the flip interval seconds.
    isStack: Boolean = false,
    initialSlideshowEnabled: Boolean = false,
    initialSlideshowIntervalSec: Int = 5,
    onSaveSlideshow: (enabled: Boolean, intervalSec: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var fontScale by remember {
        mutableFloatStateOf((initialFontScalePercent ?: globalFontScalePercent).toFloat())
    }
    var spacing by remember {
        mutableFloatStateOf((initialPaddingPercent ?: globalPaddingPercent).toFloat())
    }
    var cornerPercent by remember {
        mutableFloatStateOf((initialCornerRadiusPercent ?: globalCornerRadiusPercent).toFloat())
    }
    // Per-widget rounded-corners on/off (mirrors the global widgets-menu toggle).
    // ON = use the slider's radius; OFF = square (0%). Initialized from whether
    // this widget currently resolves to any rounding.
    var roundedEnabled by remember {
        mutableStateOf((initialCornerRadiusPercent ?: globalCornerRadiusPercent) > 0)
    }
    var slideshowEnabled by remember { mutableStateOf(initialSlideshowEnabled) }
    var slideshowInterval by remember {
        mutableFloatStateOf(initialSlideshowIntervalSec.toFloat())
    }
    // 0 = none, 1 = Widget, 2 = Stack — same toggle pattern as the app dialog.
    var expandedSection by remember { mutableIntStateOf(0) }

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
                // Live preview header — same sample-widget-with-"+"-markers
                // preview as the widgets screen's dropdown menu, driven by THIS
                // dialog's slider state so it updates live while dragging.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    // Corners track the dialog's own slider + on/off toggle (live
                    // preview). Same mapping as the real widget clip: percent of the
                    // shorter side, 100% = full pill (Compose percent shapes: 50 = pill).
                    val effPreviewCorner = if (roundedEnabled) cornerPercent else 0f
                    val previewCornerShape = RoundedCornerShape((effPreviewCorner / 2f).roundToInt())
                    val previewPaddingDp = spacing / 100f * WIDGET_MAX_PADDING_DP
                    val previewTextScale = fontScale / 100f
                    // "+" markers sit at grid intersections. Inset the widget
                    // area by HALF the marker box so the widget's edge lands
                    // exactly ON the marker centers — at 0% spacing the widget
                    // touches the "+" intersections, matching how a real widget
                    // fills its grid cells at 0%.
                    val markerSize = 12.dp
                    val markerHalf = markerSize / 2
                    val widgetInset = markerHalf
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .height(80.dp + markerHalf * 2)
                    ) {
                        val markerColor = Color.White.copy(alpha = 0.5f)
                        val markerFontSize = 10.sp
                        // Top row
                        Box(modifier = Modifier.align(Alignment.TopStart).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }
                        Box(modifier = Modifier.align(Alignment.TopCenter).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }
                        // Bottom row
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(markerSize), contentAlignment = Alignment.Center) {
                            Text("+", color = markerColor, fontSize = markerFontSize)
                        }

                        // Widget area: centered between markers
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(widgetInset)
                        ) {
                            // Scale icon and text proportionally as spacing shrinks the widget
                            val spacingScale = (1f - spacing / 100f * 0.5f).coerceIn(0.3f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(previewPaddingDp.dp)
                                    .clip(previewCornerShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val appIcon = remember {
                                    context.packageManager.getApplicationIcon(context.packageName)
                                }
                                val scaledIconSize = (36f * spacingScale).dp
                                val innerPad = (8f * spacingScale).dp
                                val gapAfterIcon = (6f * spacingScale).dp
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = innerPad)
                                ) {
                                    Image(
                                        painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(scaledIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(gapAfterIcon))
                                    Text(
                                        text = "Launcher314",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = (13f * previewTextScale * spacingScale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Section chips — same icon-above-label pattern as the app
                // dialog's Shape / Tint / Size / Label / Exp row.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    val iconSize = Modifier.size(24.dp)
                    val selectedBg = Color.White.copy(alpha = 0.1f)
                    val selectedShape = RoundedCornerShape(12.dp)

                    // Widget section (single layer icon)
                    val widgetColor = when {
                        initialFontScalePercent != null || initialPaddingPercent != null ||
                            initialCornerRadiusPercent != null -> Color.White
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
                            painter = painterResource(id = R.drawable.ic_widget_single),
                            contentDescription = "Widget options",
                            modifier = iconSize,
                            tint = widgetColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Widget",
                            color = widgetColor,
                            fontSize = 11.sp
                        )
                    }

                    // Stack section (two-layer icon) — ALWAYS visible. For
                    // non-stacked widgets it's crossed out (diagonal slash) and
                    // inert, signalling the section exists but isn't in use.
                    val stackColor = when {
                        !isStack -> Color.White.copy(alpha = 0.25f)
                        slideshowEnabled -> Color.White
                        expandedSection == 2 -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(selectedShape)
                            .then(
                                if (isStack) Modifier.clickable {
                                    expandedSection = if (expandedSection == 2) 0 else 2
                                } else Modifier
                            )
                            .then(if (expandedSection == 2) Modifier.background(selectedBg, selectedShape) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(modifier = iconSize) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stack),
                                contentDescription = "Stack options",
                                modifier = Modifier.fillMaxSize(),
                                tint = stackColor
                            )
                            if (!isStack) {
                                // Diagonal "not in use" slash across the icon
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = stackColor,
                                        start = Offset(size.width * 0.08f, size.height * 0.08f),
                                        end = Offset(size.width * 0.92f, size.height * 0.92f),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stack",
                            color = stackColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Expandable section content — same AnimatedContent transition
                // (expand/shrink + fade) and rounded 5%-white panel container as
                // the per-app customize dialog's Shape/Tint/Size/Label sections.
                AnimatedContent(
                    targetState = expandedSection,
                    transitionSpec = {
                        (expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                            .togetherWith(shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "widgetSectionContent"
                ) { section ->
                    when (section) {
                        1 -> {
                            // Widget options: text size + spacing sliders
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ThumbDragHorizontalSlider(
                                        currentValue = fontScale,
                                        config = SliderConfigs.widgetTextSize,
                                        onValueChange = { fontScale = it },
                                        onValueChangeFinished = { },
                                        onDoubleTap = { fontScale = globalFontScalePercent.toFloat() }
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ThumbDragHorizontalSlider(
                                        currentValue = spacing,
                                        config = SliderConfigs.widgetPadding,
                                        onValueChange = { spacing = it },
                                        onValueChangeFinished = { },
                                        onDoubleTap = { spacing = globalPaddingPercent.toFloat() }
                                    )
                                }
                                // Corner Roundness slider — always visible so the %
                                // reads even when the toggle is off; just disabled
                                // (greyed) until rounded corners is turned on.
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ThumbDragHorizontalSlider(
                                        currentValue = cornerPercent,
                                        config = SliderConfigs.cornerRoundness,
                                        enabled = roundedEnabled,
                                        onValueChange = { cornerPercent = it },
                                        onValueChangeFinished = { },
                                        onDoubleTap = { cornerPercent = globalCornerRadiusPercent.toFloat() }
                                    )
                                }
                                // Rounded corners on/off — sits BELOW the slider.
                                // OFF squares this widget; ON enables the slider above.
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Rounded corners",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    Switch(
                                        checked = roundedEnabled,
                                        onCheckedChange = {
                                            roundedEnabled = it
                                            // Turning ON with a 0 slider would still
                                            // be square — seed a sensible radius from
                                            // the global default (or 50%).
                                            if (it && cornerPercent <= 0f) {
                                                cornerPercent =
                                                    (if (globalCornerRadiusPercent > 0) globalCornerRadiusPercent else 50).toFloat()
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .scale(0.7f)
                                    )
                                }
                            }
                        }
                        2 -> if (isStack) {
                            // Stack options: slideshow toggle + interval
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ThumbDragHorizontalSlider(
                                        currentValue = slideshowInterval,
                                        config = SliderConfigs.widgetSlideshowInterval,
                                        enabled = slideshowEnabled,
                                        onValueChange = { slideshowInterval = it },
                                        onValueChangeFinished = { }
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Widget slideshow",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    Checkbox(
                                        checked = slideshowEnabled,
                                        onCheckedChange = { slideshowEnabled = it },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .scale(0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Buttons — same layout/colors as the app customize dialog
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
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel", fontSize = 13.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            val fs = fontScale.roundToInt()
                            val sp = spacing.roundToInt()
                            // OFF = explicit square (0%); ON = the slider value.
                            val cr = if (roundedEnabled) cornerPercent.roundToInt() else 0
                            if (isStack) {
                                onSaveSlideshow(slideshowEnabled, slideshowInterval.roundToInt())
                            }
                            onSave(
                                if (fs == globalFontScalePercent) null else fs,
                                if (sp == globalPaddingPercent) null else sp,
                                if (cr == globalCornerRadiusPercent) null else cr
                            )
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
}
