package com.pgigi.pumpkintoolkit.components.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun BoxScope.MiuixFloatingDropdown(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop? = null,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showPopup by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .then(
                    backdrop?.let { bd ->
                        Modifier.drawBackdrop(
                            backdrop = bd,
                            shape = { CircleShape },
                            effects = { blur(25.dp.toPx(), 25.dp.toPx()) },
                            onDrawSurface = { drawRect(containerColor.copy(alpha = 0.65f)) },
                        )
                    } ?: Modifier.background(containerColor)
                )
                .clickable {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showPopup = true
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label)
        }

        WindowListPopup(
            show = showPopup,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showPopup = false }
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                items.forEachIndexed { index, item ->
                    DropdownImpl(
                        text = item,
                        optionSize = items.size,
                        isSelected = selectedIndex == index,
                        onSelectedIndexChange = {
                            onSelect(index)
                            dismiss?.invoke()
                        },
                        index = index
                    )
                }
            }
        }
    }
}
