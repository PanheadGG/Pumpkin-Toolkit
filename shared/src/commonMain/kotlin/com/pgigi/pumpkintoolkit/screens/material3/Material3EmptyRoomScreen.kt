package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.components.material3.M3Card
import com.pgigi.pumpkintoolkit.components.material3.M3PickerDialog
import com.pgigi.pumpkintoolkit.components.material3.M3Row
import com.pgigi.pumpkintoolkit.components.material3.M3TrailingText
import com.pgigi.pumpkintoolkit.components.material3.NumberDatePicker
import com.pgigi.pumpkintoolkit.components.rememberNumberDatePickerState
import com.pgigi.pumpkintoolkit.utils.JsonUtil
import com.pgigi.pumpkintoolkit.utils.PinyinUtils
import com.pgigi.pumpkintoolkit.utils.QZClient
import com.pgigi.pumpkintoolkit.utils.ResourceUtils
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import com.pgigi.pumpkintoolkit.viewmodel.EmptyRoomViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3EmptyRoomScreen(
    viewModel: EmptyRoomViewModel = viewModel(factory = EmptyRoomViewModel.Factory)
) {
    val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val numberDatePickerState = rememberNumberDatePickerState()
    val navigator = LocalNavigator.current
    val windowInfo = LocalWindowInfo.current
    var showOperations by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    var pickerState by remember { mutableStateOf<M3EmptyRoomPicker?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var displayList by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var showTermSection by remember { mutableStateOf(AppConfig.startDate == null) }
    var showStartDateDialog by remember { mutableStateOf(false) }
    val startDatePickerState = rememberNumberDatePickerState()

    LaunchedEffect(viewModel.startDate) {
        if (viewModel.startDate == null) showTermSection = true
    }

    LaunchedEffect(viewModel.selectedTermIndex) {
        if (viewModel.selectedTermIndex < 0) return@LaunchedEffect
        if (AppConfig.termValueList.isEmpty()) return@LaunchedEffect
        val termId = AppConfig.termValueList[viewModel.selectedTermIndex]
        val cached = viewModel.termStartDates[termId]
        if (cached != null) {
            viewModel.startDate = cached
            return@LaunchedEffect
        }
        if (viewModel.startDate != null) {
            viewModel.termStartDates[termId] = viewModel.startDate!!
            return@LaunchedEffect
        }
        val fetched = QZClient.getStartDate(termId)
        if (fetched != null) {
            viewModel.startDate = fetched
            viewModel.termStartDates[termId] = fetched
        } else {
            viewModel.startDate = null
            showTermSection = true
        }
    }

    LaunchedEffect(Unit) {
        val json = ResourceUtils.readText("buildings.json")
        json?.let {
            val list = JsonUtil.parseListJson(json, com.pgigi.pumpkintoolkit.screens.miuix.Building.serializer())
            if (list.isNotEmpty()) {
                viewModel.buildings.clear()
                viewModel.buildings.addAll(list)
                viewModel.buildingItems.clear()
                viewModel.buildingList.clear()
                val schoolMapping = mapOf(0 to "1", 1 to "2", 2 to "3")
                val filteredBuildings =
                    viewModel.buildings.filter { it.school == schoolMapping[viewModel.selectedSchool] }
                for (building in filteredBuildings) {
                    viewModel.buildingItems.add(building.name)
                    viewModel.buildingList.add(building.value)
                }
            }
        }
        if (AppConfig.startDate != null) {
            viewModel.startDate = AppConfig.startDate
        }
        if (AppConfig.defaultTermId.isNotEmpty()) {
            val index = AppConfig.termValueList.indexOf(AppConfig.defaultTermId)
            if (index >= 0) {
                viewModel.selectedTermIndex = index
            }
        }
    }

    val schoolMapping = mapOf(0 to "1", 1 to "2", 2 to "3")
    LaunchedEffect(viewModel.selectedSchool) {
        if (viewModel.buildings.isEmpty()) return@LaunchedEffect
        viewModel.buildingItems.clear()
        viewModel.buildingList.clear()
        val filteredBuildings =
            viewModel.buildings.filter { it.school == schoolMapping[viewModel.selectedSchool] }
        for (building in filteredBuildings) {
            viewModel.buildingItems.add(building.name)
            viewModel.buildingList.add(building.value)
        }
        viewModel.selectedBuilding = 0
    }

    listOf("红湘", "雨母", "校外")
    listOf("第1、2节", "第3、4节", "第5、6节", "第7、8节", "第9、10节")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("空教室查询") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigator.pop()
                    }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    if (windowInfo.containerDpSize.width < 800.dp) {
                        AnimatedVisibility(
                            visible = showOperations,
                        ) {
                            IconButton(onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showOperations = !showOperations
                            }) {
                                Icon(MiuixIcons.ExpandLess, contentDescription = "隐藏查询操作台")
                            }
                        }
                        AnimatedVisibility(visible = !showOperations) {
                            IconButton(onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showOperations = !showOperations
                            }) {
                                Icon(MiuixIcons.ExpandMore, contentDescription = "显示查询操作台")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (windowInfo.containerDpSize.width >= 800.dp) {
            Row(modifier = Modifier.padding(paddingValues)) {
                M3EmptyRoomOperations(
                    Modifier.width(400.dp),
                    viewModel,
                    numberDatePickerState,
                    snackbarHostState,
                    windowInfo.containerDpSize.width,
                    pickerState,
                    onPickerChange = { pickerState = it },
                    onDateClick = { showDatePicker = true },
                    isLoading = isLoading,
                    onLoadingChange = { isLoading = it },
                    onQuerySuccess = { },
                    onDisplayListChange = { displayList = it },
                    showTermSection = showTermSection,
                    onShowTermSectionChange = { showTermSection = it },
                    startDatePickerState = startDatePickerState,
                    onShowStartDateDialog = {
                        viewModel.startDate?.let {
                            startDatePickerState.year = it.year
                            startDatePickerState.month = it.month.number
                            startDatePickerState.day = it.day
                        }
                        showStartDateDialog = true
                    }
                )
                M3EmptyRoomResultList(Modifier.fillMaxSize(), displayList)
            }
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                AnimatedVisibility(showOperations) {
                    M3EmptyRoomOperations(
                        Modifier,
                        viewModel,
                        numberDatePickerState,
                        snackbarHostState,
                        windowInfo.containerDpSize.width,
                        pickerState,
                        onPickerChange = { pickerState = it },
                        onDateClick = { showDatePicker = true },
                        isLoading = isLoading,
                        onLoadingChange = { isLoading = it },
                        onQuerySuccess = { showOperations = false },
                        onDisplayListChange = { displayList = it },
                        showTermSection = showTermSection,
                        onShowTermSectionChange = { showTermSection = it },
                        startDatePickerState = startDatePickerState,
                        onShowStartDateDialog = {
                            viewModel.startDate?.let {
                                startDatePickerState.year = it.year
                                startDatePickerState.month = it.month.number
                                startDatePickerState.day = it.day
                            }
                            showStartDateDialog = true
                        }
                    )
                }
                M3EmptyRoomResultList(Modifier.fillMaxSize(), displayList)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val dayList = listOf("一", "二", "三", "四", "五", "六", "日")
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("请选择日期") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NumberDatePicker(
                        numberDatePickerState = numberDatePickerState,
                        start = LocalDate(localDate.year - 1, 1, 1),
                        end = LocalDate(localDate.year + 1, 12, 31)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDatePicker = false
                }) {
                    Text("确定(周${
                        dayList[LocalDate(
                            numberDatePickerState.year,
                            numberDatePickerState.month,
                            numberDatePickerState.day
                        ).dayOfWeek.ordinal]
                    })")
                }
            }
        )
    }

    // Start date picker dialog
    if (showStartDateDialog) {
        AlertDialog(
            onDismissRequest = { showStartDateDialog = false },
            title = { Text("请选择开课日期") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NumberDatePicker(
                        numberDatePickerState = startDatePickerState,
                        start = LocalDate(localDate.year - 1, 1, 1),
                        end = LocalDate(localDate.year + 1, 12, 31)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.startDate = LocalDate(
                        startDatePickerState.year,
                        startDatePickerState.month,
                        startDatePickerState.day
                    )
                    if (viewModel.selectedTermIndex in AppConfig.termValueList.indices) {
                        val termId = AppConfig.termValueList[viewModel.selectedTermIndex]
                        viewModel.termStartDates[termId] = viewModel.startDate!!
                    }
                    showStartDateDialog = false
                }) {
                    Text("确定")
                }
            }
        )
    }

    // Picker dialog
    pickerState?.let { state ->
        M3PickerDialog(
            title = state.title,
            items = state.items,
            selectedIndex = state.selectedIndex,
            onSelectedIndexChange = state.onSelected,
            onDismiss = { pickerState = null }
        )
    }
}

