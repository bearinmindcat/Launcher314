package com.bearinmind.launcher314.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bearinmind.launcher314.data.drawableToBitmap
import com.bearinmind.launcher314.data.getSelectedIconPack
import com.bearinmind.launcher314.data.saveBitmapToFile
import com.bearinmind.launcher314.data.setSelectedIconPack
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

// ========== Icon Pack Manager ==========
// Discovers installed icon packs (ADW standard), parses appfilter.xml,
// caches icon pack icons as PNGs, and resolves which icon to use per app.
// Follows the same singleton object pattern as FontManager.

object IconPackManager {

    data class IconPackInfo(
        val packageName: String,
        val displayName: String,
        val iconPath: String  // Icon pack's own app icon (for selection UI preview)
    )

    // In-memory cache of appfilter mappings for the active icon pack
    private var cachedAppFilterMap: Map<String, String> = emptyMap()
    private var cachedIconPackPackage: String = ""

    // ========== Discovery ==========

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val intent = Intent("org.adw.launcher.THEMES")
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val iconsDir = getIconPackAppIconsDir(context)

        return resolveInfoList.mapNotNull { resolveInfo ->
            try {
                val pkgName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()

                // Cache the icon pack's own app icon for the selection list
                val iconFile = File(iconsDir, "$pkgName.png")
                if (!iconFile.exists()) {
                    val drawable = pm.getApplicationIcon(pkgName)
                    val bitmap = drawableToBitmap(drawable)
                    saveBitmapToFile(bitmap, iconFile)
                    bitmap.recycle()
                }

                IconPackInfo(
                    packageName = pkgName,
                    displayName = label,
                    iconPath = iconFile.absolutePath
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.displayName.lowercase() }
    }

    fun getSelectedIconPackName(context: Context): String {
        val selectedPkg = getSelectedIconPack(context)
        if (selectedPkg.isEmpty()) return "System Icons (Default)"
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(selectedPkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            setSelectedIconPack(context, "")
            "Default"
        }
    }

    fun getSelectedIconPackIconPath(context: Context): String? {
        val selectedPkg = getSelectedIconPack(context)
        if (selectedPkg.isEmpty()) return null
        val iconFile = File(getIconPackAppIconsDir(context), "$selectedPkg.png")
        if (iconFile.exists()) return iconFile.absolutePath
        // Cache it if not yet cached
        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(selectedPkg)
            val bitmap = drawableToBitmap(drawable)
            saveBitmapToFile(bitmap, iconFile)
            bitmap.recycle()
            iconFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ========== appfilter.xml Parsing ==========

    fun parseAppFilter(context: Context, iconPackPackage: String): Map<String, String> {
        if (iconPackPackage == cachedIconPackPackage && cachedAppFilterMap.isNotEmpty()) {
            return cachedAppFilterMap
        }

        val pm = context.packageManager
        val result = mutableMapOf<String, String>()

        try {
            val iconPackResources = pm.getResourcesForApplication(iconPackPackage)

            // Try res/xml/appfilter first
            val xmlResId = iconPackResources.getIdentifier("appfilter", "xml", iconPackPackage)
            if (xmlResId != 0) {
                parseAppFilterXml(iconPackResources.getXml(xmlResId), result)
            } else {
                // Try res/raw/appfilter
                val rawResId = iconPackResources.getIdentifier("appfilter", "raw", iconPackPackage)
                if (rawResId != 0) {
                    parseAppFilterInputStream(iconPackResources.openRawResource(rawResId), result)
                } else {
                    // Try assets/appfilter.xml
                    try {
                        val assetContext = context.createPackageContext(
                            iconPackPackage, Context.CONTEXT_IGNORE_SECURITY
                        )
                        val inputStream = assetContext.assets.open("appfilter.xml")
                        parseAppFilterInputStream(inputStream, result)
                    } catch (_: Exception) {
                        // No appfilter found in any location
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedAppFilterMap = result
        cachedIconPackPackage = iconPackPackage
        return result
    }

    private fun parseAppFilterXml(parser: XmlResourceParser, result: MutableMap<String, String>) {
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null && drawable.isNotEmpty()) {
                        result[component] = drawable
                    }
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
    }

    private fun parseAppFilterInputStream(
        inputStream: java.io.InputStream,
        result: MutableMap<String, String>
    ) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null && drawable.isNotEmpty()) {
                        result[component] = drawable
                    }
                }
                eventType = parser.next()
            }
        } finally {
            inputStream.close()
        }
    }

    // ========== Icon Caching ==========

    fun cacheIconPackIcons(context: Context, iconPackPackage: String): Int {
        val pm = context.packageManager
        val appFilterMap = parseAppFilter(context, iconPackPackage)
        val iconPackResources = pm.getResourcesForApplication(iconPackPackage)
        val cacheDir = getIconPackCacheDir(context)

        // Clear previous cache
        cacheDir.listFiles()?.forEach { it.delete() }

        // Query all launchable activities to get ComponentInfo data
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = pm.queryIntentActivities(launchIntent, 0)

        var cachedCount = 0
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val pkgName = activityInfo.packageName
            val activityName = activityInfo.name

            // Build ComponentInfo string matching icon pack format
            val componentInfo = "ComponentInfo{$pkgName/$activityName}"
            val drawableName = appFilterMap[componentInfo] ?: continue

            try {
                val drawableResId = iconPackResources.getIdentifier(
                    drawableName, "drawable", iconPackPackage
                )
                if (drawableResId == 0) continue

                @Suppress("DEPRECATION")
                val drawable = iconPackResources.getDrawable(drawableResId, null) ?: continue
                val bitmap = drawableToBitmap(drawable)
                val outFile = File(cacheDir, "$pkgName.png")
                saveBitmapToFile(bitmap, outFile)
                bitmap.recycle()
                cachedCount++
            } catch (_: Exception) {
                // Skip icons that fail to load
            }
        }

        return cachedCount
    }

    fun clearIconPackCache(context: Context) {
        val cacheDir = getIconPackCacheDir(context)
        cacheDir.listFiles()?.forEach { it.delete() }
        cachedAppFilterMap = emptyMap()
        cachedIconPackPackage = ""
    }

    // ========== Icon Resolution ==========

    fun resolveIconPath(context: Context, packageName: String, systemIconPath: String): String {
        val selectedPack = getSelectedIconPack(context)
        if (selectedPack.isEmpty()) return systemIconPath

        val iconPackFile = File(getIconPackCacheDir(context), "$packageName.png")
        return if (iconPackFile.exists()) iconPackFile.absolutePath else systemIconPath
    }

    // ========== Directory Helpers ==========

    private fun getIconPackCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "icon_pack_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getIconPackAppIconsDir(context: Context): File {
        val dir = File(context.cacheDir, "icon_pack_app_icons")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
