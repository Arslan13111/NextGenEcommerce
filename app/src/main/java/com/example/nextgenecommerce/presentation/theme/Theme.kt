package com.example.nextgenecommerce.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkOnSurface,

    secondary = DarkSecondary,
    onSecondary = DarkOnBackground,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkOnSurface,

    tertiary = AccentBlue,
    onTertiary = DarkOnPrimary,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSecondaryText,

    error = ErrorColor,
    onError = DarkOnPrimary,

    outline = DarkOutline,
    outlineVariant = DarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurface,
    onPrimaryContainer = LightOnSurface,

    secondary = LightSecondary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightSurface,
    onSecondaryContainer = LightOnSurface,

    tertiary = AccentBlue,
    onTertiary = LightOnPrimary,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightSecondaryText,

    error = ErrorColor,
    onError = LightOnPrimary,

    outline = LightOutline,
    outlineVariant = LightDivider
)

@Composable
fun NextGenEcommerceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
