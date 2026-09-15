package com.pgigi.pumpkintoolkit.utils

import com.liftric.kvault.KVault

/**
 * iOS 平台创建 KVault 实例
 * 使用 Keychain 实现，serviceName 为应用标识
 */
actual fun createKVault(): KVault {
    return KVault(serviceName = "com.pgigi.pumpkintoolkit", accessGroup = null)
}

/**
 * 小组件数据在 Keychain 中的 serviceName
 * 读写两端必须使用相同的 serviceName + accessGroup 组合才能匹配
 */
private const val WIDGET_KEYCHAIN_SERVICE = "com.pgigi.pumpkintoolkit.widget"

/**
 * Keychain Access Group（不含 Team ID 前缀）
 * entitlements 中声明为 $(AppIdentifierPrefix)com.pgigi.pumpkintoolkit.keychain.shared
 */
private const val SHARED_KEYCHAIN_GROUP = "R6UB6VD355.com.pgigi.pumpkintoolkit.keychain.shared"

/**
 * 创建用于小组件数据共享的 KVault 实例
 * 使用 Keychain Access Group 实现跨 target 数据共享
 */
fun createWidgetKVault(): KVault {
    return KVault(
        serviceName = WIDGET_KEYCHAIN_SERVICE,
        accessGroup = SHARED_KEYCHAIN_GROUP
    )
}
