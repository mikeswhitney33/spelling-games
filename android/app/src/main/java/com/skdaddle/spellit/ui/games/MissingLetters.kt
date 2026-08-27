package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.GameHeuristics
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
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.headingStyle
import kotlinx.coroutines.delay

@Composable
fun MissingLettersScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries.filter { it.word.length >= 3 }

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.MISSING_LETTERS,
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
                key(engine.roundId, engine.index) {
                    MissingLettersWord(
                        entry = entry,
                        blanks = GameHeuristics.blanks(entry.word),
                        isLast = engine.isLastWord,
                        onJudged = { engine.record(it) },
                        onNext = { engine.advance() },
                    )
                }
            }
        }
    }
}

private class MissingSetup(
    val positions: List<Int>,
    val bank: List<Char>,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissingLettersWord(
    entry: WordEntry,
    blanks: Int,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val chars = remember(entry.word) { entry.word.toList() }
    val setup = remember(entry.word) {
        val blankCount = minOf(blanks, chars.size)
        val positions = chars.indices.shuffled().take(blankCount).sorted()
        val needed = positions.map { chars[it] }
        // Compare lowercased so a needed capital ("F" in February) can't draw
        // its lowercase twin as a distractor.
        val neededLower = needed.map { it.lowercaseChar() }.toSet()
        val distractors = ('a'..'z').filter { it !in neededLower }.shuffled().take(3)
        MissingSetup(positions = positions, bank = (needed + distractors).shuffled())
    }
    // For each blank, the bank index placed there.
    var placed by remember { mutableStateOf<List<Int?>>(List(setup.positions.size) { null }) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var shaking by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val size = TileSize.forWord(entry.word)

    fun pickFromBank(bankIndex: Int) {
        if (outcome != null || shaking || placed.contains(bankIndex)) return
        val firstEmpty = placed.indexOfFirst { it == null }
        if (firstEmpty < 0) return
        val next = placed.toMutableList().also { it[firstEmpty] = bankIndex }
        placed = next
        if (next.any { it == null }) return

        val correct = setup.positions.withIndex().all { (i, pos) ->
            setup.bank[next[i]!!] == chars[pos]
        }
        if (correct) {
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

    fun clearBlank(blankIndex: Int) {
        if (outcome != null || shaking) return
        placed = placed.toMutableList().also { it[blankIndex] = null }
    }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        delay(650)
        shaking = false
        placed = List(setup.positions.size) { null }
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

        ShakeContainer(trigger = shakeTrigger) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (pos in chars.indices) {
                    val blankIndex = setup.positions.indexOf(pos)
                    if (blankIndex >= 0) {
                        val bankIndex = placed.getOrNull(blankIndex)
                        if (bankIndex != null) {
                            TileButton(
                                letter = setup.bank[bankIndex].toString(),
                                size = size,
                                fill = when {
                                    outcome == true -> Palette.LeafSoft
                                    shaking -> Palette.CoralSoft
                                    else -> Palette.SunSoft
                                },
                            ) {
                                clearBlank(blankIndex)
                            }
                        } else {
                            Tile(letter = "", size = size, fill = Palette.SunSoft, dashed = true)
                        }
                    } else {
                        Tile(letter = chars[pos].toString(), size = size, fill = Palette.SecondaryBg)
                    }
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in setup.bank.indices) {
                    TileButton(
                        letter = setup.bank[i].toString(),
                        size = TileSize.MD,
                        enabled = !(placed.contains(i) || shaking),
                    ) {
                        pickFromBank(i)
                    }
                }
            }

            ChunkyButton(text = "Hear it", bordered = true) {
                Speaker.shared(context).speak(entry.word)
            }
        }

        val done = outcome
        if (done != null) {
            FeedbackPanel(correct = done, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}
