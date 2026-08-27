package com.skdaddle.spellit.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skdaddle.spellit.data.WordData
import com.skdaddle.spellit.engine.DailyStore
import com.skdaddle.spellit.engine.DailyStreak
import com.skdaddle.spellit.engine.GameHeuristics
import com.skdaddle.spellit.engine.GradeStore
import com.skdaddle.spellit.engine.Misspell
import com.skdaddle.spellit.engine.Mulberry32
import com.skdaddle.spellit.engine.RoundEngine
import com.skdaddle.spellit.engine.Speaker
import com.skdaddle.spellit.engine.seedHash
import com.skdaddle.spellit.engine.seededPick
import com.skdaddle.spellit.model.WordEntry
import com.skdaddle.spellit.ui.ChunkyButton
import com.skdaddle.spellit.ui.FeedbackPanel
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameScaffold
import com.skdaddle.spellit.ui.GradePicker
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.ShakeContainer
import com.skdaddle.spellit.ui.SpellingField
import com.skdaddle.spellit.ui.Tile
import com.skdaddle.spellit.ui.TileButton
import com.skdaddle.spellit.ui.TileSize
import com.skdaddle.spellit.ui.WordTiles
import com.skdaddle.spellit.ui.headingStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DailyMechanic(val label: String) {
    SCRAMBLE("Unscramble it!"),
    MISSING("Fill the gaps!"),
    SPOT("Spot the real spelling!"),
    FLASH("Memorize it!"),
}

