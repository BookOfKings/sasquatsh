import SwiftUI

// Material Design Community icons matching the web app's inline SVGs.
// Each shape is drawn into a 24×24 unit space and scales to fit.

enum MDCIcon {

    // MARK: - Tab Bar / Navigation

    /// dice-5 — five dots on a die face (Games tab)
    struct Dice: Shape {
        func path(in rect: CGRect) -> Path {
            scaledPath(in: rect) { p in
                // M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3
                p.addRoundedRect(in: r(3,3,18,18), cornerSize: CGSize(width: 2, height: 2))
                // circles at corners + center
                p.addEllipse(in: r(5,5,4,4))
                p.addEllipse(in: r(15,5,4,4))
                p.addEllipse(in: r(10,10,4,4))
                p.addEllipse(in: r(5,15,4,4))
                p.addEllipse(in: r(15,15,4,4))
            }
        }
    }

    /// account-multiple — three people (Groups tab)
    struct AccountMultiple: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            let s = min(rect.width, rect.height) / 24
            let ox = rect.minX + (rect.width - 24 * s) / 2
            let oy = rect.minY + (rect.height - 24 * s) / 2
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: oy + y * s) }

            // Center person head
            path.addEllipse(in: CGRect(x: ox + 8.5 * s, y: oy + 5.5 * s, width: 7 * s, height: 7 * s))
            // Left person head
            path.addEllipse(in: CGRect(x: ox + 2 * s, y: oy + 8 * s, width: 6 * s, height: 6 * s))
            // Right person head
            path.addEllipse(in: CGRect(x: ox + 16 * s, y: oy + 8 * s, width: 6 * s, height: 6 * s))
            // Center person body
            path.move(to: pt(5.5, 18.25))
            path.addQuadCurve(to: pt(12, 14.5), control: pt(5.5, 14.5))
            path.addQuadCurve(to: pt(18.5, 18.25), control: pt(18.5, 14.5))
            path.addLine(to: pt(18.5, 20))
            path.addLine(to: pt(5.5, 20))
            path.closeSubpath()
            // Left body
            path.move(to: pt(0, 18.5))
            path.addQuadCurve(to: pt(4.45, 15.6), control: pt(0, 15.6))
            path.addQuadCurve(to: pt(3.5, 18.25), control: pt(3.5, 16.5))
            path.addLine(to: pt(3.5, 20))
            path.addLine(to: pt(0, 20))
            path.closeSubpath()
            // Right body
            path.move(to: pt(24, 18.5))
            path.addQuadCurve(to: pt(19.55, 15.6), control: pt(24, 15.6))
            path.addQuadCurve(to: pt(20.5, 18.25), control: pt(20.5, 16.5))
            path.addLine(to: pt(20.5, 20))
            path.addLine(to: pt(24, 20))
            path.closeSubpath()

            return path
        }
    }

    /// account-search — person with magnifying glass (Need Players tab)
    struct AccountSearch: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            let s = min(rect.width, rect.height) / 24
            let ox = rect.minX + (rect.width - 24 * s) / 2
            let oy = rect.minY + (rect.height - 24 * s) / 2
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: oy + y * s) }

            // Person head
            path.addEllipse(in: CGRect(x: ox + 6 * s, y: oy + 4 * s, width: 8 * s, height: 8 * s))
            // Person body
            path.move(to: pt(2, 18))
            path.addQuadCurve(to: pt(9.5, 12), control: pt(2, 12))
            path.addQuadCurve(to: pt(9, 14.5), control: pt(9, 13))
            path.addQuadCurve(to: pt(10.79, 18.93), control: pt(9, 17.5))
            path.addLine(to: pt(10, 19))
            path.addLine(to: pt(2, 19))
            path.closeSubpath()
            // Magnifying glass circle
            path.addEllipse(in: CGRect(x: ox + 11 * s, y: oy + 12 * s, width: 9 * s, height: 9 * s))
            // Cut out inner circle (draw inner as separate fill later)
            // Glass handle
            path.move(to: pt(19.31, 18.9))
            path.addLine(to: pt(22.39, 22))
            path.addLine(to: pt(21, 23.39))
            path.addLine(to: pt(17.88, 20.32))
            path.closeSubpath()

            return path
        }
    }

    /// view-dashboard — grid layout (Dashboard tab)
    struct Dashboard: Shape {
        func path(in rect: CGRect) -> Path {
            scaledPath(in: rect) { p in
                // Top-right tall block
                p.addRect(r(13, 3, 8, 6))
                // Bottom-right tall block
                p.addRect(r(13, 11, 8, 10))
                // Bottom-left short block
                p.addRect(r(3, 15, 8, 6))
                // Top-left tall block
                p.addRect(r(3, 3, 8, 10))
            }
        }
    }

    /// account — single person (Profile tab)
    struct Account: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            let s = min(rect.width, rect.height) / 24
            let ox = rect.minX + (rect.width - 24 * s) / 2
            let oy = rect.minY + (rect.height - 24 * s) / 2
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: oy + y * s) }

            // Head
            path.addEllipse(in: CGRect(x: ox + 8 * s, y: oy + 4 * s, width: 8 * s, height: 8 * s))
            // Body
            path.move(to: pt(4, 18))
            path.addQuadCurve(to: pt(12, 14), control: pt(4, 14))
            path.addQuadCurve(to: pt(20, 18), control: pt(20, 14))
            path.addLine(to: pt(20, 20))
            path.addLine(to: pt(4, 20))
            path.closeSubpath()

            return path
        }
    }

    /// Meeple — classic board game meeple silhouette (Profile tab)
    struct Meeple: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            let s = min(rect.width, rect.height) / 24
            let ox = rect.minX + (rect.width - 24 * s) / 2
            let oy = rect.minY + (rect.height - 24 * s) / 2
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: oy + y * s) }

            // Head
            path.addEllipse(in: CGRect(x: ox + 9 * s, y: oy + 2 * s, width: 6 * s, height: 6 * s))

            // Body: arms spread, legs apart
            path.move(to: pt(12, 8.5))
            // Left arm
            path.addQuadCurve(to: pt(2, 13), control: pt(7, 9))
            path.addLine(to: pt(2, 15))
            path.addQuadCurve(to: pt(8, 14), control: pt(5, 15))
            // Left leg
            path.addLine(to: pt(7, 22))
            path.addLine(to: pt(10, 22))
            path.addLine(to: pt(12, 16))
            // Right leg
            path.addLine(to: pt(14, 22))
            path.addLine(to: pt(17, 22))
            path.addLine(to: pt(16, 14))
            // Right arm
            path.addQuadCurve(to: pt(22, 15), control: pt(19, 15))
            path.addLine(to: pt(22, 13))
            path.addQuadCurve(to: pt(12, 8.5), control: pt(17, 9))
            path.closeSubpath()

            return path
        }
    }

    // MARK: - Helpers

    private static func scaledPath(in rect: CGRect, draw: (inout Path) -> Void) -> Path {
        var path = Path()
        draw(&path)
        let bounds = CGRect(x: 0, y: 0, width: 24, height: 24)
        let sx = rect.width / bounds.width
        let sy = rect.height / bounds.height
        let scale = min(sx, sy)
        let tx = rect.minX + (rect.width  - bounds.width  * scale) / 2
        let ty = rect.minY + (rect.height - bounds.height * scale) / 2
        return path.applying(CGAffineTransform(translationX: tx, y: ty).scaledBy(x: scale, y: scale))
    }

    /// Convenience rect in the 24x24 unit space.
    private static func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
        CGRect(x: x, y: y, width: w, height: h)
    }
}

