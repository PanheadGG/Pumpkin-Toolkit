package com.pgigi.pumpkintoolkit.screens.material3.evaluation

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
import com.pgigi.pumpkintoolkit.viewmodel.EvaluationPane
import com.pgigi.pumpkintoolkit.viewmodel.EvaluationPaneViewModel
import kotlinx.coroutines.flow.SharedFlow
import top.yukonga.miuix.kmp.nav.core.NavKey

@Composable
fun Material3EvaluationContainerScreen(
    viewModel: EvaluationPaneViewModel = viewModel(factory = EvaluationPaneViewModel.Factory)
) {
    if (AppConfig.predictiveBackAnimation != PredictiveBackAnimation.None) {
        Material3EvaluationMenuScreen()
        return
    }

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

        val allPanes = mutableListOf<EvaluationPane>(EvaluationPane.Menu)
        viewModel.b?.let { allPanes.add(it) }
        viewModel.c?.let { allPanes.add(it) }

        val visibleStart = (allPanes.size - maxPanes).coerceAtLeast(0)
        val visiblePanes = allPanes.subList(visibleStart, allPanes.size)

        val paneNavigator = M3EvalPaneNavigator(realNavigator, viewModel)

        CompositionLocalProvider(LocalNavigator provides paneNavigator) {
            if (maxPanes > 1) {
                Row(modifier = Modifier.fillMaxSize()) {
                    visiblePanes.forEach { pane ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (pane) {
                                is EvaluationPane.Menu -> Material3EvaluationMenuScreen()
                                is EvaluationPane.List -> Material3EvaluationListScreen(actionUrl = pane.actionUrl, title = pane.title)
                                is EvaluationPane.Detail -> Material3EvaluationDetailScreen(actionUrl = pane.actionUrl, title = pane.title)
                            }
                        }
                    }
                }
            } else {
                when (val pane = visiblePanes.last()) {
                    is EvaluationPane.Menu -> Material3EvaluationMenuScreen()
                    is EvaluationPane.List -> Material3EvaluationListScreen(actionUrl = pane.actionUrl, title = pane.title)
                    is EvaluationPane.Detail -> Material3EvaluationDetailScreen(actionUrl = pane.actionUrl, title = pane.title)
                }
            }
        }
    }
}

private class M3EvalPaneNavigator(
    private val realNavigator: Navigator,
    private val vm: EvaluationPaneViewModel,
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

    private fun toRoute(pane: EvaluationPane): Route = when (pane) {
        is EvaluationPane.Menu -> Route.EvaluationMenu
        is EvaluationPane.List -> Route.EvaluationList(pane.actionUrl, pane.title)
        is EvaluationPane.Detail -> Route.EvaluationDetail(pane.actionUrl, pane.title)
    }
}