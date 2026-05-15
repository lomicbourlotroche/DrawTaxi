package com.drawtaxi.app.ui.components

import com.drawtaxi.app.logic.fetchRoute

import android.location.Geocoder
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
import androidx.compose.ui.viewinterop.AndroidView
import com.drawtaxi.app.ui.theme.Slate400
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

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
                val geocoder = Geocoder(context, Locale.getDefault())
                // Simple geocoding implementation
                val depList = geocoder.getFromLocationName(departure, 1)
                val arrList = geocoder.getFromLocationName(arrival, 1)

                val depPoint = if (!depList.isNullOrEmpty()) GeoPoint(depList[0].latitude, depList[0].longitude) else null
                val arrPoint = if (!arrList.isNullOrEmpty()) GeoPoint(arrList[0].latitude, arrList[0].longitude) else null
                
                points = depPoint to arrPoint

                if (depPoint != null && arrPoint != null) {
                    val routeInfo = fetchRoute(depPoint, arrPoint)
                    routePoints = routeInfo.points
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
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val (start, end) = points
            if (start != null || end != null) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        
                        // Add start marker
                        if (start != null) {
                            val startMarker = Marker(mapView)
                            startMarker.position = start
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            startMarker.title = "Départ: $departure"
                            mapView.overlays.add(startMarker)
                        }

                        // Add end marker
                        if (end != null) {
                            val endMarker = Marker(mapView)
                            endMarker.position = end
                            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            endMarker.title = "Arrivée: $arrival"
                            mapView.overlays.add(endMarker)
                        }
                        
                        // Draw line if both exist
                        if (routePoints.isNotEmpty()) {
                            val line = Polyline()
                            line.setPoints(routePoints)
                            line.color = android.graphics.Color.BLUE
                            line.width = 12.0f
                            mapView.overlays.add(line)
                            
                            mapView.post {
                                mapView.zoomToBoundingBox(line.bounds, true, 50)
                            }
                        } else if (start != null && end != null) {
                            val line = Polyline()
                            line.addPoint(start)
                            line.addPoint(end)
                            line.color = android.graphics.Color.RED
                            line.width = 5.0f // removed mismatched type assignment if necessary, ensure float
                            mapView.overlays.add(line)
                            
                            // Zoom to fit
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
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun RideMapPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(com.drawtaxi.app.ui.theme.Slate100),
        contentAlignment = Alignment.Center
    ) {
        Text("Aperçu de la Carte (Statique)", color = com.drawtaxi.app.ui.theme.Slate400)
    }
}
