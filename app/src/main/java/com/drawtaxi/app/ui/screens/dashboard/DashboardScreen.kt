package com.drawtaxi.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.pricing.DashboardPeriod
import com.drawtaxi.app.logic.pricing.DailyBreakdown
import com.drawtaxi.app.logic.pricing.PeriodStats
import com.drawtaxi.app.logic.pricing.RideCalculator
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    validatedRides: List<RideRequest>,
    pendingRides: List<RideRequest>,
    brandColor: Color,
    onNavigateToInvoices: () -> Unit,
    onRideClick: (RideRequest) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(DashboardPeriod.WEEK) }
    var showDailyDetail by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DailyBreakdown?>(null) }

    val completedRides = remember(validatedRides) {
        validatedRides.filter { !it.isPending && it.status == RideStatus.COMPLETED }
    }

    val periodStats = remember(completedRides, selectedPeriod) {
        RideCalculator.calculatePeriodStats(completedRides, selectedPeriod)
    }

    val dailyBreakdown = remember(completedRides) {
        RideCalculator.calculateDailyBreakdown(completedRides)
    }

    val todayStats = remember(completedRides) {
        RideCalculator.calculatePeriodStats(completedRides, DashboardPeriod.TODAY)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Slate50),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tableau de bord",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "${periodStats.totalRides} courses • ${String.format("%.2f €", periodStats.totalRevenue)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DashboardPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label, fontSize = 12.sp) },
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
            DashboardHeroCard(stats = periodStats, brandColor = brandColor)
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            QuickStatsRow(stats = periodStats, brandColor = brandColor)
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            TodayMiniCard(stats = todayStats, brandColor = brandColor)
        }

        if (dailyBreakdown.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Détail par jour",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showDailyDetail = !showDailyDetail }) {
                                Text(if (showDailyDetail) "Réduire" else "Voir tout")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val daysToShow = if (showDailyDetail) dailyBreakdown.size else dailyBreakdown.size.coerceAtMost(5)
                        dailyBreakdown.take(daysToShow).forEach { day ->
                            DailyRow(
                                breakdown = day,
                                brandColor = brandColor,
                                onClick = {
                                    selectedDay = day
                                }
                            )
                            if (day != dailyBreakdown.take(daysToShow).last()) {
                                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Actions rapides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Receipt,
                            label = "Factures",
                            color = brandColor,
                            onClick = onNavigateToInvoices,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            icon = Icons.Default.LocalTaxi,
                            label = "${pendingRides.size} en attente",
                            color = Amber500,
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(stats: PeriodStats, brandColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(brandColor, brandColor.copy(alpha = 0.85f))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = String.format("%.2f €", stats.totalRevenue),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Revenus totaux",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HeroStatItem(
                        value = String.format("%.0f%%", stats.avgProfitability),
                        label = "Rentabilité",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = if (stats.avgProfitability >= 70) Color(0xFF86EFAC) else if (stats.avgProfitability >= 50) Color(0xFFFDE68A) else Color(0xFFFCA5A5)
                    )
                    HeroStatItem(
                        value = String.format("%.2f €", stats.totalNetProfit),
                        label = "Bénéfice net",
                        icon = Icons.Default.AttachMoney,
                        color = if (stats.totalNetProfit > 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    )
                    HeroStatItem(
                        value = stats.totalRides.toString(),
                        label = "Courses",
                        icon = Icons.Default.LocalTaxi,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun QuickStatsRow(stats: PeriodStats, brandColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatCard(
            value = String.format("%.2f €", stats.totalFuelCost),
            label = "Carburant",
            icon = Icons.Default.LocalGasStation,
            color = Amber500,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            value = String.format("%.2f €", stats.totalOpCost),
            label = "Coûts ops",
            icon = Icons.Default.Build,
            color = Slate500,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            value = String.format("%.2f €/km", stats.avgPerKm),
            label = "Revenu/km",
            icon = Icons.Default.Route,
            color = brandColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
        }
    }
}

@Composable
private fun TodayMiniCard(stats: PeriodStats, brandColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = brandColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Aujourd'hui", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${stats.totalRides} courses • ${String.format("%.0f km", stats.totalKm)}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format("%.2f €", stats.totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(String.format("%.0f%%", stats.avgProfitability), style = MaterialTheme.typography.labelSmall, color = if (stats.avgProfitability >= 70) Green500 else if (stats.avgProfitability >= 50) Amber500 else Red500)
            }
        }
    }
}

@Composable
private fun DailyRow(breakdown: DailyBreakdown, brandColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = breakdown.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = "${breakdown.rideCount} courses • ${String.format("%.0f km", breakdown.totalKm)}", style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = String.format("%.2f €", breakdown.totalRevenue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    breakdown.profitability >= 70 -> Green100
                    breakdown.profitability >= 50 -> Amber100
                    else -> Red100
                },
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.0f", breakdown.profitability)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        breakdown.profitability >= 70 -> Green800
                        breakdown.profitability >= 50 -> Color(0xFF92400E)
                        else -> Red800
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
