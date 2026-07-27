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
// `userSerial` identifies the user profile the app belongs to. Null = primary
// (personal) profile. Non-null = work / managed / cloned profile, resolved via
// UserManager.getUserForSerialNumber(). Drawer enumeration via LauncherApps
// returns apps from every profile; the serial lets us route launches and
// badge icons correctly.
data class AppInfo(
    val name: String,
    val packageName: String,
    val iconPath: String,
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val sizeBytes: Long = 0L,
    val userSerial: Long? = null,
    // In-memory only — re-derived from the live profile each enumeration via
    // LauncherAppsHelper.profileTypeFor(). Not persisted; storage just keeps
    // the user serial and we look the type up again on next load.
    val profileType: com.bearinmind.launcher314.helpers.ProfileType =
        com.bearinmind.launcher314.helpers.ProfileType.PERSONAL
)

@Serializable
data class AppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    // Package names, "" gap markers, and "folder:<id>" sub-folder refs (issue #71).
    val appPackageNames: List<String> = emptyList()
)

// ---- Sub-folder entry helpers ----
const val FOLDER_ENTRY_PREFIX = "folder:"
fun folderEntry(folderId: String) = "$FOLDER_ENTRY_PREFIX$folderId"
fun isFolderEntry(entry: String) = entry.startsWith(FOLDER_ENTRY_PREFIX)
fun folderEntryId(entry: String) = entry.removePrefix(FOLDER_ENTRY_PREFIX)

/** Ids of folders nested inside another folder — hidden from top level. */
fun nestedFolderIds(folders: List<AppFolder>): Set<String> =
    folders.flatMap { f -> f.appPackageNames.filter(::isFolderEntry).map(::folderEntryId) }.toSet()

/** [folderId] plus every folder reachable inside it — used to block cycles. */
fun folderAndDescendantIds(folders: List<AppFolder>, folderId: String): Set<String> {
    val out = mutableSetOf<String>()
    fun visit(id: String) {
        if (!out.add(id)) return
        folders.firstOrNull { it.id == id }?.appPackageNames
            ?.filter(::isFolderEntry)?.map(::folderEntryId)?.forEach(::visit)
    }
    visit(folderId)
    return out
}

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
