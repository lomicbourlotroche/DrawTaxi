package com.drawtaxi.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val EMPTY_FC = """{"type":"FeatureCollection","features":[]}"""

@Composable
fun RouteToClientMap(
    pickupAddress: String,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var pickupLocation by remember { mutableStateOf<Location?>(null) }
    var routePositions by remember { mutableStateOf<List<Position>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var durationMin by remember { mutableStateOf<Int?>(null) }
    val cameraState = rememberCameraState()

    LaunchedEffect(pickupAddress) {
        withContext(Dispatchers.IO) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    lastKnown?.let { currentLocation = it }
                }

                if (pickupAddress.isNotBlank()) {
                    GeocodingService.geocode(pickupAddress)?.let { pickupLocation = it }
                }

                val cur = currentLocation
                val pick = pickupLocation
                if (cur != null && pick != null) {
                    val route = NavigationEngine.fetchRoute(
                        fromLat = cur.latitude, fromLng = cur.longitude,
                        toLat = pick.latitude, toLng = pick.longitude
                    )
                    if (route != null) {
                        val points = mutableListOf<Position>()
                        val decoded = NavigationEngine.fetchRouteGeometry(
                            fromLat = cur.latitude, fromLng = cur.longitude,
                            toLat = pick.latitude, toLng = pick.longitude
                        )
                        routePositions = decoded.map { Position(latitude = it.first, longitude = it.second) }
                        distanceKm = route.distance / 1000.0
                        durationMin = (route.duration / 60).toInt()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
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
                                "${String.format("%.1f", distanceKm)} km • ~$durationMin min",
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
                    MaplibreMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraState = cameraState,
                        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                        options = MapOptions(
                            ornamentOptions = OrnamentOptions(
                                isLogoEnabled = false,
                                isAttributionEnabled = false,
                                isCompassEnabled = false
                            ),
                            gestureOptions = GestureOptions.AllDisabled
                        )
                    ) {
                        val routeSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(EMPTY_FC),
                            options = GeoJsonOptions(synchronousUpdate = true)
                        )
                        val vousSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(EMPTY_FC),
                            options = GeoJsonOptions(synchronousUpdate = true)
                        )
                        val pickupSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(EMPTY_FC),
                            options = GeoJsonOptions(synchronousUpdate = true)
                        )

                        LaunchedEffect(routePositions) {
                            val coords = routePositions.joinToString(",") { "[${it.longitude},${it.latitude}]" }
                            val json = if (coords.isNotEmpty()) {
                                """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
                            } else EMPTY_FC
                            routeSource.setData(GeoJsonData.JsonString(json))
                        }

                        LaunchedEffect(currentLocation) {
                            val loc = currentLocation
                            val json = if (loc != null) {
                                """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""
                            } else EMPTY_FC
                            vousSource.setData(GeoJsonData.JsonString(json))
                        }

                        LaunchedEffect(pickupLocation) {
                            val loc = pickupLocation
                            val json = if (loc != null) {
                                """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""
                            } else EMPTY_FC
                            pickupSource.setData(GeoJsonData.JsonString(json))
                        }

                        if (routePositions.isNotEmpty()) {
                            LineLayer(
                                id = "route",
                                source = routeSource,
                                color = const(Color(0xFF4CAF50)),
                                width = const(6.dp),
                                cap = const(org.maplibre.compose.expressions.value.LineCap.Round),
                                join = const(org.maplibre.compose.expressions.value.LineJoin.Round)
                            )
                        }

                        CircleLayer(
                            id = "vous",
                            source = vousSource,
                            color = const(Color(0xFF2196F3)),
                            radius = const(6.dp),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp)
                        )

                        CircleLayer(
                            id = "pickup",
                            source = pickupSource,
                            color = const(Color(0xFF4CAF50)),
                            radius = const(8.dp),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp)
                        )

                        LaunchedEffect(routePositions, currentLocation, pickupLocation) {
                            val allPositions = mutableListOf<Position>()
                            allPositions.addAll(routePositions)
                            currentLocation?.let {
                                allPositions.add(Position(latitude = it.latitude, longitude = it.longitude))
                            }
                            pickupLocation?.let {
                                allPositions.add(Position(latitude = it.latitude, longitude = it.longitude))
                            }
                            if (allPositions.isNotEmpty()) {
                                val lats = allPositions.map { it.latitude }
                                val lngs = allPositions.map { it.longitude }
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = Position(
                                            longitude = (lngs.min() + lngs.max()) / 2.0,
                                            latitude = (lats.min() + lats.max()) / 2.0
                                        ),
                                        zoom = 12.0
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Position non disponible", color = Slate500)
                    }
                }
            }
        }
    }
}
