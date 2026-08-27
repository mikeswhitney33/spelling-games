import SwiftUI

enum DailyMechanic: String, CaseIterable {
    case scramble
    case missing
    case spot
    case flash

    var label: String {
        switch self {
        case .scramble: "Unscramble it!"
        case .missing: "Fill the gaps!"
        case .spot: "Spot the real spelling!"
        case .flash: "Memorize it!"
        }
    }
}

struct DailyStreak: Codable {
    var lastPlayed = ""
    var streak = 0
    var best = 0
}

enum DailyStore {
    private static let key = "spellit.daily"

    /// Always Gregorian so the daily seed matches the website (a device set
    /// to e.g. the Buddhist calendar would otherwise hash a different year).
    private static var gregorian: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        return calendar
    }

    static func formatDate(_ date: Date = Date()) -> String {
        let parts = gregorian.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }

    static func read() -> DailyStreak {
        guard
            let data = UserDefaults.standard.data(forKey: key),
            let streak = try? JSONDecoder().decode(DailyStreak.self, from: data)
        else { return DailyStreak() }
        return streak
    }

    /// Record a completion for `today`; only the first finish of a day counts.
    static func save(today: String) -> DailyStreak {
        var data = read()
        guard data.lastPlayed != today else { return data }
        let yesterday = gregorian.date(byAdding: .day, value: -1, to: parse(today))
            .map(formatDate) ?? ""
        data.streak = data.lastPlayed == yesterday ? data.streak + 1 : 1
        data.best = max(data.best, data.streak)
        data.lastPlayed = today
        if let encoded = try? JSONEncoder().encode(data) {
            UserDefaults.standard.set(encoded, forKey: key)
        }
        return data
    }

    private static func parse(_ text: String) -> Date {
        let parts = text.split(separator: "-").compactMap { Int($0) }
        var components = DateComponents()
        if parts.count == 3 {
            components.year = parts[0]
            components.month = parts[1]
            components.day = parts[2]
            components.hour = 12
        }
        return gregorian.date(from: components) ?? Date()
    }
}

struct DailyBeeView: View {
    @AppStorage("spellit.grade") private var gradeRaw = GradeBand.g23.rawValue
    @State private var engine = RoundEngine()
    @State private var mechanicByWord: [String: DailyMechanic] = [:]
    @State private var dailyDate = ""
    @State private var streak = DailyStreak()
    @State private var recordedFor: String?
    @Environment(\.scenePhase) private var scenePhase

    private var grade: Binding<GradeBand> {
        Binding(
            get: { GradeBand(rawValue: gradeRaw) ?? .g23 },
            set: { gradeRaw = $0.rawValue },
        )
    }

    var body: some View {
        GameScaffold(
            game: .dailyBee,
            grade: grade,
            engine: engine,
            summaryText: summaryText,
            onRestart: startRound,
        ) {
            if let entry = engine.current,
               let mechanic = mechanicByWord[entry.word] {
                VStack(spacing: 12) {
                    headerRow
                    Text(mechanic.label)
                        .font(.heading(13, weight: .semibold))
                        .foregroundStyle(Color.sky)
                        .textCase(.uppercase)
                    challengeView(entry: entry, mechanic: mechanic)
                        .id("\(engine.roundId)-\(engine.index)")
                }
            }
        }
        .onAppear {
            if engine.words.isEmpty { startRound() }
            streak = DailyStore.read()
        }
        .onChange(of: gradeRaw) { startRound() }
        .onChange(of: scenePhase) { _, phase in
            // Roll over to the new day's round when the app comes back.
            if phase == .active, !dailyDate.isEmpty, DailyStore.formatDate() != dailyDate {
                startRound()
            }
        }
        .onChange(of: engine.phase) { _, phase in
            guard phase == .done else { return }
            let today = DailyStore.formatDate()
            guard recordedFor != today else { return }
            recordedFor = today
            streak = DailyStore.save(today: today)
        }
    }

    private var headerRow: some View {
        HStack(spacing: 12) {
            Text(dateLabel)
                .font(.heading(13, weight: .medium))
                .foregroundStyle(Color.mutedInk)
            if streak.streak > 0 {
                Label("\(streak.streak)-day streak", systemImage: "flame.fill")
                    .font(.heading(13, weight: .medium))
                    .foregroundStyle(Color.coral)
            }
        }
    }

    private var dateLabel: String {
        Date().formatted(.dateTime.weekday(.wide).month(.wide).day())
    }

    private var summaryText: String {
        var text = "\(engine.score) of \(engine.words.count) on today's challenge"
        if streak.streak > 0 {
            text += " — \(streak.streak)-day streak (best: \(streak.best))"
        }
        return text + "!"
    }

    @ViewBuilder
    private func challengeView(entry: WordEntry, mechanic: DailyMechanic) -> some View {
        let props = (
            isLast: engine.isLastWord,
            onJudged: { engine.record(correct: $0) },
            onNext: { engine.advance() }
        )
        switch mechanic {
        case .scramble:
            ScrambleWordView(entry: entry, isLast: props.isLast, onJudged: props.onJudged, onNext: props.onNext)
        case .missing:
            MissingLettersWordView(
                entry: entry, grade: grade.wrappedValue,
                isLast: props.isLast, onJudged: props.onJudged, onNext: props.onNext,
            )
        case .spot:
            SpotWordChallengeView(entry: entry, isLast: props.isLast, onJudged: props.onJudged, onNext: props.onNext)
        case .flash:
            FlashWordView(
                entry: entry, grade: grade.wrappedValue,
                isLast: props.isLast, onJudged: props.onJudged, onNext: props.onNext,
            )
        }
    }

    private func startRound() {
        let today = DailyStore.formatDate()
        dailyDate = today
        var rng = Mulberry32(seed: seedHash("\(today)-\(grade.wrappedValue.seedKey)"))
        let pool = WordData.words[grade.wrappedValue] ?? []
        let picked = seededPick(pool, RoundEngine.roundLength, rng: &rng)
        let mechanics = DailyMechanic.allCases
        mechanicByWord = Dictionary(
            uniqueKeysWithValues: picked.map {
                ($0.word, mechanics[Int(rng.nextDouble() * Double(mechanics.count))])
            }
        )
        engine.start(fixedWords: picked)
        streak = DailyStore.read()
    }
}
