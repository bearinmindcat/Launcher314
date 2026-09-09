package com.bearinmind.launcher314.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.bearinmind.launcher314.data.getExtendedGridSize
import com.bearinmind.launcher314.data.getExtendedIconSizes
import com.bearinmind.launcher314.data.getHomeOuterMarginPercent
import com.bearinmind.launcher314.data.getOuterMarginsEnabled
import com.bearinmind.launcher314.data.setExtendedGridSize
import com.bearinmind.launcher314.data.setExtendedIconSizes
import com.bearinmind.launcher314.data.setHomeOuterMarginPercent
import com.bearinmind.launcher314.data.setOuterMarginsEnabled
import com.bearinmind.launcher314.ui.components.SliderConfigs
import com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider
import kotlin.math.roundToInt

/** Experimental features — opt-in, use at own risk. */
@Composable
fun ExperimentalSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var extendedIcons by remember { mutableStateOf(getExtendedIconSizes(context)) }
    var extendedGrid by remember { mutableStateOf(getExtendedGridSize(context)) }

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
                "Experimental Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SettingsToggleItem(
                title = "Extended icon sizes",
                subtitle = "Icon size sliders go up to 200%",
                checked = extendedIcons,
                onCheckedChange = {
                    extendedIcons = it
                    setExtendedIconSizes(context, it)
                }
            )
            SettingsToggleItem(
                title = "Extended grid size",
                subtitle = "Increased grid to 20x20; also applies to dock",
                checked = extendedGrid,
                onCheckedChange = {
                    extendedGrid = it
                    setExtendedGridSize(context, it)
                }
            )
            // Outer margins (issue #106): 100 = stock spacing, 0 = grid flush with the screen edges; checkbox enables the slider (Font / "Hide text" pattern).
            var outerMarginsOn by remember { mutableStateOf(getOuterMarginsEnabled(context)) }
            var outerMargin by remember { mutableFloatStateOf(getHomeOuterMarginPercent(context).toFloat()) }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    ThumbDragHorizontalSlider(
                        currentValue = outerMargin,
                        config = SliderConfigs.homeOuterMargin,
                        enabled = outerMarginsOn,
                        onValueChange = {
                            outerMargin = it
                            setHomeOuterMarginPercent(context, it.roundToInt())
                        },
                        onValueChangeFinished = {
                            setHomeOuterMarginPercent(context, outerMargin.roundToInt())
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = outerMarginsOn,
                        onCheckedChange = {
                            outerMarginsOn = it
                            setOuterMarginsEnabled(context, it)
                        },
                        modifier = Modifier.offset(x = 10.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
