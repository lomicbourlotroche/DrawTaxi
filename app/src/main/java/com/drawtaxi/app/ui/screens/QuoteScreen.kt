package com.drawtaxi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.MessageChannel
import com.drawtaxi.app.data.Quote
import com.drawtaxi.app.data.QuoteStatus
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
    ride: RideRequest,
    quote: Quote?,
    settings: AppSettings,
    onCreateQuote: (Double, Double, MessageChannel) -> Unit,
    onSendQuote: (Quote) -> Unit,
    onAcceptQuote: (Quote) -> Unit,
    onRejectQuote: (Quote) -> Unit,
    onBack: () -> Unit
) {
    var distanceKm by remember { mutableStateOf(quote?.distanceKm?.toString() ?: ride.distanceKm.toString()) }
    var price by remember { mutableStateOf(quote?.price?.toString() ?: ride.price.toString()) }
    var selectedChannel by remember { mutableStateOf(quote?.messageChannel ?: ride.messageChannel) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Devis - Course")
                    Text(
                        text = ride.departure.ifBlank { "—" } + " → " + ride.arrival.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
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
                TaxiCard(title = "Détails de la course") {
                    DetailRow("Départ", ride.departure.ifBlank { "—" })
                    DetailRow("Arrivée", ride.arrival.ifBlank { "—" })
                    DetailRow("Client", ride.sender)
                    if (ride.clientEmail.isNotBlank()) {
                        DetailRow("Email", ride.clientEmail)
                    }
                    if (ride.time.isNotBlank()) {
                        DetailRow("Heure", ride.time)
                    }
                }
            }

            item {
                TaxiCard(title = "Créer le devis") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = distanceKm,
                            onValueChange = { distanceKm = it },
                            label = { Text("Distance (km)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Route, contentDescription = null)
                            }
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Prix (€)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null)
                            }
                        )

                        val now = java.util.Calendar.getInstance()
                        val priceBreakdown = com.drawtaxi.app.logic.PriceEngine.calculate(
                            distanceKm = distanceKm.toDoubleOrNull() ?: 0.0,
                            dateTime = now,
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
                        val calculatedPrice = priceBreakdown.totalTTC
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Prix calculé (TTC):",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                            Text(
                                text = String.format("%.2f €", calculatedPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = settings.brandColor
                            )
                        }
                        
                        if (priceBreakdown.isNight || priceBreakdown.isSunday || priceBreakdown.isHoliday) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (priceBreakdown.isNight) {
                                    Text(
                                        text = "⏰ Nuit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Amber500
                                    )
                                }
                                if (priceBreakdown.isSunday) {
                                    Text(
                                        text = "📅 Dimanche",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Amber500
                                    )
                                }
                                if (priceBreakdown.isHoliday) {
                                    Text(
                                        text = "🎉 Férié",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Amber500
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Canal d'envoi:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MessageChannel.entries.forEach { channel ->
                                FilterChip(
                                    selected = selectedChannel == channel,
                                    onClick = { selectedChannel = channel },
                                    label = {
                                        Text(
                                            text = when (channel) {
                                                MessageChannel.SMS -> "SMS"
                                                MessageChannel.WHATSAPP -> "WhatsApp"
                                                MessageChannel.EMAIL -> "Email"
                                                MessageChannel.WEB_FORM -> "Web"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (channel) {
                                                MessageChannel.SMS -> Icons.Default.Sms
                                                MessageChannel.WHATSAPP -> Icons.AutoMirrored.Filled.Chat
                                                MessageChannel.EMAIL -> Icons.Default.Email
                                                MessageChannel.WEB_FORM -> Icons.Default.Language
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val dist = distanceKm.toDoubleOrNull() ?: ride.distanceKm
                                val p = price.toDoubleOrNull() ?: calculatedPrice
                                onCreateQuote(dist, p, selectedChannel)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = settings.brandColor)
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Créer le devis")
                        }
                    }
                }
            }

            if (quote != null) {
                item {
                    TaxiCard(title = "Devis actuel") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailRow("Distance", String.format("%.1f km", quote.distanceKm))
                            DetailRow("Prix", String.format("%.2f €", quote.price))
                            DetailRow("Canal", when (quote.messageChannel) {
                                MessageChannel.SMS -> "SMS"
                                MessageChannel.WHATSAPP -> "WhatsApp"
                                MessageChannel.EMAIL -> "Email"
                                MessageChannel.WEB_FORM -> "Web"
                            })
                            DetailRow("Statut", when (quote.status) {
                                QuoteStatus.PENDING -> "En attente"
                                QuoteStatus.ACCEPTED -> "Accepté"
                                QuoteStatus.REJECTED -> "Refusé"
                            })

                            if (quote.sentAt > 0) {
                                DetailRow("Envoyé le", dateFormat.format(Date(quote.sentAt)))
                            }
                            if (quote.respondedAt > 0) {
                                DetailRow("Répondu le", dateFormat.format(Date(quote.respondedAt)))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Aperçu du message:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = settings.quoteTemplate
                                .replace("[DEPART]", quote.departure)
                                .replace("[ARRIVEE]", quote.arrival)
                                .replace("[DISTANCE]", String.format("%.1f", quote.distanceKm))
                                .replace("[PRIX]", String.format("%.2f", quote.price)),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (quote.status == QuoteStatus.PENDING) {
                            Button(
                                onClick = { onSendQuote(quote) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = settings.brandColor)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Envoyer")
                            }
                        }

                        if (quote.sentAt > 0 && quote.status == QuoteStatus.PENDING) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refusé")
                            }

                            Button(
                                onClick = { onAcceptQuote(quote) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Green500)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accepté")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Refuser le devis ?") },
            text = {
                Text("La course sera supprimée et un message de refus sera envoyé au client.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        quote?.let { onRejectQuote(it) }
                        showRejectDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Confirmer le refus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Slate800
        )
    }
}
