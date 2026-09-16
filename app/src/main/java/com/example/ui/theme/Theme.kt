package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = ForestGreenDark,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = MintPill,
    onSecondaryContainer = ForestGreenDark,
    tertiary = ForestGreenLight,
    background = BackgroundCream,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = MintLight,
    onSurfaceVariant = TextSecondaryMuted,
    outline = MintBorder,
    error = ErrorRed,
    onError = Color.White,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldSoft,
    onPrimary = ForestGreenDark,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = MintLight,
    secondary = EmeraldAccent,
    onSecondary = Color.Black,
    background = Color(0xFF0F1E17),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF162B22),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF1E3A2E),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF2D5A46),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep intentional brand identity by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
