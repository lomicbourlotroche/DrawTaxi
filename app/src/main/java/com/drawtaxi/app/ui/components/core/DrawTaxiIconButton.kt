package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.Slate700

@Composable
fun DrawTaxiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = DrawTaxiIconButtonDefaults.size,
    shape: Shape = DrawTaxiIconButtonDefaults.shape,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = tween(80),
        label = "iconBtnScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

object DrawTaxiIconButtonDefaults {
    val size = 48.dp
    val shape = RoundedCornerShape(12.dp)
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiIconButtonPreview() {
    DrawTaxiTheme {
        DrawTaxiIconButton(onClick = {}) {
            DrawTaxiIcon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = Slate700
            )
        }
    }
}
