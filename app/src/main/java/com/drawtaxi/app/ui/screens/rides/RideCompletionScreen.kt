package com.drawtaxi.app.ui.screens.rides

import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        waitMinutes = ride.waitMinutes,
        dateTime = now,
        pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50,
        baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
        minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
        nightSurchargePercent = settings.nightSurchargePercent,
        sundaySurchargePercent = settings.sundaySurchargePercent,
        holidaySurchargePercent = settings.holidaySurchargePercent,
        euroPerMinute = settings.euroPerMinute,
        nightStartHour = settings.nightStartHour,
        nightEndHour = settings.nightEndHour,
        tvaTransportRate = settings.tvaTransportRate,
        tvaWaitTimeRate = settings.tvaWaitTimeRate
    )
    
    val calculatedPrice = price.toDoubleOrNull() ?: priceBreakdown.totalTTC
    val distanceDomicileKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: (ride.distanceKm * 0.3)
    val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
    val profitability = if (calculatedPrice > 0) {
        RideRequest.calculateProfitability(calculatedPrice, coutDeplacement)
    } else 0.0

    Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rentabilité estimée", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rentabilité", color = Slate500)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.0f", profitability)}%",
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    profitability >= 70 -> Green500
                                    profitability >= 50 -> Color(0xFFFFA500)
                                    else -> Red500
                                }
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distance domicile → départ", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.1f km", distanceDomicileKm), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coût déplacement", color = Slate500)
                            Text(String.format(Locale.getDefault(), "%.2f €", coutDeplacement), fontWeight = FontWeight.Medium)
                        }
                        
                        val netProfit = calculatedPrice - coutDeplacement
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bénéfice net", fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f €", netProfit),
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit > 0) Green500 else Red500
                            )
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
                        
                        if (ride.waitMinutes > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Attente (${ride.waitMinutes} min)", color = Slate500)
                                Text(String.format(Locale.getDefault(), "%.2f €", priceBreakdown.waitTimePrice), fontWeight = FontWeight.Medium)
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Coordonnées client", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Téléphone", color = Slate500)
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
                        val depDistKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: (ride.distanceKm * 0.3)
                        val depCost = RideRequest.calculateCoutDeplacement(depDistKm, settings.coutParKmDeplacement)
                        val profit = RideRequest.calculateProfitability(prc, depCost)
                        
                        val updatedRide = ride.copy(
                            departure = departure,
                            arrival = arrival,
                            distanceKm = dist,
                            price = prc,
                            clientEmail = clientEmail,
                            durationMinutes = dur,
                            fuelCost = depCost,
                            operatingCost = 0.0,
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


