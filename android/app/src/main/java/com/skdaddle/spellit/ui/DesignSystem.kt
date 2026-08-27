package com.skdaddle.spellit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// Crayon palette (sRGB approximations of the site's oklch tokens)

object Palette {
    val Paper = Color(0xFFFDFBF2)
    val Ink = Color(0xFF31435C)
    val MutedInk = Color(0xFF6E7B90)
    val SoftBorder = Color(0xFFC3CCDA)
    val SecondaryBg = Color(0xFFF5F1E3)

    val Coral = Color(0xFFE2705F)
    val CoralSoft = Color(0xFFFADFD8)
    val Sun = Color(0xFFF0C452)
    val SunSoft = Color(0xFFFAF0CF)
    val Leaf = Color(0xFF47B583)
    val LeafSoft = Color(0xFFDFF2E5)
    val Sky = Color(0xFF5A8FCB)
    val SkySoft = Color(0xFFDEEAF6)
    val Grape = Color(0xFF8659B5)
    val GrapeSoft = Color(0xFFEEE4F4)
}

// Typography — one place to swap in a rounded display face later.

fun headingStyle(size: Int, weight: FontWeight = FontWeight.SemiBold): TextStyle =
    TextStyle(fontSize = size.sp, fontWeight = weight)

// Letter tile, the signature element

enum class TileSize(val side: Dp, val fontSize: Int) {
    XS(32.dp, 15),
    SM(40.dp, 19),
    MD(48.dp, 24),
    LG(56.dp, 30);

    companion object {
        fun forWord(word: String): TileSize = when {
            word.length > 10 -> XS
            word.length > 7 -> SM
            else -> MD
        }
    }
}

/** Chunky letter tile with the hard offset shadow. */
@Composable
fun Tile(
    letter: String,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.MD,
    fill: Color = Color.White,
    dashed: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier.size(size.side)) {
        if (!dashed) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .background(Palette.Ink, shape),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(if (dashed) fill.copy(alpha = 0.5f) else fill, shape)
                .then(
                    if (dashed) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = Palette.SoftBorder,
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                                    ),
                                ),
                            )
                        }
                    } else {
                        Modifier.border(3.dp, Palette.Ink, shape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.uppercase(),
                style = headingStyle(size.fontSize, FontWeight.Bold),
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Tappable tile with a press-down animation. */
@Composable
fun TileButton(
    letter: String,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.MD,
    fill: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "tilePress",
    )
    Tile(
        letter = letter,
        size = size,
        fill = fill,
        modifier = modifier
            .offset { IntOffset(0, press.dp.roundToPx()) }
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    )
}

// Chunky action button

@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Palette.Ink,
    foreground: Color = Color.White,
    bordered: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 3f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "buttonPress",
    )
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .offset { IntOffset(0, press.dp.roundToPx()) },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .background(Palette.Ink, shape),
        )
        Box(
            Modifier
                .clip(shape)
                .background(if (bordered) Color.White else background, shape)
                .then(if (bordered) Modifier.border(2.5.dp, Palette.Ink, shape) else Modifier)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 11.dp),
        ) {
            Text(
                text = text,
                style = headingStyle(16),
                color = if (bordered) Palette.Ink else foreground,
            )
        }
    }
}

// Shake effect for wrong answers

/**
 * Shakes its content horizontally whenever [trigger] changes to a new
 * non-zero value (mirrors the iOS ShakeEffect).
 */
@Composable
fun ShakeContainer(
    trigger: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val durationMs = 350f
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = ((now - start) / 1_000_000f) / durationMs
            if (t >= 1f) break
            offsetX.snapTo(6f * sin(t * PI.toFloat() * 3f * 2f))
        }
        offsetX.snapTo(0f)
    }
    Box(modifier.offset { IntOffset(offsetX.value.dp.roundToPx(), 0) }) {
        content()
    }
}
