package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.headingStyle

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

@OptIn(ExperimentalLayoutApi::class)
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
    val tileSize = TileSize.forWord(task.base + task.suffix)

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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (letter in task.base) {
                Tile(letter = letter.toString(), size = tileSize, fill = Palette.SecondaryBg)
            }
            Box(Modifier.height(tileSize.side), contentAlignment = Alignment.Center) {
                Text(
                    "+",
                    style = headingStyle(24, FontWeight.SemiBold),
                    color = Palette.MutedInk,
                )
            }
            for (letter in task.suffix) {
                Tile(letter = letter.toString(), size = tileSize, fill = Palette.SunSoft)
            }
            Box(Modifier.height(tileSize.side), contentAlignment = Alignment.Center) {
                Text(
                    "=",
                    style = headingStyle(24, FontWeight.SemiBold),
                    color = Palette.MutedInk,
                )
            }
            Tile(letter = "?", size = tileSize, fill = Palette.SunSoft)
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
