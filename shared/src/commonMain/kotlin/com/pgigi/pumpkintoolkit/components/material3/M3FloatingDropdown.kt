package com.pgigi.pumpkintoolkit.components.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

@Composable
fun BoxScope.M3FloatingDropdown(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

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
                    showMenu = !showMenu
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label)
        }
    }

    AnimatedVisibility(
        visible = showMenu,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showMenu = false
                }
        )
    }

    AnimatedVisibility(
        visible = showMenu,
        enter = scaleIn(initialScale = 0.92f, animationSpec = tween(220)) +
                fadeIn(animationSpec = tween(220)),
        exit = scaleOut(targetScale = 0.92f, animationSpec = tween(180)) +
                fadeOut(animationSpec = tween(180)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 72.dp, start = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 120.dp, max = 280.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 360.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) Modifier.background(
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                                else Modifier
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                                showMenu = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
