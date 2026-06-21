package com.bearinmind.launcher314.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Backup / restore of launcher settings + customizations to a single .json file.
 *
 * Captures:
 *  - All settings SharedPreferences (typed key/value), and
 *  - The launcher's JSON data files (home-screen layout, drawer folders,
 *    per-app customizations) embedded verbatim.
 *
 * NOT captured (binary / device-specific — they gracefully fall back on the
 * target): custom icon images, wallpaper images, imported font files, shortcut
 * icons, and live widget host IDs (placed-widget metadata IS restored, but a
 * widget's appWidgetId is device-specific, so widgets may need re-adding after
 * a restore on a different device or a reinstall).
 */
object BackupManager {
    private const val BACKUP_VERSION = 1
    private const val APP_TAG = "Launcher314"

    // Settings SharedPreferences files to back up.
    private val PREF_FILES = listOf(
        "app_drawer_settings",   // the bulk: icon/grid/text/scrollbar/gestures/hidden apps/global styling
        "home_screen_settings",  // home grid + dock config
        "wallpaper_settings",    // wallpaper mode + dim/blur + edit params
        "launcher_widgets",      // placed widgets (positions/spans/stacks/per-widget tweaks)
        "launcher_prefs"         // misc launcher flags
    )

    // Launcher data files (under filesDir) to embed verbatim.
    private val DATA_FILES = listOf(
        "home_screen_data.json", // apps, folders, dock layout, pages
        "drawer_data.json",      // drawer folders
        "app_customizations.json" // per-app icons/labels/colors/shapes/sizes/fonts
    )

    /** Serialize everything to a pretty-printed JSON string. */
    fun exportAll(context: Context): String {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("backupVersion", BACKUP_VERSION)

        val prefsRoot = JSONObject()
        for (name in PREF_FILES) {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val obj = JSONObject()
            for ((key, value) in prefs.all) {
                val entry = JSONObject()
                when (value) {
                    is String -> { entry.put("t", "s"); entry.put("v", value) }
                    is Boolean -> { entry.put("t", "b"); entry.put("v", value) }
                    is Int -> { entry.put("t", "i"); entry.put("v", value) }
                    is Float -> { entry.put("t", "f"); entry.put("v", value.toDouble()) }
                    is Long -> { entry.put("t", "l"); entry.put("v", value) }
                    is Set<*> -> {
                        entry.put("t", "ss")
                        val arr = JSONArray()
                        value.forEach { arr.put(it.toString()) }
                        entry.put("v", arr)
                    }
                    else -> continue
                }
                obj.put(key, entry)
            }
            prefsRoot.put(name, obj)
        }
        root.put("prefs", prefsRoot)

        val filesRoot = JSONObject()
        for (name in DATA_FILES) {
            val f = File(context.filesDir, name)
            if (f.exists()) {
                filesRoot.put(name, f.readText())
            }
        }
        root.put("files", filesRoot)

        return root.toString(2)
    }

    /**
     * Restore from a backup JSON string. Returns true on success. Overwrites
     * current settings. Caller should restart the launcher afterwards.
     */
    fun importAll(context: Context, json: String): Boolean {
        val root = try { JSONObject(json) } catch (_: Exception) { return false }
        // Guard against importing an unrelated JSON file.
        if (root.optString("app") != APP_TAG && !root.has("prefs")) return false

        val prefsRoot = root.optJSONObject("prefs")
        if (prefsRoot != null) {
            for (name in PREF_FILES) {
                val obj = prefsRoot.optJSONObject(name) ?: continue
                val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                editor.clear()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val entry = obj.optJSONObject(key) ?: continue
                    when (entry.optString("t")) {
                        "s" -> editor.putString(key, entry.optString("v"))
                        "b" -> editor.putBoolean(key, entry.optBoolean("v"))
                        "i" -> editor.putInt(key, entry.optInt("v"))
                        "f" -> editor.putFloat(key, entry.optDouble("v").toFloat())
                        "l" -> editor.putLong(key, entry.optLong("v"))
                        "ss" -> {
                            val arr = entry.optJSONArray("v") ?: JSONArray()
                            val set = HashSet<String>()
                            for (i in 0 until arr.length()) set.add(arr.optString(i))
                            editor.putStringSet(key, set)
                        }
                    }
                }
                editor.apply()
            }
        }

        val filesRoot = root.optJSONObject("files")
        if (filesRoot != null) {
            for (name in DATA_FILES) {
                if (!filesRoot.has(name)) continue
                val content = filesRoot.optString(name)
                try {
                    File(context.filesDir, name).writeText(content)
                } catch (_: Exception) { /* skip this file, keep going */ }
            }
        }

        return true
    }
}
