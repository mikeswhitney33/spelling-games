package com.skdaddle.spellit.ui

import androidx.compose.runtime.Composable
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

/** Routes a Game to its screen; every screen gets the store + settings hook. */
@Composable
fun GameHost(
    game: Game,
    store: BankStore,
    onManageLists: () -> Unit,
) {
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
}
