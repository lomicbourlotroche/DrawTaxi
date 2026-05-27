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
    messageTemplates: List<String> = emptyList()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rideToDelete by remember { mutableStateOf<RideRequest?>(null) }
    var selectedTemplate by remember { mutableStateOf("") }
    var showQuoteDialog by remember { mutableStateOf(false) }
    var rideForQuote by remember { mutableStateOf<RideRequest?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                        color = Slate900
                    )
                    Text(
                        text = if (pendingRides.isEmpty()) "Aucune demande en attente"
                               else "${pendingRides.size} demande${if (pendingRides.size > 1) "s" else ""} en attente",
                        style = drawTaxiType().bodyMedium,
                        color = Slate500
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
                            .background(Slate100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Tout est à jour !",
                        style = drawTaxiType().titleLarge,
                        color = Slate900,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune nouvelle demande de course pour le moment. Relaxez-vous !",
                        style = drawTaxiType().bodyMedium,
                        color = Slate500,
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

    

    // Dialog d'analyse de rentabilité avant envoi du devis

    if (showQuoteDialog && rideForQuote != null) {

        QuoteProfitabilityDialog(

            ride = rideForQuote!!,

            settings = settings,

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
    containerColor: Color = Color.White,
    contentColor: Color = Slate600
) {
    val isPrimary = containerColor != Color.White
    
    DrawTaxiSurface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        borderWidth = if (isPrimary) 0.dp else 1.dp,
        borderColor = Slate100
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
                tint = if (isPrimary) Color.White else contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = drawTaxiType().labelSmall,
                color = if (isPrimary) Color.White else contentColor,
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
    val (statusBg, statusText) = when (ride.status) {
        RideStatus.DRAFT -> Pair(Yellow100, "Brouillon")
        RideStatus.QUOTED -> Pair(Blue100, "Devis envoyé")
        RideStatus.CONFIRMED -> Pair(Green100, "Confirmée")
        RideStatus.IN_PROGRESS -> Pair(brandColor.copy(alpha = 0.1f), "En cours")
        RideStatus.COMPLETED -> Pair(Slate100, "Terminée")
        RideStatus.CANCELLED -> Pair(Red100, "Annulée")
        RideStatus.ABSENT -> Pair(Slate100, "Absent")
    }
    
    val statusColor = when (ride.status) {
        RideStatus.DRAFT -> Yellow800
        RideStatus.QUOTED -> Blue800
        RideStatus.CONFIRMED -> Green800
        RideStatus.IN_PROGRESS -> brandColor
        RideStatus.COMPLETED -> Slate600
        RideStatus.CANCELLED -> Red800
        RideStatus.ABSENT -> Orange600
    }

    DrawTaxiCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = 2.dp,
        backgroundColor = Color.White
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
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ride.time.ifBlank { "—:—" },
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    if (ride.date.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = Slate200)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ride.date,
                            style = drawTaxiType().labelSmall,
                            color = Slate500
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
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = ride.arrival.ifBlank { "Destination non définie" },
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (ride.departure.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "depuis ${ride.departure}",
                            style = drawTaxiType().bodySmall,
                            color = Slate500,
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
                        color = if (ride.price > 0) brandColor else Slate400
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", ride.distanceKm)} km",
                        style = drawTaxiType().labelSmall,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DrawTaxiDivider(color = Slate50)
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
                            .background(Slate50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ride.sender.ifBlank { "Client inconnu" },
                        style = drawTaxiType().bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )
                }
                
                if (ride.status == RideStatus.DRAFT || ride.status == RideStatus.QUOTED || ride.status == RideStatus.CONFIRMED) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Rose50, CircleShape)
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
                                onClick = onAcceptQuote,
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
                                onClick = onValidate,
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



