package com.drawtaxi.app.ui.screens.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.theme.*
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsNavigationScreen(
    ride: RideRequest,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var destLocation by remember { mutableStateOf<Location?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("--") }
    var distanceToDest by remember { mutableStateOf("--") }
    var speed by remember { mutableStateOf("0 km/h") }
    var isMapReady by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    val mapView = remember {
        MapView(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLocationUpdates(context, locationManager) { location ->
                currentLocation = location
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED && coarseLocation == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(context, locationManager) { location ->
                currentLocation = location
            }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        if (ride.arrival.isNotBlank()) {
            destLocation = geocodeAddress(context, ride.arrival)
        }
    }

    LaunchedEffect(currentLocation, destLocation, isNavigating) {
        currentLocation?.let { loc ->
            speed = if (loc.hasSpeed()) {
                String.format("%.0f km/h", loc.speed * 3.6)
            } else "0 km/h"

            if (isNavigating && destLocation != null) {
                val dist = haversineDistance(loc, destLocation!!)
                distanceToDest = String.format("%.1f km", dist)

                val avgSpeedKmh = if (loc.hasSpeed() && loc.speed > 0) loc.speed * 3.6 else 40.0
                val etaMinutes = (dist / avgSpeedKmh) * 60
                eta = if (etaMinutes < 1) "< 1 min" else "~${etaMinutes.toInt()} min"
            }
        }
    }

    LaunchedEffect(isMapReady, currentLocation) {
        if (!isMapReady || mapInstance == null || currentLocation == null) return@LaunchedEffect
        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(currentLocation!!.latitude, currentLocation!!.longitude), 15.0
        ))
    }

    LaunchedEffect(isMapReady, destLocation) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        destLocation?.let { dest ->
            mapInstance?.addMarker(MarkerOptions()
                .setPosition(LatLng(dest.latitude, dest.longitude))
                .setTitle(ride.arrival))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        TopAppBar(
            title = {
                Column {
                    Text("Navigation", color = Color.White)
                    Text(
                        "${ride.departure.ifBlank { "..." }} → ${ride.arrival.ifBlank { "..." }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        onCreate(null)
                        onStart()
                        onResume()
                        getMapAsync { map ->
                            mapInstance = map
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                                isMapReady = true
                            }
                        }
                    }
                    mapView
                }
            )

            if (currentLocation == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Acquisition GPS...", color = Color.White)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate800,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavStatColumn(value = eta, label = "ETA", icon = Icons.Default.Schedule)
                    NavStatColumn(value = distanceToDest, label = "Distance", icon = Icons.Default.Route)
                    NavStatColumn(value = speed, label = "Vitesse", icon = Icons.Default.Speed)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isNavigating = !isNavigating },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNavigating) Red500 else Green500
                    )
                ) {
                    Icon(
                        if (isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isNavigating) "Arrêter" else "Naviguer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Client", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text(ride.sender, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Heure", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text(ride.time.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun geocodeAddress(context: Context, address: String): Location? {
    return try {
        val geocoder = Geocoder(context)
        val results: List<Address>? = geocoder.getFromLocationName(address, 1)
        if (!results.isNullOrEmpty()) {
            val addr = results[0]
            Location("geocoder").apply {
                latitude = addr.latitude
                longitude = addr.longitude
            }
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun haversineDistance(from: Location, to: Location): Double {
    val r = 6371.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun startLocationUpdates(context: Context, locationManager: LocationManager, onLocation: (Location) -> Unit) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val listener = android.location.LocationListener { location ->
            onLocation(location)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0f, listener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 0f, listener)
    }
}

@Composable
private fun NavStatColumn(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
    }
}
