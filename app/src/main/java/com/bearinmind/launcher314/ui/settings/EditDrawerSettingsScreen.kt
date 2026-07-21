package com.bearinmind.launcher314.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearinmind.launcher314.data.getAutoLaunchSearchResult
import com.bearinmind.launcher314.data.getAutoOpenKeyboard
import com.bearinmind.launcher314.data.getDrawerSearchFuzziness
import com.bearinmind.launcher314.data.getHideDrawerSearchBar
import com.bearinmind.launcher314.data.getReverseDrawerSearchBar
import com.bearinmind.launcher314.data.getScrollbarColor
import com.bearinmind.launcher314.data.getScrollbarHeightPercent
import com.bearinmind.launcher314.data.getScrollbarIntensity
import com.bearinmind.launcher314.data.getScrollbarWidthPercent
import com.bearinmind.launcher314.data.isFuzzySearchEnabled
import com.bearinmind.launcher314.data.isRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.isSuggestedAppsEnabled
import com.bearinmind.launcher314.data.setAutoLaunchSearchResult
import com.bearinmind.launcher314.data.setAutoOpenKeyboard
import com.bearinmind.launcher314.data.setDrawerSearchFuzziness
import com.bearinmind.launcher314.data.setFuzzySearchEnabled
import com.bearinmind.launcher314.data.setHideDrawerSearchBar
import com.bearinmind.launcher314.data.setRecentFirstSearchEnabled
import com.bearinmind.launcher314.data.setReverseDrawerSearchBar
import com.bearinmind.launcher314.data.setSuggestedAppsEnabled
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import com.bearinmind.launcher314.ui.components.VerticalScrollbar
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
    onPreviewDrawer: () -> Unit = {},
) {
    val context = LocalContext.current
    var fuzzyEnabled by remember { mutableStateOf(isFuzzySearchEnabled(context)) }
    var fuzziness by remember { mutableFloatStateOf(getDrawerSearchFuzziness(context).toFloat()) }
    var recentFirst by remember { mutableStateOf(isRecentFirstSearchEnabled(context)) }
    var suggestedEnabled by remember { mutableStateOf(isSuggestedAppsEnabled(context)) }
    var reverseSearchBar by remember { mutableStateOf(getReverseDrawerSearchBar(context)) }
    var hideSearchBar by remember { mutableStateOf(getHideDrawerSearchBar(context)) }
    var autoOpenKeyboard by remember { mutableStateOf(getAutoOpenKeyboard(context)) }
    var autoLaunchSearchResult by remember { mutableStateOf(getAutoLaunchSearchResult(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
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

        // Live drawer preview — PINNED at the top (outside the scroll below).
        // Pass the live toggle so hiding the search bar updates the preview at once.
        DrawerPreviewCard(onPlayClick = onPreviewDrawer, hideSearchBar = hideSearchBar)

        Spacer(Modifier.height(12.dp))

        // Only this settings section scrolls; the preview above stays fixed.
        val settingsScroll = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(settingsScroll)
        ) {

        // Fuzzy app search + fuzziness slider
        SettingsToggleItem(
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
        SettingsToggleItem(
            title = "Recency search bias",
            subtitle = "While searching, ranks apps you've opened recently higher in the results",
            checked = recentFirst,
            onCheckedChange = { recentFirst = it; setRecentFirstSearchEnabled(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        // Suggested apps — the bar auto-sizes to (drawer columns - 1) × 1 row,
        // so there are no size sliders to tune.
        SettingsToggleItem(
            title = "Suggested apps bar",
            subtitle = "Bar at the top of the app list open showing your most-used apps when you search",
            checked = suggestedEnabled,
            onCheckedChange = { suggestedEnabled = it; setSuggestedAppsEnabled(context, it) }
        )

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
            title = "Hide search bar",
            checked = hideSearchBar,
            onCheckedChange = { hideSearchBar = it; setHideDrawerSearchBar(context, it) }
        )

        Spacer(Modifier.height(8.dp))

        SettingsToggleItem(
            title = "Auto open keyboard",
            subtitle = "Automatically opens the keyboard in the app drawer",
            checked = autoOpenKeyboard,
            onCheckedChange = { autoOpenKeyboard = it; setAutoOpenKeyboard(context, it) }
        )

        Spacer(Modifier.height(16.dp))
        } // end scrollable Column

        // Scrollbar styled by the user's Scroll Bar / Navigation settings (same
        // width / height / color / intensity mapping the drawer scrollbar uses).
        val sbWidthPct = getScrollbarWidthPercent(context)
        val sbHeightPct = getScrollbarHeightPercent(context)
        val sbScreenW = LocalConfiguration.current.screenWidthDp.toFloat()
        val sbScreenH = LocalConfiguration.current.screenHeightDp.toFloat()
        val sbWidth = (sbScreenW * 0.02f * sbWidthPct / 100f).toInt().coerceAtLeast(1)
        val sbHeight = (sbScreenH * 0.20f * sbHeightPct / 100f).toInt().coerceAtLeast(8)
        val sbBase = Color(getScrollbarColor(context))
        val sbFactor = (getScrollbarIntensity(context) / 100f).coerceIn(0f, 1f)
        val sbColor = Color(sbBase.red * sbFactor, sbBase.green * sbFactor, sbBase.blue * sbFactor, sbBase.alpha)
        VerticalScrollbar(
            scrollState = settingsScroll,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp, end = 2.dp),
            thumbColor = sbColor.copy(alpha = 0.3f),
            thumbSelectedColor = sbColor.copy(alpha = 0.9f),
            thumbWidth = sbWidth.dp,
            thumbMinHeight = sbHeight.dp,
            hideDelayMillis = 1500,
            alwaysShow = true
        )
        } // end Box
    }
}
