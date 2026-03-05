package com.bearinmind.launcher314.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Updated color schemes with white navigation bar
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9E9E9E),       // Medium Gray
    secondary = Color(0xFF757575),      // Darker Gray
    tertiary = Color(0xFF616161),       // Even Darker Gray
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A), // For dark mode
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE0E0E0),        // Light Gray for top bar
    secondary = Color(0xFF9E9E9E),      // Medium Gray
    tertiary = Color(0xFF616161),       // Dark Gray
    background = Color(0xFFF5F5F5),     // Very Light Gray
    surface = Color(0xFFFFFFFF),        // White - THIS IS KEY FOR NAV BAR
    surfaceVariant = Color(0xFFFFFFFF), // White
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF212121),   // Very Dark Gray
    onSurface = Color(0xFF212121)       // Very Dark Gray
)

@Composable
fun Launcher314Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // In launcher mode, the wallpaper theme + applyTransparentNavigation()
            // already set transparent bars — don't override them.
            // Detect launcher mode by checking if nav bar is already transparent.
            val isLauncher = window.navigationBarColor == android.graphics.Color.TRANSPARENT

            if (!isLauncher) {
                window.statusBarColor = if (darkTheme) {
                    Color(0xFF121212).toArgb()
                } else {
                    Color.White.toArgb()
                }

                window.navigationBarColor = if (darkTheme) {
                    Color(0xFF2A2A2A).toArgb()
                } else {
                    Color.White.toArgb()
                }

                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}