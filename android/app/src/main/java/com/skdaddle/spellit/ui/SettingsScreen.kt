package com.skdaddle.spellit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.data.WordData
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.model.WordBank
import com.skdaddle.spellit.model.WordEntry

/**
 * Word-list manager: pick the active list, build custom lists, duplicate
 * built-ins as starting points.
 */
@Composable
fun SettingsScreen(
    store: BankStore,
    onBack: () -> Unit,
    onEditBank: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Palette.Ink)
            }
            Text("Word lists", style = headingStyle(24), color = Palette.Ink)
        }
        Text(
            "Every game draws from one list at a time. A hint powers the clue games; a sentence (using the word once) powers Fix the Sentence.",
            fontSize = 13.sp,
            color = Palette.MutedInk,
        )

        SectionTitle("My lists")
        for (bank in store.customBanks) {
            BankRow(bank = bank, store = store, onEditBank = onEditBank)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clickable {
                    val bank = store.createBank()
                    onEditBank(bank.id)
                }
                .padding(vertical = 6.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(18.dp))
            Text("New list", style = headingStyle(15, FontWeight.Medium), color = Palette.Ink)
        }

        SectionTitle("Built-in lists")
        for (bank in WordData.builtInBanks) {
            BankRow(bank = bank, store = store, onEditBank = onEditBank)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = headingStyle(13, FontWeight.Medium),
        color = Palette.MutedInk,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun BankRow(
    bank: WordBank,
    store: BankStore,
    onEditBank: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(2.dp, Palette.SoftBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(bank.displayName, style = headingStyle(15), color = Palette.Ink)
                Text(
                    "${bank.entries.size} words" +
                        if (bank.blurb.isEmpty()) "" else " · ${bank.blurb}",
                    fontSize = 12.sp,
                    color = Palette.MutedInk,
                )
            }
            if (bank.id == store.activeId) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Palette.Leaf,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    " In use",
                    style = headingStyle(12, FontWeight.Medium),
                    color = Palette.Leaf,
                )
            } else {
                ChunkyButton(text = "Use", bordered = true, onClick = { store.setActive(bank.id) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!bank.builtIn) {
                RowAction(Icons.Filled.Edit, "Edit") { onEditBank(bank.id) }
                RowAction(Icons.Filled.Delete, "Delete", tint = Palette.Coral) {
                    store.deleteBank(bank.id)
                }
            } else {
                RowAction(Icons.Filled.ContentCopy, "Duplicate") {
                    val copy = store.createBank(
                        name = "${bank.name} (my copy)",
                        entries = bank.entries,
                    )
                    onEditBank(copy.id)
                }
            }
        }
    }
}

@Composable
private fun RowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Palette.MutedInk,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, style = headingStyle(12, FontWeight.Medium), color = tint)
    }
}

// Bank editor

@Composable
fun BankEditorScreen(
    store: BankStore,
    bankId: String,
    onBack: () -> Unit,
) {
    val bank = store.customBanks.firstOrNull { it.id == bankId }

    var word by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var sentence by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Palette.Ink)
            }
            Text(bank?.displayName ?: "List", style = headingStyle(24), color = Palette.Ink)
        }

        if (bank == null) {
            Text("This list is gone.", color = Palette.MutedInk)
            return@Column
        }

        SectionTitle("List name")
        EditorField(value = bank.name, placeholder = "List name") { store.rename(bankId, it) }

        SectionTitle("Add a word")
        EditorField(value = word, placeholder = "Word (required)") { word = it }
        EditorField(value = hint, placeholder = "Hint (optional)") { hint = it }
        EditorField(value = sentence, placeholder = "Sentence (optional, uses the word once)") { sentence = it }
        error?.let {
            Text(it, style = headingStyle(13, FontWeight.Medium), color = Palette.Coral)
        }
        ChunkyButton(
            text = "Add word",
            enabled = word.trim().isNotEmpty(),
            onClick = {
                error = addWord(store, bank, word, hint, sentence)
                if (error == null) {
                    word = ""
                    hint = ""
                    sentence = ""
                }
            },
        )

        if (bank.entries.size < 6) {
            Text(
                "Tip: games work best with at least 6–10 words. Hints unlock Mini Crossword and Memory Match; sentences unlock Fix the Sentence.",
                fontSize = 12.sp,
                color = Palette.MutedInk,
            )
        }

        SectionTitle("Words (${bank.entries.size})")
        bank.entries.forEachIndexed { index, entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, Palette.SoftBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(entry.word, style = headingStyle(15), color = Palette.Ink)
                    entry.hint?.let { Text(it, fontSize = 12.sp, color = Palette.MutedInk) }
                    entry.sentence?.let {
                        Text(it, fontSize = 12.sp, color = Palette.MutedInk)
                    }
                }
                IconButton(onClick = { store.removeWord(index, bankId) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "remove", tint = Palette.Coral, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            color = Palette.Ink,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(2.dp, Palette.SoftBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 15.sp, color = Palette.MutedInk.copy(alpha = 0.7f))
            }
            inner()
        },
    )
}

/** Returns an error message, or null if the word was added. */
private fun addWord(
    store: BankStore,
    bank: WordBank,
    word: String,
    hint: String,
    sentence: String,
): String? {
    val trimmed = word.trim()
    if (!Regex("^[A-Za-z]{2,20}$").matches(trimmed)) {
        return "Words are 2–20 letters, no spaces or symbols."
    }
    if (trimmed.lowercase() in WordData.blockedWords) {
        return "Let's pick a different word."
    }
    if (bank.entries.any { it.word.lowercase() == trimmed.lowercase() }) {
        return "That word is already in this list."
    }
    val trimmedSentence = sentence.trim()
    if (trimmedSentence.isNotEmpty() && !sentenceUsesWordOnce(trimmed, trimmedSentence)) {
        return "The sentence needs to use the word exactly once, on its own."
    }
    val trimmedHint = hint.trim()
    store.addWord(
        WordEntry(
            word = trimmed.lowercase(),
            hint = trimmedHint.ifEmpty { null },
            sentence = trimmedSentence.ifEmpty { null },
        ),
        bank.id,
    )
    return null
}

/**
 * Tokenizes exactly like Fix the Sentence, so "well-known" never counts
 * as "well".
 */
fun sentenceUsesWordOnce(word: String, sentence: String): Boolean {
    fun isWordChar(c: Char) = c.isLetter() || c == '\''
    val cores = sentence.split(" ").map { raw ->
        val prefix = raw.takeWhile { !isWordChar(it) }
        val suffix = raw.reversed().takeWhile { !isWordChar(it) }.reversed()
        raw.drop(prefix.length).dropLast(suffix.length)
    }
    return cores.count { it.lowercase() == word.lowercase() } == 1
}
