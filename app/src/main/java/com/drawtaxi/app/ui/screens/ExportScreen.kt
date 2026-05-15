package com.drawtaxi.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    rides: List<RideRequest>,
    brandColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedPeriod by remember { mutableStateOf("all") }
    var isExporting by remember { mutableStateOf(false) }
    
    val filteredRides = remember(rides, selectedPeriod) {
        when (selectedPeriod) {
            "month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                rides.filter { it.timestamp >= calendar.timeInMillis && !it.isPending }
            }
            "year" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, -1)
                rides.filter { it.timestamp >= calendar.timeInMillis && !it.isPending }
            }
            else -> rides.filter { !it.isPending }
        }
    }
    
    val totalAmount = filteredRides.sumOf { it.price }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val csv = generateCsv(filteredRides)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(csv)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isExporting = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Export & Rapports") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TaxiCard(title = "Période") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to "Tout",
                            "month" to "30 jours",
                            "year" to "12 mois"
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = selectedPeriod == value,
                                onClick = { selectedPeriod = value },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${filteredRides.size} courses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total: ${String.format("%.2f €", totalAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = brandColor
                            )
                        }
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = brandColor
                        )
                    }
                }
            }

            item {
                TaxiCard(title = "Export CSV") {
                    Text(
                        text = "Exportez vos données au format CSV pour une utilisation dans Excel ou Google Sheets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            val filename = "drawtaxi_export_${dateFormat.format(Date())}.csv"
                            exportLauncher.launch(filename)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && filteredRides.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Télécharger CSV")
                    }
                }
            }

            item {
                TaxiCard(title = "Aperçu des données") {
                    if (filteredRides.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune course validée sur cette période",
                                color = Slate500
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredRides.take(5).forEach { ride ->
                                ExportPreviewRow(ride = ride)
                            }
                            if (filteredRides.size > 5) {
                                Text(
                                    text = "... et ${filteredRides.size - 5} autres courses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExportPreviewRow(ride: RideRequest) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${ride.departure} → ${ride.arrival}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = ride.date.ifBlank { dateFormat.format(Date(ride.timestamp)) },
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
        Text(
            text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (ride.price > 0) Green600 else Slate400
        )
    }
}

private fun generateCsv(rides: List<RideRequest>): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val sb = StringBuilder()
    sb.appendLine("Date,Heure,Client,Dénpart,Arrivée,Distance (km),Prix (€),Numéro facture,Notes")
    
    rides.forEach { ride ->
        val date = ride.date.ifBlank { dateFormat.format(Date(ride.timestamp)) }
        val time = timeFormat.format(Date(ride.timestamp))
        val client = ride.clientName.ifBlank { ride.sender }
        val invoiceNumber = ride.invoiceNumber.ifBlank { "" }
        val notes = ride.notes.replace(",", ";").replace("\n", " ")
        
        sb.appendLine("\"$date\",\"$time\",\"$client\",\"${ride.departure}\",\"${ride.arrival}\",${ride.distanceKm},${ride.price},\"$invoiceNumber\",\"$notes\"")
    }
    
    return sb.toString()
}
