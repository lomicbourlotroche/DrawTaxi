package com.drawtaxi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PendingRideItem(
    ride: RideRequest,
    onValidate: (RideRequest) -> Unit,
    onDelete: (RideRequest) -> Unit,
    onClick: (RideRequest) -> Unit,
    brandColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }

    TaxiCard(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clickable { onClick(ride) }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "De: ${ride.sender}",
                        style = MaterialTheme.typography.labelSmall,
                        color = brandColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ride.arrival,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Prévu à ${ride.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                    Surface(
                    color = brandColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = brandColor
                        )
                        Text(
                            text = if (ride.distanceKm > 0) "${String.format("%.1f", ride.distanceKm)} km" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = brandColor.copy(0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Row {
                    IconButton(onClick = { onDelete(ride) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red.copy(0.4f))
                    }
                    Button(
                        onClick = { onValidate(ride) },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Valider", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    if (isExpanded) "Masquer les détails" else "Voir le message original",
                    fontSize = 11.sp,
                    color = Slate400,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Slate50, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .drawBehind {
                            drawLine(
                                color = brandColor,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                ) {
                    Text(
                        text = ride.body,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = Slate700,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PendingRideItemPreview() {
    val mockRide = RideRequest(
        id = "p1",
        sender = "0612345678",
        body = "Bonjour, je voudrais un taxi depuis la Gare de Lyon vers l'Opéra pour 18h30. Merci.",
        departure = "Gare de Lyon",
        arrival = "Opéra",
        time = "18:30",
        price = 24.50,
        distanceKm = 5.2,
        timestamp = System.currentTimeMillis()
    )
    Box(modifier = Modifier.padding(8.dp)) {
        PendingRideItem(
            ride = mockRide,
            onValidate = {},
            onDelete = {},
            onClick = {},
            brandColor = Color(0xFFE11D48)
        )
    }
}
