package com.pgigi.pumpkintoolkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.ScheduleCourseEntry

val ScheduleCellColors = arrayOf(
    Color(0xffff1644),
    Color(0xff2b78ff),
    Color(0xffa375ff),
    Color(0xffff9000),
    Color(0xff1ee8b5),
    Color(0xffff3d01),
    Color(0xff2097f4),
    Color(0xff005daf),
    Color(0xfffa6278)
)

val colorMap = mutableMapOf<String, Int>()
val colorIndex = mutableIntStateOf(0)

private val courseMergeRanges = listOf(1..4, 5..8)  //存储可合并的范围，以避免硬编码

object CourseDetailSheet {
    var show = mutableStateOf(false)
    val name = mutableStateOf("")
    val classroom = mutableStateOf("")
    val teacher = mutableStateOf("")
    val weeks = mutableStateOf("")
    val selectedCourse = mutableStateOf<Course?>(null)  //用于记住所点击的课程
    val selectedCustomCourseIds = mutableStateOf<List<String>>(emptyList())  //用于记住所点击的课程的id信息

    fun show(course: Course, customCourseIds: List<String> = emptyList()) {
        name.value = course.name
        classroom.value = course.classroom
        teacher.value = course.teacher
        weeks.value = "第"
        for (week in course.weeks) {
            if (week.size == 1) {
                weeks.value += week[0]
            } else if (week.size == 2) {
                if (week[0] == week[1]) {
                    weeks.value += week[0]
                } else {
                    weeks.value += week[0].toString() + "-" + week[1]
                }
            } else {
                continue
            }
            weeks.value += "、"
        }
        weeks.value = weeks.value.dropLast(1) + "周"
        selectedCourse.value = course
        selectedCustomCourseIds.value = customCourseIds
        show.value = true
    }
}

object ConflictSheet {
    var show = mutableStateOf(false)
    val courses = mutableStateListOf<Course>()
    val entries = mutableStateListOf<ScheduleCourseEntry>() //避免丢失id，可为空

    fun show(conflictCourses: List<Course>,
             conflictEntries: List<ScheduleCourseEntry> = emptyList()
    ) {
        courses.clear()
        courses.addAll(conflictCourses)

        entries.clear()
        entries.addAll(conflictEntries)

        show.value = true
    }
}

@Composable
fun CourseCell(course: Course,
               conflictCourses: List<Course> = emptyList(),
               conflictEntries: List<ScheduleCourseEntry> = emptyList(),
               modifier: Modifier = Modifier,
               customCourseIds: List<String> = emptyList()
) {
    val hasConflict = conflictCourses.size > 1

    val colorIndex = remember(course.name) {
        colorMap.getOrPut(course.name) { colorIndex.value++ }
    }

    val baseColor = ScheduleCellColors[colorIndex % ScheduleCellColors.size]

    val classroom = course.classroom    //修改为直接计算，避免了自定义课程修改了教室后显示过时的字段的问题
        .replace("【雨母校区】", "")
        .replace("【红湘校区】", "")

    val textColor = Color.White

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(baseColor.copy(alpha = 0.75f))
            .fillMaxWidth()
            .clickable(true) {
                if (hasConflict) {
                    ConflictSheet.show(conflictCourses, conflictEntries)
                } else {
                    CourseDetailSheet.show(course, customCourseIds)
                }
            }
    ) {
        Column {
            BasicText(
                text = course.name,
                modifier = Modifier.padding(3.dp),
                style = TextStyle(
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                ),
                maxLines = AppConfig.courseNameLine,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = "@$classroom",
                modifier = Modifier.padding(3.dp),
                style = TextStyle(
                    color = textColor,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                ),
                maxLines = AppConfig.courseRoomLine,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = course.teacher,
                modifier = Modifier.padding(3.dp),
                style = TextStyle(
                    color = textColor,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                ),
                maxLines = AppConfig.courseTeacherLine,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasConflict) {
            Canvas(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp)) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = Color(0xFFFF0000))
            }
        }
    }
}

