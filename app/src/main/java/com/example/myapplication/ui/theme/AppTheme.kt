package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.example.myapplication.AppThemeMode

private val lightColors = lightColorScheme()
private val darkColors = darkColorScheme()
private val appTypography = Typography()

@Composable
fun ChatAppTheme(themeMode: AppThemeMode, content: @Composable () -> Unit) {
    val colorScheme = if (themeMode == AppThemeMode.Dark) darkColors else lightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        content = content
    )
}
