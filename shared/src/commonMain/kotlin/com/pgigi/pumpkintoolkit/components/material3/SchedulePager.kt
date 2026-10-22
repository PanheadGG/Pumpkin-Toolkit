package com.pgigi.pumpkintoolkit.components.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Months
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
                CompositionLocalProvider(
                    androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.merge(
                        TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
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
                            fontSize = 8.sp,
                            lineHeight = 10.sp
                        )
                    }
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
    onAddCell: (Int, Int) -> Unit = { _, _ -> }   //回调函数，通过层层上报拿到完整的空格信息
) {
    var dayList by remember { mutableStateOf(listOf("日", "一", "二", "三", "四", "五", "六")) }

    val dayCourses by produceState(initialValue = Array(7) { emptyList() }, courses) {
        value = Array(7) { day ->
            mergeCoursesForDisplay(courses.filter { it.dayOfWeek == day })
        }
    }

    val dayEntries = remember(entries) {   //将当前周的entries按星期分成arr[7]，一个下标一天，持有一个列表
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
                CompositionLocalProvider(
                    androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.merge(
                        TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
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
                    CompositionLocalProvider(
                        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.merge(
                            TextStyle(
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )
                    ) {
                        Text(
                            text = day,
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontSize = 12.sp
                        )
                        if (date != null) {
                            Text(
                                text = date.day.toString(),
                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                fontSize = 12.sp
                            )
                        }
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
                    onEmptyCellClick = { lesson ->   //点击空白卡片的时候，把星期和节次一起传给上层
                        onAddCell(dayIndex, lesson)
                    },
                    dayEntries = dayEntries[dayIndex]
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePager(
    modifier: Modifier = Modifier,
    courseListByWeek: List<List<Course>>,
    entriesListByWeek: List<List<ScheduleCourseEntry>> = emptyList(),   //打了包的数据结构，课程和自定义id一起传下来
    timeList: List<ScheduleTime>,
    cellHeight: Dp = 70.dp,
    pagerState: PagerState,
    startDate: LocalDate? = null,
    onAddCell: (Int, Int, Int) -> Unit = { _, _, _ -> },   //(星期，节次，周次)
    onEditCustomCourse: (String) -> Unit = {},   //通过回调把id传给上层，实现对应课程的修改
    onDeleteCustomCourse: (String) -> Boolean = { false }   //String为id，bool为是否删除成功
) {
    var courseIdToConfirmDeletion by remember { mutableStateOf<String?>(null) }   //记录等待确认删除的课程id，null代表没有删除请求；有id才显示确认弹窗
    var deleteFail by remember { mutableStateOf(false) }   //标记是否删除失败，true时在原确认弹窗中显示错误

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
                    onAddCell(day, lesson, page + 1)   //page从0开始，周次从1开始；在这里补上周次再传给上层
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

    if (CourseDetailSheet.show.value) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { CourseDetailSheet.show.value = false },
            sheetState = sheetState
        ) {
            Text(
                text = "课程详细",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
            DetailRow(MiuixIcons.Info, "课程: " + CourseDetailSheet.name.value, CourseDetailSheet.name.value)
            if (CourseDetailSheet.classroom.value.isNotEmpty()) {
                DetailRow(MiuixIcons.Location, "教室: " + CourseDetailSheet.classroom.value, CourseDetailSheet.classroom.value)
            }
            if (CourseDetailSheet.teacher.value.isNotEmpty()) {
                DetailRow(MiuixIcons.Contacts, "教师: " + CourseDetailSheet.teacher.value, CourseDetailSheet.teacher.value)
            }
            DetailRow(MiuixIcons.Months, "周次: " + CourseDetailSheet.weeks.value, CourseDetailSheet.weeks.value)

            //恰好有一个自定义id才提供操作；教务课程和包含多个自定义id的合并卡片只查看详情
            CourseDetailSheet.selectedCustomCourseIds.value.singleOrNull()?.let { id ->
                Row(   //修改课程按钮绘制
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditCustomCourse(id) }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text("修改课程", style = MaterialTheme.typography.bodyLarge)
                }
                Row(   //删除课程按钮的绘制
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            deleteFail = false   //重置flag，不显示上一次的失败提示
                            courseIdToConfirmDeletion = id
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text("删除课程", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    //确认删除弹窗，courseIdToConfirmDeletion置null时关闭，有id就开启
    if (courseIdToConfirmDeletion != null) {
        AlertDialog(
            onDismissRequest = { courseIdToConfirmDeletion = null },
            title = { Text("确认删除") },
            text = {
                Text(
                    if (deleteFail) "删除失败！请重试！"
                    else "确定删除这门课程吗？\n这门课将会永久丢失！\t（真的很久！）"
                )
            },
            dismissButton = {
                TextButton(onClick = { courseIdToConfirmDeletion = null }) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        courseIdToConfirmDeletion?.let { id ->
                            if (onDeleteCustomCourse(id)) {   //如果成功删除
                                deleteFail = false   //reset
                                courseIdToConfirmDeletion = null
                                CourseDetailSheet.show.value = false
                            } else {   //失败则保留确认弹窗，修改提示文字，允许重试
                                deleteFail = true
                            }
                        }
                    }
                ) {
                    Text("确认删除")
                }
            }
        )
    }

    if (ConflictSheet.show.value) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { ConflictSheet.show.value = false },
            sheetState = sheetState
        ) {
            Text(
                text = "课程详细 (${ConflictSheet.courses.size}门)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
//            ConflictSheet.courses.forEach { course ->
            //用index拿到对应的entry，把原始id继续传给详情，避免从冲突列表选中后丢失id
            ConflictSheet.courses.forEachIndexed { index, course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ConflictSheet.show.value = false
                            CourseDetailSheet.show(
                                course,
                                ConflictSheet.entries.getOrNull(index)?.customCourseIds ?: emptyList()
                            )
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${course.classroom} ${course.teacher}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, contentDescription: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
