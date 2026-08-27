package com.skdaddle.spellit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Game(
    val id: String,
    val title: String,
    val tagline: String,
    val blurb: String,
    val instructions: String,
) {
    DAILY_BEE(
        "dailyBee", "Daily Bee", "One round a day",
        "A fresh ten-word challenge every day, mixing all the games. Come back tomorrow to grow your streak!",
        "Ten words, a mix of every challenge — one fresh round each day.",
    ),
    WORD_SCRAMBLE(
        "wordScramble", "Word Scramble", "Untangle the tiles",
        "The letters got all mixed up! Tap the tiles to put the word back together.",
        "Tap the tiles in the right order to unscramble the word.",
    ),
    MISSING_LETTERS(
        "missingLetters", "Missing Letters", "Fill in the gaps",
        "Some letters ran away. Pick the right ones to finish the word.",
        "Some letters are missing. Tap letters from the bank to finish the word.",
    ),
    LISTEN_AND_SPELL(
        "listenAndSpell", "Listen & Spell", "Hear it, spell it",
        "Press play, listen closely, and type the whole word all by yourself.",
        "Press the speaker, listen closely, then type the word you hear.",
    ),
    SPOT_THE_WORD(
        "spotTheWord", "Spot the Word", "Find the real one",
        "One spelling is right and three are fakes. Can you spot the real word?",
        "Read the clue, then tap the one spelling that's really right.",
    ),
    FLASH_SPELL(
        "flashSpell", "Flash Spell", "Look, remember, spell",
        "The word flashes on screen, then hides. Spell it from memory!",
        "Look closely while the word is showing — then spell it from memory.",
    ),
    FIX_THE_SENTENCE(
        "fixTheSentence", "Fix the Sentence", "Find it, fix it",
        "One word in the sentence is spelled wrong. Hunt it down, then type the fix!",
        "One word is spelled wrong. Tap it, then type the correct spelling.",
    ),
    ENDING_MACHINE(
        "endingMachine", "Ending Machine", "Word math",
        "Feed a word and an ending into the machine. Watch out — some letters double, drop, or change!",
        "Add the ending to the word — watch for letters that double, drop, or change!",
    ),
    MINI_CROSSWORD(
        "miniCrossword", "Mini Crossword", "Five words, one grid",
        "A tiny crossword made from your spelling words. Use the clues to fill the grid!",
        "Use the clues to fill the grid — words cross and share letters.",
    ),
    WORD_SEARCH(
        "wordSearch", "Word Search", "Hidden word hunt",
        "Your spelling words are hiding in a grid of letters. Tap the first and last letter to catch one!",
        "Tap the first letter of a hidden word, then its last letter.",
    ),
    MEMORY_MATCH(
        "memoryMatch", "Memory Match", "Flip and remember",
        "Flip two cards at a time to pair each word with what it means. Fewer flips, more stars!",
        "Flip two cards at a time to match each word with its clue.",
    ),
    BALLOON_POP(
        "balloonPop", "Balloon Pop", "Save the balloons",
        "Guess letters one at a time — every miss pops a balloon. Spell the word before they're all gone!",
        "Pick letters to spell the hidden word — every wrong guess pops a balloon.",
    );

    val accent: Color
        get() = when (this) {
            DAILY_BEE, MISSING_LETTERS, ENDING_MACHINE -> Palette.Sun
            WORD_SCRAMBLE, FLASH_SPELL -> Palette.Coral
            LISTEN_AND_SPELL, FIX_THE_SENTENCE, MEMORY_MATCH -> Palette.Sky
            SPOT_THE_WORD, MINI_CROSSWORD -> Palette.Leaf
            WORD_SEARCH, BALLOON_POP -> Palette.Grape
        }

    val accentSoft: Color
        get() = when (this) {
            DAILY_BEE, MISSING_LETTERS, ENDING_MACHINE -> Palette.SunSoft
            WORD_SCRAMBLE, FLASH_SPELL -> Palette.CoralSoft
            LISTEN_AND_SPELL, FIX_THE_SENTENCE, MEMORY_MATCH -> Palette.SkySoft
            SPOT_THE_WORD, MINI_CROSSWORD -> Palette.LeafSoft
            WORD_SEARCH, BALLOON_POP -> Palette.GrapeSoft
        }

    val icon: ImageVector
        get() = when (this) {
            DAILY_BEE -> Icons.Filled.LocalFireDepartment
            WORD_SCRAMBLE -> Icons.Filled.Shuffle
            MISSING_LETTERS -> Icons.Filled.Extension
            LISTEN_AND_SPELL -> Icons.AutoMirrored.Filled.VolumeUp
            SPOT_THE_WORD -> Icons.Filled.Search
            FLASH_SPELL -> Icons.Filled.Visibility
            FIX_THE_SENTENCE -> Icons.Filled.Edit
            ENDING_MACHINE -> Icons.Filled.Settings
            MINI_CROSSWORD -> Icons.Filled.GridOn
            WORD_SEARCH -> Icons.Filled.Explore
            MEMORY_MATCH -> Icons.Filled.Psychology
            BALLOON_POP -> Icons.Filled.Celebration
        }

    companion object {
        fun fromId(id: String?): Game? = entries.firstOrNull { it.id == id }
    }
}