@Composable
private fun M3EmptyRoomOperations(
    modifier: Modifier = Modifier,
    viewModel: EmptyRoomViewModel,
    numberDatePickerState: com.pgigi.pumpkintoolkit.components.NumberDatePickerState,
    snackbarHostState: SnackbarHostState,
    screenWidthDp: Dp,
    pickerState: M3EmptyRoomPicker?,
    onPickerChange: (M3EmptyRoomPicker?) -> Unit,
    onDateClick: () -> Unit,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onQuerySuccess: () -> Unit,
    onDisplayListChange: (List<Pair<String, Boolean>>) -> Unit,
    showTermSection: Boolean,
    onShowTermSectionChange: (Boolean) -> Unit,
    startDatePickerState: com.pgigi.pumpkintoolkit.components.NumberDatePickerState,
    onShowStartDateDialog: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val schoolItems = listOf("红湘", "雨母", "校外")
    val lessonItems = listOf("第1、2节", "第3、4节", "第5、6节", "第7、8节", "第9、10节")
    val dayList = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = modifier) {
        M3Row(
            title = "校区",
            trailingContent = { M3TrailingText(schoolItems[viewModel.selectedSchool]) },
            onClick = {
                onPickerChange(
                    M3EmptyRoomPicker(
                        title = "校区",
                        items = schoolItems,
                        selectedIndex = viewModel.selectedSchool,
                        onSelected = { viewModel.selectedSchool = it }
                    )
                )
            },
        )
        M3Row(
            title = "教学楼",
            trailingContent = {
                M3TrailingText(
                    viewModel.buildingItems.getOrElse(viewModel.selectedBuilding) { "请选择" }
                )
            },
            onClick = {
                onPickerChange(
                    M3EmptyRoomPicker(
                        title = "教学楼",
                        items = viewModel.buildingItems.toList(),
                        selectedIndex = viewModel.selectedBuilding,
                        onSelected = { viewModel.selectedBuilding = it }
                    )
                )
            },
        )
        M3Row(
            title = "日期",
            trailingContent = {
                val date = LocalDate(
                    numberDatePickerState.year,
                    numberDatePickerState.month,
                    numberDatePickerState.day
                )
                M3TrailingText("${date}(周${dayList[date.dayOfWeek.ordinal]})")
            },
            onClick = onDateClick,
        )
        M3Row(
            title = "节次",
            trailingContent = { M3TrailingText(lessonItems[viewModel.selectedLesson]) },
            onClick = {
                onPickerChange(
                    M3EmptyRoomPicker(
                        title = "节次",
                        items = lessonItems,
                        selectedIndex = viewModel.selectedLesson,
                        onSelected = { viewModel.selectedLesson = it }
                    )
                )
            },
            showDivider = false
        )
        M3Row(
            title = "学期及开课日期",
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showTermSection) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = { onShowTermSectionChange(!showTermSection) }
        )
        AnimatedVisibility(showTermSection) {
            Column {
                M3Row(
                    title = "学期",
                    trailingContent = {
                        M3TrailingText(
                            if (viewModel.selectedTermIndex >= 0 && viewModel.selectedTermIndex < AppConfig.termNameList.size)
                                AppConfig.termNameList[viewModel.selectedTermIndex]
                            else "请选择"
                        )
                    },
                    onClick = {
                        onPickerChange(
                            M3EmptyRoomPicker(
                                title = "学期",
                                items = if (AppConfig.termNameList.isEmpty()) listOf("请先登录") else AppConfig.termNameList.toList(),
                                selectedIndex = viewModel.selectedTermIndex.coerceAtLeast(0),
                                onSelected = { viewModel.selectedTermIndex = it }
                            )
                        )
                    }
                )
                M3Row(
                    title = "开课日期",
                    trailingContent = { M3TrailingText(viewModel.startDate?.toString() ?: "点击设置") },
                    onClick = onShowStartDateDialog,
                    showDivider = false
                )
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = !isLoading,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val termId = if (viewModel.selectedTermIndex in AppConfig.termValueList.indices) {
                    AppConfig.termValueList[viewModel.selectedTermIndex]
                } else AppConfig.defaultTermId
                if (termId.isEmpty()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = "请选择学期", withDismissAction = true)
                    }
                    return@Button
                }
                if (viewModel.startDate == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = "请设置开课日期", withDismissAction = true)
                    }
                    return@Button
                }
                if (viewModel.buildingList.getOrElse(viewModel.selectedBuilding) { "" }.isEmpty()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = "请选择教学楼", withDismissAction = true)
                    }
                    return@Button
                }
                onLoadingChange(true)
                coroutineScope.launch {
                    viewModel.emptyRoomMap.clear()
                    val queryDate = LocalDate(
                        numberDatePickerState.year,
                        numberDatePickerState.month,
                        numberDatePickerState.day
                    )
                    val weekCalculator = WeekCalculator(viewModel.startDate!!, 1)
                    val emptyRooms = QZClient.getEmptyRooms(
                        termId,
                        viewModel.buildingList[viewModel.selectedBuilding],
                        queryDate.dayOfWeek.isoDayNumber,
                        weekCalculator.getWeekNumber(queryDate).toInt(),
                        viewModel.selectedLesson * 2 + 1
                    )
                    if (emptyRooms != null) {
                        viewModel.emptyRoomMap.putAll(emptyRooms)
                        val list = viewModel.emptyRoomMap.entries.map { Pair(it.key, it.value) }
                            .sortedBy { PinyinUtils.toPinyin(it.first) }
                            .sortedByDescending { it.second }
                        onDisplayListChange(list)
                        onLoadingChange(false)
                        onQuerySuccess()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message = "查询完成", withDismissAction = true)
                        }
                    } else {
                        onLoadingChange(false)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message = "查询失败，请检查网络或教务系统", withDismissAction = true)
                        }
                    }
                }
            }
        ) {
            if (!isLoading) {
                Text("查询")
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun M3EmptyRoomResultList(modifier: Modifier = Modifier, displayList: List<Pair<String, Boolean>>) {
    LazyColumn(
        modifier = modifier.padding(12.dp)
    ) {
        items(displayList.size + 1) { index ->
            if (index < displayList.size) {
                M3Card(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayList[index].first,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (displayList[index].second) "教务系统无课" else "有课",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (displayList[index].second)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "本工具需要程序获取到正确学期、且正确设置开学日期的情况下才能正常使用。\n" +
                            "结果默认以中文顺序显示，教务系统无课程的教室优先显示。\n" +
                            "本工具仅是将教务系统返回的教室课程信息进行解析，不保证空教室信息正确性。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

private data class M3EmptyRoomPicker(
    val title: String,
    val items: List<String>,
    val selectedIndex: Int,
    val onSelected: (Int) -> Unit
)
