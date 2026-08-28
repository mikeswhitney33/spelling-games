package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.engine.CrosswordDirection
import com.skdaddle.spellit.engine.CrosswordGenerator
import com.skdaddle.spellit.engine.CrosswordPuzzle
import com.skdaddle.spellit.engine.GridCell
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.BankPicker
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.NotEnoughWords
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.RoundSummary
import com.skdaddle.spellit.ui.ShakeContainer
import com.skdaddle.spellit.ui.headingStyle
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The placements covering a cell, plus the clue number shown in its corner. */
private fun cellInfo(cell: GridCell, puzzle: CrosswordPuzzle): Pair<List<Int>, Int?> {
    val indices = mutableListOf<Int>()
    var number: Int? = null
    puzzle.placements.forEachIndexed { i, p ->
        if (cell in p.cells) {
            indices.add(i)
            if (p.cells.firstOrNull() == cell) number = minOf(number ?: p.number, p.number)
        }
    }
    return indices to number
}

/** Clue order for the prev/next chevrons: across by number, then down. */
private fun orderedClueIndices(puzzle: CrosswordPuzzle): List<Int> {
    val indexed = puzzle.placements.withIndex()
    val across = indexed.filter { it.value.dir == CrosswordDirection.ACROSS }.sortedBy { it.value.number }
    val down = indexed.filter { it.value.dir == CrosswordDirection.DOWN }.sortedBy { it.value.number }
    return (across + down).map { it.index }
}

/** The next clue after [index] in cycle order, preferring unsolved ones. */
private fun nextClueIndex(index: Int, step: Int, puzzle: CrosswordPuzzle, solved: Set<Int>): Int {
    val order = orderedClueIndices(puzzle)
    val at = order.indexOf(index)
    if (at < 0 || order.size <= 1) return index
    for (offset in 1 until order.size) {
        val wrapped = ((at + step * offset) % order.size + order.size) % order.size
        val candidate = order[wrapped]
        if (candidate !in solved) return candidate
    }
    return index
}

