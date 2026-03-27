package com.bearinmind.launcher314.ui.settings

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import java.io.File
import com.bearinmind.launcher314.data.getHiddenApps
import com.bearinmind.launcher314.data.setHiddenApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HideAppInfo(
    val packageName: String,
    val name: String,
    val iconPath: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HideAppsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var hiddenApps by remember { mutableStateOf(getHiddenApps(context)) }
    var pendingRemoval by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allApps by remember { mutableStateOf<List<HideAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Load all installed apps
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
                    HideAppInfo(pkg, appName, iconPath)
                } catch (_: Exception) { null }
            }.sortedBy { it.name.lowercase() }
            allApps = apps
            isLoading = false
        }
    }

    val hiddenAppsList by remember {
        derivedStateOf {
            val hidden = allApps.filter { it.packageName in hiddenApps }
            if (searchQuery.isBlank()) hidden.sortedBy { it.name.lowercase() }
            else hidden.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedBy { it.name.lowercase() }
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
                // Hidden apps section — outside LazyColumn so AnimatedVisibility always recomposes
                AnimatedVisibility(
                    visible = hiddenAppsList.isNotEmpty(),
                    enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
                ) {
                    Column {
                        Text(
                            text = "Hidden apps",
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
                            val lazyRowState = rememberLazyListState()
                            LazyRow(
                                state = lazyRowState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    hiddenAppsList,
                                    key = { it.packageName }
                                ) { app ->
                                    val isBeingRemoved = app.packageName in pendingRemoval
                                    var itemVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) { itemVisible = true }
                                    LaunchedEffect(isBeingRemoved) {
                                        if (isBeingRemoved) {
                                            itemVisible = false
                                            delay(250)
                                            hiddenApps = hiddenApps - app.packageName
                                            setHiddenApps(context, hiddenApps)
                                            pendingRemoval = pendingRemoval - app.packageName
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = itemVisible,
                                        enter = fadeIn(tween(250)),
                                        exit = fadeOut(tween(200))
                                    ) {
                                    Column(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .animateItemPlacement(tween(300))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                pendingRemoval = pendingRemoval + app.packageName
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
                                    } // AnimatedVisibility
                                }
                            }

                            // Scrollbar area — fixed height so card doesn't resize
                            val scrollbarColor = remember {
                                val baseColor = Color(com.bearinmind.launcher314.data.getScrollbarColor(context))
                                val intensity = com.bearinmind.launcher314.data.getScrollbarIntensity(context) / 100f
                                Color(baseColor.red * intensity, baseColor.green * intensity, baseColor.blue * intensity, baseColor.alpha)
                            }
                            val widthPercent = com.bearinmind.launcher314.data.getScrollbarWidthPercent(context) / 100f
                            val thumbHeight = (3.dp * widthPercent).coerceAtLeast(2.dp)
                            val isScrolling = lazyRowState.isScrollInProgress
                            val hasScrollbar = hiddenAppsList.size > 4
                            var scrollbarVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(isScrolling) {
                                if (isScrolling && hasScrollbar) { scrollbarVisible = true }
                                else { delay(1000); scrollbarVisible = false }
                            }
                            val scrollbarAlpha by animateFloatAsState(
                                targetValue = if (scrollbarVisible) 1f else 0f,
                                animationSpec = tween(300),
                                label = "hiddenScrollbarAlpha"
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(thumbHeight)
                                    .graphicsLayer { alpha = scrollbarAlpha }
                            ) {
                                if (hasScrollbar) {
                                    val layoutInfo = lazyRowState.layoutInfo
                                    val totalItems = layoutInfo.totalItemsCount.coerceAtLeast(1)
                                    val firstVisible = lazyRowState.firstVisibleItemIndex
                                    val scrollFraction = (firstVisible.toFloat() / (totalItems - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val thumbFraction = (4f / hiddenAppsList.size).coerceIn(0.2f, 1f)
                                    val trackWidth = maxWidth
                                    val thumbWidth = trackWidth * thumbFraction
                                    val thumbOffset = (trackWidth - thumbWidth) * scrollFraction
                                    Box(
                                        modifier = Modifier
                                            .offset(x = thumbOffset)
                                            .width(thumbWidth)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(thumbHeight / 2))
                                            .background(scrollbarColor.copy(alpha = 0.6f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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
                        val isHidden = app.packageName in hiddenApps && app.packageName !in pendingRemoval
                        val toggleHide = {
                            if (app.packageName in hiddenApps) {
                                pendingRemoval = pendingRemoval + app.packageName
                            } else {
                                hiddenApps = hiddenApps + app.packageName
                                setHiddenApps(context, hiddenApps)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .animateItemPlacement(tween(300))
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { toggleHide() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isHidden,
                                onCheckedChange = { toggleHide() },
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
