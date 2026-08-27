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

    /// Every word across every grade band, lowercase — never a valid "fake".
    static let allListWords: Set<String> = Set(
        WordData.words.values.flatMap { $0.map { $0.word.lowercased() } }
    )

    static func make(for word: String, count: Int) -> [String] {
        let pool = candidates(for: word.lowercased()).filter {
            !WordData.realWordGuard.contains($0) && !allListWords.contains($0)
        }
        return Array(pool.shuffled().prefix(count))
    }
}
