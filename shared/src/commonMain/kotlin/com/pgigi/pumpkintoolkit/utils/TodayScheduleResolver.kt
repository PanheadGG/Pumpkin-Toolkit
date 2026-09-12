package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.Course
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

//数据类，用于今日课程界面的数据结构，返回title、单日的课程List，是否为假期的bool值
data class TodayViewState(
    val title: String,          //“今日课程” or “明日课程”
    val courses: List<Course>,  //课程列表
    val isHoliday: Boolean      //是否为假期
)

//这个函数被用于决定返回什么课表（今/明）
fun resolveTodayView(
    now: LocalDateTime,
    courseList: List<Course>,
    startDate: LocalDate?,
    currentWeek: Int,
    totalWeek: Int,
    enabled: Boolean,
    switchHour: Int,
    switchMinute: Int
): TodayViewState {
   /* 现在过了所设置的明日课程显示时间刻了吗？（统一转化为分钟计算*/
    val nowMinute = now.hour*60 + now.minute
    val switchPassed = enabled && nowMinute >= switchHour*60 + switchMinute //明日课程为true，今日为false

    /*是否显示明天的课表？过了取明天，否则今天*/
    val targetDate = if(switchPassed) { now.date.plus(1, DateTimeUnit.DAY) } else now.date

    /*目标日期是第几周？（边界情况的考虑，可能跨周）*/
    val targetWeek = startDate?.let{
        WeekCalculator(it,1).getWeekNumber(targetDate).toInt()
    } ?: currentWeek

    /*是否是假期？*/
    val isHoliday = targetWeek <= 0 || (totalWeek>0 && targetWeek > totalWeek)

    /*是否是明日课程？*/
    val title = if (switchPassed) "明日课程" else "今日课程"

    if(isHoliday) return TodayViewState(title,emptyList(),true)

    /*取出那一周的课，并抽取出来周几对应的课*/
    val weekCourses = buildWeekCourses(courseList).getOrElse(targetWeek - 1) { emptyList() }
    val courses = weekCourses.filter {
        it.dayOfWeek == (targetDate.dayOfWeek.ordinal + 1) % 7
    }
    return TodayViewState(title,courses,false)
}
