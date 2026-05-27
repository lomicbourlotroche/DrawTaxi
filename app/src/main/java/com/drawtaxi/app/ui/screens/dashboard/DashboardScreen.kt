package com.drawtaxi.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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

    val last7DaysRevenue = remember(completedRides) {
        val calendar = Calendar.getInstance()
        val days = mutableListOf<Double>()
        for (i in 6 downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val year = cal.get(Calendar.YEAR)
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            
            val dayRevenue = completedRides.filter { ride ->
                val rideCal = Calendar.getInstance().apply { timeInMillis = ride.timestamp }
                rideCal.get(Calendar.YEAR) == year && rideCal.get(Calendar.DAY_OF_YEAR) == dayOfYear
            }.sumOf { it.price }
            days.add(dayRevenue)
        }
        days
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
                            colors = listOf(brandColor, brandColor.copy(alpha = 0.6f), drawTaxiColors().background)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tableau de bord",
                                style = drawTaxiType().headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${periodStats.totalRides} courses effectuées",
                                style = drawTaxiType().bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            DrawTaxiIcon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Filtres période
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardPeriod.entries.forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.15f))
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = period.label,
                                    style = drawTaxiType().labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
            DashboardHeroCard(stats = periodStats, brandColor = brandColor, last7DaysRevenue = last7DaysRevenue)
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
                                DrawTaxiDivider(color = drawTaxiColors().outline, modifier = Modifier.padding(vertical = 6.dp))
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
private fun DashboardHeroCard(stats: PeriodStats, brandColor: Color, last7DaysRevenue: List<Double>) {
    DrawTaxiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = brandColor,
        elevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle background decoration
            DrawTaxiIcon(
                imageVector = Icons.Default.LocalTaxi,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 60.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Revenus totaux",
                            style = drawTaxiType().labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f €", stats.totalRevenue),
                            style = drawTaxiType().displaySmall.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    MiniSparkline(
                        data = last7DaysRevenue,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(start = 12.dp),
                        lineColor = Color.White,
                        fillColor = Color.White.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroStatItem(
                        value = String.format(Locale.getDefault(), "%.0f%%", stats.avgProfitability),
                        label = "Rentabilité",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = if (stats.avgProfitability >= 70) Color(0xFF86EFAC) else if (stats.avgProfitability >= 50) Color(0xFFFDE68A) else Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatItem(
                        value = String.format(Locale.getDefault(), "%.2f €", stats.totalNetProfit),
                        label = "Bénéfice net",
                        icon = Icons.Default.AttachMoney,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatItem(
                        value = stats.totalRides.toString(),
                        label = "Courses",
                        icon = Icons.Default.LocalTaxi,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = drawTaxiType().titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = drawTaxiType().labelSmall, color = Color.White.copy(alpha = 0.6f))
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
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = brandColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    DrawTaxiCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = drawTaxiType().titleSmall, fontWeight = FontWeight.Bold, color = drawTaxiColors().onSurface)
            Text(text = label, style = drawTaxiType().labelSmall, color = drawTaxiColors().onSurfaceVariant)
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DrawTaxiIcon(imageVector = Icons.Default.Today, contentDescription = null, tint = brandColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Aujourd'hui", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = drawTaxiColors().onSurface)
                    Text(text = "${stats.totalRides} courses • ${String.format(Locale.getDefault(), "%.0f km", stats.totalKm)}", style = drawTaxiType().bodySmall, color = drawTaxiColors().onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format(Locale.getDefault(), "%.2f €", stats.totalRevenue), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = drawTaxiColors().onSurface)
                Text(text = String.format(Locale.getDefault(), "%.0f%%", stats.avgProfitability), style = drawTaxiType().labelSmall, fontWeight = FontWeight.Bold, color = if (stats.avgProfitability >= 70) Green500 else if (stats.avgProfitability >= 50) Amber500 else Red500)
            }
        }
    }
}

@Composable
private fun DailyRow(breakdown: DailyBreakdown, brandColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(drawTaxiColors().surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = drawTaxiColors().onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = breakdown.date, style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold, color = drawTaxiColors().onSurface)
                Text(text = "${breakdown.rideCount} courses • ${String.format(Locale.getDefault(), "%.0f km", breakdown.totalKm)}", style = drawTaxiType().labelSmall, color = drawTaxiColors().onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = String.format(Locale.getDefault(), "%.2f €", breakdown.totalRevenue), style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Black, color = drawTaxiColors().onSurface)
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(drawTaxiColors().profitabilityBgColor(breakdown.profitability))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", breakdown.profitability)}%",
                    style = drawTaxiType().labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = drawTaxiColors().profitabilityTextColor(breakdown.profitability)
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

@Preview(showBackground = true)
@Composable
fun DashboardHeroCardPreview() {
    val sampleStats = PeriodStats(
        rides = emptyList(),
        totalRevenue = 1250.50,
        totalRides = 42,
        totalKm = 850.0,
        totalCoutDeplacement = 350.0,
        totalNetProfit = 900.50,
        avgProfitability = 72.0,
        avgPerRide = 29.77,
        avgPerKm = 1.47
    )
    DrawTaxiTheme {
        DashboardHeroCard(
            stats = sampleStats,
            brandColor = Color(0xFF6366F1),
            last7DaysRevenue = listOf(100.0, 150.0, 80.0, 200.0, 120.0, 300.0, 250.0)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuickStatsRowPreview() {
    val sampleStats = PeriodStats(
        rides = emptyList(),
        totalRevenue = 1250.50,
        totalRides = 42,
        totalKm = 850.0,
        totalCoutDeplacement = 350.0,
        totalNetProfit = 900.50,
        avgProfitability = 72.0,
        avgPerRide = 29.77,
        avgPerKm = 1.47
    )
    DrawTaxiTheme {
        QuickStatsRow(
            stats = sampleStats,
            brandColor = Color(0xFF6366F1)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TodayMiniCardPreview() {
    val sampleStats = PeriodStats(
        rides = emptyList(),
        totalRevenue = 156.40,
        totalRides = 5,
        totalKm = 120.0,
        totalCoutDeplacement = 45.0,
        totalNetProfit = 111.40,
        avgProfitability = 71.0,
        avgPerRide = 31.28,
        avgPerKm = 1.30
    )
    DrawTaxiTheme {
        TodayMiniCard(
            stats = sampleStats,
            brandColor = Color(0xFF6366F1)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeroStatItemPreview() {
    DrawTaxiTheme {
        Box(modifier = Modifier.background(Color(0xFF6366F1)).padding(16.dp)) {
            HeroStatItem(
                value = "42",
                label = "Courses",
                icon = Icons.Default.LocalTaxi,
                color = Color.White
            )
        }
    }
}
