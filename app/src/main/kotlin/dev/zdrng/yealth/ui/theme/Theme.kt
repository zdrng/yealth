package dev.zdrng.yealth.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import dev.zdrng.yealth.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

val YealthShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val YealthRounded = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(1000))),
)

val YealthTypography = Typography(
    displayLarge = TextStyle(fontFamily = YealthRounded, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontFamily = YealthRounded, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontFamily = YealthRounded, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
)

@Composable
fun YealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Reference artwork has a deliberate palette independent of the device wallpaper.
    val colorScheme = if (darkTheme) YealthDarkColorScheme else YealthLightColorScheme

    CompositionLocalProvider(LocalMockupColors provides if (darkTheme) DarkMockupColors else LightMockupColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = YealthShapes,
            typography = YealthTypography,
            content = content,
        )
    }
}
