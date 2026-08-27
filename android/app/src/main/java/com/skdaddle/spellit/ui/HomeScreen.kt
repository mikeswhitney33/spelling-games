package com.skdaddle.spellit.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.model.GradeBand

private val heroTiles = listOf(
    Triple("S", Palette.CoralSoft, -6f), Triple("P", Palette.SunSoft, 3f),
    Triple("E", Palette.LeafSoft, -2f), Triple("L", Palette.SkySoft, 6f),
    Triple("L", Color.White, -3f), Triple("I", Palette.SunSoft, 2f),
    Triple("N", Palette.CoralSoft, -3f), Triple("G", Palette.LeafSoft, 6f),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenGame: (Game) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Word lists", tint = Palette.Ink)
            }
        }

        // Hero
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for ((letter, color, tilt) in heroTiles) {
                    Tile(
                        letter = letter,
                        size = TileSize.SM,
                        fill = color,
                        modifier = Modifier.rotate(tilt),
                    )
                }
            }
            Text(
                "Practice that feels like recess.",
                style = headingStyle(28),
                color = Palette.Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                "Twelve quick games, four levels — from first words like cat to champion stumpers like mischievous.",
                fontSize = 15.sp,
                color = Palette.MutedInk,
                textAlign = TextAlign.Center,
            )
        }

        // Games grid, two columns
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (rowGames in Game.entries.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (game in rowGames) {
                        GameCard(
                            game = game,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenGame(game) },
                        )
                    }
                    if (rowGames.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Levels
        Column(
            Modifier
                .fillMaxWidth()
                .background(Palette.SecondaryBg, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("A level for every speller", style = headingStyle(20), color = Palette.Ink)
            Text(
                "Change your level any time from inside a game — your pick is remembered.",
                fontSize = 13.sp,
                color = Palette.MutedInk,
            )
            for (band in GradeBand.entries) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(34.dp)
                            .background(Palette.SunSoft, RoundedCornerShape(10.dp))
                            .border(2.5.dp, Palette.Ink, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(band.short, style = headingStyle(14), color = Palette.Ink)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(band.label, style = headingStyle(15), color = Palette.Ink)
                        Text(band.blurb, fontSize = 12.sp, color = Palette.MutedInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .heightIn(min = 168.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 6.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(game.accent)
                .align(Alignment.TopCenter),
        )
        Column(
            Modifier
                .padding(12.dp)
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shape = RoundedCornerShape(12.dp)
            Box(Modifier.size(44.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(y = 3.dp)
                        .background(Palette.Ink, shape),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(game.accentSoft, shape)
                        .border(2.5.dp, Palette.Ink, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(game.icon, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
                }
            }
            Text(game.title, style = headingStyle(17), color = Palette.Ink)
            Text(game.tagline, style = headingStyle(12, FontWeight.Medium), color = game.accent)
            Text(
                game.blurb,
                fontSize = 12.sp,
                color = Palette.MutedInk,
                maxLines = 3,
            )
        }
    }
}
