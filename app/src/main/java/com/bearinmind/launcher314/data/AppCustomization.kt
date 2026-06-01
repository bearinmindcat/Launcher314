package com.bearinmind.launcher314.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppCustomization(
    val customLabel: String? = null,
    val hideLabel: Boolean = false,
    val customIconPath: String? = null,
    val iconTintColor: Long? = null,
    val iconTintBlendMode: String? = null,
    val iconTintIntensity: Int? = null,
    val iconTintBackgroundOnly: Boolean = false,
    val iconShape: String? = null,
    val iconShapeExp: String? = null,
    val iconShapeExpV2: String? = null,
    val iconSizePercent: Int? = null,
    val iconTextSizePercent: Int? = null,
    val labelFontId: String? = null,
    val labelColor: Long? = null,
    val labelColorIntensity: Int? = null,
    val customIconPackName: String? = null,
    // Experimental — Issue #48. When `detachedFromGrid` is true the icon is
    // skipped in the home-grid render and instead drawn as a free-floating
    // overlay on the page, positioned by detachedX/detachedY in pixels
    // (top-left of the page content area). Null x/y → initial placement.
    val detachedFromGrid: Boolean = false,
    val detachedX: Float? = null,
    val detachedY: Float? = null,
    // Independent X/Y stretch multipliers for detached icons. Applied on
    // top of iconSizePercent (which sets the base size); these let the
    // icon become rectangular when the user drags the edge handles in
    // edit mode. 1.0 = no stretch on that axis.
    val detachedScaleX: Float? = null,
    val detachedScaleY: Float? = null,
    // Text-as-icon (Total-Launcher style): when non-null + non-blank, the
    // image icon is replaced by this text rendered in the icon slot. The
    // text is still clickable / draggable like a normal icon, the rest of
    // the customization (size, color, label, detach) keeps working on top.
    val iconText: String? = null,
    // Style for the icon-text (independent of the label below). Null fields
    // fall back to defaults (label color / 100% intensity / selected font).
    val iconTextColor: Long? = null,
    val iconTextColorIntensity: Int? = null,
    val iconTextFontId: String? = null,
    // Direct SP value for the text-as-icon glyph. When null the renderer
    // auto-computes ~0.55× the per-app icon size. Independent of
    // iconTextSizePercent (which sizes the label below the icon).
    val iconAsTextSizeSp: Int? = null,
    // When true the rendered icon ignores every image-side customization
    // (custom path, shape, tint, icon-pack) and falls back to the app's
    // own launcher icon. Mutually exclusive with iconText (text mode).
    val useOriginalIcon: Boolean = false,
    // Folder popup overrides — only meaningful when this AppCustomization
    // is keyed by a folder ID. Pixel-px values committed by the in-popup
    // "Resize" drag handle. Null = use default popup size.
    val folderPopupWidthPx: Int? = null,
    val folderPopupHeightPx: Int? = null,
    // Per-folder grid override (set from the in-popup "Resize" panel).
    // Null = inherit the home-screen grid columns / rows.
    val folderGridColumns: Int? = null,
    val folderGridRows: Int? = null
)

@Serializable
data class AppCustomizations(
    val customizations: Map<String, AppCustomization> = emptyMap()
)

private const val CUSTOMIZATIONS_FILE = "app_customizations.json"

fun loadAppCustomizations(context: Context): AppCustomizations {
    return try {
        val file = File(context.filesDir, CUSTOMIZATIONS_FILE)
        if (file.exists()) {
            Json.decodeFromString<AppCustomizations>(file.readText())
        } else {
            AppCustomizations()
        }
    } catch (e: Exception) {
        AppCustomizations()
    }
}

fun saveAppCustomizations(context: Context, data: AppCustomizations) {
    try {
        val file = File(context.filesDir, CUSTOMIZATIONS_FILE)
        file.writeText(Json.encodeToString(data))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun setCustomization(
    context: Context,
    customizations: AppCustomizations,
    packageName: String,
    customization: AppCustomization
): AppCustomizations {
    val updated = customizations.copy(
        customizations = customizations.customizations + (packageName to customization)
    )
    saveAppCustomizations(context, updated)
    return updated
}

fun removeCustomization(
    context: Context,
    customizations: AppCustomizations,
    packageName: String
): AppCustomizations {
    val updated = customizations.copy(
        customizations = customizations.customizations - packageName
    )
    saveAppCustomizations(context, updated)
    val iconFile = File(getCustomIconsDir(context), "$packageName.png")
    if (iconFile.exists()) iconFile.delete()
    return updated
}

fun getCustomIconsDir(context: Context): File {
    val dir = File(context.filesDir, "custom_icons")
    if (!dir.exists()) dir.mkdirs()
    return dir
}
