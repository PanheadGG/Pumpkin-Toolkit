package com.pgigi.pumpkintoolkit.screens.miuix.sunshine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.LocalNavigator
import com.pgigi.pumpkintoolkit.Navigator
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.animation.PredictiveBackAnimation
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshinePane
import com.pgigi.pumpkintoolkit.viewmodel.sunshine.SunshinePaneViewModel
import kotlinx.coroutines.flow.SharedFlow
import top.yukonga.miuix.kmp.nav.core.NavKey

@Composable
fun SunshineContainerScreen(
    viewModel: SunshinePaneViewModel = viewModel(factory = SunshinePaneViewModel.Factory)
) {
    // Predictive back enabled: just show Menu, sub-navigation via nav push
    if (AppConfig.predictiveBackAnimation != PredictiveBackAnimation.None) {
        SunshineMenuScreen()
        return
    }

    // Multi-pane mode (predictive back disabled)
    val realNavigator = LocalNavigator.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxPanes = when {
            maxWidth >= 1200.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        DisposableEffect(Unit) {
            val previous = realNavigator.onBackIntercept
            lateinit var intercept: () -> Boolean
            intercept = {
                if (viewModel.pop()) {
                    true
                } else {
                    realNavigator.onBackIntercept = null
                    realNavigator.pop()
                    realNavigator.onBackIntercept = intercept
                    true
                }
            }
            realNavigator.onBackIntercept = intercept
            onDispose { realNavigator.onBackIntercept = previous }
        }

        val allPanes = mutableListOf<SunshinePane>(SunshinePane.Menu)
        viewModel.b?.let { allPanes.add(it) }
        viewModel.c?.let { allPanes.add(it) }

        val visibleStart = (allPanes.size - maxPanes).coerceAtLeast(0)
        val visiblePanes = allPanes.subList(visibleStart, allPanes.size)

        val paneNavigator = PaneNavigator(realNavigator, viewModel)

        CompositionLocalProvider(LocalNavigator provides paneNavigator) {
            if (maxPanes > 1) {
                Row(modifier = Modifier.fillMaxSize()) {
                    visiblePanes.forEach { pane ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            RenderPane(pane)
                        }
                    }
                }
            } else {
                RenderPane(visiblePanes.last())
            }
        }
    }
}

@Composable
private fun RenderPane(pane: SunshinePane) {
    when (pane) {
        is SunshinePane.Menu -> SunshineMenuScreen()
        is SunshinePane.List -> SunshineListScreen(typeCode = pane.typeCode, submitUrl = pane.submitUrl)
        is SunshinePane.Detail -> SunshineDetailScreen(item = pane.item)
        is SunshinePane.LostAndFoundList -> LostAndFoundListScreen()
        is SunshinePane.LostAndFoundDetail -> LostAndFoundDetailScreen(item = pane.item)
    }
}

private class PaneNavigator(
    private val realNavigator: Navigator,
    private val vm: SunshinePaneViewModel,
) : Navigator(realNavigator.backStack) {

    override fun push(key: NavKey) {
        if (key is Route && vm.applyRoute(key)) return
        realNavigator.push(key)
    }

    override fun pop() {
        if (!vm.pop()) realNavigator.pop()
    }

    override fun replace(key: NavKey) {
        if (key is Route && vm.applyRoute(key)) return
        realNavigator.replace(key)
    }

    override fun popUntil(predicate: (NavKey) -> Boolean) {
        val curC = vm.c
        if (curC != null && !predicate(toRoute(curC))) vm.pop()
        val curB = vm.b
        if (curB != null && !predicate(toRoute(curB))) vm.pop()
    }

    override fun navigateForResult(route: Route, requestKey: String) {
        if (vm.applyRoute(route)) return
        realNavigator.navigateForResult(route, requestKey)
    }

    override fun <T : Any> setResult(requestKey: String, value: T) {
        realNavigator.emitResult(requestKey, value)
        vm.pop()
    }

    override fun <T : Any> observeResult(requestKey: String): SharedFlow<T> =
        realNavigator.observeResult(requestKey)

    override fun clearResult(requestKey: String) = realNavigator.clearResult(requestKey)

    private fun toRoute(pane: SunshinePane): Route = when (pane) {
        is SunshinePane.Menu -> Route.SunshineMenu
        is SunshinePane.List -> Route.SunshineList(pane.typeCode, pane.submitUrl)
        is SunshinePane.Detail -> Route.SunshineDetail(pane.item)
        is SunshinePane.LostAndFoundList -> Route.LostAndFoundList
        is SunshinePane.LostAndFoundDetail -> Route.LostAndFoundDetail(pane.item)
    }
}