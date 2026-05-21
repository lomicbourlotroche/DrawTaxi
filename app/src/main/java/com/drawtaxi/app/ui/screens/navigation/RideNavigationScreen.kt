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
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions

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

    val routePoints = remember(activeRoute) {
        activeRoute?.geometry?.map { LatLng(it.first, it.second) } ?: emptyList()
    }

    val destPoint = remember(activeRoute) {
        activeRoute?.geometry?.lastOrNull()?.let { LatLng(it.first, it.second) }
    }

    val routeColor = remember(currentPhase) {
        when (currentPhase) {
            NavigationPhase.TO_PICKUP -> ComposeColor(0xFFFF0000)
            NavigationPhase.TO_DESTINATION -> ComposeColor(0xFF0000FF)
            NavigationPhase.TO_HOME -> ComposeColor(0xFF00FF00)
            NavigationPhase.COMPLETED -> ComposeColor.Gray
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

    LaunchedEffect(isMapReady, routePoints, currentLocation, currentPhase) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        val map = mapInstance!!

        map.clear()

        if (routePoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(routeColor.hashCode())
                    .width(14f)
            )

            val allPts = routePoints.toMutableList()
            currentLocation?.let { loc ->
                allPts.add(LatLng(loc.latitude, loc.longitude))
            }
            val midLat = allPts.map { it.latitude }.let { (it.min() + it.max()) / 2.0 }
            val midLng = allPts.map { it.longitude }.let { (it.min() + it.max()) / 2.0 }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 12.0))
        }

        destPoint?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle(
                when (currentPhase) {
                    NavigationPhase.TO_PICKUP -> "Client: ${ride.departure}"
                    NavigationPhase.TO_DESTINATION -> "Destination: ${ride.arrival}"
                    else -> "Terminé"
                }
            ))
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
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Scaffold(
        topBar = {
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

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when (currentPhase) {
                                NavigationPhase.TO_PICKUP -> "Vers client"
                                NavigationPhase.TO_DESTINATION -> "Vers destination"
                                NavigationPhase.TO_HOME -> "Retour maison"
                                NavigationPhase.COMPLETED -> "Terminé"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                }
            }
        },
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
                    mapView.getMapAsync { map ->
                        mapInstance = map
                        map.setStyle(Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")) {
                            isMapReady = true
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
                com.drawtaxi.app.ui.screens.rides.InstructionCard(
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

            // Bottom info
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
