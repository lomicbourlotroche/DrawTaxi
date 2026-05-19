package com.drawtaxi.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.util.PermissionHelper
import com.drawtaxi.app.ui.components.*
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    validatedRides: List<RideRequest>,
    confirmedRides: List<RideRequest>,
    brandColor: Color,
    onRideClick: (RideRequest) -> Unit,
    onCreateRide: () -> Unit,
    settings: com.drawtaxi.app.data.AppSettings
) {
    val context = LocalContext.current
    val now = Calendar.getInstance()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val todayDateStr = dateFormat.format(now.time)
    val currentTimeStr = timeFormat.format(now.time)

    val isIgnoringBatteryOptimization = remember { PermissionHelper.isIgnoringBatteryOptimizations(context) }

    // Uniquement les courses terminées
    val completedRides = validatedRides.filter { it.isPending == false }.sortedWith(
        compareByDescending<RideRequest> { it.date.ifBlank { "01/01/1970" } }
            .thenByDescending { it.time.ifBlank { "00:00" } }
    )

    // Dernières courses terminées (max 5)
    val recentCompletedRides = completedRides.take(5)

    val todayCompletedRides = completedRides.filter { ride ->
        val rideDate = ride.date.ifBlank { todayDateStr }
        rideDate == todayDateStr
    }

    val todayRevenue = todayCompletedRides.sumOf { it.price }
    val totalKmToday = todayCompletedRides.sumOf { it.distanceKm }
    val todayFuel = todayCompletedRides.sumOf { it.fuelCost }
    val todayNet = todayRevenue - todayFuel - todayCompletedRides.sumOf { it.operatingCost }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Slate50)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(brandColor, brandColor.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bonjour",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = settings.name.ifBlank { "Chauffeur" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        FilledIconButton(
                            onClick = onCreateRide,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nouvelle course", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeStatCard(value = String.format("%.2f €", todayRevenue), label = "Revenus", modifier = Modifier.weight(1f))
                        HomeStatCard(value = "${todayCompletedRides.size}", label = "Courses", modifier = Modifier.weight(1f))
                        HomeStatCard(value = String.format("%.0f km", totalKmToday), label = "Km", modifier = Modifier.weight(1f))
                    }

                    if (todayCompletedRides.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Bénéfice net", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(String.format("%.2f €", todayNet), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                val profitPercent = if (todayRevenue > 0) (todayNet / todayRevenue) * 100.0 else 0.0
                                Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.2f)) {
                                    Text(
                                        text = "${String.format("%.0f", profitPercent)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isIgnoringBatteryOptimization && settings.monitorSms) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                BatteryWarningBanner(onFixClick = { PermissionHelper.requestIgnoreBatteryOptimization(context) })
            }
        }

        // Section Courses Confirmées
        if (confirmedRides.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Courses confirmées",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Emerald500,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            items(confirmedRides) { ride ->
                ConfirmedRideCard(
                    ride = ride,
                    brandColor = brandColor,
                    onClick = { onRideClick(ride) }
                )
            }
        }

        // Section Dernières courses terminées
        if (recentCompletedRides.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Dernières courses terminées",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            items(recentCompletedRides) { ride ->
                RideHistoryCard(
                    ride = ride,
                    brandColor = brandColor,
                    onClick = { onRideClick(ride) }
                )
            }
        }

        if (confirmedRides.isEmpty() && completedRides.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(24.dp), color = Slate100, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Aucune course", style = MaterialTheme.typography.titleMedium, color = Slate500)
                    Text(text = "Créez une course ou attendez les SMS", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onCreateRide,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nouvelle course", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ConfirmedRideCard(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Emerald500)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ride.arrival.ifBlank { "Destination" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (ride.departure.isNotBlank()) {
                    Text(
                        text = "Depuis ${ride.departure}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${ride.date} à ${ride.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = brandColor
                )
            }

            if (ride.price > 0) {
                Text(
                    text = String.format("%.2f €", ride.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )
            }
        }
    }
}

@Composable
private fun HomeStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
fun BatteryWarningBanner(onFixClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Rose100)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Économie de batterie actif", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF9F1239))
                Text(text = "La détection SMS peut être affectée", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9F1239).copy(alpha = 0.7f))
            }
            TextButton(onClick = onFixClick) {
                Text("Corriger", color = Color(0xFF9F1239))
            }
        }
    }
}
