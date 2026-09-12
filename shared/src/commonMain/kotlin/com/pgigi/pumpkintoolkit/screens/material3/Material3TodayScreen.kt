package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalAppViewModel
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.material3.M3Card
import com.pgigi.pumpkintoolkit.utils.buildWeekCourses
import com.pgigi.pumpkintoolkit.utils.resolveTodayView
import com.pgigi.pumpkintoolkit.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = LocalAppViewModel.current
) {
    val loggedIn = AppConfig.username.isNotEmpty() && AppConfig.password.isNotEmpty()
//    val courses = buildWeekCourses(viewModel.courseList)
    val listState = rememberLazyListState()

    val navigator = LocalNavigator.current
    val hapticFeedback = LocalHapticFeedback.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp = windowInfo.containerDpSize.width

//    val weekCourses = courses.getOrElse(viewModel.currentWeek - 1) { emptyList() }
//    val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//    val todayCourses = weekCourses.filter { course ->
//        course.dayOfWeek == (localDate.dayOfWeek.ordinal + 1) % 7
//    }

    // 当前「今日课程」页的显示状态：由 resolveTodayView(now, 课表, 设置...) 算出，                                                                                                                        ▼ Modified Files
    // 进入页面先算一次；之后每次用户下拉刷新时，由 refreshToday() 重新计算并更新。
    var viewState by remember {
        mutableStateOf(
            resolveTodayView(
                now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                courseList = viewModel.courseList,
                startDate = AppConfig.startDate,
                currentWeek = viewModel.currentWeek,
                totalWeek = AppConfig.totalWeek,
                enabled = AppConfig.tomorrowScheduleEnable,
                switchHour = AppConfig.tomorrowSwitchHour,
                switchMinute = AppConfig.tomorrowSwitchMinute
            )
        )
    }

    val refreshToday = {
        viewState = resolveTodayView(
            now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            courseList = viewModel.courseList,
            startDate = AppConfig.startDate,
            currentWeek = viewModel.currentWeek,
            totalWeek = AppConfig.totalWeek,
            enabled = AppConfig.tomorrowScheduleEnable,
            switchHour = AppConfig.tomorrowSwitchHour,
            switchMinute = AppConfig.tomorrowSwitchMinute
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(viewState.title) },
                actions = {
                    AnimatedVisibility(screenWidthDp <= 800.dp) {
                        IconButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navigator.push(Route.Settings)
                        }) {
                            Icon(MiuixIcons.Settings, contentDescription = "设置")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    refreshToday()      //下拉后刷新
                    delay(200)
                    isRefreshing = false
                }
            },
            pullToRefreshState = pullToRefreshState,
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(state = listState, modifier = Modifier.padding(paddingValues)) {
                items(viewState.courses.size) { index ->
                    val course = viewState.courses[index]
                    val classroom = course.classroom.replace("【红湘校区】", "")
                        .replace("【雨母校区】", "")

                    val startIndex =
                        (course.lessonOfDay - 1).coerceIn(0, AppConfig.timeList.size - 1)
                    val endIndex =
                        (startIndex + course.duration - 1).coerceIn(0, AppConfig.timeList.size - 1)

                    M3Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${AppConfig.timeList[startIndex].start}-" +
                                            "${AppConfig.timeList[endIndex].end} " +
                                            course.teacher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = classroom,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (!loggedIn || viewState.courses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (loggedIn) "暂无课程" else "请登录使用",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .fillMaxSize(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
