import Foundation
import UIKit

/// 小组件专用的网络数据获取工具
/// 通过云端 KV 存储读取课表数据（同步，阻塞当前线程）
enum NetworkHelper {

    static let baseURL = "https://pumpkin-api.pgigi.com"

    /// 获取设备 IDFV
    /// UIDevice 在小组件 Extension 中可用（需 import UIKit）
    private static func getIDFV() -> String? {
        return UIDevice.current.identifierForVendor?.uuidString
    }

    /// 从网络获取课表数据（同步阻塞）
    /// POST https://pumpkin-api.pgigi.com/get/{IDFV}
    /// - Returns: 解码后的 WidgetData，失败返回 nil
    static func loadWidgetData() -> WidgetData? {
        guard let idfv = getIDFV(), !idfv.isEmpty else { return nil }

        let urlString = "\(baseURL)/get/\(idfv)"
        guard let url = URL(string: urlString) else { return nil }

        // 使用信号量实现同步请求（小组件 Timeline Provider 需要同步返回）
        let semaphore = DispatchSemaphore(value: 0)
        var result: WidgetData?

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 5

        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }
            guard error == nil,
                  let data = data,
                  let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                return
            }
            result = try? JSONDecoder().decode(WidgetData.self, from: data)
        }
        task.resume()
        semaphore.wait()

        return result
    }
}