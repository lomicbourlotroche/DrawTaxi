package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DrawTaxiTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind { drawRect(color = backgroundColor, size = size) }
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                title()
            }
            actions()
        }
    }
}

@Composable
fun DrawTaxiTopBarTitle(
    text: String,
    subtitle: String? = null,
    color: Color = Color.Black
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text(text = text, fontWeight = FontWeight.Bold, color = color)
        if (subtitle != null) {
            androidx.compose.material3.Text(text = subtitle, color = Color.Gray)
        }
    }
}
