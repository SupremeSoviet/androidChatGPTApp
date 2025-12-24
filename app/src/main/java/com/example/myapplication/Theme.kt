package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppThemeMode

private val LightColors = lightColorScheme(
    primary = YandexYellow,
    onPrimary = TextOnYellow,
    secondary = YandexRed,
    background = AppBackground,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    surfaceVariant = InputBackground,
    onSurfaceVariant = TextSecondary
)

private val DarkColors = darkColorScheme(
    primary = YandexYellow,
    onPrimary = TextOnYellow,
    secondary = YandexRed,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2A2A2A),
    onSurface = Color(0xFFE0E0E0),
    onBackground = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = TextSecondary
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ChatAppTheme(
    themeMode: AppThemeMode = AppThemeMode.Light,
    content: @Composable () -> Unit
) {
    val colors = when (themeMode) {
        AppThemeMode.Light -> LightColors
        AppThemeMode.Dark -> DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}