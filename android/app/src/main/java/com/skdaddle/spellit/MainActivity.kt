package com.skdaddle.spellit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.ui.BankEditorScreen
import com.skdaddle.spellit.ui.Game
import com.skdaddle.spellit.ui.GameHost
import com.skdaddle.spellit.ui.HomeScreen
import com.skdaddle.spellit.ui.Palette
import com.skdaddle.spellit.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = BankStore.shared(this)
        // Debug hook: `--es game wordScramble` (adb shell am start … --es game X)
        // launches straight into that game, for headless screenshots.
        val launchGame = Game.fromId(intent.getStringExtra("game"))
        setContent {
            SpellItNav(store = store, launchGame = launchGame)
        }
    }
}

@Composable
private fun SpellItNav(store: BankStore, launchGame: Game?) {
    val nav = rememberNavController()
    Scaffold { padding ->
        NavHost(
            navController = nav,
            startDestination = launchGame?.let { "game/${it.id}" } ?: "home",
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Paper)
                .padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    onOpenGame = { nav.navigate("game/${it.id}") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable(
                "game/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val game = Game.fromId(entry.arguments?.getString("id"))
                if (game != null) {
                    GameHost(
                        game = game,
                        store = store,
                        onManageLists = { nav.navigate("settings") },
                    )
                }
            }
            composable("settings") {
                SettingsScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                    onEditBank = { nav.navigate("settings/edit/$it") },
                )
            }
            composable(
                "settings/edit/{bankId}",
                arguments = listOf(navArgument("bankId") { type = NavType.StringType }),
            ) { entry ->
                BankEditorScreen(
                    store = store,
                    bankId = entry.arguments?.getString("bankId") ?: "",
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
