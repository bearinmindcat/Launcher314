package com.bearinmind.launcher314.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Composition local for the global label text color. Default is White. */
val LocalLabelTextColor = compositionLocalOf { Color.White }

/** Composition local for the folder border color. Default is White at 30% alpha. */
val LocalFolderBorderColor = compositionLocalOf { Color.White.copy(alpha = 0.3f) }
