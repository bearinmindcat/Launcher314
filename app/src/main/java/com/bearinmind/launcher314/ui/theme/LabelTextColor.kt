package com.bearinmind.launcher314.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Composition local for the global label text color. Default is White. */
val LocalLabelTextColor = compositionLocalOf { Color.White }

/** Composition local for the folder border color. Default is White at 30% alpha. */
val LocalFolderBorderColor = compositionLocalOf { Color.White.copy(alpha = 0.3f) }

/**
 * Composition local for the system-wide "hide icon text" toggle. When true,
 * label `Text` calls below app/folder/dock icons should not be rendered. Default
 * is false. Provide via `CompositionLocalProvider` from each top-level screen
 * after reading [com.bearinmind.launcher314.data.getHideIconText].
 */
val LocalHideIconText = compositionLocalOf { false }
