package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.*

@Composable
fun DrawTaxiOutlinedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isNumber: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null
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

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
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
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = Slate50)
                drawRoundRect(color = borderColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isFocused) 2.dp.toPx() else 1.dp.toPx()))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).onFocusChanged { isFocused = it.isFocused },
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = drawTaxiColors().onSurface),
                cursorBrush = SolidColor(drawTaxiColors().primary),
                keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Decimal else KeyboardType.Text),
                visualTransformation = VisualTransformation.None,
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
