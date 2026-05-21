package com.drawtaxi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.theme.Slate400
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RideMap(
    departure: String,
    arrival: String,
    modifier: Modifier = Modifier
) {
    var points by remember { mutableStateOf<Pair<LatLng?, LatLng?>>(null to null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(46.603354, 1.888334), 5f)
    }

    LaunchedEffect(departure, arrival) {
        withContext(Dispatchers.IO) {
            try {
                val fromLocation = GeocodingService.geocode(departure)
                val toLocation = GeocodingService.geocode(arrival)

                if (fromLocation != null && toLocation != null) {
                    val start = LatLng(fromLocation.latitude, fromLocation.longitude)
                    val end = LatLng(toLocation.latitude, toLocation.longitude)
                    points = start to end

                    val routeResult = OsrmRoutingService.calculateRoute(fromLocation, toLocation)

                    if (routeResult.success && routeResult.geometry.isNotEmpty()) {
                        routePoints = routeResult.geometry.map { LatLng(it.first, it.second) }
                    } else {
                        routePoints = listOf(start, end)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val midLat = routePoints.map { it.latitude }.let { (it.min() + it.max()) / 2.0 }
            val midLng = routePoints.map { it.longitude }.let { (it.min() + it.max()) / 2.0 }
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLng), 12f)
        }
    }

    Box(modifier = modifier.background(Slate400.copy(alpha = 0.1f))) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val (start, end) = points
            if (start != null || end != null) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = Color.Blue,
                            width = 12f
                        )
                    }

                    start?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Départ: $departure"
                        )
                    }

                    end?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Arrivée: $arrival"
                        )
                    }
                }
            } else {
                Text("Impossible de localiser les adresses", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
