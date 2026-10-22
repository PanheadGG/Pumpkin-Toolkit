package com.pgigi.pumpkintoolkit.components.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.components.ConflictSheet
import com.pgigi.pumpkintoolkit.components.CourseDetailSheet
import com.pgigi.pumpkintoolkit.components.CourseRow
import com.pgigi.pumpkintoolkit.components.mergeCoursesForDisplay
import com.pgigi.pumpkintoolkit.components.mergeScheduleEntriesForDisplay
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.ScheduleCourseEntry
import com.pgigi.pumpkintoolkit.models.ScheduleTime
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.time.Clock

@Composable
fun TimeColumn(timeList: List<ScheduleTime>, cellHeight: Dp = 70.dp) {
    Column(
        Modifier
            .width(30.dp)
            .height(cellHeight * timeList.size + 96.dp)
    ) {
        repeat(timeList.size) { i ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = (i + 1).toString(),
                    fontSize = 12.sp
                )
                if (i < timeList.size) {
                    Text(
                        text = "${timeList[i].start}\n${timeList[i].end}",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleTable(
    courses: List<Course>,
    entries: List<ScheduleCourseEntry> = emptyList(),
    timeList: List<ScheduleTime>,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 70.dp,
    firstDay: LocalDate? = null,
    onAddCell: (Int, Int) -> Unit = { _, _ -> } //回调函数，通过层层上报拿到完整的卡片信息
) {
    var dayList by remember { mutableStateOf(listOf("日", "一", "二", "三", "四", "五", "六")) }

    val dayCourses by produceState(initialValue = Array(7) { emptyList() }, courses) {
        value = Array(7) { day ->
            mergeCoursesForDisplay(courses.filter { it.dayOfWeek == day })
        }
    }

    val dayEntries = remember(entries) {    //将entries处理成周课表形式，arr[7]，一个下标一天，持有一个列表
        Array(7) { day ->
            mergeScheduleEntriesForDisplay(
                entries.filter { it.course.dayOfWeek == day }
            )
        }
    }

    val dates = remember(firstDay) {
        firstDay?.let { start ->
            List(7) { offset -> start.plus(offset, DateTimeUnit.DAY) }
        }
    }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Column(
                Modifier
                    .width(30.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                firstDay?.let {
                    Text(
                        text = firstDay.month.number.toString(),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "月",
                        fontSize = 12.sp
                    )
                }
            }

            dayList.forEachIndexed { index, day ->
                val date = dates?.get(index)
                val isToday = remember(date, today) {
                    if (date != null) date == today else false
                }
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day,
                        color = if (isToday) MiuixTheme.colorScheme.primary else Color.Unspecified,
                        fontSize = 12.sp
                    )
                    if (date != null) {
                        Text(
                            text = date.day.toString(),
                            color = if (isToday) MiuixTheme.colorScheme.primary else Color.Unspecified,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TimeColumn(timeList, cellHeight)

//            dayCourses.forEach { dayCourseList ->
            dayCourses.forEachIndexed { dayIndex, dayCourseList ->
                CourseRow(
                    dayCourses = dayCourseList,
                    cellHeight = cellHeight,
                    modifier = Modifier.weight(1f),
                    onEmptyCellClick = {lesson ->   //点击滚木卡片的时候，把星期和节次一起传给上层
                        onAddCell(dayIndex, lesson)
                    },
                    dayEntries = dayEntries[dayIndex]
                )
            }
        }
    }
}

@Composable
fun SchedulePager(
    modifier: Modifier = Modifier,
    courseListByWeek: List<List<Course>>,
    entriesListByWeek: List<List<ScheduleCourseEntry>> = emptyList(),   //打了个包的数据结构
    timeList: List<ScheduleTime>,
    cellHeight: Dp = 70.dp,
    pagerState: PagerState,
    startDate: LocalDate? = null,
    onAddCell: (Int, Int, Int) -> Unit = {_, _, _ -> },  //(星期， 节次， 周次）
    onEditCustomCourse:(String) -> Unit = {},   //保存id，通过回调实现id对应课程的修改
    onDeleteCustomCourse: (String) -> Boolean = { false }   //String为id，bool为是否删除成功
) {
    var courseIdToConfirmDeletion by remember { mutableStateOf<String?>(null) } //记录等待确认删除的课程id， null代表没有删除请求，故null时不显示确认删除弹窗，有就显示
    var deleteFail by remember { mutableStateOf(false) }    //标记是否删除成功

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
    ) { page ->
        if (startDate != null) {
            val weekCalculator = WeekCalculator(startDate)
            ScheduleTable(
                courses = courseListByWeek.getOrElse(page) { listOf() },
                entries = entriesListByWeek.getOrElse(page) { emptyList() },
                timeList = timeList,
                cellHeight = cellHeight,
                firstDay = weekCalculator.getWeekFirstDay(page + 1),
                onAddCell = { day, lesson ->
                    onAddCell(day, lesson, page + 1)
                }
            )
        } else {
            ScheduleTable(
                courses = courseListByWeek.getOrElse(page) { listOf() },
                entries = entriesListByWeek.getOrElse(page) { emptyList() },
                timeList = timeList,
                cellHeight = cellHeight,
                onAddCell = { day, lesson ->
                    onAddCell(day, lesson, page + 1)
                }
            )
        }
    }

    WindowBottomSheet(
        show = CourseDetailSheet.show.value,
        title = "课程详细",
        onDismissRequest = { CourseDetailSheet.show.value = false }
    ) {
        BasicComponent(
            title = CourseDetailSheet.name.value,
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Info,
                    contentDescription = "课程: " + CourseDetailSheet.name.value
                )
            }
        )
        if (CourseDetailSheet.classroom.value.isNotEmpty()) {
            BasicComponent(
                title = CourseDetailSheet.classroom.value,
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Location,
                        contentDescription = "教室: " + CourseDetailSheet.classroom.value
                    )
                }
            )
        }
        if (CourseDetailSheet.teacher.value.isNotEmpty()) {
            BasicComponent(
                title = CourseDetailSheet.teacher.value,
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Contacts,
                        contentDescription = "教师: " + CourseDetailSheet.teacher.value
                    )
                }
            )
        }
        BasicComponent(
            title = CourseDetailSheet.weeks.value,
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Months,
                    contentDescription = "周次: " + CourseDetailSheet.weeks.value
                )
            }
        )


        CourseDetailSheet.selectedCustomCourseIds.value.singleOrNull()?.let { id ->
            BasicComponent (    //修改课程按钮绘制
                title = "修改课程",
                onClick = {

                    onEditCustomCourse(id)
                }
            )

            BasicComponent (    //删除课程按钮的绘制
                title = "删除课程",
                onClick = {
                    deleteFail = false  //重置flag
                    courseIdToConfirmDeletion = id
                }
            )
        }

        BasicComponent()
    }

    //确认删除弹窗，courseIdToConfirm置null时关闭，有元素就开启
    WindowDialog(
        show = courseIdToConfirmDeletion != null,
        title = "确认删除",
        summary = if (deleteFail) "删除失败！请重试！" else "确定删除这门课程吗？\n这门课将会永久丢失！\t（真的很久！）",
        onDismissRequest = { courseIdToConfirmDeletion = null }
    ) {
        Row {
            TextButton(
                text = "取消",
                onClick = { courseIdToConfirmDeletion = null },
                modifier = Modifier.weight(1f)
            )

            TextButton(
                text = "确认删除",
                onClick = {
                    courseIdToConfirmDeletion?.let { id ->
                        if (onDeleteCustomCourse(id)) { //如果成功删除
                            deleteFail = false  //reset
                            courseIdToConfirmDeletion = null
                            CourseDetailSheet.show.value = false
                        } else {
                            deleteFail = true
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    WindowBottomSheet(
        show = ConflictSheet.show.value,
        title = "课程详细 (${ConflictSheet.courses.size}门)",
        onDismissRequest = { ConflictSheet.show.value = false }
    ) {
        ConflictSheet.courses.forEachIndexed { index, course ->
            BasicComponent(
                title = course.name,
                summary = "@${course.classroom} ${course.teacher}",
                onClick = {
                    ConflictSheet.show.value = false
                    CourseDetailSheet.show(
                        course,
                        ConflictSheet.entries.getOrNull(index)?.customCourseIds ?: emptyList()
                    )
                }
            )
        }
        BasicComponent()
    }
}
