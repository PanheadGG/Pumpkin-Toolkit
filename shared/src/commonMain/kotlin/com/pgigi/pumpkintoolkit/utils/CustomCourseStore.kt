package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.constants.FileName
import com.pgigi.pumpkintoolkit.models.CustomCourse
import com.pgigi.pumpkintoolkit.models.CustomScheduleCache
import kotlin.time.Clock

object CustomCourseStore {

    //加载函数，目的是从持久化的json中解析出课表数据
    //返回一个课程列表
    fun loadCustomCourseCache(): List<CustomCourse> {

        //使用工具函数读取custom_schedule.json下的数据。没有/解析失败？返回空列表。 *记得判空
        val json = FileStoreUtils.readString(FileName.CUSTOM_SCHEDULE)
            ?: return emptyList()

        //尝试进行解析。将拆解出的courses列表进行返回
        try {

            val cache = JsonUtil.parseJson(json, CustomScheduleCache.serializer())
            return cache.courses

        } catch (_: Exception) {    //抛异常了？直接返回空列表
            return emptyList()
        }

    }

    //保存函数，负责保存cache
    //返回bool，用于判断是否保存成功
    fun saveCustomCourseCache(list: List<CustomCourse>): Boolean {

        //尝试存储,成功返回true，抛错误了统一返回false
        try{

            val cache = CustomScheduleCache(
                updateTime = Clock.System.now().toEpochMilliseconds(),
                courses = list
            )

            return FileStoreUtils.writeString(
                FileName.CUSTOM_SCHEDULE,
                JsonUtil.toJson(cache, CustomScheduleCache.serializer())
            )
        } catch (_: Exception) {
            return false
        }
    }

    //这个函数的目的是在已有的列表最后添加一个自定义课程
    //返回bool，用于判断是否添加成功
    fun add(customCourse: CustomCourse): Boolean {

        try {

            val temp = loadCustomCourseCache().toMutableList()  //先使用load函数加载已有列表
            temp += customCourse                                //在末尾添加
            return saveCustomCourseCache(temp)            //返回是否添加成功（save函数会返回bool，标志是否返回成功）

        } catch (_: Exception) {                                //抛异常统一返回false
            return false
        }
    }

    //这个函数的目的是移除掉对应id的自定义课程
    fun remove(id: String): Boolean {

        try {

            val origin = loadCustomCourseCache()        //获取原有列表
            val temp = origin.filterNot { it.id == id } //获得不包含所传id的列表

            if(temp == origin){ //新列表与旧列表完全一样？说明没找到，故返回false
                return false
            }

            return saveCustomCourseCache(temp)    //同前一个函数

        } catch (_: Exception) {
            return false                                //同上
        }
    }

    //这个函数的目的是更新指定id的课程（id信息包含于CustomCourse）
    //传入一个Course，自动将原列表的Course.id的id下的课程更新
    //返回值代表是否成功执行操作
    fun update(updated: CustomCourse): Boolean {

        try{

            val temp = loadCustomCourseCache().toMutableList()          //拿到可修改的原列表
            val index = temp.indexOfFirst { it.id == updated.id }
            if(index == -1) {       //未找到？返回修改失败
                return false
            } else {
                temp[index] = updated   //将对应下标的课程进行修改
                return saveCustomCourseCache(temp)
            }

        } catch (_: Exception) {
            return false
        }
    }

    //清空现有的列表
    //返回值代表是否成功
    fun clear(): Boolean {
        return saveCustomCourseCache(emptyList())
    }
}