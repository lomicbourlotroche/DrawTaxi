package com.drawtaxi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.Slate400
import com.drawtaxi.app.ui.theme.Slate900

@Composable
fun RideDetailInfoCard(
    ride: RideRequest,
    brandColor: Color,
    onArrivalNotify: () -> Unit
) {
    TaxiCard { 
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = brandColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(ride.sender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate400.copy(0.2f))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DÉPART", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Text(ride.departure, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ARRIVÉE", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Text(ride.arrival, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Icon(Icons.Default.AccessTime, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                 Spacer(modifier = Modifier.width(4.dp))
                 Text(ride.time, fontWeight = FontWeight.Bold)
            }
            if (ride.price > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("${ride.price} €", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = brandColor)
            }

            // Je suis arrivé Action
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onArrivalNotify,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("JE SUIS ARRIVÉ", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

@Preview
@Composable
fun RideDetailInfoCardPreview() {
    RideDetailInfoCard(
        ride = RideRequest(
            id = "preview_id",
            sender = "Jean Dupont",
            body = "Course de test",
            departure = "Gare de Lyon",
            arrival = "Aéroport CDG",
            time = "14:30",
            price = 45.0
        ),
        brandColor = Color.Blue,
        onArrivalNotify = {}
    )
}
