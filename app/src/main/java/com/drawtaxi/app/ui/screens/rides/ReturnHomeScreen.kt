package com.drawtaxi.app.ui.screens.rides

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.ui.theme.*
import com.drawtaxi.app.ui.components.core.*
import kotlinx.coroutines.launch
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
    distance: String,
    modifier: Modifier = Modifier
) {
    DrawTaxiSurface(
        shape = RoundedCornerShape(28.dp),
        color = Slate900.copy(alpha = 0.96f),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Emerald500, Emerald700)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        instruction,
                        style = drawTaxiType().titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 28.sp,
                        fontSize = 20.sp
                    )
                    if (nextInstruction.isNotBlank()) {
                        Text(
                            "Puis $nextInstruction dans $nextDistance",
                            style = drawTaxiType().bodyMedium,
                            color = Slate400,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isRecalculating) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Amber500.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Amber500
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Recalcul de l'itinéraire...",
                        style = drawTaxiType().labelSmall,
                        color = Amber500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationStatItem(
                    label = "DISTANCE",
                    value = distance,
                    icon = Icons.Default.Straighten
                )
                Box(modifier = Modifier.size(width = 1.dp, height = 32.dp).background(Slate700))
                NavigationStatItem(
                    label = "ARRIVÉE",
                    value = eta,
                    icon = Icons.Default.Schedule
                )
            }
        }
    }
}

@Composable
private fun NavigationStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                style = drawTaxiType().labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            value,
            style = drawTaxiType().titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReturnHomeScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

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
            homeLocation = GeocodingService.geocode(settings.homeAddress, context)
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
                engine.startNavigation(route, toLat = homeLocation!!.latitude, toLng = homeLocation!!.longitude)
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

    DrawTaxiScaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawTaxiIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                }

                DrawTaxiSurface(
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
                        Text(engineState.currentSpeed, style = drawTaxiType().labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
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
            if (!hasLocationPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Permission de localisation requise",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DrawTaxiSolidButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            containerColor = Indigo500
                        ) {
                            Text("Accorder la permission")
                        }
                    }
                }
            } else {
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
                        isRecalculating = engineState.isRecalculating,
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
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Calcul...", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReturnHomeScreenPreview() {
    val sampleSettings = AppSettings(
        homeAddress = "12 rue de la Paix, Paris"
    )
    DrawTaxiTheme {
        ReturnHomeScreen(
            settings = sampleSettings,
            onBack = {}
        )
    }
}
