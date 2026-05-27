package com.drawtaxi.app.ui.screens.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.logic.routing.NavigationEngineState
import com.drawtaxi.app.ui.screens.rides.InstructionCard
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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

private const val EMPTY_FC = """{"type":"FeatureCollection","features":[]}"""

enum class NavigationPhase {
    TO_PICKUP,
    TO_DESTINATION,
    TO_HOME,
    COMPLETED
}

@OptIn(ExperimentalAnimationApi::class, FlowPreview::class)
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
    val viewModel: NavigationViewModel = viewModel()

    var showCallDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    val engineState by viewModel.engineState.collectAsState()
    val currentPhase by viewModel.currentPhase.collectAsState()
    val pickupLocation by viewModel.pickupLocation.collectAsState()
    val destLocation by viewModel.destLocation.collectAsState()
    val homeLocation by viewModel.homeLocation.collectAsState()

    val cameraState = rememberCameraState()
    var isCameraAutoFollow by remember { mutableStateOf(true) }

    var hasGeocoded by remember { mutableStateOf(false) }

    LaunchedEffect(ride) {
        if (!hasGeocoded) {
            viewModel.geocodeRide(ride.departure, ride.arrival)
            hasGeocoded = true
        }
    }

    var lastNavigatedPhase by remember { mutableStateOf<NavigationPhase?>(null) }

    LaunchedEffect(currentPhase, engineState.currentLocation, pickupLocation, destLocation, homeLocation) {
        val loc = engineState.currentLocation
        if (loc == null) return@LaunchedEffect

        if (currentPhase != lastNavigatedPhase) {
            when (currentPhase) {
                NavigationPhase.TO_PICKUP -> {
                    val to = pickupLocation
                    if (to != null) {
                        lastNavigatedPhase = currentPhase
                        viewModel.startNavigation(
                            fromLat = loc.latitude, fromLng = loc.longitude,
                            toLat = to.latitude, toLng = to.longitude
                        )
                    }
                }
                NavigationPhase.TO_DESTINATION -> {
                    val to = destLocation
                    if (to != null) {
                        lastNavigatedPhase = currentPhase
                        viewModel.startNavigation(
                            fromLat = loc.latitude, fromLng = loc.longitude,
                            toLat = to.latitude, toLng = to.longitude
                        )
                    }
                }
                NavigationPhase.TO_HOME -> {
                    if (settings.homeAddress.isNotBlank()) {
                        viewModel.geocodeHome(settings.homeAddress)
                        val to = homeLocation
                        if (to != null) {
                            lastNavigatedPhase = currentPhase
                            viewModel.startNavigation(
                                fromLat = loc.latitude, fromLng = loc.longitude,
                                toLat = to.latitude, toLng = to.longitude
                            )
                        }
                    }
                }
                NavigationPhase.COMPLETED -> {
                    lastNavigatedPhase = currentPhase
                    viewModel.stopNavigation()
                }
            }
        }
    }

    val activeRoutePoints = engineState.routePoints
    val activeRouteKey = "${currentPhase.name}_${activeRoutePoints.size}"

    LaunchedEffect(activeRouteKey) {
        val allPoints = mutableListOf<Pair<Double, Double>>()
        allPoints.addAll(activeRoutePoints)
        engineState.currentLocation?.let { allPoints.add(it.latitude to it.longitude) }
        val target = when (currentPhase) {
            NavigationPhase.TO_PICKUP -> pickupLocation
            NavigationPhase.TO_DESTINATION -> destLocation
            NavigationPhase.TO_HOME -> homeLocation
            NavigationPhase.COMPLETED -> null
        }
        target?.let { allPoints.add(it.latitude to it.longitude) }

        if (allPoints.isNotEmpty()) {
            val minLat = allPoints.minOf { it.first }
            val maxLat = allPoints.maxOf { it.first }
            val minLng = allPoints.minOf { it.second }
            val maxLng = allPoints.maxOf { it.second }
            val dynamicZoom = NavigationEngine.calculateZoom(allPoints)
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        longitude = (minLng + maxLng) / 2.0,
                        latitude = (minLat + maxLat) / 2.0
                    ),
                    zoom = dynamicZoom
                )
            )
        }
    }

    LaunchedEffect(isCameraAutoFollow, engineState.currentLocation, engineState.isNavigating) {
        val loc = engineState.currentLocation
        if (isCameraAutoFollow && engineState.isNavigating && loc != null) {
            val speedKmh = loc.speed * 3.6
            val zoom = when {
                speedKmh > 80 -> 14.5
                speedKmh > 50 -> 15.5
                else -> 16.5
            }
            val bearing = if (loc.hasBearing()) loc.bearing.toDouble() else 0.0
            cameraState.animateTo(
                CameraPosition(
                    target = Position(longitude = loc.longitude, latitude = loc.latitude),
                    zoom = zoom,
                    bearing = bearing,
                    tilt = 55.0  // 3D perspective tilt
                )
            )
        }
    }

    LaunchedEffect(currentPhase) {
        if (currentPhase == NavigationPhase.TO_PICKUP) {
            viewModel.engineState.debounce(2000).collectLatest {
                viewModel.checkArrivedAtPickup()
            }
        }
    }

    DrawTaxiScaffold { padding ->
        RideNavigationContent(
            engineState = engineState,
            currentPhase = currentPhase,
            pickupLocation = pickupLocation,
            destLocation = destLocation,
            homeLocation = homeLocation,
            cameraState = cameraState,
            hasLocationPermission = hasLocationPermission,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onCallClick = { showCallDialog = true },
            onMessageClick = { showMessageDialog = true },
            onPhaseChange = { phase ->
                if (phase == NavigationPhase.TO_HOME && settings.homeAddress.isBlank()) {
                    android.widget.Toast.makeText(
                        context,
                        "Veuillez configurer votre adresse de domicile dans les paramètres pro pour utiliser le retour à la maison.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    viewModel.setPhase(phase)
                }
            },
            onStartRideClick = {
                isCameraAutoFollow = true
                viewModel.navigateToPickup()
                viewModel.startLocationTracking(ride.id)
            },
            onCompleteClick = { showCompleteDialog = true },
            onRecenterClick = {
                isCameraAutoFollow = true
            },
            isCameraAutoFollow = isCameraAutoFollow,
            onAutoFollowChanged = { isCameraAutoFollow = it },
            modifier = Modifier.padding(padding)
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(48.dp)) },
            title = { Text("Terminer la course", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Confirmez-vous la fin de la course ?")
                    Spacer(modifier = Modifier.height(8.dp))
                    val trackedKm = viewModel.getTotalDistanceKm()
                    if (trackedKm > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Straighten, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Distance parcourue : ${"%.2f".format(trackedKm)} km", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                        }
                    }
                }
            },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        viewModel.stopLocationTracking()
                        val trackedKm = viewModel.getTotalDistanceKm()
                        val updatedRide = if (trackedKm > 0) {
                            ride.copy(distanceKm = trackedKm.toDouble())
                        } else ride
                        showCompleteDialog = false
                        onComplete(updatedRide)
                    },
                    containerColor = Emerald500,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Oui, terminer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RideNavigationContent(
    engineState: NavigationEngineState,
    currentPhase: NavigationPhase,
    pickupLocation: Location?,
    destLocation: Location?,
    homeLocation: Location?,
    cameraState: org.maplibre.compose.camera.CameraState,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onPhaseChange: (NavigationPhase) -> Unit,
    onStartRideClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onRecenterClick: () -> Unit,
    isCameraAutoFollow: Boolean,
    onAutoFollowChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("Map Placeholder (Preview Mode)")
            }
        } else if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Permission de localisation requise",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DrawTaxiSolidButton(
                        onClick = onRequestPermission,
                        containerColor = Indigo500
                    ) {
                        Text("Accorder la permission")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed }) {
                                    onAutoFollowChanged(false)
                                }
                            }
                        }
                    }
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
                        )
                    )
                ) {
                    val routeSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(EMPTY_FC),
                        options = GeoJsonOptions(synchronousUpdate = true)
                    )
                    val vousSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(EMPTY_FC),
                        options = GeoJsonOptions(synchronousUpdate = true)
                    )
                    val destSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(EMPTY_FC),
                        options = GeoJsonOptions(synchronousUpdate = true)
                    )

                    val routePoints = engineState.routePoints
                    val routeKey = "${currentPhase.name}_${routePoints.size}"

                    LaunchedEffect(routeKey) {
                        val coords = routePoints.joinToString(",") { "[${it.second},${it.first}]" }
                        val json = if (coords.isNotEmpty()) {
                            """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
                        } else EMPTY_FC
                        routeSource.setData(GeoJsonData.JsonString(json))
                    }

                    LaunchedEffect(engineState.currentLocation) {
                        val loc = engineState.currentLocation ?: return@LaunchedEffect
                        val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""
                        vousSource.setData(GeoJsonData.JsonString(json))
                    }

                    LaunchedEffect(currentPhase, pickupLocation, destLocation, homeLocation) {
                        val target = when (currentPhase) {
                            NavigationPhase.TO_PICKUP -> pickupLocation
                            NavigationPhase.TO_DESTINATION -> destLocation
                            NavigationPhase.TO_HOME -> homeLocation
                            NavigationPhase.COMPLETED -> null
                        }
                        if (target != null) {
                            val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${target.longitude},${target.latitude}]}}]}"""
                            destSource.setData(GeoJsonData.JsonString(json))
                        } else {
                            destSource.setData(GeoJsonData.JsonString(EMPTY_FC))
                        }
                    }

                    LineLayer(
                        id = "route-line",
                        source = routeSource,
                        color = const(Color(0xFF2196F3)),
                        width = const(8.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round)
                    )

                    CircleLayer(
                        id = "vous-marker",
                        source = vousSource,
                        color = const(Color(0xFF2196F3)),
                        radius = const(10.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )

                    CircleLayer(
                        id = "dest-marker",
                        source = destSource,
                        color = const(Color(0xFF10B981)),
                        radius = const(12.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )
                }
            }
        }

        if (hasLocationPermission) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 180.dp)
            ) {
                NavigationFab(
                    onClick = onRecenterClick,
                    icon = Icons.Default.MyLocation,
                    backgroundColor = if (isCameraAutoFollow) Indigo500 else Color.White,
                    contentColor = if (isCameraAutoFollow) Color.White else Slate700
                )

                NavigationFab(
                    onClick = onCallClick,
                    icon = Icons.Default.Phone,
                    backgroundColor = Emerald500
                )
                NavigationFab(
                    onClick = onMessageClick,
                    icon = Icons.AutoMirrored.Filled.Message,
                    backgroundColor = Indigo500
                )
            }

            AnimatedVisibility(
                visible = engineState.currentInstruction.isNotBlank(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                InstructionCard(
                    instruction = engineState.currentInstruction,
                    nextInstruction = engineState.nextInstruction,
                    nextDistance = engineState.nextInstructionDistance,
                    isRecalculating = engineState.isRecalculating,
                    eta = engineState.etaText,
                    distance = engineState.distanceText
                )
            }

            if (engineState.isCalculating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Calcul...", color = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DrawTaxiSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White.copy(alpha = 0.98f),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(Slate200, CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentPhase == NavigationPhase.TO_PICKUP) {
                        DrawTaxiSolidButton(
                            onClick = onStartRideClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Indigo600
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Démarrer la course", style = drawTaxiType().titleSmall, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhaseButton(
                            phase = NavigationPhase.TO_PICKUP,
                            label = "Client",
                            icon = Icons.Default.PersonPin,
                            currentPhase = currentPhase,
                            onClick = { onPhaseChange(NavigationPhase.TO_PICKUP) }
                        )
                        PhaseButton(
                            phase = NavigationPhase.TO_DESTINATION,
                            label = "Course",
                            icon = Icons.Default.Route,
                            currentPhase = currentPhase,
                            onClick = { onPhaseChange(NavigationPhase.TO_DESTINATION) }
                        )
                        PhaseButton(
                            phase = NavigationPhase.TO_HOME,
                            label = "Retour",
                            icon = Icons.Default.Home,
                            currentPhase = currentPhase,
                            onClick = { onPhaseChange(NavigationPhase.TO_HOME) }
                        )
                        PhaseButton(
                            phase = NavigationPhase.COMPLETED,
                            label = "Terminer",
                            icon = Icons.Default.CheckCircle,
                            currentPhase = currentPhase,
                            onClick = onCompleteClick,
                            isDestructive = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationFab(
    onClick: () -> Unit,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    DrawTaxiIconButton(
        onClick = onClick,
        shape = CircleShape,
        size = 56.dp,
        modifier = modifier
            .shadow(12.dp, CircleShape)
            .background(backgroundColor, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun PhaseButton(
    phase: NavigationPhase,
    label: String,
    icon: ImageVector,
    currentPhase: NavigationPhase,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val isActive = phase == currentPhase
    val tint = when {
        isActive && isDestructive -> Rose500
        isActive -> Indigo500
        else -> Slate400
    }
    val bgAlpha = if (isActive) 0.15f else 0f
    val bgColor = if (isDestructive) Rose500 else Indigo500

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(if (isActive) Modifier.background(bgColor.copy(alpha = 0.1f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = drawTaxiType().labelSmall,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = tint,
            fontSize = 11.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideNavigationContentPreview() {
    val sampleEngineState = NavigationEngineState(
        currentInstruction = "Tournez à droite sur Rue de la Paix",
        nextInstruction = "continuez tout droit",
        nextInstructionDistance = "200m",
        etaText = "12 min",
        distanceText = "4.2 km",
        currentSpeed = "45 km/h"
    )
    DrawTaxiTheme {
        val cameraState = rememberCameraState()
        RideNavigationContent(
            engineState = sampleEngineState,
            currentPhase = NavigationPhase.TO_PICKUP,
            pickupLocation = null,
            destLocation = null,
            homeLocation = null,
            cameraState = cameraState,
            hasLocationPermission = true,
            onRequestPermission = {},
            onCallClick = {},
            onMessageClick = {},
            onPhaseChange = {},
            onStartRideClick = {},
            onCompleteClick = {},
            onRecenterClick = {},
            isCameraAutoFollow = true,
            onAutoFollowChanged = {}
        )
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    pointerInput(onClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed.not() && it.previousPressed }) onClick()
            }
        }
    }
)
