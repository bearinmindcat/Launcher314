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

    /**
     * Captured first-finger position of the active preview gesture. Drives
     * the rubber-band stretch's pivot — the side OPPOSITE the touched edge
     * stays anchored, so a pinch near the left side only stretches the
     * left side outward (instead of all four corners moving symmetrically).
     */
    var pinchAnchor: PinchAnchor by mutableStateOf(PinchAnchor.DEFAULT)

    data class PreviewEntry(
        val sourceBitmap: Bitmap,
        val edit: DeviceWallpaperEdit,
        /** Pre-computed ColorFilter equal to the editor's `previewColorFilter`. */
        val colorFilter: ColorFilter?
    )

    data class PinchAnchor(
        /** Fractional pivot used by graphicsLayer.transformOrigin. */
        val pivot: ColorFilterIndependentTransformOrigin,
        /** True = stretch on X axis (one of the left/right sides). False = Y. */
        val isHorizontal: Boolean
    ) {
        companion object {
            val DEFAULT = PinchAnchor(
                pivot = ColorFilterIndependentTransformOrigin(0.5f, 0.5f),
                isHorizontal = true
            )
        }
    }

    /**
     * Lightweight pivot value stored in the bus. Mirrors Compose's
     * `androidx.compose.ui.graphics.TransformOrigin` (which we can't depend
     * on in the data layer cleanly).
     */
    data class ColorFilterIndependentTransformOrigin(
        val pivotFractionX: Float,
        val pivotFractionY: Float
    )
}
