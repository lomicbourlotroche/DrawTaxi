package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.*

@Composable
fun DrawTaxiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = drawTaxiColors().surface,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = tween(100),
        label = "cardAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .then(
                if (onClick != null) Modifier.pointerInput(onClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            if (event.changes.any { it.pressed.not() && it.previousPressed }) {
                                onClick()
                                pressed = false
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        Box(modifier = Modifier.then(Modifier.clip(shape))) {
            Box(modifier = Modifier.matchParentSize().then(Modifier.clip(shape))) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(color = backgroundColor.copy(alpha = alpha))
                }
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRect(color: Color) {
    drawRect(color = color, size = size)
}
