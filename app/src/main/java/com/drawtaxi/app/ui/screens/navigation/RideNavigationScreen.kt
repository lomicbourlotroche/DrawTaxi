package com.drawtaxi.app.ui.screens.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.screens.rides.InstructionCard
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

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
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    var currentPhase by remember { mutableStateOf(NavigationPhase.TO_PICKUP) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

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

    var currentInstruction by remember { mutableStateOf("") }
    var nextInstruction by remember { mutableStateOf("") }
    var nextInstructionDistance by remember { mutableStateOf("") }
    var isMapReady by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context)
    }

    val activeRoute = remember(currentPhase, routeToPickup, routeToDestination) {
        when (currentPhase) {
            NavigationPhase.TO_PICKUP -> routeToPickup
            NavigationPhase.TO_DESTINATION -> routeToDestination
            else -> null
        }
    }

    val routeColor = remember(currentPhase) {
        when (currentPhase) {
            NavigationPhase.TO_PICKUP -> "#FF0000"
            NavigationPhase.TO_DESTINATION -> "#0000FF"
            NavigationPhase.TO_HOME -> "#00AA00"
            NavigationPhase.COMPLETED -> "#888888"
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            val locationListener = android.location.LocationListener { location ->
                currentLocation = location
                currentSpeed = if (location.hasSpeed()) {
                    String.format("%.0f km/h", location.speed * 3.6)
                } else "0 km/h"
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 0f, locationListener)
        }
    }

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

    LaunchedEffect(currentLocation, ride.departure) {
        if (currentLocation != null && ride.departure.isNotBlank()) {
            val pickupLoc = GeocodingService.geocode(ride.departure)
            pickupLoc?.let {
                routeToPickup = OsrmRoutingService.calculateRoute(currentLocation!!, it)
            }
        }
    }

    LaunchedEffect(isMapReady, activeRoute, currentLocation, currentPhase) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        val map = mapInstance!!
        map.clear()

        val currentRoute = when (currentPhase) {
            NavigationPhase.TO_PICKUP -> routeToPickup
            NavigationPhase.TO_DESTINATION -> routeToDestination
            else -> null
        }

        val points = currentRoute?.geometry?.map { LatLng(it.first, it.second) } ?: emptyList()

        if (points.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.parseColor("#2196F3"))
                    .width(16f)
            )

            val allPts = points.toMutableList()
            currentLocation?.let { allPts.add(LatLng(it.latitude, it.longitude)) }

            if (allPts.isNotEmpty()) {
                val minLat = allPts.minOf { it.latitude }
                val maxLat = allPts.maxOf { it.latitude }
                val minLng = allPts.minOf { it.longitude }
                val maxLng = allPts.maxOf { it.longitude }

                val center = LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 13.0))
            }
        }

        currentLocation?.let {
            map.addMarker(MarkerOptions().setPosition(LatLng(it.latitude, it.longitude)).setTitle("Vous"))
        }
    }

    LaunchedEffect(currentLocation, currentPhase, routeToPickup, routeToDestination) {
        if (currentLocation == null) return@LaunchedEffect

        val active = when (currentPhase) {
            NavigationPhase.TO_PICKUP -> routeToPickup
            NavigationPhase.TO_DESTINATION -> routeToDestination
            else -> null
        }

        active?.let { route ->
            if (route.success && route.geometry.isNotEmpty()) {
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

                var allSteps = route.legs.flatMap { it.steps }
                if (allSteps.isNotEmpty()) {
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

    LaunchedEffect(isMapReady, currentLocation) {
        if (!isMapReady || mapInstance == null || currentLocation == null) return@LaunchedEffect
        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(currentLocation!!.latitude, currentLocation!!.longitude), 16.0
        ))
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Scaffold(
        floatingActionButton = {
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        onCreate(null)
                        onStart()
                        onResume()
                        getMapAsync { map ->
                            mapInstance = map
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                                isMapReady = true
                            }
                        }
                    }
                    mapView
                }
            )

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
                    eta = etaText,
                    distance = distanceText
                )
            }

            if (isRecalculating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(ComposeColor.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = ComposeColor.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Recalcul...", color = ComposeColor.White)
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Vitesse: $currentSpeed", style = MaterialTheme.typography.bodySmall)
                            Text("Distance: $distanceText", style = MaterialTheme.typography.bodySmall)
                            if (currentPhase == NavigationPhase.TO_PICKUP) {
                                Button(
                                    onClick = { currentPhase = NavigationPhase.TO_DESTINATION },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Démarrer course", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PhaseButton(
                                phase = NavigationPhase.TO_PICKUP,
                                label = "Client",
                                icon = Icons.Default.PersonPin,
                                currentPhase = currentPhase,
                                onClick = { currentPhase = NavigationPhase.TO_PICKUP }
                            )
                            PhaseButton(
                                phase = NavigationPhase.TO_DESTINATION,
                                label = "Destination",
                                icon = Icons.Default.Place,
                                currentPhase = currentPhase,
                                onClick = { currentPhase = NavigationPhase.TO_DESTINATION }
                            )
                            PhaseButton(
                                phase = NavigationPhase.TO_HOME,
                                label = "Retour",
                                icon = Icons.Default.Home,
                                currentPhase = currentPhase,
                                onClick = { currentPhase = NavigationPhase.TO_HOME }
                            )
                            PhaseButton(
                                phase = NavigationPhase.COMPLETED,
                                label = "Terminer",
                                icon = Icons.Default.CheckCircle,
                                currentPhase = currentPhase,
                                onClick = { showCompleteDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseButton(
    phase: NavigationPhase,
    label: String,
    icon: ImageVector,
    currentPhase: NavigationPhase,
    onClick: () -> Unit
) {
    val isActive = phase == currentPhase
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isActive) Modifier.background(Indigo500.copy(alpha = 0.1f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Indigo500 else Slate400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Indigo500 else Slate500
        )
    }
}
