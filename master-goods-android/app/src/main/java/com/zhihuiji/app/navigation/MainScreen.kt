package com.zhihuiji.app.navigation

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.zhihuiji.core.designsystem.GlassShadow
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.MainBottomBarHeight
import com.zhihuiji.core.designsystem.ZhihuijiPrimaryBright
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.abs
import kotlin.math.roundToInt

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomBarDestinations = listOf(
    BottomNavItem(TabRoutes.HOME, "首页", Icons.Filled.Home),
    BottomNavItem(TabRoutes.DOCUMENTS, "单据", Icons.Filled.Description),
    BottomNavItem(TabRoutes.ARCHIVES, "档案", Icons.Filled.Inventory),
    BottomNavItem(TabRoutes.REPORTS, "报表", Icons.Filled.Analytics),
    BottomNavItem(TabRoutes.AGENT, "助手", Icons.Filled.SmartToy),
)

private val BottomNavInactive = Color(0xFF5D6571)
private val BottomBarGlassSurface = Color(0xFFEAF2FF).copy(alpha = 0.88f)
private val BottomBarHorizontalMargin = 12.dp
private val BottomBarFloatingBottomGap = 10.dp
private val BottomBarContainerShape = RoundedCornerShape(32.dp)
private val BottomBarIndicatorShape = RoundedCornerShape(27.dp)
private val BottomBarBlurRadius = 52.dp
private val BottomBarIndicatorBlurRadius = 42.dp
private val BottomBarContentHorizontalPadding = 3.dp
private val BottomBarContentVerticalPadding = 3.dp
private val BottomBarIndicatorBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.92f),
        Color(0xFFF6FAFF).copy(alpha = 0.84f),
        Color(0xFFDCE8F7).copy(alpha = 0.78f),
    )
)

