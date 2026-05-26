package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.*

data class DrawTaxiFilterChipColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val selectedBorderColor: Color,
    val unselectedBorderColor: Color
)

object DrawTaxiFilterChipDefaults {
    @Composable
    fun colors(
        selectedContainerColor: Color = Emerald50.copy(alpha = 0.3f),
        selectedContentColor: Color = Emerald600,
        unselectedContainerColor: Color = Color.Transparent,
        unselectedContentColor: Color = Slate600,
        selectedBorderColor: Color = Emerald500,
        unselectedBorderColor: Color = Slate300
    ): DrawTaxiFilterChipColors = DrawTaxiFilterChipColors(
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        unselectedContainerColor = unselectedContainerColor,
        unselectedContentColor = unselectedContentColor,
        selectedBorderColor = selectedBorderColor,
        unselectedBorderColor = unselectedBorderColor
    )
}

@Composable
fun DrawTaxiFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DrawTaxiFilterChipColors = DrawTaxiFilterChipDefaults.colors()
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.selectedContainerColor else colors.unselectedContainerColor,
        animationSpec = tween(150),
        label = "chipContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.selectedContentColor else colors.unselectedContentColor,
        animationSpec = tween(150),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.selectedBorderColor else colors.unselectedBorderColor,
        animationSpec = tween(150),
        label = "chipBorder"
    )

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.ProvideTextStyle(
                value = drawTaxiType().labelMedium.copy(color = contentColor)
            ) {
                label()
            }
        }
    }
}
