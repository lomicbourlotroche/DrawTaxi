package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.components.core.DrawTaxiIcon
import com.drawtaxi.app.ui.components.core.DrawTaxiSwitch
import com.drawtaxi.app.ui.theme.*

@Composable
fun TaxiToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    iconTint: Color = drawTaxiColors().primary
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
            androidx.compose.material3.Text(
                text = title,
                style = drawTaxiType().bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = drawTaxiColors().onSurface
            )
            androidx.compose.material3.Text(
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

@Preview(showBackground = true)
@Composable
fun TaxiToggleRowPreview() {
    DrawTaxiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TaxiToggleRow(
                title = "Surveiller les SMS",
                subtitle = "Détecter les commandes entrantes en temps réel",
                checked = true,
                onCheckedChange = {},
                icon = Icons.Default.Sms
            )
        }
    }
}
