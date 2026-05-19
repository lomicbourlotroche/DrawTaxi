package com.drawtaxi.app.ui.screens.rides

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Looper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.screens.navigation.NavigationPhase
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
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

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ReturnHomeScreen(
    settings: AppSettings,
    brandColor: ComposeColor,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var osmMapView by remember { mutableStateOf<MapView?>(null) }
    var routeToHome by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }

    var etaText by remember { mutableStateOf("-- min") }
    var distanceText by remember { mutableStateOf("-- km") }
    var currentSpeed by remember { mutableStateOf("0 km/h") }
    var isRecalculating by remember { mutableStateOf(false) }
    var currentInstruction by remember { mutableStateOf("") }
    var nextInstruction by remember { mutableStateOf("") }
    var nextInstructionDistance by remember { mutableStateOf("") }

    // GPS tracking
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

    // Calculate route to home
    LaunchedEffect(currentLocation, settings.homeAddress) {
        if (currentLocation != null && settings.homeAddress.isNotBlank()) {
            val homeLoc = GeocodingService.geocode(settings.homeAddress)
            homeLoc?.let {
                routeToHome = OsrmRoutingService.calculateRoute(currentLocation!!, it)
            }
        }
    }

    // Turn-by-turn instructions + recalculation
    LaunchedEffect(currentLocation, routeToHome) {
        if (currentLocation == null || routeToHome == null) return@LaunchedEffect

        val route = routeToHome!!
        if (route.success && route.geometry.isNotEmpty()) {
            val currentLoc = Location("current").apply {
                latitude = currentLocation!!.latitude
                longitude = currentLocation!!.longitude
            }

            var minDistance = Double.MAX_VALUE
            var nearestIndex = 0
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

            // Recalculate if deviation
            if (minDistance > 100.0) {
                isRecalculating = true
                val homeLoc = GeocodingService.geocode(settings.homeAddress)
                if (homeLoc != null) {
                    routeToHome = OsrmRoutingService.calculateRoute(currentLocation!!, homeLoc)
                }
                isRecalculating = false
            }
        }
    }

    // ETA updates
    LaunchedEffect(routeToHome) {
        routeToHome?.let {
            if (it.success) {
                etaText = OsrmRoutingService.formatDuration(it.duration)
                distanceText = OsrmRoutingService.formatDistance(it.distance)
            }
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
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

                    routeToHome?.let { route ->
                        if (route.success && route.geometry.isNotEmpty()) {
                            val polyline = Polyline()
                            polyline.setPoints(ArrayList(route.geometry.map { GeoPoint(it.first, it.second) }))
                            polyline.color = Color.GREEN
                            polyline.width = 14f
                            map.overlays.add(0, polyline)

                            val destPoint = route.geometry.last()
                            val marker = Marker(map)
                            marker.position = GeoPoint(destPoint.first, destPoint.second)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Domicile"
                            map.overlays.add(marker)

                            currentLocation?.let { loc ->
                                val geoPoints = ArrayList<GeoPoint>()
                                geoPoints.addAll(route.geometry.map { GeoPoint(it.first, it.second) })
                                geoPoints.add(GeoPoint(loc.latitude, loc.longitude))
                                val bounds = BoundingBox.fromGeoPoints(geoPoints)
                                map.zoomToBoundingBox(bounds, true, 100)
                            }
                        }
                    }

                    currentLocation?.let { loc ->
                        val currentPoint = GeoPoint(loc.latitude, loc.longitude)
                        map.controller.animateTo(currentPoint)
                    }
                },
                modifier = Modifier.fillMaxSize()
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
                    phase = NavigationPhase.TO_HOME
                )
            }

            ReturnHomeBottomPanel(
                eta = etaText,
                distance = distanceText,
                homeAddress = settings.homeAddress,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ReturnHomeBottomPanel(
    eta: String,
    distance: String,
    homeAddress: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Retour domicile",
                            style = MaterialTheme.typography.labelMedium,
                            color = Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = homeAddress.ifBlank { "Non défini" },
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
            }
        }
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
private fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

private fun getDirectionIcon(instruction: String): androidx.compose.ui.graphics.vector.ImageVector {
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
