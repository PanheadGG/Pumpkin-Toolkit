package com.pgigi.pumpkintoolkit

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val appVersion: String
        get() {
        val verName = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""
        val verCode = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String ?: ""
        return "$verName ($verCode)"
    }
}

actual fun getPlatform(): Platform = IOSPlatform()