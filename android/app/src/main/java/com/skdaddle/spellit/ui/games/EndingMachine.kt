package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.skdaddle.spellit.data.WordData
import com.skdaddle.spellit.engine.GradeStore
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.pickRandom
import com.skdaddle.spellit.model.EndingTask
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.ChunkyButton
import com.skdaddle.spellit.ui.FeedbackPanel
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.GradePicker
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.SpellingField
import com.skdaddle.spellit.ui.Tile
import com.skdaddle.spellit.ui.TileLadder
import com.skdaddle.spellit.ui.TileRow
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.headingStyle

/** Room the "+" and "=" signs and their gaps claim on the equation line. */
private val OperatorRoom = 72.dp

@Composable
private fun Operator(glyph: String) {
    Text(
        glyph,
        style = headingStyle(20, FontWeight.SemiBold),
        color = Palette.MutedInk,
    )
}

@Composable
fun EndingMachineScreen(onManageLists: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { RoundEngine() }
    var grade by remember { mutableStateOf(GradeStore.read(context)) }
    var tasks by remember { mutableStateOf(listOf<EndingTask>()) }

    fun startRound() {
        val pool = WordData.endings[grade] ?: emptyList()
        val picked = pickRandom(pool, RoundEngine.ROUND_LENGTH)
        engine.startFixed(picked.map { WordEntry(word = it.word, hint = it.hint, sentence = "") })
        // Keep the tasks aligned with the engine's shuffled order.
        tasks = engine.words.mapNotNull { word -> pool.firstOrNull { it.word == word.word } }
    }

    LaunchedEffect(grade) { startRound() }

    GameScaffold(
        game = Game.ENDING_MACHINE,
        engine = engine,
        onRestart = { startRound() },
        picker = {
            GradePicker(
                grade = grade,
                onGradeChange = {
                    grade = it
                    GradeStore.save(context, it)
                },
            )
        },
    ) {
        if (engine.current != null && engine.index < tasks.size) {
            key(engine.roundId, engine.index) {
                EndingTaskChallenge(
                    task = tasks[engine.index],
                    isLast = engine.isLastWord,
                    onJudged = { engine.record(it) },
                    onNext = { engine.advance() },
                )
            }
        }
    }
}

@Composable
private fun EndingTaskChallenge(
    task: EndingTask,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }

    fun submit() {
        if (outcome != null) return
        val attempt = typed.trim().lowercase()
        if (attempt.isEmpty()) return
        if (attempt == task.word.lowercase() || attempt in task.also) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            retrying = true
            typed = ""
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val slots = task.base.length + task.suffix.length + 1
            val fit = TileLadder.fit(slots, maxWidth, OperatorRoom)
            // Once the equation no longer fits across one line, stack it rather
            // than let the words break wherever the row runs out of room.
            if (fit.perRow >= slots) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        fit.spacing * 2,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(fit.spacing)) {
                        for (letter in task.base) {
                            Tile(letter.toString(), size = fit.size, fill = Palette.SecondaryBg)
                        }
                    }
                    Operator("+")
                    Row(horizontalArrangement = Arrangement.spacedBy(fit.spacing)) {
                        for (letter in task.suffix) {
                            Tile(letter.toString(), size = fit.size, fill = Palette.SunSoft)
                        }
                    }
                    Operator("=")
                    Tile("?", size = fit.size, fill = Palette.SunSoft)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TileRow(count = task.base.length) { i, size ->
                        Tile(task.base[i].toString(), size = size, fill = Palette.SecondaryBg)
                    }
                    Operator("+")
                    TileRow(count = task.suffix.length) { i, size ->
                        Tile(task.suffix[i].toString(), size = size, fill = Palette.SunSoft)
                    }
                    Operator("=")
                    Tile("?", size = TileSize.MD, fill = Palette.SunSoft)
                }
            }
        }

        if (outcome == null) {
            SpellingField(
                placeholder = "What comes out?",
                text = typed,
                onTextChange = { typed = it },
                onSubmit = { submit() },
            )

            if (retrying) {
                Text(
                    "Not quite! Hint: ${task.hint}",
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Coral,
                    textAlign = TextAlign.Center,
                )
            }

            ChunkyButton(
                text = "Crank the machine",
                enabled = typed.trim().isNotEmpty(),
            ) { submit() }
        }

        outcome?.let { result ->
            Text(
                "Rule: ${task.hint}",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.SunSoft, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
            FeedbackPanel(correct = result, word = task.word, isLast = isLast, onNext = onNext)
        }
    }
}
