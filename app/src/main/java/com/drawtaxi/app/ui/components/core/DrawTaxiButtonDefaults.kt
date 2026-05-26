package com.drawtaxi.app.ui.components.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.drawtaxi.app.ui.theme.*

@Immutable
data class DrawTaxiButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color
)

object DrawTaxiButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = TaxiRed,
        contentColor: Color = Color.White,
        disabledContainerColor: Color = Slate200,
        disabledContentColor: Color = Slate400
    ): DrawTaxiButtonColors = DrawTaxiButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = TaxiRed,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Slate400
    ): DrawTaxiButtonColors = DrawTaxiButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = TaxiRed,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Slate400
    ): DrawTaxiButtonColors = DrawTaxiButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}
