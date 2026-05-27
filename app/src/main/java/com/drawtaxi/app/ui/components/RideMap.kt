package com.drawtaxi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.Slate100
import com.drawtaxi.app.ui.theme.Slate400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
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
fun RideMap(
    departure: String,
    arrival: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var depPosition by remember { mutableStateOf<Position?>(null) }
    var arrPosition by remember { mutableStateOf<Position?>(null) }
    var routePoints by remember { mutableStateOf<List<Position>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val cameraState = rememberCameraState()

    LaunchedEffect(departure, arrival) {
        withContext(Dispatchers.IO) {
            try {
                val fromLocation = GeocodingService.geocode(departure)
                val toLocation = GeocodingService.geocode(arrival)

                if (fromLocation != null && toLocation != null) {
                    val start = Position(latitude = fromLocation.latitude, longitude = fromLocation.longitude)
                    val end = Position(latitude = toLocation.latitude, longitude = toLocation.longitude)
                    depPosition = start
                    arrPosition = end

                    val geometry = NavigationEngine.fetchRouteGeometry(
                        fromLat = fromLocation.latitude, fromLng = fromLocation.longitude,
                        toLat = toLocation.latitude, toLng = toLocation.longitude
                    )

                    if (geometry.isNotEmpty()) {
                        routePoints = geometry.map { Position(latitude = it.first, longitude = it.second) }
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

    Box(modifier = modifier) {
        MaplibreMap(
            modifier = Modifier.matchParentSize(),
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
            val depSource = rememberGeoJsonSource(
                data = GeoJsonData.JsonString(EMPTY_FC),
                options = GeoJsonOptions(synchronousUpdate = true)
            )
            val arrSource = rememberGeoJsonSource(
                data = GeoJsonData.JsonString(EMPTY_FC),
                options = GeoJsonOptions(synchronousUpdate = true)
            )

            LaunchedEffect(routePoints) {
                val coords = routePoints.joinToString(",") { "[${it.longitude},${it.latitude}]" }
                val json = if (coords.isNotEmpty()) {
                    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
                } else EMPTY_FC
                routeSource.setData(GeoJsonData.JsonString(json))
            }

            LaunchedEffect(depPosition) {
                val pos = depPosition
                val json = if (pos != null) {
                    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${pos.longitude},${pos.latitude}]}}]}"""
                } else EMPTY_FC
                depSource.setData(GeoJsonData.JsonString(json))
            }

            LaunchedEffect(arrPosition) {
                val pos = arrPosition
                val json = if (pos != null) {
                    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${pos.longitude},${pos.latitude}]}}]}"""
                } else EMPTY_FC
                arrSource.setData(GeoJsonData.JsonString(json))
            }

            LineLayer(
                id = "route",
                source = routeSource,
                color = const(Color(0xFF2196F3)),
                width = const(8.dp),
                cap = const(org.maplibre.compose.expressions.value.LineCap.Round),
                join = const(org.maplibre.compose.expressions.value.LineJoin.Round)
            )

            CircleLayer(
                id = "departure",
                source = depSource,
                color = const(Color(0xFF10B981)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )

            CircleLayer(
                id = "arrival",
                source = arrSource,
                color = const(Color(0xFFF43F5E)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )

            LaunchedEffect(routePoints) {
                if (routePoints.isNotEmpty()) {
                    val lats = routePoints.map { it.latitude }
                    val lngs = routePoints.map { it.longitude }
                    val centerLat = (lats.min() + lats.max()) / 2.0
                    val centerLng = (lngs.min() + lngs.max()) / 2.0
                    cameraState.animateTo(
                        CameraPosition(
                            target = Position(longitude = centerLng, latitude = centerLat),
                            zoom = 12.0
                        )
                    )
                }
            }
        }

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

@Preview(showBackground = true)
@Composable
fun RideMapPreview() {
    DrawTaxiTheme {
        // Placeholder statique — MapLibre nécessite un contexte Android réel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate100),
            contentAlignment = Alignment.Center
        ) {
            Text("Carte : Paris → Lyon", color = Slate400)
        }
    }
}
