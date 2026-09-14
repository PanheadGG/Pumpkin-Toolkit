package com.pgigi.pumpkintoolkit.screens.miuix.evaluation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.utils.QZClient
import kotlinx.coroutines.launch
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationDetail
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixEvaluationDetailScreen(
    actionUrl: String,
    title: String = "评教详情"
) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<EvaluationDetail?>(null) }
    val selectedValues = remember { mutableStateMapOf<String, String>() }
    var comment by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionUrl) {
        loading = true
        val result = QZClient.getEvaluationDetail(actionUrl)
        if (result != null) {
            detail = result
            selectedValues.clear()
            result.evaluationList.forEach { item ->
                selectedValues[item.evaluationId] =
                    result.keyValueMap["pj0601id_${item.evaluationId}"] ?: ""
            }
            comment = result.comment
        }
        loading = false
    }

    var submittedSuccessfully by remember { mutableStateOf(false) }

    val submit: () -> Unit = {
        val d = detail
        if (!submitting && !loading && d != null) {
            coroutineScope.launch {
                submitting = true
                val msg = QZClient.submitEvaluation(d, selectedValues.toMap(), comment)
                submitting = false
                resultMessage = msg ?: "网络错误，提交失败"
                if (msg != null && msg.contains("成功")) {
                    submittedSuccessfully = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        text = if (submitting) "提交中..." else "提交",
                        onClick = { submit() }
                    )
                }
            )
        }
    ) { paddingValues ->
        val cardPadding = PaddingValues(12.dp, 6.dp)
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val d = detail
                if (d != null) {
                    items(d.evaluationList.size) { index ->
                        val item = d.evaluationList[index]
                        Card(modifier = Modifier.fillMaxWidth().padding(cardPadding)) {
                            Text(
                                text = item.title,
                                modifier = Modifier.padding(16.dp, 12.dp)
                            )
                            val sortedOptions = item.scoreKeyValueMap.entries
                                .sortedByDescending { it.value }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                sortedOptions.forEach { (key, score) ->
                                    val isSelected = selectedValues[item.evaluationId] == key
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Checkbox(
                                            state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                                            onClick = {
                                                if (isSelected) {
                                                    selectedValues[item.evaluationId] = ""
                                                } else {
                                                    selectedValues[item.evaluationId] = key
                                                }
                                            }
                                        )
                                        Text(
                                            text = formatScore(score),
                                            fontSize = 14.sp,
                                            color = if (isSelected)
                                                MiuixTheme.colorScheme.primary
                                            else
                                                MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(cardPadding)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "评语",
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                BasicTextField(
                                    value = comment,
                                    onValueChange = { comment = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp),
                                    textStyle = TextStyle(
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    ),
                                    singleLine = false,
                                    cursorBrush = SolidColor(MiuixTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                } else if (!loading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "加载失败",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }
            if (loading || submitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    InfiniteProgressIndicator()
                }
            }
        }
    }
    resultMessage?.let { msg ->
        WindowDialog(
            show = resultMessage != null,
            title = "提示",
            summary = msg,
            onDismissRequest = { resultMessage = null }
        ) {
            val dismiss = LocalDismissState.current
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                text = "确定",
                onClick = {
                    dismiss?.invoke()
                    if (submittedSuccessfully) {
                        navigator.setResult("evaluation_refresh", true)
                    }
                }
            )
        }
    }
}

private fun formatScore(score: Float): String {
    return if (score % 1 == 0f) score.toInt().toString() else score.toString()
}
