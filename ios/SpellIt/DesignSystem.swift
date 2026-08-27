import SwiftUI

// MARK: - Crayon palette (sRGB approximations of the site's oklch tokens)

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
        )
    }

    static let paper = Color(hex: 0xFDFBF2)
    static let ink = Color(hex: 0x31435C)
    static let mutedInk = Color(hex: 0x6E7B90)
    static let softBorder = Color(hex: 0xC3CCDA)
    static let secondaryBg = Color(hex: 0xF5F1E3)

    static let coral = Color(hex: 0xE2705F)
    static let coralSoft = Color(hex: 0xFADFD8)
    static let sun = Color(hex: 0xF0C452)
    static let sunSoft = Color(hex: 0xFAF0CF)
    static let leaf = Color(hex: 0x47B583)
    static let leafSoft = Color(hex: 0xDFF2E5)
    static let sky = Color(hex: 0x5A8FCB)
    static let skySoft = Color(hex: 0xDEEAF6)
    static let grape = Color(hex: 0x8659B5)
    static let grapeSoft = Color(hex: 0xEEE4F4)
}

// MARK: - Typography

extension Font {
    static func heading(_ size: CGFloat, weight: Font.Weight = .semibold) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }
}

// MARK: - Letter tile, the signature element

enum TileSize {
    case xs, sm, md, lg

    var side: CGFloat {
        switch self {
        case .xs: 32
        case .sm: 40
        case .md: 48
        case .lg: 56
        }
    }

    var fontSize: CGFloat {
        switch self {
        case .xs: 15
        case .sm: 19
        case .md: 24
        case .lg: 30
        }
    }

    static func forWord(_ word: String) -> TileSize {
        if word.count > 10 { return .xs }
        if word.count > 7 { return .sm }
        return .md
    }
}

/// Chunky letter tile with the hard offset shadow.
struct TileView: View {
    var letter: String
    var size: TileSize = .md
    var fill: Color = .white
    var dashed = false

    var body: some View {
        ZStack {
            if !dashed {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.ink)
                    .offset(y: 4)
            }
            RoundedRectangle(cornerRadius: 12)
                .fill(dashed ? fill.opacity(0.5) : fill)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(
                            dashed ? Color.softBorder : Color.ink,
                            style: StrokeStyle(lineWidth: 3, dash: dashed ? [6, 5] : []),
                        )
                )
            Text(letter.uppercased())
                .font(.heading(size.fontSize, weight: .bold))
                .foregroundStyle(Color.ink)
                .minimumScaleFactor(0.5)
        }
        .frame(width: size.side, height: size.side)
        .accessibilityLabel(letter.isEmpty ? "blank" : letter)
    }
}

/// Tappable tile with a press-down animation.
struct TileButton: View {
    var letter: String
    var size: TileSize = .md
    var fill: Color = .white
    var disabled = false
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            TileView(letter: letter, size: size, fill: fill)
                .opacity(disabled ? 0.35 : 1)
        }
        .buttonStyle(PressStyle())
        .disabled(disabled)
    }
}

struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .offset(y: configuration.isPressed ? 3 : 0)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

// MARK: - Chunky action buttons

struct ChunkyButtonStyle: ButtonStyle {
    var background: Color = .ink
    var foreground: Color = .white
    var bordered = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.heading(16, weight: .semibold))
            .padding(.horizontal, 20)
            .padding(.vertical, 11)
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.ink)
                        .offset(y: 3)
                    RoundedRectangle(cornerRadius: 14)
                        .fill(bordered ? Color.white : background)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(Color.ink, lineWidth: bordered ? 2.5 : 0)
                        )
                }
            )
            .foregroundStyle(bordered ? Color.ink : foreground)
            .offset(y: configuration.isPressed ? 3 : 0)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

// MARK: - Shake effect for wrong answers

struct ShakeEffect: GeometryEffect {
    var travel: CGFloat = 6
    var shakes: CGFloat = 3
    var animatableData: CGFloat

    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(
            CGAffineTransform(
                translationX: travel * sin(animatableData * .pi * shakes * 2),
                y: 0,
            )
        )
    }
}

extension View {
    func shake(trigger: Int) -> some View {
        modifier(ShakeModifier(trigger: trigger))
    }
}

private struct ShakeModifier: ViewModifier {
    var trigger: Int

    func body(content: Content) -> some View {
        content
            .modifier(ShakeEffect(animatableData: CGFloat(trigger)))
            .animation(.easeInOut(duration: 0.35), value: trigger)
    }
}
