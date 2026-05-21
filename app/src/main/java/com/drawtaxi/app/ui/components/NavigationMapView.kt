package com.drawtaxi.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun NavigationMapView(
    departure: String,
    arrival: String,
    brandColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var routeResult by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStepIndex by remember { mutableStateOf(0) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isRecalculating by remember { mutableStateOf(false) }
    var deviationCount by remember { mutableStateOf(0) }
    var lastRecalculationTime by remember { mutableStateOf(0L) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(46.603354, 1.888334), 5f)
    }

    val routePoints = remember(routeResult) {
        routeResult?.geometry?.map { LatLng(it.first, it.second) } ?: emptyList()
    }

    val startPoint = remember(routeResult) {
        routeResult?.geometry?.firstOrNull()?.let { LatLng(it.first, it.second) }
    }

    val endPoint = remember(routeResult) {
        routeResult?.geometry?.lastOrNull()?.let { LatLng(it.first, it.second) }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000L
            ).apply {
                setMinUpdateIntervalMillis(2000L)
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

    LaunchedEffect(departure, arrival) {
        calculateRoute(departure, arrival, scope,
            onStart = { isLoading = true },
            onSuccess = { route ->
                routeResult = route
                isLoading = false
                errorMessage = null
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    LaunchedEffect(currentLocation, routeResult) {
        if (currentLocation == null || routeResult == null || !routeResult!!.success) return@LaunchedEffect

        delay(5000)

        val distanceFromRoute = calculateDistanceFromRoute(currentLocation!!, routeResult!!.geometry)

        if (distanceFromRoute > 100.0 &&
            System.currentTimeMillis() - lastRecalculationTime > 10000) {

            deviationCount++

            if (deviationCount >= 2 || distanceFromRoute > 200.0) {
                isRecalculating = true
                lastRecalculationTime = System.currentTimeMillis()

                calculateRouteFromCurrentPosition(
                    currentLocation!!, arrival, scope,
                    onStart = { },
                    onSuccess = { route ->
                        routeResult = route
                        isRecalculating = false
                        deviationCount = 0
                    },
                    onError = {
                        isRecalculating = false
                    }
                )
            }
        } else if (distanceFromRoute < 50.0) {
            deviationCount = 0
        }
    }

    LaunchedEffect(routeResult) {
        routeResult?.let { route ->
            if (route.success && route.geometry.size >= 2) {
                val pts = route.geometry
                val midLat = (pts.first().first + pts.last().first) / 2.0
                val midLng = (pts.first().second + pts.last().second) / 2.0
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLng), 12f)
            }
        }
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude), 16f
            )
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                )
            ) {
                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = brandColor,
                        width = 8f
                    )
                }

                startPoint?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Départ",
                        snippet = departure,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                endPoint?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Arrivée",
                        snippet = arrival,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            if (isLoading) {
                LoadingOverlay("Calcul de l'itinéraire...")
            }

            if (isRecalculating) {
                LoadingOverlay("Recalcul de l'itinéraire...", Rose500)
            }

            errorMessage?.let { error ->
                ErrorOverlay(error)
            }

            if (deviationCount > 0 && !isRecalculating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Amber500,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Déviation détectée - Recalcul...",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        routeResult?.let { route ->
            if (route.success) {
                NavigationInfoPanel(
                    route = route,
                    currentStepIndex = currentStepIndex,
                    brandColor = brandColor,
                    onNextStep = {
                        if (currentStepIndex < route.legs.sumOf { it.steps.size } - 1) {
                            currentStepIndex++
                        }
                    },
                    onPreviousStep = {
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                        }
                    }
                )
            }
        }
    }
}

private suspend fun calculateRoute(
    departure: String,
    arrival: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (OsrmRoutingService.RouteResult) -> Unit,
    onError: (String) -> Unit
) {
    if (departure.isBlank() || arrival.isBlank()) {
        onError("Adresses invalides")
        return
    }

    scope.launch {
        onStart()

        val fromLocation = GeocodingService.geocode(departure)
        val toLocation = GeocodingService.geocode(arrival)

        if (fromLocation == null || toLocation == null) {
            onError("Impossible de localiser les adresses")
            return@launch
        }

        val result = OsrmRoutingService.calculateRoute(fromLocation, toLocation)

        if (result.success) {
            onSuccess(result)
        } else {
            onError(result.errorMessage ?: "Erreur de calcul")
        }
    }
}

private suspend fun calculateRouteFromCurrentPosition(
    currentLocation: Location,
    arrival: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (OsrmRoutingService.RouteResult) -> Unit,
    onError: () -> Unit
) {
    scope.launch {
        onStart()

        val toLocation = GeocodingService.geocode(arrival)

        if (toLocation == null) {
            onError()
            return@launch
        }

        val result = OsrmRoutingService.calculateRoute(currentLocation, toLocation)

        if (result.success) {
            onSuccess(result)
        } else {
            onError()
        }
    }
}

private fun calculateDistanceFromRoute(
    location: Location,
    routeGeometry: List<Pair<Double, Double>>
): Double {
    if (routeGeometry.isEmpty()) return Double.MAX_VALUE

    var minDistance = Double.MAX_VALUE
    val currentLatLng = Location("current").apply {
        latitude = location.latitude
        longitude = location.longitude
    }

    for (i in 0 until minOf(routeGeometry.size, 100)) {
        val point = routeGeometry[i]
        val routePoint = Location("route").apply {
            latitude = point.first
            longitude = point.second
        }

        val distance = currentLatLng.distanceTo(routePoint).toDouble()
        if (distance < minDistance) {
            minDistance = distance
        }
    }

    return minDistance
}

@Composable
private fun LoadingOverlay(message: String, color: androidx.compose.ui.graphics.Color = Indigo500) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = color
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(message)
            }
        }
    }
}

@Composable
private fun ErrorOverlay(error: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Rose50,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Rose500
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(error, color = Rose700)
            }
        }
    }
}

@Composable
private fun NavigationInfoPanel(
    route: OsrmRoutingService.RouteResult,
    currentStepIndex: Int,
    brandColor: androidx.compose.ui.graphics.Color,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit
) {
    val allSteps = remember(route) {
        route.legs.flatMap { it.steps }
    }

    val currentStep = allSteps.getOrNull(currentStepIndex)

    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Route,
                    label = "Distance",
                    value = OsrmRoutingService.formatDistance(route.distance)
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = "Durée",
                    value = OsrmRoutingService.formatDuration(route.duration)
                )
                StatItem(
                    icon = Icons.Default.Flag,
                    label = "Étapes",
                    value = "${allSteps.size}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(16.dp))

            currentStep?.let { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = brandColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    step.maneuver.contains("left") -> Icons.Default.TurnLeft
                                    step.maneuver.contains("right") -> Icons.Default.TurnRight
                                    step.maneuver.contains("straight") || step.maneuver.contains("continue") -> Icons.Default.Straight
                                    else -> Icons.Default.Navigation
                                },
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = OsrmRoutingService.getManeuverInstruction(step.maneuver, step.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${OsrmRoutingService.formatDistance(step.distance)} · ${OsrmRoutingService.formatDuration(step.duration)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onPreviousStep,
                        enabled = currentStepIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Précédent")
                    }

                    Text(
                        text = "${currentStepIndex + 1} / ${allSteps.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )

                    TextButton(
                        onClick = onNextStep,
                        enabled = currentStepIndex < allSteps.size - 1
                    ) {
                        Text("Suivant")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Slate500,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate500
        )
    }
}
