package com.drawtaxi.app.ui.screens.rides

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ReturnHomeScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var routeToHome by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }

    var etaText by remember { mutableStateOf("-- min") }
    var distanceText by remember { mutableStateOf("-- km") }
    var currentSpeed by remember { mutableStateOf("0 km/h") }
    var isRecalculating by remember { mutableStateOf(false) }
    var currentInstruction by remember { mutableStateOf("") }
    var nextInstruction by remember { mutableStateOf("") }
    var nextInstructionDistance by remember { mutableStateOf("") }
    var isMapReady by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context)
    }

    val routePoints = remember(routeToHome) {
        routeToHome?.geometry?.map { LatLng(it.first, it.second) } ?: emptyList()
    }

    val homePoint = remember(routeToHome) {
        routeToHome?.geometry?.lastOrNull()?.let { LatLng(it.first, it.second) }
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

    LaunchedEffect(currentLocation, settings.homeAddress) {
        if (currentLocation != null && settings.homeAddress.isNotBlank()) {
            val homeLoc = GeocodingService.geocode(settings.homeAddress)
            homeLoc?.let {
                routeToHome = OsrmRoutingService.calculateRoute(currentLocation!!, it)
            }
        }
    }

    LaunchedEffect(isMapReady, routeToHome) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        val map = mapInstance!!

        map.clear()

        if (routePoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(android.graphics.Color.rgb(0, 255, 0))
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

        homePoint?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle("Domicile"))
        }
    }

    LaunchedEffect(isMapReady, currentLocation) {
        if (!isMapReady || mapInstance == null || currentLocation == null) return@LaunchedEffect
        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(currentLocation!!.latitude, currentLocation!!.longitude), 16.0
        ))
    }

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

    LaunchedEffect(routeToHome) {
        routeToHome?.let {
            if (it.success) {
                etaText = OsrmRoutingService.formatDuration(it.duration)
                distanceText = OsrmRoutingService.formatDistance(it.distance)
            }
        }
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
        }
    }
}

@Composable
fun InstructionCard(
    instruction: String,
    nextInstruction: String,
    nextDistance: String,
    isRecalculating: Boolean,
    eta: String,
    distance: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ComposeColor.White.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = ComposeColor(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        instruction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (nextInstruction.isNotBlank()) {
                        Text(
                            "Puis $nextInstruction dans $nextDistance",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            }

            if (isRecalculating) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recalcul de l'itinéraire...", style = MaterialTheme.typography.bodySmall, color = Amber500)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distance", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(distance, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ETA", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(eta, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
