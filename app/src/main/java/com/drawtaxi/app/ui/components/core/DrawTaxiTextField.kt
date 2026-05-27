package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.*

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
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = drawTaxiColors()
    val typography = drawTaxiType()

    val targetBorderColor = when {
        isError -> colors.error
        isFocused -> colors.primary
        else -> colors.outline
    }
    
    val borderColor by animateColorAsState(targetValue = targetBorderColor, label = "borderColor")
    val borderWidth by animateDpAsState(targetValue = if (isFocused) 2.dp else 1.dp, label = "borderWidth")
    val surfaceColor by animateColorAsState(
        targetValue = if (isFocused) colors.surface else colors.surfaceVariant.copy(alpha = 0.5f),
        label = "surfaceColor"
    )

    val labelColor = when {
        isError -> colors.error
        isFocused -> colors.primary
        else -> Slate400
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            DrawTaxiText(
                text = label.uppercase(),
                color = labelColor,
                fontSize = typography.labelSmall.fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = typography.labelSmall.letterSpacing,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
        DrawTaxiSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            borderColor = borderColor,
            borderWidth = borderWidth
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 12.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        DrawTaxiText(
                            text = placeholder,
                            color = Slate400,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = singleLine,
                        maxLines = maxLines,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = visualTransformation
                    )
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 12.dp)) { trailingIcon() }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiTextFieldPreview() {
    DrawTaxiTheme {
        DrawTaxiTextField(
            value = "Paris, Gare du Nord",
            onValueChange = {},
            label = "Départ",
            placeholder = "Entrez une adresse",
            leadingIcon = {
                DrawTaxiIcon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate400
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiTextFieldEmptyPreview() {
    DrawTaxiTheme {
        DrawTaxiTextField(
            value = "",
            onValueChange = {},
            label = "Arrivée",
            placeholder = "Entrez une adresse"
        )
    }
}
