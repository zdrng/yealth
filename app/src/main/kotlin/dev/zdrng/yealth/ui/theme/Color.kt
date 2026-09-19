package dev.zdrng.yealth.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

val Ink = Color(0xFF20163F)
val Lilac = Color(0xFFDDC6FF)
val Mint = Color(0xFFB7F6D9)
val Lemon = Color(0xFFFFF5A1)
val Blue = Color(0xFFB4DBFF)
val Electric = Color(0xFF514DFF)

/** Pastel surfaces sampled from the separate light and dark reference artwork. */
data class MockupColors(val lilac: Color, val mint: Color, val lemon: Color, val blue: Color)
val DarkMockupColors = MockupColors(Lilac, Mint, Lemon, Blue)
val LightMockupColors = MockupColors(Color(0xFFEFE4FD), Color(0xFFC9FCE0), Color(0xFFFEF7B1), Color(0xFFC8E4FD))
val LocalMockupColors = staticCompositionLocalOf { DarkMockupColors }

val YealthLightColorScheme = lightColorScheme(
    primary = Electric, onPrimary = Color.White,
    primaryContainer = LightMockupColors.lilac, onPrimaryContainer = Ink,
    secondary = Color(0xFF7151C6), onSecondary = Color.White,
    secondaryContainer = LightMockupColors.mint, onSecondaryContainer = Ink,
    tertiary = Color(0xFF958BFC), onTertiary = Color.White,
    tertiaryContainer = LightMockupColors.lemon, onTertiaryContainer = Ink,
    background = Color(0xFFFCFAFF), onBackground = Ink,
    surface = Color(0xFFFCFAFF), onSurface = Ink,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F2FC),
    surfaceContainer = Color(0xFFF0EAF9), surfaceContainerHigh = Color(0xFFEDE7F8),
    surfaceContainerHighest = Color(0xFFE4DAF5),
    surfaceVariant = Color(0xFFEEE9F7), onSurfaceVariant = Color(0xFF655681),
    outline = Color(0xFF9484AC), outlineVariant = Color(0xFFE0D6EE),
)

val YealthDarkColorScheme = darkColorScheme(
    primary = Electric, onPrimary = Color.White,
    primaryContainer = Lilac, onPrimaryContainer = Ink,
    secondary = Color(0xFFCFB0FB), onSecondary = Ink,
    secondaryContainer = Mint, onSecondaryContainer = Ink,
    tertiary = Color(0xFF7164FF), onTertiary = Color.White,
    tertiaryContainer = Lemon, onTertiaryContainer = Ink,
    background = Color(0xFF140F20), onBackground = Color(0xFFF9F2FF),
    surface = Color(0xFF140F20), onSurface = Color(0xFFF9F2FF),
    surfaceDim = Color(0xFF140F20), surfaceBright = Color(0xFF3B3050),
    surfaceContainerLowest = Color(0xFF100C18), surfaceContainerLow = Color(0xFF20192F),
    surfaceContainer = Color(0xFF29213C), surfaceContainerHigh = Color(0xFF33294B),
    surfaceContainerHighest = Color(0xFF44365F),
    surfaceVariant = Color(0xFF392D50), onSurfaceVariant = Color(0xFFCDBAE9),
    outline = Color(0xFFBDA0E4), outlineVariant = Color(0xFF4A3A62),
)
