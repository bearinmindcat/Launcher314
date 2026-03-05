package com.bearinmind.launcher314.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.helpers.FontManager
import com.bearinmind.launcher314.data.getSelectedFont
import com.bearinmind.launcher314.data.setSelectedFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFontId by remember { mutableStateOf(getSelectedFont(context)) }
    var importedFonts by remember { mutableStateOf(FontManager.getImportedFonts(context)) }
    var showDeleteDialog by remember { mutableStateOf<FontManager.FontItem?>(null) }

    // File picker for importing fonts
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val imported = FontManager.importFont(context, it)
            if (imported != null) {
                importedFonts = FontManager.getImportedFonts(context)
                Toast.makeText(context, "Font \"${imported.displayName}\" imported", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import font", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Build the full font list: Default + Bundled + Imported
    val defaultItem = FontManager.FontItem(
        id = "default",
        displayName = "Default (System Font)",
        fontFamily = FontFamily.Default,
        isBundled = true
    )

    // Filter based on search
    val filteredBundled = remember(searchQuery) {
        if (searchQuery.isBlank()) FontManager.bundledFonts
        else FontManager.bundledFonts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredImported = remember(searchQuery, importedFonts) {
        if (searchQuery.isBlank()) importedFonts
        else importedFonts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }
    val showDefault = searchQuery.isBlank() || "default".contains(searchQuery, ignoreCase = true)

    val fontsBackground = Color(0xFF121212)

    // Delete confirmation dialog
    showDeleteDialog?.let { fontToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Font", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${fontToDelete.displayName}\" from imported fonts?") },
            confirmButton = {
                TextButton(onClick = {
                    FontManager.deleteImportedFont(context, fontToDelete)
                    importedFonts = FontManager.getImportedFonts(context)
                    if (selectedFontId == fontToDelete.id) {
                        selectedFontId = "default"
                    }
                    showDeleteDialog = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fontsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search bar (matching WidgetsScreen style)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search Fonts", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
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

            // "Add Fonts" button
            Button(
                onClick = {
                    fontPickerLauncher.launch(arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-opentype",
                        "application/octet-stream"
                    ))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add custom font (OTF or TTF)", fontSize = 14.sp)
            }

            // Font list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                // Default font
                if (showDefault) {
                    item {
                        FontItemRow(
                            fontItem = defaultItem,
                            isSelected = selectedFontId == "default",
                            onSelect = {
                                selectedFontId = "default"
                                setSelectedFont(context, "default")
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }

                // Imported fonts (under User Fonts header, after Default)
                if (filteredImported.isNotEmpty()) {
                    items(filteredImported, key = { it.id }) { fontItem ->
                        FontItemRow(
                            fontItem = fontItem,
                            isSelected = selectedFontId == fontItem.id,
                            onSelect = {
                                selectedFontId = fontItem.id
                                setSelectedFont(context, fontItem.id)
                            },
                            onDelete = {
                                showDeleteDialog = fontItem
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }

                // Bundled fonts
                if (filteredBundled.isNotEmpty()) {
                    items(filteredBundled, key = { it.id }) { fontItem ->
                        FontItemRow(
                            fontItem = fontItem,
                            isSelected = selectedFontId == fontItem.id,
                            onSelect = {
                                selectedFontId = fontItem.id
                                setSelectedFont(context, fontItem.id)
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FontItemRow(
    fontItem: FontManager.FontItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
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
            text = fontItem.displayName,
            fontFamily = fontItem.fontFamily,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.87f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete font",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