// Free function wrappers so Shape methods can call them
private func scaledPath(in rect: CGRect, draw: (inout Path) -> Void) -> Path {
    var path = Path()
    draw(&path)
    let sx = rect.width / 24
    let sy = rect.height / 24
    let scale = min(sx, sy)
    let tx = rect.minX + (rect.width  - 24 * scale) / 2
    let ty = rect.minY + (rect.height - 24 * scale) / 2
    return path.applying(CGAffineTransform(scaleX: scale, y: scale).concatenating(CGAffineTransform(translationX: tx, y: ty)))
}

private func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
    CGRect(x: x, y: y, width: w, height: h)
}

// MARK: - UIImage rendering for tab bars

extension MDCIcon {
    /// Render any MDC icon Shape into a template UIImage suitable for tab bars.
    /// Use `evenOdd: true` for icons like dice where inner shapes should be cutout holes.
    @MainActor
    static func uiImage<S: Shape>(_ shape: S, size: CGFloat = 25, evenOdd: Bool = false) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: size, height: size))
        let img = renderer.image { ctx in
            let rect = CGRect(origin: .zero, size: CGSize(width: size, height: size))
            let swiftUIPath = shape.path(in: rect)
            let cgPath = swiftUIPath.cgPath
            let context = ctx.cgContext
            context.addPath(cgPath)
            context.setFillColor(UIColor.black.cgColor)
            if evenOdd {
                context.fillPath(using: .evenOdd)
            } else {
                context.fillPath()
            }
        }
        return img.withRenderingMode(.alwaysTemplate)
    }
}
