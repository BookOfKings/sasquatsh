import Foundation

extension Date {
    static let apiDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    static let apiDateTimeFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    var displayDate: String {
        let f = DateFormatter()
        f.dateStyle = .medium
        return f.string(from: self)
    }

    var displayTime: String {
        let f = DateFormatter()
        f.timeStyle = .short
        return f.string(from: self)
    }

    var displayDateTime: String {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f.string(from: self)
    }

    var relativeDisplay: String {
        let f = RelativeDateTimeFormatter()
        f.unitsStyle = .short
        return f.localizedString(for: self, relativeTo: Date())
    }

    var apiDateString: String {
        Date.apiDateFormatter.string(from: self)
    }
}

extension String {
    var toDate: Date? {
        Date.apiDateTimeFormatter.date(from: self) ?? Date.apiDateFormatter.date(from: self)
    }
}
