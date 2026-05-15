package com.drawtaxi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RideHistoryItem(
    ride: RideRequest,
    brandColor: Color,
    iconColor: Color,
    isUpcoming: Boolean,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(ride.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isUpcoming) iconColor.copy(alpha = 0.15f) else Slate100,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUpcoming) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(ride.arrival, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isUpcoming) Slate900 else Slate900.copy(0.5f))
                Text(dateString, fontSize = 10.sp, color = Slate400)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("%.2f €", ride.price),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isUpcoming) brandColor else Slate900
            )
            Text(
                if (isUpcoming) "À venir" else "Détail",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isUpcoming) brandColor.copy(0.6f) else Slate400,
                textDecoration = if (isUpcoming) null else TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RideHistoryItemPreview() {
    val mockRide = RideRequest(
        id = "1",
        sender = "Marie",
        body = "Taxi pour demain",
        departure = "Gare du Nord",
        arrival = "Aéroport CDG",
        time = "10:30",
        timestamp = System.currentTimeMillis() + 86400000
    )
    Box(modifier = Modifier.padding(16.dp)) {
        RideHistoryItem(
            ride = mockRide,
            brandColor = Color(0xFFE11D48),
            iconColor = Color(0xFFE11D48),
            isUpcoming = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideHistoryItemPastPreview() {
    val mockRide = RideRequest(
        id = "2",
        sender = "Jean",
        body = "Taxi maintenant",
        departure = "Place de Clichy",
        arrival = "Bastille",
        time = "14:15",
        timestamp = System.currentTimeMillis() - 3600000
    )
    Box(modifier = Modifier.padding(16.dp)) {
        RideHistoryItem(
            ride = mockRide,
            brandColor = Color(0xFFE11D48),
            iconColor = Color(0xFFE11D48),
            isUpcoming = false,
            onClick = {}
        )
    }
}
