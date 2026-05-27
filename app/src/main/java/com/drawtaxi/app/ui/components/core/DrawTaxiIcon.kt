package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.Indigo500

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

@Preview(showBackground = true)
@Composable
fun DrawTaxiIconPreview() {
    DrawTaxiTheme {
        DrawTaxiIcon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = "Voiture",
            tint = Indigo500
        )
    }
}
