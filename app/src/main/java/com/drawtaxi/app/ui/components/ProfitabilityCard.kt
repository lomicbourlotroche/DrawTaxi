package com.drawtaxi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.pricing.PriceEngine
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*

@Composable
fun ProfitabilityAnalysisCard(
    ride: RideRequest,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val actualPrice = ride.price.takeIf { it > 0 } ?: run {
        val priceBreakdown = PriceEngine.calculate(
            distanceKm = ride.distanceKm, dateTime = java.util.Calendar.getInstance(),
            pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50,
            baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
            minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
            nightSurchargePercent = settings.nightSurchargePercent,
            sundaySurchargePercent = settings.sundaySurchargePercent,
            holidaySurchargePercent = settings.holidaySurchargePercent,
            euroPerMinute = settings.euroPerMinute,
            nightStartHour = settings.nightStartHour, nightEndHour = settings.nightEndHour,
            tvaTransportRate = settings.tvaTransportRate, tvaWaitTimeRate = settings.tvaWaitTimeRate
        )
        priceBreakdown.totalTTC
    }
    val distanceDomicileKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: ride.distanceKm * 0.3
    val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
    val totalCost = coutDeplacement
    val netProfit = actualPrice - totalCost
    val profitabilityPercent = if (actualPrice > 0) (netProfit / actualPrice) * 100 else 0.0

    val (statusColor, statusText, statusIcon) = when {
        profitabilityPercent >= 50 -> Triple(Emerald500, "Excellente rentabilite", Icons.Default.TrendingUp)
        profitabilityPercent >= 30 -> Triple(Amber500, "Rentabilite correcte", Icons.Default.CheckCircle)
        profitabilityPercent >= 15 -> Triple(Orange500, "Rentabilite faible", Icons.Default.Warning)
        else -> Triple(Rose500, "Course non rentable", Icons.Default.TrendingDown)
    }

    DrawTaxiCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrawTaxiIcon(Icons.Default.Analytics, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text("Analyse de rentabilite", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                }
                DrawTaxiChip(containerColor = statusColor.copy(alpha = 0.1f), labelColor = statusColor) {
                    DrawTaxiIcon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Text(String.format("%.0f%%", profitabilityPercent), style = drawTaxiType().labelMedium, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DrawTaxiProgressBar(progress = (profitabilityPercent / 100).coerceIn(0.0, 1.0).toFloat(), color = statusColor)
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.Text(statusText, style = drawTaxiType().bodySmall, color = statusColor, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            DrawTaxiDivider()
            Spacer(modifier = Modifier.height(16.dp))
            CostRow(label = "Prix total TTC", value = actualPrice, color = Emerald500, isPositive = true)
            Spacer(modifier = Modifier.height(8.dp))
            DrawTaxiDivider()
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text("Couts estimes", style = drawTaxiType().labelSmall, color = Slate500, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            CostRow(label = "Deplacement", value = coutDeplacement, color = Slate600, isPositive = false)
            Spacer(modifier = Modifier.height(8.dp))
            DrawTaxiDivider()
            Spacer(modifier = Modifier.height(8.dp))
            CostRow(label = "BENEFICE NET", value = netProfit, color = if (netProfit >= 0) Emerald500 else Rose500, isPositive = netProfit >= 0, isBold = true)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InfoChip(icon = Icons.Default.Schedule, label = "Duree", value = "${ride.durationMinutes} min")
                InfoChip(icon = Icons.Default.LocalGasStation, label = "Carburant", value = String.format("%.1f L", ride.distanceKm * 0.07))
                InfoChip(icon = Icons.Default.Speed, label = "Distance", value = "${String.format("%.1f", ride.distanceKm)} km")
            }
        }
    }
}

@Composable
private fun CostRow(label: String, value: Double, color: Color, isPositive: Boolean, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(text = label, style = if (isBold) drawTaxiType().bodyMedium else drawTaxiType().bodySmall, color = if (isBold) Slate900 else Slate600, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        androidx.compose.material3.Text(text = "${if (isPositive) "" else "-"}${String.format("%.2f", value)} euro", style = drawTaxiType().bodyMedium, color = color, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DrawTaxiIcon(icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.Text(text = label, style = drawTaxiType().labelSmall, color = Slate500)
        androidx.compose.material3.Text(text = value, style = drawTaxiType().bodySmall, fontWeight = FontWeight.Bold, color = Slate700)
    }
}

@Composable
fun QuoteProfitabilityDialog(
    ride: RideRequest, settings: AppSettings, onConfirm: () -> Unit, onCancel: () -> Unit
) {
    val dep = ride.departure.ifBlank { "?" }
    val arr = ride.arrival.ifBlank { "?" }
    DrawTaxiDialog(
        onDismissRequest = onCancel,
        icon = { DrawTaxiIcon(Icons.Default.Assessment, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(28.dp)) },
        title = { androidx.compose.material3.Text("Confirmer l'envoi du devis") },
        text = {
            Column {
                androidx.compose.material3.Text("Course : $dep -> $arr", style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text("${ride.date} a ${ride.time}", style = drawTaxiType().bodySmall, color = Slate500)
                Spacer(modifier = Modifier.height(16.dp))
                ProfitabilityAnalysisCard(ride = ride, settings = settings)
            }
        },
        confirmButton = {
            DrawTaxiSolidButton(onClick = onConfirm, containerColor = settings.brandColor) {
                DrawTaxiIcon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text("Envoyer le devis")
            }
        },
        dismissButton = {
            DrawTaxiOutlinedButton(onClick = onCancel, contentColor = drawTaxiColors().primary) {
                androidx.compose.material3.Text("Annuler")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfitabilityAnalysisCardPreview() {
    val sampleSettings = AppSettings()
    val sampleRide = RideRequest(
        id = "1",
        sender = "0612345678",
        body = "Taxi depuis Paris vers Lyon à 14h30",
        departure = "Paris",
        arrival = "Lyon",
        time = "14:30",
        price = 45.0,
        distanceKm = 18.5,
        durationMinutes = 25
    )
    DrawTaxiTheme {
        ProfitabilityAnalysisCard(
            ride = sampleRide,
            settings = sampleSettings
        )
    }
}
