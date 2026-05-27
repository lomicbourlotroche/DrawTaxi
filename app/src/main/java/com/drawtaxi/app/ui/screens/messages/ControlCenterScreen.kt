package com.drawtaxi.app.ui.screens.messages

import java.util.Locale



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.drawtaxi.app.data.*

import com.drawtaxi.app.ui.components.QuoteProfitabilityDialog

import com.drawtaxi.app.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.logic.pricing.PriceEngine
import java.util.Calendar
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch



@Composable
fun ControlCenterScreen(
    pendingRides: List<RideRequest>,
    onValidate: (RideRequest) -> Unit,
    onDelete: (RideRequest) -> Unit,
    onRideClick: (RideRequest) -> Unit,
    brandColor: Color,
    onCreateRide: () -> Unit,
    settings: AppSettings = AppSettings(),
    onCheckSms: () -> Unit = {},
    onSendQuote: (RideRequest) -> Unit = {},
    onAcceptQuote: (RideRequest) -> Unit = {},
    onRejectQuote: (RideRequest) -> Unit = {},
    onDeleteWithMessage: (RideRequest, String) -> Unit = { _, _ -> },
    onOpenAgenda: () -> Unit = {},
    messageTemplates: List<String> = emptyList(),
    onUpdateRide: (RideRequest) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rideToDelete by remember { mutableStateOf<RideRequest?>(null) }
    var selectedTemplate by remember { mutableStateOf("") }
    var showQuoteDialog by remember { mutableStateOf(false) }
    var rideForQuote by remember { mutableStateOf<RideRequest?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isCalculatingRoute by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(drawTaxiColors().background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(drawTaxiColors().surface)
                .statusBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Centre de contrôle",
                        style = drawTaxiType().displaySmall,
                        color = drawTaxiColors().onSurface
                    )
                    Text(
                        text = if (pendingRides.isEmpty()) "Aucune demande en attente"
                               else "${pendingRides.size} demande${if (pendingRides.size > 1) "s" else ""} en attente",
                        style = drawTaxiType().bodyMedium,
                        color = drawTaxiColors().onSurfaceVariant
                    )
                }
                
                if (pendingRides.isNotEmpty()) {
                    IconButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Rose50, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Rose500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "Créer",
                    onClick = onCreateRide,
                    modifier = Modifier.weight(1f),
                    containerColor = brandColor
                )
                QuickActionButton(
                    icon = Icons.Default.CalendarMonth,
                    label = "Agenda",
                    onClick = onOpenAgenda,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Refresh,
                    label = "Vérifier",
                    onClick = onCheckSms,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingRides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(drawTaxiColors().surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = drawTaxiColors().onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Tout est à jour !",
                        style = drawTaxiType().titleLarge,
                        color = drawTaxiColors().onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune nouvelle demande de course pour le moment. Relaxez-vous !",
                        style = drawTaxiType().bodyMedium,
                        color = drawTaxiColors().onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    DrawTaxiOutlinedButton(
                        onClick = onCreateRide,
                        shape = RoundedCornerShape(16.dp),
                        contentColor = brandColor
                    ) {
                        Text("Créer manuellement", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(pendingRides) { ride ->
                    EnhancedRideCard(
                        ride = ride,
                        brandColor = brandColor,
                        onValidate = { onValidate(ride) },
                        onDelete = {
                            rideToDelete = ride
                            showDeleteDialog = true
                        },
                        onSendQuote = {
                            rideForQuote = ride
                            showQuoteDialog = true
                            scope.launch {
                                isCalculatingRoute = true
                                try {
                                    val dep = ride.departure
                                    val arr = ride.arrival
                                    if (dep.isNotBlank() && arr.isNotBlank()) {
                                        val depLoc = GeocodingService.geocode(dep, context)
                                        val arrLoc = GeocodingService.geocode(arr, context)
                                        if (depLoc != null && arrLoc != null) {
                                            val route = NavigationEngine.fetchRoute(
                                                fromLat = depLoc.latitude, fromLng = depLoc.longitude,
                                                toLat = arrLoc.latitude, toLng = arrLoc.longitude
                                            )
                                            if (route != null) {
                                                val realDistance = route.distance / 1000.0
                                                val realDuration = (route.duration / 60.0).toInt()
                                                
                                                val priceBreakdown = PriceEngine.calculate(
                                                    distanceKm = realDistance,
                                                    dateTime = Calendar.getInstance(),
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
                                                val finalPrice = priceBreakdown.totalTTC
                                                
                                                val emptyKm = RideRequest.estimateEmptyKm(realDistance, 0.3) * 2
                                                val fuelCost = emptyKm * settings.coutParKmDeplacement
                                                val operatingCost = (realDuration / 60.0) * settings.operatingCostPerHour
                                                val totalCost = fuelCost + operatingCost
                                                val netProfit = finalPrice - totalCost
                                                val profitability = if (finalPrice > 0.001) ((netProfit / finalPrice) * 100).coerceIn(-999.0, 100.0) else 0.0
                                                
                                                val updatedRide = ride.copy(
                                                    distanceKm = realDistance,
                                                    durationMinutes = realDuration,
                                                    price = finalPrice,
                                                    fuelCost = fuelCost,
                                                    operatingCost = operatingCost,
                                                    profitabilityPercent = profitability
                                                )
                                                rideForQuote = updatedRide
                                                onUpdateRide(updatedRide)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ControlCenterScreen", "Error calculating real distance: ${e.message}")
                                } finally {
                                    isCalculatingRoute = false
                                }
                            }
                        },
                        onAcceptQuote = { onAcceptQuote(ride) },
                        onRejectQuote = { onRejectQuote(ride) },
                        onClick = { onRideClick(ride) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }



    if (showDeleteDialog && rideToDelete != null) {

        AlertDialog(

            onDismissRequest = {

                showDeleteDialog = false

                rideToDelete = null

                selectedTemplate = ""

            },

            title = { Text("Supprimer la course ?") },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("Choisissez un message à envoyer au client:")



                    if (messageTemplates.isNotEmpty()) {

                        messageTemplates.forEach { template ->

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                RadioButton(

                                    selected = selectedTemplate == template,

                                    onClick = { selectedTemplate = template },

                                    colors = RadioButtonDefaults.colors(selectedColor = brandColor)

                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(

                                    text = template,

                                    style = drawTaxiType().bodySmall,

                                    modifier = Modifier.weight(1f)

                                )

                            }

                        }

                    }



                    OutlinedTextField(

                        value = if (selectedTemplate in messageTemplates) "" else selectedTemplate,

                        onValueChange = { selectedTemplate = it },

                        label = { Text("Ou écrivez un message...") },

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(12.dp),

                        maxLines = 3

                    )

                }

            },

            confirmButton = {

                DrawTaxiSolidButton(

                    onClick = {

                        rideToDelete?.let { ride ->

                            if (selectedTemplate.isNotBlank()) {

                                onDeleteWithMessage(ride, selectedTemplate)

                            } else {

                                onDelete(ride)

                            }

                        }

                        showDeleteDialog = false

                        rideToDelete = null

                        selectedTemplate = ""

                    },

                    containerColor = Red500

                ) {

                    Text("Supprimer")

                }

            },

            dismissButton = {

                DrawTaxiSolidButton(onClick = {

                    showDeleteDialog = false

                    rideToDelete = null

                    selectedTemplate = ""

                }) {

                    Text("Annuler")

                }

            }

        )

    }

    

    if (showQuoteDialog && rideForQuote != null) {

        QuoteProfitabilityDialog(

            ride = rideForQuote!!,

            settings = settings,

            isCalculating = isCalculatingRoute,

            onConfirm = {

                onSendQuote(rideForQuote!!)

                showQuoteDialog = false

                rideForQuote = null

            },

            onCancel = {

                showQuoteDialog = false

                rideForQuote = null

            }

        )

    }

    

    // Dialog de confirmation pour tout supprimer

    if (showDeleteAllDialog) {

        AlertDialog(

            onDismissRequest = { showDeleteAllDialog = false },

            icon = { 

                Icon(

                    Icons.Default.DeleteSweep,

                    contentDescription = null,

                    tint = Rose500,

                    modifier = Modifier.size(32.dp)

                )

            },

            title = { Text("Supprimer tous les brouillons ?") },

            text = {

                Column {

                    Text(

                        "Vous êtes sur le point de supprimer ${pendingRides.size} course${if (pendingRides.size > 1) "s" else ""} en attente.",

                        style = drawTaxiType().bodyMedium

                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(

                        "Cette action est irréversible.",

                        style = drawTaxiType().bodySmall,

                        color = Rose500,

                        fontWeight = FontWeight.Bold

                    )

                }

            },

            confirmButton = {

                DrawTaxiSolidButton(

                    onClick = {

                        // Supprimer toutes les courses en attente

                        pendingRides.forEach { ride ->

                            onDelete(ride)

                        }

                        showDeleteAllDialog = false

                    },

                    containerColor = Rose500

                ) {

                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Tout supprimer")

                }

            },

            dismissButton = {

                DrawTaxiSolidButton(onClick = { showDeleteAllDialog = false }) {

                    Text("Annuler")

                }

            }

        )

    }

}



@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.Unspecified
) {
    val isPrimary = containerColor != Color.Transparent && containerColor != drawTaxiColors().surface
    val finalContainer = if (containerColor == Color.Transparent) drawTaxiColors().surface else containerColor
    val finalContent = if (contentColor == Color.Unspecified) drawTaxiColors().onSurfaceVariant else contentColor
    
    DrawTaxiSurface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = finalContainer,
        borderWidth = if (isPrimary) 0.dp else 1.dp,
        borderColor = drawTaxiColors().outline
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isPrimary) Color.White else finalContent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = drawTaxiType().labelSmall,
                color = if (isPrimary) Color.White else finalContent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EnhancedRideCard(
    ride: RideRequest,
    brandColor: Color,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
    onSendQuote: () -> Unit,
    onAcceptQuote: () -> Unit,
    onRejectQuote: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isDark = drawTaxiColors().surface == SurfaceDark
    val (statusBg, statusText) = when (ride.status) {
        RideStatus.DRAFT -> Pair(if (isDark) Yellow800.copy(alpha = 0.2f) else Yellow100, "Brouillon")
        RideStatus.QUOTED -> Pair(if (isDark) Blue800.copy(alpha = 0.2f) else Blue100, "Devis envoyé")
        RideStatus.CONFIRMED -> Pair(if (isDark) Green800.copy(alpha = 0.2f) else Green100, "Confirmée")
        RideStatus.IN_PROGRESS -> Pair(brandColor.copy(alpha = 0.15f), "En cours")
        RideStatus.COMPLETED -> Pair(drawTaxiColors().surfaceVariant, "Terminée")
        RideStatus.CANCELLED -> Pair(if (isDark) Red800.copy(alpha = 0.2f) else Red100, "Annulée")
        RideStatus.ABSENT -> Pair(drawTaxiColors().surfaceVariant, "Absent")
    }
    
    val statusColor = when (ride.status) {
        RideStatus.DRAFT -> if (isDark) Yellow500 else Yellow800
        RideStatus.QUOTED -> if (isDark) Blue500 else Blue800
        RideStatus.CONFIRMED -> if (isDark) Green500 else Green800
        RideStatus.IN_PROGRESS -> brandColor
        RideStatus.COMPLETED -> drawTaxiColors().onSurfaceVariant
        RideStatus.CANCELLED -> if (isDark) Red500 else Red800
        RideStatus.ABSENT -> Orange600
    }

    DrawTaxiCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = 2.dp,
        backgroundColor = drawTaxiColors().surface
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = drawTaxiColors().onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ride.time.ifBlank { "—:—" },
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = drawTaxiColors().onSurface
                    )
                    if (ride.date.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = drawTaxiColors().outline)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ride.date,
                            style = drawTaxiType().labelSmall,
                            color = drawTaxiColors().onSurfaceVariant
                        )
                    }
                }
                
                DrawTaxiStatusChip(
                    statusText = statusText,
                    containerColor = statusBg,
                    labelColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ARRIVÉE",
                        style = drawTaxiType().labelSmall,
                        color = drawTaxiColors().onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = ride.arrival.ifBlank { "Destination non définie" },
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Black,
                        color = drawTaxiColors().onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (ride.departure.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "depuis ${ride.departure}",
                            style = drawTaxiType().bodySmall,
                            color = drawTaxiColors().onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (ride.price > 0) String.format(Locale.getDefault(), "%.2f €", ride.price) else "Devis",
                        style = drawTaxiType().headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (ride.price > 0) brandColor else drawTaxiColors().onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", ride.distanceKm)} km",
                        style = drawTaxiType().labelSmall,
                        color = drawTaxiColors().onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(drawTaxiColors().surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = drawTaxiColors().onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ride.sender.ifBlank { "Client inconnu" },
                        style = drawTaxiType().bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = drawTaxiColors().onSurface
                    )
                }
                
                if (ride.status == RideStatus.DRAFT || ride.status == RideStatus.QUOTED || ride.status == RideStatus.CONFIRMED) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Rose500.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Rose500, modifier = Modifier.size(18.dp))
                        }

                        if (ride.status == RideStatus.DRAFT) {
                            DrawTaxiSolidButton(
                                onClick = onSendQuote,
                                shape = RoundedCornerShape(12.dp),
                                containerColor = brandColor,
                                minHeight = 36.dp
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, modifier = Modifier.size(14.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Devis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (ride.status == RideStatus.QUOTED) {
                            DrawTaxiSolidButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onAcceptQuote()
                                },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color(0xFF10B981),
                                minHeight = 36.dp
                            ) {
                                Icon(Icons.Default.Check, modifier = Modifier.size(14.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accepter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (ride.status == RideStatus.CONFIRMED) {
                            DrawTaxiSolidButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onValidate()
                                },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color(0xFF10B981),
                                minHeight = 36.dp
                            ) {
                                Icon(Icons.Default.Navigation, modifier = Modifier.size(14.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Démarrer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ControlCenterScreenPreview() {
    DrawTaxiTheme {
        ControlCenterScreen(
            pendingRides = emptyList(),
            onValidate = {},
            onDelete = {},
            onRideClick = {},
            brandColor = Color(0xFF6366F1),
            onCreateRide = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ControlCenterScreenWithDataPreview() {
    val sampleRides = listOf(
        RideRequest(
            id = "1",
            sender = "Jean Dupont",
            body = "Course aéroport",
            arrival = "Aéroport de Paris-CDG",
            departure = "Paris Centre",
            time = "14:30",
            date = "12 Oct",
            price = 55.0,
            distanceKm = 28.5,
            status = RideStatus.DRAFT
        ),
        RideRequest(
            id = "2",
            sender = "Marie Curie",
            body = "Gare",
            arrival = "Gare de Lyon",
            departure = "Boulogne",
            time = "16:00",
            date = "12 Oct",
            price = 35.0,
            distanceKm = 12.0,
            status = RideStatus.QUOTED
        ),
        RideRequest(
            id = "3",
            sender = "Albert Einstein",
            body = "Versailles",
            arrival = "Château de Versailles",
            departure = "Paris 16e",
            time = "09:00",
            date = "13 Oct",
            price = 45.0,
            distanceKm = 18.0,
            status = RideStatus.CONFIRMED
        )
    )
    DrawTaxiTheme {
        ControlCenterScreen(
            pendingRides = sampleRides,
            onValidate = {},
            onDelete = {},
            onRideClick = {},
            brandColor = Color(0xFF6366F1),
            onCreateRide = {}
        )
    }
}



