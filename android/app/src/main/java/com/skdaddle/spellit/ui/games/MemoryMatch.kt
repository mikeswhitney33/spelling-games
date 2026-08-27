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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.pickRandom
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.RoundSummary
import com.skdaddle.spellit.ui.headingStyle
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAIR_COUNT = 6

private data class MemoryCard(
    val id: Int,
    val pairId: Int,
    val isWord: Boolean,
    val text: String,
)

@Composable
fun MemoryMatchScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val scope = rememberCoroutineScope()
    val pool = store.activeBank.entries.filter { it.hint != null }

    var cards by remember { mutableStateOf(listOf<MemoryCard>()) }
    var faceUp by remember { mutableStateOf(listOf<Int>()) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var attempts by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var resolveJob by remember { mutableStateOf<Job?>(null) }

    fun startRound() {
        resolveJob?.cancel()
        if (pool.size < PAIR_COUNT) {
            cards = emptyList()
            engine.clear()
            return
        }
        cards = pickRandom(pool, PAIR_COUNT).flatMapIndexed { pairId, entry ->
            listOf(
                MemoryCard(id = pairId * 2, pairId = pairId, isWord = true, text = entry.word),
                MemoryCard(id = pairId * 2 + 1, pairId = pairId, isWord = false, text = entry.hint ?: ""),
            )
        }.shuffled()
        faceUp = emptyList()
        matched = emptySet()
        attempts = 0
        finished = false
    }

    fun flip(card: MemoryCard) {
        if (faceUp.size >= 2 || card.id in faceUp || card.pairId in matched) return
        faceUp = faceUp + card.id
        if (faceUp.size < 2) return

        attempts += 1
        val flipped = faceUp.mapNotNull { id -> cards.firstOrNull { it.id == id } }
        if (flipped.size == 2 && flipped[0].pairId == flipped[1].pairId) {
            matched = matched + flipped[0].pairId
            faceUp = emptyList()
            if (matched.size == PAIR_COUNT) finished = true
        } else {
            resolveJob = scope.launch {
                delay(1100)
                faceUp = emptyList()
            }
        }
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    val scoreForAttempts = when {
        attempts <= PAIR_COUNT + 2 -> PAIR_COUNT
        attempts <= PAIR_COUNT + 6 -> ceil(PAIR_COUNT * 0.75).toInt()
        attempts <= PAIR_COUNT + 11 -> ceil(PAIR_COUNT * 0.6).toInt()
        else -> ceil(PAIR_COUNT * 0.5).toInt()
    }

    GameScaffold(
        game = Game.MEMORY_MATCH,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        when {
            pool.size < PAIR_COUNT -> NotEnoughWords(PAIR_COUNT, "words with hints", onManageLists)

            finished -> RoundSummary(
                score = scoreForAttempts,
                total = PAIR_COUNT,
                bestStreak = 0,
                missed = emptyList(),
                summaryText =
                    "You matched all $PAIR_COUNT pairs in $attempts ${if (attempts == 1) "try" else "tries"}!",
                onRestart = { startRound() },
            )

            else -> {
                val statusText = when {
                    attempts == 0 && faceUp.isEmpty() -> "Flip a card to start!"
                    attempts == 0 -> "Now find its partner!"
                    else ->
                        "${matched.size} of $PAIR_COUNT pairs matched · $attempts ${if (attempts == 1) "try" else "tries"}"
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (rowCards in cards.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (card in rowCards) {
                                    MemoryCardView(
                                        card = card,
                                        isMatched = card.pairId in matched,
                                        isUp = card.pairId in matched || card.id in faceUp,
                                        onClick = { flip(card) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowCards.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Text(
                        statusText,
                        style = headingStyle(13, FontWeight.Medium),
                        color = Palette.MutedInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    isMatched: Boolean,
    isUp: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "cardPress",
    )
    Box(
        modifier = modifier
            .alpha(if (isMatched) 0.8f else 1f)
            .offset { IntOffset(0, press.dp.roundToPx()) },
    ) {
        if (!isUp) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .background(Palette.Ink, shape),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 92.dp)
                .background(
                    when {
                        isMatched -> Palette.LeafSoft
                        isUp -> Palette.SkySoft
                        else -> Palette.Ink
                    },
                    shape,
                )
                .border(3.dp, Palette.Ink, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isUp) {
                if (card.isWord) {
                    Text(
                        card.text,
                        style = headingStyle(17),
                        color = Palette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp),
                    )
                } else {
                    Text(
                        card.text,
                        fontSize = 11.sp,
                        color = Palette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            } else {
                Text("?", style = headingStyle(26), color = Color.White)
            }
        }
    }
}
