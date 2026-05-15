package com.drawtaxi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TaxiLogo(
    modifier: Modifier = Modifier,
    brandColor: Color = TaxiRed,
    size: Int = 100
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(brandColor, CircleShape)
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalTaxi,
            contentDescription = "Taxi Logo",
            tint = Color.White,
            modifier = Modifier.size((size * 0.6).dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TaxiLogoPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        TaxiLogo()
    }
}
