package com.drawtaxi.app.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.OsrmRoutingService
import com.drawtaxi.app.ui.theme.Slate400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RideMap(
    departure: String,
    arrival: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var points by remember { mutableStateOf<Pair<GeoPoint?, GeoPoint?>>(null to null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(departure, arrival) {
        withContext(Dispatchers.IO) {
            try {
                // Géocodage avec GeocodingService
                val fromLocation = GeocodingService.geocode(departure)
                val toLocation = GeocodingService.geocode(arrival)

                if (fromLocation != null && toLocation != null) {
                    points = GeoPoint(fromLocation.latitude, fromLocation.longitude) to
                            GeoPoint(toLocation.latitude, toLocation.longitude)

                    // Calcul itinéraire via OSRM (trajet réel, pas ligne droite)
                    val routeResult = OsrmRoutingService.calculateRoute(fromLocation, toLocation)

                    if (routeResult.success && routeResult.geometry.isNotEmpty()) {
                        routePoints = routeResult.geometry.map { GeoPoint(it.first, it.second) }
                    } else {
                        // Fallback: ligne directe si OSRM échoue
                        routePoints = listOf(points.first!!, points.second!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier.background(Slate400.copy(0.1f))) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val (start, end) = points
            if (start != null || end != null) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_ride_detail", 0))
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()

                        // Marqueur départ
                        if (start != null) {
                            val startMarker = Marker(mapView)
                            startMarker.position = start
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            startMarker.title = "Départ: $departure"
                            mapView.overlays.add(startMarker)
                        }

                        // Marqueur arrivée
                        if (end != null) {
                            val endMarker = Marker(mapView)
                            endMarker.position = end
                            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            endMarker.title = "Arrivée: $arrival"
                            mapView.overlays.add(endMarker)
                        }

                        // Tracé itinéraire réel (OSRM)
                        if (routePoints.isNotEmpty()) {
                            val line = Polyline()
                            line.setPoints(routePoints)
                            line.color = android.graphics.Color.BLUE
                            line.width = 12.0f
                            mapView.overlays.add(line)

                            mapView.post {
                                if (line.bounds != null) {
                                    mapView.zoomToBoundingBox(line.bounds, true, 50)
                                }
                            }
                        } else if (start != null && end != null) {
                            // Fallback: ligne directe
                            val line = Polyline()
                            line.addPoint(start)
                            line.addPoint(end)
                            line.color = android.graphics.Color.RED
                            line.width = 5.0f
                            mapView.overlays.add(line)

                            mapView.post {
                                mapView.zoomToBoundingBox(line.bounds, true, 100)
                            }
                        } else if (start != null) {
                            mapView.controller.setCenter(start)
                        } else if (end != null) {
                            mapView.controller.setCenter(end)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Impossible de localiser les adresses", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
