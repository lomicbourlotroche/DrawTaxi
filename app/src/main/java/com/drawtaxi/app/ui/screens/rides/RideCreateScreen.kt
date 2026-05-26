package com.drawtaxi.app.ui.screens.rides

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
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.sms.parseSms
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import android.location.Location
import android.util.Log
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

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
    var distanceInput by remember { mutableStateOf(if (initialRide?.distanceKm ?: 0.0 > 0) String.format("%.1f", initialRide?.distanceKm ?: 0.0) else "") }
    var hasCalculatedRoute by remember { mutableStateOf(initialRide?.distanceKm ?: 0.0 > 0) }
    var departureLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var arrivalLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var routePositions by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun recalculatePriceFromDistance(dist: Double) {
        val now = java.util.Calendar.getInstance()
        val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
            distanceKm = dist,
            dateTime = now,
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
        price = String.format("%.2f", priceBreakdown.totalTTC).replace(",", ".")
    }

    fun calculateRoute() {
        if (departure.length < 3 || arrival.length < 3) {
            routeError = "Adresses trop courtes"
            return
        }

        isCalculatingRoute = true
        routeError = null

        scope.launch(Dispatchers.IO) {
            try {
                Log.d("DrawTaxi", "Calculating route: $departure -> $arrival")
                val depLocation = com.drawtaxi.app.logic.geocoding.GeocodingService.geocode(departure, context)
                if (depLocation == null) {
                    Log.w("DrawTaxi", "Geocoding failed for departure: $departure")
                    withContext(Dispatchers.Main) {
                        routeError = "Départ introuvable"
                        isCalculatingRoute = false
                    }
                    return@launch
                }

                val arrLocation = com.drawtaxi.app.logic.geocoding.GeocodingService.geocode(arrival, context)
                if (arrLocation == null) {
                    Log.w("DrawTaxi", "Geocoding failed for arrival: $arrival")
                    withContext(Dispatchers.Main) {
                        routeError = "Destination introuvable"
                        isCalculatingRoute = false
                    }
                    return@launch
                }

                Log.d("DrawTaxi", "Geocoded: dep=(${depLocation.latitude},${depLocation.longitude}) arr=(${arrLocation.latitude},${arrLocation.longitude})")

                val route = NavigationEngine.fetchRoute(
                    fromLat = depLocation.latitude, fromLng = depLocation.longitude,
                    toLat = arrLocation.latitude, toLng = arrLocation.longitude
                )
                Log.d("DrawTaxi", "Route result: route=${route != null}, distance=${route?.distance ?: 0}m")

                var dist: Double
                if (route != null && route.distance > 0) {
                    dist = route.distance / 1000.0
                    Log.d("DrawTaxi", "Using Directions API distance: $dist km")
                } else {
                    val lat1 = Math.toRadians(depLocation.latitude)
                    val lat2 = Math.toRadians(arrLocation.latitude)
                    val dLat = Math.toRadians(arrLocation.latitude - depLocation.latitude)
                    val dLon = Math.toRadians(arrLocation.longitude - depLocation.longitude)
                    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(lat1) * Math.cos(lat2) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2)
                    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                    dist = 6371.0 * c * 1.3
                    Log.d("DrawTaxi", "Using haversine fallback: $dist km (x1.3 for road)")
                }

                val geometry = NavigationEngine.fetchRouteGeometry(
                    fromLat = depLocation.latitude, fromLng = depLocation.longitude,
                    toLat = arrLocation.latitude, toLng = arrLocation.longitude
                )
                val rPoints = if (geometry.isNotEmpty()) {
                    geometry
                } else {
                    val d = Location("").apply { latitude = depLocation.latitude; longitude = depLocation.longitude }
                    val a = Location("").apply { latitude = arrLocation.latitude; longitude = arrLocation.longitude }
                    listOf(d.latitude to d.longitude, a.latitude to a.longitude)
                }

                withContext(Dispatchers.Main) {
                    departureLocation = depLocation
                    arrivalLocation = arrLocation
                    routePositions = rPoints
                    distanceKm = dist
                    distanceInput = String.format("%.1f", dist)
                    recalculatePriceFromDistance(dist)
                    isCalculatingRoute = false
                    hasCalculatedRoute = true
                    Log.d("DrawTaxi", "Route calculated: ${dist}km, ${price}€")
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

    LaunchedEffect(autoFilled) {
        autoFilled?.let {
            if (clientPhone.isBlank()) clientPhone = it.sender
            if (departure.isBlank()) departure = it.departure
            if (arrival.isBlank()) arrival = it.arrival
            if (time.isBlank()) time = it.time
        }
    }

    LaunchedEffect(initialRide?.id) {
        if (initialRide != null && initialRide.distanceKm > 0) {
            distanceKm = initialRide.distanceKm
            distanceInput = String.format("%.1f", initialRide.distanceKm)
            recalculatePriceFromDistance(initialRide.distanceKm)
            hasCalculatedRoute = true
        } else if (departure.length >= 3 && arrival.length >= 3) {
            kotlinx.coroutines.delay(1000L)
            calculateRoute()
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

    DrawTaxiScaffold(
        topBar = {
            DrawTaxiTopBar(
                title = { Text("Nouvelle Course", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    DrawTaxiIconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler")
                    }
                },
                backgroundColor = Color.White
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
                        Text("Message original", style = drawTaxiType().labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(sharedText, style = drawTaxiType().bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
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

            Text("Détails de la course", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = brandColor)

            TaxiCard(title = "Trajet") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = departure,
                        onValueChange = { departure = it; stopTimer(); hasCalculatedRoute = false },
                        label = { Text("Adresse de départ") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.TripOrigin, contentDescription = null, tint = brandColor) }
                    )
                    OutlinedTextField(
                        value = arrival,
                        onValueChange = { arrival = it; stopTimer(); hasCalculatedRoute = false },
                        label = { Text("Adresse de destination") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFF43F5E)) }
                    )
                    // Date and Time pickers
                    val calendar = remember { java.util.Calendar.getInstance() }
                    
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCalendar = java.util.Calendar.getInstance().apply {
                                set(year, month, dayOfMonth)
                            }
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            date = dateFormat.format(selectedCalendar.time)
                            stopTimer()
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    
                    val timePickerDialog = android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            time = String.format("%02dh%02d", hourOfDay, minute)
                            stopTimer()
                        },
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        true
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrawTaxiOutlinedButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (date.isNotBlank()) date else "Date",
                                color = if (date.isNotBlank()) androidx.compose.ui.graphics.Color.Unspecified else androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                        
                        DrawTaxiOutlinedButton(
                            onClick = { timePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (time.isNotBlank()) time else "Heure",
                                color = if (time.isNotBlank()) androidx.compose.ui.graphics.Color.Unspecified else androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = {
                                distanceInput = it
                                stopTimer()
                                val d = it.toDoubleOrNull()
                                if (d != null && d > 0) {
                                    distanceKm = d
                                    recalculatePriceFromDistance(d)
                                }
                            },
                            label = { Text("Distance (km)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = brandColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                if (distanceKm > 0) {
                                    Text(
                                        text = "✓",
                                        style = drawTaxiType().bodyMedium,
                                        color = Emerald500,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        )
                        DrawTaxiSolidButton(
                            onClick = { calculateRoute() },
                            enabled = !isCalculatingRoute && departure.length >= 3 && arrival.length >= 3,
                            shape = RoundedCornerShape(12.dp),
                            containerColor = brandColor,
                            modifier = Modifier.align(Alignment.Bottom)
                        ) {
                            if (isCalculatingRoute) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        Text("Calcul de l'itinéraire...", style = drawTaxiType().bodyMedium, color = Amber700)
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
                        Text(error, style = drawTaxiType().bodyMedium, color = Rose700)
                    }
                }
            }

            if (distanceKm > 0 && !isCalculatingRoute) {
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
                            Text("Distance", style = drawTaxiType().bodySmall, color = Slate500)
                            Text(String.format("%.1f km", distanceKm), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (price.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Prix estimé", style = drawTaxiType().bodySmall, color = Slate500)
                                Text("$price €", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                            }
                        }
                    }
                }

                if (departureLocation != null && arrivalLocation != null) {
                    val cameraState = rememberCameraState()

                    LaunchedEffect(routePositions, departureLocation, arrivalLocation) {
                        val allPositions = mutableListOf<Pair<Double, Double>>()
                        if (routePositions.isNotEmpty()) {
                            allPositions.addAll(routePositions)
                        }
                        departureLocation?.let { allPositions.add(it.latitude to it.longitude) }
                        arrivalLocation?.let { allPositions.add(it.latitude to it.longitude) }
                        if (allPositions.isNotEmpty()) {
                            val lats = allPositions.map { it.first }
                            val lngs = allPositions.map { it.second }
                            cameraState.animateTo(
                                CameraPosition(
                                    target = Position(
                                        longitude = (lngs.min() + lngs.max()) / 2.0,
                                        latitude = (lats.min() + lats.max()) / 2.0
                                    ),
                                    zoom = if (routePositions.isNotEmpty()) 12.0 else 11.0
                                )
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        MaplibreMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraState = cameraState,
                            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                            options = MapOptions(
                                ornamentOptions = OrnamentOptions(
                                    isLogoEnabled = false,
                                    isAttributionEnabled = false,
                                    isCompassEnabled = false
                                ),
                                gestureOptions = org.maplibre.compose.map.GestureOptions.AllDisabled
                            )
                        ) {
                            val routeSource = rememberGeoJsonSource(
                                data = GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}"""),
                                options = GeoJsonOptions(synchronousUpdate = true)
                            )
                            val depSource = rememberGeoJsonSource(
                                data = GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}"""),
                                options = GeoJsonOptions(synchronousUpdate = true)
                            )
                            val arrSource = rememberGeoJsonSource(
                                data = GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}"""),
                                options = GeoJsonOptions(synchronousUpdate = true)
                            )

                            LaunchedEffect(routePositions) {
                                val coords = routePositions.joinToString(",") { "[${it.second},${it.first}]" }
                                val json = if (coords.isNotEmpty()) {
                                    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
                                } else """{"type":"FeatureCollection","features":[]}"""
                                routeSource.setData(GeoJsonData.JsonString(json))
                            }

                            LaunchedEffect(departureLocation) {
                                val loc = departureLocation
                                if (loc != null) {
                                    depSource.setData(GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""))
                                }
                            }

                            LaunchedEffect(arrivalLocation) {
                                val loc = arrivalLocation
                                if (loc != null) {
                                    arrSource.setData(GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""))
                                }
                            }

                            LineLayer(
                                id = "route",
                                source = routeSource,
                                color = const(Color(0xFF2196F3)),
                                width = const(6.dp),
                                cap = const(LineCap.Round),
                                join = const(LineJoin.Round)
                            )

                            CircleLayer(
                                id = "departure",
                                source = depSource,
                                color = const(Color(0xFF10B981)),
                                radius = const(8.dp),
                                strokeColor = const(Color.White),
                                strokeWidth = const(2.dp)
                            )

                            CircleLayer(
                                id = "arrival",
                                source = arrSource,
                                color = const(Color(0xFFF43F5E)),
                                radius = const(8.dp),
                                strokeColor = const(Color.White),
                                strokeWidth = const(2.dp)
                            )
                        }
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
                DrawTaxiSolidButton(
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
                DrawTaxiOutlinedButton(
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
                            com.drawtaxi.app.logic.sms.SmsUtils.sendSms(context, phone, message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = brandColor.copy(0.2f)
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
            Text(title, style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RideCreateScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        RideCreateScreen(
            initialRide = null,
            settings = sampleSettings,
            onConfirm = {},
            onCancel = {}
        )
    }
}


