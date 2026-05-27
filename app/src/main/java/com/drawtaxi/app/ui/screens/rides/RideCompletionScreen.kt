package com.drawtaxi.app.ui.screens.rides

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RideCompletionScreen(
    ride: RideRequest,
    settings: AppSettings,
    onComplete: (RideRequest) -> Unit,
    onBack: () -> Unit
) {
    var departure by remember { mutableStateOf(ride.departure) }
    var arrival by remember { mutableStateOf(ride.arrival) }
    var distanceKm by remember { mutableStateOf(ride.distanceKm.toString()) }
    var price by remember { mutableStateOf(ride.price.toString()) }
    var durationMinutes by remember { mutableStateOf(ride.durationMinutes.toString()) }
    var clientEmail by remember { mutableStateOf(ride.clientEmail) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val hasChanges = departure != ride.departure ||
            arrival != ride.arrival ||
            distanceKm != ride.distanceKm.toString() ||
            price != ride.price.toString()

    val now = java.util.Calendar.getInstance()
    val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
        distanceKm = distanceKm.toDoubleOrNull() ?: 0.0,
        dateTime = now,
        pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50,
        baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
        minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
        nightSurchargePercent = settings.nightSurchargePercent,
        sundaySurchargePercent = settings.sundaySurchargePercent,
        holidaySurchargePercent = settings.holidaySurchargePercent,
        nightStartHour = settings.nightStartHour,
        nightEndHour = settings.nightEndHour,
        tvaTransportRate = settings.tvaTransportRate
    )
    
    val currentDistance = distanceKm.toDoubleOrNull() ?: 0.0
    val currentPrice = price.toDoubleOrNull() ?: priceBreakdown.totalTTC
    val currentDuration = durationMinutes.toDoubleOrNull() ?: 0.0

    val calculatedFuelCost = currentDistance * settings.fuelCostPerKm
    val calculatedOperatingCost = (currentDuration / 60.0) * settings.operatingCostPerHour
    val totalCost = calculatedFuelCost + calculatedOperatingCost
    val profitability = if (currentPrice > 0.0) {
        ((currentPrice - totalCost) / currentPrice) * 100.0
    } else 0.0

    Column(modifier = Modifier.fillMaxSize().background(drawTaxiColors().background)) {
        DrawTaxiTopBar(
            title = {
                Column {
                    Text("Terminer la course")
                    if (hasChanges) {
                        Text(
                            text = "Modifications détectées",
                            style = drawTaxiType().bodySmall,
                            color = Color(0xFFCA8A04)
                        )
                    }
                }
            },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            backgroundColor = Color.Transparent
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DrawTaxiCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informations de la course", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        
                        DrawTaxiTextField(
                            value = departure,
                            onValueChange = { departure = it },
                            label = "Lieu de départ",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { DrawTaxiIcon(Icons.Default.TripOrigin, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(18.dp)) }
                        )

                        DrawTaxiTextField(
                            value = arrival,
                            onValueChange = { arrival = it },
                            label = "Lieu d'arrivée",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { DrawTaxiIcon(Icons.Default.Place, contentDescription = null, tint = Red500, modifier = Modifier.size(18.dp)) }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DrawTaxiTextField(
                                value = distanceKm,
                                onValueChange = { distanceKm = it },
                                label = "Distance (km)",
                                modifier = Modifier.weight(1f),
                                leadingIcon = { DrawTaxiIcon(Icons.Default.Route, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) }
                            )

                            DrawTaxiTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = "Prix (€)",
                                modifier = Modifier.weight(1f),
                                leadingIcon = { DrawTaxiIcon(Icons.Default.AttachMoney, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) }
                            )
                        }

                        DrawTaxiTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = "Durée (minutes)",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { DrawTaxiIcon(Icons.Default.Timer, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            item {
                DrawTaxiCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = settings.brandColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Analytics, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Rentabilité estimée", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        when {
                                            profitability >= 50 -> "Excellente"
                                            profitability >= 30 -> "Correcte"
                                            profitability >= 15 -> "Faible"
                                            else -> "Non rentable"
                                        },
                                        style = drawTaxiType().bodySmall,
                                        color = when {
                                            profitability >= 50 -> Green500
                                            profitability >= 30 -> Amber500
                                            profitability >= 15 -> Color(0xFFFF6B00)
                                            else -> Red500
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            val pColor = when {
                                profitability >= 50 -> Green500
                                profitability >= 30 -> Amber500
                                profitability >= 15 -> Color(0xFFFF6B00)
                                else -> Red500
                            }
                            Surface(color = pColor.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    "${String.format(Locale.getDefault(), "%.0f", profitability)}%",
                                    style = drawTaxiType().headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = pColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress bar
                        val pColor2 = when {
                            profitability >= 50 -> Green500
                            profitability >= 30 -> Amber500
                            profitability >= 15 -> Color(0xFFFF6B00)
                            else -> Red500
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(pColor2.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((profitability / 100.0).coerceIn(0.0, 1.0).toFloat())
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(pColor2)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Breakdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(drawTaxiColors().surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Prix TTC", style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold)
                                Text("+${String.format(Locale.getDefault(), "%.2f €", currentPrice)}", style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold, color = Green500)
                            }
                            DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.3f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Coût kilométrique (${String.format(Locale.getDefault(), "%.1f", currentDistance)} km × ${String.format(Locale.getDefault(), "%.2f", settings.fuelCostPerKm)} €)", style = drawTaxiType().bodySmall, color = drawTaxiColors().onSurfaceVariant)
                                Text("-${String.format(Locale.getDefault(), "%.2f €", calculatedFuelCost)}", style = drawTaxiType().bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exploitation (${durationMinutes.toIntOrNull() ?: 0} min)", style = drawTaxiType().bodySmall, color = drawTaxiColors().onSurfaceVariant)
                                Text("-${String.format(Locale.getDefault(), "%.2f €", calculatedOperatingCost)}", style = drawTaxiType().bodySmall, fontWeight = FontWeight.Medium)
                            }
                            DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.3f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total coûts", style = drawTaxiType().bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("-${String.format(Locale.getDefault(), "%.2f €", totalCost)}", style = drawTaxiType().bodySmall, fontWeight = FontWeight.SemiBold, color = Red500)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Net profit badge
                        val netProfit = currentPrice - totalCost
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = (if (netProfit > 0) Green500 else Red500).copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (netProfit > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (netProfit > 0) Green500 else Red500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Bénéfice net", style = drawTaxiType().titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    String.format(Locale.getDefault(), "%.2f €", netProfit),
                                    style = drawTaxiType().titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netProfit > 0) Green500 else Red500
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Détail du prix", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Prix de base", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.basePrice), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distance (${String.format(Locale.getDefault(), "%.1f", distanceKm.toDoubleOrNull() ?: 0.0)} km)", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.distancePrice), fontWeight = FontWeight.Medium)
                        }
                        
                        if (priceBreakdown.isNight) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration nuit", color = Amber500)
                                Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.nightSurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        if (priceBreakdown.isSunday) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration dimanche", color = Amber500)
                                Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.sundaySurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        if (priceBreakdown.isHoliday) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration férié", color = Amber500)
                                Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.holidaySurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        
                        DrawTaxiDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sous-total HT", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.subtotalHT), fontWeight = FontWeight.Bold)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TVA (10%)", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.tvaAmount), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL TTC", fontWeight = FontWeight.Bold, color = settings.brandColor)
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f €", priceBreakdown.totalTTC),
                                fontWeight = FontWeight.Bold,
                                color = settings.brandColor
                            )
                        }
                    }
                }
            }

            item {
                DrawTaxiCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Coordonnées client", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Téléphone", color = drawTaxiColors().onSurfaceVariant)
                            Text(ride.sender, fontWeight = FontWeight.Medium)
                        }

                        DrawTaxiTextField(
                            value = clientEmail,
                            onValueChange = { clientEmail = it },
                            label = "Email client",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { DrawTaxiIcon(Icons.Default.Email, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                DrawTaxiSolidButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    containerColor = settings.brandColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Terminer la course", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmer la fin de course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Trajet: ${departure.ifBlank { "—" }} → ${arrival.ifBlank { "—" }}")
                    Text("Distance: ${distanceKm} km")
                    Text("Prix: ${price} €")
                    Text("Durée: ${durationMinutes} min")
                    Text("Rentabilité: ${String.format(Locale.getDefault(), "%.0f", profitability)}%")
                }
            },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        val dist = distanceKm.toDoubleOrNull() ?: ride.distanceKm
                        val prc = price.toDoubleOrNull() ?: priceBreakdown.totalTTC
                        val dur = durationMinutes.toIntOrNull() ?: 0
                        
                        val fuelCostValue = dist * settings.fuelCostPerKm
                        val operatingCostValue = (dur / 60.0) * settings.operatingCostPerHour
                        val totalCostValue = fuelCostValue + operatingCostValue
                        val profit = if (prc > 0.0) {
                            ((prc - totalCostValue) / prc) * 100.0
                        } else 0.0
                        
                        val updatedRide = ride.copy(
                            departure = departure,
                            arrival = arrival,
                            distanceKm = dist,
                            price = prc,
                            clientEmail = clientEmail,
                            durationMinutes = dur,
                            fuelCost = fuelCostValue,
                            operatingCost = operatingCostValue,
                            profitabilityPercent = profit,
                            priceBreakdown = priceBreakdown.summary
                        )
                        showConfirmDialog = false
                        onComplete(updatedRide)
                    },
                    containerColor = settings.brandColor
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideCompletionScreenPreview() {
    val sampleSettings = AppSettings()
    val sampleRide = RideRequest(
        id = "1",
        sender = "0612345678",
        body = "Taxi depuis Paris vers Lyon à 14h30",
        departure = "Paris",
        arrival = "Lyon",
        time = "14:30",
        price = 45.0,
        distanceKm = 18.5
    )
    DrawTaxiTheme {
        RideCompletionScreen(
            ride = sampleRide,
            settings = sampleSettings,
            onComplete = {},
            onBack = {}
        )
    }
}


