package com.drawtaxi.app.ui.components

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50
    val coutParKm = settings.coutParKmDeplacement

    val actualPrice = ride.price.takeIf { it > 0 } ?: run {
        val priceBreakdown = PriceEngine.calculate(
            distanceKm = ride.distanceKm, dateTime = java.util.Calendar.getInstance(),
            pricePerKm = pricePerKm,
            baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
            minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
            nightSurchargePercent = settings.nightSurchargePercent,
            sundaySurchargePercent = settings.sundaySurchargePercent,
            holidaySurchargePercent = settings.holidaySurchargePercent,
            nightStartHour = settings.nightStartHour, nightEndHour = settings.nightEndHour,
            tvaTransportRate = settings.tvaTransportRate
        )
        priceBreakdown.totalTTC
    }

    // Use actual stored costs if available (completed rides), otherwise estimate
    val hasRealCosts = ride.fuelCost > 0 || ride.operatingCost > 0
    val trajetBKm = ride.distanceKm

    val fuelCostDisplay: Double
    val operatingCostDisplay: Double
    val totalKm: Double
    val isEstimated: Boolean

    if (hasRealCosts) {
        fuelCostDisplay = ride.fuelCost
        operatingCostDisplay = ride.operatingCost
        totalKm = trajetBKm
        isEstimated = false
    } else {
        // Estimate: 30% empty runs (A + C) × cout/km for fuel, operating based on duration
        val emptyKm = RideRequest.estimateEmptyKm(trajetBKm, 0.3) * 2
        fuelCostDisplay = emptyKm * coutParKm
        val estimatedDurationHours = if (ride.durationMinutes > 0) ride.durationMinutes / 60.0 else (trajetBKm / 40.0)
        operatingCostDisplay = estimatedDurationHours * settings.operatingCostPerHour
        totalKm = trajetBKm + emptyKm
        isEstimated = true
    }

    val totalCost = fuelCostDisplay + operatingCostDisplay
    val netProfit = actualPrice - totalCost
    val profitabilityPercent = if (actualPrice > 0.001) ((netProfit / actualPrice) * 100).coerceIn(-999.0, 100.0) else 0.0

    val (statusColor, statusText, statusIcon) = when {
        profitabilityPercent >= 50 -> Triple(drawTaxiColors().tertiary, "Excellente rentabilité", Icons.AutoMirrored.Filled.TrendingUp)
        profitabilityPercent >= 30 -> Triple(Amber500, "Rentabilité correcte", Icons.Default.CheckCircle)
        profitabilityPercent >= 15 -> Triple(Orange500, "Rentabilité faible", Icons.Default.Warning)
        else -> Triple(drawTaxiColors().error, "Course non rentable", Icons.AutoMirrored.Filled.TrendingDown)
    }

    DrawTaxiCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 0.dp,
        backgroundColor = drawTaxiColors().surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(settings.brandColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        DrawTaxiIcon(Icons.Default.Analytics, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        androidx.compose.material3.Text(
                            "Analyse de rentabilité",
                            style = drawTaxiType().titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = drawTaxiColors().onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.material3.Text(
                                statusText,
                                style = drawTaxiType().bodySmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                            if (isEstimated) {
                                androidx.compose.material3.Text(
                                    "• estimé",
                                    style = drawTaxiType().labelSmall,
                                    color = drawTaxiColors().onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                DrawTaxiChip(
                    containerColor = statusColor.copy(alpha = 0.12f),
                    labelColor = statusColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DrawTaxiIcon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    androidx.compose.material3.Text(
                        String.format(Locale.getDefault(), "%.0f%%", profitabilityPercent),
                        style = drawTaxiType().labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            DrawTaxiProgressBar(
                progress = (profitabilityPercent / 100).coerceIn(0.0, 1.0).toFloat(),
                color = statusColor,
                height = 6.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(drawTaxiColors().surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                CostRow(label = "Chiffre d'affaires", value = actualPrice, color = drawTaxiColors().tertiary, isPositive = true, isBold = true)
                Spacer(modifier = Modifier.height(12.dp))
                DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Text(
                    "COÛTS${if (isEstimated) " (ESTIMÉS)" else " (RÉELS)"}",
                    style = drawTaxiType().labelSmall,
                    color = drawTaxiColors().onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                CostRow(
                    label = if (isEstimated) "Coût kilométrique (est.)" else "Coût kilométrique",
                    value = fuelCostDisplay,
                    color = drawTaxiColors().onSurfaceVariant,
                    isPositive = false
                )
                Spacer(modifier = Modifier.height(6.dp))
                CostRow(
                    label = if (isEstimated) "Charges exploitation (est.)" else "Charges exploitation",
                    value = operatingCostDisplay,
                    color = drawTaxiColors().onSurfaceVariant,
                    isPositive = false
                )
                Spacer(modifier = Modifier.height(10.dp))
                DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))
                CostRow(label = "Total des coûts", value = totalCost, color = drawTaxiColors().onSurface, isPositive = false, isBold = true)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    "BÉNÉFICE NET",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = drawTaxiColors().onSurface
                )
                androidx.compose.material3.Text(
                    text = String.format(Locale.getDefault(), "%.2f €", netProfit),
                    style = drawTaxiType().headlineSmall,
                    color = if (netProfit >= 0) drawTaxiColors().tertiary else drawTaxiColors().error,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Route,
                    label = "Trajet",
                    value = "${String.format(Locale.getDefault(), "%.1f", trajetBKm)} km",
                    modifier = Modifier.weight(1f)
                )
                if (ride.durationMinutes > 0) {
                    InfoChip(
                        icon = Icons.Default.Schedule,
                        label = "Durée",
                        value = "${ride.durationMinutes} min",
                        modifier = Modifier.weight(1f)
                    )
                }
                InfoChip(
                    icon = Icons.Default.LocalOffer,
                    label = "€/km",
                    value = if (trajetBKm > 0) String.format(Locale.getDefault(), "%.2f", actualPrice / trajetBKm) else "–",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CostRow(label: String, value: Double, color: Color, isPositive: Boolean, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(
            text = label,
            style = if (isBold) drawTaxiType().bodyLarge else drawTaxiType().bodyMedium,
            color = if (isBold) drawTaxiColors().onSurface else drawTaxiColors().onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        androidx.compose.material3.Text(
            text = "${if (isPositive) "" else "-"}${String.format(Locale.getDefault(), "%.2f €", value)}",
            style = if (isBold) drawTaxiType().bodyLarge else drawTaxiType().bodyMedium,
            color = if (isBold && isPositive) drawTaxiColors().tertiary else color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(drawTaxiColors().surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DrawTaxiIcon(icon, contentDescription = null, tint = drawTaxiColors().onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.material3.Text(text = label, style = drawTaxiType().labelSmall, color = drawTaxiColors().onSurfaceVariant)
        androidx.compose.material3.Text(text = value, style = drawTaxiType().bodySmall, fontWeight = FontWeight.Bold, color = drawTaxiColors().onSurface)
    }
}

@Composable
fun QuoteProfitabilityDialog(
    ride: RideRequest, settings: AppSettings, isCalculating: Boolean = false, onConfirm: () -> Unit, onCancel: () -> Unit
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
                if (isCalculating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(color = settings.brandColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Text(
                                "Calcul de l'itinéraire et de la rentabilité...",
                                style = drawTaxiType().bodyMedium,
                                color = drawTaxiColors().onSurfaceVariant
                            )
                        }
                    }
                } else {
                    ProfitabilityAnalysisCard(ride = ride, settings = settings)
                }
            }
        },
        confirmButton = {
            DrawTaxiSolidButton(onClick = onConfirm, containerColor = settings.brandColor, enabled = !isCalculating) {
                DrawTaxiIcon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
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
