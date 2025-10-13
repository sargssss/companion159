package com.lifelover.companion159.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Military Dark Theme - main theme for the app
 * Designed for military/tactical use with high readability
 */
private val MilitaryDarkColorScheme = darkColorScheme(
    // Primary - Military Green (main actions, buttons)
    primary = MilitaryGreen,
    onPrimary = MilitaryWhite,
    primaryContainer = MilitaryGreenLight,
    onPrimaryContainer = MilitaryWhite,

    // Secondary - Military Olive (secondary actions)
    secondary = MilitaryOlive,
    onSecondary = MilitaryWhite,
    secondaryContainer = MilitaryTan,
    onSecondaryContainer = MilitaryDarkGray,

    // Tertiary - Military Khaki (accents)
    tertiary = MilitaryKhaki,
    onTertiary = MilitaryDarkGray,
    tertiaryContainer = MilitaryTan,
    onTertiaryContainer = MilitaryDarkGray,

    // Background
    background = MilitaryDarkGray,
    onBackground = MilitaryWhite,

    // Surface (cards, dialogs)
    surface = MilitaryGray,
    onSurface = MilitaryWhite,
    surfaceVariant = MilitaryLightGray,
    onSurfaceVariant = MilitaryKhaki,

    // Error (delete, warnings)
    error = MilitaryRed,
    onError = MilitaryWhite,
    errorContainer = MilitaryRed.copy(alpha = 0.2f),
    onErrorContainer = MilitaryRed,

    // Outline
    outline = MilitaryTan.copy(alpha = 0.5f),
    outlineVariant = MilitaryLightGray
)

/**
 * Military Light Theme - for daytime use
 * Higher contrast for outdoor visibility
 */
private val MilitaryLightColorScheme = lightColorScheme(
    // Primary - Military Green
    primary = MilitaryGreen,
    onPrimary = MilitaryWhite,
    primaryContainer = MilitaryGreenLight,
    onPrimaryContainer = MilitaryDarkGray,

    // Secondary - Military Olive
    secondary = MilitaryOlive,
    onSecondary = MilitaryWhite,
    secondaryContainer = MilitaryBeige,
    onSecondaryContainer = MilitaryDarkGray,

    // Tertiary - Military Tan
    tertiary = MilitaryTan,
    onTertiary = MilitaryWhite,
    tertiaryContainer = MilitaryLightTan,
    onTertiaryContainer = MilitaryDarkGray,

    // Background
    background = MilitarySand,
    onBackground = MilitaryDarkGray,

    // Surface (cards, dialogs)
    surface = MilitaryWhite,
    onSurface = MilitaryDarkGray,
    surfaceVariant = MilitaryBeige,
    onSurfaceVariant = MilitaryGray,

    // Error
    error = MilitaryRed,
    onError = MilitaryWhite,
    errorContainer = MilitaryRed.copy(alpha = 0.1f),
    onErrorContainer = MilitaryRed,

    // Outline
    outline = MilitaryTan,
    outlineVariant = MilitaryLightGray
)

/**
 * Main theme for Companion159 app
 *
 * @param darkTheme Use dark theme
 * @param useDynamicColor Use Material You dynamic colors (Android 12+)
 *                        Set to false to always use military theme
 * @param content Composable content
 */
@Composable
fun Companion159Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,  // Changed: default to false for military theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic colors (Material You) - only if explicitly enabled
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Military theme (default)
        darkTheme -> MilitaryDarkColorScheme
        else -> MilitaryLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}