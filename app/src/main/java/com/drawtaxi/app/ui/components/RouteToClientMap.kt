package com.drawtaxi.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.logic.routing.fetchRoute
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@Composable
fun RouteToClientMap(
    pickupAddress: String,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var pickupLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var durationMin by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pickupAddress) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val location = fusedLocationClient.lastLocation.await()
                        location?.let {
                            currentLocation = GeoPoint(it.latitude, it.longitude)
                        }
                    }

                    if (pickupAddress.isNotBlank()) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocationName(pickupAddress, 1)
                        if (!addresses.isNullOrEmpty()) {
                            pickupLocation = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                        }
                    }

                    if (currentLocation != null && pickupLocation != null) {
                        val routeInfo = fetchRoute(currentLocation!!, pickupLocation!!)
                        routePoints = routeInfo.points
                        distanceKm = routeInfo.distanceMeters / 1000.0
                        durationMin = (routeInfo.durationSeconds / 60).toInt()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Trajet vers le client",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (distanceKm != null && durationMin != null) {
                            Text(
                                "${String.format("%.1f", distanceKm)} km • ~${durationMin} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = brandColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (currentLocation != null || pickupLocation != null) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(14.0)
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.clear()

                            currentLocation?.let { loc ->
                                val marker = Marker(mapView)
                                marker.position = loc
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Ma position"
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(marker)
                            }

                            pickupLocation?.let { loc ->
                                val marker = Marker(mapView)
                                marker.position = loc
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Point de rendez-vous"
                                mapView.overlays.add(marker)
                            }

                            if (routePoints.isNotEmpty()) {
                                val line = Polyline()
                                line.setPoints(routePoints)
                                line.color = android.graphics.Color.parseColor("#4CAF50")
                                line.width = 10.0f
                                mapView.overlays.add(line)
                                
                                mapView.post {
                                    mapView.zoomToBoundingBox(line.bounds, true, 50)
                                }
                            } else if (currentLocation != null && pickupLocation != null) {
                                val line = Polyline()
                                line.addPoint(currentLocation!!)
                                line.addPoint(pickupLocation!!)
                                line.color = android.graphics.Color.parseColor("#FF9800")
                                line.width = 6.0f
                                mapView.overlays.add(line)
                                
                                mapView.post {
                                    mapView.zoomToBoundingBox(line.bounds, true, 100)
                                }
                            }

                            mapView.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Position non disponible",
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}
