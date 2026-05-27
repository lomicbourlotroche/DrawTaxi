package com.drawtaxi.app.ui.screens.rides

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import java.util.Locale

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
        modifier = Modifier.padding(bottom = 8.dp),
        onClick = { onClick(ride) }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.arrival,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Slate900,
                        lineHeight = 28.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prévu à ${ride.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                Surface(
                    color = brandColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (ride.price > 0) String.format(Locale.getDefault(), "%.2f €", ride.price) else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = brandColor
                        )
                        if (ride.distanceKm > 0) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", ride.distanceKm)} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = brandColor.copy(0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Client",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                        Text(
                            text = ride.sender,
                            style = MaterialTheme.typography.labelLarge,
                            color = Slate900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDelete(ride) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color.Red.copy(0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { onValidate(ride) },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Valider",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isExpanded = !isExpanded },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Masquer les détails" else "Voir le message original",
                        style = MaterialTheme.typography.labelMedium,
                        color = brandColor.copy(0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = brandColor.copy(0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .drawBehind {
                            drawLine(
                                color = brandColor.copy(0.2f),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                ) {
                    Text(
                        text = ride.body,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Slate700,
                        modifier = Modifier.padding(start = 12.dp),
                        lineHeight = 20.sp
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
