package com.zhihuiji.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Main bottom navigation is overlay chrome, so document lists reserve explicit clearance.
val MainBottomBarHeight = 80.dp
private val DocumentFabSize = 56.dp
private val DocumentFabBottomGap = 40.dp
private val DocumentListEndGap = 52.dp

val DocumentListFabBottomPadding: Dp
    @Composable
    get() {
        val density = LocalDensity.current
        val navigationBarHeight = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        return MainBottomBarHeight + navigationBarHeight + DocumentFabBottomGap
    }

val DocumentListBottomContentPadding: Dp
    @Composable
    get() = DocumentListFabBottomPadding + DocumentFabSize + DocumentListEndGap
