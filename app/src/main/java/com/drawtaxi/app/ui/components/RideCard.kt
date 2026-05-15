package com.drawtaxi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = brandColor.copy(alpha = 0.08f)
            ),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                brandColor.copy(alpha = 0.12f),
                                brandColor.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = brandColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = ride.time.ifBlank { "—:—" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = brandColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${String.format("%.1f", ride.distanceKm)} km",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate500
                        )
                        if (ride.date.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                Text(
                                    text = ride.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = Emerald500) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ride.arrival.ifBlank { "—" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (ride.departure.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = Slate300) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ride.departure,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (ride.price > 0) brandColor else Slate400
                        )
                        if (ride.profitabilityPercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    ride.profitabilityPercent >= 70 -> Emerald100
                                    ride.profitabilityPercent >= 50 -> Color(0xFFFFF3CD)
                                    else -> Rose100
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.0f", ride.profitabilityPercent)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        ride.profitabilityPercent >= 70 -> Color(0xFF065F46)
                                        ride.profitabilityPercent >= 50 -> Color(0xFF92400E)
                                        else -> Color(0xFF9F1239)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Slate100, modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = ride.sender.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ride.sender.ifBlank { "Client" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                    
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when (ride.status) {
                                    com.drawtaxi.app.data.RideStatus.DRAFT -> "Brouillon"
                                    com.drawtaxi.app.data.RideStatus.QUOTED -> "Devis envoyé"
                                    com.drawtaxi.app.data.RideStatus.CONFIRMED -> "Confirmée"
                                    com.drawtaxi.app.data.RideStatus.IN_PROGRESS -> "En cours"
                                    com.drawtaxi.app.data.RideStatus.COMPLETED -> "Terminée"
                                    com.drawtaxi.app.data.RideStatus.CANCELLED -> "Annulée"
                                    com.drawtaxi.app.data.RideStatus.ABSENT -> "Absent"
                                    else -> "En attente"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (ride.status) {
                                com.drawtaxi.app.data.RideStatus.DRAFT -> Amber100
                                com.drawtaxi.app.data.RideStatus.QUOTED -> Blue100
                                com.drawtaxi.app.data.RideStatus.CONFIRMED -> Emerald100
                                else -> Slate100
                            },
                            labelColor = when (ride.status) {
                                com.drawtaxi.app.data.RideStatus.DRAFT -> Color(0xFF92400E)
                                com.drawtaxi.app.data.RideStatus.QUOTED -> Color(0xFF1E40AF)
                                com.drawtaxi.app.data.RideStatus.CONFIRMED -> Color(0xFF065F46)
                                else -> Slate600
                            }
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                }

                if (ride.isPending) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Delete, modifier = Modifier.size(16.dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = onValidate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, modifier = Modifier.size(16.dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Valider", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(10.dp), color = Emerald100, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = ride.departure.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.padding(horizontal = 6.dp).size(14.dp))
                        Text(text = ride.arrival.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = ride.time.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(text = " • ", color = Slate400)
                        Text(text = ride.sender.ifBlank { "Client" }, style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ride.price > 0) Emerald600 else Slate400
                )
                if (ride.profitabilityPercent > 0) {
                    Text(
                        text = "${String.format("%.0f", ride.profitabilityPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ride.profitabilityPercent >= 70) Emerald600 else Amber500
                    )
                }
            }
        }
    }
}
