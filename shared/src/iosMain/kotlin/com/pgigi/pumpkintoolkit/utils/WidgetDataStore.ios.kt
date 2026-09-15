package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.WidgetData
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDevice

const val WIDGET_DATA_CHANGED_NOTIFICATION = "com.pgigi.pumpkintoolkit.widgetDataChanged"

private const val API_BASE_URL = "https://pumpkin-api.pgigi.com"

actual object WidgetDataStore {

    private fun idfv(): String {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: ""
    }

    actual fun save(data: WidgetData) {
        val json = JsonUtil.toJson(data, WidgetData.serializer())
        val deviceId = idfv()
        if (deviceId.isEmpty()) return

        // 云端 KV 存储（异步，不阻塞主流程）
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            try {
                val client = HttpClient()
                client.post("$API_BASE_URL/put/$deviceId") {
                    contentType(ContentType.Application.Json)
                    setBody(json)
                }
                client.close()
            } catch (_: Exception) { }
        }
    }

    actual fun load(): WidgetData? {
        // iOS 端不本地缓存，小组件自行从云端获取
        return null
    }

    /**
     * 清除小组件课表数据（调试用）
     * 云端数据无法直接删除
     */
    actual fun clearWidgetData() {
        // 无本地数据可清
    }
}

actual fun reloadWidgetTimelines() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        WIDGET_DATA_CHANGED_NOTIFICATION,
        `object` = null
    )
}