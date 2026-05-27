package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Box(modifier = Modifier.then(Modifier.clip(shape))) {
            Box(modifier = Modifier.matchParentSize().then(Modifier.clip(shape))) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(color = backgroundColor)
                }
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiCardPreview() {
    DrawTaxiTheme {
        DrawTaxiCard {
            DrawTaxiText(text = "Contenu de la carte")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiCardClickablePreview() {
    DrawTaxiTheme {
        DrawTaxiCard(onClick = {}) {
            DrawTaxiText(text = "Carte cliquable")
        }
    }
}
