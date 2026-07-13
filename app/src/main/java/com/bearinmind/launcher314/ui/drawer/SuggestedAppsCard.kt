package com.bearinmind.launcher314.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.AppInfo
import java.io.File

/**
 * "Suggested apps" card at the top of the drawer — a frecency-ranked grid of the
 * apps you use most, One UI / prediction-row style. Rendered only when the
 * feature is on and the drawer isn't being searched. `apps` is already the
 * ranked, capped (columns × rows) list.
 */
@Composable
fun SuggestedAppsCard(
    apps: List<AppInfo>,
    columns: Int,
    iconSize: Int,
    labelFontSize: androidx.compose.ui.unit.TextUnit,
    labelFontFamily: FontFamily?,
    globalIconShapeName: String?,
    iconBgColor: Int?,
    onAppClick: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) return
    val context = LocalContext.current

    // Match the search card's grey (#3B3B3B), no outline.
    val cardShape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(cardShape)
            .background(Color(0xFF3B3B3B))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Suggested apps",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // Chunk the ranked list into rows of `columns`; pad the last row with
        // invisible weight-1 spacers so items stay column-aligned.
        apps.chunked(columns).forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowApps.forEach { app ->
                    val shaped = remember(app.packageName, globalIconShapeName, iconBgColor) {
                        com.bearinmind.launcher314.helpers.peekShapedIconCache(
                            context, app.packageName, globalIconShapeName, iconBgColor
                        )
                    }
                    val iconPath = shaped ?: app.iconPath
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppClick(app) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = File(iconPath),
                            contentDescription = app.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(iconSize.dp)
                        )
                        if (!com.bearinmind.launcher314.ui.theme.LocalHideIconText.current) {
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = app.name,
                                fontSize = labelFontSize,
                                fontFamily = labelFontFamily ?: FontFamily.Default,
                                color = com.bearinmind.launcher314.ui.theme.LocalLabelTextColor.current,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                // Keep the last row's columns aligned.
                repeat(columns - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
