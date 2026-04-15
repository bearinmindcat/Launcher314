package com.bearinmind.launcher314.ui.home

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bearinmind.launcher314.ui.widgets.PlacedWidget
import com.bearinmind.launcher314.ui.widgets.WidgetManager

/**
 * Composable that displays an AppWidget using AndroidView.
 * Uses Fossify-style long-press detection with Handler and coordinate tracking.
 */
@Composable
fun WidgetHostView(
    placedWidget: PlacedWidget,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Float = 12f,
    viewRefreshKey: Int = 0,
    isInStack: Boolean = false,
    onLongPress: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val context = LocalContext.current

    // Get or create widget view (with crash protection).
    // Uses cached view when available to avoid content flash during pager re-composition.
    // viewRefreshKey is bumped by the parent after WidgetManager.recreateWidgetView() so
    // we re-fetch the freshly-rebuilt view (e.g., after a per-widget font scale change).
    val widgetView = remember(placedWidget.appWidgetId, viewRefreshKey) {
        try {
            WidgetManager.getOrCreateWidgetView(context, placedWidget.appWidgetId)
        } catch (e: Exception) {
            android.util.Log.e("WidgetHostView", "Failed to create widget view", e)
            null
        }
    }

    // Track if the AndroidView factory crashed so we can show placeholder
    var factoryCrashed by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    // Track last reported size to avoid infinite re-layout loops
    var lastReportedSize by remember { mutableStateOf(Pair(0, 0)) }

    if (widgetView != null && !factoryCrashed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .onGloballyPositioned { coords ->
                    val widthDp = with(density) { coords.size.width.toDp().value.toInt() }
                    val heightDp = with(density) { coords.size.height.toDp().value.toInt() }
                    val newSize = Pair(widthDp, heightDp)
                    if (widthDp > 0 && heightDp > 0 && newSize != lastReportedSize) {
                        lastReportedSize = newSize
                        WidgetManager.updateWidgetViewSize(
                            placedWidget.appWidgetId, widthDp, heightDp
                        )
                    }
                }
        ) {
            // key() forces AndroidView to tear down + re-create when viewRefreshKey
            // changes, so the new widget view (with the new font-scaled Context) actually
            // gets attached to the screen.
            key(viewRefreshKey) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            // Remove from parent if already attached
                            (widgetView.parent as? ViewGroup)?.removeView(widgetView)
                            widgetView.apply {
                                this.isInStack = isInStack
                                longPressListener = { _, _ ->
                                    onLongPress()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WidgetHostView", "Widget factory crashed", e)
                            factoryCrashed = true
                            // Return a blank view as fallback
                            android.view.View(ctx)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { _ ->
                        try {
                            widgetView.isInStack = isInStack
                            widgetView.longPressListener = { _, _ ->
                                onLongPress()
                            }
                        } catch (e: Exception) {
                            // Ignore update errors
                        }
                    }
                )
            }
        }
    } else {
        // Widget view couldn't be created - show placeholder
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Widget",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}
