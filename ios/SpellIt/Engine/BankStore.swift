import Foundation
import Observation

/// Custom word banks + the active-bank choice, persisted in UserDefaults.
@Observable
final class BankStore {
    static let shared = BankStore()

    private static let customKey = "spellit.customBanks"
    private static let activeKey = "spellit.activeBank"
    static let defaultBankId = "band-2-3"

    private struct StoredBank: Codable {
        var id: String
        var name: String
        var entries: [WordEntry]
    }

    private(set) var customBanks: [WordBank] = []
    private(set) var activeId: String = BankStore.defaultBankId

    private init() {
        if let data = UserDefaults.standard.data(forKey: Self.customKey),
           let stored = try? JSONDecoder().decode([StoredBank].self, from: data) {
            customBanks = stored.map {
                WordBank(
                    id: $0.id,
                    name: $0.name,
                    blurb: "Your custom list",
                    builtIn: false,
                    entries: $0.entries,
                )
            }
        }
        if let saved = UserDefaults.standard.string(forKey: Self.activeKey),
           allBanks.contains(where: { $0.id == saved }) {
            activeId = saved
        }
    }

    var allBanks: [WordBank] {
        WordData.builtInBanks + customBanks
    }

    var activeBank: WordBank {
        allBanks.first { $0.id == activeId }
            ?? WordData.builtInBanks.first { $0.id == Self.defaultBankId }
            ?? WordData.builtInBanks[0]
    }

    func setActive(_ id: String) {
        activeId = id
        UserDefaults.standard.set(id, forKey: Self.activeKey)
    }

    private func persist() {
        let stored = customBanks.map {
            StoredBank(id: $0.id, name: $0.name, entries: $0.entries)
        }
        if let data = try? JSONEncoder().encode(stored) {
            UserDefaults.standard.set(data, forKey: Self.customKey)
        }
    }

    @discardableResult
    func createBank(name: String = "My new list", entries: [WordEntry] = []) -> WordBank {
        let bank = WordBank(
            id: UUID().uuidString,
            name: name,
            blurb: "Your custom list",
            builtIn: false,
            entries: entries,
        )
        customBanks.append(bank)
        persist()
        return bank
    }

    func deleteBank(_ id: String) {
        customBanks.removeAll { $0.id == id }
        if activeId == id { setActive(Self.defaultBankId) }
        persist()
    }

    func rename(_ id: String, to name: String) {
        guard let index = customBanks.firstIndex(where: { $0.id == id }) else { return }
        customBanks[index].name = name
        persist()
    }

    func addWord(_ entry: WordEntry, to id: String) {
        guard let index = customBanks.firstIndex(where: { $0.id == id }) else { return }
        customBanks[index].entries.append(entry)
        persist()
    }

    func removeWord(at wordIndex: Int, from id: String) {
        guard let index = customBanks.firstIndex(where: { $0.id == id }),
              customBanks[index].entries.indices.contains(wordIndex)
        else { return }
        customBanks[index].entries.remove(at: wordIndex)
        persist()
    }
}

// MARK: - Word-length heuristics (banks carry no grade)

enum GameHeuristics {
    /** How many letters Missing Letters blanks out. */
    static func blanks(for word: String) -> Int {
        min(4, max(1, Int((Double(word.count) / 3).rounded())))
    }

    /** How long Flash Spell shows the word before hiding it. */
    static func flashSeconds(for word: String) -> Double {
        if word.count <= 4 { return 4 }
        if word.count <= 7 { return 3.5 }
        return 3
    }
}
