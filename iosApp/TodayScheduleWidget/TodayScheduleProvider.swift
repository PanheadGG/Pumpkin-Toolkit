import WidgetKit
import SwiftUI

struct ScheduleEntry: TimelineEntry {
    let date: Date
    let weekNumber: Int
    let totalWeek: Int
    let isHoliday: Bool
    let dayOfWeekText: String
    let todayCourses: [DisplayCourse]
    let tomorrowCourses: [DisplayCourse]
    let hasData: Bool
}

struct TodayScheduleProvider: TimelineProvider {

    func placeholder(in context: Context) -> ScheduleEntry {
        ScheduleEntry(
            date: Date(),
            weekNumber: 1,
            totalWeek: 20,
            isHoliday: false,
            dayOfWeekText: "周一",
            todayCourses: [
                DisplayCourse(name: "高等数学", classroom: "教学楼A301", teacher: "张老师",
                              startTime: "08:00", endTime: "09:40", isTomorrow: false)
            ],
            tomorrowCourses: [],
            hasData: true
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (ScheduleEntry) -> Void) {
        let entry = buildEntry(date: Date())
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let now = Date()
        let calendar = Calendar.current

        guard let data = WidgetHelper.loadWidgetData() else {
            let entry = ScheduleEntry(
                date: now, weekNumber: 0, totalWeek: 0, isHoliday: false,
                dayOfWeekText: "", todayCourses: [], tomorrowCourses: [], hasData: false
            )
            completion(Timeline(entries: [entry], policy: .after(now.addingTimeInterval(3600))))
            return
        }

        var entries: [ScheduleEntry] = []
        var currentDate = now
        // 利用完整学期数据，为每个课程时间点生成 entry
        // 最多生成 50 个 entry，覆盖大约一周的关键时间节点
        let maxEntries = 50
        var count = 0

        while count < maxEntries {
            let entry = buildEntry(date: currentDate, data: data)
            entries.append(entry)

            let remaining = entry.todayCourses
            var nextDate: Date? = nil

            // 找到下一个课程结束时间作为下一个刷新点
            for course in remaining {
                if let endTime = WidgetHelper.timeToDate(course.endTime, baseDate: currentDate) {
                    if endTime > currentDate {
                        if nextDate == nil || endTime < nextDate! {
                            nextDate = endTime
                        }
                    }
                }
            }

            // 如果今天没有更多课程了，跳到明天第一节课开始时间
            if nextDate == nil {
                // 跳到明天的 00:00
                if let midnight = calendar.date(bySettingHour: 0, minute: 0, second: 0,
                                                 of: calendar.date(byAdding: .day, value: 1, to: currentDate) ?? currentDate) {
                    nextDate = midnight
                }
            }

            guard let next = nextDate, next > currentDate else { break }
            currentDate = next
            count += 1
        }

        if entries.isEmpty {
            let entry = buildEntry(date: now, data: data)
            entries.append(entry)
        }

        // 如果有全学期数据，可以设置更长的刷新间隔
        // 即使 1 小时后刷新，Keychain 数据不会过期，小组件始终有数据可用
        let nextReload = now.addingTimeInterval(3600)
        completion(Timeline(entries: entries, policy: .after(nextReload)))
    }

    private func buildEntry(date: Date, data: WidgetData? = nil) -> ScheduleEntry {
        let actualData = data ?? WidgetHelper.loadWidgetData()
        guard let data = actualData else {
            return ScheduleEntry(
                date: date, weekNumber: 0, totalWeek: 0, isHoliday: false,
                dayOfWeekText: "", todayCourses: [], tomorrowCourses: [], hasData: false
            )
        }

        let weekNumber = WidgetHelper.getWeekNumber(startDateStr: data.startDate, date: date)
        let totalWeek = data.totalWeek
        let isHoliday = WidgetHelper.isHoliday(weekNumber: weekNumber, totalWeek: totalWeek)
        let dayOfWeek = WidgetHelper.getDayOfWeek(date: date)
        let dayOfWeekText = WidgetHelper.getDayOfWeekText(dayOfWeek)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        let currentTime = formatter.string(from: date)

        let todayCourses = WidgetHelper.getRemainingCourses(data: data, date: date, currentTime: currentTime)
        let tomorrowCourses = WidgetHelper.getTomorrowCourses(data: data, today: date)

        return ScheduleEntry(
            date: date, weekNumber: weekNumber, totalWeek: totalWeek,
            isHoliday: isHoliday, dayOfWeekText: dayOfWeekText,
            todayCourses: todayCourses, tomorrowCourses: tomorrowCourses,
            hasData: true
        )
    }
}
