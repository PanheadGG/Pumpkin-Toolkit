package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.components.material3.M3FloatingDropdown
import com.pgigi.pumpkintoolkit.components.material3.SchedulePager
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.utils.QZClient
import com.pgigi.pumpkintoolkit.utils.buildWeekCourses
import com.pgigi.pumpkintoolkit.viewmodel.OtherScheduleViewModel
import kotlinx.datetime.LocalDate
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3OtherScheduleScreen(
    viewModel: OtherScheduleViewModel = viewModel(factory = OtherScheduleViewModel.Factory)
) {
    val navigator = LocalNavigator.current
    var title by remember { mutableStateOf("课程表") }
    var loading by remember { mutableStateOf(true) }
    var initialPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    val list = remember { mutableStateListOf<Course>() }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    var scheduleComment by remember { mutableStateOf("") }
    var showComment by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.currentTermIndex) {
        loading = true
        if (viewModel.currentTermIndex < 0 || viewModel.currentTermIndex >= AppConfig.termValueList.size) {
            return@LaunchedEffect
        }
        val termId = AppConfig.termValueList[viewModel.currentTermIndex]
        if (viewModel.courseListMap[termId].isNullOrEmpty()) {
            val tmpList = QZClient.getAllCourses(termId)
            tmpList?.let {
                viewModel.courseListMap[termId] = tmpList
            }
        }
        viewModel.courseListMap[termId]?.let {
            list.clear()
            list.addAll(viewModel.courseListMap[termId]!!)
        }
        if (viewModel.startDateMap[termId] == null) {
            val tmpStartDate = QZClient.getStartDate(termId)
            tmpStartDate?.let {
                viewModel.startDateMap[termId] = tmpStartDate
            }
        }
        scheduleComment = QZClient.getScheduleComment(termId)
        startDate = viewModel.startDateMap[termId]
        loading = false
    }

    val courseListByWeek by mutableStateOf(buildWeekCourses(list))
    pageCount = courseListByWeek.size
    initialPage = 0

    val pagerState = rememberPagerState(
        pageCount = { pageCount },
        initialPage = initialPage
    )

    LaunchedEffect(pagerState.pageCount) {
        pagerState.scrollToPage(0)
    }

    LaunchedEffect(pagerState.currentPage) {
        title = "第${pagerState.currentPage + 1}周"
    }

    val shortLabels = remember(AppConfig.termNameList) {
        AppConfig.termNameList.map { it }
    }

    val currentLabel = shortLabels.getOrElse(viewModel.currentTermIndex) { "选择学期" }
    val windowInfo = LocalWindowInfo.current

    Scaffold(
        modifier = Modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (windowInfo.containerDpSize.width < 800.dp) {
                NavigationBar {
                    NavigationBarItem(
                        selected = !showComment,
                        onClick = { showComment = false },
                        icon = { Icon(MiuixIcons.ListView, contentDescription = "课表") },
                        label = { Text("课表") }
                    )
                    NavigationBarItem(
                        selected = showComment,
                        onClick = { showComment = true },
                        icon = { Icon(MiuixIcons.GridView, contentDescription = "备注") },
                        label = { Text("备注") }
                    )
                }
            }
        }
    ) { outerPaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(outerPaddingValues)) {
            if (windowInfo.containerDpSize.width >= 800.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // SchedulePager with its own Scaffold
                    Scaffold(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            TopAppBar(
                                title = { Text(title) },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navigator.pop()
                                    }) {
                                        Icon(MiuixIcons.Back, contentDescription = "返回")
                                    }
                                }
                            )
                        },
                    ) { innerPaddingValues ->
                        SchedulePager(
                            modifier = Modifier.fillMaxSize().padding(innerPaddingValues),
                            cellHeight = AppConfig.cellHeight.dp,
                            courseListByWeek = courseListByWeek,
                            pagerState = pagerState,
                            timeList = AppConfig.timeList,
                            startDate = startDate
                        )
                    }

                    // Text with its own Scaffold
                    Scaffold(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            TopAppBar(
                                title = { Text("备注") },
                            )
                        },
                    ) { innerPaddingValues ->
                        Text(
                            modifier = Modifier.padding(innerPaddingValues).padding(horizontal = 16.dp).fillMaxSize(),
                            text = scheduleComment
                        )
                    }
                }
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (!showComment) {
                    // SchedulePager view
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text(title) },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navigator.pop()
                                    }) {
                                        Icon(MiuixIcons.Back, contentDescription = "返回")
                                    }
                                }
                            )
                        }
                    ) { innerPaddingValues ->
                        SchedulePager(
                            modifier = Modifier.padding(innerPaddingValues),
                            cellHeight = AppConfig.cellHeight.dp,
                            courseListByWeek = courseListByWeek,
                            pagerState = pagerState,
                            timeList = AppConfig.timeList,
                            startDate = startDate
                        )
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text("备注") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navigator.pop()
                                    }) {
                                        Icon(MiuixIcons.Back, contentDescription = "返回")
                                    }
                                }
                            )
                        }
                    ) { innerPaddingValues ->
                        Text(
                            modifier = Modifier.padding(innerPaddingValues).padding(horizontal = 16.dp),
                            text = scheduleComment
                        )
                    }
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (shortLabels.isNotEmpty()) {
                M3FloatingDropdown(
                    label = currentLabel,
                    items = shortLabels,
                    selectedIndex = viewModel.currentTermIndex,
                    onSelect = { index ->
                        if (index != viewModel.currentTermIndex) {
                            viewModel.currentTermIndex = index
                            title = "第${pagerState.currentPage + 1}周"
                        }
                    },
                )
            }
        }
    }
}
