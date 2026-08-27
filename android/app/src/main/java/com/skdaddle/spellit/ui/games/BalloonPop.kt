package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.skdaddle.spellit.ui.TileSize

@Composable
fun BalloonPopScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }

    fun startRound() {
        val pool = store.activeBank.entries
        if (pool.size >= 4) {
            engine.start(pool)
        } else {
            engine.clear()
        }
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.BALLOON_POP,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        val pool = store.activeBank.entries
        if (pool.size < 4) {
            NotEnoughWords(need = 4, requirement = "words", onManageLists = onManageLists)
        } else {
            val entry = engine.current
            if (entry != null) {
                key(engine.roundId, engine.index, store.activeId) {
                    BalloonWordChallenge(
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

private const val MAX_MISSES = 6

private val balloonColors = listOf(
    Palette.Coral, Palette.Sun, Palette.Leaf, Palette.Sky, Palette.Grape, Palette.Coral,
)

private val balloonAlphabet = ('a'..'z').toList()

@Composable
private fun BalloonShape(popped: Boolean, color: Color) {
    Box(Modifier.size(34.dp, 52.dp), contentAlignment = Alignment.Center) {
        if (popped) {
            Icon(
                Icons.Filled.Flare,
                contentDescription = null,
                tint = Palette.MutedInk.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        } else {
            Canvas(Modifier.size(34.dp, 52.dp)) {
                val balloonW = 30.dp.toPx()
                val balloonH = 38.dp.toPx()
                val knotW = 10.dp.toPx()
                val knotH = 7.dp.toPx()
                val overlap = 2.dp.toPx()
                val stroke = 2.5.dp.toPx()
                val top = (size.height - (balloonH + knotH - overlap)) / 2f
                val left = (size.width - balloonW) / 2f

                drawOval(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(balloonW, balloonH),
                )
                drawOval(
                    color = Palette.Ink,
                    topLeft = Offset(left + stroke / 2f, top + stroke / 2f),
                    size = Size(balloonW - stroke, balloonH - stroke),
                    style = Stroke(width = stroke),
                )
                val knotTop = top + balloonH - overlap
                val cx = size.width / 2f
                val knot = Path().apply {
                    moveTo(cx, knotTop)
                    lineTo(cx - knotW / 2f, knotTop + knotH)
                    lineTo(cx + knotW / 2f, knotTop + knotH)
                    close()
                }
                drawPath(knot, color)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BalloonWordChallenge(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var guessed by remember { mutableStateOf(setOf<Char>()) }
    var misses by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val tileSize = TileSize.forWord(entry.word)
    val balloonsLeft = MAX_MISSES - misses
    // Guesses are lowercase a–z; compare against the lowercased word so
    // capitalized entries like "February" stay winnable.
    val word = entry.word.lowercase()

    fun guess(letter: Char) {
        if (outcome != null || letter in guessed) return
        guessed = guessed + letter
        if (letter in word) {
            if (word.all { it in guessed }) {
                outcome = true
                onJudged(true)
            }
        } else {
            misses += 1
            shakeTrigger += 1
            if (misses >= MAX_MISSES) {
                outcome = false
                onJudged(false)
            }
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "$balloonsLeft of $MAX_MISSES balloons left"
            },
        ) {
            for (i in 0 until MAX_MISSES) {
                BalloonShape(popped = i >= balloonsLeft, color = balloonColors[i])
            }
        }

        ShakeContainer(trigger = shakeTrigger) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (letter in word) {
                    val revealed = letter in guessed || outcome != null
                    val wasGuessed = letter in guessed
                    Tile(
                        letter = if (revealed) letter.toString() else "",
                        size = tileSize,
                        fill = when {
                            outcome == true -> Palette.LeafSoft
                            revealed && wasGuessed -> Palette.GrapeSoft
                            revealed -> Palette.CoralSoft
                            else -> Palette.GrapeSoft.copy(alpha = 0.5f)
                        },
                        dashed = !revealed,
                    )
                }
            }
        }

        if (outcome == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (letter in balloonAlphabet) {
                    val used = letter in guessed
                    val hit = used && letter in word
                    TileButton(
                        letter = letter.toString(),
                        size = TileSize.SM,
                        fill = when {
                            used && hit -> Palette.LeafSoft
                            used -> Palette.CoralSoft
                            else -> Color.White
                        },
                        enabled = !used,
                    ) { guess(letter) }
                }
            }

            ChunkyButton(text = "Hear it", bordered = true) {
                Speaker.shared(context).speak(entry.word)
            }
        }

        outcome?.let { result ->
            FeedbackPanel(correct = result, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}
