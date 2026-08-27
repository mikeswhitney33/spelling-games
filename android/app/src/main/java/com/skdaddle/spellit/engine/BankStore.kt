package com.skdaddle.spellit.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skdaddle.spellit.data.WordData
import com.skdaddle.spellit.model.WordBank
import com.skdaddle.spellit.model.WordEntry
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Custom word banks + the active-bank choice, persisted in SharedPreferences. */
class BankStore private constructor(private val prefs: SharedPreferences) {
    companion object {
        private const val PREFS_NAME = "spellit"
        private const val CUSTOM_KEY = "spellit.customBanks"
        private const val ACTIVE_KEY = "spellit.activeBank"
        const val DEFAULT_BANK_ID = "band-2-3"

        @Volatile
        private var instance: BankStore? = null

        fun shared(context: Context): BankStore =
            instance ?: synchronized(this) {
                instance ?: BankStore(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                ).also { instance = it }
            }
    }

    @Serializable
    private data class StoredBank(
        val id: String,
        val name: String,
        val entries: List<WordEntry>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    var customBanks by mutableStateOf<List<WordBank>>(emptyList())
        private set
    var activeId by mutableStateOf(DEFAULT_BANK_ID)
        private set

    /** Bumps on any custom-bank mutation so mid-round edits re-deal games. */
    var revision by mutableIntStateOf(0)
        private set

    init {
        val data = prefs.getString(CUSTOM_KEY, null)
        if (data != null) {
            val stored = runCatching { json.decodeFromString<List<StoredBank>>(data) }
                .getOrDefault(emptyList())
            customBanks = stored.map {
                WordBank(
                    id = it.id,
                    name = it.name,
                    blurb = "Your custom list",
                    builtIn = false,
                    entries = it.entries,
                )
            }
        }
        val saved = prefs.getString(ACTIVE_KEY, null)
        if (saved != null && allBanks.any { it.id == saved }) {
            activeId = saved
        }
    }

    val allBanks: List<WordBank>
        get() = WordData.builtInBanks + customBanks

    val activeBank: WordBank
        get() = allBanks.firstOrNull { it.id == activeId }
            ?: WordData.builtInBanks.firstOrNull { it.id == DEFAULT_BANK_ID }
            ?: WordData.builtInBanks[0]

    fun setActive(id: String) {
        activeId = id
        prefs.edit().putString(ACTIVE_KEY, id).apply()
    }

    private fun persist() {
        revision += 1
        val stored = customBanks.map { StoredBank(it.id, it.name, it.entries) }
        prefs.edit().putString(CUSTOM_KEY, json.encodeToString(stored)).apply()
    }

    fun createBank(name: String = "My new list", entries: List<WordEntry> = emptyList()): WordBank {
        val bank = WordBank(
            id = UUID.randomUUID().toString(),
            name = name,
            blurb = "Your custom list",
            builtIn = false,
            entries = entries,
        )
        customBanks = customBanks + bank
        persist()
        return bank
    }

    fun deleteBank(id: String) {
        customBanks = customBanks.filterNot { it.id == id }
        if (activeId == id) setActive(DEFAULT_BANK_ID)
        persist()
    }

    fun rename(id: String, name: String) {
        val index = customBanks.indexOfFirst { it.id == id }
        if (index < 0) return
        customBanks = customBanks.toMutableList().also {
            it[index] = it[index].copy(name = name)
        }
        persist()
    }

    fun addWord(entry: WordEntry, bankId: String) {
        val index = customBanks.indexOfFirst { it.id == bankId }
        if (index < 0) return
        customBanks = customBanks.toMutableList().also {
            it[index] = it[index].copy(entries = it[index].entries + entry)
        }
        persist()
    }

    fun removeWord(wordIndex: Int, bankId: String) {
        val index = customBanks.indexOfFirst { it.id == bankId }
        if (index < 0) return
        val entries = customBanks[index].entries
        if (wordIndex !in entries.indices) return
        customBanks = customBanks.toMutableList().also {
            it[index] = it[index].copy(
                entries = entries.toMutableList().apply { removeAt(wordIndex) },
            )
        }
        persist()
    }
}

// Word-length heuristics (banks carry no grade)

object GameHeuristics {
    /** How many letters Missing Letters blanks out. */
    fun blanks(word: String): Int =
        minOf(4, maxOf(1, Math.round(word.length / 3.0).toInt()))

    /** How long Flash Spell shows the word before hiding it, in milliseconds. */
    fun flashMs(word: String): Long = when {
        word.length <= 4 -> 4000L
        word.length <= 7 -> 3500L
        else -> 3000L
    }
}
