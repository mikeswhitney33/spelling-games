package com.skdaddle.spellit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.model.WordEntry

// Score bar

@Composable
fun ScoreBar(
    unit: String = "Word",
    index: Int,
    total: Int,
    score: Int,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$unit ${minOf(index + 1, total)} of $total",
                style = headingStyle(13, FontWeight.Medium),
                color = Palette.MutedInk,
            )
            Spacer(Modifier.weight(1f))
            if (streak >= 2) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Palette.Coral,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    " $streak in a row!  ",
                    style = headingStyle(13, FontWeight.Medium),
                    color = Palette.Coral,
                )
            }
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = Palette.Sun,
                modifier = Modifier.size(16.dp),
            )
            Text(
                " $score",
                style = headingStyle(13, FontWeight.Medium),
                color = Palette.Ink,
            )
        }
        LinearProgressIndicator(
            progress = { index.toFloat() / maxOf(total, 1) },
            color = Palette.Ink,
            trackColor = Palette.SecondaryBg,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape),
        )
    }
}

// Round summary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoundSummary(
    score: Int,
    total: Int,
    bestStreak: Int,
    missed: List<WordEntry>,
    summaryText: String? = null,
    onRestart: () -> Unit,
) {
    val stars = RoundEngine.stars(score, total)
    val headline = when (stars) {
        3 -> "Wow! Spelling superstar!"
        2 -> "Nice spelling! One more round?"
        1 -> "Good effort — try for more stars!"
        else -> "Keep practicing — you'll get there!"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 3) {
                val shape = RoundedCornerShape(14.dp)
                Box(
                    Modifier
                        .size(56.dp)
                        .rotate(if (i == 0) -6f else if (i == 2) 6f else 0f),
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .offset(y = 4.dp)
                            .background(Palette.Ink, shape),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(if (i < stars) Palette.SunSoft else Palette.SecondaryBg, shape)
                            .border(3.dp, Palette.Ink, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (i < stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (i < stars) Palette.Sun else Palette.MutedInk,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }

        Text(
            headline,
            style = headingStyle(22),
            color = Palette.Ink,
            textAlign = TextAlign.Center,
        )

        val defaultSummary = buildString {
            append("You spelled $score of $total words right")
            if (bestStreak >= 3) append(" — best streak: $bestStreak in a row")
            append(".")
        }
        Text(
            summaryText ?: defaultSummary,
            fontSize = 15.sp,
            color = Palette.MutedInk,
            textAlign = TextAlign.Center,
        )

        if (missed.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.SecondaryBg, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Words to practice",
                    style = headingStyle(13, FontWeight.Medium),
                    color = Palette.MutedInk,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (entry in missed) {
                        Text(
                            entry.word,
                            style = headingStyle(14, FontWeight.Medium),
                            color = Palette.Ink,
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(10.dp))
                                .border(2.dp, Palette.Ink, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        ChunkyButton(text = "Play again", onClick = onRestart)
    }
}

// Feedback panel (correct / reveal)

@Composable
fun FeedbackPanel(
    correct: Boolean,
    word: String,
    isLast: Boolean,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (correct) Palette.LeafSoft else Palette.CoralSoft,
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (correct) {
            Text("Nailed it! ⭐️", style = headingStyle(19), color = Palette.Ink)
        } else {
            Text("Almost! It's spelled:", style = headingStyle(17), color = Palette.Ink)
            WordTiles(word = word)
        }
        // A same-frame double-tap must not advance twice (skipping a word);
        // the panel leaves composition on advance, so this resets per word.
        val advanced = androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(false)
        }
        ChunkyButton(
            text = if (isLast) "See my score" else "Next word",
            onClick = {
                if (!advanced.value) {
                    advanced.value = true
                    onNext()
                }
            },
        )
    }
}

/** A word rendered as a wrapping row of tiles. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordTiles(
    word: String,
    modifier: Modifier = Modifier,
    fill: Color = Color.White,
    size: TileSize? = null,
) {
    val tileSize = size ?: TileSize.forWord(word)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (letter in word) {
            Tile(letter = letter.toString(), size = tileSize, fill = fill)
        }
    }
}

// Game scaffold

@Composable
fun GameScaffold(
    game: Game,
    engine: RoundEngine,
    unit: String = "Word",
    summaryText: String? = null,
    onRestart: () -> Unit,
    picker: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(Modifier.size(54.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(y = 4.dp)
                        .background(Palette.Ink, shape),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(game.accentSoft, shape)
                        .border(3.dp, Palette.Ink, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        game.icon,
                        contentDescription = null,
                        tint = Palette.Ink,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(game.title, style = headingStyle(26), color = Palette.Ink)
                Text(game.instructions, fontSize = 13.sp, color = Palette.MutedInk)
            }
        }

        picker()

        if (engine.phase == RoundEngine.Phase.PLAYING && engine.words.isNotEmpty()) {
            ScoreBar(
                unit = unit,
                index = engine.index,
                total = engine.words.size,
                score = engine.score,
                streak = engine.streak,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .background(Color.White, RoundedCornerShape(18.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(
                        game.accent,
                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    )
                    .align(Alignment.TopCenter),
            )
            Box(Modifier.padding(18.dp)) {
                if (engine.phase == RoundEngine.Phase.DONE) {
                    RoundSummary(
                        score = engine.score,
                        total = engine.words.size,
                        bestStreak = engine.bestStreak,
                        missed = engine.missedWords,
                        summaryText = summaryText,
                        onRestart = onRestart,
                    )
                } else {
                    content()
                }
            }
        }
    }
}
