import Foundation

/// Exact port of the site's mulberry32 PRNG so date-seeded picks (Daily Bee)
/// produce the same words on iOS and the web.
struct Mulberry32 {
    private var state: UInt32

    init(seed: UInt32) {
        state = seed
    }

    /// Matches the JS implementation bit-for-bit, returning in [0, 1).
    mutating func nextDouble() -> Double {
        state = state &+ 0x6D2B_79F5
        var t = state
        t = (t ^ (t >> 15)) &* (t | 1)
        t ^= t &+ (t ^ (t >> 7)) &* (t | 61)
        return Double(t ^ (t >> 14)) / 4_294_967_296
    }
}

/// Port of the site's djb2-style string hash (matches hashString in
/// daily-bee-game.tsx).
func seedHash(_ text: String) -> UInt32 {
    var hash: UInt32 = 5381
    for scalar in text.unicodeScalars {
        hash = (hash &* 33) ^ UInt32(scalar.value & 0xFFFF)
    }
    return hash
}

/// Fisher–Yates shuffle matching the site's shuffle(), so seeded picks agree.
func seededShuffle<T>(_ items: [T], rng: inout Mulberry32) -> [T] {
    var out = items
    var i = out.count - 1
    while i > 0 {
        let j = Int(rng.nextDouble() * Double(i + 1))
        out.swapAt(i, j)
        i -= 1
    }
    return out
}

func seededPick<T>(_ items: [T], _ n: Int, rng: inout Mulberry32) -> [T] {
    Array(seededShuffle(items, rng: &rng).prefix(n))
}

/// Unseeded conveniences for everyday rounds.
func pickRandom<T>(_ items: [T], _ n: Int) -> [T] {
    Array(items.shuffled().prefix(n))
}
