package com.skdaddle.spellit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
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

// Fitting a word onto one line

/** How a run of tiles should sit inside a measured width. */
data class TileFit(val size: TileSize, val spacing: Dp, val perRow: Int)

/**
 * Fixed tile sizes wrap a word wherever the row happens to run out of room,
 * which leaves orphan letters on the next line and is hard to read. A fit
 * shrinks the tiles so the whole word stays on one line, and only splits — into
 * even rows — once that would push the letters below a legible size, so
 * "pronunciation" reads as 7 + 6 rather than 9 + 4.
 */
object TileLadder {
    /** Full-size tile; matches [TileSize.MD]. */
    private val MaxSide = 48.dp
    /** Smallest tile a young reader can still read comfortably. */
    private val FloorSide = 28.dp
    /** Only reached when even three rows will not fit. */
    private val HardMinSide = 18.dp
    /** Gaps tighten alongside the tiles so narrow screens buy back some room. */
    private val RoomySpacing = 6.dp
    private val TightSpacing = 4.dp
    /** Past this, splitting the word does more harm than shrinking it. */
    private const val MaxRows = 3

    /**
     * Lays [count] tiles into [width], reserving [extra] for anything else
     * sharing the line (the "+" and "=" in Ending Machine).
     */
    fun fit(count: Int, width: Dp, extra: Dp = 0.dp): TileFit {
        if (count <= 0) return TileFit(TileSize.MD, RoomySpacing, 1)
        // Before the row is measured, fall back to the static heuristic.
        if (!width.isSpecified || !width.value.isFinite() || width <= 0.dp) {
            return TileFit(TileSize.forCount(count), RoomySpacing, count)
        }
        for (rows in 1..MaxRows) {
            val perRow = ceil(count.toDouble() / rows).toInt()
            val (side, spacing) = sideFor(perRow, width, extra)
            if (side >= FloorSide || rows == MaxRows) {
                return TileFit(TileSize.of(maxOf(HardMinSide, side)), spacing, perRow)
            }
        }
        return TileFit(TileSize.forCount(count), RoomySpacing, count)
    }

    private fun sideFor(perRow: Int, width: Dp, extra: Dp): Pair<Dp, Dp> {
        val roomy = (width - extra - RoomySpacing * (perRow - 1)) / perRow
        if (roomy >= 36.dp) return minOf(MaxSide, roomy) to RoomySpacing
        val tight = (width - extra - TightSpacing * (perRow - 1)) / perRow
        return minOf(MaxSide, tight) to TightSpacing
    }
}

/**
 * A run of tiles sized to fit the width it is given, split into even rows only
 * when the tiles would otherwise be too small to read.
 */
@Composable
fun TileRow(
    count: Int,
    modifier: Modifier = Modifier,
    tile: @Composable (index: Int, size: TileSize) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fit = TileLadder.fit(count, maxWidth)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(fit.spacing),
        ) {
            for (start in 0 until count step fit.perRow) {
                key(start) {
                    Row(horizontalArrangement = Arrangement.spacedBy(fit.spacing)) {
                        for (index in start until minOf(start + fit.perRow, count)) {
                            key(index) { tile(index, fit.size) }
                        }
                    }
                }
            }
        }
    }
}

/** A word rendered as a row of tiles that fits the space it is given. */
@Composable
fun WordTiles(
    word: String,
    modifier: Modifier = Modifier,
    fill: Color = Color.White,
) {
    TileRow(count = word.length, modifier = modifier) { index, size ->
        Tile(letter = word[index].toString(), size = size, fill = fill)
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
