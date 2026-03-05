package com.bearinmind.launcher314.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.ui.theme.Launcher314Theme
import com.bearinmind.launcher314.services.AppDrawerAccessibilityService

/**
 * AccessibilitySettingsActivity
 *
 * This activity is shown when the user opens the accessibility service settings
 * in Android Settings > Accessibility > App Drawer.
 *
 * It displays the "App Drawer shortcut" toggle that controls whether
 * tapping the accessibility button opens the app drawer.
 *
 * This mimics how KISS launcher shows its shortcut toggle in Android settings.
 */
class AccessibilitySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Launcher314Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AccessibilitySettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen() {
    val context = LocalContext.current
    var shortcutEnabled by remember { mutableStateOf(AppDrawerAccessibilityService.isEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "App Drawer",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Drawer shortcut toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        shortcutEnabled = !shortcutEnabled
                        AppDrawerAccessibilityService.setEnabled(context, shortcutEnabled)
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "App Drawer shortcut",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (shortcutEnabled)
                            "Tap accessibility button to open drawer"
                        else
                            "Enable to use accessibility button",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = shortcutEnabled,
                    onCheckedChange = { checked ->
                        shortcutEnabled = checked
                        AppDrawerAccessibilityService.setEnabled(context, checked)
                    }
                )
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // Settings link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Open main app settings
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.let { context.startActivity(it) }
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // App info link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Open app info in Android settings
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App info",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
