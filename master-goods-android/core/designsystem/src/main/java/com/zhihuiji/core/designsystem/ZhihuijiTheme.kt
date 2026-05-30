package com.zhihuiji.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ZhihuijiColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A5F),
    secondary = ZhihuijiColors.InfoBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF1E3A5F),
    tertiary = ZhihuijiColors.Success,
    onTertiary = Color.White,
    error = ZhihuijiColors.Danger,
    onError = Color.White,
    background = ZhihuijiColors.BackgroundGradientEnd,
    onBackground = ZhihuijiColors.TextPrimary,
    surface = Color.White,
    onSurface = ZhihuijiColors.TextPrimary,
    surfaceVariant = ZhihuijiColors.SurfaceVariant,
    onSurfaceVariant = ZhihuijiColors.TextSecondary,
    outline = ZhihuijiColors.BorderLight,
    outlineVariant = ZhihuijiColors.CardBorder,
)

val LocalExtendedColors = staticCompositionLocalOf { ZhihuijiColors }

@Composable
fun ZhihuijiTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    CompositionLocalProvider(
        LocalExtendedColors provides ZhihuijiColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZhihuijiTypography,
            shapes = ZhihuijiShapes,
            content = content,
        )
    }
}

object ExtendedTheme {
    val colors: ZhihuijiColors
        @Composable get() = LocalExtendedColors.current
}
