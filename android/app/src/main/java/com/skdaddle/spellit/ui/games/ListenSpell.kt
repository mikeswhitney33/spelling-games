package com.skdaddle.spellit.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import com.skdaddle.spellit.ui.SpellingField
import com.skdaddle.spellit.ui.headingStyle

@Composable
fun ListenSpellScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.LISTEN_AND_SPELL,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 4) {
            NotEnoughWords(need = 4, requirement = "words", onManageLists = onManageLists)
        } else {
            val entry = engine.current
            if (entry != null) {
                key(engine.roundId, engine.index, store.activeId) {
                    ListenWord(
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

@Composable
private fun ListenWord(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var typed by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    fun submit() {
        if (outcome != null) return
        val attempt = typed.trim().lowercase()
        if (attempt.isEmpty()) return
        if (attempt == entry.word.lowercase()) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            retrying = true
            typed = ""
            Speaker.shared(context).speak(entry.word)
        } else {
            outcome = false
            onJudged(false)
        }
    }

    LaunchedEffect(entry.word) { Speaker.shared(context).speak(entry.word) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpeakerButton { Speaker.shared(context).speak(entry.word) }

        Text(
            "Tap the speaker to hear your word.",
            fontSize = 13.sp,
            color = Palette.MutedInk,
        )

        if (outcome == null) {
            SpellingField(
                placeholder = "Type the word…",
                text = typed,
                onTextChange = { typed = it },
                onSubmit = { submit() },
            )

            if (retrying) {
                Text(
                    "Not quite — listen again and give it one more try!",
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Coral,
                    textAlign = TextAlign.Center,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    text = "Check my spelling",
                    enabled = typed.trim().isNotEmpty(),
                ) {
                    submit()
                }
                if (entry.hint != null) {
                    ChunkyButton(text = "Clue", bordered = true, enabled = !showHint) {
                        showHint = true
                    }
                }
            }

            val hint = entry.hint
            if (showHint && hint != null) {
                Text(
                    "Clue: $hint",
                    fontSize = 14.sp,
                    color = Palette.MutedInk,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val done = outcome
        if (done != null) {
            FeedbackPanel(correct = done, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}

/** The big tappable speaker, with the same press-down as the tiles. */
@Composable
private fun SpeakerButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "speakerPress",
    )
    Box(
        Modifier
            .size(96.dp)
            .offset { IntOffset(0, press.dp.roundToPx()) },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(Palette.Ink, shape),
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(Palette.SkySoft, shape)
                .border(3.5.dp, Palette.Ink, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Play the word out loud",
                tint = Palette.Ink,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}
