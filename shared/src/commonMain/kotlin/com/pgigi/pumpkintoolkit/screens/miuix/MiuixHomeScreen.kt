package com.pgigi.pumpkintoolkit.screens.miuix

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.components.FloatingBottomBar
import com.pgigi.pumpkintoolkit.components.FloatingBottomBarMode
import com.pgigi.pumpkintoolkit.components.isLiquidGlassSupported
import com.pgigi.pumpkintoolkit.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme


object Navigation {
    val items = listOf("日程", "课表", "功能")
    val icons = listOf(MiuixIcons.ListView, MiuixIcons.VerticalSplit, MiuixIcons.GridView)
}

@Composable
fun MiuixHomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var pagerCount by remember { mutableIntStateOf(Navigation.items.size) }
    val pagerState = rememberPagerState(pageCount = { pagerCount }, initialPage = viewModel.currentPagerIndex)
    val liquidGlassSupported = isLiquidGlassSupported()
    val useBlur = AppConfig.floatingNavigation && AppConfig.enableBlurEffect && liquidGlassSupported
    val backdrop = if (useBlur) rememberLayerBackdrop() else null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val isLandscape = maxWidth > maxHeight

        when {
            // >= 1200dp: three columns equally
            widthDp >= 1400.dp -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TodayScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ScheduleScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        FunctionScreen()
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
                        TodayScreen()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ScheduleScreen()
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
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFunctionPanel = true
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.GridView,
                            contentDescription = "展开功能",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
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
                                .background(MiuixTheme.colorScheme.surface)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* consume click to prevent closing */ }
                        ) {
                            FunctionScreen()
                        }

                        // Close FAB at bottom-right of the overlay
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFunctionPanel = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = fabPadding, bottom = fabPadding)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.surfaceVariant),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Reset,
                                contentDescription = "收起功能",
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
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
                                Navigation.items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            coroutineScope.launch {
                                                viewModel.currentPagerIndex = index
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = Navigation.icons[index],
                                        label = item
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
                                Navigation.items.forEachIndexed { index, item ->
                                    NavigationRailItem(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            coroutineScope.launch {
                                                viewModel.currentPagerIndex = index
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = Navigation.icons[index],
                                        label = item
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
                                    0 -> TodayScreen()
                                    1 -> ScheduleScreen()
                                    2 -> FunctionScreen()
                                }
                            }

                            if (AppConfig.floatingNavigation) {
                                FloatingBottomBar(
                                    items = Navigation.items,
                                    selectedIndex = { pagerState.currentPage },
                                    onSelected = { index ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    iconContent = { _, index ->
                                        Icon(Navigation.icons[index], contentDescription = null)
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
