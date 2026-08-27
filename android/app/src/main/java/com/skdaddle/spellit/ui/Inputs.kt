package com.skdaddle.spellit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skdaddle.spellit.model.GradeBand

/** The chunky text field kids type spellings into, shared across games. */
@Composable
fun SpellingField(
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(Palette.Ink, shape),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.White, shape)
                .border(3.dp, Palette.Ink, shape)
                .padding(vertical = 12.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(Palette.Ink),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) {
                            Text(
                                placeholder,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Palette.MutedInk.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** Grade-band capsule picker (Daily Bee + Ending Machine still use bands). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradePicker(
    grade: GradeBand,
    onGradeChange: (GradeBand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Pick your level",
            style = headingStyle(13, FontWeight.Medium),
            color = Palette.MutedInk,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (band in GradeBand.entries) {
                val selected = band == grade
                Text(
                    band.label,
                    style = headingStyle(13, FontWeight.Medium),
                    color = if (selected) Color.White else Palette.Ink,
                    modifier = Modifier
                        .background(if (selected) Palette.Ink else Color.White, CircleShape)
                        .border(2.5.dp, Palette.Ink, CircleShape)
                        .clickable { onGradeChange(band) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
        Text(grade.blurb, fontSize = 13.sp, color = Palette.MutedInk)
    }
}
