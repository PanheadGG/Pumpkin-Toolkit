package com.pgigi.pumpkintoolkit.screens.material3.evaluation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.components.material3.M3Card
import com.pgigi.pumpkintoolkit.utils.QZClient
import kotlinx.coroutines.launch
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationDetail
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3EvaluationDetailScreen(
    actionUrl: String,
    title: String = "评教详情"
) {
    val navigator = LocalNavigator.current
    val hapticFeedback = LocalHapticFeedback.current
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { submit() },
                        enabled = !submitting && !loading && detail != null
                    ) {
                        Text(if (submitting) "提交中..." else "提交")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val d = detail
                if (d != null) {
                    items(d.evaluationList.size) { index ->
                        val item = d.evaluationList[index]
                        M3Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                        modifier = Modifier.clickable {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedValues[item.evaluationId] = key
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedValues[item.evaluationId] = key
                                            }
                                        )
                                        Text(
                                            text = formatScore(score),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("评语") },
                            placeholder = { Text("请输入评语...") },
                            singleLine = false
                        )
                    }
                } else if (!loading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "加载失败",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            if (loading || submitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    resultMessage = null
                    if (submittedSuccessfully) {
                        navigator.setResult("evaluation_refresh", true)
                    }
                }) {
                    Text("确定")
                }
            }
        )
    }
}

private fun formatScore(score: Float): String {
    return if (score % 1 == 0f) score.toInt().toString() else score.toString()
}
