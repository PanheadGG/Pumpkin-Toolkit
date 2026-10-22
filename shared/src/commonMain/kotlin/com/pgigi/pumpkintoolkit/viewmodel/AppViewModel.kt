package com.pgigi.pumpkintoolkit.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import top.yukonga.miuix.kmp.nav.core.NavKey
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.CustomCourse
import com.pgigi.pumpkintoolkit.models.ScheduleCourseEntry
import com.pgigi.pumpkintoolkit.utils.CustomCourseStore
import kotlin.time.Clock

class AppViewModel : ViewModel(){
    val backStack = mutableStateListOf<NavKey>(Route.Home)
    val courseList = mutableStateListOf<Course>()
    val customCourseList = mutableStateListOf<CustomCourse>()
    var currentWeek by mutableIntStateOf(1)

    //自定义课表只需要再首次被创建时进行一次加载，不会随着教务一起刷新。故，放在此类下，保证第一次被加载时就初始化customCourseList，而不是和正常的courseList一起刷新、加载
    init {
        customCourseList.addAll(CustomCourseStore.loadCustomCourseCache())
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel()
            }
        }
    }

    /*以下为customCourseList的相关方法。由于需要修改后实时传给界面，故将方法放在此类下。如果以后逻辑增多，可以单独成类*/

    //此函数用于合并教务课程与自定义课程
    //从customCourseList中抽取出所有的course，与原courseList合并成新列表后返回
    fun mergedCourses(): List<Course> { //此函数调用后会丢失CustomCourse的id信息！
        return courseList + customCourseList.map { it.course }
    }

    //给所有的教务课程和自定义课程套层壳，丢给课表展示页
    fun scheduleCourseEntries(): List<ScheduleCourseEntry> {
        val remote = courseList.map { ScheduleCourseEntry(course = it) }
        val custom = customCourseList.map {
            ScheduleCourseEntry(
                course = it.course,
                customCourseIds = listOf(it.id)
            )
        }
        return remote + custom
    }

    //传入一个课程，创建标识符后打包成CustomCourse，后写入json并加入customCourseList列表；返回bool标志是否成功
    fun addCustomCourse(course: Course): Boolean {

        val customCourse = CustomCourse(id = generateCustomCourseId(), course = course) //打包成customCourse对象

        if(!CustomCourseStore.add(customCourse)) return false   //尝试将自定义课程写入json。失败则返回false

        customCourseList.add(customCourse)                      //将自定义课程加入列表，并返回true
        return true
    }

    //传入一个自定义课程的id，并在json与列表中移除。同理，返回bool
    fun removeCustomCourse(id: String): Boolean {
        if(!CustomCourseStore.remove(id)) return false          //尝试删除json中的自定义课程。失败直接返回false

        customCourseList.removeAll { it.id == id }              //将列表中的对应id的自定义课程删了，并返回true
        return true
    }

    //传入一个CustomCourse，更新id相同的课程。同理，返回bool
    fun updateCustomCourse(updated: CustomCourse): Boolean {
        val index = customCourseList.indexOfFirst { it.id == updated.id }   //获取下标，即被更新的自定义课程

        if(index == -1)                        return false                 //如果下标为-1，则表示没有，返回false
        if(!CustomCourseStore.update(updated)) return false                 //尝试更新自定义课程，失败返回false

        customCourseList[index] = updated                                   //更新原课程为新课程，并返回true
        return true
    }

    //生成自定义课程的id作为标识符。使用当前系统时间作为id
    //应该不会有人在一毫秒内自定义两个课程吧？不会吧不会吧
    private fun generateCustomCourseId(): String {
        return Clock.System.now().toString()
    }
}