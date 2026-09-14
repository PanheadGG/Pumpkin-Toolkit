package com.pgigi.pumpkintoolkit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pgigi.pumpkintoolkit.animation.PredictiveBackAnimation
import com.pgigi.pumpkintoolkit.animation.PredictiveBackExitDirection
import com.pgigi.pumpkintoolkit.constants.TimeList.summerAutumnTime
import com.pgigi.pumpkintoolkit.constants.TimeList.winterSpringTime
import com.pgigi.pumpkintoolkit.utils.KVaultUtils
import com.pgigi.pumpkintoolkit.utils.reloadWidgetTimelines
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object AppConfig {
    val kvault = KVaultUtils
    val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // UI
    var colorSchemeMode by mutableStateOf(ColorSchemeMode.System)
    var uiMode by mutableIntStateOf(0) // 0=Miuix, 2=Material
    var floatingNavigation by mutableStateOf(false)
    var functionDisplayMode by mutableIntStateOf(0) // 0=列表, 1=平铺
    var enableBlurEffect by mutableStateOf(true)
    var predictiveBackAnimation by mutableStateOf(PredictiveBackAnimation.None)
    var predictiveBackExitDirection by mutableStateOf(PredictiveBackExitDirection.FOLLOW_GESTURE)

    // 课表
    var cellHeight by mutableIntStateOf(70)
    var courseNameLine by mutableIntStateOf(4)
    var courseRoomLine by mutableIntStateOf(2)
    var courseTeacherLine by mutableIntStateOf(2)
    var startDate by mutableStateOf<LocalDate?>(null)
    var timeSeason by mutableIntStateOf(0) // 0自动, 1夏秋, 2冬春
    var defaultTermId by mutableStateOf("")
    var totalWeekNum by mutableStateOf(0)

    var timeList by mutableStateOf(when(timeSeason){
        1 -> summerAutumnTime
        2 -> winterSpringTime
        else -> if(localDate.month.number in 5..<10) summerAutumnTime else winterSpringTime
    })

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var totalWeek by mutableIntStateOf(0)
    var serverUrl by mutableStateOf("")

    var termValueMap by mutableStateOf(mapOf<String, String>())
    var termNameList = mutableListOf<String>()
    var termValueList = mutableListOf<String>()
    var rawTermValueMap by mutableStateOf(mapOf<String, String>())
    var termFilterStartId by mutableStateOf("")

    var hideFailScore by mutableStateOf(false)

    var lockStartDate by mutableStateOf(false)

    object KEY {
        const val COLOR_MODE = "color_mode"
        const val UI_MODE = "ui_mode"
        const val FLOATING_NAVIGATION = "floating_navigation"
        const val FUNCTION_DISPLAY_MODE = "function_display_mode"
        const val ENABLE_BLUR_EFFECT = "enable_blur_effect"
        const val PREDICTIVE_BACK_ANIMATION = "predictive_back_animation"
        const val PREDICTIVE_BACK_EXIT_DIRECTION = "predictive_back_exit_direction"
        const val CELL_HEIGHT = "cell_height"
        const val COURSE_NAME_LINE = "course_name_line"
        const val COURSE_ROOM_LINE = "course_room_line"
        const val COURSE_TEACHER_LINE = "course_teacher_line"
        const val TIME_SEASON = "time_season"
        const val START_DATE = "start_date"
        const val DEFAULT_TERM = "default_term_id"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val SERVER_URL = "server_url"
        const val TOTAL_WEEK = "total_week"
        const val TERM_FILTER_START = "term_filter_start"
        const val HIDE_FAIL_SCORE = "hide_fail_score"
        const val LOCK_START_DATE = "lock_start_date"
    }


    fun load(){

        colorSchemeMode = when(kvault.getString(KEY.COLOR_MODE)){
            ColorSchemeMode.Light.name -> ColorSchemeMode.Light
            ColorSchemeMode.Dark.name -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
        uiMode = kvault.getInt(KEY.UI_MODE) ?: 0
        floatingNavigation = kvault.getBoolean(KEY.FLOATING_NAVIGATION)?:false
        functionDisplayMode = kvault.getInt(KEY.FUNCTION_DISPLAY_MODE)?:0
        enableBlurEffect = kvault.getBoolean(KEY.ENABLE_BLUR_EFFECT)?:true
        predictiveBackAnimation = PredictiveBackAnimation.fromName(kvault.getString(KEY.PREDICTIVE_BACK_ANIMATION))
        predictiveBackExitDirection = PredictiveBackExitDirection.fromName(kvault.getString(KEY.PREDICTIVE_BACK_EXIT_DIRECTION))
        cellHeight = kvault.getInt(KEY.CELL_HEIGHT)?:70
        courseNameLine = kvault.getInt(KEY.COURSE_NAME_LINE)?:4
        courseRoomLine = kvault.getInt(KEY.COURSE_ROOM_LINE)?:2
        courseTeacherLine = kvault.getInt(KEY.COURSE_TEACHER_LINE)?:2

        timeSeason = kvault.getInt(KEY.TIME_SEASON)?:1

        timeList = when(timeSeason){
            1 -> summerAutumnTime
            2 -> winterSpringTime
            else -> if(localDate.month.number in 5..<10) summerAutumnTime else winterSpringTime
        }

        startDate = kvault.getString(KEY.START_DATE)?.let { LocalDate.parse(it) }
        defaultTermId = kvault.getString(KEY.DEFAULT_TERM)?:""
        username = kvault.getString(KEY.USERNAME)?:""
        password = kvault.getString(KEY.PASSWORD)?:""
        serverUrl = kvault.getString(KEY.SERVER_URL)?:"http://61.187.179.66:8924/"
        totalWeek = kvault.getInt(KEY.TOTAL_WEEK)?:0
        termFilterStartId = kvault.getString(KEY.TERM_FILTER_START)?:""
        hideFailScore = kvault.getBoolean(KEY.HIDE_FAIL_SCORE)?:false
        lockStartDate = kvault.getBoolean(KEY.LOCK_START_DATE)?:false
    }

    fun save(){
        kvault.putString(KEY.COLOR_MODE,colorSchemeMode.name)
        kvault.putInt(KEY.UI_MODE, uiMode)
        kvault.putBoolean(KEY.FLOATING_NAVIGATION, floatingNavigation)
        kvault.putInt(KEY.FUNCTION_DISPLAY_MODE, functionDisplayMode)
        kvault.putBoolean(KEY.ENABLE_BLUR_EFFECT, enableBlurEffect)
        kvault.putString(KEY.PREDICTIVE_BACK_ANIMATION, predictiveBackAnimation.name)
        kvault.putString(KEY.PREDICTIVE_BACK_EXIT_DIRECTION, predictiveBackExitDirection.name)
        kvault.putInt(KEY.CELL_HEIGHT,cellHeight)
        kvault.putInt(KEY.COURSE_NAME_LINE,courseNameLine)
        kvault.putInt(KEY.COURSE_ROOM_LINE,courseRoomLine)
        kvault.putInt(KEY.COURSE_TEACHER_LINE,courseTeacherLine)
        kvault.putString(KEY.USERNAME,username)
        kvault.putString(KEY.PASSWORD,password)
        kvault.putString(KEY.DEFAULT_TERM,defaultTermId)
        startDate?.toString()?.let { kvault.putString(KEY.START_DATE, it) }
        kvault.putInt(KEY.TIME_SEASON,timeSeason)
        kvault.putString(KEY.SERVER_URL,serverUrl)
        kvault.putInt(KEY.TOTAL_WEEK,totalWeek)
        kvault.putString(KEY.TERM_FILTER_START, termFilterStartId)
        kvault.putBoolean(KEY.HIDE_FAIL_SCORE, hideFailScore)
        kvault.putBoolean(KEY.LOCK_START_DATE, lockStartDate)
        reloadWidgetTimelines()
    }

    fun updateTermData(map: Map<String, String>) {
        rawTermValueMap = map
        applyTermFilter()
    }

    fun applyTermFilter() {
        val raw = rawTermValueMap
        val filtered = if (termFilterStartId.isNotEmpty()) {
            raw.filterKeys { it >= termFilterStartId }
        } else {
            raw
        }
        termValueMap = filtered
        termValueList.clear()
        termValueList.addAll(filtered.keys)
        termNameList.clear()
        termNameList.addAll(filtered.values)
    }

}