package com.bearinmind.launcher314.ui.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bitmap-based home screen preview.
 *
 * Instead of reconstructing the home screen from data (apps, folders, widgets),
 * this displays a captured screenshot of the actual home screen. The screenshot
 * is saved by LauncherWithDrawer via PixelCopy when the home screen is visible.
 *
 * Benefits:
 * - Pixel-perfect fidelity: widgets, wallpaper, icons look exactly like the real thing
 * - Much simpler code: no need to recreate widgets via AppWidgetHost
 * - Automatically stays in sync with the actual home screen
 */
@Composable
fun HomeScreenBitmapPreviewSection(
    onPreviewLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val screenshotFile = remember { File(context.filesDir, "home_screen_preview.jpg") }
    var screenshotBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Load screenshot from file
    fun loadScreenshot() {
        try {
            if (screenshotFile.exists()) {
                val opts = BitmapFactory.Options().apply {
                    // Downsample to save memory — preview is small
                    inSampleSize = 2
                }
                screenshotBitmap?.recycle()
                screenshotBitmap = BitmapFactory.decodeFile(screenshotFile.absolutePath, opts)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load on first compose
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            loadScreenshot()
        }
    }

    // Reload when screen resumes (e.g., returning from home screen)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadScreenshot()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            screenshotBitmap?.recycle()
            screenshotBitmap = null
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Preview dimensions matching the device screen aspect ratio
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val screenAspect = screenHeightDp / screenWidthDp
    val previewWidth = (screenWidthDp * 0.4f).dp
    val previewHeight = previewWidth * screenAspect

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onPreviewLauncher() }
        ) {
            val bitmap = screenshotBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                // Show the captured screenshot scaled to fit the preview box
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Home screen preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // No screenshot yet — show placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview will appear after\nvisiting the home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Play button overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onPreviewLauncher() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open Home Screen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
