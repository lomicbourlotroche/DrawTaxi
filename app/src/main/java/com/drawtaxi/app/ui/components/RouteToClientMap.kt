package com.drawtaxi.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import com.drawtaxi.app.logic.routing.fetchRoute
import com.drawtaxi.app.logic.routing.RouteInfo
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
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
    var isMapReady by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context)
    }

    LaunchedEffect(pickupAddress) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        lastKnown?.let {
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

    LaunchedEffect(isMapReady, routePoints, currentLocation, pickupLocation) {
        if (!isMapReady || mapInstance == null) return@LaunchedEffect
        val map = mapInstance!!

        map.clear()

        currentLocation?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle("Ma position"))
        }

        pickupLocation?.let {
            map.addMarker(MarkerOptions().setPosition(it).setTitle("Point de rendez-vous"))
        }

        if (routePoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(android.graphics.Color.rgb(76, 175, 80))
                    .width(10f)
            )

            val allPoints = routePoints.toMutableList()
            currentLocation?.let { allPoints.add(it) }
            pickupLocation?.let { allPoints.add(it) }
            if (allPoints.isNotEmpty()) {
                val midLat = allPoints.map { it.latitude }.let { (it.min() + it.max()) / 2.0 }
                val midLng = allPoints.map { it.longitude }.let { (it.min() + it.max()) / 2.0 }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 12.0))
            }
        } else if (currentLocation != null && pickupLocation != null) {
            map.addPolyline(
                PolylineOptions()
                    .add(currentLocation!!)
                    .add(pickupLocation!!)
                    .color(android.graphics.Color.rgb(255, 152, 0))
                    .width(6f)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
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
