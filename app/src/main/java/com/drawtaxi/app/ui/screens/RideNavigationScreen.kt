package com.drawtaxi.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.GeocodingService
import com.drawtaxi.app.logic.OsrmRoutingService
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de navigation complète pour une course taxi
 * Style Google Maps avec 3 phases : Rouge → Bleu → Vert
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideNavigationScreen(
    ride: RideRequest,
    settings: AppSettings,
    brandColor: Color,
    onBack: () -> Unit,
    onComplete: (RideRequest) -> Unit,
    onEditRide: (RideRequest) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // États de navigation
    var currentPhase by remember { mutableStateOf(NavigationPhase.TO_PICKUP) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }

    // Routes pour chaque phase
    var routeToPickup by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }
    var routeToDestination by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }
    var routeToHome by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }

    // UI States
    var showCallDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(true) }
    var etaText by remember { mutableStateOf("-- min") }
    var distanceText by remember { mutableStateOf("-- km") }
    var messageText by remember { mutableStateOf("") }

    // Initialiser Mapbox
    LaunchedEffect(Unit) {
        Mapbox.getInstance(context)
    }

    // Démarrer le suivi GPS
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000L
            ).apply {
                setMinUpdateIntervalMillis(1000L)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        currentLocation = location
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // Calculer les 3 routes
    LaunchedEffect(ride, currentLocation) {
        scope.launch {
            // Route 1: Position actuelle → Point de départ (Rouge)
            if (currentLocation != null && ride.departure.isNotBlank()) {
                val pickupLoc = GeocodingService.geocode(ride.departure)
                pickupLoc?.let {
                    routeToPickup = OsrmRoutingService.calculateRoute(currentLocation!!, it)
                }
            }

            // Route 2: Point de départ → Destination (Bleu)
            if (ride.departure.isNotBlank() && ride.arrival.isNotBlank()) {
                val fromLoc = GeocodingService.geocode(ride.departure)
                val toLoc = GeocodingService.geocode(ride.arrival)
                if (fromLoc != null && toLoc != null) {
                    routeToDestination = OsrmRoutingService.calculateRoute(fromLoc, toLoc)
                }
            }

            // Route 3: Destination → Domicile chauffeur (Vert)
            if (ride.arrival.isNotBlank() && settings.homeAddress.isNotBlank()) {
                val destLoc = GeocodingService.geocode(ride.arrival)
                val homeLoc = GeocodingService.geocode(settings.homeAddress)
                if (destLoc != null && homeLoc != null) {
                    routeToHome = OsrmRoutingService.calculateRoute(destLoc, homeLoc)
                }
            }
        }
    }

    // Mettre à jour les routes sur la carte
    LaunchedEffect(mapboxMap, routeToPickup, routeToDestination, routeToHome) {
        mapboxMap?.let { map ->
            updateMapWithAllRoutes(map, routeToPickup, routeToDestination, routeToHome, currentPhase)
        }
    }

    // Mettre à jour la position et suivre l'étape
    LaunchedEffect(currentLocation, currentPhase) {
        currentLocation?.let { location ->
            // Centrer la carte sur la position
            mapboxMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    17.0
                )
            )

            // Mettre à jour ETA et distance selon la phase
            when (currentPhase) {
                NavigationPhase.TO_PICKUP -> {
                    routeToPickup?.let {
                        if (it.success) {
                            etaText = OsrmRoutingService.formatDuration(it.duration)
                            distanceText = OsrmRoutingService.formatDistance(it.distance)

                            // Auto-switch si proche du point de départ (< 50m)
                            val pickupLoc = GeocodingService.geocode(ride.departure)
                            pickupLoc?.let { pickup ->
                                val distance = location.distanceTo(pickup)
                                if (distance < 50f) {
                                    currentPhase = NavigationPhase.TO_DESTINATION
                                }
                            }
                        }
                    }
                }
                NavigationPhase.TO_DESTINATION -> {
                    routeToDestination?.let {
                        if (it.success) {
                            etaText = OsrmRoutingService.formatDuration(it.duration)
                            distanceText = OsrmRoutingService.formatDistance(it.distance)

                            // Auto-switch si proche de la destination (< 50m)
                            val destLoc = GeocodingService.geocode(ride.arrival)
                            destLoc?.let { dest ->
                                val distance = location.distanceTo(dest)
                                if (distance < 50f) {
                                    currentPhase = NavigationPhase.TO_HOME
                                }
                            }
                        }
                    }
                }
                NavigationPhase.TO_HOME -> {
                    routeToHome?.let {
                        if (it.success) {
                            etaText = OsrmRoutingService.formatDuration(it.duration)
                            distanceText = OsrmRoutingService.formatDistance(it.distance)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (currentPhase) {
                                NavigationPhase.TO_PICKUP -> "Aller chercher le client"
                                NavigationPhase.TO_DESTINATION -> "En course"
                                NavigationPhase.TO_HOME -> "Retour au domicile"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ride.date} à ${ride.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = when (currentPhase) {
                        NavigationPhase.TO_PICKUP -> Rose500
                        NavigationPhase.TO_DESTINATION -> Indigo500
                        NavigationPhase.TO_HOME -> Emerald500
                    }
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton appel
                SmallFloatingActionButton(
                    onClick = { showCallDialog = true },
                    containerColor = Emerald500,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Appeler")
                }

                // Bouton message
                SmallFloatingActionButton(
                    onClick = { showMessageDialog = true },
                    containerColor = Indigo500,
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message")
                }
            }
        },
        bottomBar = {
            NavigationBottomBar(
                currentPhase = currentPhase,
                eta = etaText,
                distance = distanceText,
                ride = ride,
                onPhaseChange = { currentPhase = it },
                onComplete = { onComplete(ride) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Carte MapLibre
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        getMapAsync { map ->
                            mapboxMap = map

                            map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                                // Ajouter les 3 sources pour les routes
                                style.addSource(GeoJsonSource("route-pickup"))
                                style.addSource(GeoJsonSource("route-destination"))
                                style.addSource(GeoJsonSource("route-home"))

                                // Couche Rouge: vers point de départ
                                style.addLayerBelow(
                                    LineLayer("layer-pickup", "route-pickup")
                                        .withProperties(
                                            PropertyFactory.lineColor(Rose500.toArgb()),
                                            PropertyFactory.lineWidth(8f),
                                            PropertyFactory.lineOpacity(0.9f),
                                            PropertyFactory.lineCap(com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND),
                                            PropertyFactory.lineJoin(com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND)
                                        ),
                                    "road-label"
                                )

                                // Couche Bleue: vers destination
                                style.addLayerBelow(
                                    LineLayer("layer-destination", "route-destination")
                                        .withProperties(
                                            PropertyFactory.lineColor(Indigo500.toArgb()),
                                            PropertyFactory.lineWidth(8f),
                                            PropertyFactory.lineOpacity(0.9f),
                                            PropertyFactory.lineCap(com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND),
                                            PropertyFactory.lineJoin(com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND)
                                        ),
                                    "road-label"
                                )

                                // Couche Verte: vers domicile
                                style.addLayerBelow(
                                    LineLayer("layer-home", "route-home")
                                        .withProperties(
                                            PropertyFactory.lineColor(Emerald500.toArgb()),
                                            PropertyFactory.lineWidth(8f),
                                            PropertyFactory.lineOpacity(0.9f),
                                            PropertyFactory.lineCap(com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND),
                                            PropertyFactory.lineJoin(com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND)
                                        ),
                                    "road-label"
                                )
                            }

                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(46.603354, 1.888334))
                                .zoom(5.0)
                                .build()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Légende des couleurs
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RouteLegendItem(color = Rose500, label = "→ Point de départ", isActive = currentPhase == NavigationPhase.TO_PICKUP)
                    RouteLegendItem(color = Indigo500, label = "→ Destination", isActive = currentPhase == NavigationPhase.TO_DESTINATION)
                    RouteLegendItem(color = Emerald500, label = "→ Domicile", isActive = currentPhase == NavigationPhase.TO_HOME)
                }
            }

            // Bouton recenter
            FloatingActionButton(
                onClick = {
                    currentLocation?.let { loc ->
                        mapboxMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                17.0
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 200.dp, end = 16.dp),
                containerColor = Color.White
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Centrer")
            }
        }
    }

    // Dialog appel
    if (showCallDialog) {
        CallClientDialog(
            clientName = ride.clientName.ifBlank { "Client" },
            phoneNumber = ride.clientPhone,
            onDismiss = { showCallDialog = false },
            onCall = { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
                showCallDialog = false
            }
        )
    }

    // Dialog message
    if (showMessageDialog) {
        MessageClientDialog(
            clientName = ride.clientName.ifBlank { "Client" },
            phoneNumber = ride.clientPhone,
            defaultMessage = "Bonjour, je suis votre chauffeur et j'arrive.",
            onDismiss = { showMessageDialog = false },
            onSend = { message ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${ride.clientPhone}")
                    putExtra("sms_body", message)
                }
                context.startActivity(intent)
                showMessageDialog = false
            }
        )
    }

    // Dialog modifier course
    if (showEditDialog) {
        EditRideDialog(
            ride = ride,
            onDismiss = { showEditDialog = false },
            onSave = { updatedRide ->
                onEditRide(updatedRide)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun RouteLegendItem(color: Color, label: String, isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else Slate500,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun NavigationBottomBar(
    currentPhase: NavigationPhase,
    eta: String,
    distance: String,
    ride: RideRequest,
    onPhaseChange: (NavigationPhase) -> Unit,
    onComplete: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Info principale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (currentPhase) {
                            NavigationPhase.TO_PICKUP -> "Aller chercher:"
                            NavigationPhase.TO_DESTINATION -> "Destination:"
                            NavigationPhase.TO_HOME -> "Retour:"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                    Text(
                        text = when (currentPhase) {
                            NavigationPhase.TO_PICKUP -> ride.departure
                            NavigationPhase.TO_DESTINATION -> ride.arrival
                            NavigationPhase.TO_HOME -> "Domicile"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatBox(value = eta, label = "ETA")
                    StatBox(value = distance, label = "Distance")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Boutons de phase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PhaseButton(
                    phase = NavigationPhase.TO_PICKUP,
                    currentPhase = currentPhase,
                    color = Rose500,
                    icon = Icons.Default.PersonPin,
                    label = "Client",
                    onClick = { onPhaseChange(NavigationPhase.TO_PICKUP) }
                )

                PhaseButton(
                    phase = NavigationPhase.TO_DESTINATION,
                    currentPhase = currentPhase,
                    color = Indigo500,
                    icon = Icons.Default.Flag,
                    label = "Course",
                    onClick = { onPhaseChange(NavigationPhase.TO_DESTINATION) }
                )

                PhaseButton(
                    phase = NavigationPhase.TO_HOME,
                    currentPhase = currentPhase,
                    color = Emerald500,
                    icon = Icons.Default.Home,
                    label = "Maison",
                    onClick = { onPhaseChange(NavigationPhase.TO_HOME) }
                )

                // Bouton terminer
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Terminer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PhaseButton(
    phase: NavigationPhase,
    currentPhase: NavigationPhase,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val isActive = phase == currentPhase

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isActive) color else color.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else Slate500,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatBox(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate500
        )
    }
}

@Composable
private fun CallClientDialog(
    clientName: String,
    phoneNumber: String,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald500) },
        title = { Text("Appeler $clientName") },
        text = {
            Column {
                Text("Numéro: $phoneNumber")
                if (phoneNumber.isBlank()) {
                    Text(
                        "Aucun numéro de téléphone disponible",
                        color = Rose500,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCall(phoneNumber) },
                enabled = phoneNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Appeler")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun MessageClientDialog(
    clientName: String,
    phoneNumber: String,
    defaultMessage: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf(defaultMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Indigo500) },
        title = { Text("Message à $clientName") },
        text = {
            Column {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                if (phoneNumber.isBlank()) {
                    Text(
                        "Aucun numéro de téléphone disponible",
                        color = Rose500,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(message) },
                enabled = phoneNumber.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envoyer SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun EditRideDialog(
    ride: RideRequest,
    onDismiss: () -> Unit,
    onSave: (RideRequest) -> Unit
) {
    var departure by remember { mutableStateOf(ride.departure) }
    var arrival by remember { mutableStateOf(ride.arrival) }
    var time by remember { mutableStateOf(ride.time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Indigo500) },
        title = { Text("Modifier la course") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = departure,
                    onValueChange = { departure = it },
                    label = { Text("Point de départ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = arrival,
                    onValueChange = { arrival = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Heure") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(ride.copy(
                        departure = departure,
                        arrival = arrival,
                        time = time
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Sauvegarder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun updateMapWithAllRoutes(
    map: MapboxMap,
    routeToPickup: OsrmRoutingService.RouteResult?,
    routeToDestination: OsrmRoutingService.RouteResult?,
    routeToHome: OsrmRoutingService.RouteResult?,
    currentPhase: NavigationPhase
) {
    val style = map.style ?: return

    // Route Rouge: vers point de départ
    routeToPickup?.let { route ->
        if (route.success) {
            val geoJson = createRouteGeoJson(route.geometry)
            style.getSourceAs<GeoJsonSource>("route-pickup")?.setGeoJson(geoJson)
        }
    }

    // Route Bleue: vers destination
    routeToDestination?.let { route ->
        if (route.success) {
            val geoJson = createRouteGeoJson(route.geometry)
            style.getSourceAs<GeoJsonSource>("route-destination")?.setGeoJson(geoJson)
        }
    }

    // Route Verte: vers domicile
    routeToHome?.let { route ->
        if (route.success) {
            val geoJson = createRouteGeoJson(route.geometry)
            style.getSourceAs<GeoJsonSource>("route-home")?.setGeoJson(geoJson)
        }
    }

    // Ajuster la visibilité selon la phase
    when (currentPhase) {
        NavigationPhase.TO_PICKUP -> {
            style.getLayer("layer-pickup")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.VISIBLE))
            style.getLayer("layer-destination")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
            style.getLayer("layer-home")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
        }
        NavigationPhase.TO_DESTINATION -> {
            style.getLayer("layer-pickup")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
            style.getLayer("layer-destination")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.VISIBLE))
            style.getLayer("layer-home")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
        }
        NavigationPhase.TO_HOME -> {
            style.getLayer("layer-pickup")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
            style.getLayer("layer-destination")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.NONE))
            style.getLayer("layer-home")?.setProperties(PropertyFactory.visibility(com.mapbox.mapboxsdk.style.layers.Property.VISIBLE))
        }
    }
}

private fun createRouteGeoJson(geometry: List<Pair<Double, Double>>): String {
    val coordinatesArray = JSONArray()
    geometry.forEach { point ->
        val coord = JSONArray()
        coord.put(point.second) // longitude
        coord.put(point.first)  // latitude
        coordinatesArray.put(coord)
    }

    return JSONObject().apply {
        put("type", "Feature")
        put("geometry", JSONObject().apply {
            put("type", "LineString")
            put("coordinates", coordinatesArray)
        })
        put("properties", JSONObject())
    }.toString()
}

enum class NavigationPhase {
    TO_PICKUP,      // Rouge: Aller chercher le client
    TO_DESTINATION, // Bleu: Conduire le client
    TO_HOME         // Vert: Retour au domicile
}
