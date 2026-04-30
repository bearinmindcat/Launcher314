package com.bearinmind.launcher314.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.data.getRecentAppsCount
import com.bearinmind.launcher314.data.getRecentAppsSort
import com.bearinmind.launcher314.data.launchApp
import com.bearinmind.launcher314.helpers.UsageStatsHelper
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recent Apps overlay (issue #40 / KISS-style "history").
 *
 * Slides up from the bottom showing the user's recently-used apps via the
 * system [UsageStatsManager]. If usage access hasn't been granted, shows a
 * single button that opens the system-settings page where the user can grant
 * it. Tap an app row to launch; tap outside or on the close affordance to
 * dismiss.
 */
@Composable
fun RecentAppsOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<UsageStatsHelper.RecentAppEntry>>(emptyList()) }
    var hasAccess by remember { mutableStateOf(UsageStatsHelper.hasUsageAccess(context)) }
    var loading by remember { mutableStateOf(hasAccess) }

    LaunchedEffect(visible, hasAccess) {
        if (!visible) return@LaunchedEffect
        if (!hasAccess) { apps = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        val sort = if (getRecentAppsSort(context) == 1)
            UsageStatsHelper.Sort.Frequency else UsageStatsHelper.Sort.Recency
        val limit = getRecentAppsCount(context)
        apps = withContext(Dispatchers.IO) {
            UsageStatsHelper.queryRecentApps(context, sort, limit)
        }
        loading = false
    }

    // Re-check access whenever the overlay (re-)appears, in case the user
    // toggled it via the grant intent and came back.
    LaunchedEffect(visible) {
        if (visible) hasAccess = UsageStatsHelper.hasUsageAccess(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectAndDismiss(onDismiss) }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { /* eat outside-tap so the inner area doesn't dismiss */ }
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Apps",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Close",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))

            if (!hasAccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recent Apps needs Usage Access",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap the button below, then enable Launcher314 in the list.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { UsageStatsHelper.openUsageAccessSettings(context) }) {
                        Text("Grant Usage Access")
                    }
                }
            } else if (loading) {
                Text(
                    text = "Loading…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            } else if (apps.isEmpty()) {
                Text(
                    text = "No recent app activity yet.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(apps, key = { it.packageName }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    launchApp(context, entry.packageName)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val icon = entry.icon
                            if (icon != null) {
                                androidx.compose.foundation.Image(
                                    painter = rememberDrawablePainter(icon),
                                    contentDescription = entry.label,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                            Text(
                                text = entry.label,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectAndDismiss(
    onDismiss: () -> Unit
) {
    detectTapGestures(onTap = { onDismiss() })
}
