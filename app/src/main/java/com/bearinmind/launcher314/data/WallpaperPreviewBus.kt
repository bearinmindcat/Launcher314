package com.bearinmind.launcher314.data

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter

/**
 * Process-wide preview override for the wallpaper editor. When a preview
 * entry is non-null, `LauncherWithDrawer` renders the edited bitmap as its
 * backdrop (instead of the saved custom wallpaper) so the user can see the
 * in-progress edit behind the real home-screen chrome — time widget, app
 * icons, dock, etc. — before committing to save.
 *
 * The editor writes an entry when "Preview" is tapped and clears it on
 * exit. The launcher reads the MutableState reactively.
 */
object WallpaperPreviewBus {
    var activePreview: PreviewEntry? by mutableStateOf(null)

    /**
     * Edit state to restore when the user returns to Settings after a
     * preview. Consumed by the wallpaper-editor composable on its next
     * mount; cleared immediately after it's been applied.
     */
    var pendingResumeEdit: DeviceWallpaperEdit? by mutableStateOf(null)

    data class PreviewEntry(
        val sourceBitmap: Bitmap,
        val edit: DeviceWallpaperEdit,
        /** Pre-computed ColorFilter equal to the editor's `previewColorFilter`. */
        val colorFilter: ColorFilter?
    )
}
