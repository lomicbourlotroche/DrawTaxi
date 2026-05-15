package com.drawtaxi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.parseSms
import com.drawtaxi.app.logic.fetchRoute
import com.drawtaxi.app.ui.theme.Slate400
import com.drawtaxi.app.ui.theme.Slate500
import com.drawtaxi.app.ui.theme.Slate700
import com.drawtaxi.app.ui.theme.Amber50
import com.drawtaxi.app.ui.theme.Amber500
import com.drawtaxi.app.ui.theme.Amber700
import com.drawtaxi.app.ui.theme.Rose50
import com.drawtaxi.app.ui.theme.Rose500
import com.drawtaxi.app.ui.theme.Rose700
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideCreateScreen(
    initialRide: RideRequest? = null,
    sharedText: String = initialRide?.body ?: "",
    onConfirm: (RideRequest) -> Unit,
    onCancel: () -> Unit,
    settings: AppSettings
) {
    val context = LocalContext.current
    val brandColor = settings.brandColor
    
    val autoFilled = remember(sharedText) {
        if (sharedText.isNotBlank()) parseSms("Client", sharedText) else null
    }

    var clientFirstName by remember { mutableStateOf(initialRide?.clientFirstName ?: "") }
    var clientLastName by remember { mutableStateOf(initialRide?.clientName ?: "") }
    var clientPhone by remember { mutableStateOf(initialRide?.clientPhone ?: initialRide?.sender ?: autoFilled?.sender ?: "") }
    var clientEmail by remember { mutableStateOf(initialRide?.clientEmail ?: "") }
    var departure by remember { mutableStateOf(initialRide?.departure ?: autoFilled?.departure ?: "") }
    var arrival by remember { mutableStateOf(initialRide?.arrival ?: autoFilled?.arrival ?: "") }
    var date by remember { mutableStateOf(initialRide?.date ?: autoFilled?.date ?: "") }
    var time by remember { mutableStateOf(initialRide?.time ?: autoFilled?.time ?: "") }
    var price by remember { mutableStateOf(if (initialRide != null && initialRide.price > 0) initialRide.price.toString() else "") }
    var distanceKm by remember { mutableStateOf(initialRide?.distanceKm ?: 0.0) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(autoFilled) {
        autoFilled?.let {
            if (clientPhone.isBlank()) clientPhone = it.sender
            if (departure.isBlank()) departure = it.departure
            if (arrival.isBlank()) arrival = it.arrival
            if (time.isBlank()) time = it.time
        }
    }

    LaunchedEffect(departure, arrival) {
        if (departure.length < 3 || arrival.length < 3) {
            isCalculatingRoute = false
            routeError = null
            return@LaunchedEffect
        }

        kotlinx.coroutines.delay(1500L)
        isCalculatingRoute = true
        routeError = null

        withContext(Dispatchers.IO) {
            try {
                Log.d("DrawTaxi", "Calculating route: $departure -> $arrival")
                val depLocation = com.drawtaxi.app.logic.GeocodingService.geocode(departure)
                if (depLocation == null) {
                    Log.w("DrawTaxi", "Geocoding failed for departure: $departure")
                    withContext(Dispatchers.Main) {
                        routeError = "Adresse de départ introuvable"
                        isCalculatingRoute = false
                    }
                    return@withContext
                }

                val arrLocation = com.drawtaxi.app.logic.GeocodingService.geocode(arrival)
                if (arrLocation == null) {
                    Log.w("DrawTaxi", "Geocoding failed for arrival: $arrival")
                    withContext(Dispatchers.Main) {
                        routeError = "Adresse de destination introuvable"
                        isCalculatingRoute = false
                    }
                    return@withContext
                }

                Log.d("DrawTaxi", "Geocoded: dep=(${depLocation.latitude},${depLocation.longitude}) arr=(${arrLocation.latitude},${arrLocation.longitude})")

                val depPoint = org.osmdroid.util.GeoPoint(depLocation.latitude, depLocation.longitude)
                val arrPoint = org.osmdroid.util.GeoPoint(arrLocation.latitude, arrLocation.longitude)

                val routeInfo = fetchRoute(depPoint, arrPoint)
                Log.d("DrawTaxi", "Route result: distance=${routeInfo.distanceMeters}m, points=${routeInfo.points.size}")

                if (routeInfo.distanceMeters > 0) {
                    val dist = routeInfo.distanceMeters / 1000.0
                    val now = java.util.Calendar.getInstance()
                    val priceBreakdown = com.drawtaxi.app.logic.PriceEngine.calculate(
                        distanceKm = dist,
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

                    withContext(Dispatchers.Main) {
                        distanceKm = dist
                        price = String.format("%.2f", priceBreakdown.totalTTC).replace(",", ".")
                        isCalculatingRoute = false
                        Log.d("DrawTaxi", "Route calculated: ${dist}km, ${price}€")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        routeError = "Impossible de calculer l'itinéraire"
                        isCalculatingRoute = false
                    }
                    Log.w("DrawTaxi", "OSRM returned 0 distance")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    routeError = "Erreur: ${e.message}"
                    isCalculatingRoute = false
                }
                Log.e("DrawTaxi", "Routing error: ${e.message}", e)
            }
        }
    }
    
    var timeLeft by remember { mutableStateOf(30) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var hasConfirmed by remember { mutableStateOf(false) }
    
    fun stopTimer() {
        isTimerRunning = false
    }

    LaunchedEffect(isTimerRunning, timeLeft, hasConfirmed) {
        if (hasConfirmed) return@LaunchedEffect
        if (isTimerRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        } else if (isTimerRunning && timeLeft == 0) {
            val timestamp = if (initialRide != null) initialRide.timestamp else System.currentTimeMillis()
            val id = if (initialRide != null) initialRide.id else RideRequest.createStableId(clientPhone.ifBlank { "Client" }, sharedText, timestamp)
            val newRide = RideRequest(
                id = id,
                sender = clientPhone.ifBlank { "Client" },
                body = sharedText,
                clientFirstName = clientFirstName,
                clientName = clientLastName,
                clientPhone = clientPhone,
                clientEmail = clientEmail,
                date = date,
                time = time,
                departure = departure,
                arrival = arrival,
                isPending = true,
                status = com.drawtaxi.app.data.RideStatus.DRAFT,
                price = price.toDoubleOrNull() ?: 0.0,
                distanceKm = distanceKm,
                timestamp = timestamp
            )
            hasConfirmed = true
            onConfirm(newRide)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle Course", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (sharedText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Message original", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(sharedText, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }

            TaxiCard(title = "Client") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = clientFirstName,
                            onValueChange = { clientFirstName = it; stopTimer() },
                            label = { Text("Prénom") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = brandColor) }
                        )
                        OutlinedTextField(
                            value = clientLastName,
                            onValueChange = { clientLastName = it; stopTimer() },
                            label = { Text("Nom") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = brandColor) }
                        )
                    }
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it; stopTimer() },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = brandColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it; stopTimer() },
                        label = { Text("Email (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = brandColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            }

            Text("Détails de la course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brandColor)

            TaxiCard(title = "Trajet") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = departure,
                        onValueChange = { departure = it; stopTimer() },
                        label = { Text("Adresse de départ") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.TripOrigin, contentDescription = null, tint = brandColor) }
                    )
                    OutlinedTextField(
                        value = arrival,
                        onValueChange = { arrival = it; stopTimer() },
                        label = { Text("Adresse de destination") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFF43F5E)) }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it; stopTimer() },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = brandColor) },
                            placeholder = { Text("dd/MM/yyyy") }
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it; stopTimer() },
                            label = { Text("Heure") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = brandColor) },
                            placeholder = { Text("HHhMM") }
                        )
                    }
                }
            }

            if (distanceKm > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = brandColor.copy(0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Distance estimée", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            Text(String.format("%.1f km", distanceKm), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (price.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Prix estimé", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                Text("$price €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                            }
                        }
                    }
                }
            }

            if (isCalculatingRoute) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Amber50),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Amber500
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Calcul de l'itinéraire...", style = MaterialTheme.typography.bodyMedium, color = Amber700)
                    }
                }
            }

            routeError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Rose50),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = Rose700)
                    }
                }
            }

            com.drawtaxi.app.ui.components.RideCreateTimerButton(
                timeLeft = timeLeft,
                isTimerRunning = isTimerRunning,
                onConfirm = {
                    hasConfirmed = true
                    val timestamp = if (initialRide != null) initialRide.timestamp else System.currentTimeMillis()
                    val id = if (initialRide != null) initialRide.id else RideRequest.createStableId(clientPhone.ifBlank { "Client" }, sharedText, timestamp)
                    
                    val newRide = RideRequest(
                        id = id,
                        sender = clientPhone.ifBlank { "Client" },
                        body = sharedText,
                        clientFirstName = clientFirstName,
                        clientName = clientLastName,
                        clientPhone = clientPhone,
                        clientEmail = clientEmail,
                        date = date,
                        time = time,
                        departure = departure,
                        arrival = arrival,
                        isPending = true,
                        status = com.drawtaxi.app.data.RideStatus.DRAFT,
                        price = price.toDoubleOrNull() ?: 0.0,
                        distanceKm = distanceKm,
                        timestamp = timestamp
                    )
                    onConfirm(newRide)
                },
                brandColor = brandColor
            )

            if (isTimerRunning && timeLeft > 0) {
                TextButton(
                    onClick = { stopTimer() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer sans minuterie", color = Slate400)
                }
            }

            val missingFields = mutableListOf<String>()
            if (clientPhone.isBlank()) missingFields.add("Téléphone")
            if (departure.isBlank()) missingFields.add("Départ")
            if (arrival.isBlank()) missingFields.add("Arrivée")
            if (time.isBlank()) missingFields.add("Heure")

            if (missingFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val fieldsText = missingFields.joinToString(", ")
                        val template = settings.missingInfoTemplate
                        val message = if (template.contains("[FIELDS]")) {
                            template.replace("[FIELDS]", fieldsText)
                        } else {
                            "$template $fieldsText"
                        }
                        val phone = clientPhone.ifBlank { initialRide?.sender ?: "" }
                        if (phone.isNotBlank()) {
                            com.drawtaxi.app.logic.SmsUtils.sendSms(context, phone, message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(0.2f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = brandColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Demander les infos manquantes", color = brandColor, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TaxiCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
