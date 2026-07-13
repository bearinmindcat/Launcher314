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
import com.bearinmind.launcher314.data.getDrawerSearchFuzziness
import com.bearinmind.launcher314.data.getSuggestedAppsColumns
import com.bearinmind.launcher314.data.getSuggestedAppsRows
import com.bearinmind.launcher314.data.isFuzzySearchEnabled
import com.bearinmind.launcher314.data.isRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.isSuggestedAppsEnabled
import com.bearinmind.launcher314.data.setDrawerSearchFuzziness
import com.bearinmind.launcher314.data.setFuzzySearchEnabled
import com.bearinmind.launcher314.data.setRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.setSuggestedAppsColumns
import com.bearinmind.launcher314.data.setSuggestedAppsEnabled
import com.bearinmind.launcher314.data.setSuggestedAppsRows
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlin.math.roundToInt

/** A title + subtitle row with a trailing checkbox, matching the drawer settings style. */
@Composable
private fun DrawerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Box(modifier = Modifier.width(72.dp).height(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

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
    onManageTabsClick: () -> Unit,
) {
    val context = LocalContext.current
    var fuzzyEnabled by remember { mutableStateOf(isFuzzySearchEnabled(context)) }
    var fuzziness by remember { mutableFloatStateOf(getDrawerSearchFuzziness(context).toFloat()) }
    var recentFirst by remember { mutableStateOf(isRecentFirstSearchEnabled(context)) }
    var suggestedEnabled by remember { mutableStateOf(isSuggestedAppsEnabled(context)) }
    var suggestedCols by remember { mutableFloatStateOf(getSuggestedAppsColumns(context).toFloat()) }
    var suggestedRows by remember { mutableFloatStateOf(getSuggestedAppsRows(context).toFloat()) }
    var tabsEnabled by remember {
        mutableStateOf(com.bearinmind.launcher314.ui.drawer.isDrawerTabsEnabled(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
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
                "Edit Drawer Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(8.dp))

        // Fuzzy app search + fuzziness slider
        DrawerToggleRow(
            title = "Fuzzy app search",
            subtitle = "Find apps by initials, e.g. \"yt\" → YouTube",
            checked = fuzzyEnabled,
            onCheckedChange = { fuzzyEnabled = it; setFuzzySearchEnabled(context, it) }
        )
        if (fuzzyEnabled) {
            SettingSlider(fuzziness, SliderConfigs.searchFuzziness) {
                fuzziness = it; setDrawerSearchFuzziness(context, it.roundToInt())
            }
        }

        Spacer(Modifier.height(8.dp))

        // Recently used first when searching
        DrawerToggleRow(
            title = "Recently used first when searching",
            subtitle = "Surfaces the app you opened most recently first",
            checked = recentFirst,
            onCheckedChange = { recentFirst = it; setRecentFirstSearchEnabled(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        // Suggested apps + columns/rows sliders
        DrawerToggleRow(
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

        Spacer(Modifier.height(12.dp))

        // Drawer tabs — manage button + enable checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Button(
                    onClick = onManageTabsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Manage Tab Settings") }
            }
            Box(modifier = Modifier.width(72.dp).height(48.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = tabsEnabled,
                    onCheckedChange = { checked ->
                        tabsEnabled = checked
                        com.bearinmind.launcher314.ui.drawer.setDrawerTabsEnabled(context, checked)
                    }
                )
            }
        }
        Text(
            text = "Enable drawer tabs",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 16.dp)
        )
    }
}