@Composable
fun CourseRow(dayCourses: List<Course>,
              cellHeight: Dp = 70.dp,
              modifier: Modifier = Modifier,
              onEmptyCellClick: (Int) -> Unit = {},      //实现点击滚木卡片添加自定义课程，这是一个点击空白格时调用的回调函数；回调函数，通过层层上报拿到完整的卡片信息
              dayEntries: List<ScheduleCourseEntry> = emptyList()
    ) {
    val courseGroups = remember(dayCourses) {
        groupConflictingCourses(dayCourses)
    }
    val entryGroups = remember(dayEntries) {
        groupConflictingEntries(dayEntries)
    }
    val groupsToDraw = if (dayEntries.isNotEmpty()) {
        entryGroups
    } else {
        courseGroups.map { group ->
            group.map { course -> ScheduleCourseEntry(course) }
        }
    }
    val occupiedLessons = mutableSetOf<Int>()   //标记哪些节次编号已经有课了
    val lessonCount = AppConfig.timeList.size   //通过Config获取一天课数量，避免硬编码

//    dayCourses.forEach { course ->              //依次取出当天的每门课程
//        for (lesson in course.lessonOfDay until course.lessonOfDay + course.duration) { //[3,4] == [3,5)    //负责将一节课所占的小课填入occupiedLessons
//            occupiedLessons.add(lesson)
//        }
//    }

    groupsToDraw.flatten().forEach { entry ->
        val course = entry.course
        for (lesson in course.lessonOfDay until course.lessonOfDay + course.duration) {
            occupiedLessons.add(lesson)
        }
    }

    //负责实现滚木卡片可点击的功能
    Box(modifier = modifier.height(cellHeight * lessonCount)) {
        for (lesson in 1..lessonCount) {
            if(lesson !in occupiedLessons) {    //如果卡片所在的课为滚木课，就添加可点击的滚木卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cellHeight)
                        .offset(y = cellHeight * (lesson - 1))
                        .clickable { onEmptyCellClick(lesson) }
                )
            }
        }

//        courseGroups.forEach { group ->
//            val course = group.first()
//            CourseCell(
//                course = course,
//                conflictCourses = group,
//                modifier = Modifier
//                    .padding(1.dp)
//                    .padding(top = cellHeight * (course.lessonOfDay - 1) + 1.dp)
//                    .height(cellHeight * course.duration - 2.dp)
//            )
//        }

        groupsToDraw.forEach { group ->
            val entry = group.first()
            val course = entry.course

            CourseCell(
                course = course,
                conflictCourses = group.map { it.course },
                conflictEntries = group,
                customCourseIds = entry.customCourseIds,
                modifier = Modifier
                    .padding(1.dp)
                    .padding(top = cellHeight * (course.lessonOfDay - 1) + 1.dp)
                    .height(cellHeight * course.duration - 2.dp)
            )
        }
    }
}

//这个方法用于合并两张卡片。例如，1~4节 合称为一张卡片
//传入两个Entry，传出合并后的ScheduleCourseEntry，id列表： [A, B]
private fun mergeAdjacentEntries(
    current: ScheduleCourseEntry,
    next: ScheduleCourseEntry,
): ScheduleCourseEntry {
    return current.copy(
        course = current.course.copy(
            duration = current.course.duration + next.course.duration
        ),
        customCourseIds = (current.customCourseIds + next.customCourseIds).distinct()
    )
}

private fun Array<IntArray>.weeksEquals(other: Array<IntArray>): Boolean {
    if (size != other.size) return false
    return indices.all { idx -> this[idx].contentEquals(other[idx]) }
}

private fun canMergeCourse(a: Course, b: Course): Boolean {
    return a.name == b.name &&
            a.classroom == b.classroom &&
            a.teacher == b.teacher &&
            a.weeks.weeksEquals(b.weeks)
}

internal fun mergeCoursesForDisplay(courses: List<Course>): List<Course> {
    val sortedCourses = courses.sortedBy { it.lessonOfDay }
    if (sortedCourses.isEmpty()) return sortedCourses

    val result = mutableListOf<Course>()
    val ranges = listOf(1..4, 5..8)

    ranges.forEach { range ->
        val blockCourses = sortedCourses.filter { course ->
            val start = course.lessonOfDay
            val end = course.lessonOfDay + course.duration - 1
            start in range && end in range
        }
        if (blockCourses.isEmpty()) return@forEach

        var current = blockCourses.first().copy()
        for (next in blockCourses.drop(1)) {
            val currentEnd = current.lessonOfDay + current.duration - 1
            val isAdjacent = next.lessonOfDay == currentEnd + 1
            if (isAdjacent && canMergeCourse(current, next)) {
                current.duration += next.duration
            } else {
                result.add(current)
                current = next.copy()
            }
        }
        result.add(current)
    }

    result.addAll(sortedCourses.filter { course ->
        val start = course.lessonOfDay
        val end = course.lessonOfDay + course.duration - 1
        !(start in 1..4 && end in 1..4) && !(start in 5..8 && end in 5..8)
    })

    return result.sortedBy { it.lessonOfDay }
}

