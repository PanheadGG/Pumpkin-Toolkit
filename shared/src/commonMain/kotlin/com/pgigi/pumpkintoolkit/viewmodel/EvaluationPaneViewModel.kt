package com.pgigi.pumpkintoolkit.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pgigi.pumpkintoolkit.Route

/**
 * Pane types for the evaluation parallel window.
 */
sealed interface EvaluationPane {
    data object Menu : EvaluationPane
    data class List(val actionUrl: String, val title: String = "评教列表") : EvaluationPane
    data class Detail(val actionUrl: String, val title: String = "评教详情") : EvaluationPane
}

/**
 * ViewModel that holds the fixed-slot pane state for evaluation.
 * Survives recomposition and Composable tree restructuring.
 */
class EvaluationPaneViewModel : ViewModel() {
    var b: EvaluationPane? by mutableStateOf(null)
        private set

    var c: EvaluationPane? by mutableStateOf(null)
        private set

    fun applyRoute(route: Route): Boolean {
        return when (route) {
            is Route.EvaluationMenu -> true
            is Route.EvaluationList -> {
                b = EvaluationPane.List(route.actionUrl, route.title)
                c = null
                true
            }
            is Route.EvaluationDetail -> {
                c = EvaluationPane.Detail(route.actionUrl, route.title)
                true
            }
            else -> false
        }
    }

    fun pop(): Boolean {
        val curC = c
        if (curC != null) {
            c = null
            return true
        }
        val curB = b
        if (curB != null) {
            b = null
            return true
        }
        return false
    }

    fun visiblePanes(maxPanes: Int): List<EvaluationPane> {
        val all = mutableListOf<EvaluationPane>(EvaluationPane.Menu)
        b?.let { all.add(it) }
        c?.let { all.add(it) }
        val start = (all.size - maxPanes).coerceAtLeast(0)
        return all.subList(start, all.size)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EvaluationPaneViewModel() }
        }
    }
}