package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalAppViewModel
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
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
        val indexInDay: Int,
        val course: Course,
        val startTime: String,
        val endTime: String,
        val classroom: String
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
        .mapIndexed { index, course ->
            val si = (course.lessonOfDay - 1).coerceIn(0, AppConfig.timeList.size - 1)
            val ei = (si + course.duration - 1).coerceIn(0, AppConfig.timeList.size - 1)
            TimelineItem.CourseCard(
                date = date,
                indexInDay = index,
                course = course,
                startTime = AppConfig.timeList[si].start,
                endTime = AppConfig.timeList[ei].end,
                classroom = course.classroom
                    .replace("【红湘校区】", "")
                    .replace("【雨母校区】", "")
            )
        }

    if (dayCourses.isEmpty()) return emptyList()

    val result = mutableListOf<TimelineItem>()
    result.add(TimelineItem.DayHeader(date, date == today))
    result.addAll(dayCourses)
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
//    val courses = remember(viewModel.courseList.size) { buildWeekCourses(viewModel.courseList) }   //原先remember只依据教务课程数量更新，修改自定义课程后可能拿到过时的课表
    val courses = buildWeekCourses(viewModel.mergedCourses())   //传入融合的课表，页面重新计算时也能拿到修改后的课程
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

//    LaunchedEffect(viewModel.courseList.size) {
//        if (weekCalculator != null && viewModel.courseList.isNotEmpty()) {
//            val end = today.plus(120, DateTimeUnit.DAY)
//            timelineData = generateRange(courses, weekCalculator, today, end, today)
//            dataStart = today
//        }
//    }
    LaunchedEffect(courses, weekCalculator) {   //当courses或weekCalculator变化时重新生成日程；旧代码只监听教务课程数量，不会随自定义课程修改而更新
        if (weekCalculator != null && courses.any { it.isNotEmpty() }) {
            val end = today.plus(120, DateTimeUnit.DAY)
            timelineData = generateRange(courses, weekCalculator, today, end, today)
            dataStart = today
        } else {
            timelineData = emptyList()   //没有课程或开学日期时清空旧日程，避免删完后还显示之前的内容
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
        if (!loggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请登录使用",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (weekCalculator == null || timelineData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "近期暂无课程安排",
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
                    modifier = Modifier.fillMaxSize(),
                    // 与 PumpkinCampus 日程页一致：左右 16dp、条目间距 8dp
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 4.dp,
                        end = 16.dp,
                        bottom = 64.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = timelineData.size,
                        key = { index ->
                            when (val item = timelineData[index]) {
                                is TimelineItem.DayHeader -> "day_${item.date}"
                                is TimelineItem.CourseCard -> "c_${item.date}_${item.indexInDay}"
                            }
                        }
                    ) { index ->
                        when (val item = timelineData[index]) {
                            is TimelineItem.DayHeader -> M3TimelineDayHeader(item.date, item.isToday)
                            is TimelineItem.CourseCard -> M3TimelineCourseCard(item)
                        }
                    }
                }
            }
        }
    }
}

/** 卡片左右分隔的红色竖线颜色（与 PumpkinCampus 日程页一致）。 */
private val AgendaAccentLine = Color(0xFFE53935)

/** 日期标题：`9月26日 周五`，今天高亮并带「今天」徽标。 */
@Composable
private fun M3TimelineDayHeader(date: LocalDate, isToday: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${date.month.number}月${date.day}日",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isToday) accent else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = getDayOfWeekText(date.dayOfWeek),
            fontSize = 12.sp,
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
                Text("今天", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * 课程卡片（样式与 PumpkinCampus 日程页一致）：
 * ```
 * ┌───────────────────────────────────────────┐
 * │ 08:00   │ 高等数学              A101   │
 * │ 09:40   │ 张伟（小字）                       │
 * └───────────────────────────────────────────┘
 *  ↑左列          ↑5dp + 红竖线 + 5dp
 * ```
 */
@Composable
private fun M3TimelineCourseCard(item: TimelineItem.CourseCard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            // 自定义背景下该 token 会自动换成半透明纱，背景透出来
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左：上面开始时间，下面结束时间
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = item.startTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.endTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 中：左右各 5dp 间距，中间红色竖线
        Box(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(AgendaAccentLine)
        )

        // 右：上面课程名，下面小字老师名
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.course.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.course.teacher,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 最右：教室
        Text(
            text = item.classroom,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 8.dp)
                .widthIn(max = 110.dp)
        )
    }
}
