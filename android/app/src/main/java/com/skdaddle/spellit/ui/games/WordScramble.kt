package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.Speaker
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.ChunkyButton
import com.skdaddle.spellit.ui.FeedbackPanel
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.ShakeContainer
import com.skdaddle.spellit.ui.Tile
import com.skdaddle.spellit.ui.TileButton
import com.skdaddle.spellit.ui.TileRow
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.headingStyle
import kotlinx.coroutines.delay

@Composable
fun WordScrambleScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries.filter { it.word.length >= 3 }

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.WORD_SCRAMBLE,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 4) {
            NotEnoughWords(
                need = 4,
                requirement = "words of three or more letters",
                onManageLists = onManageLists,
            )
        } else {
            val entry = engine.current
            if (entry != null) {
                key(engine.roundId, engine.index, store.activeId) {
                    ScrambleWord(
                        entry = entry,
                        isLast = engine.isLastWord,
                        onJudged = { engine.record(it) },
                        onNext = { engine.advance() },
                    )
                }
            }
        }
    }
}

/** Scramble a word's letters, guaranteed different when possible. */
private fun scrambleLetters(word: String): List<Char> {
    val original = word.toList()
    repeat(20) {
        val mixed = original.shuffled()
        if (mixed.joinToString("") != word) return mixed
    }
    return original.reversed()
}

@Composable
private fun ScrambleWord(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val letters = remember(entry.word) { scrambleLetters(entry.word) }
    var picked by remember { mutableStateOf<List<Int>>(emptyList()) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var shaking by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    fun pick(index: Int) {
        if (outcome != null || picked.contains(index)) return
        val next = picked + index
        picked = next
        if (next.size < letters.size) return
        val attempt = next.map { letters[it] }.joinToString("")
        if (attempt == entry.word) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            shaking = true
            shakeTrigger += 1
        } else {
            outcome = false
            onJudged(false)
        }
    }

    fun unpick(position: Int) {
        if (outcome != null || shaking) return
        picked = picked.toMutableList().also { it.removeAt(position) }
    }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        delay(650)
        shaking = false
        picked = emptyList()
        retrying = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val hint = entry.hint
        if (hint != null) {
            Text(
                "Clue: $hint",
                fontSize = 14.sp,
                color = Palette.MutedInk,
                textAlign = TextAlign.Center,
            )
        }

        // Answer slots
        ShakeContainer(trigger = shakeTrigger) {
            TileRow(count = letters.size) { i, size ->
                if (i < picked.size) {
                    TileButton(
                        letter = letters[picked[i]].toString(),
                        size = size,
                        fill = when {
                            outcome == true -> Palette.LeafSoft
                            shaking -> Palette.CoralSoft
                            else -> Color.White
                        },
                    ) {
                        unpick(i)
                    }
                } else {
                    Tile(letter = "", size = size, dashed = true)
                }
            }
        }

        if (retrying && outcome == null) {
            Text(
                "Not quite — try again!",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.Coral,
            )
        }

        if (outcome == null) {
            TileRow(count = letters.size) { i, size ->
                TileButton(
                    letter = letters[i].toString(),
                    size = size,
                    fill = Palette.CoralSoft,
                    enabled = !(picked.contains(i) || shaking),
                ) {
                    pick(i)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(text = "Hear it", bordered = true) {
                    Speaker.shared(context).speak(entry.word)
                }
                ChunkyButton(
                    text = "Clear",
                    bordered = true,
                    enabled = picked.isNotEmpty() && !shaking,
                ) {
                    picked = emptyList()
                }
            }
        }

        val done = outcome
        if (done != null) {
            FeedbackPanel(correct = done, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}
