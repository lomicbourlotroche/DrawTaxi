package com.drawtaxi.app.ui.screens.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.screens.rides.InstructionCard
import com.drawtaxi.app.ui.theme.*
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

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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

    var currentPhase by remember { mutableStateOf(NavigationPhase.TO_PICKUP) }
    var showCallDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    var pickupLocation by remember { mutableStateOf<Location?>(null) }
    var destLocation by remember { mutableStateOf<Location?>(null) }
    var hasGeocoded by remember { mutableStateOf(false) }

    val engine = remember {
        NavigationEngine(context, scope)
    }
    val engineState by engine.state.collectAsState()

    val cameraState = rememberCameraState()

    val activeRoutePoints = engineState.routePoints
    val activeRouteKey = "${currentPhase.name}_${activeRoutePoints.size}"

    DisposableEffect(Unit) {
        onDispose { engine.destroy() }
    }

    LaunchedEffect(ride) {
        if (!hasGeocoded) {
            if (ride.departure.isNotBlank()) {
                pickupLocation = GeocodingService.geocode(ride.departure)
            }
            if (ride.arrival.isNotBlank()) {
                if (ride.departure.isNotBlank()) {
                    val fromLoc = GeocodingService.geocode(ride.departure)
                    destLocation = GeocodingService.geocode(ride.arrival)
                } else {
                    destLocation = GeocodingService.geocode(ride.arrival)
                }
            }
            hasGeocoded = true
        }
    }

    LaunchedEffect(currentPhase, engineState.currentLocation, pickupLocation, destLocation) {
        if (currentPhase == NavigationPhase.TO_PICKUP || currentPhase == NavigationPhase.TO_DESTINATION) {
            val from = when (currentPhase) {
                NavigationPhase.TO_PICKUP -> engineState.currentLocation
                NavigationPhase.TO_DESTINATION -> pickupLocation
                else -> null
            }
            val to = when (currentPhase) {
                NavigationPhase.TO_PICKUP -> pickupLocation
                NavigationPhase.TO_DESTINATION -> destLocation
                else -> null
            }

            if (from != null && to != null && !engineState.isNavigating) {
                val route = NavigationEngine.fetchRoute(
                    fromLat = from.latitude, fromLng = from.longitude,
                    toLat = to.latitude, toLng = to.longitude
                )
                if (route != null) {
                    engine.startNavigation(route)
                }
            }
        }
    }

    LaunchedEffect(currentPhase) {
        if (currentPhase != NavigationPhase.TO_PICKUP && currentPhase != NavigationPhase.TO_DESTINATION) {
            engine.stopNavigation()
        }
    }

    LaunchedEffect(activeRouteKey, engineState.currentLocation, pickupLocation, destLocation) {
        val allPoints = mutableListOf<Pair<Double, Double>>()
        allPoints.addAll(activeRoutePoints)
        engineState.currentLocation?.let { allPoints.add(it.latitude to it.longitude) }
        val target = when (currentPhase) {
            NavigationPhase.TO_PICKUP -> pickupLocation
            NavigationPhase.TO_DESTINATION -> destLocation
            else -> null
        }
        target?.let { allPoints.add(it.latitude to it.longitude) }

        if (allPoints.isNotEmpty()) {
            val minLat = allPoints.minOf { it.first }
            val maxLat = allPoints.maxOf { it.first }
            val minLng = allPoints.minOf { it.second }
            val maxLng = allPoints.maxOf { it.second }
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        longitude = (minLng + maxLng) / 2.0,
                        latitude = (minLat + maxLat) / 2.0
                    ),
                    zoom = 13.0
                )
            )
        }
    }

    LaunchedEffect(engineState.currentLocation, pickupLocation, currentPhase) {
        if (currentPhase == NavigationPhase.TO_PICKUP && engineState.currentLocation != null && pickupLocation != null) {
            if (engineState.currentLocation!!.distanceTo(pickupLocation!!) < 50f) {
                currentPhase = NavigationPhase.TO_DESTINATION
            }
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
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Appeler", modifier = Modifier.size(24.dp))
                }
                FloatingActionButton(
                    onClick = { showMessageDialog = true },
                    containerColor = Indigo500,
                    contentColor = Color.White,
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

                LaunchedEffect(activeRouteKey) {
                    val coords = activeRoutePoints.joinToString(",") { "[${it.second},${it.first}]" }
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

                LaunchedEffect(currentPhase, pickupLocation, destLocation) {
                    val target = when (currentPhase) {
                        NavigationPhase.TO_PICKUP -> pickupLocation
                        NavigationPhase.TO_DESTINATION -> destLocation
                        else -> null
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

            AnimatedVisibility(
                visible = engineState.currentInstruction.isNotBlank(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp)
            ) {
                InstructionCard(
                    instruction = engineState.currentInstruction,
                    nextInstruction = engineState.nextInstruction,
                    nextDistance = engineState.nextInstructionDistance,
                    isRecalculating = false,
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
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Calcul...", color = Color.White)
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
                    color = Color.White.copy(alpha = 0.95f),
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
                            Text("Vitesse: ${engineState.currentSpeed}", style = MaterialTheme.typography.bodySmall)
                            Text("Distance: ${engineState.distanceText}", style = MaterialTheme.typography.bodySmall)
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
