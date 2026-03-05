package com.bearinmind.launcher314.data

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.Serializable
import java.util.UUID

// Sort options enum
enum class SortOption(val displayName: String) {
    MANUAL("Manual"),
    UPDATED("Updated"),
    NAME("Name"),
    INSTALLED("Installed"),
    SIZE("Size")
}

// Data classes
data class AppInfo(
    val name: String,
    val packageName: String,
    val iconPath: String,
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val sizeBytes: Long = 0L
)

@Serializable
data class AppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val appPackageNames: List<String> = emptyList()
)

@Serializable
data class DrawerData(
    val folders: List<AppFolder> = emptyList()
)

/** Bundled home-screen drag callbacks to reduce MainDrawerContent param count (DEX 256-register limit) */
data class HomeDragCallbacks(
    val onDragToHome: (Any, Offset) -> Unit = { _, _ -> },
    val onDragToHomeMove: (Offset) -> Unit = {},
    val onDragToHomeDrop: () -> Unit = {}
)

/** Escape drag hover state passed to MainDrawerContent as a single param */
data class EscapeHoverState(
    val folderId: String? = null,
    val iconPath: String? = null,
    val dropZoneBoundsRef: MutableState<Rect>,
    val isEscapeDragActive: Boolean = false,
    val isInDropZone: Boolean = false
)
