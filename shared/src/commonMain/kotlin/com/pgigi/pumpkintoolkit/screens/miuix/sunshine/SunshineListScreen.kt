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
import com.pgigi.pumpkintoolkit.utils.SunshineClient
import com.pgigi.pumpkintoolkit.utils.toLocalDateTime
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshineListItem
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshineListViewModel
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
fun SunshineListScreen(
    typeCode: String = "",
    submitUrl: String? = null,
    viewModel: SunshineListViewModel = viewModel(factory = SunshineListViewModel.Factory)
) {
    val navigator = LocalNavigator.current
    val client = SunshineClient
    var loading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.firstVisibleItemScrollOffset
    )

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    viewModel.map.getOrPut(typeCode) { SunshineListItem() }.typeCode = typeCode

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
//            Log.i("TAG", "searchKey: ${viewModel.map.getOrPut(typeCode) { SunshineListItem() }.searchKey}")
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list.addAll(it.list)
            if (it.totalPage < viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex)
                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded = true
            viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex++
        }
        loading = false
        isRefreshing = false
    }

    fun doSearch() {
        focusManager.clearFocus()
        viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded = false
        loading = false
        viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list.clear()
        viewModel.map.getOrPut(typeCode) { SunshineListItem() }.pageIndex = 1
        coroutineScope.launch {
            listState.scrollToItem(0)
            getList()
        }
    }

    // 监听到底部状态变化
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !loading && !viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded) {
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
                    submitUrl?.let {
                        IconButton(onClick = {
                            navigator.push(Route.WebView(submitUrl))
                        }) {
                            Icon(MiuixIcons.Add, contentDescription = "提交")
                        }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            onClick = {
                                navigator.push(
                                    Route.SunshineDetail(
                                        viewModel.map.getOrPut(
                                            typeCode
                                        ) { SunshineListItem() }.list[it]
                                    )
                                )
                            }
                        ) {
                            BasicComponent(
                                endActions = {
                                    Text(
                                        text = when (viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it].currentStatus) {
                                            "1" -> "已转交"
                                            "9" -> "已处理"
                                            else -> "未知"
                                        },
                                        fontSize = 12.sp,
                                        color = when (viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it].currentStatus) {
                                            "1" -> MiuixTheme.colorScheme.error
                                            "9" -> MiuixTheme.colorScheme.primary
                                            else -> MiuixTheme.colorScheme.onSurfaceContainer
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = "流水号: ${viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it].id}",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = htmlToAnnotatedString(viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it].title),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.list[it].addDate.toLocalDateTime()
                                        .toString()
                                        .replace("T", " ").replace("Z", ""),
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
                        if (viewModel.map.getOrPut(typeCode) { SunshineListItem() }.listEnded) {
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
                            query = viewModel.map.getOrPut(typeCode) { SunshineListItem() }.searchKey,
                            onQueryChange = {
                                viewModel.map.getOrPut(typeCode) { SunshineListItem() }.searchKey =
                                    it
                            },
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