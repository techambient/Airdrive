package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NaturalGreenPrimary,
    onPrimary = NaturalBg,
    primaryContainer = NaturalSurfaceVariant,
    onPrimaryContainer = NaturalTextPrimary,
    secondary = NaturalTextSecondary,
    onSecondary = NaturalBg,
    background = NaturalBg,
    onBackground = NaturalTextPrimary,
    surface = NaturalSurface,
    onSurface = NaturalTextPrimary,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalTextSecondary,
    outline = NaturalBorder,
    error = NaturalError,
    onError = NaturalBg
)

private val DarkColorScheme = darkColorScheme(
    primary = NaturalGreenDarkTheme,
    onPrimary = NaturalBgDark,
    primaryContainer = NaturalSurfaceVariantDark,
    onPrimaryContainer = NaturalTextPrimaryDark,
    secondary = NaturalTextSecondaryDark,
    onSecondary = NaturalBgDark,
    background = NaturalBgDark,
    onBackground = NaturalTextPrimaryDark,
    surface = NaturalSurfaceDark,
    onSurface = NaturalTextPrimaryDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalTextSecondaryDark,
    outline = NaturalBorderDark,
    error = NaturalError,
    onError = NaturalTextPrimaryDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set Natural Tones (warm light aesthetic) as default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
