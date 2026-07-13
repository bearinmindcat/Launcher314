package com.bearinmind.launcher314.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.data.getAutoLaunchSearchResult
import com.bearinmind.launcher314.data.getAutoOpenKeyboard
import com.bearinmind.launcher314.data.getReverseDrawerSearchBar
import com.bearinmind.launcher314.data.getSuggestedAppsColumns
import com.bearinmind.launcher314.data.getSuggestedAppsRows
import com.bearinmind.launcher314.data.isRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.isSuggestedAppsEnabled
import com.bearinmind.launcher314.data.setAutoLaunchSearchResult
import com.bearinmind.launcher314.data.setAutoOpenKeyboard
import com.bearinmind.launcher314.data.setRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.setReverseDrawerSearchBar
import com.bearinmind.launcher314.data.setSuggestedAppsColumns
import com.bearinmind.launcher314.data.setSuggestedAppsEnabled
import com.bearinmind.launcher314.data.setSuggestedAppsRows
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlin.math.roundToInt

@Composable
private fun SettingSlider(
    value: Float,
    config: com.bearinmind.launcher314.ui.components.HorizontalSliderConfig,
    onChange: (Float) -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 76.dp)) {
        ThumbDragHorizontalSlider(
            currentValue = value,
            config = config,
            onValueChange = onChange,
            onValueChangeFinished = {}
        )
    }
}

/**
 * Full-screen "Edit Drawer Settings" — the drawer search/behavior options moved
 * off the crowded App Drawer preview section: fuzzy search, recently-used-first,
 * suggested apps, and the tab manager.
 */
@Composable
fun EditDrawerSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var recentFirst by remember { mutableStateOf(isRecentFirstSearchEnabled(context)) }
    var suggestedEnabled by remember { mutableStateOf(isSuggestedAppsEnabled(context)) }
    var suggestedCols by remember { mutableFloatStateOf(getSuggestedAppsColumns(context).toFloat()) }
    var suggestedRows by remember { mutableFloatStateOf(getSuggestedAppsRows(context).toFloat()) }
    var reverseSearchBar by remember { mutableStateOf(getReverseDrawerSearchBar(context)) }
    var autoOpenKeyboard by remember { mutableStateOf(getAutoOpenKeyboard(context)) }
    var autoLaunchSearchResult by remember { mutableStateOf(getAutoLaunchSearchResult(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Additional Drawer Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Live drawer preview, same as the main settings screen.
        DrawerPreviewCard()

        Spacer(Modifier.height(12.dp))

        // NOTE: "Fuzzy app search" was retired here — see LegacyFeatures.kt.

        // Recently used first when searching
        SettingsToggleItem(
            title = "Recently used first when searching",
            subtitle = "Surfaces the app you opened most recently first",
            checked = recentFirst,
            onCheckedChange = { recentFirst = it; setRecentFirstSearchEnabled(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        // Suggested apps + columns/rows sliders
        SettingsToggleItem(
            title = "Suggested apps",
            subtitle = "A row of your most-used apps when you open search",
            checked = suggestedEnabled,
            onCheckedChange = { suggestedEnabled = it; setSuggestedAppsEnabled(context, it) }
        )
        if (suggestedEnabled) {
            SettingSlider(suggestedCols, SliderConfigs.suggestedColumns) {
                suggestedCols = it; setSuggestedAppsColumns(context, it.roundToInt())
            }
            SettingSlider(suggestedRows, SliderConfigs.suggestedRows) {
                suggestedRows = it; setSuggestedAppsRows(context, it.roundToInt())
            }
        }

        Spacer(Modifier.height(8.dp))

        SettingsToggleItem(
            title = "Launch app from search results",
            subtitle = "Opens the app when the search narrows to one; press enter to launch the first of several",
            checked = autoLaunchSearchResult,
            onCheckedChange = { autoLaunchSearchResult = it; setAutoLaunchSearchResult(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        SettingsToggleItem(
            title = "Reverse drawer search bar",
            subtitle = "Moves the drawer search bar to the bottom",
            checked = reverseSearchBar,
            onCheckedChange = { reverseSearchBar = it; setReverseDrawerSearchBar(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        SettingsToggleItem(
            title = "Auto open keyboard",
            subtitle = "Automatically opens the keyboard in the app drawer",
            checked = autoOpenKeyboard,
            onCheckedChange = { autoOpenKeyboard = it; setAutoOpenKeyboard(context, it) }
        )

        Spacer(Modifier.height(16.dp))
    }
}
