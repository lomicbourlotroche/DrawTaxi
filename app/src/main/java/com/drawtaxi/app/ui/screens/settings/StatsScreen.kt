package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.*
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Deprecated("Replaced by DashboardScreen", level = DeprecationLevel.HIDDEN)
@Composable
fun StatsScreen(validatedRides: List<RideRequest>, pendingRides: List<RideRequest>, brandColor: Color) {
    val now = Calendar.getInstance()
    val today = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time) }
    val weekAgo = remember(now) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    }
    val monthAgo = remember(now) {
        Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
    }

    val todayRides = remember(validatedRides, today) { validatedRides.filter { !it.isPending && it.date == today } }
    val weekRides = remember(validatedRides, weekAgo) { validatedRides.filter { !it.isPending && it.timestamp >= weekAgo } }
    val monthRides = remember(validatedRides, monthAgo) { validatedRides.filter { !it.isPending && it.timestamp >= monthAgo } }

    val todayRevenue = todayRides.sumOf { it.price }
    val todayKm = todayRides.sumOf { it.distanceKm }
    val todayFuel = todayRides.sumOf { it.fuelCost }
    val todayNet = todayRevenue - todayFuel - todayRides.sumOf { it.operatingCost }
    
    val weekRevenue = weekRides.sumOf { it.price }
    val weekKm = weekRides.sumOf { it.distanceKm }
    val weekFuel = weekRides.sumOf { it.fuelCost }
    val weekNet = weekRevenue - weekFuel - weekRides.sumOf { it.operatingCost }
    
    val monthRevenue = monthRides.sumOf { it.price }
    val monthKm = monthRides.sumOf { it.distanceKm }
    val monthFuel = monthRides.sumOf { it.fuelCost }
    val monthNet = monthRevenue - monthFuel - monthRides.sumOf { it.operatingCost }

    val monthProfitability = if (monthRevenue > 0) (monthNet / monthRevenue) * 100.0 else 0.0

    LazyColumn(modifier = Modifier.fillMaxSize().background(Slate50)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Statistiques", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Aujourd'hui", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard(value = String.format("%.2f €", todayRevenue), label = "revenus", icon = Icons.Default.AttachMoney, brandColor = brandColor)
                        StatCard(value = "${todayRides.size}", label = "courses", icon = Icons.Default.LocalTaxi, brandColor = brandColor)
                        StatCard(value = String.format("%.0f km", todayKm), label = "parcourus", icon = Icons.Default.Route, brandColor = brandColor)
                    }
                    if (todayRides.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatCard(value = String.format("%.2f €", todayNet), label = "bénéfice", icon = Icons.AutoMirrored.Filled.TrendingUp, brandColor = if (todayNet > 0) Green500 else Red500)
                            StatCard(value = "${String.format("%.0f", if (todayRevenue > 0) (todayNet / todayRevenue) * 100.0 else 0.0)}%", label = "rentabilité", icon = Icons.Default.Analytics, brandColor = if (todayRevenue > 0 && (todayNet / todayRevenue) * 100.0 >= 70) Green500 else if (todayRevenue > 0 && (todayNet / todayRevenue) * 100.0 >= 50) Color(0xFFFFA500) else Red500)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cette semaine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard(value = String.format("%.2f €", weekRevenue), label = "revenus", icon = Icons.AutoMirrored.Filled.TrendingUp, brandColor = brandColor)
                        StatCard(value = "${weekRides.size}", label = "courses", icon = Icons.Default.LocalTaxi, brandColor = brandColor)
                        StatCard(value = String.format("%.0f km", weekKm), label = "parcourus", icon = Icons.Default.Route, brandColor = brandColor)
                    }
                    if (weekRides.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatCard(value = String.format("%.2f €", weekNet), label = "bénéfice", icon = Icons.AutoMirrored.Filled.TrendingUp, brandColor = if (weekNet > 0) Green500 else Red500)
                            StatCard(value = "${String.format("%.0f", if (weekRevenue > 0) (weekNet / weekRevenue) * 100.0 else 0.0)}%", label = "rentabilité", icon = Icons.Default.Analytics, brandColor = if (weekRevenue > 0 && (weekNet / weekRevenue) * 100.0 >= 70) Green500 else if (weekRevenue > 0 && (weekNet / weekRevenue) * 100.0 >= 50) Color(0xFFFFA500) else Red500)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ce mois", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard(value = String.format("%.2f €", monthRevenue), label = "revenus", icon = Icons.AutoMirrored.Filled.TrendingUp, brandColor = brandColor)
                        StatCard(value = "${monthRides.size}", label = "courses", icon = Icons.Default.LocalTaxi, brandColor = brandColor)
                        StatCard(value = String.format("%.0f km", monthKm), label = "parcourus", icon = Icons.Default.Route, brandColor = brandColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard(value = String.format("%.2f €", monthNet), label = "bénéfice", icon = Icons.AutoMirrored.Filled.TrendingUp, brandColor = if (monthNet > 0) Green500 else Red500)
                        StatCard(value = "${String.format("%.0f", monthProfitability)}%", label = "rentabilité", icon = Icons.Default.Analytics, brandColor = if (monthProfitability >= 70) Green500 else if (monthProfitability >= 50) Color(0xFFFFA500) else Red500)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Moyennes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AverageRow(label = "Panier moyen (mois)", value = if (monthRides.isNotEmpty()) String.format("%.2f €", monthRevenue / monthRides.size) else "—")
                    AverageRow(label = "Revenu moyen / jour (semaine)", value = if (weekRides.isNotEmpty()) String.format("%.2f €", weekRevenue / 7) else "—")
                    AverageRow(label = "Distance moyenne / course", value = if (monthRides.isNotEmpty()) String.format("%.1f km", monthKm / monthRides.size) else "—")
                    AverageRow(label = "Rentabilité moyenne", value = if (monthRides.isNotEmpty()) String.format("%.0f%%", monthProfitability) else "—")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("État actuel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard(value = "${pendingRides.size}", label = "en attente", icon = Icons.Default.HourglassEmpty, brandColor = Color(0xFFEAB308))
                        StatCard(value = "${validatedRides.size}", label = "total courses", icon = Icons.Default.CheckCircle, brandColor = Green500)
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
private fun AverageRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate600)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
    }
}
