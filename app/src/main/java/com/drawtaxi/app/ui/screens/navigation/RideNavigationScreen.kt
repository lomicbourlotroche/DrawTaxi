package com.drawtaxi.app.ui.screens.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import java.util.*

enum class NavigationPhase {
    TO_PICKUP,
    TO_DESTINATION,
    TO_HOME,
    COMPLETED
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RideNavigationScreen(
    ride: RideRequest,
    settings: AppSettings,
    brandColor: ComposeColor,
    onBack: () -> Unit,
    onComplete: (RideRequest) -> Unit,
    onEditRide: (RideRequest) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentPhase by remember { mutableStateOf(NavigationPhase.TO_PICKUP) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var osmMapView by remember { mutableStateOf<MapView?>(null) }

    var routeToPickup by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }
    var routeToDestination by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }

    var showCallDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showPhaseSkipConfirm by remember { mutableStateOf(false) }

    var etaText by remember { mutableStateOf("-- min") }
    var distanceText by remember { mutableStateOf("-- km") }
    var currentSpeed by remember { mutableStateOf("0 km/h") }
    var isRecalculating by remember { mutableStateOf(false) }
    var deviationCount by remember { mutableStateOf(0) }
    var lastRecalculationTime by remember { mutableStateOf(0L) }

    // Instructions turn-by-turn
    var currentInstruction by remember { mutableStateOf("") }
    var nextInstruction by remember { mutableStateOf("") }
    var nextInstructionDistance by remember { mutableStateOf("") }

