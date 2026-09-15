package com.pgigi.pumpkintoolkit.screens.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.components.miuix.MiuixFloatingDropdown
import com.pgigi.pumpkintoolkit.components.miuix.SchedulePager
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.utils.JsonUtil
import com.pgigi.pumpkintoolkit.utils.QZClient
import com.pgigi.pumpkintoolkit.utils.buildWeekCourses
import com.pgigi.pumpkintoolkit.viewmodel.OtherScheduleViewModel
import kotlinx.datetime.LocalDate
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixOtherScheduleScreen(viewModel: OtherScheduleViewModel = viewModel(factory = OtherScheduleViewModel.Factory)) {
    val navigator = LocalNavigator.current
    var title by remember { mutableStateOf("课程表") }
    rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var initialPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    val list = remember { mutableStateListOf<Course>() }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var scheduleComment by remember { mutableStateOf("") }
    val windowInfo = LocalWindowInfo.current
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
                println(JsonUtil.toListJson(tmpList, Course.serializer()))
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
    val containerColor = MiuixTheme.colorScheme.surfaceContainer

    Scaffold(
        modifier = Modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (windowInfo.containerDpSize.width < 800.dp) {
                NavigationBar {
                    // Add navigation items for single column view
                    NavigationBarItem(
                        selected = !showComment,
                        onClick = {
                            showComment = false
                        },
                        icon = MiuixIcons.ListView,
                        label = "课表"
                    )
                    NavigationBarItem(
                        selected = showComment,
                        onClick = {
                            showComment = true
                        },
                        icon = MiuixIcons.GridView,
                        label = "备注"
                    )
                }
            }
        }
    ) { outerPaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(outerPaddingValues)){
            if (windowInfo.containerDpSize.width >= 800.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // SchedulePager with its own Scaffold
                    Scaffold(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            SmallTopAppBar(
                                title = title,
                                navigationIcon = {
                                    IconButton(onClick = { navigator.pop() }) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = "返回"
                                        )
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
                            SmallTopAppBar(
                                title = "备注",
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
                    InfiniteProgressIndicator(
                        Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.windowDimming)
                    )
                }
            } else {
                if (!showComment) {
                    // SchedulePager view
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            SmallTopAppBar(
                                title = title,
                                navigationIcon = {
                                    IconButton(onClick = { navigator.pop() }) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = "返回"
                                        )
                                    }
                                }
                            )
                        }
                    ) {innerPaddingValues->
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
                            SmallTopAppBar(
                                title = "备注",
                                navigationIcon = {
                                    IconButton(onClick = { navigator.pop() }) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = "返回"
                                        )
                                    }
                                }
                            )
                        }
                    ) {innerPaddingValues->
                        Text(
                            modifier = Modifier.padding(innerPaddingValues).padding(horizontal = 16.dp),
                            text = scheduleComment
                        )
                    }
                }


                if (loading) {
                    InfiniteProgressIndicator(
                        Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.windowDimming)
                    )
                }
            }

            if (shortLabels.isNotEmpty()) {
                MiuixFloatingDropdown(
                    label = currentLabel,
                    items = shortLabels,
                    selectedIndex = viewModel.currentTermIndex,
                    onSelect = { index ->
                        if (index != viewModel.currentTermIndex) {
                            viewModel.currentTermIndex = index
                            title = "第${pagerState.currentPage + 1}周"
                        }
                    },
                    containerColor = containerColor,
                )
            }
        }
    }
}
