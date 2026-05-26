package com.drawtaxi.app.ui.screens.rides

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.Amber500
import com.drawtaxi.app.ui.theme.Slate400
import com.drawtaxi.app.ui.theme.Slate500
import com.drawtaxi.app.ui.theme.Slate700
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
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
fun InstructionCard(
    instruction: String,
    nextInstruction: String,
    nextDistance: String,
    isRecalculating: Boolean,
    eta: String,
    distance: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        instruction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (nextInstruction.isNotBlank()) {
                        Text(
                            "Puis $nextInstruction dans $nextDistance",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            }

            if (isRecalculating) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recalcul de l'itinéraire...", style = MaterialTheme.typography.bodySmall, color = Amber500)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distance", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(distance, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ETA", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(eta, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ReturnHomeScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var homeLocation by remember { mutableStateOf<Location?>(null) }
    var hasGeocoded by remember { mutableStateOf(false) }

    val engine = remember {
        NavigationEngine(context, scope)
    }
    val engineState by engine.state.collectAsState()

    val cameraState = rememberCameraState()
    val routeKey = remember(engineState.routePoints) { engineState.routePoints.size }

    DisposableEffect(Unit) {
        onDispose { engine.destroy() }
    }

    LaunchedEffect(settings.homeAddress) {
        if (settings.homeAddress.isNotBlank() && !hasGeocoded) {
            homeLocation = GeocodingService.geocode(settings.homeAddress)
            hasGeocoded = true
        }
    }

    LaunchedEffect(engineState.currentLocation, homeLocation) {
        if (engineState.currentLocation != null && homeLocation != null && !engineState.isNavigating) {
            val route = NavigationEngine.fetchRoute(
                fromLat = engineState.currentLocation!!.latitude,
                fromLng = engineState.currentLocation!!.longitude,
                toLat = homeLocation!!.latitude,
                toLng = homeLocation!!.longitude
            )
            if (route != null) {
                engine.startNavigation(route)
            }
        }
    }

    LaunchedEffect(routeKey, engineState.currentLocation, homeLocation) {
        val allPoints = mutableListOf<Pair<Double, Double>>()
        allPoints.addAll(engineState.routePoints)
        engineState.currentLocation?.let { allPoints.add(it.latitude to it.longitude) }
        homeLocation?.let { allPoints.add(it.latitude to it.longitude) }

        if (allPoints.isNotEmpty()) {
            val minLat = allPoints.minOf { it.first }
            val maxLat = allPoints.maxOf { it.first }
            val minLng = allPoints.minOf { it.second }
            val maxLng = allPoints.maxOf { it.second }
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        longitude = (minLng + maxLng) / 2.0,
                        latitude = (minLat + maxLat) / 2.0
                    ),
                    zoom = 12.0
                )
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(engineState.currentSpeed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                val homeSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(EMPTY_FC),
                    options = GeoJsonOptions(synchronousUpdate = true)
                )

                LaunchedEffect(routeKey) {
                    val coords = engineState.routePoints.joinToString(",") { "[${it.second},${it.first}]" }
                    val json = if (coords.isNotEmpty()) {
                        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
                    } else EMPTY_FC
                    routeSource.setData(GeoJsonData.JsonString(json))
                }

                LaunchedEffect(engineState.currentLocation) {
                    val loc = engineState.currentLocation ?: return@LaunchedEffect
                    val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]}}]}"""
                    vousSource.setData(GeoJsonData.JsonString(json))
                }

                LaunchedEffect(homeLocation) {
                    if (homeLocation != null) {
                        val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${homeLocation!!.longitude},${homeLocation!!.latitude}]}}]}"""
                        homeSource.setData(GeoJsonData.JsonString(json))
                    } else {
                        homeSource.setData(GeoJsonData.JsonString(EMPTY_FC))
                    }
                }

                LineLayer(
                    id = "route",
                    source = routeSource,
                    color = const(Color(0xFF10B981)),
                    width = const(8.dp),
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round)
                )

                CircleLayer(
                    id = "vous",
                    source = vousSource,
                    color = const(Color(0xFF2196F3)),
                    radius = const(10.dp),
                    strokeColor = const(Color.White),
                    strokeWidth = const(3.dp)
                )

                CircleLayer(
                    id = "home",
                    source = homeSource,
                    color = const(Color(0xFF10B981)),
                    radius = const(12.dp),
                    strokeColor = const(Color.White),
                    strokeWidth = const(3.dp)
                )
            }

            AnimatedVisibility(
                visible = engineState.currentInstruction.isNotBlank(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp)
            ) {
                InstructionCard(
                    instruction = engineState.currentInstruction,
                    nextInstruction = engineState.nextInstruction,
                    nextDistance = engineState.nextInstructionDistance,
                    isRecalculating = false,
                    eta = engineState.etaText,
                    distance = engineState.distanceText
                )
            }

            if (engineState.isCalculating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Calcul...", color = Color.White)
                    }
                }
            }
        }
    }
}