    // Suivi GPS
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1500L
            ).apply {
                setMinUpdateIntervalMillis(1000L)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        currentLocation = location
                        currentSpeed = if (location.hasSpeed()) {
                            String.format("%.0f km/h", location.speed * 3.6)
                        } else "0 km/h"
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

    // Calculer les routes
    LaunchedEffect(ride) {
        scope.launch {
            if (ride.departure.isNotBlank()) {
                val fromLoc = GeocodingService.geocode(ride.departure)
                val toLoc = GeocodingService.geocode(ride.arrival)

                if (fromLoc != null && toLoc != null) {
                    routeToDestination = OsrmRoutingService.calculateRoute(fromLoc, toLoc)
                }
            }
        }
    }

    // Route dynamique vers pickup
    LaunchedEffect(currentLocation, ride.departure) {
        if (currentLocation != null && ride.departure.isNotBlank()) {
            val pickupLoc = GeocodingService.geocode(ride.departure)
            pickupLoc?.let {
                routeToPickup = OsrmRoutingService.calculateRoute(currentLocation!!, it)
            }
        }
    }

    // Recalcul + instructions turn-by-turn
    LaunchedEffect(currentLocation, currentPhase) {
        if (currentLocation == null) return@LaunchedEffect

        val activeRoute = when (currentPhase) {
            NavigationPhase.TO_PICKUP -> routeToPickup
            NavigationPhase.TO_DESTINATION -> routeToDestination
            else -> null
        }

        activeRoute?.let { route ->
            if (route.success && route.geometry.isNotEmpty()) {
                // Trouver le point le plus proche
                var minDistance = Double.MAX_VALUE
                var nearestIndex = 0
                val currentLoc = Location("current").apply {
                    latitude = currentLocation!!.latitude
                    longitude = currentLocation!!.longitude
                }

                for (i in route.geometry.indices) {
                    val point = route.geometry[i]
                    val routePoint = Location("route").apply {
                        latitude = point.first
                        longitude = point.second
                    }
                    val distance = currentLoc.distanceTo(routePoint).toDouble()
                    if (distance < minDistance) {
                        minDistance = distance
                        nearestIndex = i
                    }
                }

                // Instructions turn-by-turn
                var allSteps = route.legs.flatMap { it.steps }
                if (allSteps.isNotEmpty()) {
                    // Trouver l'étape actuelle
                    var stepIndex = 0
                    var accumulatedDistance = 0.0
                    for (i in allSteps.indices) {
                        accumulatedDistance += allSteps[i].distance
                        if (accumulatedDistance > minDistance + route.geometry.drop(nearestIndex).size * 5) {
                            stepIndex = i
                            break
                        }
                    }

                    currentInstruction = OsrmRoutingService.getManeuverInstruction(
                        allSteps.getOrElse(stepIndex) { allSteps.first() }.maneuver,
                        allSteps.getOrElse(stepIndex) { allSteps.first() }.name
                    )

                    if (stepIndex + 1 < allSteps.size) {
                        val nextStep = allSteps[stepIndex + 1]
                        nextInstruction = OsrmRoutingService.getManeuverInstruction(nextStep.maneuver, nextStep.name)
                        nextInstructionDistance = OsrmRoutingService.formatDistance(nextStep.distance)
                    } else {
                        nextInstruction = ""
                        nextInstructionDistance = ""
                    }
                }

                // Recalcul si déviation
                if (minDistance > 100.0 && System.currentTimeMillis() - lastRecalculationTime > 10000) {
                    deviationCount++
                    if (deviationCount >= 2 || minDistance > 200.0) {
                        isRecalculating = true
                        lastRecalculationTime = System.currentTimeMillis()

                        val destAddress = when (currentPhase) {
                            NavigationPhase.TO_PICKUP -> ride.departure
                            NavigationPhase.TO_DESTINATION -> ride.arrival
                            else -> ""
                        }

                        if (destAddress.isNotBlank()) {
                            val destLoc = GeocodingService.geocode(destAddress)
                            if (destLoc != null) {
                                val newRoute = OsrmRoutingService.calculateRoute(currentLocation!!, destLoc)
                                when (currentPhase) {
                                    NavigationPhase.TO_PICKUP -> routeToPickup = newRoute
                                    NavigationPhase.TO_DESTINATION -> routeToDestination = newRoute
                                    else -> {}
                                }
                            }
                        }
                        isRecalculating = false
                        deviationCount = 0
                    }
                } else if (minDistance < 50.0) {
                    deviationCount = 0
                }
            }
        }
    }

    // ETA + auto-switch
    LaunchedEffect(currentLocation, currentPhase, routeToPickup, routeToDestination) {
        currentLocation?.let { location ->
            when (currentPhase) {
                NavigationPhase.TO_PICKUP -> {
                    routeToPickup?.let {
                        if (it.success) {
                            etaText = OsrmRoutingService.formatDuration(it.duration)
                            distanceText = OsrmRoutingService.formatDistance(it.distance)
                            val pickupLoc = GeocodingService.geocode(ride.departure)
                            pickupLoc?.let { pickup ->
                                if (location.distanceTo(pickup) < 50f) {
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
                        }
                    }
                }
                NavigationPhase.TO_HOME, NavigationPhase.COMPLETED -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            // Barre supérieure minimaliste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ComposeColor.White.copy(alpha = 0.95f))
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                }

                // Vitesse actuelle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentSpeed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }
            }
        },
        floatingActionButton = {
            // Boutons flottants
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showCallDialog = true },
                    containerColor = Emerald500,
                    contentColor = ComposeColor.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Appeler", modifier = Modifier.size(24.dp))
                }
                FloatingActionButton(
                    onClick = { showMessageDialog = true },
                    containerColor = Indigo500,
                    contentColor = ComposeColor.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Carte osmdroid plein écran
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_nav", 0))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        overlays.add(locationOverlay)
                        locationOverlay.enableMyLocation()
                        locationOverlay.isDrawAccuracyEnabled = false

                        osmMapView = this
                    }
                },
                update = { map ->
                    map.overlays.filterIsInstance<Polyline>().toList().forEach { map.overlays.remove(it) }
                    map.overlays.filterIsInstance<Marker>().toList().forEach { map.overlays.remove(it) }

                    val activeRoute = when (currentPhase) {
                        NavigationPhase.TO_PICKUP -> routeToPickup
                        NavigationPhase.TO_DESTINATION -> routeToDestination
                        else -> null
                    }

                    activeRoute?.let { route ->
                        if (route.success && route.geometry.isNotEmpty()) {
                            val polyline = Polyline()
                            polyline.setPoints(ArrayList(route.geometry.map { GeoPoint(it.first, it.second) }))
                            polyline.color = when (currentPhase) {
                                NavigationPhase.TO_PICKUP -> Color.RED
                                NavigationPhase.TO_DESTINATION -> Color.BLUE
                                else -> Color.GRAY
                            }
                            polyline.width = 14f
                            map.overlays.add(0, polyline)

                            // Marqueur destination
                            val destPoint = route.geometry.last()
                            val marker = Marker(map)
                            marker.position = GeoPoint(destPoint.first, destPoint.second)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = when (currentPhase) {
                                NavigationPhase.TO_PICKUP -> "Client: ${ride.departure}"
                                NavigationPhase.TO_DESTINATION -> "Destination: ${ride.arrival}"
                                else -> "Terminé"
                            }
                            map.overlays.add(marker)

                            // Zoom sur l'itinéraire + position actuelle
                            currentLocation?.let { loc ->
                                val geoPoints = ArrayList<GeoPoint>()
                                geoPoints.addAll(route.geometry.map { GeoPoint(it.first, it.second) })
                                geoPoints.add(GeoPoint(loc.latitude, loc.longitude))
                                val bounds = BoundingBox.fromGeoPoints(geoPoints)
                                map.zoomToBoundingBox(bounds, true, 100)
                            }
                        }
                    }

                    // Centrer sur la position actuelle
                    currentLocation?.let { loc ->
                        val currentPoint = GeoPoint(loc.latitude, loc.longitude)
                        map.controller.animateTo(currentPoint)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Panneau d'instructions turn-by-turn
            AnimatedVisibility(
                visible = currentInstruction.isNotBlank(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp)
            ) {
                InstructionCard(
                    instruction = currentInstruction,
                    nextInstruction = nextInstruction,
                    nextDistance = nextInstructionDistance,
                    isRecalculating = isRecalculating,
                    phase = currentPhase
                )
            }

            // Panneau d'information en bas
            BottomNavigationPanel(
                currentPhase = currentPhase,
                eta = etaText,
                distance = distanceText,
                ride = ride,
                onComplete = { showCompleteDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Dialog appel
    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald500) },
            title = { Text("Appeler ${ride.clientName.ifBlank { "Client" }}") },
            text = { Text("Numéro: ${ride.clientPhone.ifBlank { "Non disponible" }}") },
            confirmButton = {
                Button(
                    onClick = {
                        if (ride.clientPhone.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ride.clientPhone}")))
                        }
                        showCallDialog = false
                    },
                    enabled = ride.clientPhone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appeler")
                }
            },
            dismissButton = { TextButton(onClick = { showCallDialog = false }) { Text("Annuler") } }
        )
    }

    // Dialog message
    if (showMessageDialog) {
        var message by remember { mutableStateOf("Bonjour, je suis votre chauffeur et j'arrive.") }
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Indigo500) },
            title = { Text("Message à ${ride.clientName.ifBlank { "Client" }}") },
            text = {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ride.clientPhone.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${ride.clientPhone}")).apply {
                                putExtra("sms_body", message)
                            })
                        }
                        showMessageDialog = false
                    },
                    enabled = ride.clientPhone.isNotBlank() && message.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoyer")
                }
            },
            dismissButton = { TextButton(onClick = { showMessageDialog = false }) { Text("Annuler") } }
        )
    }

    // Dialog modifier
    if (showEditDialog) {
        var departure by remember { mutableStateOf(ride.departure) }
        var arrival by remember { mutableStateOf(ride.arrival) }
        var time by remember { mutableStateOf(ride.time) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modifier la course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = departure, onValueChange = { departure = it }, label = { Text("Départ") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = arrival, onValueChange = { arrival = it }, label = { Text("Arrivée") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Heure") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditRide(ride.copy(departure = departure, arrival = arrival, time = time))
                    showEditDialog = false
                }) { Text("Sauvegarder") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Annuler") } }
        )
    }

    // Dialog terminer course
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(40.dp)) },
            title = { Text("Terminer la course ?") },
            text = {
                Column {
                    Text("Course: ${ride.departure} → ${ride.arrival}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La course sera marquée comme terminée et archivée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val completedRide = ride.copy(
                            status = RideStatus.COMPLETED,
                            isPending = false
                        )
                        onComplete(completedRide)
                        showCompleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terminer")
                }
            },
            dismissButton = { TextButton(onClick = { showCompleteDialog = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun InstructionCard(
    instruction: String,
    nextInstruction: String,
    nextDistance: String,
    isRecalculating: Boolean,
    phase: NavigationPhase
) {
    val phaseColor = when (phase) {
        NavigationPhase.TO_PICKUP -> Rose500
        NavigationPhase.TO_DESTINATION -> Indigo500
        NavigationPhase.TO_HOME -> Emerald500
        NavigationPhase.COMPLETED -> Slate500
    }

    if (isRecalculating) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ComposeColor.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = phaseColor, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Recalcul de l'itinéraire...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ComposeColor.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Instruction principale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = phaseColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getDirectionIcon(instruction),
                                contentDescription = null,
                                tint = phaseColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Prochaine instruction
                if (nextInstruction.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dans $nextDistance : $nextInstruction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationPanel(
    currentPhase: NavigationPhase,
    eta: String,
    distance: String,
    ride: RideRequest,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (currentPhase) {
        NavigationPhase.TO_PICKUP -> Rose500
        NavigationPhase.TO_DESTINATION -> Indigo500
        NavigationPhase.TO_HOME -> Emerald500
        NavigationPhase.COMPLETED -> Slate500
    }

    val phaseLabel = when (currentPhase) {
        NavigationPhase.TO_PICKUP -> "Aller au client"
        NavigationPhase.TO_DESTINATION -> "En course"
        NavigationPhase.TO_HOME -> "Retour domicile"
        NavigationPhase.COMPLETED -> "Terminé"
    }

    val destinationText = when (currentPhase) {
        NavigationPhase.TO_PICKUP -> ride.departure
        NavigationPhase.TO_DESTINATION -> ride.arrival
        NavigationPhase.TO_HOME -> "Domicile"
        NavigationPhase.COMPLETED -> "Course terminée"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Barre de progression des phases
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PhaseProgressDot(isActive = currentPhase == NavigationPhase.TO_PICKUP, isCompleted = currentPhase.ordinal > 0, color = Rose500)
            PhaseProgressDot(isActive = currentPhase == NavigationPhase.TO_DESTINATION, isCompleted = currentPhase.ordinal > 1, color = Indigo500)
        }

        // Panneau principal
        Surface(
            color = ComposeColor.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // En-tête avec destination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = phaseLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = phaseColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destinationText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatItem(value = eta, label = "Arrivée", icon = Icons.Default.Schedule)
                        StatItem(value = distance, label = "Distance", icon = Icons.Default.Route)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton terminer
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (currentPhase) {
                            NavigationPhase.TO_PICKUP -> "Client récupéré"
                            NavigationPhase.TO_DESTINATION -> "Terminer la course"
                            NavigationPhase.TO_HOME -> "Retour en cours"
                            NavigationPhase.COMPLETED -> "Course terminée"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PhaseProgressDot(isActive: Boolean, isCompleted: Boolean, color: ComposeColor) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                when {
                    isCompleted -> color
                    isActive -> color
                    else -> Slate200
                }
            )
    )
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
    }
}

private fun getDirectionIcon(instruction: String): ImageVector {
    return when {
        instruction.contains("tourner", ignoreCase = true) -> Icons.Default.TurnRight
        instruction.contains("gauche", ignoreCase = true) -> Icons.Default.TurnLeft
        instruction.contains("droite", ignoreCase = true) -> Icons.Default.TurnRight
        instruction.contains("rond-point", ignoreCase = true) -> Icons.Default.Loop
        instruction.contains("continuer", ignoreCase = true) -> Icons.Default.Straight
        instruction.contains("départ", ignoreCase = true) -> Icons.Default.PlayArrow
        instruction.contains("arrivée", ignoreCase = true) -> Icons.Default.Flag
        else -> Icons.Default.Navigation
    }
}
