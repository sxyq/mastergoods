package com.zhihuiji.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val baseLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun zhihuijiTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight,
    lineHeightStyle = baseLineHeightStyle,
)

val ZhihuijiTypography = Typography(
    displayLarge = zhihuijiTextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp),
    displayMedium = zhihuijiTextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    headlineLarge = zhihuijiTextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp),
    headlineMedium = zhihuijiTextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleLarge = zhihuijiTextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleMedium = zhihuijiTextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    titleSmall = zhihuijiTextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    bodyLarge = zhihuijiTextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodyMedium = zhihuijiTextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    bodySmall = zhihuijiTextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 15.sp),
    labelLarge = zhihuijiTextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp),
    labelMedium = zhihuijiTextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp),
    labelSmall = zhihuijiTextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp),
)
