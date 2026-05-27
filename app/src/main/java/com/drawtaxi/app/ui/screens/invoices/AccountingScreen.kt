package com.drawtaxi.app.ui.screens.invoices



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.material.icons.automirrored.filled.TrendingUp

import androidx.compose.material.icons.filled.*

import androidx.compose.material.Text
import com.drawtaxi.app.ui.components.core.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview

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



@Deprecated("Replaced by DashboardScreen", level = DeprecationLevel.WARNING)
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

    val totalCoutDeplacement = filteredRides.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * 0.15) }

    val totalNetProfit = totalRevenue - totalCoutDeplacement
    val avgProfitability = if (totalRevenue > 0) (totalNetProfit / totalRevenue) * 100.0 else 0.0

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
                } catch (_: Exception) { null }

                

                if (parsed != null) {

                    calendar.time = parsed

                }

                

                val dayRevenue = rides.sumOf { it.price }

                val dayCoutDeplacement = rides.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * 0.15) }

                val dayNet = dayRevenue - dayCoutDeplacement
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
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Comptabilité & Rentabilité",
                style = drawTaxiType().headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountingPeriod.entries.forEach { period ->
                    DrawTaxiFilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) },
                        colors = DrawTaxiFilterChipDefaults.colors(
                            selectedContainerColor = brandColor.copy(alpha = 0.1f),
                            selectedContentColor = brandColor,
                            selectedBorderColor = brandColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            
            DrawTaxiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Revenus totaux",
                        style = drawTaxiType().labelLarge,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f €", totalRevenue),
                        style = drawTaxiType().displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfitStatColumn(value = String.format(Locale.getDefault(), "%.0f%%", avgProfitability), label = "rentabilité", icon = Icons.Default.Analytics, color = if (avgProfitability >= 70) Green600 else if (avgProfitability >= 50) Color(0xFFD97706) else Red600)
                        ProfitStatColumn(value = String.format(Locale.getDefault(), "%.2f €", totalNetProfit), label = "bénéfice net", icon = Icons.AutoMirrored.Filled.TrendingUp, color = if (totalNetProfit > 0) Green600 else Red600)
                        ProfitStatColumn(value = totalRides.toString(), label = "courses", icon = Icons.Default.LocalTaxi)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    DrawTaxiDivider(color = Slate100.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfitStatColumn(value = String.format(Locale.getDefault(), "%.2f €", totalCoutDeplacement), label = "déplacement", icon = Icons.Default.Route)
                        ProfitStatColumn(value = String.format(Locale.getDefault(), "%.2f €/km", averagePerKm), label = "revenu/km", icon = Icons.Default.Route)
                    }
                }
            }
        }

        if (dailyStats.isNotEmpty()) {
            item {
                DrawTaxiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Détail par jour", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        Spacer(modifier = Modifier.height(16.dp))
                        dailyStats.take(7).forEach { stats ->
                            DailyStatRowWithProfit(stats = stats)
                            if (stats != dailyStats.take(7).last()) {
                                DrawTaxiDivider(color = Slate100.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            DrawTaxiCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = brandColor.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            DrawTaxiIcon(Icons.Default.Receipt, contentDescription = null, tint = brandColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Générer des factures", style = drawTaxiType().titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                            Text("Exportez vos courses pour Kolecto", style = drawTaxiType().bodySmall, color = Slate500)
                        }
                    }
                    DrawTaxiSolidButton(
                        onClick = onNavigateToInvoices,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = brandColor,
                        minHeight = 44.dp,
                        modifier = Modifier.width(56.dp)
                    ) {
                        DrawTaxiIcon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DrawTaxiOutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Export CSV")
                }
                DrawTaxiSolidButton(
                    onClick = onNavigateToInvoices,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = brandColor
                ) {
                    DrawTaxiIcon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factures", color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

}



@Composable
private fun ProfitStatColumn(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Slate900) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Slate100, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = Slate600, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = drawTaxiType().labelSmall, color = Slate500)
    }
}

@Composable
private fun DailyStatRowWithProfit(stats: DailyStats) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Slate50, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(Icons.Default.CalendarToday, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = stats.dayName.uppercase(), style = drawTaxiType().labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
                Text(text = stats.date, style = drawTaxiType().labelSmall, color = Slate400)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format(Locale.getDefault(), "%.2f €", stats.totalRevenue), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = "${stats.rideCount} courses • ${String.format(Locale.getDefault(), "%.0f", stats.totalKm)} km", style = drawTaxiType().labelSmall, color = Slate500)
            }
            Spacer(modifier = Modifier.width(12.dp))
            DrawTaxiSurface(shape = RoundedCornerShape(8.dp), color = when {
                stats.avgProfitability >= 70 -> Green100
                stats.avgProfitability >= 50 -> Color(0xFFFFF3CD)
                else -> Red100
            }) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", stats.avgProfitability)}%",
                    style = drawTaxiType().labelMedium,
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



@Suppress("DEPRECATION")
@Preview(showBackground = true)
@Composable
fun AccountingScreenPreview() {

    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    val sampleRides = listOf(

        RideRequest(

            id = "1",

            sender = "0601020304",

            body = "Course 1",

            departure = "Gare de Lyon",

            arrival = "Orly",

            time = "10:00",

            date = today,

            timestamp = System.currentTimeMillis(),

            price = 45.0,

            distanceKm = 18.5,

            isPending = false

        ),

        RideRequest(

            id = "2",

            sender = "0601020304",

            body = "Course 2",

            departure = "Eiffel Tower",

            arrival = "Louvre",

            time = "14:30",

            date = today,

            timestamp = System.currentTimeMillis(),

            price = 25.0,

            distanceKm = 5.2,

            isPending = false

        )

    )



    DrawTaxiTheme(brandColor = Indigo500) {

        AccountingScreen(

            validatedRides = sampleRides,

            brandColor = Indigo500,

            onExportCsv = {},

            onNavigateToInvoices = {}

        )

    }

}



