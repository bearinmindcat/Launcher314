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
    val customIconPackName: String? = null
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
