package com.bearinmind.launcher314.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance

/** Label shadow (home + drawer): only behind light text; on dark (light-theme) text it smeared the label bold (issue #123). */
fun labelShadowFor(color: Color): Shadow? =
    if (color.luminance() > 0.5f) Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 3f) else null

/** Composition local for the global label text color. Default is White. */
val LocalLabelTextColor = compositionLocalOf { Color.White }

/** Home folder card text (title, menu, names): custom label color if set, else contrast with the card. */
val LocalFolderCardTextColor = compositionLocalOf { Color.White }

/** Composition local for the folder border color. Default is White at 30% alpha. */
val LocalFolderBorderColor = compositionLocalOf { Color.White.copy(alpha = 0.3f) }

/**
 * Composition local for the system-wide "hide icon text" toggle. When true,
 * label `Text` calls below app/folder/dock icons should not be rendered. Default
 * is false. Provide via `CompositionLocalProvider` from each top-level screen
 * after reading [com.bearinmind.launcher314.data.getHideIconText].
 */
val LocalHideIconText = compositionLocalOf { false }

/** Bumped when a folder's customization is saved, so cells re-read it. */
val LocalFolderCustomizationVersion = compositionLocalOf { 0 }
