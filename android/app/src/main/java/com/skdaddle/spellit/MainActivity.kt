package com.skdaddle.spellit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore

// Placeholder shell: the game screens land in the next PR. This proves the
// module wiring (Compose, data generation, engines) end to end.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = BankStore.shared(this)
        setContent {
            Splash(wordCount = store.activeBank.entries.size)
        }
    }
}

@Composable
private fun Splash(wordCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF6EC)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Spell It! — $wordCount words ready",
            color = Color(0xFF31435C),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
