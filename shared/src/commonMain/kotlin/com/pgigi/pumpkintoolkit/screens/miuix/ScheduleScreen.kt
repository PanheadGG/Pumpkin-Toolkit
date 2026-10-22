package com.pgigi.pumpkintoolkit.screens.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalAppViewModel
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.CourseDetailSheet
import com.pgigi.pumpkintoolkit.components.miuix.SchedulePager
import com.pgigi.pumpkintoolkit.constants.FileName
import com.pgigi.pumpkintoolkit.constants.Texts
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.CustomCourse
import com.pgigi.pumpkintoolkit.models.ScheduleCache
import com.pgigi.pumpkintoolkit.utils.FileStoreUtils
import com.pgigi.pumpkintoolkit.utils.JsonUtil
import com.pgigi.pumpkintoolkit.utils.QZClient
import com.pgigi.pumpkintoolkit.utils.buildWeekCourses
import com.pgigi.pumpkintoolkit.utils.buildWeekScheduleEntries
import com.pgigi.pumpkintoolkit.utils.reloadWidgetTimelines
import com.pgigi.pumpkintoolkit.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RangeSlider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun ScheduleScreen(modifier: Modifier = Modifier, viewModel: AppViewModel = LocalAppViewModel.current ){
    val loggedIn = AppConfig.username.isNotEmpty() && AppConfig.password.isNotEmpty()
    val navigator = LocalNavigator.current
//    var title by remember { mutableStateOf("课程表") }           //原先的title只取决于第几周、是否拥有课程。对于添加自定义课表后，可能显示过时的标题，故进行修改
    val coroutineScope = rememberCoroutineScope()
    var initialPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp = windowInfo.containerDpSize.width

    //以下为自定义课程组件的相关变量
    var showAddEditor by remember { mutableStateOf(false) }
    var courseName by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(-1) }
    var selectedWeeks by remember { mutableStateOf(emptySet<Int>()) }
    val weekCount = if (AppConfig.totalWeek > 0) AppConfig.totalWeek else 20   //使用AppConfig中的设置参数；否则默认20周
    var startLesson by remember { mutableIntStateOf(1) }
    var endLesson by remember { mutableIntStateOf(2) }  //[startLesson, endLesson]
    val lessonCount = AppConfig.timeList.size   //决定滑块最多滑多少
    var saveError by remember { mutableStateOf<String?>(null) }
    var editingCourse by remember { mutableStateOf<CustomCourse?>(null) }   //保存所编辑的自定义课程

//    val courseListByWeek by mutableStateOf(buildWeekCourses(viewModel.courseList) )           //过往只传入教务的课表
    val courseListByWeek by mutableStateOf(buildWeekCourses(viewModel.mergedCourses())) //传入融合的课表
    val scheduleEntriesByWeek = buildWeekScheduleEntries(viewModel.scheduleCourseEntries())

    //原先只需要判断教务科表，可放置在前；现在需要判断融合课表，故放在持有融合课表之后判断
    val loading = loggedIn && courseListByWeek.none{it.isNotEmpty()}    //由于所持有的课表是二维列表，故需要降低维度去观察，而非直接.isNotEmpty()

    pageCount = maxOf(courseListByWeek.size,viewModel.currentWeek,AppConfig.totalWeek)
    initialPage = viewModel.currentWeek - 1


    val pagerState = rememberPagerState(
        pageCount = {pageCount},
        initialPage = initialPage.coerceIn(0, pageCount)
    )

    val title = if (courseListByWeek.any { it.isNotEmpty() }) {
        "第${pagerState.currentPage +1}周"
    } else {
        "课程表"
    }

    LaunchedEffect(pagerState.pageCount) {
        pagerState.scrollToPage((viewModel.currentWeek - 1).coerceIn(0,pageCount))
    }

