//在课程内容之外保留对应的自定义课程 ID，供详情中的编辑和删除使用
//模型类，提供给课表绘制页的数据结构
//原有展示链只传递 Course，会丢失 CustomCourse 的 id；编辑和删除时需要保留这个 id 来定位原始记录

package com.pgigi.pumpkintoolkit.models

data class ScheduleCourseEntry (
    val course: Course,
    val customCourseIds: List<String> = emptyList()
)