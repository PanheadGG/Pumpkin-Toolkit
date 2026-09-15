package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.components.FloatingBottomBar
import com.pgigi.pumpkintoolkit.components.FloatingBottomBarColors
import com.pgigi.pumpkintoolkit.components.FloatingBottomBarMode
import com.pgigi.pumpkintoolkit.components.isLiquidGlassSupported
import com.pgigi.pumpkintoolkit.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit

object M3Navigation {
    val items = listOf("日程", "课表", "功能")
    val icons = listOf(MiuixIcons.ListView, MiuixIcons.VerticalSplit, MiuixIcons.GridView)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var pagerCount by remember { mutableIntStateOf(M3Navigation.items.size) }
    val pagerState = rememberPagerState(pageCount = { pagerCount }, initialPage = viewModel.currentPagerIndex)
    val liquidGlassSupported = isLiquidGlassSupported()
    val useBlur = AppConfig.floatingNavigation && AppConfig.enableBlurEffect && liquidGlassSupported
    val backdrop = if (useBlur) rememberLayerBackdrop() else null

    val colors = FloatingBottomBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        activeContentColor = MaterialTheme.colorScheme.primary,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val isLandscape = maxWidth > maxHeight

        when {
            // >= 1400dp: three columns equally
            widthDp >= 1400.dp -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Material3TodayScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Material3ScheduleScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Material3FunctionScreen()
                    }
                }
            }

            // 800dp..<1400dp: left TodayScreen, right ScheduleScreen, FAB for FunctionScreen
            widthDp >= 800.dp -> {
                var showFunctionPanel by remember { mutableStateOf(false) }
                // padding values matching the FAB's right and bottom padding
                val fabPadding = 16.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Material3TodayScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Material3ScheduleScreen()
                    }
                }

                // Open FAB at bottom-right (hidden when panel is open)
                AnimatedVisibility(
                    visible = !showFunctionPanel,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = fabPadding, bottom = fabPadding)
                ) {
                    FloatingActionButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showFunctionPanel = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.GridView,
                            contentDescription = "展开功能"
                        )
                    }
                }

                // Expanded FunctionScreen overlay
                AnimatedVisibility(
                    visible = showFunctionPanel,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showFunctionPanel = false }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = fabPadding, bottom = fabPadding)
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* consume click to prevent closing */ }
                        ) {
                            Material3FunctionScreen()
                        }

                        // Close FAB at bottom-right of the overlay
                        FloatingActionButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFunctionPanel = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = fabPadding, bottom = fabPadding),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Reset,
                                contentDescription = "收起功能"
                            )
                        }
                    }
                }
            }

            // < 600dp: keep existing layout
            else -> {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!AppConfig.floatingNavigation && !isLandscape) {
                            NavigationBar {
                                M3Navigation.items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            coroutineScope.launch {
                                                viewModel.currentPagerIndex = index
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = { Icon(M3Navigation.icons[index], contentDescription = null) },
                                        label = { Text(item) }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (!AppConfig.floatingNavigation && isLandscape) {
                            NavigationRail {
                                M3Navigation.items.forEachIndexed { index, item ->
                                    NavigationRailItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            coroutineScope.launch {
                                                viewModel.currentPagerIndex = index
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = { Icon(M3Navigation.icons[index], contentDescription = null) },
                                        label = { Text(item) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                                beyondViewportPageCount = 1
                            ) {
                                when (it) {
                                    0 -> Material3TodayScreen()
                                    1 -> Material3ScheduleScreen()
                                    2 -> Material3FunctionScreen()
                                }
                            }

                            if (AppConfig.floatingNavigation) {
                                FloatingBottomBar(
                                    items = M3Navigation.items,
                                    selectedIndex = { pagerState.currentPage },
                                    onSelected = { index ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        coroutineScope.launch {
                                            viewModel.currentPagerIndex = index
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    backdrop = backdrop,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    mode = if (useBlur) FloatingBottomBarMode.LiquidGlass else FloatingBottomBarMode.None,
                                    colors = colors,
                                    iconContent = { _, index ->
                                        Icon(M3Navigation.icons[index], contentDescription = null)
                                    },
                                    labelContent = { item, _ ->
                                        Text(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
