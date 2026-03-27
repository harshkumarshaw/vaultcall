package com.vaultcall.ui.theme

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

/**
 * VaultCall Material 3 theme.
 *
 * Supports Dark (default), Light, and AMOLED modes.
 * Dark mode is default to match the security/vault branding.
 */

/** Theme mode enum used in settings. */
enum class ThemeMode { LIGHT, DARK, AMOLED }

private val DarkColorScheme = darkColorScheme(
    primary = VaultTeal,
    onPrimary = TextOnPrimary,
    primaryContainer = VaultTealDark,
    onPrimaryContainer = VaultTealLight,
    secondary = VaultCyan,
    onSecondary = TextOnPrimary,
    secondaryContainer = VaultCyanDark,
    onSecondaryContainer = VaultCyanLight,
    tertiary = CallScreened,
    background = DarkSurface,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = VaultTealDark,
    onPrimary = LightSurfaceElevated,
    primaryContainer = VaultTealLight.copy(alpha = 0.3f),
    onPrimaryContainer = VaultTealDark,
    secondary = VaultCyanDark,
    onSecondary = LightSurfaceElevated,
    secondaryContainer = VaultCyanLight.copy(alpha = 0.3f),
    onSecondaryContainer = VaultCyanDark,
    tertiary = CallScreened,
    background = LightSurface,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = ErrorRed,
    onError = LightSurfaceElevated,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed
)

private val AmoledColorScheme = darkColorScheme(
    primary = VaultTeal,
    onPrimary = TextOnPrimary,
    primaryContainer = VaultTealDark,
    onPrimaryContainer = VaultTealLight,
    secondary = VaultCyan,
    onSecondary = TextOnPrimary,
    secondaryContainer = VaultCyanDark,
    onSecondaryContainer = VaultCyanLight,
    tertiary = CallScreened,
    background = AmoledSurface,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed
)

@Composable
fun VaultCallTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
            insetsController.isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultCallTypography,
        content = content
    )
}
