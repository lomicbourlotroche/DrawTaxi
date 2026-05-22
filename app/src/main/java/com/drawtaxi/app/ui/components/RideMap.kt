package com.drawtaxi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.theme.Slate400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@Composable
fun RideMap(
    departure: String,
    arrival: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var points by remember { mutableStateOf<Pair<LatLng?, LatLng?>>(null to null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
                } else {
                    errorMessage = "Impossible de localiser les adresses"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(isMapReady, routePoints, points) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        val map = mapInstance!!

        if (routePoints.isNotEmpty()) {
            map.clear()
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(android.graphics.Color.BLUE)
                    .width(12f)
            )

            val midLat = routePoints.map { it.latitude }.let { (it.min() + it.max()) / 2.0 }
            val midLng = routePoints.map { it.longitude }.let { (it.min() + it.max()) / 2.0 }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 12.0))
        }

        points.first?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle("Départ: $departure"))
        }
        points.second?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle("Arrivée: $arrival"))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.let {
                it.onPause()
                it.onStop()
                it.onDestroy()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
                MapView(it).apply {
                    onCreate(null)
                    onStart()
                    onResume()
                    getMapAsync { map ->
                        mapInstance = map
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                            isMapReady = true
                        }
                    }
                    mapViewRef = this
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = Slate400)
            }
        }
    }
}
