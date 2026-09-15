package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.ColorSchemeMode
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.animation.PredictiveBackAnimation
import com.pgigi.pumpkintoolkit.animation.PredictiveBackExitDirection
import com.pgigi.pumpkintoolkit.components.material3.M3GroupHeader
import com.pgigi.pumpkintoolkit.components.material3.M3GroupSection
import com.pgigi.pumpkintoolkit.components.material3.M3PickerDialog
import com.pgigi.pumpkintoolkit.components.material3.M3Row
import com.pgigi.pumpkintoolkit.components.material3.M3SectionSpacer
import com.pgigi.pumpkintoolkit.components.material3.M3TrailingText
import com.pgigi.pumpkintoolkit.components.material3.NumberDatePicker
import com.pgigi.pumpkintoolkit.components.rememberNumberDatePickerState
import com.pgigi.pumpkintoolkit.constants.TimeList
import com.pgigi.pumpkintoolkit.getPlatform
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import com.pgigi.pumpkintoolkit.utils.WidgetDataStore
import com.pgigi.pumpkintoolkit.utils.reloadWidgetTimelines
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import kotlin.math.roundToInt
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3SettingScreen() {
    val navigator = LocalNavigator.current
    val showDialog = remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current

    var pickerDialog by remember { mutableStateOf<PickerState?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigator.pop()
                    }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme settings
            M3GroupHeader("主题设置")
            M3GroupSection {
                val uiItems = listOf("Miuix", "Material 3")
                val uiIndex = if (AppConfig.uiMode == 2) 1 else 0
                M3Row(
                    title = "UI 风格",
                    trailingContent = { M3TrailingText(uiItems[uiIndex]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "UI 风格",
                            items = uiItems,
                            selectedIndex = uiIndex,
                            onSelected = {
                                val newMode = if (it == 1) 2 else 0
                                if (newMode != AppConfig.uiMode) {
                                    AppConfig.uiMode = newMode
                                    AppConfig.save()
                                }
                            }
                        )
                    },
                )
                val colorItems = listOf("跟随系统", "浅色模式", "深色模式")
                val selectedColor = when (AppConfig.colorSchemeMode) {
                    ColorSchemeMode.Light -> 1
                    ColorSchemeMode.Dark -> 2
                    else -> 0
                }
                M3Row(
                    title = "主题模式",
                    trailingContent = { M3TrailingText(colorItems[selectedColor]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "主题模式",
                            items = colorItems,
                            selectedIndex = selectedColor,
                            onSelected = {
                                AppConfig.colorSchemeMode = when (it) {
                                    1 -> ColorSchemeMode.Light
                                    2 -> ColorSchemeMode.Dark
                                    else -> ColorSchemeMode.System
                                }
                                AppConfig.save()
                            }
                        )
                    },
                )
                M3Row(
                    title = "悬浮导航栏",
                    summary = "仅在单栏模式下生效",
                    trailingContent = {
                        Switch(
                            checked = AppConfig.floatingNavigation,
                            onCheckedChange = {
                                AppConfig.floatingNavigation = it
                                AppConfig.save()
                            }
                        )
                    },
                    onClick = {
                        AppConfig.floatingNavigation = !AppConfig.floatingNavigation
                        AppConfig.save()
                    },
                )
                AnimatedVisibility(visible = AppConfig.floatingNavigation){
                    M3Row(
                        title = "悬浮导航栏液态玻璃效果",
                        summary = "Android 需 13+(SDK 33+) 版本才能使用",
                        trailingContent = {
                            Switch(
                                checked = AppConfig.enableBlurEffect,
                                onCheckedChange = {
                                    AppConfig.enableBlurEffect = it
                                    AppConfig.save()
                                }
                            )
                        },
                        onClick = {
                            AppConfig.enableBlurEffect = !AppConfig.enableBlurEffect
                            AppConfig.save()
                        },
                    )
                }
                val displayModeItems = listOf("列表", "平铺")
                M3Row(
                    title = "功能列表显示模式",
                    trailingContent = { M3TrailingText(displayModeItems[AppConfig.functionDisplayMode]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "功能列表显示模式",
                            items = displayModeItems,
                            selectedIndex = AppConfig.functionDisplayMode,
                            onSelected = {
                                AppConfig.functionDisplayMode = it
                                AppConfig.save()
                            }
                        )
                    },
                )
                val predictiveBackAnimationItems = PredictiveBackAnimation.entries.map { it.displayName }
                M3Row(
                    title = "预测返回动画",
                    summary = "部分设备不支持，开启将不支持平行视界",
                    trailingContent = { M3TrailingText(AppConfig.predictiveBackAnimation.displayName) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "预测返回动画",
                            items = predictiveBackAnimationItems,
                            selectedIndex = PredictiveBackAnimation.entries.indexOf(AppConfig.predictiveBackAnimation),
                            onSelected = {
                                AppConfig.predictiveBackAnimation = PredictiveBackAnimation.entries[it]
                                AppConfig.save()
                            }
                        )
                    },
                )
                AnimatedVisibility(AppConfig.predictiveBackAnimation == PredictiveBackAnimation.Scale){
                    val exitDirectionItems =
                        PredictiveBackExitDirection.entries.map { it.displayName }
                    M3Row(
                        title = "返回退出方向",
                        trailingContent = { M3TrailingText(AppConfig.predictiveBackExitDirection.displayName) },
                        onClick = {
                            pickerDialog = PickerState(
                                title = "返回退出方向",
                                items = exitDirectionItems,
                                selectedIndex = PredictiveBackExitDirection.entries.indexOf(
                                    AppConfig.predictiveBackExitDirection
                                ),
                                onSelected = {
                                    AppConfig.predictiveBackExitDirection =
                                        PredictiveBackExitDirection.entries[it]
                                    AppConfig.save()
                                }
                            )
                        },
                    )
                }
            }

            // Schedule settings
            M3GroupHeader("课表设置")
            M3GroupSection {
                val timeTypeItems = listOf("自动切换", "夏秋时间", "秋冬时间")
                M3Row(
                    title = "课程时间",
                    trailingContent = { M3TrailingText(timeTypeItems[AppConfig.timeSeason]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "课程时间",
                            items = timeTypeItems,
                            selectedIndex = AppConfig.timeSeason,
                            onSelected = {
                                AppConfig.timeSeason = it
                                AppConfig.save()
                                val localDate = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                                AppConfig.timeList = when (AppConfig.timeSeason) {
                                    1 -> TimeList.summerAutumnTime
                                    2 -> TimeList.winterSpringTime
                                    else -> if (localDate.month.number in 5..<10) TimeList.summerAutumnTime else TimeList.winterSpringTime
                                }
                            }
                        )
                    },
                )
                M3Row(
                    title = "课表开始时间",
                    trailingContent = {
                        M3TrailingText(AppConfig.startDate?.toString() ?: "")
                    },
                    onClick = {
                        showDialog.value = true
                    },
                )
                M3Row(
                    title = "锁定课表开始时间",
                    summary = "关闭此按钮将自动同步课表开始时间",
                    trailingContent = {
                        Switch(
                            checked = AppConfig.lockStartDate,
                            onCheckedChange = {
                                AppConfig.lockStartDate = it
                                AppConfig.save()
                            }
                        )
                    },
                    onClick = {
                        AppConfig.lockStartDate = !AppConfig.lockStartDate
                        AppConfig.save()
                    },
                )

                var cellHeight by remember { mutableFloatStateOf(AppConfig.cellHeight.toFloat()) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "单元格高度",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${cellHeight.roundToInt()} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    var lastStep by remember { mutableIntStateOf(((cellHeight - 50f) / 5f).roundToInt()) }
                    Slider(
                        value = cellHeight,
                        onValueChange = {
                            cellHeight = it
                            AppConfig.cellHeight = it.roundToInt()
                            AppConfig.save()
                            val currentStep = ((it - 50f) / 5f).roundToInt()
                            if (currentStep != lastStep) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastStep = currentStep
                            }
                        },
                        valueRange = 50f..100f,
                        steps = 9,
                    )
                }
