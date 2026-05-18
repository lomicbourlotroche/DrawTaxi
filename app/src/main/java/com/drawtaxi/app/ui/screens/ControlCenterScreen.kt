package com.drawtaxi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.*
import com.drawtaxi.app.ui.components.QuoteProfitabilityDialog
import com.drawtaxi.app.ui.components.RideCard
import com.drawtaxi.app.ui.theme.*

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Centre de contrôle",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = if (pendingRides.isEmpty()) "Aucune demande en attente"
                               else "${pendingRides.size} demande${if (pendingRides.size > 1) "s" else ""} en attente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenAgenda,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agenda")
                    }

                    OutlinedButton(
                        onClick = onCheckSms,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vérifier")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateRide,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer", fontSize = 12.sp)
                }
                
                // Bouton supprimer tous les brouillons
                if (pendingRides.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Rose500
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Rose500
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tout supprimer", fontSize = 12.sp, color = Rose500)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

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
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Slate200,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Aucune course en attente",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Les nouvelles demandes apparaîtront ici automatiquement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onCreateRide,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Créer une course manuellement")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
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
                            // Afficher l'analyse de rentabilité avant envoi
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
                                    style = MaterialTheme.typography.bodySmall,
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
                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Cette action est irréversible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Rose500,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Supprimer toutes les courses en attente
                        pendingRides.forEach { ride ->
                            onDelete(ride)
                        }
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tout supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Annuler")
                }
            }
        )
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brandColor.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ride.time.ifBlank { "—:—" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%.1f", ride.distanceKm)} km",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate500
                    )
                    if (ride.date.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ride.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ARRIVÉE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = ride.arrival.ifBlank { "—" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        if (ride.departure.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "depuis ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                                Text(
                                    text = ride.departure,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700
                                )
                            }
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (ride.price > 0) brandColor else Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ride.sender.ifBlank { "Client" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                    
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when (ride.status) {
                                    RideStatus.DRAFT -> "Brouillon"
                                    RideStatus.QUOTED -> "Devis envoyé"
                                    RideStatus.CONFIRMED -> "Confirmée"
                                    RideStatus.IN_PROGRESS -> "En cours"
                                    RideStatus.COMPLETED -> "Terminée"
                                    RideStatus.CANCELLED -> "Annulée"
                                    RideStatus.ABSENT -> "Absent"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (ride.status) {
                                RideStatus.DRAFT -> Yellow100
                                RideStatus.QUOTED -> Blue100
                                RideStatus.CONFIRMED -> Green100
                                else -> Slate100
                            },
                            labelColor = when (ride.status) {
                                RideStatus.DRAFT -> Yellow800
                                RideStatus.QUOTED -> Blue800
                                RideStatus.CONFIRMED -> Green800
                                else -> Slate600
                            }
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                }

                if (ride.status == RideStatus.DRAFT || ride.status == RideStatus.QUOTED || ride.status == RideStatus.CONFIRMED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, modifier = Modifier.size(16.dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer", fontSize = 12.sp)
                        }

                        if (ride.status == RideStatus.DRAFT) {
                            Button(
                                onClick = onSendQuote,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Send, modifier = Modifier.size(16.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Devis", fontSize = 12.sp)
                            }
                        }

                        if (ride.status == RideStatus.QUOTED) {
                            OutlinedButton(
                                onClick = onRejectQuote,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Close, modifier = Modifier.size(16.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refusé", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onAcceptQuote,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, modifier = Modifier.size(16.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accepté", fontSize = 12.sp)
                            }
                        }

                        if (ride.status == RideStatus.CONFIRMED) {
                            Button(
                                onClick = onValidate,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Navigation, modifier = Modifier.size(16.dp), contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Démarrer", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
