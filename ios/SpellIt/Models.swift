import SwiftUI

enum GradeBand: String, CaseIterable, Identifiable, Codable {
    case k1
    case g23
    case g45
    case g6plus

    var id: String { rawValue }

    var label: String {
        switch self {
        case .k1: "Grades K–1"
        case .g23: "Grades 2–3"
        case .g45: "Grades 4–5"
        case .g6plus: "Grades 6+"
        }
    }

    var short: String {
        switch self {
        case .k1: "K–1"
        case .g23: "2–3"
        case .g45: "4–5"
        case .g6plus: "6+"
        }
    }

    var blurb: String {
        switch self {
        case .k1: "Short, sound-it-out words like cat, sun, and hop."
        case .g23: "Everyday words with tricky parts, like friend and because."
        case .g45: "Longer words people often misspell, like separate and library."
        case .g6plus: "Challenge words like rhythm, committee, and mischievous."
        }
    }

    /// Seed component matching the web app's grade ids, so the Daily Bee
    /// serves the same words on both platforms.
    var seedKey: String {
        switch self {
        case .k1: "k-1"
        case .g23: "2-3"
        case .g45: "4-5"
        case .g6plus: "6-plus"
        }
    }
}

struct WordEntry: Hashable, Identifiable {
    let word: String
    let hint: String
    let sentence: String
    var id: String { word }
}

struct EndingTask: Hashable, Identifiable {
    let base: String
    let suffix: String
    let word: String
    let hint: String
    let also: [String]
    var id: String { word }
}

enum Game: String, CaseIterable, Identifiable {
    case dailyBee
    case wordScramble
    case missingLetters
    case listenAndSpell
    case spotTheWord
    case flashSpell
    case fixTheSentence
    case endingMachine
    case miniCrossword
    case wordSearch
    case memoryMatch
    case balloonPop

    var id: String { rawValue }

    var title: String {
        switch self {
        case .dailyBee: "Daily Bee"
        case .wordScramble: "Word Scramble"
        case .missingLetters: "Missing Letters"
        case .listenAndSpell: "Listen & Spell"
        case .spotTheWord: "Spot the Word"
        case .flashSpell: "Flash Spell"
        case .fixTheSentence: "Fix the Sentence"
        case .endingMachine: "Ending Machine"
        case .miniCrossword: "Mini Crossword"
        case .wordSearch: "Word Search"
        case .memoryMatch: "Memory Match"
        case .balloonPop: "Balloon Pop"
        }
    }

    var tagline: String {
        switch self {
        case .dailyBee: "One round a day"
        case .wordScramble: "Untangle the tiles"
        case .missingLetters: "Fill in the gaps"
        case .listenAndSpell: "Hear it, spell it"
        case .spotTheWord: "Find the real one"
        case .flashSpell: "Look, remember, spell"
        case .fixTheSentence: "Find it, fix it"
        case .endingMachine: "Word math"
        case .miniCrossword: "Five words, one grid"
        case .wordSearch: "Hidden word hunt"
        case .memoryMatch: "Flip and remember"
        case .balloonPop: "Save the balloons"
        }
    }

    var blurb: String {
        switch self {
        case .dailyBee:
            "A fresh ten-word challenge every day, mixing all the games. Come back tomorrow to grow your streak!"
        case .wordScramble:
            "The letters got all mixed up! Tap the tiles to put the word back together."
        case .missingLetters:
            "Some letters ran away. Pick the right ones to finish the word."
        case .listenAndSpell:
            "Press play, listen closely, and type the whole word all by yourself."
        case .spotTheWord:
            "One spelling is right and three are fakes. Can you spot the real word?"
        case .flashSpell:
            "The word flashes on screen, then hides. Spell it from memory!"
        case .fixTheSentence:
            "One word in the sentence is spelled wrong. Hunt it down, then type the fix!"
        case .endingMachine:
            "Feed a word and an ending into the machine. Watch out — some letters double, drop, or change!"
        case .miniCrossword:
            "A tiny crossword made from your spelling words. Use the clues to fill the grid!"
        case .wordSearch:
            "Your spelling words are hiding in a grid of letters. Tap the first and last letter to catch one!"
        case .memoryMatch:
            "Flip two cards at a time to pair each word with what it means. Fewer flips, more stars!"
        case .balloonPop:
            "Guess letters one at a time — every miss pops a balloon. Spell the word before they're all gone!"
        }
    }

    var symbol: String {
        switch self {
        case .dailyBee: "flame.fill"
        case .wordScramble: "shuffle"
        case .missingLetters: "puzzlepiece.fill"
        case .listenAndSpell: "speaker.wave.2.fill"
        case .spotTheWord: "magnifyingglass"
        case .flashSpell: "eye.fill"
        case .fixTheSentence: "pencil"
        case .endingMachine: "gearshape.fill"
        case .miniCrossword: "square.grid.3x3.fill"
        case .wordSearch: "safari.fill"
        case .memoryMatch: "brain.head.profile"
        case .balloonPop: "balloon.2.fill"
        }
    }

    var accent: Color {
        switch self {
        case .dailyBee, .missingLetters, .endingMachine: .sun
        case .wordScramble, .flashSpell: .coral
        case .listenAndSpell, .fixTheSentence, .memoryMatch: .sky
        case .spotTheWord, .miniCrossword: .leaf
        case .wordSearch, .balloonPop: .grape
        }
    }

    var accentSoft: Color {
        switch self {
        case .dailyBee, .missingLetters, .endingMachine: .sunSoft
        case .wordScramble, .flashSpell: .coralSoft
        case .listenAndSpell, .fixTheSentence, .memoryMatch: .skySoft
        case .spotTheWord, .miniCrossword: .leafSoft
        case .wordSearch, .balloonPop: .grapeSoft
        }
    }

    var instructions: String {
        switch self {
        case .dailyBee: "Ten words, a mix of every challenge — one fresh round each day."
        case .wordScramble: "Tap the tiles in the right order to unscramble the word."
        case .missingLetters: "Some letters are missing. Tap letters from the bank to finish the word."
        case .listenAndSpell: "Press the speaker, listen closely, then type the word you hear."
        case .spotTheWord: "Read the clue, then tap the one spelling that's really right."
        case .flashSpell: "Look closely while the word is showing — then spell it from memory."
        case .fixTheSentence: "One word is spelled wrong. Tap it, then type the correct spelling."
        case .endingMachine: "Add the ending to the word — watch for letters that double, drop, or change!"
        case .miniCrossword: "Use the clues to fill the grid — words cross and share letters."
        case .wordSearch: "Tap the first letter of a hidden word, then its last letter."
        case .memoryMatch: "Flip two cards at a time to match each word with its clue."
        case .balloonPop: "Pick letters to spell the hidden word — every wrong guess pops a balloon."
        }
    }
}
