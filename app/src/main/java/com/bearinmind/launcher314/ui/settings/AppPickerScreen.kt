package com.bearinmind.launcher314.ui.settings

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.GestureAction
import com.bearinmind.launcher314.data.GestureId
import com.bearinmind.launcher314.data.getGestureAction
import com.bearinmind.launcher314.data.setGestureAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private data class PickerAppInfo(
    val packageName: String,
    val name: String,
    val iconPath: String
)

/**
 * Single-select app picker, visual style cloned 1:1 from [HideAppsScreen].
 * Used by the "Open specific app" entry on each [GestureCard]. The
 * horizontal "Selected" strip shows the currently-bound app (if any) — tap
 * the badge to clear it. The "All apps" list checkbox indicates the
 * current selection; tapping a row replaces the selection.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppPickerScreen(
    gestureId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gesture = remember(gestureId) {
        try { GestureId.valueOf(gestureId) } catch (_: Exception) { null }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackage by remember(gesture) {
        mutableStateOf(
            gesture?.let { (getGestureAction(context, it) as? GestureAction.OpenApp)?.packageName }
        )
    }
    var allApps by remember { mutableStateOf<List<PickerAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val apps = activities.mapNotNull { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val iconDir = File(context.cacheDir, "app_icons")
                    val iconFile = File(iconDir, "$pkg.png")
                    val iconPath = if (iconFile.exists()) iconFile.absolutePath else ""
                    PickerAppInfo(pkg, appName, iconPath)
                } catch (_: Exception) { null }
            }.sortedBy { it.name.lowercase() }
            allApps = apps
            isLoading = false
        }
    }

    // Resolved info for the currently-selected app, or null if nothing
    // chosen / not yet loaded / filtered out by search.
    val selectedAppInfo by remember {
        derivedStateOf {
            val pkg = selectedPackage ?: return@derivedStateOf null
            val match = allApps.firstOrNull { it.packageName == pkg } ?: return@derivedStateOf null
            if (searchQuery.isBlank() || match.name.contains(searchQuery, ignoreCase = true)) match
            else null
        }
    }

    val allAppsList by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) allApps.sortedBy { it.name.lowercase() }
            else allApps.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedBy { it.name.lowercase() }
        }
    }

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
            // Search bar (same style as IconPacksScreen)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search Apps", color = Color.White.copy(alpha = 0.6f))
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // Selected app section — the outer AnimatedVisibility opens/closes
                // the strip when the gesture goes between None and OpenApp; the
                // inner Crossfade smoothly swaps the icon+label when switching
                // apps so the strip stays anchored (no flash, no layout shift).
                AnimatedVisibility(
                    visible = selectedAppInfo != null,
                    enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
                ) {
                    Column {
                        Text(
                            text = "Selected",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(vertical = 12.dp)
                        ) {
                            Crossfade(
                                targetState = selectedAppInfo,
                                animationSpec = tween(300),
                                label = "selectedAppCrossfade",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) { app ->
                                if (app != null) {
                                    Column(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedPackage = null
                                                gesture?.let {
                                                    setGestureAction(context, it, GestureAction.None)
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val iconSizeDp = 40.dp
                                        val badgeSize = iconSizeDp * 0.42f
                                        val badgeOffset = iconSizeDp * 0.083f
                                        Box(modifier = Modifier.size(iconSizeDp)) {
                                            if (app.iconPath.isNotEmpty()) {
                                                AsyncImage(
                                                    model = File(app.iconPath),
                                                    contentDescription = app.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.size(iconSizeDp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(iconSizeDp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.1f))
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = badgeOffset, y = -badgeOffset)
                                                    .size(badgeSize)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF8B2020).copy(alpha = 0.85f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(badgeSize * 0.5f)
                                                        .height(2.dp)
                                                        .background(Color(0xFFCCCCCC).copy(alpha = 0.85f))
                                                )
                                            }
                                        }
                                        Text(
                                            text = app.name,
                                            color = Color.White.copy(alpha = 0.87f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
                                    }
                                } else {
                                    // Empty placeholder keeps the box height
                                    // stable while AnimatedVisibility shrinks
                                    // it on clear.
                                    Spacer(modifier = Modifier.size(64.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    // All apps section header
                    item {
                        Text(
                            text = "All apps",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // All apps list (same style as IconPacksScreen)
                    items(allAppsList, key = { "all_${it.packageName}" }) { app ->
                        val isSelected = app.packageName == selectedPackage
                        val toggleSelect = {
                            if (app.packageName == selectedPackage) {
                                // Tapping the already-selected app clears it.
                                selectedPackage = null
                                gesture?.let {
                                    setGestureAction(context, it, GestureAction.None)
                                }
                            } else {
                                selectedPackage = app.packageName
                                gesture?.let {
                                    setGestureAction(
                                        context,
                                        it,
                                        GestureAction.OpenApp(app.packageName)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .animateItemPlacement(tween(300))
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { toggleSelect() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { toggleSelect() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = Color(0xFF3A3A3A),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )

                            Text(
                                text = app.name,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.87f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (app.iconPath.isNotEmpty()) {
                                AsyncImage(
                                    model = File(app.iconPath),
                                    contentDescription = app.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
