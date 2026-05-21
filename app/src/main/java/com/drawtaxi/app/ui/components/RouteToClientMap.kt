package com.drawtaxi.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drawtaxi.app.logic.routing.fetchRoute
import com.drawtaxi.app.logic.routing.RouteInfo
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun RouteToClientMap(
    pickupAddress: String,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var pickupLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var durationMin by remember { mutableStateOf<Int?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(48.39, -4.49), 10f)
    }

    LaunchedEffect(pickupAddress) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val location = fusedLocationClient.lastLocation.await()
                        location?.let {
                            currentLocation = LatLng(it.latitude, it.longitude)
                        }
                    }

                    if (pickupAddress.isNotBlank()) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocationName(pickupAddress, 1)
                        if (!addresses.isNullOrEmpty()) {
                            pickupLocation = LatLng(addresses[0].latitude, addresses[0].longitude)
                        }
                    }

                    if (currentLocation != null && pickupLocation != null) {
                        val startLoc = Location("start").apply {
                            latitude = currentLocation!!.latitude
                            longitude = currentLocation!!.longitude
                        }
                        val endLoc = Location("end").apply {
                            latitude = pickupLocation!!.latitude
                            longitude = pickupLocation!!.longitude
                        }
                        val routeInfo = fetchRoute(startLoc, endLoc)
                        routePoints = routeInfo.points.map {
                            LatLng(it.latitude, it.longitude)
                        }
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

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val allPoints = routePoints.toMutableList()
            currentLocation?.let { allPoints.add(it) }
            pickupLocation?.let { allPoints.add(it) }
            if (allPoints.isNotEmpty()) {
                val midLat = allPoints.map { it.latitude }.let { (it.min() + it.max()) / 2.0 }
                val midLng = allPoints.map { it.longitude }.let { (it.min() + it.max()) / 2.0 }
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLng), 12f)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = brandColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (currentLocation != null || pickupLocation != null) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        currentLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Ma position",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                        }

                        pickupLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Point de rendez-vous",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }

                        if (routePoints.isNotEmpty()) {
                            Polyline(
                                points = routePoints,
                                color = Color(0xFF4CAF50),
                                width = 10f
                            )
                        } else if (currentLocation != null && pickupLocation != null) {
                            Polyline(
                                points = listOf(currentLocation!!, pickupLocation!!),
                                color = Color(0xFFFF9800),
                                width = 6f
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Position non disponible", color = Slate500)
                    }
                }
            }
        }
    }
}
