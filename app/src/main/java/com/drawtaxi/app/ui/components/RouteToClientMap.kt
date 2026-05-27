package com.drawtaxi.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
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
    modifier: Modifier = Modifier,
    onNavigateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Trajet vers le client",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        if (distanceKm != null && durationMin != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Slate100, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${String.format(Locale.getDefault(), "%.1f", distanceKm)} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.size(3.dp).background(Slate300, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "~$durationMin min",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(brandColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Naviguer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                            )
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
                            color = const(brandColor),
                            radius = const(8.dp),
                            strokeColor = const(Color.White),
                            strokeWidth = const(3.dp)
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
                                val dynamicZoom = NavigationEngine.calculateZoomPositions(allPositions)
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = Position(
                                            longitude = (lngs.min() + lngs.max()) / 2.0,
                                            latitude = (lats.min() + lats.max()) / 2.0
                                        ),
                                        zoom = dynamicZoom
                                    )
                                )
                            }
                        }
                    }

                    // Floating Recenter Button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
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
                                        val dynamicZoom = NavigationEngine.calculateZoomPositions(allPositions)
                                        cameraState.animateTo(
                                            CameraPosition(
                                                target = Position(
                                                    longitude = (lngs.min() + lngs.max()) / 2.0,
                                                    latitude = (lats.min() + lats.max()) / 2.0
                                                ),
                                                zoom = dynamicZoom
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recentrer",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
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

@Preview(showBackground = true)
@Composable
fun RouteToClientMapPreview() {
    DrawTaxiTheme {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Indigo500.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Indigo500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Trajet vers le client",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Slate100, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "3.2 km",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(modifier = Modifier.size(3.dp).background(Slate300, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "~8 min",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo500
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Indigo500, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = "Naviguer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Carte MapLibre", color = Slate500, style = MaterialTheme.typography.bodyMedium)

                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recentrer",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
