import Foundation

/// Port of the site's misspelling generator: single-edit candidates filtered
/// against the dictionary-derived guard so a "fake" is never a real word.
enum Misspell {
    private static let vowelSwaps: [Character: [Character]] = [
        "a": ["e", "u"], "e": ["a", "i"], "i": ["e", "y"],
        "o": ["u", "a"], "u": ["o", "e"], "y": ["i", "e"],
    ]

    private static let phoneticSwaps: [Character: [Character]] = [
        "c": ["k", "s"], "k": ["c"], "s": ["z", "c"], "z": ["s"],
        "f": ["v"], "v": ["f"], "g": ["j"], "j": ["g"],
    ]

    private static let vowelish = Set("aeiouhgk")
    private static let vowels = Set("aeiou")

    static func candidates(for word: String) -> [String] {
        let chars = Array(word)
        var out: Set<String> = []

        func replaced(_ i: Int, with ch: Character) -> String {
            var copy = chars
            copy[i] = ch
            return String(copy)
        }

        // Transpose adjacent letters
        for i in 0..<max(chars.count - 1, 0) where chars[i] != chars[i + 1] {
            var copy = chars
            copy.swapAt(i, i + 1)
            out.insert(String(copy))
        }
        // Vowel and phonetic swaps
        for i in chars.indices {
            for s in vowelSwaps[chars[i]] ?? [] { out.insert(replaced(i, with: s)) }
            for s in phoneticSwaps[chars[i]] ?? [] { out.insert(replaced(i, with: s)) }
        }
        // Undouble a doubled letter
        for i in 0..<max(chars.count - 1, 0) where chars[i] == chars[i + 1] {
            var copy = chars
            copy.remove(at: i)
            out.insert(String(copy))
        }
        // Double a letter
        for i in chars.indices {
            var copy = chars
            copy.insert(chars[i], at: i)
            out.insert(String(copy))
        }
        // Drop a silent-ish letter
        for i in 1..<max(chars.count - 1, 1) where vowelish.contains(chars[i]) {
            var copy = chars
            copy.remove(at: i)
            out.insert(String(copy))
        }

        out.remove(word)
        return out.filter { $0.count >= 2 }
    }

    /// Every built-in word, lowercase — never a valid "fake", and the
    /// boundary for when the cautious custom-word rules apply.
    static let allListWords: Set<String> = Set(
        WordData.builtInBanks.flatMap { $0.entries.map { $0.word.lowercased() } }
    )

    /// Candidates for custom words the dictionary guard can't vet: only edits
    /// that essentially never produce real English words (general doubling
    /// lands on real words exactly where spelling lists live —
    /// hoping/hopping, diner/dinner).
    static func cautiousCandidates(for word: String) -> [String] {
        var out: Set<String> = []
        let chars = Array(word)
        for i in 0..<max(chars.count - 1, 0) where chars[i] == chars[i + 1] {
            var copy = chars
            copy.insert(chars[i], at: i)
            out.insert(String(copy))
        }
        out.insert(String(chars[0]) + word)
        if chars.count >= 4 {
            out.insert(word + String(chars[chars.count - 1]))
        }
        out.remove(word)
        return Array(out).sorted()
    }

    static func make(for word: String, count: Int) -> [String] {
        // Generate from the lowercased word so the lowercase-keyed swap tables
        // apply to every letter, then restore the leading capital so the real
        // answer's casing doesn't give it away. Custom words the dictionary
        // guard can't vet fall back to the cautious rules.
        let lower = word.lowercased()
        let cautious = !allListWords.contains(lower)
        let raw = cautious ? cautiousCandidates(for: lower) : candidates(for: lower)
        let pool = raw.filter {
            !WordData.realWordGuard.contains($0) && !allListWords.contains($0)
        }
        return pool.shuffled().prefix(count).map { matchCase(model: word, text: $0) }
    }
}

/// Copy the model word's leading capital (if any) onto text.
func matchCase(model: String, text: String) -> String {
    guard let first = model.first, first.isUppercase, let start = text.first else { return text }
    return String(start).uppercased() + text.dropFirst()
}
