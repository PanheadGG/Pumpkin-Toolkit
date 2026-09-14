package com.pgigi.pumpkintoolkit.screens.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pgigi.pumpkintoolkit.App
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.ColorSchemeMode
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.miuix.NumberDatePicker
import com.pgigi.pumpkintoolkit.components.rememberNumberDatePickerState
import com.pgigi.pumpkintoolkit.constants.TimeList
import com.pgigi.pumpkintoolkit.animation.PredictiveBackAnimation
import com.pgigi.pumpkintoolkit.animation.PredictiveBackExitDirection
import com.pgigi.pumpkintoolkit.getPlatform
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
fun MiuixSettingScreen() {
    val navigator = LocalNavigator.current
    val showDialog = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val uriHandler = LocalUriHandler.current

    //明日课程功能变量的申明
    val showTimeDialog = remember { mutableStateOf(false) }
    var tempHour by remember { mutableIntStateOf(AppConfig.tomorrowSwitchHour) }
    var tempMinute by remember { mutableIntStateOf(AppConfig.tomorrowSwitchMinute) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val cardPadding = PaddingValues(12.dp, 6.dp)
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SmallTitle("主题设置")
            Card(modifier = Modifier.padding(cardPadding)){
                val uiItems = listOf("Miuix", "Material 3")
                WindowDropdownPreference(
                    title = "UI 风格",
                    items = uiItems,
                    selectedIndex = if (AppConfig.uiMode == 2) 1 else 0,
                    onSelectedIndexChange = {
                        val newMode = if (it == 1) 2 else 0
                        if (newMode != AppConfig.uiMode) {
                            AppConfig.uiMode = newMode
                            AppConfig.save()
                        }
                    }
                )
                val colorItems = listOf("跟随系统", "关闭", "开启")
                var selectedColor by remember { mutableIntStateOf(0) }
                selectedColor = when (AppConfig.colorSchemeMode) {
                    ColorSchemeMode.Light -> 1
                    ColorSchemeMode.Dark -> 2
                    else -> 0
                }
                WindowDropdownPreference(
                    title = "深色模式",
                    items = colorItems,
                    selectedIndex = selectedColor,
                    onSelectedIndexChange = {
                        if (it != selectedColor) {
                            selectedColor = it
                            AppConfig.colorSchemeMode = when (selectedColor) {
                                1 -> ColorSchemeMode.Light
                                2 -> ColorSchemeMode.Dark
                                else -> ColorSchemeMode.System
                            }
                            AppConfig.save()
                        }
                    }
                )
                SwitchPreference(
                    title = "悬浮导航栏",
                    checked = AppConfig.floatingNavigation,
                    onCheckedChange = {
                        AppConfig.floatingNavigation = it
                        AppConfig.save()
                    }
                )
                val displayModeItems = listOf("列表", "平铺")
                WindowDropdownPreference(
                    title = "功能列表显示模式",
                    items = displayModeItems,
                    selectedIndex = AppConfig.functionDisplayMode,
                    onSelectedIndexChange = {
                        AppConfig.functionDisplayMode = it
                        AppConfig.save()
                    }
                )
                SwitchPreference(
                    title = "模糊效果",
                    summary = "Android 需 13+(SDK 33+) 版本才能使用",
                    checked = AppConfig.enableBlurEffect,
                    onCheckedChange = {
                        AppConfig.enableBlurEffect = it
                        AppConfig.save()
                    }
                )
                /*
                var cellHeight by remember{ mutableStateOf(AppConfig.cellHeight.toFloat()) }
                SliderPreference(
                    title = "单元格高度",
                    value = cellHeight,
                    onValueChange = {
                        cellHeight = it
                        AppConfig.cellHeight = it.roundToInt()
                        AppConfig.save()
                    },
                    valueRange = 50f..100f,
                    steps = 9,
                    endActions = {
                        Text(
                            text = "$cellHeight dp",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    keyPoints = listOf(50f,55f,60f,65f,70f,75f,80f,85f,90f,95f,100f),
                    showKeyPoints = true
                )
                */
                var fontScale by remember{ mutableStateOf(AppConfig.fontScale) }
                SliderPreference(
                    title = "字体大小",
                    value = fontScale,
                    onValueChange = {
                        fontScale = it
                        AppConfig.fontScale = it
                        AppConfig.save()
                    },
                    valueRange = 0.7f..1.3f,
                    steps = 11,
                    endActions = {
                        Text(
                            text = "${(fontScale * 100).roundToInt()}%",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    keyPoints = listOf(0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1.0f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f),
                    showKeyPoints = true
                )
                val predictiveBackAnimationItems = PredictiveBackAnimation.entries.map { it.displayName }
                WindowDropdownPreference(
                    title = "预测返回动画",
                    summary = "部分设备不支持",
                    items = predictiveBackAnimationItems,
                    selectedIndex = PredictiveBackAnimation.entries.indexOf(AppConfig.predictiveBackAnimation),
                    onSelectedIndexChange = {
                        AppConfig.predictiveBackAnimation = PredictiveBackAnimation.entries[it]
                        AppConfig.save()
                    }
                )
                AnimatedVisibility(AppConfig.predictiveBackAnimation == PredictiveBackAnimation.Scale){
                    val exitDirectionItems =
                        PredictiveBackExitDirection.entries.map { it.displayName }
                    WindowDropdownPreference(
                        title = "返回退出方向",
                        items = exitDirectionItems,
                        selectedIndex = PredictiveBackExitDirection.entries.indexOf(AppConfig.predictiveBackExitDirection),
                        onSelectedIndexChange = {
                            AppConfig.predictiveBackExitDirection =
                                PredictiveBackExitDirection.entries[it]
                            AppConfig.save()
                        }
                    )
                }
            }

            SmallTitle("课表设置")
            Card(modifier = Modifier.padding(cardPadding)) {
                /*val cellTypeItems = listOf("彩底白字", "彩底彩字")
                var selectedCellType by remember { mutableIntStateOf(0) }
                selectedCellType = AppConfig.cellType
                WindowDropdownPreference(
                    title = "课程表单元格样式",
                    items = cellTypeItems,
                    selectedIndex = selectedCellType,
                    onSelectedIndexChange = {
                        if(it != selectedCellType){
                            selectedCellType = it
                            AppConfig.cellType = selectedCellType
                            AppConfig.saveSchedule()
                        }
                    }
                )*/
                val timeTypeItems = listOf("自动切换", "夏秋时间", "秋冬时间")
                var selectedTimeType by remember { mutableIntStateOf(0) }
                selectedTimeType = AppConfig.timeSeason
                WindowDropdownPreference(
                    title = "课程时间",
                    items = timeTypeItems,
                    selectedIndex = selectedTimeType,
                    onSelectedIndexChange = {
                        if(it != selectedTimeType){
                            selectedTimeType = it
                            AppConfig.timeSeason = selectedTimeType
                            AppConfig.save()
                            val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                            AppConfig.timeList = when(AppConfig.timeSeason){
                                1 -> TimeList.summerAutumnTime
                                2 -> TimeList.winterSpringTime
                                else -> if(localDate.month.number in 5..<10) TimeList.summerAutumnTime else TimeList.winterSpringTime
                            }
                        }
                    }
                )
                ArrowPreference(title = "课表开始时间",
                    endActions = {
                        Text(
                            text = if(AppConfig.startDate==null) "" else AppConfig.startDate.toString(),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    onClick = {
                        if(AppConfig.startDate == null){
                            datePickerState.selectedDateMillis = Clock.System.now().toEpochMilliseconds()
                        }else{
                            datePickerState.selectedDateMillis = AppConfig.startDate!!.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                        }
                        showDialog.value = true
                    }
                )
                SwitchPreference(
                    title = "锁定课表开始时间",
                    summary = "关闭此按钮将自动同步课表开始时间",
                    checked = AppConfig.lockStartDate,
                    onCheckedChange = {
                        AppConfig.lockStartDate = it
                        AppConfig.save()
                    }
                )

                var cellHeight by remember{ mutableStateOf(AppConfig.cellHeight.toFloat()) }
                SliderPreference(
                    title = "单元格高度",
                    value = cellHeight,
                    onValueChange = {
                        cellHeight = it
                        AppConfig.cellHeight = it.roundToInt()
                        AppConfig.save()
                    },
                    valueRange = 50f..100f,
                    steps = 9,
                    endActions = {
                        Text(
                            text = "$cellHeight dp",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    keyPoints = listOf(50f,55f,60f,65f,70f,75f,80f,85f,90f,95f,100f),
                    showKeyPoints = true
                )
                val lineItems = listOf("1","2","3","4")
                WindowDropdownPreference(
                    title = "课程名称显示行数",
                    items = lineItems,
                    selectedIndex = AppConfig.courseNameLine - 1,
                    onSelectedIndexChange = {
                        AppConfig.courseNameLine = it + 1
                        AppConfig.save()
                    }
                )
                WindowDropdownPreference(
                    title = "课程教室显示行数",
                    items = lineItems,
                    selectedIndex = AppConfig.courseRoomLine - 1,
                    onSelectedIndexChange = {
                        AppConfig.courseRoomLine = it + 1
                        AppConfig.save()
                    }
                )
                WindowDropdownPreference(
                    title = "课程教师显示行数",
                    items = lineItems,
                    selectedIndex = AppConfig.courseTeacherLine - 1,
                    onSelectedIndexChange = {
                        AppConfig.courseTeacherLine = it + 1
                        AppConfig.save()
                    }
                )

                //设置中“时间刻后显示明日课程(可能需要手动刷新)”按钮，决定是否启用明日课程功能；同时，决定是否显示“切换时间”设置项
                SwitchPreference(
                    title = "时间刻后显示明日课程(可能需要手动刷新)",
                    checked = AppConfig.tomorrowScheduleEnable,
                    onCheckedChange = {
                        AppConfig.tomorrowScheduleEnable = it
                        AppConfig.save()
                    }
                )

                //设置中”切换时间“设置项
                AnimatedVisibility(AppConfig.tomorrowScheduleEnable) {
                    ArrowPreference(
                        title = "切换时间",
                        endActions = {
                            Text(
                                text =  "${AppConfig.tomorrowSwitchHour.toString().padStart(2, '0')}:" +
                                        "${AppConfig.tomorrowSwitchMinute.toString().padStart(2, '0')}",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = { showTimeDialog.value = AppConfig.tomorrowScheduleEnable }
                    )
                }

                //用于展示滚轮时间选择器的弹窗时间
                //点击”切换时间“按钮后，弹出滚轮事件选择弹窗，进行时间的选择
                WindowDialog(
                    title = "选择切换时间",
                    show = showTimeDialog.value,
                    onDismissRequest = { showTimeDialog.value = false }
                ) {
                    val dismiss = LocalDismissState.current
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberPicker(
                                value = tempHour,
                                onValueChange = { tempHour = it },
                                range = 0..23,
                                label = { it.toString().padStart(2, '0') },
                                wrapAround = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text(":")
                            NumberPicker(
                                value = tempMinute,
                                onValueChange = { tempMinute = it },
                                range = 0..59,
                                label = { it.toString().padStart(2, '0') },
                                wrapAround = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row {
                            TextButton(
                                text = "取消",
                                onClick = { dismiss?.invoke() },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                            )
                            TextButton(
                                text = "确认",
                                onClick = {
                                    AppConfig.tomorrowSwitchHour = tempHour
                                    AppConfig.tomorrowSwitchMinute = tempMinute
                                    AppConfig.save()
                                    dismiss?.invoke()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            SmallTitle("成绩查询")
            Card(modifier = Modifier.padding(cardPadding)) {
                SwitchPreference(title = "隐藏不及格成绩",
                    summary = "应该永远都用不到这个功能吧",
                    checked = AppConfig.hideFailScore,
                    onCheckedChange = {
                        AppConfig.hideFailScore = it
                        AppConfig.save()
                    }
                )
            }

            SmallTitle("教务系统")
            Card(modifier = Modifier.padding(cardPadding)) {
                val serverItems = listOf(
                    "http://61.187.179.66:8924/",
                    "http://jwzx.usc.edu.cn:8924/"
                )
                var selectedServer by remember { mutableIntStateOf(0) }
                selectedServer = if (AppConfig.serverUrl == serverItems[1]) 1 else 0
                WindowDropdownPreference(
                    title = "服务器",
                    items = serverItems,
                    selectedIndex = selectedServer,
                    onSelectedIndexChange = {
                        if (it != selectedServer) {
                            selectedServer = it
                            AppConfig.serverUrl = serverItems[it]
                            AppConfig.save()
                        }
                    }
                )
                val termFilterItems = remember(AppConfig.rawTermValueMap) {
                    listOf("不过滤") + AppConfig.rawTermValueMap.values.toList()
                }
                var selectedTermFilter by remember { mutableIntStateOf(0) }
                selectedTermFilter = if (AppConfig.termFilterStartId.isEmpty()) 0
                    else AppConfig.rawTermValueMap.keys.indexOf(AppConfig.termFilterStartId) + 1
                WindowDropdownPreference(
                    title = "学期过滤",
                    items = termFilterItems,
                    selectedIndex = selectedTermFilter,
                    onSelectedIndexChange = {
                        if (it != selectedTermFilter) {
                            selectedTermFilter = it
                            val newFilterId = if (it == 0) ""
                                else AppConfig.rawTermValueMap.keys.elementAtOrNull(it - 1) ?: ""
                            AppConfig.termFilterStartId = newFilterId
                            AppConfig.applyTermFilter()
                            AppConfig.save()
                        }
                    }
                )
            }

            SmallTitle("关于")
            Card(modifier = Modifier.padding(cardPadding)) {
                BasicComponent (
                    title = "应用版本",
                    endActions = {
                        Text(
                            text = getPlatform().appVersion,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    }
                )
                ArrowPreference(title = "开放源代码许可",
                    onClick = {
                        navigator.push(Route.OssLicense)
                    }
                )
                BasicComponent(
                    title = "问题反馈",
                    endActions = {
                        Text(
                            text = "nggjx@pgigi.com",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    onClick = {
                        uriHandler.openUri("mailto:nggjx@pgigi.com")
                    }
                )
                BasicComponent(
                    title = "问题反馈",
                    endActions = {
                        Text(
                            text = "pumpkintoolkit@pgigi.com",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            textAlign = TextAlign.End,
                        )
                    },
                    onClick = {
                        uriHandler.openUri("mailto:pumpkintoolkit@pgigi.com")
                    }
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
        WindowDialog(title = "请选择开课时间", show = showDialog.value, onDismissRequest = { showDialog.value = false }) {
            val dismiss = LocalDismissState.current
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val numberDatePickerState = rememberNumberDatePickerState(if(AppConfig.startDate==null) localDate else AppConfig.startDate!!)
//                Text(text = "请注意: 星期日算一周的第一天!")
                NumberDatePicker(
                    numberDatePickerState = numberDatePickerState,
                    start = LocalDate(localDate.year-1, 1, 1),
                    end = LocalDate(localDate.year+1, 12, 31)
                )

                val dayList = listOf("一", "二", "三", "四", "五", "六", "日")
                Text("若${numberDatePickerState.year}/${numberDatePickerState.month}/${numberDatePickerState.day}" +
                        "(周${dayList[LocalDate(numberDatePickerState.year, numberDatePickerState.month, numberDatePickerState.day).dayOfWeek.ordinal]})开课, " +
                        "则本周为第${
                            WeekCalculator(
                                LocalDate(
                                    numberDatePickerState.year,
                                    numberDatePickerState.month,
                                    numberDatePickerState.day
                                ), 1
                            ).getWeekNumber(localDate)}周")
                Row {
                    TextButton(
                        text = "取消",
                        onClick = { dismiss?.invoke() },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    )
                    TextButton(
                        text = "确认",
                        colors = TextButtonColors(
                            MiuixTheme.colorScheme.primary,
                            MiuixTheme.colorScheme.disabledPrimary,
                            MiuixTheme.colorScheme.onPrimary,
                            MiuixTheme.colorScheme.disabledOnPrimary
                        ),
                        onClick = {
                            AppConfig.startDate = LocalDate(numberDatePickerState.year, numberDatePickerState.month, numberDatePickerState.day)
                            AppConfig.save()
                            dismiss?.invoke()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}