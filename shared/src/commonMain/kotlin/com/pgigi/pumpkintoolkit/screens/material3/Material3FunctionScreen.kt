package com.pgigi.pumpkintoolkit.screens.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.material3.M3GroupSection
import com.pgigi.pumpkintoolkit.components.material3.M3Row
import com.pgigi.pumpkintoolkit.utils.QZClient
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit

private data class FunctionGridItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3FunctionScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("功能") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 账号 - 始终列表模式
            M3GroupSection {
                M3Row(
                    title = "教务系统账号",
                    summary = AppConfig.username.ifEmpty { "未登录" },
                    icon = MiuixIcons.Contacts,
                    onClick = { navigator.push(Route.Login) },
                    showDivider = false
                )
                AnimatedVisibility(
                    AppConfig.username.isNotBlank() &&
                            AppConfig.password.isNotBlank()
                ) {
                    M3Row(
                        title = if (loading) "正在刷新登录状态..." else "点击刷新登录状态",
                        icon = MiuixIcons.Refresh,
                        enabled = !loading,
                        onClick = {
                            loading = true
                            coroutineScope.launch {
                                QZClient.login(
                                    onSuccess = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "登录成功",
                                                withDismissAction = true
                                            )
                                        }
                                        loading = false
                                    },
                                    onFailure = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = it,
                                                withDismissAction = true
                                            )
                                        }
                                        loading = false
                                    }
                                )
                            }
                        },
                        trailingContent = {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        showDivider = false
                    )
                }
            }

            // 设置 - 始终列表模式
            M3GroupSection {
                M3Row(
                    title = "设置",
                    icon = MiuixIcons.Settings,
                    onClick = { navigator.push(Route.Settings) },
                    showDivider = false
                )
            }

            // 功能 - 平铺模式或列表模式
            if (AppConfig.functionDisplayMode == 1) {
                val gridItems = listOf(
                    FunctionGridItem(MiuixIcons.SelectAll, "考试查询") { navigator.push(Route.Exam) },
                    FunctionGridItem(MiuixIcons.File, "成绩查询") { navigator.push(Route.ExamScore) },
                    FunctionGridItem(MiuixIcons.Location, "空教室查询") { navigator.push(Route.EmptyRoom) },
                    FunctionGridItem(MiuixIcons.Notes, "课程执行计划") { navigator.push(Route.Plan) },
                    FunctionGridItem(MiuixIcons.VerticalSplit, "学期课表") { navigator.push(Route.OtherSchedule) },
                    FunctionGridItem(MiuixIcons.Edit, "学生评教") { navigator.push(Route.EvaluationMenu) },
                    FunctionGridItem(MiuixIcons.Background, "第二课堂成绩单") {
                        navigator.push(Route.WebView("https://m1wxluid.yichafen.com/", "第二课堂成绩单"))
                    },
                    FunctionGridItem(MiuixIcons.Backup, "教务系统") {
                        navigator.push(Route.WebView(QZClient.loginRedirectUrl.ifEmpty { AppConfig.serverUrl }, "教务系统"))
                    },
                    FunctionGridItem(MiuixIcons.File, "绩点排名成绩单") {
                        navigator.push(Route.WebView("https://ai.usc.edu.cn:9080/gztcyAPP/", "绩点排名成绩单"))
                    },
                    FunctionGridItem(MiuixIcons.Send, "阳光平台") { navigator.push(Route.SunshineMenu) },
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val columns = (maxWidth / 130.dp).toInt().coerceIn(1, 6)
                        val spacing = 8.dp
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            gridItems.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(spacing)
                                ) {
                                    rowItems.forEach { item ->
                                        M3GridItem(
                                            icon = item.icon,
                                            title = item.title,
                                            onClick = item.onClick,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < columns) {
                                        Spacer(Modifier.weight((columns - rowItems.size).toFloat()))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                M3GroupSection {
                    M3Row(
                        title = "考试查询",
                        icon = MiuixIcons.SelectAll,
                        onClick = { navigator.push(Route.Exam) },
                        showDivider = false
                    )
                    M3Row(
                        title = "成绩查询",
                        icon = MiuixIcons.File,
                        onClick = { navigator.push(Route.ExamScore) },
                        showDivider = false
                    )
                    M3Row(
                        title = "空教室查询",
                        icon = MiuixIcons.Location,
                        onClick = { navigator.push(Route.EmptyRoom) },
                        showDivider = false
                    )
                    M3Row(
                        title = "课程执行计划",
                        icon = MiuixIcons.Notes,
                        onClick = { navigator.push(Route.Plan) },
                        showDivider = false
                    )
                    M3Row(
                        title = "其他学期课表",
                        icon = MiuixIcons.VerticalSplit,
                        onClick = { navigator.push(Route.OtherSchedule) },
                        showDivider = false
                    )
                    M3Row(
                        title = "学生评教",
                        icon = MiuixIcons.Edit,
                        onClick = { navigator.push(Route.EvaluationMenu) },
                        showDivider = false
                    )
                    M3Row(
                        title = "第二课堂成绩单",
                        icon = MiuixIcons.Background,
                        onClick = { navigator.push(Route.WebView("https://m1wxluid.yichafen.com/", "第二课堂成绩单")) }
                    )
                }

                M3GroupSection {
                    M3Row(
                        title = "教务系统",
                        icon = MiuixIcons.Backup,
                        onClick = {
                            navigator.push(
                                Route.WebView(
                                    QZClient.loginRedirectUrl.ifEmpty { AppConfig.serverUrl },
                                    "教务系统"
                                )
                            )
                        },
                        showDivider = false
                    )
                    M3Row(
                        title = "绩点排名成绩单",
//                        summary = "与教务系统是两个系统, 可看专业排名成绩单",
                        summary = "南华教务公众号 线上注册及成绩单",
                        icon = MiuixIcons.File,
                        onClick = {
                            navigator.push(Route.WebView("https://ai.usc.edu.cn:9080/gztcyAPP/", "绩点排名成绩单"))
                        },
                        showDivider = false
                    )
                }

                M3GroupSection {
                    M3Row(
                        title = "阳光平台",
                        icon = MiuixIcons.Send,
                        onClick = { navigator.push(Route.SunshineMenu) },
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun M3GridItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clickable(
                enabled = !loading,
                onClick = onClick
            )
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            maxLines = 2,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
