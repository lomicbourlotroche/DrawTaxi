package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ThemeToggleButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) TaxiRed else Slate100),
        color = if (isSelected) TaxiRed.copy(0.05f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier
    ) {
        Text(
            label,
            modifier = Modifier.padding(12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) TaxiRed else Slate700
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeToggleButtonPreview() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeToggleButton(label = "Moderne", isSelected = true, onClick = {}, modifier = Modifier.weight(1f))
        ThemeToggleButton(label = "Minimal", isSelected = false, onClick = {}, modifier = Modifier.weight(1f))
    }
}
