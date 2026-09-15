package com.pgigi.pumpkintoolkit.utils

import com.pgigi.pumpkintoolkit.models.WidgetData

const val WIDGET_DATA_FILE = "widget_data.json"

actual object WidgetDataStore {

    actual fun save(data: WidgetData) {
        FileStoreUtils.writeString(
            WIDGET_DATA_FILE,
            JsonUtil.toJson(data, WidgetData.serializer())
        )
    }

    actual fun load(): WidgetData? {
        return FileStoreUtils.readString(WIDGET_DATA_FILE)?.let {
            try {
                JsonUtil.parseJson(it, WidgetData.serializer())
            } catch (_: Exception) { null }
        }
    }

    actual fun clearWidgetData() {
        FileStoreUtils.writeString(WIDGET_DATA_FILE, "")
    }
}

actual fun reloadWidgetTimelines() {
    try {
        val context = KVaultContextHolder.context
        val intent = android.content.Intent()
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE")
        val mgr = android.appwidget.AppWidgetManager.getInstance(context)
        val cn = android.content.ComponentName(
            context,
            "com.pgigi.pumpkintoolkit.widget.TodayScheduleWidgetReceiver"
        )
        val ids = mgr.getAppWidgetIds(cn)
        if (ids.isNotEmpty()) {
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    } catch (_: Exception) { }
}
