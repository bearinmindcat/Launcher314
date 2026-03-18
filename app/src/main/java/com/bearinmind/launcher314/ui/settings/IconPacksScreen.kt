package com.bearinmind.launcher314.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.getSelectedIconPack
import com.bearinmind.launcher314.data.setSelectedIconPack
import com.bearinmind.launcher314.helpers.IconPackManager
import com.bearinmind.launcher314.helpers.clearGlobalShapedIcons
import com.bearinmind.launcher314.helpers.clearBgColorShapedIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPacksScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackId by remember { mutableStateOf(getSelectedIconPack(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }

    // Discover installed icon packs
    val installedPacks = remember { IconPackManager.getInstalledIconPacks(context) }

    // Filter based on search
    val filteredPacks = remember(searchQuery, installedPacks) {
        if (searchQuery.isBlank()) installedPacks
        else installedPacks.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }
    val showSystemIcons = searchQuery.isBlank() ||
        "system icons".contains(searchQuery, ignoreCase = true)

    val screenBackground = Color(0xFF121212)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search bar (same style as FontsScreen)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search Icon Packs", color = Color.White.copy(alpha = 0.6f))
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

            // Icon pack list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
            ) {
                // System Icons (default) option
                if (showSystemIcons) {
                    item {
                        IconPackItemRow(
                            displayName = "System Icons (Default)",
                            iconPath = null,
                            isSelected = selectedPackId.isEmpty(),
                            onSelect = {
                                if (selectedPackId.isNotEmpty()) {
                                    selectedPackId = ""
                                    setSelectedIconPack(context, "")
                                    clearGlobalShapedIcons(context)
                                    clearBgColorShapedIcons(context)
                                    scope.launch(Dispatchers.IO) {
                                        IconPackManager.clearIconPackCache(context)
                                    }
                                    Toast.makeText(
                                        context, "Switched to system icons", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }

                items(filteredPacks, key = { it.packageName }) { pack ->
                    IconPackItemRow(
                        displayName = pack.displayName,
                        iconPath = pack.iconPath,
                        isSelected = selectedPackId == pack.packageName,
                        onSelect = {
                            if (selectedPackId != pack.packageName) {
                                selectedPackId = pack.packageName
                                setSelectedIconPack(context, pack.packageName)
                                clearGlobalShapedIcons(context)
                                clearBgColorShapedIcons(context)
                                isLoading = true
                                loadingMessage = "Applying ${pack.displayName}..."
                                scope.launch(Dispatchers.IO) {
                                    val count = IconPackManager.cacheIconPackIcons(
                                        context, pack.packageName
                                    )
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Applied ${pack.displayName} ($count icons)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        onOpenApp = {
                            val launchIntent = context.packageManager
                                .getLaunchIntentForPackage(pack.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "${pack.displayName} has no launchable activity",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }

                // Empty state when no packs installed
                if (installedPacks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No icon packs installed.\nInstall icon packs from the Play Store or F-Droid that support ADW/Nova launcher themes.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingMessage, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun IconPackItemRow(
    displayName: String,
    iconPath: String?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenApp: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color(0xFF3A3A3A),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Text(
            text = displayName,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.87f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Icon pack preview icon (tappable to open the app)
        if (iconPath != null && onOpenApp != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onOpenApp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(iconPath),
                    contentDescription = "Open $displayName",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                )
            }
        } else if (iconPath != null) {
            AsyncImage(
                model = File(iconPath),
                contentDescription = displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