//    LaunchedEffect(pagerState.currentPage) {
//        if(viewModel.courseList.isNotEmpty()){
//            title = "第${pagerState.currentPage + 1}周"
//        }
//    }

    fun resetPara() {   //重置自定义课表的相关变量并关闭弹窗
        courseName = ""
        classroom = ""
        teacher = ""
        selectedDay = -1
        selectedWeeks = emptySet()
        startLesson = 1
        endLesson = 2
        showAddEditor = false
        saveError = null
        editingCourse = null
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SmallTopAppBar(
                title = title,
                actions = {
                    //Add的图标，点击后拉起自定义课程界面；点击后将showAddEditor变量修改为true
                    IconButton(onClick = { showAddEditor = true }) {
                        Icon(MiuixIcons.Add, contentDescription = "添加自定义课程")
                    }

                    AnimatedVisibility(screenWidthDp<=800.dp){
                        IconButton(onClick = { navigator.push(Route.Settings) }) {
                            Icon(MiuixIcons.Settings, contentDescription = "设置")
                        }
                    }
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = maxOf(0, viewModel.currentWeek-1)!=pagerState.currentPage &&
                                AppConfig.startDate!=null &&
//                                viewModel.courseList.isNotEmpty(),        //过往使用viewModel中存储的教务课表观察
                                courseListByWeek.any { it.isNotEmpty() },   //此处改为通过本类所持有的学期总课表列表观察
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        viewModel.currentWeek-1
                                    )
                                }
                            }
                        ) {
                            Icon(MiuixIcons.Reset, contentDescription = "返回当前周")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (loggedIn) {
            var isRefreshing by rememberSaveable { mutableStateOf(false)}
            val pullToRefreshState = rememberPullToRefreshState()
            LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                    delay(60000.milliseconds)
                    isRefreshing = false
                }
            }
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        val list = QZClient.getAllCourses()
                        list?.let {
                            viewModel.courseList.clear()
                            viewModel.courseList.addAll(list)
                            FileStoreUtils.writeString(
                                FileName.SCHEDULE,
                                JsonUtil.toJson(
                                    ScheduleCache(
                                        updateTime = Clock.System.now().toEpochMilliseconds(),
                                        courses = list
                                    ),
                                    ScheduleCache.serializer()
                                )
                            )
                        }
                        if(!AppConfig.lockStartDate){
                            val startDate = QZClient.getStartDate()
                            startDate?.let {
                                AppConfig.startDate = startDate
                                AppConfig.save()
                            }
                        }
                        val totalWeek = QZClient.getWeekNum()
                        totalWeek?.let {
                            AppConfig.totalWeek = totalWeek
                            AppConfig.save()
                        }
                        val map = QZClient.getTermValueMap()
                        map?.let {
                            AppConfig.updateTermData(it)
                        }
                        isRefreshing = false
                        reloadWidgetTimelines()
                    }
                },
                pullToRefreshState = pullToRefreshState,
                modifier = Modifier.padding(paddingValues),
                refreshTexts = Texts.REFRESH_TEXTS,
            ) {
                SchedulePager(
                    modifier = Modifier.fillMaxSize(),
                    cellHeight = AppConfig.cellHeight.dp,
                    courseListByWeek = courseListByWeek,
                    entriesListByWeek = scheduleEntriesByWeek,
                    pagerState = pagerState,
                    timeList = AppConfig.timeList,
                    startDate = AppConfig.startDate,
                    onAddCell = { day, lesson,week ->   //当点击滚木卡片后，自动填写表单
                        resetPara()
                        selectedDay = day
                        selectedWeeks = setOf(week)
                        startLesson = if ( lesson % 2 ==1 ){    //我们使用大课为单位进行填写。点击奇数卡片的时候，就是奇数没啥说的；点击偶数卡片的时候，我们需要 - 1 以获取正确的开始时间，而不是默认延后一节课
                            lesson
                        } else {
                            lesson - 1
                        }
                        endLesson = (startLesson + 1).coerceAtMost(lessonCount)
                        showAddEditor = true    //显示表单
                    },
                    onEditCustomCourse = {id ->
                        val original = viewModel.customCourseList.firstOrNull { it.id == id } //拿到id所对应的课程    可能为空！

                        original?.let { customCourse ->
                            resetPara()
                            editingCourse = customCourse    //存储现在正在修改的课程

                            val course = customCourse.course

                            //临时存储变量，用于自动填写表单
                            courseName = course.name
                            classroom = course.classroom
                            teacher = course.teacher
                            selectedDay = course.dayOfWeek
                            selectedWeeks = course.weeks.flatMap { range ->
                                (range[0]..range[1]).toList()
                            }.toSet()
                            startLesson = course.lessonOfDay
                            endLesson = course.lessonOfDay + course.duration - 1

                            CourseDetailSheet.show.value = false    //关闭课程详情页
                            showAddEditor = true    //拉起表单
                        }
                    },
                    onDeleteCustomCourse = { id ->
                        viewModel.removeCustomCourse(id)
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请登录使用",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        if(loading){
            InfiniteProgressIndicator(
                Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.windowDimming)
            )
        }

    }

    //关于添加/修改自定义课程的弹窗组件
    WindowBottomSheet(
        title = if (editingCourse == null) "添加自定义课程" else "修改自定义课程",
        show = showAddEditor,
        onDismissRequest = { resetPara() }  //用户点击后，重新置false
    ) {
        Column (
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {    //按顺序排列输入框
            TextField(  //课程名称的输入框绘制
                value = courseName,
                onValueChange = {courseName = it},
                label = "课程名称",
                modifier = Modifier.padding(bottom = 8.dp),
                singleLine = true
            )

            TextField(  //教室位置的输入框绘制
                value = classroom,
                onValueChange = {classroom = it},
                label = "教室位置",
                modifier = Modifier.padding(bottom = 8.dp),
                singleLine = true
            )

            TextField(  //教师名称的输入框绘制
                value = teacher,
                onValueChange = {teacher = it},
                label = "教师名称",
                modifier = Modifier.padding(bottom = 8.dp),
                singleLine = true
            )

            Text("星期")  //星期X的选择按钮绘制
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {   //横着排的容器，负责星期X的计算；遍历list后显示出每一个元素；效果： 星期 + X
                listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { index, day ->
                    Box(    //负责卡片大小、排列、点击
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedDay == index) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { selectedDay = index },   // 0-6 天-六
                        contentAlignment = Alignment.Center       //居中
                    ) {
                        Text(   //负责文本展示、颜色设置
                            text = day,
                            color = if(selectedDay == index) {
                                MiuixTheme.colorScheme.onPrimary
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                }
            }

            //周次选择的标题、全选按钮、清空按钮绘制
            Row (   //显示标题，并提供全选与清空按钮
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("周数", modifier = Modifier.weight(1f))

                Text(
                    "全选",
                    modifier = Modifier.clickable {
                        selectedWeeks = (1..weekCount).toSet()
                    }
                )

                Text(
                    "清空",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clickable {
                        selectedWeeks = emptySet()
                    }
                )
            }

            //周次选择按钮的绘制
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            )  {
                (1..weekCount).forEach { week ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (week in selectedWeeks) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable {    //处理点击逻辑：点击一下选择，再点一下取消
                                selectedWeeks = if (week in selectedWeeks){
                                    selectedWeeks - week
                                } else {
                                    selectedWeeks + week
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = week.toString(),
                            color = if (week in selectedWeeks) {
                                MiuixTheme.colorScheme.onPrimary
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                }
            }

            //节次选择的滑块绘制
            Text("节次：第 $startLesson ~ $endLesson 节")
            RangeSlider(
                value = startLesson.toFloat()..endLesson.toFloat(),
                onValueChange = { range ->
                    startLesson = range.start.roundToInt()
                    endLesson = range.endInclusive.roundToInt()
                },
                valueRange = 1f..lessonCount.toFloat(),
                steps = (lessonCount - 2).coerceAtLeast(0)
            )

            //出现错误了？在添加课程按钮上方显示。仅 !null 时执行
            saveError?.let { message ->
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            //添加/修改课程按钮点击后逻辑实现及绘制
            Button(
                onClick = {
                    val weeks = selectedWeeks.sorted()
                        .map { week -> intArrayOf(week, week) }
                        .toTypedArray()

                    val course = Course(
                        name = courseName.trim(),
                        classroom = classroom.trim(),
                        teacher = teacher.trim(),
                        weeks = weeks,
                        dayOfWeek = selectedDay,
                        lessonOfDay = startLesson,
                        duration = endLesson - startLesson + 1
                    )

                    val original = editingCourse    //保存点击的卡片处所在的课程，空为新增，非空则为修改

                    val saved = if (original == null){  //如果original为空，则说明是第一次添加
                        viewModel.addCustomCourse(course)
                    } else {    //否则为修改
                        viewModel.updateCustomCourse(original.copy(course = course))
                    }

                    if (saved) {    //保存成功？reset并关闭
                        resetPara()
                    } else {    //否则报错误
                        saveError = "保存失败，请重试！"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = courseName.isNotBlank() &&    //当输入了课程、周几、周次后，才允许保存
                    selectedDay in 0..6 &&
                    selectedWeeks.isNotEmpty()
            ) {
                Text(if (editingCourse == null ) "添加课程" else "保存修改")
            }
        }
    }
}


