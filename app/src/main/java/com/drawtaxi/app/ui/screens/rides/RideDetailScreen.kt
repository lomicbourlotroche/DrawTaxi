package com.drawtaxi.app.ui.screens.rides

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.*
import com.drawtaxi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    ride: RideRequest,
    onBack: () -> Unit,
    onDelete: (RideRequest) -> Unit,
    onEdit: (RideRequest) -> Unit = {},
    onStartRide: (RideRequest) -> Unit = {},
    isPending: Boolean = false,
    settings: AppSettings,
    onShareLocation: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val brandColor = settings.brandColor
    var showTemplateDialog by remember { mutableStateOf(false) }

    // Calcul de rentabilité basé sur le prix réel de la course
    val actualPrice = ride.price.takeIf { it > 0 } ?: run {
        val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
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
        priceBreakdown.totalTTC
    }

    val distanceDomicileKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: ride.distanceKm * 0.3
    val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
    val totalCost = coutDeplacement
    val netProfit = actualPrice - totalCost
    val profitability = if (actualPrice > 0) {
        (netProfit / actualPrice) * 100
    } else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Course", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(ride.date.ifBlank { "" }, style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (isPending) {
                        AssistChip(
                            onClick = {},
                            label = { Text("En attente") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDelete(ride); onBack() },
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                    }

                    IconButton(
                        onClick = { onEdit(ride) },
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = brandColor.copy(alpha = 0.1f),
                            contentColor = brandColor
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }

                    if (isPending) {
                        Button(
                            onClick = { onStartRide(ride) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Commencer", fontWeight = FontWeight.Bold)
                        }
                    } else if (ride.status == com.drawtaxi.app.data.RideStatus.CONFIRMED || 
                               ride.status == com.drawtaxi.app.data.RideStatus.IN_PROGRESS) {
                        // Course confirmée → Démarrer la navigation
                        Button(
                            onClick = { onStartRide(ride) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Démarrer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPending && ride.departure.isNotBlank()) {
                RouteToClientMap(pickupAddress = ride.departure, brandColor = brandColor)
            }

            RideDetailMapSection(departure = ride.departure, arrival = ride.arrival, brandColor = brandColor)

            RideDetailClientCard(
                ride = ride,
                brandColor = brandColor,
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${ride.sender}") }
                    context.startActivity(intent)
                },
                onMessage = { message ->
                    com.drawtaxi.app.logic.sms.SmsUtils.sendSms(context, ride.sender, message)
                },
                onShareLocation = onShareLocation,
                messageTemplates = settings.messageTemplates,
                showTemplates = { showTemplateDialog = true }
            )

            RideDetailTripCard(ride = ride, brandColor = brandColor)

            RideDetailPriceCard(ride = ride, settings = settings, brandColor = brandColor)

            // Afficher la rentabilité calculée avec le prix réel de la course
            RideDetailProfitabilityCard(
                ride = ride,
                profitability = profitability,
                netProfit = netProfit,
                coutDeplacement = coutDeplacement,
                distanceDomicileKm = distanceDomicileKm,
                totalPrice = actualPrice,
                brandColor = brandColor
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun RideDetailProfitabilityCard(
    ride: RideRequest,
    profitability: Double,
    netProfit: Double,
    coutDeplacement: Double,
    distanceDomicileKm: Double,
    totalPrice: Double,
    brandColor: Color
) {
    val profitColor = when {
        profitability >= 50 -> Green500
        profitability >= 30 -> Color(0xFFFFA500)
        else -> Red500
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rentabilité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${String.format("%.0f", profitability)}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = profitColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfitabilityStatItem(label = "Prix TTC", value = String.format("%.2f €", totalPrice), modifier = Modifier.weight(1f))
                ProfitabilityStatItem(label = "Déplacement", value = String.format("%.2f €", coutDeplacement), modifier = Modifier.weight(1f))
                ProfitabilityStatItem(label = "Distance domicile", value = String.format("%.1f km", distanceDomicileKm), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bénéfice net", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                Text(
                    text = String.format("%.2f €", netProfit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netProfit > 0) Green500 else Red500
                )
            }

            if (ride.durationMinutes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Revenu/heure", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                    Text(
                        text = String.format("%.2f €/h", (ride.price / ride.durationMinutes) * 60),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitabilityStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate800)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailClientCard(
    ride: RideRequest,
    brandColor: Color,
    onCall: () -> Unit,
    onMessage: (String) -> Unit,
    onShareLocation: (() -> Unit)? = null,
    messageTemplates: List<String> = emptyList(),
    showTemplates: () -> Unit = {}
) {
    var showTemplateSheet by remember { mutableStateOf(false) }
    
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = brandColor.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = (ride.clientName.ifBlank { ride.sender }).take(2).uppercase(), fontWeight = FontWeight.Bold, color = brandColor)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = ride.clientName.ifBlank { "Client" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = ride.sender, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onCall, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Appeler", fontSize = MaterialTheme.typography.labelMedium.fontSize)
                }
                FilledTonalButton(onClick = { showTemplateSheet = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Message", fontSize = MaterialTheme.typography.labelMedium.fontSize)
                }
                if (onShareLocation != null) {
                    FilledTonalButton(
                        onClick = onShareLocation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GPS", fontSize = MaterialTheme.typography.labelMedium.fontSize)
                    }
                }
            }
        }
    }
    
    if (showTemplateSheet) {
        ModalBottomSheet(onDismissRequest = { showTemplateSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Envoyer un message", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(onClick = { onMessage(""); showTemplateSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Écrire un message...")
                }
                
                if (messageTemplates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    messageTemplates.forEach { template ->
                        Card(
                            onClick = { onMessage(template); showTemplateSheet = false },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = template, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RideDetailTripCard(ride: RideRequest, brandColor: Color) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = ride.time.ifBlank { "—" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = brandColor)
                }
                if (ride.date.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(text = ride.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(12.dp), shape = RoundedCornerShape(6.dp), color = Green500) {}
                    Surface(modifier = Modifier.width(2.dp).height(40.dp), color = MaterialTheme.colorScheme.outlineVariant) {}
                    Surface(modifier = Modifier.size(12.dp), shape = RoundedCornerShape(6.dp), color = brandColor) {}
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Column(modifier = Modifier.padding(bottom = 32.dp)) {
                        Text(text = "DÉPART", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text(text = ride.departure.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text(text = "ARRIVÉE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text(text = ride.arrival.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (ride.distanceKm > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${String.format("%.1f", ride.distanceKm)} km", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "~${((ride.distanceKm * 2).toInt())} min", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                    }
                }
            }
        }
    }
}

@Composable
fun RideDetailPriceCard(ride: RideRequest, settings: AppSettings, brandColor: Color) {
    val basePrice = settings.basePrice.toDoubleOrNull() ?: 2.60
    val perKm = settings.pricePerKm.toDoubleOrNull() ?: 1.20
    val calculatedPrice = basePrice + (ride.distanceKm * perKm)
    val displayedPrice = if (ride.price > 0) ride.price else calculatedPrice

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Prix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = if (displayedPrice > 0) String.format("%.2f €", displayedPrice) else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (displayedPrice > 0) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ride.price == 0.0 && ride.distanceKm > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                PriceRow("Prise en charge", String.format("%.2f €", basePrice))
                PriceRow("${String.format("%.1f", ride.distanceKm)} km × ${String.format("%.2f", perKm)} €/km", String.format("%.2f €", ride.distanceKm * perKm))
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "TOTAL ESTIMÉ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format("%.2f €", displayedPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                }
            }

            if (ride.price > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(text = "Prix confirmé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