private const val BottomBarAnimationDurationMillis = 240
private const val BottomBarClickPulseDurationMillis = 180
private const val BottomBarRepeatedTapDebounceMillis = 260L
private const val BottomBarSelectionSpringDamping = 0.88f
private const val BottomBarSelectionSpringStiffness = 320f
private const val BottomBarSelectionFarSpringStiffness = 210f
private const val BottomBarVelocityNormalization = 3600f
private const val BottomBarVelocityScaleClamp = 0.12f
private const val BottomBarHorizontalIntentRatio = 1.15f
private const val BottomBarVerticalIntentRatio = 1.08f

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    startupAgentLaunch: AgentLaunchRequest? = null,
) {
    val navController = rememberNavController()
    val accessViewModel: MainAccessViewModel = hiltViewModel()
    val accessState by accessViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current

    if (!accessState.isResolved && accessState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val visibleBottomBarDestinations = remember(accessState.isResolved, accessState.permissions) {
        bottomBarDestinations.filter { accessState.canAccessRoute(it.route) }
    }
    val selectedIndex = remember(visibleBottomBarDestinations, currentRoute) {
        visibleBottomBarDestinations.indexOfFirst { currentRoute.matchesTopLevelRoute(it.route) }
            .takeIf { it >= 0 } ?: 0
    }
    val bottomBarScrollEvents = remember {
        MutableSharedFlow<Float>(extraBufferCapacity = 64)
    }
    var pendingStartupAgentLaunch by remember(startupAgentLaunch) {
        mutableStateOf(
            startupAgentLaunch?.takeIf {
                it.openChat || !it.initialQuestion.isNullOrBlank() || it.conversationId != null
            }
        )
    }

    LaunchedEffect(accessState.isResolved, accessState.permissions, currentRoute) {
        if (!accessState.isResolved || currentRoute == null || accessState.canAccessRoute(currentRoute)) {
            return@LaunchedEffect
        }
        val isTopLevelRoute = bottomBarDestinations.any { currentRoute.matchesTopLevelRoute(it.route) }
        if (isTopLevelRoute) {
            val fallbackRoute = accessState.firstAllowedTopLevelRoute()
            if (!currentRoute.matchesTopLevelRoute(fallbackRoute)) {
                navController.navigate(fallbackRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    LaunchedEffect(pendingStartupAgentLaunch, accessState.isResolved, accessState.permissions) {
        val request = pendingStartupAgentLaunch ?: return@LaunchedEffect
        if (!accessState.isResolved) {
            return@LaunchedEffect
        }
        if (!accessState.canAccessRoute(TabRoutes.AGENT)) {
            pendingStartupAgentLaunch = null
            return@LaunchedEffect
        }
        navController.navigate(agentChatRoute(request.initialQuestion, request.conversationId)) {
            launchSingleTop = true
        }
        pendingStartupAgentLaunch = null
    }

    GlassScaffold(
        bottomBar = {
            MainBottomBar(
                destinations = visibleBottomBarDestinations,
                currentRoute = currentRoute,
                selectedIndex = selectedIndex,
                density = density,
                backdrop = bottomBarBackdrop,
                bottomBarScrollEvents = bottomBarScrollEvents,
                onNavigate = { index, route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    ) { paddingValues ->
        MainNavGraph(
            navController = navController,
            selectedIndex = selectedIndex,
            homeBottomBarScrollEvents = bottomBarScrollEvents,
            onNavigateToSettings = onNavigateToSettings,
            accessState = accessState,
            modifier = Modifier
                .padding(paddingValues)
                .layerBackdrop(bottomBarBackdrop)
        )
    }
}

@Composable
private fun MainBottomBar(
    destinations: List<BottomNavItem>,
    currentRoute: String?,
    selectedIndex: Int,
    density: androidx.compose.ui.unit.Density,
    backdrop: Backdrop,
    bottomBarScrollEvents: MutableSharedFlow<Float>,
    onNavigate: (Int, String) -> Unit,
) {
    if (destinations.isEmpty() || !destinations.any { currentRoute.matchesTopLevelRoute(it.route) }) {
        return
    }

    var pendingTapIndex by remember { mutableStateOf<Int?>(null) }
    var lastBottomBarTapIndex by remember { mutableStateOf<Int?>(null) }
    var lastBottomBarTapTime by remember { mutableStateOf(0L) }
    var isBottomBarDragging by remember { mutableStateOf(false) }
    var bottomBarDragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var bottomBarDragVelocity by remember { mutableFloatStateOf(0f) }
    var bottomBarNavigationStartIndex by remember { mutableStateOf(selectedIndex) }

    fun navigateTopLevel(index: Int, route: String) {
        val now = android.os.SystemClock.uptimeMillis()
        if (
            lastBottomBarTapIndex == index &&
            now - lastBottomBarTapTime < BottomBarRepeatedTapDebounceMillis
        ) {
            return
        }
        lastBottomBarTapIndex = index
        lastBottomBarTapTime = now
        pendingTapIndex = index
        onNavigate(index, route)
    }

    val targetIndicatorIndex = pendingTapIndex ?: selectedIndex
    val bottomBarNavigationDistance = abs(targetIndicatorIndex - bottomBarNavigationStartIndex)
    val bottomBarNavigationDuration = resolveBottomBarNavigationDurationMillis(
        currentIndex = bottomBarNavigationStartIndex,
        targetIndex = targetIndicatorIndex
    )

    LaunchedEffect(selectedIndex, pendingTapIndex) {
        if (!isBottomBarDragging) {
            bottomBarDragPosition = (pendingTapIndex ?: selectedIndex).toFloat()
        }
        if (pendingTapIndex == selectedIndex) {
            pendingTapIndex = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = BottomBarHorizontalMargin,
                        end = BottomBarHorizontalMargin,
                        bottom = BottomBarFloatingBottomGap
                    ),
                blurRadius = BottomBarBlurRadius,
                shape = BottomBarContainerShape,
                surfaceColor = BottomBarGlassSurface,
                backdrop = backdrop
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MainBottomBarHeight - BottomBarFloatingBottomGap)
                        .padding(
                            horizontal = BottomBarContentHorizontalPadding,
                            vertical = BottomBarContentVerticalPadding
                        )
                ) {
                    val itemCount = destinations.size.coerceAtLeast(1)
                    val slotWidth = maxWidth / itemCount
                    val slotWidthPx = with(density) { slotWidth.toPx() }.coerceAtLeast(1f)
                    val indicatorWidth = slotWidth
                    val indicatorHeight = (
                        MainBottomBarHeight -
                            BottomBarFloatingBottomGap -
                            (BottomBarContentVerticalPadding * 2)
                        ).coerceAtMost(slotWidth / 1.6f)
                    val indicatorPosition by animateFloatAsState(
                        targetValue = if (isBottomBarDragging) {
                            bottomBarDragPosition
                        } else {
                            (pendingTapIndex ?: selectedIndex).toFloat()
                        },
                        animationSpec = spring(
                            dampingRatio = if (bottomBarNavigationDistance > 1) {
                                BottomBarSelectionSpringDamping + 0.04f
                            } else {
                                BottomBarSelectionSpringDamping
                            },
                            stiffness = if (bottomBarNavigationDistance > 1) {
                                BottomBarSelectionFarSpringStiffness
                            } else {
                                BottomBarSelectionSpringStiffness
                            },
                            visibilityThreshold = 0.01f
                        ),
                        label = "bottom_bar_indicator_position"
                    )
                    val velocityScaleTarget = (
                        abs(bottomBarDragVelocity) / BottomBarVelocityNormalization
                        ).coerceAtMost(BottomBarVelocityScaleClamp)
                    val indicatorVelocityScale by animateFloatAsState(
                        targetValue = if (isBottomBarDragging) velocityScaleTarget else 0f,
                        animationSpec = tween(
                            durationMillis = BottomBarAnimationDurationMillis,
                            easing = LinearOutSlowInEasing
                        ),
                        label = "bottom_bar_velocity_scale"
                    )
                    val tapPulseTarget = if (pendingTapIndex != null && !isBottomBarDragging) 1f else 0f
                    val tapPulse by animateFloatAsState(
                        targetValue = tapPulseTarget,
                        animationSpec = tween(
                            durationMillis = if (tapPulseTarget > 0f) {
                                bottomBarNavigationDuration.coerceAtMost(420)
                            } else {
                                BottomBarClickPulseDurationMillis
                            },
                            easing = LinearOutSlowInEasing
                        ),
                        label = "bottom_bar_click_pulse"
                    )
                    val indicatorOffsetPx = with(density) { (slotWidth * indicatorPosition).toPx() }

                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(indicatorHeight)
                            .align(Alignment.CenterStart)
                            .graphicsLayer {
                                translationX = indicatorOffsetPx
                                scaleX = 1f + indicatorVelocityScale * 0.46f + tapPulse * 0.04f
                                scaleY = 1f - indicatorVelocityScale * 0.16f + tapPulse * 0.02f
                            }
                            .bottomNavGlassIndicator(
                                backdrop = backdrop,
                                shape = BottomBarIndicatorShape
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .bottomNavSweepGesture(
                                itemWidthPx = slotWidthPx,
                                itemCount = itemCount,
                                currentIndicatorPosition = indicatorPosition,
                                onDragPositionChange = { position ->
                                    bottomBarDragPosition = position
                                },
                                onDragStateChange = { dragging ->
                                    isBottomBarDragging = dragging
                                    if (!dragging) {
                                        bottomBarDragVelocity = 0f
                                    }
                                },
                                onVelocityChange = { velocity ->
                                    bottomBarDragVelocity = velocity
                                },
                                onVerticalDrag = {
                                    if (selectedIndex == 0 && it != 0f) {
                                        bottomBarScrollEvents.tryEmit(it)
                                    }
                                },
                                onSelected = { index ->
                                    destinations.getOrNull(index)?.let { dest ->
                                        bottomBarNavigationStartIndex = selectedIndex
                                        navigateTopLevel(index, dest.route)
                                    }
                                }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            BottomNavTab(
                                destination = dest,
                                selected = index == selectedIndex,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    bottomBarNavigationStartIndex = selectedIndex
                                    navigateTopLevel(index, dest.route)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private fun String?.matchesTopLevelRoute(route: String): Boolean =
    this == route || this?.startsWith("$route?") == true

private fun resolveBottomBarNavigationDurationMillis(
    currentIndex: Int,
    targetIndex: Int
): Int {
    val distance = abs(targetIndex - currentIndex).coerceAtLeast(2)
    return 100 * distance + 100
}

@Composable
private fun BottomNavTab(
    destination: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentColor by animateColorAsState(
        targetValue = if (selected) ZhihuijiPrimaryBright else BottomNavInactive,
        animationSpec = tween(
            durationMillis = BottomBarAnimationDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${destination.label}_bottom_nav_content_color"
    )
    val tabScale = animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = if (isPressed) 90 else BottomBarAnimationDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${destination.label}_bottom_nav_scale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(80.dp)
                .height(54.dp)
                .graphicsLayer {
                    scaleX = tabScale.value
                    scaleY = tabScale.value
                }
                .padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = destination.label,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

private fun Modifier.bottomNavGlassIndicator(
    backdrop: Backdrop,
    shape: RoundedCornerShape
): Modifier {
    val chrome = this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow.copy(alpha = 0.18f),
            spotColor = GlassShadow.copy(alpha = 0.22f)
        )

    val core = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        chrome
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(BottomBarIndicatorBlurRadius.toPx())
                },
                highlight = {
                    Highlight.Default.copy(alpha = 1f)
                },
                shadow = {
                    Shadow(
                        color = GlassShadow.copy(alpha = 0.18f),
                        alpha = 0.42f
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 8.dp,
                        alpha = 0.48f,
                        color = Color.White.copy(alpha = 0.36f)
                    )
                },
                onDrawSurface = {
                    drawRect(brush = BottomBarIndicatorBrush)
                }
            )
    } else {
        chrome
            .background(
                brush = BottomBarIndicatorBrush,
                shape = shape
            )
    }
    return core.border(
        width = 0.5.dp,
        color = Color.White.copy(alpha = 0.56f),
        shape = shape
    )
}

@Composable
private fun Modifier.bottomNavSweepGesture(
    itemWidthPx: Float,
    itemCount: Int,
    currentIndicatorPosition: Float,
    onDragPositionChange: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onVelocityChange: (Float) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onSelected: (Int) -> Unit
): Modifier {
    val currentIndicatorPositionState = rememberUpdatedState(currentIndicatorPosition)
    val onDragPositionChangeState = rememberUpdatedState(onDragPositionChange)
    val onDragStateChangeState = rememberUpdatedState(onDragStateChange)
    val onVelocityChangeState = rememberUpdatedState(onVelocityChange)
    val onVerticalDragState = rememberUpdatedState(onVerticalDrag)
    val onSelectedState = rememberUpdatedState(onSelected)

    return pointerInput(itemWidthPx, itemCount) {
    if (itemWidthPx <= 0f || itemCount <= 1) return@pointerInput

    val velocityTracker = VelocityTracker()
    val touchSlop = viewConfiguration.touchSlop
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            var latestPositionX = down.position.x
            var latestVelocityX = 0f
            val startIndicatorPosition = currentIndicatorPositionState.value
            val shouldFollowIndicator = down.position.x in
                (startIndicatorPosition * itemWidthPx)..((startIndicatorPosition + 1f) * itemWidthPx)

            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            var dragStart = down
            var hasHorizontalDrag = false
            var hasVerticalDrag = false
            var lastVerticalPositionY = down.position.y
            while (!hasHorizontalDrag) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val horizontalTravel = change.position.x - down.position.x
                val verticalTravel = change.position.y - down.position.y
                val verticalStep = change.position.y - lastVerticalPositionY
                val absHorizontalTravel = abs(horizontalTravel)
                val absVerticalTravel = abs(verticalTravel)
                val hasVerticalIntent = hasVerticalDrag ||
                    (
                        absVerticalTravel > touchSlop &&
                            absVerticalTravel > absHorizontalTravel * BottomBarVerticalIntentRatio
                        )
                if (hasVerticalIntent) {
                    hasVerticalDrag = true
                    onDragStateChangeState.value(false)
                    if (verticalStep != 0f) {
                        onVerticalDragState.value(-verticalStep)
                    }
                    lastVerticalPositionY = change.position.y
                    continue
                }

                val hasHorizontalIntent = absHorizontalTravel > touchSlop &&
                    absHorizontalTravel > absVerticalTravel * BottomBarHorizontalIntentRatio
                if (!hasHorizontalIntent) continue

                change.consume()
                dragStart = change
                latestPositionX = change.position.x
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                onDragStateChangeState.value(true)

                val initialPosition = if (shouldFollowIndicator) {
                    startIndicatorPosition + horizontalTravel / itemWidthPx
                } else {
                    change.position.x / itemWidthPx
                }
                onDragPositionChangeState.value(initialPosition.coerceIn(0f, (itemCount - 1).toFloat()))
                hasHorizontalDrag = true
            }

            if (!hasHorizontalDrag) {
                onDragStateChangeState.value(false)
                continue
            }

            var wasCancelled = false
            try {
                horizontalDrag(dragStart.id) { change ->
                    change.consume()
                    latestPositionX = change.position.x
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    val dragPosition = if (shouldFollowIndicator) {
                        startIndicatorPosition + (change.position.x - down.position.x) / itemWidthPx
                    } else {
                        change.position.x / itemWidthPx
                    }
                    onDragPositionChangeState.value(dragPosition.coerceIn(0f, (itemCount - 1).toFloat()))
                    latestVelocityX = velocityTracker.calculateVelocity().x
                }
            } catch (_: Exception) {
                wasCancelled = true
            }

            if (!wasCancelled) {
                onVelocityChangeState.value(latestVelocityX)
                val velocityNudge = (latestVelocityX / BottomBarVelocityNormalization)
                    .coerceIn(-0.36f, 0.36f)
                val releaseIndex = ((latestPositionX / itemWidthPx) + velocityNudge)
                    .roundToInt()
                    .coerceIn(0, itemCount - 1)
                onSelectedState.value(releaseIndex)
            }
            onDragStateChangeState.value(false)
        }
    }
}
}
