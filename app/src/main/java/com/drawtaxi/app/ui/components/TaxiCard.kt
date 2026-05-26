package com.drawtaxi.app.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.components.core.DrawTaxiCard
import com.drawtaxi.app.ui.components.core.DrawTaxiIcon
import com.drawtaxi.app.ui.theme.*

@Composable
fun TaxiCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleIcon: ImageVector? = null,
    gradient: Boolean = false,
    brandColor: Color = Indigo500,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = if (gradient) {
        Brush.linearGradient(colors = listOf(brandColor, brandColor.copy(alpha = 0.8f)))
    } else {
        null
    }

    DrawTaxiCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        backgroundColor = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundBrush != null) {
                Box(modifier = Modifier.matchParentSize().background(backgroundBrush))
            } else {
                Box(modifier = Modifier.matchParentSize().background(drawTaxiColors().surface))
            }
            Column(modifier = Modifier.matchParentSize().padding(16.dp)) {
                if (title != null) {
                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (titleIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                DrawTaxiIcon(imageVector = titleIcon, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        androidx.compose.material3.Text(
                            text = title,
                            style = drawTaxiType().titleMedium,
                            color = drawTaxiColors().onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun TaxiCardHighlighted(
    modifier: Modifier = Modifier,
    brandColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    DrawTaxiCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        backgroundColor = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.matchParentSize().background(brandColor.copy(alpha = 0.08f)))
            Row(
                modifier = Modifier.matchParentSize().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = content
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    brandColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.Text(
            text = value,
            style = drawTaxiType().titleLarge,
            fontWeight = FontWeight.Bold,
            color = drawTaxiColors().onSurface
        )
        androidx.compose.material3.Text(
            text = label,
            style = drawTaxiType().labelSmall,
            color = drawTaxiColors().onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TaxiCardPreview() {
    DrawTaxiTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            TaxiCard(
                title = "Card Title",
                titleIcon = Icons.Default.Star,
                brandColor = Color(0xFF6366F1)
            ) {
                androidx.compose.material3.Text(
                    text = "This is a card content.",
                    style = drawTaxiType().bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaxiCardHighlightedPreview() {
    DrawTaxiTheme {
        TaxiCardHighlighted(
            brandColor = Color(0xFF6366F1)
        ) {
            StatCard(
                value = "10",
                label = "Pending",
                icon = Icons.Default.Star,
                brandColor = Color(0xFF6366F1)
            )
            StatCard(
                value = "45",
                label = "Completed",
                icon = Icons.Default.Star,
                brandColor = Color(0xFF6366F1)
            )
        }
    }
}
