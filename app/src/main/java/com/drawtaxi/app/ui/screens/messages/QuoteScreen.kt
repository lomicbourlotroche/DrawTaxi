package com.drawtaxi.app.ui.screens.messages



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
import com.drawtaxi.app.ui.components.core.*

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

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
import com.drawtaxi.app.ui.theme.*

import com.drawtaxi.app.ui.components.TaxiCard

import com.drawtaxi.app.ui.theme.*

import java.text.SimpleDateFormat

import java.util.*



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

        DrawTaxiTopBar(

            title = {

                Column {

                    Text("Devis - Course")

                    Text(

                        text = ride.departure.ifBlank { "—" } + " → " + ride.arrival.ifBlank { "—" },

                        style = drawTaxiType().bodySmall,

                        color = Slate500

                    )

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

                    val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
                        distanceKm = ride.distanceKm,
                        dateTime = java.util.Calendar.getInstance(),
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

                        val calculatedPrice = priceBreakdown.totalTTC

                        

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {

                            Text(

                                text = "Prix calculé (TTC):",

                                style = drawTaxiType().bodySmall,

                                color = Slate500

                            )

                            Text(

                                text = String.format("%.2f €", calculatedPrice),

                                style = drawTaxiType().bodyMedium,

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

                                        style = drawTaxiType().labelSmall,

                                        color = Amber500

                                    )

                                }

                                if (priceBreakdown.isSunday) {

                                    Text(

                                        text = "📅 Dimanche",

                                        style = drawTaxiType().labelSmall,

                                        color = Amber500

                                    )

                                }

                                if (priceBreakdown.isHoliday) {

                                    Text(

                                        text = "🎉 Férié",

                                        style = drawTaxiType().labelSmall,

                                        color = Amber500

                                    )

                                }

                            }

                        }



                        Spacer(modifier = Modifier.height(8.dp))

                        val distDomicile = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 } ?: 5.0
                        val coutDeplacement = distDomicile * settings.coutParKmDeplacement
                        val enteredPrice = price.toDoubleOrNull() ?: calculatedPrice
                        val profit = if (enteredPrice > 0) ((enteredPrice - coutDeplacement) / enteredPrice) * 100 else 0.0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (profit >= 70) Emerald500.copy(alpha = 0.1f) else if (profit >= 50) Amber500.copy(alpha = 0.1f) else Red500.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rentabilité estimée", style = drawTaxiType().labelMedium, color = Slate600)
                                    Text(
                                        text = String.format("%.0f%%", profit),
                                        style = drawTaxiType().titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profit >= 70) Emerald600 else if (profit >= 50) Amber600 else Red600
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Coût déplacement", style = drawTaxiType().labelSmall, color = Slate500)
                                    Text(String.format("%.2f €", coutDeplacement), style = drawTaxiType().bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(

                            text = "Canal d'envoi:",

                            style = drawTaxiType().bodyMedium,

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



                        DrawTaxiSolidButton(

                            onClick = {

                                val dist = distanceKm.toDoubleOrNull() ?: ride.distanceKm

                                val p = price.toDoubleOrNull() ?: calculatedPrice

                                onCreateQuote(dist, p, selectedChannel)

                            },

                            modifier = Modifier.fillMaxWidth(),

                            containerColor = settings.brandColor

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

                        style = drawTaxiType().bodyMedium,

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

                            style = drawTaxiType().bodyMedium,

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

                            DrawTaxiSolidButton(

                                onClick = { onSendQuote(quote) },

                                modifier = Modifier.weight(1f),

                                containerColor = settings.brandColor

                            ) {

                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)

                                Spacer(modifier = Modifier.width(4.dp))

                                Text("Envoyer")

                            }

                        }



                        if (quote.sentAt > 0 && quote.status == QuoteStatus.PENDING) {

                            DrawTaxiOutlinedButton(

                                onClick = { showRejectDialog = true },

                                modifier = Modifier.weight(1f),

                                contentColor = Red500

                            ) {

                                Icon(Icons.Default.Close, contentDescription = null)

                                Spacer(modifier = Modifier.width(4.dp))

                                Text("Refusé")

                            }



                            DrawTaxiSolidButton(

                                onClick = { onAcceptQuote(quote) },

                                modifier = Modifier.weight(1f),

                                containerColor = Green500

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

                DrawTaxiSolidButton(

                    onClick = {

                        quote?.let { onRejectQuote(it) }

                        showRejectDialog = false

                        onBack()

                    },

                    containerColor = Red500

                ) {

                    Text("Confirmer le refus")

                }

            },

            dismissButton = {

                DrawTaxiSolidButton(onClick = { showRejectDialog = false }) {

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

            style = drawTaxiType().bodyMedium,

            color = Slate500

        )

        Text(

            text = value,

            style = drawTaxiType().bodyMedium,

            fontWeight = FontWeight.Medium,

            color = Slate800

        )

    }

}

@Preview(showBackground = true)
@Composable
fun QuoteScreenPreview() {
    val sampleSettings = AppSettings()
    val sampleRide = RideRequest(
        id = "1",
        sender = "0612345678",
        body = "Taxi depuis Gare de Lyon vers Orly",
        departure = "Gare de Lyon",
        arrival = "Orly",
        time = "10:00",
        price = 45.0,
        distanceKm = 18.5
    )
    DrawTaxiTheme {
        QuoteScreen(
            ride = sampleRide,
            quote = null,
            settings = sampleSettings,
            onCreateQuote = { _, _, _ -> },
            onSendQuote = {},
            onAcceptQuote = {},
            onRejectQuote = {},
            onBack = {}
        )
    }
}



