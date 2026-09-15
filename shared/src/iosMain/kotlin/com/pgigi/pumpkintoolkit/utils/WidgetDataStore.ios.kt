package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.WidgetData
import platform.Foundation.NSNotificationCenter

const val WIDGET_DATA_CHANGED_NOTIFICATION = "com.pgigi.pumpkintoolkit.widgetDataChanged"

/**
 * Keychain 中存储 widget 课表数据的 key
 * 需与小组件 Swift 端的 KeychainHelper.widgetDataKey 保持一致
 */
private const val KEYCHAIN_WIDGET_DATA_KEY = "widget_schedule_data"

actual object WidgetDataStore {

    actual fun save(data: WidgetData) {
        val json = JsonUtil.toJson(data, WidgetData.serializer())
        createWidgetKVault().set(KEYCHAIN_WIDGET_DATA_KEY, json)
    }

    actual fun load(): WidgetData? {
        val json = createWidgetKVault().string(forKey = KEYCHAIN_WIDGET_DATA_KEY) ?: return null
        return try {
            JsonUtil.parseJson(json, WidgetData.serializer())
        } catch (_: Exception) { null }
    }

    /**
     * 清除 Keychain 中的小组件课表数据（调试用）
     */
    actual fun clearWidgetData() {
        createWidgetKVault().deleteObject(forKey = KEYCHAIN_WIDGET_DATA_KEY)
    }
}

actual fun reloadWidgetTimelines() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        WIDGET_DATA_CHANGED_NOTIFICATION,
        `object` = null
    )
}
