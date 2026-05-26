package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun DrawTaxiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> drawTaxiColors().error
        isFocused -> drawTaxiColors().primary
        else -> Slate200
    }
    val labelColor = when {
        isError -> drawTaxiColors().error
        isFocused -> drawTaxiColors().primary
        else -> Slate400
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            androidx.compose.material3.Text(
                text = label.uppercase(),
                style = drawTaxiType().labelSmall,
                color = labelColor,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = Slate50)
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = if (isFocused) 2.dp.toPx() else 1.dp.toPx())
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 12.dp)) { leadingIcon() }
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused },
                    singleLine = singleLine,
                    maxLines = maxLines,
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = drawTaxiColors().onSurface),
                    cursorBrush = SolidColor(drawTaxiColors().primary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = visualTransformation,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                androidx.compose.material3.Text(placeholder, color = Slate400, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