@Composable
fun DailyBeeScreen(onManageLists: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { RoundEngine() }
    var grade by remember { mutableStateOf(GradeStore.read(context)) }
    var mechanicByWord by remember { mutableStateOf(mapOf<String, DailyMechanic>()) }
    var dailyDate by remember { mutableStateOf("") }
    var streak by remember { mutableStateOf(DailyStreak()) }
    var recordedFor by remember { mutableStateOf<String?>(null) }

    fun startRound() {
        val today = DailyStore.formatDate()
        dailyDate = today
        val rng = Mulberry32(seedHash("$today-${grade.seedKey}"))
        val pool = WordData.words[grade] ?: emptyList()
        val picked = seededPick(pool, RoundEngine.ROUND_LENGTH, rng)
        val mechanics = DailyMechanic.entries
        mechanicByWord = picked.associate { entry ->
            entry.word to mechanics[(rng.nextDouble() * mechanics.size).toInt()]
        }
        engine.startFixed(picked)
        streak = DailyStore.read(context)
    }

    LaunchedEffect(grade) { startRound() }

    LaunchedEffect(engine.phase) {
        if (engine.phase != RoundEngine.Phase.DONE) return@LaunchedEffect
        val today = DailyStore.formatDate()
        if (recordedFor == today) return@LaunchedEffect
        recordedFor = today
        streak = DailyStore.save(context, today)
    }

    // Roll over to the new day's round when the app comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                dailyDate.isNotEmpty() && DailyStore.formatDate() != dailyDate
            ) {
                startRound()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val summaryText = buildString {
        append("${engine.score} of ${engine.words.size} on today's challenge")
        if (streak.streak > 0) {
            append(" — ${streak.streak}-day streak (best: ${streak.best})")
        }
        append("!")
    }

    GameScaffold(
        game = Game.DAILY_BEE,
        engine = engine,
        summaryText = summaryText,
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
        val entry = engine.current
        val mechanic = entry?.let { mechanicByWord[it.word] }
        if (entry != null && mechanic != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DailyHeaderRow(streak = streak)
                Text(
                    mechanic.label.uppercase(),
                    style = headingStyle(13, FontWeight.SemiBold),
                    color = Palette.Sky,
                )
                key(engine.roundId, engine.index) {
                    when (mechanic) {
                        DailyMechanic.SCRAMBLE -> DailyScrambleChallenge(
                            entry = entry,
                            isLast = engine.isLastWord,
                            onJudged = { engine.record(it) },
                            onNext = { engine.advance() },
                        )
                        DailyMechanic.MISSING -> DailyMissingLettersChallenge(
                            entry = entry,
                            blanks = GameHeuristics.blanks(entry.word),
                            isLast = engine.isLastWord,
                            onJudged = { engine.record(it) },
                            onNext = { engine.advance() },
                        )
                        DailyMechanic.SPOT -> DailySpotChallenge(
                            entry = entry,
                            isLast = engine.isLastWord,
                            onJudged = { engine.record(it) },
                            onNext = { engine.advance() },
                        )
                        DailyMechanic.FLASH -> DailyFlashChallenge(
                            entry = entry,
                            showMs = GameHeuristics.flashMs(entry.word),
                            isLast = engine.isLastWord,
                            onJudged = { engine.record(it) },
                            onNext = { engine.advance() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyHeaderRow(streak: DailyStreak) {
    val dateLabel = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            dateLabel,
            style = headingStyle(13, FontWeight.Medium),
            color = Palette.MutedInk,
        )
        if (streak.streak > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Palette.Coral,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "${streak.streak}-day streak",
                    style = headingStyle(13, FontWeight.Medium),
                    color = Palette.Coral,
                )
            }
        }
    }
}

// Scramble mechanic (mirrors iOS ScrambleWordView)

/** Scramble a word's letters, guaranteed different when possible. */
private fun dailyScrambleLetters(word: String): List<Char> {
    val original = word.toList()
    repeat(20) {
        val mixed = original.shuffled()
        if (mixed.joinToString("") != word) return mixed
    }
    return original.reversed()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyScrambleChallenge(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val letters = remember(entry.word) { dailyScrambleLetters(entry.word) }
    var picked by remember { mutableStateOf(listOf<Int>()) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var shaking by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val tileSize = TileSize.forWord(entry.word)

    fun pick(index: Int) {
        if (outcome != null || picked.contains(index)) return
        val next = picked + index
        picked = next
        if (next.size < letters.size) return
        val attempt = next.map { letters[it] }.joinToString("")
        if (attempt == entry.word) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            shaking = true
            shakeTrigger += 1
            scope.launch {
                delay(650)
                shaking = false
                picked = emptyList()
                retrying = true
            }
        } else {
            outcome = false
            onJudged(false)
        }
    }

    fun unpick(position: Int) {
        if (outcome != null || shaking) return
        picked = picked.toMutableList().also { it.removeAt(position) }
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

        // Answer slots
        ShakeContainer(trigger = shakeTrigger) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in letters.indices) {
                    if (i < picked.size) {
                        TileButton(
                            letter = letters[picked[i]].toString(),
                            size = tileSize,
                            fill = when {
                                outcome == true -> Palette.LeafSoft
                                shaking -> Palette.CoralSoft
                                else -> Color.White
                            },
                        ) { unpick(i) }
                    } else {
                        Tile(letter = "", size = tileSize, dashed = true)
                    }
                }
            }
        }

        if (retrying && outcome == null) {
            Text(
                "Not quite — try again!",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.Coral,
            )
        }

        if (outcome == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in letters.indices) {
                    TileButton(
                        letter = letters[i].toString(),
                        size = tileSize,
                        fill = Palette.CoralSoft,
                        enabled = !(picked.contains(i) || shaking),
                    ) { pick(i) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(text = "Hear it", bordered = true) {
                    Speaker.shared(context).speak(entry.word)
                }
                ChunkyButton(
                    text = "Clear",
                    bordered = true,
                    enabled = picked.isNotEmpty() && !shaking,
                ) { picked = emptyList() }
            }
        }

        outcome?.let { result ->
            FeedbackPanel(correct = result, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}

// Missing-letters mechanic (mirrors iOS MissingLettersWordView)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyMissingLettersChallenge(
    entry: WordEntry,
    blanks: Int,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chars = entry.word.toList()
    val setup = remember(entry.word) {
        val positions = chars.indices.shuffled().take(minOf(blanks, chars.size)).sorted()
        val needed = positions.map { chars[it] }
        // Compare lowercased so a needed capital ("F" in February) can't draw
        // its lowercase twin as a distractor.
        val neededLower = needed.map { it.lowercaseChar() }.toSet()
        val distractors = ('a'..'z').filter { it !in neededLower }.shuffled().take(3)
        positions to (needed + distractors).shuffled()
    }
    val positions = setup.first
    val bank = setup.second
    var placed by remember { mutableStateOf(List<Int?>(positions.size) { null }) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var shaking by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val tileSize = TileSize.forWord(entry.word)

    fun pickFromBank(bankIndex: Int) {
        if (outcome != null || shaking || placed.contains(bankIndex)) return
        val firstEmpty = placed.indexOfFirst { it == null }
        if (firstEmpty < 0) return
        val next = placed.toMutableList().also { it[firstEmpty] = bankIndex }
        placed = next
        if (next.any { it == null }) return

        val correct = positions.withIndex().all { (i, pos) -> bank[next[i]!!] == chars[pos] }
        if (correct) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            shaking = true
            shakeTrigger += 1
            scope.launch {
                delay(650)
                shaking = false
                placed = List(positions.size) { null }
                retrying = true
            }
        } else {
            outcome = false
            onJudged(false)
        }
    }

    fun clearBlank(blankIndex: Int) {
        if (outcome != null || shaking) return
        placed = placed.toMutableList().also { it[blankIndex] = null }
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

        ShakeContainer(trigger = shakeTrigger) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (pos in chars.indices) {
                    val blankIndex = positions.indexOf(pos)
                    if (blankIndex >= 0) {
                        val bankIndex = placed.getOrNull(blankIndex)
                        if (bankIndex != null) {
                            TileButton(
                                letter = bank[bankIndex].toString(),
                                size = tileSize,
                                fill = when {
                                    outcome == true -> Palette.LeafSoft
                                    shaking -> Palette.CoralSoft
                                    else -> Palette.SunSoft
                                },
                            ) { clearBlank(blankIndex) }
                        } else {
                            Tile(letter = "", size = tileSize, fill = Palette.SunSoft, dashed = true)
                        }
                    } else {
                        Tile(
                            letter = chars[pos].toString(),
                            size = tileSize,
                            fill = Palette.SecondaryBg,
                        )
                    }
                }
            }
        }

        if (retrying && outcome == null) {
            Text(
                "Not quite — try again!",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.Coral,
            )
        }

        if (outcome == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in bank.indices) {
                    TileButton(
                        letter = bank[i].toString(),
                        size = TileSize.MD,
                        enabled = !(placed.contains(i) || shaking),
                    ) { pickFromBank(i) }
                }
            }

            ChunkyButton(text = "Hear it", bordered = true) {
                Speaker.shared(context).speak(entry.word)
            }
        }

        outcome?.let { result ->
            FeedbackPanel(correct = result, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}

// Spot-the-word mechanic (mirrors iOS SpotWordChallengeView)

@Composable
private fun DailySpotChallenge(
    entry: WordEntry,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val options = remember(entry.word) { (Misspell.make(entry.word, 3) + entry.word).shuffled() }
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
                        DailySpotOption(
                            option = option,
                            word = entry.word,
                            chosen = chosen,
                            shakeTrigger = shakeTrigger,
                            onChoose = { choose(option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowOptions.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        chosen?.let { picked ->
            FeedbackPanel(
                correct = picked == entry.word,
                word = entry.word,
                isLast = isLast,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun DailySpotOption(
    option: String,
    word: String,
    chosen: String?,
    shakeTrigger: Int,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReal = option == word
    val isPicked = option == chosen
    val revealed = chosen != null
    val shape = RoundedCornerShape(14.dp)
    ShakeContainer(
        trigger = if (revealed && isPicked && !isReal) shakeTrigger else 0,
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .alpha(if (revealed && !isReal && !isPicked) 0.4f else 1f),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        when {
                            revealed && isReal -> Palette.LeafSoft
                            revealed && isPicked -> Palette.CoralSoft
                            else -> Color.White
                        },
                        shape,
                    )
                    .border(3.dp, Palette.Ink, shape)
                    .clickable(enabled = !revealed) { onChoose() }
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
                    option,
                    style = headingStyle(19, FontWeight.Medium),
                    color = Palette.Ink,
                )
            }
        }
    }
}

// Flash mechanic (mirrors iOS FlashWordView)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyFlashChallenge(
    entry: WordEntry,
    showMs: Long,
    isLast: Boolean,
    onJudged: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var showing by remember { mutableStateOf(true) }
    var typed by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var retrying by remember { mutableStateOf(false) }
    var hideDelayMs by remember { mutableLongStateOf(showMs) }
    var flashPass by remember { mutableIntStateOf(0) }
    val tileSize = TileSize.forWord(entry.word)

    LaunchedEffect(flashPass) {
        delay(hideDelayMs)
        showing = false
    }

    fun submit() {
        if (outcome != null || showing) return
        val attempt = typed.trim().lowercase()
        if (attempt.isEmpty()) return
        if (attempt == entry.word.lowercase()) {
            outcome = true
            onJudged(true)
        } else if (!retrying) {
            retrying = true
            typed = ""
            // One more short look before the final try (iOS shows 2 seconds).
            hideDelayMs = 2000
            showing = true
            flashPass += 1
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
        entry.hint?.let { hint ->
            Text(
                "Clue: $hint",
                fontSize = 14.sp,
                color = Palette.MutedInk,
                textAlign = TextAlign.Center,
            )
        }

        if (showing) {
            WordTiles(word = entry.word, fill = Palette.CoralSoft, size = tileSize)
            Text(
                if (retrying) "One more look — you've got this!" else "Look closely… it's about to hide!",
                style = headingStyle(14, FontWeight.Medium),
                color = Palette.MutedInk,
            )
            ChunkyButton(text = "Hear it", bordered = true) {
                Speaker.shared(context).speak(entry.word)
            }
        }

        if (!showing && outcome == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(entry.word.length) {
                    Tile(letter = "", size = tileSize, dashed = true)
                }
            }

            SpellingField(
                placeholder = "Type it from memory…",
                text = typed,
                onTextChange = { typed = it },
                onSubmit = { submit() },
            )

            if (retrying) {
                Text(
                    "Not quite — try once more!",
                    style = headingStyle(14, FontWeight.Medium),
                    color = Palette.Coral,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChunkyButton(
                    text = "Check my spelling",
                    enabled = typed.trim().isNotEmpty(),
                ) { submit() }
                ChunkyButton(text = "Hear it", bordered = true) {
                    Speaker.shared(context).speak(entry.word)
                }
            }
        }

        outcome?.let { result ->
            FeedbackPanel(correct = result, word = entry.word, isLast = isLast, onNext = onNext)
        }
    }
}
