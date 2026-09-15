package com.pgigi.pumpkintoolkit.utils

import com.liftric.kvault.KVault

/**
 * iOS 平台创建 KVault 实例
 * 使用 Keychain 实现，serviceName 为应用标识
 */
actual fun createKVault(): KVault {
    return KVault(serviceName = "com.pgigi.pumpkintoolkit", accessGroup = null)
}