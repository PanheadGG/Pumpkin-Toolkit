package com.pgigi.pumpkintoolkit

import android.os.Build
import com.pgigi.pumpkintoolkit.utils.KVaultContextHolder

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val appVersion: String
        get(){
        val context = KVaultContextHolder.context
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val verName = info.versionName ?: ""
        val verCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString() else info.versionCode.toString()
        return "$verName ($verCode)"
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()