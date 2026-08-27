import Foundation
import Observation

/// The shared 10-word round state machine, mirroring the site's useGameRound.
@Observable
final class RoundEngine {
    enum Phase {
        case playing
        case done
    }

    static let roundLength = 10

    private(set) var words: [WordEntry] = []
    private(set) var index = 0
    private(set) var score = 0
    private(set) var streak = 0
    private(set) var bestStreak = 0
    private(set) var results: [Bool] = []
    private(set) var phase: Phase = .playing
    /// Bumps whenever a fresh round starts, for per-word view identity.
    private(set) var roundId = 0

    var current: WordEntry? {
        phase == .playing && index < words.count ? words[index] : nil
    }

    var isLastWord: Bool { index + 1 >= words.count }

    func start(pool: [WordEntry], length: Int = RoundEngine.roundLength) {
        words = pickRandom(pool, length)
        resetProgress()
    }

    /// Deterministic variant for the Daily Bee (set is seeded, order shuffles).
    func start(fixedWords: [WordEntry]) {
        words = fixedWords.shuffled()
        resetProgress()
    }

    private func resetProgress() {
        index = 0
        score = 0
        streak = 0
        bestStreak = 0
        results = []
        phase = .playing
        roundId += 1
    }

    func record(correct: Bool) {
        guard phase == .playing else { return }
        if correct {
            score += 1
            streak += 1
            bestStreak = max(bestStreak, streak)
        } else {
            streak = 0
        }
        results.append(correct)
    }

    func advance() {
        guard phase == .playing else { return }
        if index + 1 >= words.count {
            phase = .done
        } else {
            index += 1
        }
    }

    var missedWords: [WordEntry] {
        words.enumerated()
            .filter { $0.offset < results.count && results[$0.offset] == false }
            .map(\.element)
    }

    static func stars(score: Int, total: Int) -> Int {
        guard total > 0 else { return 0 }
        let ratio = Double(score) / Double(total)
        if ratio >= 0.9 { return 3 }
        if ratio >= 0.7 { return 2 }
        if ratio >= 0.5 { return 1 }
        return 0
    }
}
