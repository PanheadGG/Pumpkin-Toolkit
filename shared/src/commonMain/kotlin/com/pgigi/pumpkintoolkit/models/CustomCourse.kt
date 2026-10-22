//自定义课程的数据结构类，只提供数据结构，不提供方法

package com.pgigi.pumpkintoolkit.models

import kotlinx.serialization.Serializable

//用于存储自定义课程的数据结构，内部包含：Course、id（用于精确查找自定义课程）、color（颜色设置，可为空）
@Serializable
data class CustomCourse (
    val id: String,
    val course: Course,
    val color: Int? = null
)

//用于存储自定义课表缓存，内部包含：自定义课程的列表、updateTime（不知道有什么用，我看ScheduleCache这么写的就直接抄了）
@Serializable
data class CustomScheduleCache(
    val updateTime: Long,
    val courses: List<CustomCourse>
)