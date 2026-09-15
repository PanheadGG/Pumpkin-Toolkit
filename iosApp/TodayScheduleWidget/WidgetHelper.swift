import Foundation

enum WidgetHelper {

    static let dayOfWeekText = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]

    /// 从 Keychain 读取小组件课表数据
    static func loadWidgetData() -> WidgetData? {
        return KeychainHelper.loadWidgetData()
    }

    static func getWeekNumber(startDateStr: String?, date: Date) -> Int {
        guard let startDateStr = startDateStr else { return 0 }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        guard let startDate = formatter.date(from: startDateStr) else { return 0 }
        let calendar = Calendar.current
        let startSunday = sundayOfWeek(startDate, calendar: calendar)
        let targetSunday = sundayOfWeek(date, calendar: calendar)
        let dayDiff = calendar.dateComponents([.day], from: startSunday, to: targetSunday).day ?? 0
        return dayDiff / 7 + 1
    }

    private static func sundayOfWeek(_ date: Date, calendar: Calendar) -> Date {
        let weekday = calendar.component(.weekday, from: date)
        let backDays = (weekday - 1) % 7
        return calendar.date(byAdding: .day, value: -backDays, to: date)!
    }

    static func isHoliday(weekNumber: Int, totalWeek: Int) -> Bool {
        return weekNumber <= 0 || (totalWeek > 0 && weekNumber > totalWeek)
    }

    static func getDayOfWeek(date: Date) -> Int {
        let calendar = Calendar.current
        let weekday = calendar.component(.weekday, from: date)
        return weekday - 1
    }

    static func getDayOfWeekText(_ dayOfWeek: Int) -> String {
        guard dayOfWeek >= 0 && dayOfWeek <= 6 else { return "" }
        return dayOfWeekText[dayOfWeek]
    }

    static func currentTimeStr() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: Date())
    }

    static func getRemainingCourses(data: WidgetData, date: Date, currentTime: String) -> [DisplayCourse] {
        let weekNumber = getWeekNumber(startDateStr: data.startDate, date: date)
        if isHoliday(weekNumber: weekNumber, totalWeek: data.totalWeek) { return [] }

        let dayOfWeek = getDayOfWeek(date: date)
        return data.courses
            .filter { course in
                course.dayOfWeek == dayOfWeek &&
                course.weeks.contains { $0[0] <= weekNumber && $0[1] >= weekNumber }
            }
            .sorted { $0.lessonOfDay < $1.lessonOfDay }
            .filter { course in
                let endIndex = min(course.lessonOfDay - 1 + course.duration - 1, data.timeList.count - 1)
                return data.timeList[endIndex].end >= currentTime
            }
            .map { course in
                toDisplayCourse(course, timeList: data.timeList, isTomorrow: false)
            }
    }

    static func getTomorrowCourses(data: WidgetData, today: Date) -> [DisplayCourse] {
        let calendar = Calendar.current
        guard let tomorrow = calendar.date(byAdding: .day, value: 1, to: today) else { return [] }
        let weekNumber = getWeekNumber(startDateStr: data.startDate, date: tomorrow)
        if isHoliday(weekNumber: weekNumber, totalWeek: data.totalWeek) { return [] }

        let dayOfWeek = getDayOfWeek(date: tomorrow)
        return data.courses
            .filter { course in
                course.dayOfWeek == dayOfWeek &&
                course.weeks.contains { $0[0] <= weekNumber && $0[1] >= weekNumber }
            }
            .sorted { $0.lessonOfDay < $1.lessonOfDay }
            .map { course in
                toDisplayCourse(course, timeList: data.timeList, isTomorrow: true)
            }
    }

    static func toDisplayCourse(_ course: WidgetCourseItem, timeList: [WidgetScheduleTime], isTomorrow: Bool) -> DisplayCourse {
        let startIndex = max(0, min(course.lessonOfDay - 1, timeList.count - 1))
        let endIndex = max(0, min(startIndex + course.duration - 1, timeList.count - 1))
        let classroom = course.classroom
            .replacingOccurrences(of: "【红湘校区】", with: "")
            .replacingOccurrences(of: "【雨母校区】", with: "")
        return DisplayCourse(
            name: course.name,
            classroom: classroom,
            teacher: course.teacher,
            startTime: timeList[startIndex].start,
            endTime: timeList[endIndex].end,
            isTomorrow: isTomorrow
        )
    }

    static func timeToDate(_ timeStr: String, baseDate: Date) -> Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        let calendar = Calendar.current
        guard let timeDate = formatter.date(from: timeStr) else { return nil }
        let timeComps = calendar.dateComponents([.hour, .minute], from: timeDate)
        return calendar.date(bySettingHour: timeComps.hour ?? 0,
                             minute: timeComps.minute ?? 0,
                             second: 0, of: baseDate)
    }
}
