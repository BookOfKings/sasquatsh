import SwiftUI

extension Font {
    static func inter(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        switch weight {
        case .bold, .heavy, .black:
            return .custom("Inter-Bold", size: size)
        case .semibold:
            return .custom("Inter-SemiBold", size: size)
        case .medium:
            return .custom("Inter-Medium", size: size)
        default:
            return .custom("Inter-Regular", size: size)
        }
    }

    // MARK: - MD3 Type Scale
    static let md3DisplayLarge = Font.custom("Inter-Bold", size: 28)
    static let md3HeadlineLarge = Font.custom("Inter-SemiBold", size: 22)
    static let md3HeadlineMedium = Font.custom("Inter-SemiBold", size: 19)
    static let md3HeadlineSmall = Font.custom("Inter-SemiBold", size: 17)
    static let md3TitleLarge = Font.custom("Inter-SemiBold", size: 18)
    static let md3TitleMedium = Font.custom("Inter-Medium", size: 14)
    static let md3TitleSmall = Font.custom("Inter-Medium", size: 12)
    static let md3BodyLarge = Font.custom("Inter-Regular", size: 14)
    static let md3BodyMedium = Font.custom("Inter-Regular", size: 12)
    static let md3BodySmall = Font.custom("Inter-Regular", size: 10)
    static let md3LabelLarge = Font.custom("Inter-Medium", size: 13)
    static let md3LabelMedium = Font.custom("Inter-Medium", size: 11)
    static let md3LabelSmall = Font.custom("Inter-Medium", size: 9)

    // MARK: - Legacy aliases
    static let interLargeTitle = md3DisplayLarge
    static let interTitle = md3HeadlineLarge
    static let interTitle2 = md3HeadlineMedium
    static let interTitle3 = md3HeadlineSmall
    static let interHeadline = Font.custom("Inter-SemiBold", size: 15)
    static let interBody = md3BodyLarge
    static let interCallout = Font.custom("Inter-Regular", size: 13)
    static let interSubheadline = md3TitleSmall
    static let interFootnote = md3LabelMedium
    static let interCaption = md3BodySmall
    static let interCaption2 = md3LabelSmall
}
