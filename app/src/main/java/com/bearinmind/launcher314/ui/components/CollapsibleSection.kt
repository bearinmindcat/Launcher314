package com.bearinmind.launcher314.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    triangleEndPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) -90f else 0f,
        label = "triangle_rotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Clickable title bar with triangle indicator
        // Touch area extends fully edge-to-edge vertically (no padding gaps)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onToggle() }
                .padding(start = 16.dp, end = triangleEndPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )

            // Triangle indicator
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .rotate(rotationAngle)
            ) {
                val path = Path().apply {
                    // Triangle pointing down (will rotate to point right when expanded)
                    moveTo(size.width / 2, size.height * 0.8f)
                    lineTo(size.width * 0.15f, size.height * 0.2f)
                    lineTo(size.width * 0.85f, size.height * 0.2f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.Gray,
                    style = Fill
                )
            }
        }

        // Animated content visibility
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                content()
            }
        }
    }
}
