package com.drawtaxi.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.pricing.DashboardPeriod
import com.drawtaxi.app.logic.pricing.DailyBreakdown
import com.drawtaxi.app.logic.pricing.PeriodStats
import com.drawtaxi.app.logic.pricing.RideCalculator
import com.drawtaxi.app.ui.components.core.*
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
        modifier = Modifier.fillMaxSize().background(drawTaxiColors().background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            // ── HERO HEADER ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(brandColor, brandColor.copy(alpha = 0.75f), drawTaxiColors().background)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        text = "Tableau de bord",
                        style = drawTaxiType().headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${periodStats.totalRides} courses · ${String.format(Locale.getDefault(), "%.2f €", periodStats.totalRevenue)}",
                        style = drawTaxiType().bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Filtres période
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DashboardPeriod.entries.forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.15f))
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = period.label,
                                    style = drawTaxiType().labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) brandColor else Color.White
                                )
                            }
                        }
                    }
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
                DrawTaxiCard(
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
                                text = "Détail par jour",
                                style = drawTaxiType().titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DrawTaxiButton(onClick = { showDailyDetail = !showDailyDetail }) {
                                Text(text = if (showDailyDetail) "Réduire" else "Voir tout", style = drawTaxiType().labelLarge)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val daysToShow = if (showDailyDetail) dailyBreakdown.size else dailyBreakdown.size.coerceAtMost(5)
                        dailyBreakdown.take(daysToShow).forEach { day ->
                            DailyRow(
                                breakdown = day,
                                brandColor = brandColor,
                                onClick = { selectedDay = day }
                            )
                            if (day != dailyBreakdown.take(daysToShow).last()) {
                                DrawTaxiDivider(color = Slate100, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            DrawTaxiCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Actions rapides", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
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
    DrawTaxiCard(
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
                    text = String.format(Locale.getDefault(), "%.2f €", stats.totalRevenue),
                    style = drawTaxiType().displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Revenus totaux",
                    style = drawTaxiType().bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HeroStatItem(
                        value = String.format(Locale.getDefault(), "%.0f%%", stats.avgProfitability),
                        label = "Rentabilité",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = if (stats.avgProfitability >= 70) Color(0xFF86EFAC) else if (stats.avgProfitability >= 50) Color(0xFFFDE68A) else Color(0xFFFCA5A5)
                    )
                    HeroStatItem(
                        value = String.format(Locale.getDefault(), "%.2f €", stats.totalNetProfit),
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
        DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = drawTaxiType().labelSmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun QuickStatsRow(stats: PeriodStats, brandColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatCard(
            value = String.format(Locale.getDefault(), "%.0f km", stats.totalKm),
            label = "Distance",
            icon = Icons.Default.Route,
            color = Slate500,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            value = String.format(Locale.getDefault(), "%.2f €", stats.totalCoutDeplacement),
            label = "Déplacement",
            icon = Icons.Default.LocalGasStation,
            color = Amber500,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            value = String.format(Locale.getDefault(), "%.2f €/km", stats.avgPerKm),
            label = "Revenu/km",
            icon = Icons.Default.TrendingUp,
            color = brandColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    DrawTaxiCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = drawTaxiType().titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = label, style = drawTaxiType().labelSmall, color = Slate500)
        }
    }
}

@Composable
private fun TodayMiniCard(stats: PeriodStats, brandColor: Color) {
    DrawTaxiCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawTaxiSurface(shape = RoundedCornerShape(10.dp), color = brandColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        DrawTaxiIcon(imageVector = Icons.Default.Today, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Aujourd'hui", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${stats.totalRides} courses • ${String.format(Locale.getDefault(), "%.0f km", stats.totalKm)}", style = drawTaxiType().bodySmall, color = Slate500)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format(Locale.getDefault(), "%.2f €", stats.totalRevenue), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = String.format(Locale.getDefault(), "%.0f%%", stats.avgProfitability), style = drawTaxiType().labelSmall, color = if (stats.avgProfitability >= 70) Green500 else if (stats.avgProfitability >= 50) Amber500 else Red500)
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
            DrawTaxiIcon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = breakdown.date, style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = "${breakdown.rideCount} courses • ${String.format(Locale.getDefault(), "%.0f km", breakdown.totalKm)}", style = drawTaxiType().labelSmall, color = Slate500)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = String.format(Locale.getDefault(), "%.2f €", breakdown.totalRevenue), style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
            DrawTaxiSurface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    breakdown.profitability >= 70 -> Green100
                    breakdown.profitability >= 50 -> Amber100
                    else -> Red100
                },
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", breakdown.profitability)}%",
                    style = drawTaxiType().labelSmall,
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
    DrawTaxiSurface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = drawTaxiType().labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DrawTaxiTheme {
        DashboardScreen(
            validatedRides = emptyList(),
            pendingRides = emptyList(),
            brandColor = Color(0xFF6366F1),
            onNavigateToInvoices = {},
            onRideClick = {}
        )
    }
}
