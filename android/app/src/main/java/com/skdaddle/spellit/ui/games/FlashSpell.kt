package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.skdaddle.spellit.ui.SpellingField
import com.skdaddle.spellit.ui.Tile
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.WordTiles
import com.skdaddle.spellit.ui.headingStyle
import kotlinx.coroutines.delay

@Composable
fun FlashSpellScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.FLASH_SPELL,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 4) {
            NotEnoughWords(need = 4, requirement = "words", onManageLists = onManageLists)
        } else {
            val entry = engine.current
            if (entry != null) {
                key(engine.roundId, engine.index) {
                    FlashWord(
                        entry = entry,
                        showMs = GameHeuristics.flashMs(entry.word),
                        isLast = engine.isLastWord,
                        onJudged = { engine.record(it) },
                        onNext = { engine.advance() },
                    )
                }
            }
        }
    }
}

private enum class FlashPhase { SHOW, TYPE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashWord(
    entry: WordEntry,
    showMs: Long,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(FlashPhase.SHOW) }
    var typed by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    val size = TileSize.forWord(entry.word)

    // Mirrors the iOS scheduleHide: the fresh look on a retry lasts 2 seconds.
    LaunchedEffect(retrying) {
        delay(if (retrying) 2000L else showMs)
        phase = FlashPhase.TYPE
    }

    fun submit() {
        if (outcome != null || phase != FlashPhase.TYPE) return
        val attempt = typed.trim().lowercase()
        if (attempt.isEmpty()) return
        if (attempt == entry.word.lowercase()) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            retrying = true
            typed = ""
            phase = FlashPhase.SHOW
        } else {
            outcome = false
            onJudged(false)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        entry.hint?.let { hint ->
            Text(
                "Clue: $hint",
                fontSize = 14.sp,
                color = Palette.MutedInk,
                textAlign = TextAlign.Center,
            )
        }

        if (phase == FlashPhase.SHOW) {
            WordTiles(word = entry.word, fill = Palette.CoralSoft, size = size)
            Text(
                if (retrying) "One more look — you've got this!" else "Look closely… it's about to hide!",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.MutedInk,
            )
            ChunkyButton(text = "Hear it", bordered = true, onClick = {
                Speaker.shared(context).speak(entry.word)
            })
        }

        if (phase == FlashPhase.TYPE && outcome == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(entry.word.length) {
                    Tile(letter = "", size = size, dashed = true)
                }
            }

            SpellingField(
                placeholder = "Type it from memory…",
                text = typed,
                onTextChange = { typed = it },
                onSubmit = { submit() },
            )

            if (retrying) {
                Text(
                    "Not quite — try once more!",
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Coral,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    text = "Check my spelling",
                    enabled = typed.trim().isNotEmpty(),
                    onClick = { submit() },
                )
                ChunkyButton(text = "Hear it", bordered = true, onClick = {
                    Speaker.shared(context).speak(entry.word)
                })
            }
        }

        outcome?.let {
            FeedbackPanel(correct = it, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}
