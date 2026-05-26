package com.drawtaxi.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.Client
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClientDirectoryScreen(
    rides: List<RideRequest>,
    brandColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val clients = remember(rides) {
        rides
            .filter { it.sender.isNotBlank() }
            .groupBy { it.sender }
            .map { (phone, phoneRides) ->
                Client(
                    id = Client.createStableId(phone),
                    name = phoneRides.firstOrNull()?.clientName?.takeIf { it.isNotBlank() } ?: phone,
                    phone = phone,
                    rideCount = phoneRides.size,
                    totalAmount = phoneRides.sumOf { it.price },
                    lastRideDate = phoneRides.maxOfOrNull { it.timestamp } ?: 0L,
                    notes = ""
                )
            }
            .sortedByDescending { it.lastRideDate }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Client?>(null) }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DrawTaxiTopBar(
            title = { Text("Carnet Client") },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                Text(
                    text = "${clients.size} client${if (clients.size > 1) "s" else ""}",
                    style = drawTaxiType().labelMedium,
                    color = Slate500
                )
                Spacer(modifier = Modifier.width(16.dp))
            },
            backgroundColor = Color.Transparent
        )

        if (clients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Slate300
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun client",
                        style = drawTaxiType().titleMedium,
                        color = Slate500
                    )
                    Text(
                        text = "Vos clients apparaîtront ici",
                        style = drawTaxiType().bodySmall,
                        color = Slate400
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Rechercher un client...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                DrawTaxiIconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                items(filteredClients) { client ->
                    ClientCard(
                        client = client,
                        brandColor = brandColor,
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${client.phone}")
                            }
                            context.startActivity(intent)
                        },
                        onSms = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("sms:${client.phone}")
                            }
                            context.startActivity(intent)
                        },
                        onClick = { selectedClient = client }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    selectedClient?.let { client ->
        ClientDetailBottomSheet(
            client = client,
            rides = rides.filter { it.sender == client.phone },
            brandColor = brandColor,
            onDismiss = { selectedClient = null }
        )
    }
}

@Composable
private fun ClientCard(
    client: Client,
    brandColor: Color,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = client.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = client.phone,
                    style = drawTaxiType().bodySmall,
                    color = Slate500
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Slate400
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${client.rideCount} course${if (client.rideCount > 1) "s" else ""}",
                        style = drawTaxiType().labelSmall,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = String.format("%.2f €", client.totalAmount),
                        style = drawTaxiType().labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Green600
                    )
                }
            }
            
            Row {
                DrawTaxiIconButton(onClick = onSms) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = "SMS",
                        tint = Slate500
                    )
                }
                DrawTaxiIconButton(onClick = onCall) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Appeler",
                        tint = brandColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailBottomSheet(
    client: Client,
    rides: List<RideRequest>,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = client.name,
                        style = drawTaxiType().headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = client.phone,
                        style = drawTaxiType().bodyMedium,
                        color = Slate500
                    )
                }
                DrawTaxiIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DrawTaxiOutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${client.phone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appeler")
                }
                DrawTaxiOutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("sms:${client.phone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMS")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate50)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${client.rideCount}",
                            style = drawTaxiType().headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = brandColor
                        )
                        Text("Courses", style = drawTaxiType().labelSmall, color = Slate500)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.2f €", client.totalAmount),
                            style = drawTaxiType().headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Green600
                        )
                        Text("Total dépensé", style = drawTaxiType().labelSmall, color = Slate500)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (client.lastRideDate > 0) dateFormat.format(Date(client.lastRideDate)) else "—",
                            style = drawTaxiType().bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Text("Dernière course", style = drawTaxiType().labelSmall, color = Slate500)
                    }
                }
            }
            
            if (rides.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Historique des courses",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                rides.take(5).forEach { ride ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${ride.departure} → ${ride.arrival}",
                                    style = drawTaxiType().bodyMedium
                                )
                                Text(
                                    text = ride.date.ifBlank { dateFormat.format(Date(ride.timestamp)) },
                                    style = drawTaxiType().labelSmall,
                                    color = Slate500
                                )
                            }
                            Text(
                                text = if (ride.price > 0) String.format("%.2f €", ride.price) else "—",
                                style = drawTaxiType().titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (ride.price > 0) brandColor else Slate400
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClientDirectoryScreenPreview() {
    DrawTaxiTheme {
        ClientDirectoryScreen(
            rides = emptyList(),
            brandColor = Color(0xFF6366F1),
            onBack = {}
        )
    }
}


