package com.bearinmind.launcher314.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlin.math.roundToInt

/**
 * Resize control panel shown during folder Resize mode.
 *
 * Provides the same thumb-drag sliders used everywhere else for grid sizing
 * (SliderConfigs.folderColumns / folderRows) plus Reset / Done. Anchors to
 * whichever screen edge is OPPOSITE the folder popup so it never overlaps the
 * card. The drag-resize handles around the popup (FolderResizeOverlay) stay
 * active in parallel, so the user can drag the popup pixel-size while the
 * sliders adjust row / column counts.
 */
@Composable
fun FolderResizePanel(
    currentColumns: Int,
    currentRows: Int,
    minGrid: Int = 1,
    maxGrid: Int = 8,
    progress: Float = 1f,
    interactive: Boolean = true,
    // When the folder popup is in the TOP half of the screen, anchor the
    // panel to the BOTTOM (and vice-versa) so it sits OPPOSITE the popup and
    // never overlaps the folder card.
    anchorBottom: Boolean = false,
    onColumnsChange: (Int) -> Unit,
    onRowsChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    // Full-screen positioning container (no background / no pointer handler,
    // so taps on empty space fall through to the close-backdrop below). The
    // card aligns to the side opposite the popup.
    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .zIndex(15f)
    ) {
        Box(modifier = Modifier
            .align(if (anchorBottom) Alignment.BottomCenter else Alignment.TopCenter)
            // Compact fixed width — keeps the panel small (close to the old
            // stepper size) while giving the sliders a bounded width to lay
            // out in (they fillMaxWidth internally).
            .width(230.dp)
            // Fade + slide from the anchored edge on enter / exit, in sync
            // with the outline's collapse into the popup card.
            .graphicsLayer {
                alpha = progress
                translationY = (if (anchorBottom) 16f else -16f) * (1f - progress)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                    // Absorb every touch inside the panel card so taps that
                    // miss a control don't fall through to the close-backdrop.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Same thumb-drag sliders as App Grid Columns / Rows.
                ThumbDragHorizontalSlider(
                    currentValue = currentColumns.coerceIn(minGrid, maxGrid).toFloat(),
                    config = SliderConfigs.folderColumns,
                    enabled = interactive,
                    onValueChange = {
                        if (interactive) onColumnsChange(it.roundToInt().coerceIn(minGrid, maxGrid))
                    },
                    onValueChangeFinished = {}
                )
                ThumbDragHorizontalSlider(
                    currentValue = currentRows.coerceIn(minGrid, maxGrid).toFloat(),
                    config = SliderConfigs.folderRows,
                    enabled = interactive,
                    onValueChange = {
                        if (interactive) onRowsChange(it.roundToInt().coerceIn(minGrid, maxGrid))
                    },
                    onValueChangeFinished = {}
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset — outlined card, red TEXT only, white outline.
                    OutlinedButton(
                        onClick = { if (interactive) onReset() },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("Reset", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Done — outlined card, white outline, white text.
                    OutlinedButton(
                        onClick = { if (interactive) onDone() },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("Done", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
