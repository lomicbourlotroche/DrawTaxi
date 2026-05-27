package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.components.core.DrawTaxiText
import com.drawtaxi.app.ui.theme.*

@Composable
fun DrawTaxiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = drawTaxiColors().primary,
    contentColor: Color = drawTaxiColors().onPrimary,
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    DrawTaxiSolidButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        minHeight = minHeight,
        content = content
    )
}

@Composable
fun DrawTaxiSolidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = drawTaxiColors().primary,
    contentColor: Color = drawTaxiColors().onPrimary,
    minHeight: Dp = 52.dp,
    content: @Composable RowScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed && enabled) 0.97f else 1f, animationSpec = tween(80), label = "btnScale")
    val alpha by animateFloatAsState(targetValue = if (pressed && enabled) 0.7f else 1f, animationSpec = tween(80), label = "btnAlpha")

    val effectiveColor = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
    val effectiveText = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = minHeight)
            .clip(shape)
            .then(
                if (enabled) Modifier.pointerInput(onClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            if (event.changes.any { it.pressed.not() && it.previousPressed }) { onClick(); pressed = false }
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(Modifier.clip(shape))
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = effectiveColor.copy(alpha = effectiveColor.alpha * alpha))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun DrawTaxiOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    contentColor: Color = drawTaxiColors().primary,
    borderColor: Color = contentColor.copy(alpha = 0.5f),
    minHeight: Dp = 52.dp,
    content: @Composable RowScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed && enabled) 0.97f else 1f, animationSpec = tween(80), label = "btnScale")
    val alpha by animateFloatAsState(targetValue = if (pressed && enabled) 0.6f else 1f, animationSpec = tween(80), label = "btnAlpha")

    val effectiveContent = if (enabled) contentColor else contentColor.copy(alpha = 0.4f)
    val effectiveBorder = if (enabled) borderColor else borderColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = minHeight)
            .clip(shape)
            .then(
                if (enabled) Modifier.pointerInput(onClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            if (event.changes.any { it.pressed.not() && it.previousPressed }) { onClick(); pressed = false }
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier.matchParentSize().clip(shape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = effectiveBorder,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
                if (pressed && enabled) {
                    drawRect(color = contentColor.copy(alpha = 0.08f * alpha))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiButtonPreview() {
    DrawTaxiTheme {
        DrawTaxiButton(
            onClick = { /* Do nothing */ },
            enabled = true
        ) {
            DrawTaxiText("Button")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiSolidButtonPreview() {
    DrawTaxiTheme {
        DrawTaxiSolidButton(
            onClick = { /* Do nothing */ },
            enabled = true
        ) {
            DrawTaxiText("Solid Button")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiOutlinedButtonPreview() {
    DrawTaxiTheme {
        DrawTaxiOutlinedButton(
            onClick = { /* Do nothing */ },
            enabled = true
        ) {
            DrawTaxiText("Outlined Button")
        }
    }
}