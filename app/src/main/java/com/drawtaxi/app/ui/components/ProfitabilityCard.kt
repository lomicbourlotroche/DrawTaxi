package com.drawtaxi.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.PriceEngine
import com.drawtaxi.app.ui.theme.*

/**
 * Carte affichant l'analyse détaillée de rentabilité d'une course
 */
@Composable
fun ProfitabilityAnalysisCard(
    ride: RideRequest,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    // Calculs détaillés
    val priceBreakdown = remember(ride, settings) {
        PriceEngine.calculate(
            distanceKm = ride.distanceKm,
            dateTime = java.util.Calendar.getInstance(),
            pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 1.20,
            baseFare = settings.basePrice.toDoubleOrNull() ?: 2.60,
            nightSurchargePercent = settings.nightSurchargePercent,
            sundaySurchargePercent = settings.sundaySurchargePercent,
            holidaySurchargePercent = settings.holidaySurchargePercent,
            euroPerMinute = settings.euroPerMinute,
            nightStartHour = settings.nightStartHour,
            nightEndHour = settings.nightEndHour,
            tvaTransportRate = settings.tvaTransportRate,
            tvaWaitTimeRate = settings.tvaWaitTimeRate
        )
    }
    
    val fuelCost = ride.distanceKm * settings.fuelCostPerKm
    val estimatedDurationHours = ride.distanceKm / if (ride.distanceKm < 10) 30.0 else 50.0
    val operatingCost = estimatedDurationHours * settings.operatingCostPerHour
    val totalCost = fuelCost + operatingCost
    val netProfit = priceBreakdown.totalTTC - totalCost
    val profitabilityPercent = if (priceBreakdown.totalTTC > 0) {
        (netProfit / priceBreakdown.totalTTC) * 100
    } else 0.0
    
    // Couleur selon rentabilité
    val (statusColor, statusText, statusIcon) = when {
        profitabilityPercent >= 50 -> Triple(Emerald500, "Excellente rentabilité", Icons.Default.TrendingUp)
        profitabilityPercent >= 30 -> Triple(Color(0xFFF59E0B), "Rentabilité correcte", Icons.Default.CheckCircle)
        profitabilityPercent >= 15 -> Triple(Orange500, "Rentabilité faible", Icons.Default.Warning)
        else -> Triple(Rose500, "Course non rentable", Icons.Default.TrendingDown)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = settings.brandColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyse de rentabilité",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.0f%%", profitabilityPercent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Barre de progression
            val progressValue = (profitabilityPercent / 100).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = progressValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Slate200
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Détail des revenus
            CostRow(
                label = "Prix total TTC",
                value = priceBreakdown.totalTTC,
                color = Emerald500,
                isPositive = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Détail des coûts
            Text(
                text = "Coûts estimés",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CostRow(
                label = "Carburant (${String.format("%.1f", ride.distanceKm)} km × ${String.format("%.2f", settings.fuelCostPerKm)} €)",
                value = fuelCost,
                color = Slate600,
                isPositive = false
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            CostRow(
                label = "Opérationnel (${String.format("%.1f", estimatedDurationHours * 60)} min × ${String.format("%.2f", settings.operatingCostPerHour / 60)} €/min)",
                value = operatingCost,
                color = Slate600,
                isPositive = false
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bénéfice net
            CostRow(
                label = "BÉNÉFICE NET",
                value = netProfit,
                color = if (netProfit >= 0) Emerald500 else Rose500,
                isPositive = netProfit >= 0,
                isBold = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Informations supplémentaires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip(
                    icon = Icons.Default.Schedule,
                    label = "Durée",
                    value = "${(estimatedDurationHours * 60).toInt()} min"
                )
                InfoChip(
                    icon = Icons.Default.LocalGasStation,
                    label = "Carburant",
                    value = String.format("%.1f L", ride.distanceKm * 0.07) // 7L/100km moyenne
                )
                InfoChip(
                    icon = Icons.Default.Speed,
                    label = "Distance",
                    value = "${String.format("%.1f", ride.distanceKm)} km"
                )
            }
        }
    }
}

@Composable
private fun CostRow(
    label: String,
    value: Double,
    color: Color,
    isPositive: Boolean,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (isBold) Slate900 else Slate600,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isPositive) "" else "-"}${String.format("%.2f", value)} €",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate500
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Slate700
        )
    }
}

/**
 * Dialog affichant l'analyse de rentabilité avant envoi du devis
 */
@Composable
fun QuoteProfitabilityDialog(
    ride: RideRequest,
    settings: AppSettings,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = settings.brandColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Confirmer l'envoi du devis")
            }
        },
        text = {
            Column {
                Text(
                    text = "Course : ${ride.departure.ifBlank { "?" }} → ${ride.arrival.ifBlank { "?" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${ride.date} à ${ride.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfitabilityAnalysisCard(
                    ride = ride,
                    settings = settings
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = settings.brandColor)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envoyer le devis")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Annuler")
            }
        }
    )
}
