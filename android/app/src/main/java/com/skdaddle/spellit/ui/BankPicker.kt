package com.skdaddle.spellit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.engine.BankStore
import com.skdaddle.spellit.model.WordBank

/**
 * Word-list selector shown in every bank-driven game, with a jump to the
 * list manager.
 */
@Composable
fun BankPicker(
    store: BankStore,
    onManageLists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Word list",
            style = headingStyle(13, FontWeight.Medium),
            color = Palette.MutedInk,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                val shape = RoundedCornerShape(12.dp)
                Box {
                    Box(
                        Modifier
                            .matchParentSize()
                            .offset(y = 3.dp)
                            .background(Palette.Ink, shape),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color.White, shape)
                            .border(2.5.dp, Palette.Ink, shape)
                            .clickable { menuOpen = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(
                            store.activeBank.displayName,
                            style = headingStyle(15, FontWeight.Medium),
                            color = Palette.Ink,
                            maxLines = 1,
                        )
                        Icon(
                            Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = Palette.Ink,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    bankSection("Levels", store.allBanks.filter { it.builtIn && it.id.startsWith("band-") }, store) { menuOpen = false }
                    bankSection("Collections", store.allBanks.filter { it.builtIn && !it.id.startsWith("band-") }, store) { menuOpen = false }
                    if (store.customBanks.isNotEmpty()) {
                        bankSection("My lists", store.customBanks, store) { menuOpen = false }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onManageLists),
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Palette.MutedInk,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Manage lists",
                    style = headingStyle(13, FontWeight.Medium),
                    color = Palette.MutedInk,
                )
            }
        }
        Text(store.activeBank.blurb, fontSize = 13.sp, color = Palette.MutedInk)
    }
}

@Composable
private fun bankSection(
    title: String,
    banks: List<WordBank>,
    store: BankStore,
    onPicked: () -> Unit,
) {
    if (banks.isEmpty()) return
    Text(
        title,
        style = headingStyle(12, FontWeight.Medium),
        color = Palette.MutedInk,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    for (bank in banks) {
        DropdownMenuItem(
            text = { Text(bank.displayName, style = headingStyle(14, FontWeight.Medium), color = Palette.Ink) },
            leadingIcon = {
                if (bank.id == store.activeId) {
                    Icon(Icons.Filled.Check, contentDescription = "in use", tint = Palette.Leaf)
                }
            },
            onClick = {
                store.setActive(bank.id)
                onPicked()
            },
        )
    }
}

/** Shown when the active bank lacks enough usable words for a game. */
@Composable
fun NotEnoughWords(
    need: Int,
    requirement: String,
    onManageLists: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "This list needs more words for this game.",
            style = headingStyle(17),
            color = Palette.Ink,
        )
        Text(
            "It takes at least $need $requirement to play. Add some, or pick a different list above.",
            fontSize = 14.sp,
            color = Palette.MutedInk,
        )
        ChunkyButton(text = "Manage word lists", bordered = true, onClick = onManageLists)
    }
}
