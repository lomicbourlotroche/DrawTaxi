package com.drawtaxi.app.ui.screens.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class AccountingPeriod(val label: String) {
    WEEK("Semaine"),
    MONTH("Mois"),
    YEAR("Année")
}

data class DailyStats(
    val date: String,
    val dayName: String,
    val totalRevenue: Double,
    val rideCount: Int,
    val totalKm: Double,
    val avgProfitability: Double
)

@Deprecated("Replaced by DashboardScreen", level = DeprecationLevel.HIDDEN)
@Composable
fun AccountingScreen(
    validatedRides: List<RideRequest>,
    brandColor: Color,
    onExportCsv: () -> Unit,
    onNavigateToInvoices: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(AccountingPeriod.WEEK) }
    
    val filteredRides = remember(validatedRides, selectedPeriod) {
        val calendar = Calendar.getInstance()
        val startOfPeriod = when (selectedPeriod) {
            AccountingPeriod.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AccountingPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AccountingPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
        
        validatedRides.filter { it.timestamp >= startOfPeriod && !it.isPending }
    }
    
    val totalRevenue = filteredRides.sumOf { it.price }
    val totalRides = filteredRides.size
    val totalKm = filteredRides.sumOf { it.distanceKm }
    val totalFuelCost = filteredRides.sumOf { it.fuelCost }
    val totalOpCost = filteredRides.sumOf { it.operatingCost }
    val totalNetProfit = totalRevenue - totalFuelCost - totalOpCost
    val avgProfitability = if (totalRevenue > 0) (totalNetProfit / totalRevenue) * 100.0 else 0.0
    val averagePerRide = if (totalRides > 0) totalRevenue / totalRides else 0.0
    val averagePerKm = if (totalKm > 0) totalRevenue / totalKm else 0.0
    
    val dailyStats = remember(filteredRides) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        filteredRides
            .groupBy { it.date.ifBlank { "Sans date" } }
            .map { (date, rides) ->
                val calendar = Calendar.getInstance()
                val parsed = try {
                    sdf.parse(date)
                } catch (e: Exception) { null }
                
                if (parsed != null) {
                    calendar.time = parsed
                }
                
                val dayRevenue = rides.sumOf { it.price }
                val dayFuel = rides.sumOf { it.fuelCost }
                val dayOp = rides.sumOf { it.operatingCost }
                val dayNet = dayRevenue - dayFuel - dayOp
                val dayProfit = if (dayRevenue > 0) (dayNet / dayRevenue) * 100.0 else 0.0
                
                DailyStats(
                    date = date,
                    dayName = dayNameFormat.format(calendar.time),
                    totalRevenue = dayRevenue,
                    rideCount = rides.size,
                    totalKm = rides.sumOf { it.distanceKm },
                    avgProfitability = dayProfit
                )
            }
            .sortedByDescending { it.date }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Slate50)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Comptabilité & Rentabilité",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountingPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandColor,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f €", totalRevenue),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(text = "Revenus totaux", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfitStatColumn(value = String.format("%.0f%%", avgProfitability), label = "rentabilité", icon = Icons.Default.Analytics, color = if (avgProfitability >= 70) Green500 else if (avgProfitability >= 50) Color(0xFFFFA500) else Red500)
                        ProfitStatColumn(value = String.format("%.2f €", totalNetProfit), label = "bénéfice net", icon = Icons.AutoMirrored.Filled.TrendingUp, color = if (totalNetProfit > 0) Green500 else Red500)
                        ProfitStatColumn(value = totalRides.toString(), label = "courses", icon = Icons.Default.LocalTaxi)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfitStatColumn(value = String.format("%.2f €", totalFuelCost), label = "carburant", icon = Icons.Default.LocalGasStation)
                        ProfitStatColumn(value = String.format("%.2f €", totalOpCost), label = "coûts ops", icon = Icons.Default.Build)
                        ProfitStatColumn(value = String.format("%.2f €/km", averagePerKm), label = "revenu/km", icon = Icons.Default.Route)
                    }
                }
            }
        }

        if (dailyStats.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Détail par jour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        dailyStats.take(7).forEach { stats ->
                            DailyStatRowWithProfit(stats = stats, brandColor = brandColor)
                            if (stats != dailyStats.take(7).last()) {
                                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = brandColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Générer des factures", fontWeight = FontWeight.Bold)
                            Text("Créer des factures pour Kolecto", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    Button(onClick = onNavigateToInvoices, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export CSV")
                }
                Button(
                    onClick = onNavigateToInvoices,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Factures")
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ProfitStatColumn(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Slate900) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
    }
}

@Composable
private fun DailyStatRowWithProfit(stats: DailyStats, brandColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = stats.dayName.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
                Text(text = stats.date, style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format("%.2f €", stats.totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = "${stats.rideCount} courses • ${String.format("%.0f", stats.totalKm)} km", style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = when {
                stats.avgProfitability >= 70 -> Green100
                stats.avgProfitability >= 50 -> Color(0xFFFFF3CD)
                else -> Red100
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "${String.format("%.0f", stats.avgProfitability)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        stats.avgProfitability >= 70 -> Green800
                        stats.avgProfitability >= 50 -> Color(0xFF856404)
                        else -> Red800
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
