package com.zhihuiji.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ZhihuijiPrimary,
    onPrimary = Color.White,
    primaryContainer = ZhihuijiPrimaryLight,
    onPrimaryContainer = ZhihuijiPrimaryDark,
    secondary = SuccessGreen,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,
    outline = DividerLight,
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = ZhihuijiPrimary,
    onPrimary = Color.White,
    primaryContainer = ZhihuijiPrimaryDark,
    onPrimaryContainer = ZhihuijiPrimaryLight,
    secondary = SuccessGreen,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444),
    error = DangerRed,
    onError = Color.White
)

@Composable
fun ZhihuijiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZhihuijiTypography,
        shapes = ZhihuijiShapes,
        content = content
    )
}
