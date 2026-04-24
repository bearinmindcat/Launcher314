package com.bearinmind.launcher314.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.WALLPAPER_MODE_CUSTOM
import com.bearinmind.launcher314.data.WALLPAPER_MODE_DEVICE
import com.bearinmind.launcher314.data.WALLPAPER_MODE_SYSTEM
import com.bearinmind.launcher314.data.getCustomWallpaperPath
import com.bearinmind.launcher314.data.getDeviceWallpaperEdit
import com.bearinmind.launcher314.data.getDeviceWallpaperSourceFile
import com.bearinmind.launcher314.data.getDeviceWallpaperSourcePath
import com.bearinmind.launcher314.data.getWallpaperBlur
import com.bearinmind.launcher314.data.getWallpaperCacheVersion
import com.bearinmind.launcher314.data.getWallpaperDim
import com.bearinmind.launcher314.data.getWallpaperMode
import com.bearinmind.launcher314.data.setWallpaperBlur
import com.bearinmind.launcher314.data.setWallpaperDim
import com.bearinmind.launcher314.data.setWallpaperMode
import com.bearinmind.launcher314.helpers.WallpaperHelper
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun WallpaperPersonalizationCard(
    onPreviewLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(getWallpaperMode(context)) }
    var customPath by remember { mutableStateOf(getCustomWallpaperPath(context)) }
    var deviceSourcePath by remember { mutableStateOf(getDeviceWallpaperSourcePath(context)) }
    var dim by remember { mutableIntStateOf(getWallpaperDim(context)) }
    var blur by remember { mutableIntStateOf(getWallpaperBlur(context)) }
    var cacheVersion by remember { mutableIntStateOf(getWallpaperCacheVersion(context)) }
    var editorOpenForDevice by remember { mutableStateOf(false) }

    // Auto-reopen the editor on Settings re-entry if we came back from a
    // launcher-preview round trip. Clears the active-preview backdrop so the
    // home-screen stops showing the preview bitmap once Settings is
    // foregrounded again.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val pending = com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit
        if (pending != null && deviceSourcePath != null) {
            editorOpenForDevice = true
            com.bearinmind.launcher314.data.WallpaperPreviewBus.activePreview = null
        }
    }

    // Step 1 overlay picker — custom image on top of launcher
    val pickCustom = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    WallpaperHelper.importCustomWallpaper(context, uri)
                }
                if (saved != null) {
                    customPath = saved
                    mode = WALLPAPER_MODE_CUSTOM
                    setWallpaperMode(context, WALLPAPER_MODE_CUSTOM)
                    cacheVersion = getWallpaperCacheVersion(context)
                }
            }
        }
    }

    // Step 2 device-wallpaper editor picker — opens the editor after import.
    // Resets any previously-saved edit state (scale/offset/crop/etc.) so the new
    // image starts with a clean 1:1 transform and full-image crop, otherwise an
    // old saved scale could shrink the new picture inside the preview box.
    val pickDeviceSource = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    WallpaperHelper.importDeviceWallpaperSource(context, uri)
                }
                if (saved != null) {
                    deviceSourcePath = saved
                    cacheVersion = getWallpaperCacheVersion(context)
                    com.bearinmind.launcher314.data.setDeviceWallpaperEdit(
                        context,
                        com.bearinmind.launcher314.data.DeviceWallpaperEdit()
                    )
                    editorOpenForDevice = true
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Mode selector — three radios
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = mode == WALLPAPER_MODE_SYSTEM,
                onClick = {
                    mode = WALLPAPER_MODE_SYSTEM
                    setWallpaperMode(context, WALLPAPER_MODE_SYSTEM)
                }
            )
            Text("System wallpaper", modifier = Modifier.padding(start = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = mode == WALLPAPER_MODE_CUSTOM,
                onClick = {
                    mode = WALLPAPER_MODE_CUSTOM
                    setWallpaperMode(context, WALLPAPER_MODE_CUSTOM)
                }
            )
            Text("Custom overlay (launcher only)", modifier = Modifier.padding(start = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = mode == WALLPAPER_MODE_DEVICE,
                onClick = {
                    mode = WALLPAPER_MODE_DEVICE
                    setWallpaperMode(context, WALLPAPER_MODE_DEVICE)
                }
            )
            Text("Device wallpaper (edited here)", modifier = Modifier.padding(start = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (mode) {
            WALLPAPER_MODE_CUSTOM -> CustomOverlayControls(
                customPath = customPath,
                cacheVersion = cacheVersion,
                dim = dim,
                blur = blur,
                onPick = {
                    pickCustom.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = {
                    WallpaperHelper.clearCustomWallpaper(context)
                    customPath = null
                    mode = WALLPAPER_MODE_SYSTEM
                    setWallpaperMode(context, WALLPAPER_MODE_SYSTEM)
                    cacheVersion = getWallpaperCacheVersion(context)
                },
                onDimChange = { dim = it; setWallpaperDim(context, it) },
                onBlurChange = { blur = it; setWallpaperBlur(context, it) }
            )
            WALLPAPER_MODE_DEVICE -> DeviceWallpaperControls(
                deviceSourcePath = deviceSourcePath,
                cacheVersion = cacheVersion,
                onPickAndEdit = {
                    pickDeviceSource.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onEditAgain = { editorOpenForDevice = true },
                onClear = {
                    WallpaperHelper.clearDeviceWallpaperSource(context)
                    deviceSourcePath = null
                    mode = WALLPAPER_MODE_SYSTEM
                    setWallpaperMode(context, WALLPAPER_MODE_SYSTEM)
                    cacheVersion = getWallpaperCacheVersion(context)
                }
            )
            else -> {
                // SYSTEM mode — just show dim slider (the only knob that applies to system wallpaper)
                Text("Dim", style = MaterialTheme.typography.labelMedium)
                ThumbDragHorizontalSlider(
                    currentValue = dim.toFloat(),
                    config = SliderConfigs.wallpaperPercent,
                    onValueChange = { dim = it.roundToInt(); setWallpaperDim(context, dim) },
                    onValueChangeFinished = { setWallpaperDim(context, dim) }
                )
            }
        }
    }

    // Editor dialog — opens when user picks a fresh image or taps "Edit again..."
    if (editorOpenForDevice && deviceSourcePath != null) {
        val sourceFile = File(deviceSourcePath!!)
        if (sourceFile.exists()) {
            // If returning from a launcher preview, restore the in-progress
            // edit (rather than re-reading the last-saved prefs). Consume the
            // pending value so subsequent opens start from saved prefs.
            val resume = com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit
            val initial = remember(resume) {
                if (resume != null) {
                    com.bearinmind.launcher314.data.WallpaperPreviewBus.pendingResumeEdit = null
                    resume
                } else getDeviceWallpaperEdit(context)
            }
            WallpaperEditorScreen(
                sourceFile = sourceFile,
                initialEdit = initial,
                onDismiss = { editorOpenForDevice = false },
                onApplied = {
                    cacheVersion = getWallpaperCacheVersion(context)
                    mode = WALLPAPER_MODE_DEVICE
                },
                onRequestPreviewLauncher = onPreviewLauncher
            )
        } else {
            editorOpenForDevice = false
        }
    }
}

@Composable
private fun CustomOverlayControls(
    customPath: String?,
    cacheVersion: Int,
    dim: Int,
    blur: Int,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onDimChange: (Int) -> Unit,
    onBlurChange: (Int) -> Unit
) {
    if (customPath != null) {
        androidx.compose.runtime.key(cacheVersion) {
            AsyncImage(
                model = File(customPath),
                contentDescription = "Custom wallpaper preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPick) {
            Text(if (customPath == null) "Pick image" else "Replace image")
        }
        if (customPath != null) {
            OutlinedButton(onClick = onClear) { Text("Clear") }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text("Dim", style = MaterialTheme.typography.labelMedium)
    ThumbDragHorizontalSlider(
        currentValue = dim.toFloat(),
        config = SliderConfigs.wallpaperPercent,
        onValueChange = { onDimChange(it.roundToInt()) },
        onValueChangeFinished = { onDimChange(dim) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Blur (Android 12+)", style = MaterialTheme.typography.labelMedium)
    ThumbDragHorizontalSlider(
        currentValue = blur.toFloat(),
        config = SliderConfigs.wallpaperPercent,
        enabled = android.os.Build.VERSION.SDK_INT >= 31,
        onValueChange = { onBlurChange(it.roundToInt()) },
        onValueChangeFinished = { onBlurChange(blur) }
    )
}

@Composable
private fun DeviceWallpaperControls(
    deviceSourcePath: String?,
    cacheVersion: Int,
    onPickAndEdit: () -> Unit,
    onEditAgain: () -> Unit,
    onClear: () -> Unit
) {
    Text(
        "Edits and sets the real Android wallpaper. Shows in Recents and other apps.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (deviceSourcePath != null) {
        androidx.compose.runtime.key(cacheVersion) {
            AsyncImage(
                model = File(deviceSourcePath),
                contentDescription = "Device wallpaper source preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPickAndEdit) {
            Text(if (deviceSourcePath == null) "Pick & edit…" else "Pick new image…")
        }
        if (deviceSourcePath != null) {
            Button(onClick = onEditAgain) { Text("Edit again…") }
            OutlinedButton(onClick = onClear) { Text("Clear") }
        }
    }
    // Launcher dim slider lives inside the wallpaper editor's Effects section
    // now (so it sits next to brightness/contrast/blur). No standalone slider
    // here.
}
