package com.pgigi.pumpkintoolkit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.animation.PredictiveBackAnimation
import com.pgigi.pumpkintoolkit.animation.predictiveback.installerNavTransition
import com.pgigi.pumpkintoolkit.constants.FileName
import com.pgigi.pumpkintoolkit.models.ScheduleCache
import com.pgigi.pumpkintoolkit.screens.material3.Material3AppTheme
import com.pgigi.pumpkintoolkit.screens.material3.Material3EmptyRoomScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3ExamScoreScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3ExamScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3HomeScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3LoginScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3OssLicenseDetailScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3OssLicenseScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3OtherScheduleScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3PlanScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3SettingScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3SimpleHtmlScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3WebViewScreen
import com.pgigi.pumpkintoolkit.screens.material3.Material3WebViewWithDataScreen
import com.pgigi.pumpkintoolkit.screens.material3.evaluation.Material3EvaluationContainerScreen
import com.pgigi.pumpkintoolkit.screens.material3.evaluation.Material3EvaluationDetailScreen
import com.pgigi.pumpkintoolkit.screens.material3.evaluation.Material3EvaluationListScreen
import com.pgigi.pumpkintoolkit.screens.material3.sunshine.Material3LostAndFoundDetailScreen
import com.pgigi.pumpkintoolkit.screens.material3.sunshine.Material3LostAndFoundListScreen
import com.pgigi.pumpkintoolkit.screens.material3.sunshine.Material3SunshineContainerScreen
import com.pgigi.pumpkintoolkit.screens.material3.sunshine.Material3SunshineDetailScreen
import com.pgigi.pumpkintoolkit.screens.material3.sunshine.Material3SunshineListScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixEmptyRoomScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixExamScoreScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixExamScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixHomeScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixLoginScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixOtherScheduleScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixPlanScreen
import com.pgigi.pumpkintoolkit.screens.miuix.MiuixSettingScreen
import com.pgigi.pumpkintoolkit.screens.miuix.OssLicenseDetailScreen
import com.pgigi.pumpkintoolkit.screens.miuix.OssLicenseScreen
import com.pgigi.pumpkintoolkit.screens.miuix.SimpleHtmlScreen
import com.pgigi.pumpkintoolkit.screens.miuix.WebViewScreen
import com.pgigi.pumpkintoolkit.screens.miuix.WebViewWithDataScreen
import com.pgigi.pumpkintoolkit.screens.miuix.evaluation.EvaluationContainerScreen
import com.pgigi.pumpkintoolkit.screens.miuix.evaluation.MiuixEvaluationDetailScreen
import com.pgigi.pumpkintoolkit.screens.miuix.evaluation.MiuixEvaluationListScreen
import com.pgigi.pumpkintoolkit.screens.miuix.sunshine.LostAndFoundDetailScreen
import com.pgigi.pumpkintoolkit.screens.miuix.sunshine.LostAndFoundListScreen
import com.pgigi.pumpkintoolkit.screens.miuix.sunshine.SunshineContainerScreen
import com.pgigi.pumpkintoolkit.screens.miuix.sunshine.SunshineDetailScreen
import com.pgigi.pumpkintoolkit.screens.miuix.sunshine.SunshineListScreen
import com.pgigi.pumpkintoolkit.utils.FileStoreUtils
import com.pgigi.pumpkintoolkit.utils.JsonUtil
import com.pgigi.pumpkintoolkit.utils.QZClient
import com.pgigi.pumpkintoolkit.utils.WeekCalculator
import com.pgigi.pumpkintoolkit.utils.WidgetDataHelper
import com.pgigi.pumpkintoolkit.utils.WidgetDataStore
import com.pgigi.pumpkintoolkit.utils.reloadWidgetTimelines
import com.pgigi.pumpkintoolkit.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.time.Clock
import top.yukonga.miuix.kmp.theme.ColorSchemeMode as MiuixColorSchemeMode

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator found!") }
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { error("No AppViewModel found!") }

