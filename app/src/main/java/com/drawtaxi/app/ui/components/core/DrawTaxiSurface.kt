package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.Indigo500

@Composable
fun DrawTaxiSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape) else Modifier)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            .clip(shape)
            .background(color),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiSurfacePreview() {
    DrawTaxiTheme {
        DrawTaxiSurface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = Indigo500.copy(alpha = 0.1f),
            shadowElevation = 4.dp
        ) {}
    }
}
