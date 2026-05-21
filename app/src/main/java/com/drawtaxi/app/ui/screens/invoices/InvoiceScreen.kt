package com.drawtaxi.app.ui.screens.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class InvoiceFilter(val label: String) {
    ALL("Toutes"),
    WEEK("Semaine"),
    MONTH("Mois"),
    UNINVOICED("Non facturées")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    validatedRides: List<RideRequest>,
    brandColor: Color,
    onRideSelected: (RideRequest) -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRide by remember { mutableStateOf<RideRequest?>(null) }

    val completedRides = remember(validatedRides) {
        validatedRides.filter { !it.isPending && it.status == com.drawtaxi.app.data.RideStatus.COMPLETED }
            .sortedByDescending { it.timestamp }
    }

    val filteredRides = remember(completedRides, selectedFilter, searchQuery) {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        
        var rides = when (selectedFilter) {
            InvoiceFilter.ALL -> completedRides
            InvoiceFilter.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                completedRides.filter { it.timestamp >= calendar.timeInMillis }
            }
            InvoiceFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                completedRides.filter { it.timestamp >= calendar.timeInMillis }
            }
            InvoiceFilter.UNINVOICED -> completedRides.filter { it.invoiceNumber.isBlank() }
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            rides = rides.filter {
                it.departure.lowercase().contains(query) ||
                it.arrival.lowercase().contains(query) ||
                it.sender.lowercase().contains(query) ||
                it.clientName.lowercase().contains(query)
            }
        }

        rides
    }

    val totalRevenue = filteredRides.sumOf { it.price }
    val totalRides = filteredRides.size

    if (selectedRide != null) {
        InvoiceDetailScreen(
            ride = selectedRide!!,
            onBack = { selectedRide = null },
            brandColor = brandColor
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
            TopAppBar(
                title = {
                    Column {
                        Text("Factures", fontWeight = FontWeight.Bold)
                        Text("$totalRides courses • ${String.format("%.2f €", totalRevenue)}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher par client, lieu...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InvoiceFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brandColor,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(24.dp), color = Slate100, modifier = Modifier.size(80.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Aucune course", style = MaterialTheme.typography.titleMedium, color = Slate500)
                        Text(text = "Terminez des courses pour les voir ici", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    items(filteredRides) { ride ->
                        InvoiceRideCard(
                            ride = ride,
                            brandColor = brandColor,
                            onClick = { selectedRide = ride }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRideCard(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(10.dp), color = brandColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = ride.sender.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = brandColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = ride.departure.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.padding(horizontal = 4.dp).size(14.dp))
                        Text(text = ride.arrival.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = ride.date.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(text = " • ", color = Slate400)
                        Text(text = ride.sender, style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.2f €", ride.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )
                if (ride.invoiceNumber.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Green100, modifier = Modifier.padding(top = 4.dp)) {
                        Text(text = "Facturé", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Green800, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else {
                    Surface(shape = RoundedCornerShape(6.dp), color = Amber100, modifier = Modifier.padding(top = 4.dp)) {
                        Text(text = "À facturer", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    ride: RideRequest,
    onBack: () -> Unit,
    brandColor: Color
) {
    val context = LocalContext.current
    var invoiceNumber by remember { mutableStateOf(ride.invoiceNumber) }
    var clientName by remember { mutableStateOf(ride.clientName) }
    var clientAddress by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf(ride.clientEmail) }
    var notes by remember { mutableStateOf(ride.notes) }

    val profitability = if (ride.price > 0) {
        RideRequest.calculateProfitability(ride.price, ride.fuelCost.takeIf { it > 0 } ?: ride.distanceKm * 0.3 * 0.15)
    } else 0.0

    Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
        TopAppBar(
            title = {
                Column {
                    Text("Détails Facture", fontWeight = FontWeight.Bold)
                    Text(ride.date.ifBlank { "Sans date" }, style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informations course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        InfoRow(label = "Départ", value = ride.departure)
                        InfoRow(label = "Arrivée", value = ride.arrival)
                        InfoRow(label = "Date", value = ride.date)
                        InfoRow(label = "Heure", value = ride.time)
                        InfoRow(label = "Distance", value = String.format("%.1f km", ride.distanceKm))
                        InfoRow(label = "Durée", value = if (ride.durationMinutes > 0) "${ride.durationMinutes} min" else "—")
                        
                        HorizontalDivider(color = Slate100)
                        
                        InfoRow(label = "Prix", value = String.format("%.2f €", ride.price), bold = true)

                        InfoRow(label = "Déplacement", value = String.format("%.2f €", ride.fuelCost.takeIf { it > 0 } ?: ride.distanceKm * 0.3 * 0.15))

                        InfoRow(label = "Rentabilité", value = String.format("%.0f%%", profitability), bold = true)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informations client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("Nom du client") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = clientEmail,
                            onValueChange = { clientEmail = it },
                            label = { Text("Email client") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = clientAddress,
                            onValueChange = { clientAddress = it },
                            label = { Text("Adresse client (optionnel)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                        )

                        InfoRow(label = "Téléphone", value = ride.sender)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Numéro de facture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("N° facture (ex: F-2024-001)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes pour la facture") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pour Kolecto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Text(
                            "Utilisez ces informations pour créer manuellement la facture sur Kolecto :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )

                        KolectoDataItem(label = "Date", value = ride.date)
                        KolectoDataItem(label = "Client", value = clientName.ifBlank { ride.sender })
                        KolectoDataItem(label = "Trajet", value = "${ride.departure} → ${ride.arrival}")
                        KolectoDataItem(label = "Montant HT", value = String.format("%.2f €", ride.price / 1.10))
                        KolectoDataItem(label = "TVA (10%)", value = String.format("%.2f €", ride.price - (ride.price / 1.10)))
                        KolectoDataItem(label = "Montant TTC", value = String.format("%.2f €", ride.price), bold = true)
                        KolectoDataItem(label = "Distance", value = String.format("%.1f km", ride.distanceKm))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://app.kolecto.fr")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir Kolecto", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = Slate800
        )
    }
}

@Composable
private fun KolectoDataItem(label: String, value: String, bold: Boolean = false, accentColor: Color = Indigo500) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Slate500)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (bold) accentColor else Slate700
        )
    }
}