//                HorizontalDivider()
                val lineItems = listOf("1", "2", "3", "4")
                M3Row(
                    title = "课程名称显示行数",
                    trailingContent = { M3TrailingText(lineItems[AppConfig.courseNameLine - 1]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "课程名称显示行数",
                            items = lineItems,
                            selectedIndex = AppConfig.courseNameLine - 1,
                            onSelected = {
                                AppConfig.courseNameLine = it + 1
                                AppConfig.save()
                            }
                        )
                    },
                )
                M3Row(
                    title = "课程教室显示行数",
                    trailingContent = { M3TrailingText(lineItems[AppConfig.courseRoomLine - 1]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "课程教室显示行数",
                            items = lineItems,
                            selectedIndex = AppConfig.courseRoomLine - 1,
                            onSelected = {
                                AppConfig.courseRoomLine = it + 1
                                AppConfig.save()
                            }
                        )
                    },
                )
                M3Row(
                    title = "课程教师显示行数",
                    trailingContent = { M3TrailingText(lineItems[AppConfig.courseTeacherLine - 1]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "课程教师显示行数",
                            items = lineItems,
                            selectedIndex = AppConfig.courseTeacherLine - 1,
                            onSelected = {
                                AppConfig.courseTeacherLine = it + 1
                                AppConfig.save()
                            }
                        )
                    },
                )
            }

            // Score settings
            M3GroupHeader("成绩查询")
            M3GroupSection {
                M3Row(
                    title = "隐藏不及格成绩",
                    summary = "应该永远都用不到这个功能吧",
                    trailingContent = {
                        Switch(
                            checked = AppConfig.hideFailScore,
                            onCheckedChange = {
                                AppConfig.hideFailScore = it
                                AppConfig.save()
                            }
                        )
                    },
                    onClick = {
                        AppConfig.hideFailScore = !AppConfig.hideFailScore
                        AppConfig.save()
                    },
                    showDivider = false
                )
            }

            // Server settings
            M3GroupHeader("教务系统")
            M3GroupSection {
                val serverItems = listOf(
                    "http://61.187.179.66:8924/",
                    "http://jwzx.usc.edu.cn:8924/"
                )
                val selectedServer = if (AppConfig.serverUrl == serverItems[1]) 1 else 0
                M3Row(
                    title = "服务器",
                    trailingContent = { M3TrailingText(serverItems[selectedServer]) },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "服务器",
                            items = serverItems,
                            selectedIndex = selectedServer,
                            onSelected = {
                                AppConfig.serverUrl = serverItems[it]
                                AppConfig.save()
                            }
                        )
                    },
                )
                val termFilterItems = remember(AppConfig.rawTermValueMap) {
                    listOf("不过滤") + AppConfig.rawTermValueMap.values.toList()
                }
                val termFilterIndex = if (AppConfig.termFilterStartId.isEmpty()) 0
                    else AppConfig.rawTermValueMap.keys.indexOf(AppConfig.termFilterStartId) + 1
                M3Row(
                    title = "学期过滤",
                    summary = "早于所选学期的将不再显示",
                    trailingContent = {
                        M3TrailingText(termFilterItems.getOrElse(termFilterIndex) { "不过滤" })
                    },
                    onClick = {
                        pickerDialog = PickerState(
                            title = "学期过滤",
                            items = termFilterItems,
                            selectedIndex = termFilterIndex.coerceAtLeast(0),
                            onSelected = { index ->
                                val newFilterId = if (index == 0) ""
                                    else AppConfig.rawTermValueMap.keys.elementAtOrNull(index - 1) ?: ""
                                if (newFilterId != AppConfig.termFilterStartId) {
                                    AppConfig.termFilterStartId = newFilterId
                                    AppConfig.applyTermFilter()
                                    AppConfig.save()
                                }
                            }
                        )
                    },
                )
            }

            // About
            M3GroupHeader("关于")
            M3GroupSection {
                M3Row(
                    title = "应用版本",
                    trailingContent = { M3TrailingText(getPlatform().appVersion) },
                )
                M3Row(
                    title = "开放源代码许可",
                    onClick = {
                        navigator.push(Route.OssLicense)
                    },
                )
                M3Row(
                    title = "问题反馈",
                    summary = "nggjx@pgigi.com",
                    onClick = {
                        uriHandler.openUri("mailto:nggjx@pgigi.com")
                    },
                )
                M3Row(
                    title = "问题反馈",
                    summary = "pumpkintoolkit@pgigi.com",
                    onClick = {
                        uriHandler.openUri("mailto:pumpkintoolkit@pgigi.com")
                    },
                )
            }

            // Debug
            M3GroupHeader("调试")
            M3GroupSection {
                M3Row(
                    title = "清除应用及 Keychain 数据",
                    summary = "清除所有配置、登录信息和小组件课表缓存",
                    onClick = {
                        AppConfig.kvault.clear()
                        WidgetDataStore.clearWidgetData()
                        reloadWidgetTimelines()
                    },
                    showDivider = false
                )
            }

            M3SectionSpacer()
        }
    }

    // Date picker dialog
    if (showDialog.value) {
        val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val numberDatePickerState = rememberNumberDatePickerState(
            if (AppConfig.startDate == null) localDate else AppConfig.startDate!!
        )
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("请选择开课时间") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
//                    Text("请注意: 星期日算一周的第一天!")
                    NumberDatePicker(
                        numberDatePickerState = numberDatePickerState,
                        start = LocalDate(localDate.year - 1, 1, 1),
                        end = LocalDate(localDate.year + 1, 12, 31)
                    )
                    val dayList = listOf("一", "二", "三", "四", "五", "六", "日")
                    Text(
                        "若${numberDatePickerState.year}/${numberDatePickerState.month}/${numberDatePickerState.day}" +
                                "(周${dayList[LocalDate(numberDatePickerState.year, numberDatePickerState.month, numberDatePickerState.day).dayOfWeek.ordinal]})开课, " +
                                "则本周为第${
                                    WeekCalculator(
                                        LocalDate(
                                            numberDatePickerState.year,
                                            numberDatePickerState.month,
                                            numberDatePickerState.day
                                        ), 1
                                    ).getWeekNumber(localDate)
                                }周"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    AppConfig.startDate = LocalDate(
                        numberDatePickerState.year,
                        numberDatePickerState.month,
                        numberDatePickerState.day
                    )
                    AppConfig.save()
                    showDialog.value = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showDialog.value = false
                }) { Text("取消") }
            }
        )
    }

    // Picker dialog for dropdowns
    pickerDialog?.let { state ->
        M3PickerDialog(
            title = state.title,
            items = state.items,
            selectedIndex = state.selectedIndex,
            onSelectedIndexChange = state.onSelected,
            onDismiss = { pickerDialog = null }
        )
    }
}

private data class PickerState(
    val title: String,
    val items: List<String>,
    val selectedIndex: Int,
    val onSelected: (Int) -> Unit
)
