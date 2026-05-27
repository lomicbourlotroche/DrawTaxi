package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 12,
    verticalPadding: Int = 8,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = drawTaxiType().labelSmall,
            fontWeight = FontWeight.Bold,
            color = drawTaxiColors().onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        DrawTaxiCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = drawTaxiColors().surface,
            elevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = drawTaxiColors().primary,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = drawTaxiType().bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = drawTaxiColors().onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = drawTaxiType().bodySmall,
                        color = drawTaxiColors().onSurfaceVariant
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                DrawTaxiIcon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = drawTaxiColors().onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TaxiToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = drawTaxiColors().primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            DrawTaxiIcon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = drawTaxiType().bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = drawTaxiColors().onSurface
            )
            Text(
                text = subtitle,
                style = drawTaxiType().bodySmall,
                color = drawTaxiColors().onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        DrawTaxiSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            checkedTrackColor = drawTaxiColors().primary
        )
    }
}
