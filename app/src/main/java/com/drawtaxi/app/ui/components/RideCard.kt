package com.drawtaxi.app.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.core.DrawTaxiIcon
import com.drawtaxi.app.ui.theme.*

@Composable
fun RideCard(
    ride: RideRequest,
    brandColor: Color,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = tween(80), label = "cardScale")
    val alpha by animateFloatAsState(targetValue = if (pressed) 0.85f else 1f, animationSpec = tween(80), label = "cardAlpha")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = brandColor.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(onClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                        if (event.changes.any { it.pressed.not() && it.previousPressed }) { onClick(); pressed = false }
                    }
                }
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(brandColor.copy(alpha = 0.12f), brandColor.copy(alpha = 0.06f))))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).background(brandColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            DrawTaxiIcon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.material3.Text(text = ride.time.ifBlank { "\u2014:\u2014" }, style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(text = " km", style = drawTaxiType().labelMedium, color = Slate500)
                        if (ride.date.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.background(Slate100, RoundedCornerShape(6.dp))) {
                                androidx.compose.material3.Text(text = ride.date, style = drawTaxiType().labelSmall, color = Slate600, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Emerald500))
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Text(text = ride.arrival.ifBlank { "\u2014" }, style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (ride.departure.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Slate300))
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.Text(text = ride.departure, style = drawTaxiType().bodyMedium, color = Slate600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        androidx.compose.material3.Text(
                            text = if (ride.price > 0) String.format("%.2f \u20ac", ride.price) else "\u2014",
                            style = drawTaxiType().headlineSmall, fontWeight = FontWeight.Black,
                            color = if (ride.price > 0) brandColor else Slate400
                        )
                        if (ride.profitabilityPercent > 0) {
                            val bgColor = when { ride.profitabilityPercent >= 70 -> Emerald100; ride.profitabilityPercent >= 50 -> Amber100; else -> Rose100 }
                            val textColor = when { ride.profitabilityPercent >= 70 -> Green800; ride.profitabilityPercent >= 50 -> Amber700; else -> Rose700 }
                            Box(modifier = Modifier.padding(top = 4.dp).background(bgColor, RoundedCornerShape(6.dp))) {
                                androidx.compose.material3.Text(
                                    text = "%",
                                    style = drawTaxiType().labelSmall, fontWeight = FontWeight.Bold,
                                    color = textColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate100))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).background(Slate100, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Text(text = ride.sender.take(2).uppercase(), style = drawTaxiType().labelSmall, fontWeight = FontWeight.Bold, color = Slate600)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text(text = ride.sender.ifBlank { "Client" }, style = drawTaxiType().bodyMedium, color = Slate600)
                    }
                    com.drawtaxi.app.ui.components.core.DrawTaxiChip(
                        containerColor = when (ride.status) { com.drawtaxi.app.data.RideStatus.DRAFT -> Amber100; com.drawtaxi.app.data.RideStatus.QUOTED -> Blue100; com.drawtaxi.app.data.RideStatus.CONFIRMED -> Emerald100; else -> Slate100 },
                        labelColor = when (ride.status) { com.drawtaxi.app.data.RideStatus.DRAFT -> Color(0xFF92400E); com.drawtaxi.app.data.RideStatus.QUOTED -> Color(0xFF1E40AF); com.drawtaxi.app.data.RideStatus.CONFIRMED -> Color(0xFF065F46); else -> Slate600 }
                    ) {
                        androidx.compose.material3.Text(
                            text = when (ride.status) { com.drawtaxi.app.data.RideStatus.DRAFT -> "Brouillon"; com.drawtaxi.app.data.RideStatus.QUOTED -> "Devis envoy\u00e9"; com.drawtaxi.app.data.RideStatus.CONFIRMED -> "Confirm\u00e9e"; com.drawtaxi.app.data.RideStatus.IN_PROGRESS -> "En cours"; com.drawtaxi.app.data.RideStatus.COMPLETED -> "Termin\u00e9e"; com.drawtaxi.app.data.RideStatus.CANCELLED -> "Annul\u00e9e"; com.drawtaxi.app.data.RideStatus.ABSENT -> "Absent"; else -> "En attente" },
                            style = drawTaxiType().labelSmall
                        )
                    }
                }
                if (ride.isPending) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.drawtaxi.app.ui.components.core.DrawTaxiOutlinedButton(
                            onClick = onDelete, modifier = Modifier.weight(1f), contentColor = Rose500
                        ) {
                            DrawTaxiIcon(Icons.Default.Delete, modifier = Modifier.size(16.dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Text("Supprimer", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        com.drawtaxi.app.ui.components.core.DrawTaxiSolidButton(
                            onClick = onValidate, modifier = Modifier.weight(1f), containerColor = brandColor
                        ) {
                            DrawTaxiIcon(Icons.Default.Check, modifier = Modifier.size(16.dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Text("Valider", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RideHistoryCard(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = tween(80), label = "histScale")
    val alpha by animateFloatAsState(targetValue = if (pressed) 0.85f else 1f, animationSpec = tween(80), label = "histAlpha")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = alpha))
            .pointerInput(onClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                        if (event.changes.any { it.pressed.not() && it.previousPressed }) { onClick(); pressed = false }
                    }
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(32.dp).background(Emerald100, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    DrawTaxiIcon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(text = ride.departure.ifBlank { "\u2014" }, style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Medium)
                        DrawTaxiIcon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.padding(horizontal = 6.dp).size(14.dp))
                        androidx.compose.material3.Text(text = ride.arrival.ifBlank { "\u2014" }, style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(text = ride.time.ifBlank { "\u2014" }, style = drawTaxiType().labelSmall, color = Slate500)
                        androidx.compose.material3.Text(text = " \u2022 ", color = Slate400)
                        androidx.compose.material3.Text(text = ride.sender.ifBlank { "Client" }, style = drawTaxiType().labelSmall, color = Slate500)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.Text(
                    text = if (ride.price > 0) String.format("%.2f \u20ac", ride.price) else "\u2014",
                    style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold,
                    color = if (ride.price > 0) Emerald600 else Slate400
                )
                if (ride.profitabilityPercent > 0) {
                    androidx.compose.material3.Text(
                        text = "%",
                        style = drawTaxiType().labelSmall, fontWeight = FontWeight.Bold,
                        color = if (ride.profitabilityPercent >= 70) Emerald600 else Amber500
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RideCardPreview() {
    val sampleRide = RideRequest(
        id = "123",
        sender = "0612345678",
        body = "Taxi de Lyon à Paris",
        departure = "Lyon",
        arrival = "Paris",
        time = "14:30",
        date = "26/05/2026",
        price = 150.0,
        status = com.drawtaxi.app.data.RideStatus.CONFIRMED,
        profitabilityPercent = 75.0,
        isPending = true
    )
    DrawTaxiTheme {
        RideCard(
            ride = sampleRide,
            brandColor = Color(0xFF6366F1),
            onValidate = {},
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideHistoryCardPreview() {
    val sampleRide = RideRequest(
        id = "124",
        sender = "0687654321",
        body = "Course passée",
        departure = "Nantes",
        arrival = "Rennes",
        time = "10:15",
        date = "25/05/2026",
        price = 95.0,
        status = com.drawtaxi.app.data.RideStatus.COMPLETED,
        profitabilityPercent = 60.0,
        isPending = false
    )
    DrawTaxiTheme {
        RideHistoryCard(
            ride = sampleRide,
            brandColor = Color(0xFF6366F1),
            onClick = {}
        )
    }
}
