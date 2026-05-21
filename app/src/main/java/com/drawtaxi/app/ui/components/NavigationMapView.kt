package com.drawtaxi.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

@SuppressLint("MissingPermission")
@Composable
fun NavigationMapView(
    departure: String,
    arrival: String,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var routeResult by remember { mutableStateOf<OsrmRoutingService.RouteResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deviationCount by remember { mutableStateOf(0) }
    var lastRecalculationTime by remember { mutableStateOf(0L) }
    var isRecalculating by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationListener = android.location.LocationListener { location ->
                currentLocation = location
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 3000L, 0f, locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 3000L, 0f, locationListener
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
                    onError = { error ->
                        errorMessage = error
                        isRecalculating = false
                    }
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")) {
                            // Map ready
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading || isRecalculating) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Chargement...", color = Color.White)
                }
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
    scope.launch {
        onStart()
        val from = GeocodingService.geocode(departure)
        val to = GeocodingService.geocode(arrival)
        
        if (from != null && to != null) {
            val result = OsrmRoutingService.calculateRoute(from, to)
            if (result.success) {
                onSuccess(result)
            } else {
                onError(result.errorMessage ?: "Erreur inconnue")
            }
        } else {
            onError("Adresse introuvable")
        }
    }
}

private suspend fun calculateRouteFromCurrentPosition(
    currentLocation: Location,
    arrival: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (OsrmRoutingService.RouteResult) -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        onStart()
        val to = GeocodingService.geocode(arrival)
        
        if (to != null) {
            val result = OsrmRoutingService.calculateRoute(currentLocation, to)
            if (result.success) {
                onSuccess(result)
            } else {
                onError(result.errorMessage ?: "Erreur inconnue")
            }
        } else {
            onError("Destination introuvable")
        }
    }
}

private fun calculateDistanceFromRoute(location: Location, geometry: List<Pair<Double, Double>>): Double {
    var minDistance = Double.MAX_VALUE
    for (point in geometry) {
        val routePoint = Location("route").apply {
            latitude = point.first
            longitude = point.second
        }
        val distance = location.distanceTo(routePoint).toDouble()
        if (distance < minDistance) {
            minDistance = distance
        }
    }
    return minDistance
}