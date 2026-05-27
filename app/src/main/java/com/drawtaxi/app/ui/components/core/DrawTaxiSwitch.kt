package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.Slate200
import com.drawtaxi.app.ui.theme.TaxiRed

@Composable
fun DrawTaxiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.White,
    checkedTrackColor: Color = TaxiRed,
    uncheckedTrackColor: Color = Slate200
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(200), label = "switchTrack"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(color = trackColor, cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiSwitchOnPreview() {
    DrawTaxiTheme {
        DrawTaxiSwitch(checked = true, onCheckedChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiSwitchOffPreview() {
    DrawTaxiTheme {
        DrawTaxiSwitch(checked = false, onCheckedChange = {})
    }
}
