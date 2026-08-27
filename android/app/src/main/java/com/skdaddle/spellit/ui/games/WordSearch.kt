package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.GridCell
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.WordSearchGenerator
import com.skdaddle.spellit.engine.WordSearchPuzzle
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.RoundSummary
import com.skdaddle.spellit.ui.headingStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val scope = rememberCoroutineScope()
    val pool = store.activeBank.entries.filter { it.word.length in 3..12 }

    var puzzle by remember { mutableStateOf<WordSearchPuzzle?>(null) }
    var found by remember { mutableStateOf(setOf<String>()) }
    var firstTap by remember { mutableStateOf<GridCell?>(null) }
    var flashCells by remember { mutableStateOf(setOf<GridCell>()) }
    var flashJob by remember { mutableStateOf<Job?>(null) }

    fun startRound() {
        flashJob?.cancel()
        if (pool.size < 5) {
            puzzle = null
            engine.clear()
            return
        }
        puzzle = WordSearchGenerator.generate(pool)
        found = emptySet()
        firstTap = null
        flashCells = emptySet()
    }

    fun flash(cells: List<GridCell>) {
        flashJob?.cancel()
        flashCells = cells.toSet()
        flashJob = scope.launch {
            delay(500)
            flashCells = emptySet()
        }
    }

    fun lineBetween(a: GridCell, b: GridCell): List<GridCell>? {
        val dr = (b.row - a.row).sign
        val dc = (b.col - a.col).sign
        val steps = max(abs(b.row - a.row), abs(b.col - a.col))
        if (dr != 0 && dc != 0 && abs(b.row - a.row) != abs(b.col - a.col)) return null
        return (0..steps).map { GridCell(row = a.row + dr * it, col = a.col + dc * it) }
    }

    fun tap(cell: GridCell) {
        val p = puzzle ?: return
        val start = firstTap
        if (start == null) {
            firstTap = cell
            return
        }
        if (start == cell) {
            firstTap = null
            return
        }
        firstTap = null
        val line = lineBetween(start, cell)
        if (line == null) {
            flash(listOf(start, cell))
            return
        }
        val hit = p.placements.firstOrNull { it.cells == line || it.cells == line.reversed() }
        if (hit != null) {
            if (hit.word !in found) found = found + hit.word
        } else {
            flash(line)
        }
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }

    GameScaffold(
        game = Game.WORD_SEARCH,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 5) {
            NotEnoughWords(5, "words of 3–12 letters", onManageLists)
        } else {
            val p = puzzle
            if (p != null) {
                if (found.size == p.placements.size) {
                    RoundSummary(
                        score = p.placements.size,
                        total = p.placements.size,
                        bestStreak = 0,
                        missed = emptyList(),
                        summaryText = "You found every hidden word — great hunting!",
                        onRestart = { startRound() },
                    )
                } else {
                    val foundCells = p.placements
                        .filter { it.word in found }
                        .flatMap { it.cells }
                        .toSet()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cellSide = maxWidth / p.size
                            Column {
                                for (r in 0 until p.size) {
                                    Row {
                                        for (c in 0 until p.size) {
                                            val cell = GridCell(row = r, col = c)
                                            val fill = when {
                                                cell in flashCells -> Palette.CoralSoft
                                                firstTap == cell -> Palette.SkySoft
                                                cell in foundCells -> Palette.LeafSoft
                                                else -> Color.Transparent
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(cellSide)
                                                    .clickable { tap(cell) },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Box(
                                                    Modifier
                                                        .matchParentSize()
                                                        .padding(1.dp)
                                                        .background(fill, RoundedCornerShape(6.dp)),
                                                )
                                                Text(
                                                    p.grid[r][c].uppercase(),
                                                    style = headingStyle(15, FontWeight.Medium),
                                                    color = Palette.Ink,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            if (firstTap == null) "Tap the FIRST letter of a word you spot."
                            else "Now tap the LAST letter of that word.",
                            style = headingStyle(13, FontWeight.Medium),
                            color = Palette.MutedInk,
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (placement in p.placements) {
                                val isFound = placement.word in found
                                val chipShape = RoundedCornerShape(10.dp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            if (isFound) Palette.LeafSoft else Color.White,
                                            chipShape,
                                        )
                                        .border(
                                            2.dp,
                                            if (isFound) Palette.Leaf else Palette.Ink,
                                            chipShape,
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    if (isFound) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Palette.Leaf,
                                            modifier = Modifier.size(11.dp),
                                        )
                                    }
                                    Text(
                                        placement.word,
                                        style = headingStyle(14, FontWeight.Medium),
                                        color = if (isFound) Palette.MutedInk else Palette.Ink,
                                        textDecoration = if (isFound) TextDecoration.LineThrough else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
