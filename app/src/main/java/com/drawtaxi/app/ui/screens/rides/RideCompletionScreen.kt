package com.drawtaxi.app.ui.screens.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.HorizontalDivider
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val distanceDomicileKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: 5.0
    val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
    val profitability = if (calculatedPrice > 0) {
        RideRequest.calculateProfitability(calculatedPrice, coutDeplacement)
    } else 0.0

    Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
        TopAppBar(
            title = {
                Column {
                    Text("Terminer la course")
                    if (hasChanges) {
                        Text(
                            text = "Modifications détectées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCA8A04)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informations de la course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = departure,
                            onValueChange = { departure = it },
                            label = { Text("Lieu de départ") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.TripOrigin, contentDescription = null, tint = settings.brandColor) }
                        )

                        OutlinedTextField(
                            value = arrival,
                            onValueChange = { arrival = it },
                            label = { Text("Lieu d'arrivée") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Red500) }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = distanceKm,
                                onValueChange = { distanceKm = it },
                                label = { Text("Distance (km)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) }
                            )

                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Prix (€)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                            )
                        }

                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Durée (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rentabilité estimée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rentabilité", color = Slate500)
                            Text(
                                text = "${String.format("%.0f", profitability)}%",
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
                            Text(String.format("%.1f km", distanceDomicileKm), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coût déplacement", color = Slate500)
                            Text(String.format("%.2f €", coutDeplacement), fontWeight = FontWeight.Medium)
                        }
                        
                        val netProfit = calculatedPrice - coutDeplacement
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bénéfice net", fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.2f €", netProfit),
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
                        Text("Détail du prix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Prix de base", color = Slate500)
                            Text(String.format("%.2f €", priceBreakdown.basePrice), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distance (${String.format("%.1f", distanceKm.toDoubleOrNull() ?: 0.0)} km)", color = Slate500)
                            Text(String.format("%.2f €", priceBreakdown.distancePrice), fontWeight = FontWeight.Medium)
                        }
                        
                        if (priceBreakdown.isNight) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration nuit", color = Amber500)
                                Text(String.format("%.2f €", priceBreakdown.nightSurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        if (priceBreakdown.isSunday) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration dimanche", color = Amber500)
                                Text(String.format("%.2f €", priceBreakdown.sundaySurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        if (priceBreakdown.isHoliday) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Majoration férié", color = Amber500)
                                Text(String.format("%.2f €", priceBreakdown.holidaySurcharge), fontWeight = FontWeight.Medium, color = Amber500)
                            }
                        }
                        
                        if (ride.waitMinutes > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Attente (${ride.waitMinutes} min)", color = Slate500)
                                Text(String.format("%.2f €", priceBreakdown.waitTimePrice), fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sous-total HT", color = Slate500)
                            Text(String.format("%.2f €", priceBreakdown.subtotalHT), fontWeight = FontWeight.Bold)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TVA (10%)", color = Slate500)
                            Text(String.format("%.2f €", priceBreakdown.tvaAmount), fontWeight = FontWeight.Medium)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL TTC", fontWeight = FontWeight.Bold, color = settings.brandColor)
                            Text(
                                text = String.format("%.2f €", priceBreakdown.totalTTC),
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
                        Text("Coordonnées client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Téléphone", color = Slate500)
                            Text(ride.sender, fontWeight = FontWeight.Medium)
                        }

                        OutlinedTextField(
                            value = clientEmail,
                            onValueChange = { clientEmail = it },
                            label = {                     Text("Email client")
                         },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = settings.brandColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Terminer la course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text("Rentabilité: ${String.format("%.0f", profitability)}%")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dist = distanceKm.toDoubleOrNull() ?: ride.distanceKm
                        val prc = price.toDoubleOrNull() ?: priceBreakdown.totalTTC
                        val dur = durationMinutes.toIntOrNull() ?: 0
                        val depDistKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: 5.0
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
                    colors = ButtonDefaults.buttonColors(containerColor = settings.brandColor)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
