package com.zhihuiji.core.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

val LocalSetBottomBarVisible = compositionLocalOf<(Boolean) -> Unit> {
    error("No bottom bar visibility controller provided")
}

val LocalBottomBarVisible = compositionLocalOf { true }

@Composable
fun BottomBarScrollVisibilityEffect(
    listState: LazyListState,
    hideThresholdPx: Int = 8,
) {
    val setBottomBarVisible = LocalSetBottomBarVisible.current

    LaunchedEffect(listState, setBottomBarVisible) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { (index, offset) ->
            when {
                index < lastIndex -> setBottomBarVisible(true)
                index > lastIndex -> setBottomBarVisible(false)
                offset - lastOffset > hideThresholdPx -> setBottomBarVisible(false)
                lastOffset - offset > hideThresholdPx -> setBottomBarVisible(true)
                index == 0 && offset == 0 -> setBottomBarVisible(true)
            }
            lastIndex = index
            lastOffset = offset
        }
    }
}

@Composable
fun BottomBarScrollVisibilityEffect(
    scrollState: ScrollState,
    hideThresholdPx: Int = 8,
) {
    val setBottomBarVisible = LocalSetBottomBarVisible.current

    LaunchedEffect(scrollState, setBottomBarVisible) {
        var lastOffset = scrollState.value
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                when {
                    offset - lastOffset > hideThresholdPx -> setBottomBarVisible(false)
                    lastOffset - offset > hideThresholdPx -> setBottomBarVisible(true)
                    offset == 0 -> setBottomBarVisible(true)
                }
                lastOffset = offset
            }
    }
}

@Composable
fun BottomBarScrollToTopEffect(
    signal: Int,
    listState: LazyListState,
) {
    LaunchedEffect(signal) {
        if (signal > 0) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
fun BottomBarScrollToTopEffect(
    signal: Int,
    scrollState: ScrollState,
) {
    LaunchedEffect(signal) {
        if (signal > 0) {
            scrollState.animateScrollTo(0)
        }
    }
}
