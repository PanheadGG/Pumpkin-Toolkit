import Foundation
import Security

/// 小组件专用的 Keychain 读取工具
/// 通过 Keychain Access Group 与主 App 共享数据
enum KeychainHelper {

    /// Keychain Access Group（不含 Team ID 前缀）
    /// entitlements 中声明为 $(AppIdentifierPrefix)com.pgigi.pumpkintoolkit.keychain.shared
    static let sharedAccessGroup = "R6UB6VD355.com.pgigi.pumpkintoolkit.keychain.shared"

    /// 主 App (KVault) 写入时使用的 serviceName
    /// 必须与 Kotlin 端 WIDGET_KEYCHAIN_SERVICE 完全一致
    static let widgetServiceName = "com.pgigi.pumpkintoolkit.widget"

    /// 主 App 存储 widget data 时使用的 Keychain account key
    static let widgetDataKey = "widget_schedule_data"

    /// 从 Keychain 读取字符串数据
    /// - Parameter key: 要读取的 key (account)
    /// - Returns: 存储的字符串，如果不存在则返回 nil
    static func getString(forKey key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: widgetServiceName,
            kSecAttrAccount as String: key,
            kSecAttrAccessGroup as String: sharedAccessGroup,
            kSecReturnData as String: kCFBooleanTrue as Any,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }

        return string
    }

    /// 从 Keychain 读取 widget 课表数据
    /// - Returns: 解码后的 WidgetData，如果不存在或解析失败则返回 nil
    static func loadWidgetData() -> WidgetData? {
        guard let jsonString = getString(forKey: widgetDataKey),
              let jsonData = jsonString.data(using: .utf8) else {
            return nil
        }
        return try? JSONDecoder().decode(WidgetData.self, from: jsonData)
    }
}