@Composable
fun MiniCrosswordScreen(store: BankStore, onManageLists: () -> Unit) {
    val engine = remember { RoundEngine() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val pool = store.activeBank.entries.filter {
        it.hint != null && it.word.length >= 3 && it.word.length <= CrosswordGenerator.MAX_WORD_LENGTH
    }

    var puzzle by remember { mutableStateOf<CrosswordPuzzle?>(null) }
    var letters by remember { mutableStateOf(mapOf<GridCell, Char>()) }
    var solved by remember { mutableStateOf(setOf<Int>()) }
    // First-fill correctness per placement index (absent until attempted).
    var results by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    var selected by remember { mutableIntStateOf(0) }
    var activeCell by remember { mutableStateOf<GridCell?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var shakingIndex by remember { mutableStateOf<Int?>(null) }
    // The hidden field holds a sentinel space, so a real backspace (which
    // deletes the space) is observable as a change to "".
    var buffer by remember { mutableStateOf(TextFieldValue(" ", TextRange(1))) }

    fun isLocked(cell: GridCell, p: CrosswordPuzzle): Boolean =
        cellInfo(cell, p).first.any { it in solved }

    fun isFinished(p: CrosswordPuzzle): Boolean = solved.size == p.placements.size

    fun startRound() {
        if (pool.size < 5) {
            puzzle = null
            engine.clear()
            return
        }
        val p = CrosswordGenerator.generate(pool)
        puzzle = p
        letters = emptyMap()
        solved = emptySet()
        results = emptyMap()
        selected = 0
        activeCell = p.placements.firstOrNull()?.cells?.firstOrNull()
        shakingIndex = null
        buffer = TextFieldValue(" ", TextRange(1))
    }

    fun selectClue(index: Int, p: CrosswordPuzzle) {
        selected = index
        val cells = p.placements[index].cells
        activeCell = cells.firstOrNull { letters[it] == null && !isLocked(it, p) } ?: cells.firstOrNull()
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun tapCell(cell: GridCell, p: CrosswordPuzzle) {
        val info = cellInfo(cell, p)
        if (info.first.isEmpty()) return
        // Prefer words that can still be played over solved ones.
        val playable = info.first.filter { it !in solved }
        if (activeCell == cell && info.first.size > 1) {
            // Re-tapping a crossing flips to the other playable word.
            playable.firstOrNull { it != selected }?.let { selected = it }
        } else if (selected !in info.first) {
            selected = playable.firstOrNull { p.placements[it].dir == CrosswordDirection.ACROSS }
                ?: playable.firstOrNull()
                ?: info.first[0]
        }
        activeCell = cell
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun moveActive(from: GridCell, delta: Int, p: CrosswordPuzzle) {
        val cells = p.placements.getOrNull(selected)?.cells ?: return
        var at = cells.indexOf(from)
        if (at < 0) return
        at += delta
        while (at in cells.indices) {
            if (!isLocked(cells[at], p)) {
                activeCell = cells[at]
                return
            }
            at += delta
        }
    }

    fun checkWords(changed: GridCell, p: CrosswordPuzzle) {
        for ((index, placement) in p.placements.withIndex()) {
            if (index in solved) continue
            val cells = placement.cells
            val filled = cells.mapNotNull { letters[it] }
            if (filled.size != cells.size) continue
            val attempt = filled.joinToString("")
            if (attempt == placement.word.lowercase()) {
                solved = solved + index
                if (results[index] == null) results = results + (index to true)
                if (solved.size == p.placements.size) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                } else if (index == selected) {
                    // Let the green lock land, then hop to the next unsolved clue.
                    // The target is recomputed after the delay: the same
                    // keystroke may have solved several words at once.
                    scope.launch {
                        delay(600)
                        if (isFinished(p) || index !in solved || selected != index) return@launch
                        val next = nextClueIndex(index, 1, p, solved)
                        if (next == index || next in solved) return@launch
                        selectClue(next, p)
                    }
                }
            } else if (index == selected && changed in cells && results[index] == null) {
                results = results + (index to false)
                shakingIndex = index
                shakeTrigger += 1
            }
        }
    }

    fun handleTyped(value: String, p: CrosswordPuzzle) {
        val cell = activeCell ?: return
        if (value.isEmpty()) {
            // The sentinel space was deleted: a real backspace.
            if (!isLocked(cell, p) && letters[cell] != null) {
                letters = letters - cell
            } else {
                moveActive(cell, -1, p)
                val prev = activeCell
                if (prev != null && !isLocked(prev, p)) {
                    letters = letters - prev
                }
            }
            return
        }
        val ch = value.lowercase().lastOrNull() ?: return
        if (!ch.isLetter()) return
        if (!isLocked(cell, p)) {
            letters = letters + (cell to ch)
            checkWords(cell, p)
        }
        moveActive(cell, 1, p)
    }

    LaunchedEffect(store.activeId, store.revision) { startRound() }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        delay(500)
        shakingIndex = null
    }

    GameScaffold(
        game = Game.MINI_CROSSWORD,
        engine = engine,
        onRestart = { startRound() },
        picker = { BankPicker(store, onManageLists) },
    ) {
        if (pool.size < 5) {
            NotEnoughWords(5, "words with hints", onManageLists)
        } else {
            val p = puzzle
            if (p != null) {
                if (isFinished(p)) {
                    RoundSummary(
                        score = results.values.count { it },
                        total = p.placements.size,
                        bestStreak = 0,
                        missed = p.placements.withIndex()
                            .filter { results[it.index] == false }
                            .map { WordEntry(word = it.value.word, hint = it.value.hint, sentence = "") },
                        summaryText = null,
                        onRestart = { startRound() },
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Hidden field that drives the system keyboard for cell entry.
                        BasicTextField(
                            value = buffer,
                            onValueChange = { newValue ->
                                if (newValue.text != " ") handleTyped(newValue.text, p)
                                buffer = TextFieldValue(" ", TextRange(1))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .size(1.dp)
                                .alpha(0.02f)
                                .focusRequester(focusRequester),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.None,
                            ),
                        )

                        val selectedCells = p.placements.getOrNull(selected)?.cells?.toSet() ?: emptySet()
                        val shakeCells = shakingIndex
                            ?.let { idx -> p.placements.getOrNull(idx)?.cells?.toSet() }
                            ?: emptySet()

                        ShakeContainer(trigger = shakeTrigger, modifier = Modifier.fillMaxWidth()) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val side = maxWidth / max(p.width, 1)
                                Column {
                                    for (r in 0 until p.height) {
                                        Row {
                                            for (c in 0 until p.width) {
                                                val cell = GridCell(row = r, col = c)
                                                val info = cellInfo(cell, p)
                                                if (info.first.isEmpty()) {
                                                    Box(Modifier.size(side))
                                                } else {
                                                    CrosswordCellView(
                                                        letter = letters[cell],
                                                        number = info.second,
                                                        locked = isLocked(cell, p),
                                                        isSelected = cell in selectedCells,
                                                        isActive = activeCell == cell,
                                                        shaking = cell in shakeCells,
                                                        side = side,
                                                        onClick = { tapCell(cell, p) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // iOS pins this above the keyboard so the active clue is
                        // always readable while typing; here it sits by the grid.
                        val crossing = activeCell?.let { cell ->
                            // Offer the swap only when the crossing word is still playable.
                            cellInfo(cell, p).first.any { it != selected && it !in solved }
                        } ?: false
                        ClueBar(
                            puzzle = p,
                            selected = selected,
                            crossing = crossing,
                            onPrev = { selectClue(nextClueIndex(selected, -1, p, solved), p) },
                            onNext = { selectClue(nextClueIndex(selected, 1, p, solved), p) },
                            onSwap = { activeCell?.let { tapCell(it, p) } },
                            // Tapping the clue re-focuses the word's first empty cell.
                            onTapClue = { selectClue(selected, p) },
                        )

                        CluesList(
                            puzzle = p,
                            solved = solved,
                            selected = selected,
                            onSelectClue = { selectClue(it, p) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosswordCellView(
    letter: Char?,
    number: Int?,
    locked: Boolean,
    isSelected: Boolean,
    isActive: Boolean,
    shaking: Boolean,
    side: Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    val fill = when {
        shaking -> Palette.CoralSoft
        locked -> Palette.LeafSoft
        isSelected -> Palette.SkySoft
        else -> Color.White
    }
    val activeBorder = isActive && !locked
    Box(
        modifier = Modifier
            .size(side)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(1.dp)
                .background(fill, shape)
                .border(
                    if (activeBorder) 2.5.dp else 1.5.dp,
                    if (activeBorder) Palette.Sky else Palette.Ink,
                    shape,
                ),
        )
        if (number != null) {
            Text(
                "$number",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.MutedInk,
                modifier = Modifier.padding(top = 2.dp, start = 3.dp),
            )
        }
        Text(
            letter?.uppercase() ?: "",
            style = headingStyle(16),
            color = Palette.Ink,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ClueBar(
    puzzle: CrosswordPuzzle,
    selected: Int,
    crossing: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSwap: () -> Unit,
    onTapClue: () -> Unit,
) {
    val placement = puzzle.placements.getOrNull(selected)
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.SkySoft, shape)
            .border(2.5.dp, Palette.Ink, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "Previous clue",
                tint = Palette.Ink,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTapClue),
        ) {
            if (placement != null) {
                Text(
                    "${placement.number} ${if (placement.dir == CrosswordDirection.ACROSS) "ACROSS" else "DOWN"} · ${placement.word.length} letters",
                    style = headingStyle(11),
                    color = Palette.MutedInk,
                )
                Text(
                    placement.hint,
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (crossing) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSwap),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "Switch to the crossing clue",
                    tint = Palette.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(
            Modifier
                .size(width = 36.dp, height = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next clue",
                tint = Palette.Ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun CluesList(
    puzzle: CrosswordPuzzle,
    solved: Set<Int>,
    selected: Int,
    onSelectClue: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (dir in listOf(CrosswordDirection.ACROSS, CrosswordDirection.DOWN)) {
            val clues = puzzle.placements.withIndex()
                .filter { it.value.dir == dir }
                .sortedBy { it.value.number }
            if (clues.isEmpty()) continue
            Text(
                if (dir == CrosswordDirection.ACROSS) "ACROSS" else "DOWN",
                style = headingStyle(12),
                color = Palette.MutedInk,
            )
            for ((index, p) in clues) {
                val isSolved = index in solved
                val textColor = if (isSolved) Palette.MutedInk else Palette.Ink
                val rowShape = RoundedCornerShape(10.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(
                            if (selected == index && !isSolved) Palette.SkySoft else Color.Transparent,
                            rowShape,
                        )
                        .clickable { onSelectClue(index) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        "${p.number}.",
                        style = headingStyle(14),
                        color = textColor,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        "${p.hint} (${p.word.length} letters)",
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                    )
                    if (isSolved) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Palette.Leaf,
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}
