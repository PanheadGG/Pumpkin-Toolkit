package com.pgigi.pumpkintoolkit.screens.material3.sunshine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.material3.M3Card
import com.pgigi.pumpkintoolkit.constants.Texts
import com.pgigi.pumpkintoolkit.utils.SunshineClient
import com.pgigi.pumpkintoolkit.utils.toLocalDateTime
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshineListItem
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshineListViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3SunshineListScreen(
    typeCode: String = "",
    submitUrl: String? = null,
    viewModel: SunshineListViewModel = viewModel(factory = SunshineListViewModel.Factory)
) {
    val navigator = LocalNavigator.current
    val client = SunshineClient
    val hapticFeedback = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemScrollOffset
    )

    viewModel.map.getOrPut(typeCode) { SunshineListItem() }.typeCode = typeCode

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 1 && totalItems > 0
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemIndex = index
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemScrollOffset =
                offset
        }
    }

    suspend fun getList() {
        loading = true
        client.getGuestBookList(
            pageIndex = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex,
            searchKey = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.searchKey,
            typeCode = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.typeCode
        )?.let {
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list.addAll(it.list)
            if (it.totalPage < viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex)
                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded = true
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex++
        }
        loading = false
        isRefreshing = false
    }

    fun doSearch(item: SunshineListItem) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        focusManager.clearFocus()
        item.listEnded = false
        loading = false
        item.list.clear()
        item.pageIndex = 1
        coroutineScope.launch {
            listState.scrollToItem(0)
            getList()
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !loading && !viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded) {
            getList()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("列表") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigator.pop()
                    }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    submitUrl?.let {
                        IconButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.push(Route.WebView(submitUrl))
                        }) {
                            Icon(MiuixIcons.Add, contentDescription = "提交")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded = false
                loading = false
                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list.clear()
                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex = 1
                coroutineScope.launch {
                    listState.scrollToItem(0)
                    getList()
                }
            },
            pullToRefreshState = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            refreshTexts = Texts.REFRESH_TEXTS,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                    items(viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list.size) {
                        val listItem =
                            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it]
                        M3Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = {
                                navigator.push(Route.SunshineDetail(listItem))
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "流水号: ${listItem.id}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when (listItem.currentStatus) {
                                            "1" -> "已转交"
                                            "9" -> "已处理"
                                            else -> "未知"
                                        },
                                        fontSize = 12.sp,
                                        color = when (listItem.currentStatus) {
                                            "1" -> MaterialTheme.colorScheme.error
                                            "9" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Text(
                                    text = htmlToAnnotatedString(listItem.title),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listItem.addDate.toLocalDateTime().toString()
                                        .replace("T", " ").replace("Z", ""),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        if (loading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        if (viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                )
                                Text(
                                    text = "到底了",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
                val item = viewModel.map.getOrPut(typeCode) { SunshineListItem() }
                OutlinedTextField(
                    value = item.searchKey,
                    onValueChange = { item.searchKey = it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { doSearch(item) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { doSearch(item) }) {
                            Icon(MiuixIcons.Search, contentDescription = "搜索")
                        }
                    }
                )
            }

        }
    }
}
