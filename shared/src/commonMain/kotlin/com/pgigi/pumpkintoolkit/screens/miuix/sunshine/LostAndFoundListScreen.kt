package com.pgigi.pumpkintoolkit.screens.miuix.sunshine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.constants.Texts
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.LostAndFoundListViewModel
import com.pgigi.pumpkintoolkit.utils.SunshineClient
import com.pgigi.pumpkintoolkit.utils.toLocalDateTime
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LostAndFoundListScreen(viewModel: LostAndFoundListViewModel = viewModel(factory = LostAndFoundListViewModel.Factory)) {
    val navigator = LocalNavigator.current
    val client = SunshineClient
    var loading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = viewModel.firstVisibleItemScrollOffset
    )

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // 判断是否滑到底部
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // 当最后一个可见项是最后一项，或已显示所有项时
            lastVisibleItem >= totalItems - 1 && totalItems > 0
        }
    }

    // 监听滚动变化并保存到 ViewModel
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

    // 监听到底部状态变化
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !loading && !viewModel.listEnded) {
            getList()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "列表",
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.pop()
                    }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navigator.push(Route.WebView("http://usc.tabbycms.com/column/swxxfb/index.shtml"))
                    }) {
                        Icon(MiuixIcons.Add, contentDescription = "提交")
                    }
                },
            )
        },
    ) { paddingValues ->
        val cardPadding = PaddingValues(12.dp, 6.dp)

        PullToRefresh(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            isRefreshing = isRefreshing,
            refreshTexts = Texts.REFRESH_TEXTS,
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
        ) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                    items(viewModel.list.size) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            onClick = {
                                navigator.push(Route.LostAndFoundDetail(viewModel.list[it]))
                            }
                        ) {
                            BasicComponent {
                                Text(
                                    text = viewModel.list[it].lostTime.toLocalDateTime().toString()
                                        .replace("T", " ").replace("Z", ""),
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = htmlToAnnotatedString(viewModel.list[it].propertyName),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = "遗失地点: ${viewModel.list[it].lostPlace}",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    item {
                        if (loading) {
                            InfiniteProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(cardPadding)
                            )
                        }
                        if (viewModel.listEnded) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .align(Alignment.CenterVertically)
                                )
                                Text(
                                    text = "到底了",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(cardPadding),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
                SearchBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(cardPadding),
                    inputField = {
                        InputField(
                            query = viewModel.searchKey,
                            onQueryChange = { viewModel.searchKey = it },
                            onSearch = { doSearch() },
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {}
            }
        }
    }
}