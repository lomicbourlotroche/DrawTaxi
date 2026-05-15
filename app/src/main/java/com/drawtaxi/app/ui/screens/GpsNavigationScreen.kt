package com.drawtaxi.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
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
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
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
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var destMarkerAdded by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLocationUpdates(context, fusedLocationClient) { location ->
                currentLocation = location
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED && coarseLocation == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(context, fusedLocationClient) { location ->
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

                updateMapPosition(mapView, loc, destLocation, ride)
            }
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

        if (currentLocation != null) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        overlays.add(locationOverlay)
                        locationOverlay.enableMyLocation()
                        locationOverlay.isDrawAccuracyEnabled = true

                        currentLocation?.let { loc ->
                            controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                        }

                        addRouteOverlay(this, currentLocation, destLocation, ride)

                        mapView = this
                    }
                },
                update = { map ->
                    if (!destMarkerAdded && destLocation != null) {
                        addDestinationMarker(map, destLocation!!, ride.arrival)
                        destMarkerAdded = true
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acquisition GPS...", color = Color.White)
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

private fun addRouteOverlay(
    map: MapView,
    current: Location?,
    dest: Location?,
    ride: RideRequest
) {
    if (current != null) {
        val startMarker = Marker(map).apply {
            position = GeoPoint(current.latitude, current.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Position actuelle"
            icon = map.context.getDrawable(android.R.drawable.ic_menu_mylocation)
        }
        map.overlays.add(startMarker)
    }

    if (dest != null) {
        val destMarker = Marker(map).apply {
            position = GeoPoint(dest.latitude, dest.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = ride.arrival
            icon = map.context.getDrawable(android.R.drawable.ic_menu_compass)
        }
        map.overlays.add(destMarker)
    }
}

private fun addDestinationMarker(map: MapView, dest: Location, markerTitle: String) {
    val marker = Marker(map).apply {
        position = GeoPoint(dest.latitude, dest.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = markerTitle
        icon = map.context.getDrawable(android.R.drawable.ic_menu_compass)
    }
    map.overlays.add(marker)
    map.invalidate()
}

private fun updateMapPosition(map: MapView?, current: Location, dest: Location?, ride: RideRequest) {
    map?.let { mv ->
        mv.controller.setCenter(GeoPoint(current.latitude, current.longitude))

        if (dest != null) {
            val existingMarkers = mv.overlays.filterIsInstance<Marker>().filter { it.title == ride.arrival }
            if (existingMarkers.isEmpty()) {
                addDestinationMarker(mv, dest, ride.arrival)
            }
        }
        mv.invalidate()
    }
}

private fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, onLocation: (Location) -> Unit) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
        setMinUpdateIntervalMillis(2000L)
    }.build()

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onLocation)
            }
        }, Looper.getMainLooper())
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