//这个函数的目的是检查是否有可以合并的卡片，并进行合并，返回合并后的列表
//判断是否可合并的代码不是我写的，我直接用的，合并条件看他源码去 awa
//如果说两张卡片课程一样，就进行卡片的合并
private fun mergeEntriesInBlock(
    block: List<ScheduleCourseEntry>
): List<ScheduleCourseEntry> {
    if (block.isEmpty()) return emptyList()

    val result = mutableListOf<ScheduleCourseEntry>()
    var current = block.first()

    for (next in block.drop(1)) {
        val currentEnd = current.course.lessonOfDay + current.course.duration - 1   //一节课上课节次为 [3, 4]， 即记录4
        val isAdjacent = next.course.lessonOfDay == currentEnd + 1      //上张卡片为 [1, 2]，下张卡片为 [3, 4]，2 + 1 == 3，即毗连

        if (isAdjacent && canMergeCourse(current.course, next.course)) {    //毗连且可合并？
            current = mergeAdjacentEntries(current, next)
        } else {
            result.add(current)
            current = next
        }
    }

    result.add(current)
    return result
}

//处理一天的课表，将毗连的卡片相互合并并传回
internal fun mergeScheduleEntriesForDisplay(
    entries: List<ScheduleCourseEntry>
): List<ScheduleCourseEntry> {
    val sorted = entries.sortedBy { it.course.lessonOfDay }
    val result = mutableListOf<ScheduleCourseEntry>()

    courseMergeRanges.forEach { range ->    //分别将上午、下午的课丢进mergeEntriesInBlock里面，他会自动处理毗连的卡片，返回一个list
        val block = sorted.filter { entry ->
            val start = entry.course.lessonOfDay
            val end = start + entry.course.duration - 1

            start in range && end in range
        }
        result.addAll(mergeEntriesInBlock(block))
    }

    result.addAll(sorted.filter { entry ->  //这里专门处理跨range课程，例如 [4, 5]、 [9, 10]。不跨区间的上面处理了，这里进行额外处理
        val start = entry.course.lessonOfDay
        val end = start + entry.course.duration - 1

        courseMergeRanges.none { range ->   //不跨区间的处理过了，故使用none排除；剩下的再add进去
            start in range && end in range
        }
    })

    return result.sortedBy { it.course.lessonOfDay }   //返回根据时间排序的List
}

private fun groupConflictingCourses(courses: List<Course>): List<List<Course>> {
    val sorted = courses.sortedBy { it.lessonOfDay }
    if (sorted.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<Course>>()
    var currentGroup = mutableListOf(sorted.first())
    var currentMaxEnd = sorted.first().let { it.lessonOfDay + it.duration - 1 }

    for (course in sorted.drop(1)) {
        if (course.lessonOfDay <= currentMaxEnd) {
            currentGroup.add(course)
            currentMaxEnd = maxOf(currentMaxEnd, course.lessonOfDay + course.duration - 1)
        } else {
            groups.add(currentGroup)
            currentGroup = mutableListOf(course)
            currentMaxEnd = course.lessonOfDay + course.duration - 1
        }
    }
    groups.add(currentGroup)

    return groups
}

//
private fun groupConflictingEntries(entries: List<ScheduleCourseEntry>): List<List<ScheduleCourseEntry>> {
    val sorted = entries.sortedBy { it.course.lessonOfDay }
    if (sorted.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<ScheduleCourseEntry>>()
    var currentGroup = mutableListOf(sorted.first())    //这是一个临时变量，用于存储上一张卡片，为列表，包含所有的冲突课程
    var currentMaxEnd = sorted.first().let { it.course.lessonOfDay + it.course.duration - 1 }

    for (entry in sorted.drop(1)) {
        if (entry.course.lessonOfDay <= currentMaxEnd) {    //如果一个entry的开始节次与此前所存储的临时变量的节次发生重叠即为冲突
            currentGroup.add(entry)
            currentMaxEnd = maxOf(currentMaxEnd, entry.course.lessonOfDay + entry.course.duration - 1)
        } else {
            groups.add(currentGroup)
            currentGroup = mutableListOf(entry)
            currentMaxEnd = entry.course.lessonOfDay + entry.course.duration - 1
        }
    }
    groups.add(currentGroup)

    return groups
}