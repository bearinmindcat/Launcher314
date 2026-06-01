package com.bearinmind.launcher314.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Add

/**
 * Compact resize control panel shown during folder Resize mode.
 *
 * Sits at the top of the screen (under the status bar) and provides:
 *   - Columns +/- stepper
 *   - Rows +/- stepper
 *   - "Reset" — clears all per-folder overrides (popup size + grid dims)
 *   - "Done" — commits the in-flight changes
 *
 * The drag-resize handles around the popup (FolderResizeOverlay) stay
 * active in parallel, so the user can drag the popup pixel-size while
 * the panel adjusts row / column counts.
 *
 * Bounds: columns / rows clamped to 1..maxGrid (passed in — usually 8).
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
            .wrapContentSize()
            // Fade + slide from the anchored edge on enter / exit, in sync
            // with the outline's collapse into the popup card.
            .graphicsLayer {
                alpha = progress
                translationY = (if (anchorBottom) 16f else -16f) * (1f - progress)
            }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                    // Absorb every touch inside the panel card. Without this,
                    // taps that just miss the small IconButtons (32dp) fall
                    // through to the dim backdrop's onClick {} and close the
                    // folder — feels like a crash.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Resize folder",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Stepper(
                    label = "Columns",
                    value = currentColumns,
                    onChange = { if (interactive) onColumnsChange(it.coerceIn(minGrid, maxGrid)) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Stepper(
                    label = "Rows",
                    value = currentRows,
                    onChange = { if (interactive) onRowsChange(it.coerceIn(minGrid, maxGrid)) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { if (interactive) onReset() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (interactive) onDone() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Done", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(72.dp)
        )
        IconButton(
            onClick = { onChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Decrease $label",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(
            onClick = { onChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Increase $label",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
