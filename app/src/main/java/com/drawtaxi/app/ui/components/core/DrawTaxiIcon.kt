package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun DrawTaxiIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val painter = rememberVectorPainter(image = imageVector)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
    )
}
