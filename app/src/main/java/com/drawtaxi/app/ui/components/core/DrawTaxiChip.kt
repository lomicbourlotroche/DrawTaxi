package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.Slate100
import com.drawtaxi.app.ui.theme.Slate600

@Composable
fun DrawTaxiChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = Slate100,
    labelColor: Color = Slate600,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(shape)
            .background(containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun DrawTaxiStatusChip(
    statusText: String,
    containerColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    DrawTaxiChip(
        modifier = modifier,
        containerColor = containerColor,
        labelColor = labelColor
    ) {
        androidx.compose.material3.Text(
            text = statusText,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