@Composable
fun App(
    viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
) {
    val navigator = remember(viewModel.backStack) { Navigator(viewModel.backStack) }
    val coroutineScope = rememberCoroutineScope()
    AppConfig.load()

    LaunchedEffect(AppConfig.startDate){
        AppConfig.startDate?.let{
            val weekCalculator = WeekCalculator(AppConfig.startDate!!,1)
            viewModel.currentWeek = weekCalculator.getWeekNumber(AppConfig.localDate).toInt()
        }
        WidgetDataStore.save(WidgetDataHelper.buildWidgetData(
            viewModel.courseList.toList(),
            AppConfig.startDate,
            AppConfig.totalWeek,
            AppConfig.timeSeason
        ))
        reloadWidgetTimelines()
    }

    coroutineScope.launch {
        val courseStr = FileStoreUtils.readString(FileName.SCHEDULE)
        courseStr?.let {
            try {
                val cache = JsonUtil.parseJson(it, ScheduleCache.serializer())
                viewModel.courseList.clear()
                viewModel.courseList.addAll(cache.courses)
                WidgetDataStore.save(WidgetDataHelper.buildWidgetData(
                    viewModel.courseList.toList(),
                    AppConfig.startDate,
                    AppConfig.totalWeek,
                    AppConfig.timeSeason
                ))
                reloadWidgetTimelines()
            } catch (_: Exception) { }
        }
    }

    QZClient.addLoginSuccessCallback {
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
            val startDate = QZClient.getStartDate()
            startDate?.let {
                if(!AppConfig.lockStartDate){
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
            val defaultTermId = QZClient.getDefaultTermId()
            defaultTermId?.let {
                AppConfig.defaultTermId = defaultTermId
                AppConfig.save()
            }
            WidgetDataStore.save(WidgetDataHelper.buildWidgetData(
                viewModel.courseList.toList(),
                AppConfig.startDate,
                AppConfig.totalWeek,
                AppConfig.timeSeason
            ))
            reloadWidgetTimelines()
        }
    }

    if(AppConfig.username.isNotEmpty() && AppConfig.password.isNotEmpty()){
        QZClient.username = AppConfig.username
        QZClient.password = AppConfig.password
        coroutineScope.launch {
            QZClient.login()
        }
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppViewModel provides viewModel,
    ){
        Box(modifier = Modifier.fillMaxSize()) {
            val transition = remember(AppConfig.predictiveBackAnimation, AppConfig.predictiveBackExitDirection) {
                installerNavTransition(AppConfig.predictiveBackAnimation, AppConfig.predictiveBackExitDirection)
            }
            val effects = if (AppConfig.predictiveBackAnimation == PredictiveBackAnimation.None) {
                remember { NavDisplayEffects() }
            } else {
                val cornerRadius = rememberNavSystemCornerRadius().coerceAtLeast(16.dp)
                remember(cornerRadius) {
                    NavDisplayEffects(cornerClipRadius = cornerRadius, cornerClipMode = NavCornerClipMode.All)
                }
            }
            when (AppConfig.uiMode) {
                2 -> {
                    Material3AppTheme {
                        NavDisplay(
                            backStack = viewModel.backStack,
                            onBack = { navigator.pop() },
                            transition = transition,
                            effects = effects,
                        ) {
                            entry<Route.Home> {
                                Material3HomeScreen()
                            }
                            entry<Route.Login> {
                                Material3LoginScreen()
                            }
                            entry<Route.Settings> {
                                Material3SettingScreen()
                            }
                            entry<Route.OtherSchedule> {
                                Material3OtherScheduleScreen()
                            }
                            entry<Route.WebView>{route ->
                                Material3WebViewScreen(url = route.url, route.title)
                            }
                            entry<Route.WebViewWithData>{route ->
                                Material3WebViewWithDataScreen(html = route.html, route.title)
                            }
                            entry<Route.SunshineMenu>{
                                Material3SunshineContainerScreen()
                            }
                            entry<Route.SunshineList>{route ->
                                Material3SunshineListScreen(typeCode = route.typeCode, submitUrl = route.submitUrl)
                            }
                            entry<Route.SunshineDetail>{route ->
                                Material3SunshineDetailScreen(route.item)
                            }
                            entry<Route.LostAndFoundList>{
                                Material3LostAndFoundListScreen()
                            }
                            entry<Route.LostAndFoundDetail>{route ->
                                Material3LostAndFoundDetailScreen(route.item)
                            }
                            entry<Route.SimpleHtml>{route ->
                                Material3SimpleHtmlScreen(html = route.html, route.title)
                            }
                            entry<Route.OssLicense>{
                                Material3OssLicenseScreen()
                            }
                            entry<Route.OssLicenseDetail>{route ->
                                Material3OssLicenseDetailScreen(route.license)
                            }
                            entry<Route.EmptyRoom> {
                                Material3EmptyRoomScreen()
                            }
                            entry<Route.Exam> {
                                Material3ExamScreen()
                            }
                            entry<Route.ExamScore> {
                                Material3ExamScoreScreen()
                            }
                            entry<Route.Plan> {
                                Material3PlanScreen()
                            }
                            entry<Route.EvaluationMenu> {
                                Material3EvaluationContainerScreen()
                            }
                            entry<Route.EvaluationList> { route ->
                                Material3EvaluationListScreen(route.actionUrl, route.title)
                            }
                            entry<Route.EvaluationDetail> { route ->
                                Material3EvaluationDetailScreen(route.actionUrl, route.title)
                            }
                        }
                    }
                }
                else -> {
                    val controller = remember(AppConfig.colorSchemeMode) {
                    when (AppConfig.colorSchemeMode) {
                            ColorSchemeMode.Light -> ThemeController(MiuixColorSchemeMode.Light)
                            ColorSchemeMode.Dark -> ThemeController(MiuixColorSchemeMode.Dark)
                            else -> ThemeController(MiuixColorSchemeMode.System)
                        }
                    }
                    MiuixTheme(controller = controller){
                        NavDisplay(
                            backStack = viewModel.backStack,
                            onBack = { navigator.pop() },
                            transition = transition,
                            effects = effects,
                        ) {
                            entry<Route.Home> {
                                MiuixHomeScreen()
                            }
                            entry<Route.Login> {
                                MiuixLoginScreen()
                            }
                            entry<Route.Settings> {
                                MiuixSettingScreen()
                            }
                            entry<Route.OtherSchedule> {
                                MiuixOtherScheduleScreen()
                            }
                            entry<Route.WebView>{route ->
                                WebViewScreen(url = route.url, route.title)
                            }
                            entry<Route.WebViewWithData>{route ->
                                WebViewWithDataScreen(html = route.html, route.title)
                            }
                            entry<Route.SunshineMenu>{
                                SunshineContainerScreen()
                            }
                            entry<Route.SunshineList>{route ->
                                SunshineListScreen(typeCode = route.typeCode, submitUrl = route.submitUrl)
                            }
                            entry<Route.SunshineDetail>{route ->
                                SunshineDetailScreen(route.item)
                            }
                            entry<Route.LostAndFoundList>{
                                LostAndFoundListScreen()
                            }
                            entry<Route.LostAndFoundDetail>{route ->
                                LostAndFoundDetailScreen(route.item)
                            }
                            entry<Route.SimpleHtml>{route ->
                                SimpleHtmlScreen(html = route.html, route.title)
                            }
                            entry<Route.OssLicense>{
                                OssLicenseScreen()
                            }
                            entry<Route.OssLicenseDetail>{route ->
                                OssLicenseDetailScreen(route.license)
                            }
                            entry<Route.EmptyRoom> {
                                MiuixEmptyRoomScreen()
                            }
                            entry<Route.Exam> {
                                MiuixExamScreen()
                            }
                            entry<Route.ExamScore> {
                                MiuixExamScoreScreen()
                            }
                            entry<Route.Plan> {
                                MiuixPlanScreen()
                            }
                            entry<Route.EvaluationMenu> {
                                EvaluationContainerScreen()
                            }
                            entry<Route.EvaluationList> { route ->
                                MiuixEvaluationListScreen(route.actionUrl, route.title)
                            }
                            entry<Route.EvaluationDetail> { route ->
                                MiuixEvaluationDetailScreen(route.actionUrl, route.title)
                            }
                        }
                    }
                }
            }
        }
    }
}
