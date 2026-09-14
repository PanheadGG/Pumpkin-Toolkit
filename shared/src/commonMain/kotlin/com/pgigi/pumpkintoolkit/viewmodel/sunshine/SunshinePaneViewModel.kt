package com.pgigi.pumpkintoolkit.viewmodel.sunshine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pgigi.pumpkintoolkit.Route
import com.pgigi.pumpkintoolkit.models.sunshine.GuestBookItem
import com.pgigi.pumpkintoolkit.models.sunshine.LostItem

/**
 * Pane types for the sunshine parallel window.
 */
sealed interface SunshinePane {
    data object Menu : SunshinePane
    data class List(val typeCode: String = "", val submitUrl: String? = null) : SunshinePane
    data class Detail(val item: GuestBookItem) : SunshinePane
    data object LostAndFoundList : SunshinePane
    data class LostAndFoundDetail(val item: LostItem) : SunshinePane
}

/**
 * ViewModel that holds the fixed-slot pane state for sunshine.
 * Survives recomposition and Composable tree restructuring.
 */
class SunshinePaneViewModel : ViewModel() {
    var b: SunshinePane? by mutableStateOf(null)
        private set

    var c: SunshinePane? by mutableStateOf(null)
        private set

    fun applyRoute(route: Route): Boolean {
        return when (route) {
            is Route.SunshineMenu -> true
            is Route.SunshineList -> {
                b = SunshinePane.List(route.typeCode, route.submitUrl)
                c = null
                true
            }
            is Route.LostAndFoundList -> {
                b = SunshinePane.LostAndFoundList
                c = null
                true
            }
            is Route.SunshineDetail -> {
                c = SunshinePane.Detail(route.item)
                true
            }
            is Route.LostAndFoundDetail -> {
                c = SunshinePane.LostAndFoundDetail(route.item)
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

    /** Rightmost maxPanes from [Menu, B?, C?]. */
    fun visiblePanes(maxPanes: Int): List<SunshinePane> {
        val all = mutableListOf<SunshinePane>(SunshinePane.Menu)
        b?.let { all.add(it) }
        c?.let { all.add(it) }
        val start = (all.size - maxPanes).coerceAtLeast(0)
        return all.subList(start, all.size)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SunshinePaneViewModel() }
        }
    }
}