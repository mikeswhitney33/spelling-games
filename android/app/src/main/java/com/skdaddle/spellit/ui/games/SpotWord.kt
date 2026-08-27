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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.Misspell
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.FeedbackPanel
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.ShakeContainer
import com.skdaddle.spellit.ui.headingStyle

@Composable
fun SpotWordScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val pool = store.activeBank.entries

    fun startRound() {
        if (pool.size >= 4) engine.start(pool) else engine.clear()
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.SPOT_THE_WORD,
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
                    SpotWordChallenge(
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
fun SpotWordChallenge(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val options = remember(entry) {
        (Misspell.make(entry.word, 3) + entry.word).shuffled()
    }
    var chosen by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    fun choose(option: String) {
        if (chosen != null) return
        chosen = option
        if (option != entry.word) shakeTrigger += 1
        onJudged(option == entry.word)
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (rowOptions in options.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (option in rowOptions) {
                        val isReal = option == entry.word
                        val isPicked = option == chosen
                        val revealed = chosen != null
                        ShakeContainer(
                            trigger = if (revealed && isPicked && !isReal) shakeTrigger else 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            SpotOption(
                                text = option,
                                revealed = revealed,
                                isReal = isReal,
                                isPicked = isPicked,
                                onClick = { choose(option) },
                            )
                        }
                    }
                    if (rowOptions.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        chosen?.let {
            FeedbackPanel(
                correct = it == entry.word,
                word = entry.word,
                isLast = isLast,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun SpotOption(
    text: String,
    revealed: Boolean,
    isReal: Boolean,
    isPicked: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "optionPress",
    )
    val fill = when {
        revealed && isReal -> Palette.LeafSoft
        revealed && isPicked -> Palette.CoralSoft
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (revealed && !isReal && !isPicked) 0.4f else 1f)
            .offset { IntOffset(0, press.dp.roundToPx()) },
    ) {
        if (!revealed) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .background(Palette.Ink, shape),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(fill, shape)
                .border(3.dp, Palette.Ink, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !revealed,
                    onClick = onClick,
                )
                .padding(vertical = 16.dp),
        ) {
            if (revealed && isReal) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Palette.Leaf,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (revealed && isPicked && !isReal) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Palette.Coral,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text,
                style = headingStyle(19, FontWeight.Medium),
                color = Palette.Ink,
            )
        }
    }
}
