package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalAppViewModel
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.material3.M3Row
import com.pgigi.pumpkintoolkit.constants.Texts
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import com.pgigi.pumpkintoolkit.utils.buildWeekCourses
import com.pgigi.pumpkintoolkit.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import kotlin.time.Clock

private sealed class TimelineItem {
    data class DayHeader(val date: LocalDate, val isToday: Boolean) : TimelineItem()
    data class CourseCard(
        val date: LocalDate,
        val course: Course,
        val startTime: String,
        val endTime: String,
        val classroom: String,
        val isLastInDay: Boolean
    ) : TimelineItem()
}

private fun buildDayItems(
    allWeekCourses: List<List<Course>>,
    weekCalculator: WeekCalculator,
    date: LocalDate,
    today: LocalDate
): List<TimelineItem> {
    val weekNumber = weekCalculator.getWeekNumber(date).toInt()
    val weekIndex = weekNumber - 1
    if (weekIndex !in allWeekCourses.indices) return emptyList()

    val dayOfWeek = (date.dayOfWeek.ordinal + 1) % 7
    val dayCourses = allWeekCourses[weekIndex].filter { it.dayOfWeek == dayOfWeek }
        .sortedBy { it.lessonOfDay }
        .map { course ->
            val si = (course.lessonOfDay - 1).coerceIn(0, AppConfig.timeList.size - 1)
            val ei = (si + course.duration - 1).coerceIn(0, AppConfig.timeList.size - 1)
            TimelineItem.CourseCard(
                date = date,
                course = course,
                startTime = AppConfig.timeList[si].start,
                endTime = AppConfig.timeList[ei].end,
                classroom = course.classroom
                    .replace("【红湘校区】", "")
                    .replace("【雨母校区】", ""),
                isLastInDay = false
            )
        }

    if (dayCourses.isEmpty()) return emptyList()

    val result = mutableListOf<TimelineItem>()
    result.add(TimelineItem.DayHeader(date, date == today))
    dayCourses.forEachIndexed { index, card ->
        result.add(card.copy(isLastInDay = index == dayCourses.size - 1))
    }
    return result
}

private fun generateRange(
    allWeekCourses: List<List<Course>>,
    weekCalculator: WeekCalculator,
    from: LocalDate,
    to: LocalDate,
    today: LocalDate
): List<TimelineItem> {
    val result = mutableListOf<TimelineItem>()
    var date = from
    while (date <= to) {
        result.addAll(buildDayItems(allWeekCourses, weekCalculator, date, today))
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return result
}

private fun getDayOfWeekText(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = LocalAppViewModel.current
) {
    val loggedIn = AppConfig.username.isNotEmpty() && AppConfig.password.isNotEmpty()
    val courses = remember(viewModel.courseList.size) { buildWeekCourses(viewModel.courseList) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val hapticFeedback = LocalHapticFeedback.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp = windowInfo.containerDpSize.width

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val weekCalculator = remember(AppConfig.startDate) {
        AppConfig.startDate?.let { WeekCalculator(it, 1) }
    }

    var timelineData by remember { mutableStateOf(emptyList<TimelineItem>()) }
    var dataStart by remember { mutableStateOf(today) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    fun todayIndex(): Int {
        return timelineData.indexOfFirst { it is TimelineItem.DayHeader && it.isToday }
            .coerceAtLeast(0)
    }

    LaunchedEffect(viewModel.courseList.size) {
        if (weekCalculator != null && viewModel.courseList.isNotEmpty()) {
            val end = today.plus(120, DateTimeUnit.DAY)
            timelineData = generateRange(courses, weekCalculator, today, end, today)
            dataStart = today
        }
    }

    LaunchedEffect(timelineData.size) {
        if (timelineData.isNotEmpty()) {
            listState.scrollToItem(todayIndex())
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("日程") },
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
        if (!loggedIn || weekCalculator == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (loggedIn) "暂无课程" else "请登录使用",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            PullToRefresh(
                modifier = Modifier.padding(paddingValues),
                refreshTexts = Texts.REFRESH_TEXTS,
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        val newStart = dataStart.minus(30, DateTimeUnit.DAY)
                        val past = generateRange(courses, weekCalculator, newStart, dataStart.minus(1, DateTimeUnit.DAY), today)
                        if (past.isNotEmpty()) {
                            val prevIndex = listState.firstVisibleItemIndex
                            val prevOffset = listState.firstVisibleItemScrollOffset
                            timelineData = past + timelineData
                            dataStart = newStart
                            listState.scrollToItem(prevIndex + past.size, prevOffset)
                        }
                        isRefreshing = false
                    }
                },
                pullToRefreshState = pullToRefreshState,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = timelineData.size,
                        key = { index ->
                            when (val item = timelineData[index]) {
                                is TimelineItem.DayHeader -> "day_${item.date}"
                                is TimelineItem.CourseCard -> "c_${item.date}_${item.course.lessonOfDay}_${item.course.name}"
                            }
                        }
                    ) { index ->
                        when (val item = timelineData[index]) {
                            is TimelineItem.DayHeader -> M3TimelineDayHeader(item.date, item.isToday)
                            is TimelineItem.CourseCard -> M3TimelineCourseCard(item)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }
}

@Composable
private fun M3TimelineDayHeader(date: LocalDate, isToday: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${date.month.number}月${date.day}日",
            fontWeight = FontWeight.Bold,
            color = if (isToday) accent else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = getDayOfWeekText(date.dayOfWeek),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("今天", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun M3TimelineCourseCard(item: TimelineItem.CourseCard) {
    val accent = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.End) {
            Text(item.startTime, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.endTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!item.isLastInDay) {
                Canvas(Modifier.width(1.dp).height(24.dp).align(Alignment.TopCenter)) {
                    drawRect(lineColor, Offset.Zero, Size(size.width, size.height))
                }
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            if (!item.isLastInDay) {
                Canvas(Modifier.width(1.dp).height(56.dp).align(Alignment.BottomCenter)) {
                    drawRect(lineColor, Offset.Zero, Size(size.width, size.height))
                }
            }
        }

        Card(
            modifier = Modifier.weight(1f).padding(end = 16.dp, bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            /*Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(item.course.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.course.teacher, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.classroom, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }*/
            M3Row(
                modifier = Modifier.fillMaxWidth(),
                title = item.course.name,
                summary = item.course.teacher,
                trailingContent = {
                    Text(item.classroom)
                }
            )
        }
    }
}
