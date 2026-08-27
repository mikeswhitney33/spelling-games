package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.Misspell
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.matchCase
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.ChunkyButton
import com.skdaddle.spellit.ui.FeedbackPanel
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.ShakeContainer
import com.skdaddle.spellit.ui.SpellingField
import com.skdaddle.spellit.ui.headingStyle

@Composable
fun FixSentenceScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries.filter { it.sentence != null }

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.FIX_THE_SENTENCE,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 4) {
            NotEnoughWords(need = 4, requirement = "words with sentences", onManageLists = onManageLists)
        } else {
            val entry = engine.current
            if (entry != null) {
                key(engine.roundId, engine.index) {
                    FixSentenceWord(
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

data class SentenceToken(
    val id: Int,
    val prefix: String,
    val core: String,
    val suffix: String,
    val isTarget: Boolean,
    /** The core as displayed — the planted misspelling for the target. */
    val shown: String,
)

fun tokenizeSentence(sentence: String, target: String): List<SentenceToken> {
    var targetFound = false
    fun isWordChar(c: Char) = c.isLetter() || c == '\''
    return sentence.split(" ")
        .filter { it.isNotEmpty() }
        .mapIndexed { index, text ->
            val prefix = text.takeWhile { !isWordChar(it) }
            val suffix = text.reversed().takeWhile { !isWordChar(it) }.reversed()
            val core = text.drop(prefix.length).dropLast(suffix.length)
            val isTarget = !targetFound && core.lowercase() == target.lowercase()
            if (isTarget) targetFound = true
            SentenceToken(
                id = index, prefix = prefix, core = core, suffix = suffix,
                isTarget = isTarget, shown = core,
            )
        }
}

private enum class FixStage { FIND, FIX }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixSentenceWord(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    var tokens by remember { mutableStateOf<List<SentenceToken>>(emptyList()) }
    var stage by remember { mutableStateOf(FixStage.FIND) }
    var typed by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var findMisses by remember { mutableIntStateOf(0) }
    var lastMissWord by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(entry) {
        if (tokens.isNotEmpty()) return@LaunchedEffect
        val parsed = tokenizeSentence(entry.sentence ?: "", entry.word).toMutableList()
        val targetIndex = parsed.indexOfFirst { it.isTarget }
        if (targetIndex >= 0) {
            val lower = entry.word.lowercase()
            val fake = Misspell.make(entry.word, 1).firstOrNull() ?: (lower + lower.last())
            parsed[targetIndex] = parsed[targetIndex].copy(
                shown = matchCase(model = parsed[targetIndex].core, text = fake),
            )
            tokens = parsed
        } else {
            // Data safety net: sentence lacks its own word — skip with credit.
            tokens = parsed
            onJudged(true)
            onNext()
        }
    }

    fun tap(token: SentenceToken) {
        if (stage != FixStage.FIND) return
        if (token.isTarget) {
            stage = FixStage.FIX
        } else {
            findMisses += 1
            lastMissWord = token.shown
            shakeTrigger += 1
        }
    }

    fun submit() {
        if (outcome != null || stage != FixStage.FIX) return
        val attempt = typed.trim().lowercase()
        if (attempt.isEmpty()) return
        if (attempt == entry.word.lowercase()) {
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

    val findStatus = if (findMisses == 0) {
        "Tap the word that's spelled wrong."
    } else {
        val missed = lastMissWord?.let { "\"$it\" is spelled fine" } ?: "That one's spelled fine"
        if (findMisses == 1) {
            "$missed — keep hunting!"
        } else {
            val hint = tokens.firstOrNull { it.isTarget }?.shown?.firstOrNull()?.toString() ?: "?"
            "$missed. Psst — the wrong word starts with \"$hint\"."
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (token in tokens) {
                SentenceTokenView(
                    token = token,
                    stage = stage,
                    outcome = outcome,
                    findMisses = findMisses,
                    lastMissWord = lastMissWord,
                    shakeTrigger = shakeTrigger,
                    entryWord = entry.word,
                    onTap = { tap(token) },
                )
            }
        }

        if (stage == FixStage.FIND) {
            Text(
                findStatus,
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.MutedInk,
                textAlign = TextAlign.Center,
            )
        }

        if (stage == FixStage.FIX && outcome == null) {
            Text(
                "You found it! Now type it the right way.",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.MutedInk,
            )
            SpellingField(
                placeholder = "Type the fix…",
                text = typed,
                onTextChange = { typed = it },
                onSubmit = { submit() },
            )
            if (retrying) {
                Text(
                    "Not quite — look at the clue and try again!",
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Coral,
                )
            }
            entry.hint?.let { hint ->
                Text(
                    "Clue: $hint",
                    fontSize = 14.sp,
                    color = Palette.MutedInk,
                    textAlign = TextAlign.Center,
                )
            }
            ChunkyButton(
                text = "Fix it",
                enabled = typed.trim().isNotEmpty(),
                onClick = { submit() },
            )
        }

        outcome?.let {
            FeedbackPanel(correct = it, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}

@Composable
private fun SentenceTokenView(
    token: SentenceToken,
    stage: FixStage,
    outcome: Boolean?,
    findMisses: Int,
    lastMissWord: String?,
    shakeTrigger: Int,
    entryWord: String,
    onTap: () -> Unit,
) {
    val highlighted = token.isTarget && findMisses >= 2 && stage == FixStage.FIND
    val struck = token.isTarget && stage == FixStage.FIX && outcome == null
    val fixed = token.isTarget && outcome != null
    val text = if (fixed) matchCase(model = token.shown, text = entryWord.lowercase()) else token.shown
    val fill = when {
        fixed -> Palette.LeafSoft
        struck -> Palette.CoralSoft
        highlighted -> Palette.SkySoft
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(8.dp)
    ShakeContainer(trigger = if (lastMissWord == token.shown) shakeTrigger else 0) {
        Text(
            token.prefix + text + token.suffix,
            fontSize = 19.sp,
            fontWeight = if (token.isTarget && stage != FixStage.FIND) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
            color = Palette.Ink,
            modifier = Modifier
                .clip(shape)
                .background(fill, shape)
                .clickable(
                    enabled = stage == FixStage.FIND && outcome == null,
                    onClick = onTap,
                )
                .padding(horizontal = 4.dp, vertical = 3.dp),
        )
    }
}
