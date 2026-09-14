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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.components.material3.M3Card
import com.pgigi.pumpkintoolkit.constants.Texts
import com.pgigi.pumpkintoolkit.utils.SunshineClient
import com.pgigi.pumpkintoolkit.utils.toLocalDateTime
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.LostAndFoundListViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3LostAndFoundListScreen(
    viewModel: LostAndFoundListViewModel = viewModel(factory = LostAndFoundListViewModel.Factory)
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
        initialFirstVisibleItemIndex = viewModel.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = viewModel.firstVisibleItemScrollOffset
    )

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
            viewModel.firstVisibleItemIndex = index
            viewModel.firstVisibleItemScrollOffset = offset
        }
    }

    suspend fun getList() {
        loading = true
        client.getLostAndFoundList(pageIndex = viewModel.pageIndex, searchKey = viewModel.searchKey)
            ?.let {
                viewModel.list.addAll(it.list)
                if (it.total <= viewModel.list.size) viewModel.listEnded = true
                viewModel.pageIndex++
            }
        loading = false
        isRefreshing = false
    }

    fun doSearch() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        focusManager.clearFocus()
        viewModel.listEnded = false
        loading = false
        viewModel.list.clear()
        viewModel.pageIndex = 1
        coroutineScope.launch {
            listState.scrollToItem(0)
            getList()
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !loading && !viewModel.listEnded) {
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
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.push(Route.WebView("http://usc.tabbycms.com/column/swxxfb/index.shtml"))
                    }) {
                        Icon(MiuixIcons.Add, contentDescription = "提交")
                    }
                }
            )
        }
    ) { paddingValues ->

        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.listEnded = false
                loading = false
                viewModel.list.clear()
                viewModel.pageIndex = 1
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
                    items(viewModel.list.size) {
                        val listItem = viewModel.list[it]
                        M3Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = {
                                navigator.push(Route.LostAndFoundDetail(listItem))
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = listItem.lostTime.toLocalDateTime().toString()
                                        .replace("T", " ").replace("Z", ""),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = htmlToAnnotatedString(listItem.propertyName),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "遗失地点: ${listItem.lostPlace}",
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
                        if (viewModel.listEnded) {
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

                OutlinedTextField(
                    value = viewModel.searchKey,
                    onValueChange = { viewModel.searchKey = it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { doSearch() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { doSearch() }) {
                            Icon(MiuixIcons.ListView, contentDescription = "搜索")
                        }
                    }
                )
            }
        }
    }
}
