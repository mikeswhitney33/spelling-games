package com.skdaddle.spellit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.ui.games.BalloonPopScreen
import com.skdaddle.spellit.ui.games.DailyBeeScreen
import com.skdaddle.spellit.ui.games.EndingMachineScreen
import com.skdaddle.spellit.ui.games.FixSentenceScreen
import com.skdaddle.spellit.ui.games.FlashSpellScreen
import com.skdaddle.spellit.ui.games.ListenSpellScreen
import com.skdaddle.spellit.ui.games.MemoryMatchScreen
import com.skdaddle.spellit.ui.games.MiniCrosswordScreen
import com.skdaddle.spellit.ui.games.MissingLettersScreen
import com.skdaddle.spellit.ui.games.SpotWordScreen
import com.skdaddle.spellit.ui.games.WordScrambleScreen
import com.skdaddle.spellit.ui.games.WordSearchScreen

/**
 * Routes a Game to its screen. "Manage lists" opens Settings as an overlay
 * ON TOP of the game (mirroring the iOS sheet) so the game composable stays
 * mounted and a round in progress survives the trip.
 */
@Composable
fun GameHost(
    game: Game,
    store: BankStore,
) {
    var showSettings by remember { mutableStateOf(false) }
    var editingBankId by remember { mutableStateOf<String?>(null) }
    val onManageLists = { showSettings = true }

    Box(Modifier.fillMaxSize()) {
        when (game) {
            Game.DAILY_BEE -> DailyBeeScreen(onManageLists)
            Game.WORD_SCRAMBLE -> WordScrambleScreen(store, onManageLists)
            Game.MISSING_LETTERS -> MissingLettersScreen(store, onManageLists)
            Game.LISTEN_AND_SPELL -> ListenSpellScreen(store, onManageLists)
            Game.SPOT_THE_WORD -> SpotWordScreen(store, onManageLists)
            Game.FLASH_SPELL -> FlashSpellScreen(store, onManageLists)
            Game.FIX_THE_SENTENCE -> FixSentenceScreen(store, onManageLists)
            Game.ENDING_MACHINE -> EndingMachineScreen(onManageLists)
            Game.MINI_CROSSWORD -> MiniCrosswordScreen(store, onManageLists)
            Game.WORD_SEARCH -> WordSearchScreen(store, onManageLists)
            Game.MEMORY_MATCH -> MemoryMatchScreen(store, onManageLists)
            Game.BALLOON_POP -> BalloonPopScreen(store, onManageLists)
        }

        if (showSettings) {
            BackHandler {
                if (editingBankId != null) editingBankId = null else showSettings = false
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Palette.Paper),
            ) {
                val editing = editingBankId
                if (editing == null) {
                    SettingsScreen(
                        store = store,
                        onBack = { showSettings = false },
                        onEditBank = { editingBankId = it },
                    )
                } else {
                    BankEditorScreen(
                        store = store,
                        bankId = editing,
                        onBack = { editingBankId = null },
                    )
                }
            }
        }
    }
}
