import SwiftUI

struct HomeView: View {
    private static let heroTiles: [(letter: String, color: Color, tilt: Double)] = [
        ("S", .coralSoft, -6), ("P", .sunSoft, 3), ("E", .leafSoft, -2),
        ("L", .skySoft, 6), ("L", .white, -3), ("I", .sunSoft, 2),
        ("N", .coralSoft, -3), ("G", .leafSoft, 6),
    ]

    @State private var path: [Game] = HomeView.launchGame()

    /// Debug hook: `-game wordScramble` launches straight into that game.
    private static func launchGame() -> [Game] {
        let args = ProcessInfo.processInfo.arguments
        guard let flag = args.firstIndex(of: "-game"), args.indices.contains(flag + 1),
              let game = Game(rawValue: args[flag + 1])
        else { return [] }
        return [game]
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(spacing: 24) {
                    hero
                    gamesGrid
                    levels
                }
                .padding(16)
            }
            .background(Color.paper)
            .navigationDestination(for: Game.self) { game in
                destination(for: game)
            }
        }
        .tint(Color.ink)
    }

    private var hero: some View {
        VStack(spacing: 14) {
            FlowLayout(spacing: 5) {
                ForEach(Array(Self.heroTiles.enumerated()), id: \.offset) { _, tile in
                    TileView(letter: tile.letter, size: .sm, fill: tile.color)
                        .rotationEffect(.degrees(tile.tilt))
                }
            }
            .accessibilityHidden(true)
            Text("Practice that feels like recess.")
                .font(.heading(28, weight: .semibold))
                .foregroundStyle(Color.ink)
                .multilineTextAlignment(.center)
            Text("Twelve quick games, four levels — from first words like cat to champion stumpers like mischievous.")
                .font(.system(size: 15))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 12)
    }

    private var gamesGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
            ForEach(Game.allCases) { game in
                NavigationLink(value: game) {
                    VStack(alignment: .leading, spacing: 8) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12).fill(Color.ink).offset(y: 3)
                            RoundedRectangle(cornerRadius: 12)
                                .fill(game.accentSoft)
                                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color.ink, lineWidth: 2.5))
                            Image(systemName: game.symbol)
                                .font(.system(size: 19))
                                .foregroundStyle(Color.ink)
                        }
                        .frame(width: 44, height: 44)
                        Text(game.title)
                            .font(.heading(17, weight: .semibold))
                            .foregroundStyle(Color.ink)
                        Text(game.tagline)
                            .font(.heading(12, weight: .medium))
                            .foregroundStyle(game.accent)
                        Text(game.blurb)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.mutedInk)
                            .lineLimit(3)
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, minHeight: 168, alignment: .topLeading)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color.white)
                            .shadow(color: Color.ink.opacity(0.08), radius: 5, y: 3)
                    )
                    .overlay(alignment: .top) {
                        UnevenRoundedRectangle(topLeadingRadius: 16, topTrailingRadius: 16)
                            .fill(game.accent)
                            .frame(height: 6)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var levels: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("A level for every speller")
                .font(.heading(20, weight: .semibold))
                .foregroundStyle(Color.ink)
            Text("Change your level any time from inside a game — your pick is remembered.")
                .font(.system(size: 13))
                .foregroundStyle(Color.mutedInk)
            ForEach(GradeBand.allCases) { band in
                HStack(spacing: 12) {
                    Text(band.short)
                        .font(.heading(14, weight: .semibold))
                        .frame(width: 48, height: 34)
                        .background(
                            RoundedRectangle(cornerRadius: 10).fill(Color.sunSoft)
                        )
                        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color.ink, lineWidth: 2.5))
                        .foregroundStyle(Color.ink)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(band.label)
                            .font(.heading(15, weight: .semibold))
                            .foregroundStyle(Color.ink)
                        Text(band.blurb)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.mutedInk)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color.secondaryBg))
    }

    @ViewBuilder
    private func destination(for game: Game) -> some View {
        switch game {
        case .dailyBee: DailyBeeView()
        case .wordScramble: WordScrambleView()
        case .missingLetters: MissingLettersView()
        case .listenAndSpell: ListenSpellView()
        case .spotTheWord: SpotWordView()
        case .flashSpell: FlashSpellView()
        case .fixTheSentence: FixSentenceView()
        case .endingMachine: EndingMachineView()
        case .miniCrossword: MiniCrosswordView()
        case .wordSearch: WordSearchView()
        case .memoryMatch: MemoryMatchView()
        case .balloonPop: BalloonPopView()
        }
    }
}

#Preview {
    HomeView()
}
