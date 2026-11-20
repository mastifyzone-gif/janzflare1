package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val JanzDarkColorScheme = darkColorScheme(
    primary = FlareOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A0B),
    onPrimaryContainer = FlareOrangeLight,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF281A4B),
    onSecondaryContainer = ElectricVioletLight,
    tertiary = FlareAmber,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = MidnightNavy,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepNavySurface,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DeepNavySurface,
    surfaceContainerHigh = ElevatedNavyCard,
    outline = SlateBorderDark,
    error = ErrorCrimson,
    onError = Color.White
)

private val JanzLightColorScheme = lightColorScheme(
    primary = FlareOrangeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5D9),
    onPrimaryContainer = Color(0xFF4A1802),
    secondary = ElectricVioletDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = FlareAmber,
    onTertiary = TextPrimaryLight,
    background = CrispWhiteBg,
    onBackground = TextPrimaryLight,
    surface = PureSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftTintLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = SoftTintLight,
    surfaceContainerHigh = PureSurfaceLight,
    outline = SlateBorderLight,
    error = ErrorCrimson,
    onError = Color.White
)

val JanzShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JanzFlareTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = if (useDark) JanzDarkColorScheme else JanzLightColorScheme,
        typography = JanzTypography,
        shapes = JanzShapes,
        content = content
    )
}
