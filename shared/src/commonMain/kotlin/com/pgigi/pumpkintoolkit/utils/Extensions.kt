package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.ScheduleCourseEntry
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// 预计算每周课程，避免每次pager切换重新计算
fun buildWeekCourses(courses: List<Course>): List<List<Course>> {
    val courseListByWeek = mutableListOf(mutableListOf<Course>())
    courses.forEach { course ->
        course.weeks.forEach { week ->
            if (week[1] > courseListByWeek.size) {
                for (i in courseListByWeek.size..< week[1]) {
                    courseListByWeek.add(mutableListOf())
                }
            }
            for (i in week[0]-1..< week[1]) {
                courseListByWeek[i].add(course)
            }
        }
    }
    return courseListByWeek
}

//预处理融合课表
//通过此方法处理的总课表会包含教务课程和自定义课程；教务课程id为empty，自定义课程的id会得以保留
fun buildWeekScheduleEntries(entries: List<ScheduleCourseEntry>): List<List<ScheduleCourseEntry>> {
    val entriesListByWeek = mutableListOf(mutableListOf<ScheduleCourseEntry>())
    entries.forEach { entry ->
        entry.course.weeks.forEach { week ->
            if (week[1] > entriesListByWeek.size) {
                for (i in entriesListByWeek.size..< week[1]) {
                    entriesListByWeek.add(mutableListOf())
                }
            }
            for (i in week[0]-1..< week[1]) {
                entriesListByWeek[i].add(entry) //id与课程强绑定。通过week加进来后，id也会自己进来
            }
        }
    }
    return entriesListByWeek
}

fun <T> List<T>.contentEqualsIgnoreOrder(other: List<T>): Boolean {
    // 第一步：长度不同直接返回 false
    if (this.size != other.size) return false

    // 第二步：创建可变副本，避免修改原列表
    val otherCopy = other.toMutableList()

    // 第三步：遍历当前列表，逐个匹配并移除已匹配元素（避免重复匹配）
    for (element in this) {
        val index = otherCopy.indexOf(element)
        if (index == -1) {
            // 找不到匹配元素，直接返回 false
            return false
        }
        // 移除已匹配的元素，防止重复匹配（比如列表中有重复元素的场景）
        otherCopy.removeAt(index)
    }

    // 所有元素都匹配完成，返回 true
    return true
}

fun Long .toLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}