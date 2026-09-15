package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.WidgetData

expect object WidgetDataStore {

    fun save(data: WidgetData)

    fun load(): WidgetData?

    /**
     * 清除小组件课表数据（调试用）
     */
    fun clearWidgetData()
}

expect fun reloadWidgetTimelines()
