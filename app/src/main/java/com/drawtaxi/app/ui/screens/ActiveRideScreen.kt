package com.drawtaxi.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.logic.OsmRoute
import com.drawtaxi.app.logic.OsmRoutingService
import com.drawtaxi.app.logic.RouteStep
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import kotlin.math.*

enum class RideStage(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String) {
    TO_PICKUP("Aller au point de départ", Icons.Default.LocationOn, "Rejoignez le client"),
    TO_DESTINATION("Aller à la destination", Icons.Default.Place, "Conduisez le client"),
    TO_HOME("Rentrer chez soi", Icons.Default.Home, "Retournez à votre adresse"),
    COMPLETED("Course terminée", Icons.Default.CheckCircle, "Sauvegardé pour facturation")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRideScreen(
    ride: RideRequest,
    settings: AppSettings,
    brandColor: Color,
    onBack: () -> Unit,
    onComplete: (RideRequest) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var pickupLocation by remember { mutableStateOf<Location?>(null) }
    var destLocation by remember { mutableStateOf<Location?>(null) }
    var homeLocation by remember { mutableStateOf<Location?>(null) }
    var currentStage by remember { mutableStateOf(RideStage.TO_PICKUP) }
    var currentRoute by remember { mutableStateOf<OsmRoute?>(null) }
    var currentStep by remember { mutableStateOf<RouteStep?>(null) }
    var nextStep by remember { mutableStateOf<RouteStep?>(null) }
    var remainingDistance by remember { mutableStateOf(0.0) }
    var eta by remember { mutableStateOf("--") }
    var speed by remember { mutableStateOf("0 km/h") }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var showCallMenu by remember { mutableStateOf(false) }
    var showStageComplete by remember { mutableStateOf(false) }
    var rideStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        rideStartTime = System.currentTimeMillis()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startActiveRideLocationUpdates(context, fusedLocationClient) { location ->
                currentLocation = location
            }
        }

        if (ride.departure.isNotBlank()) {
            pickupLocation = com.drawtaxi.app.logic.GeocodingService.geocode(ride.departure)
        }
        if (ride.arrival.isNotBlank()) {
            destLocation = com.drawtaxi.app.logic.GeocodingService.geocode(ride.arrival)
        }
        if (settings.homeAddress.isNotBlank()) {
            homeLocation = com.drawtaxi.app.logic.GeocodingService.geocode(settings.homeAddress)
        }
    }

    LaunchedEffect(currentStage, currentLocation, pickupLocation, destLocation, homeLocation) {
        val start = currentLocation
        val end = when (currentStage) {
            RideStage.TO_PICKUP -> pickupLocation
            RideStage.TO_DESTINATION -> destLocation
            RideStage.TO_HOME -> homeLocation
            RideStage.COMPLETED -> null
        }

        if (start != null && end != null) {
            isLoadingRoute = true
            val route = OsmRoutingService.getRoute(start, end)
            currentRoute = route
            isLoadingRoute = false

            if (route != null) {
                remainingDistance = route.distance
                val etaMinutes = route.duration / 60.0
                eta = if (etaMinutes < 1) "< 1 min" else "~${etaMinutes.toInt()} min"

                currentStep = route.steps.firstOrNull()
                nextStep = route.steps.getOrNull(1)

                zoomToRoute(mapView, route, start, end)
            }
        }
    }

    LaunchedEffect(currentLocation, currentRoute) {
        currentLocation?.let { loc ->
            speed = if (loc.hasSpeed()) {
                String.format("%.0f km/h", loc.speed * 3.6)
            } else "0 km/h"

            currentRoute?.let { route ->
                val nearestIndex = OsmRoutingService.findNearestRoutePoint(loc, route.geometry)
                val remaining = OsmRoutingService.calculateRemainingRoute(nearestIndex, route.geometry)
                remainingDistance = remaining

                val avgSpeedKmh = if (loc.hasSpeed() && loc.speed > 0) loc.speed * 3.6 else 40.0
                val etaMinutes = (remaining / 1000.0 / avgSpeedKmh) * 60
                eta = if (etaMinutes < 1) "< 1 min" else "~${etaMinutes.toInt()} min"

                val step = OsmRoutingService.getCurrentStep(loc, route.geometry, route.steps)
                if (step != currentStep) {
                    currentStep = step
                    val stepIdx = route.steps.indexOf(step)
                    nextStep = if (stepIdx >= 0) route.steps.getOrNull(stepIdx + 1) else null
                }

                updateMapPosition(mapView, loc)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Slate900)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Navigation", color = Color.White)
                    Text(
                        text = currentStage.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { showCallMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        StageProgressIndicator(
            currentStage = currentStage,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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

                        currentLocation?.let { loc ->
                            controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                        }

                        mapView = this
                    }
                },
                update = { map ->
                    currentRoute?.let { route ->
                        drawRouteOnMap(map, route, currentStage, pickupLocation, destLocation, homeLocation, ride)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Calcul de l'itinéraire...", color = Color.White)
                    } else {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Acquisition GPS...", color = Color.White)
                    }
                }
            }
        }

        if (currentStep != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = brandColor,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (currentStep!!.maneuverType) {
                            "turn" -> if (currentStep!!.maneuverModifier.contains("left")) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward
                            "roundabout", "rotary" -> Icons.AutoMirrored.Filled.RotateRight
                            "arrive" -> Icons.Default.Place
                            else -> Icons.Default.Navigation
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStep!!.displayInstruction,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                        if (nextStep != null) {
                            Text(
                                text = "Puis: ${nextStep!!.displayInstruction}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = String.format("%.0f m", remainingDistance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
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
                    NavStatColumn(value = String.format("%.1f km", remainingDistance / 1000.0), label = "Distance", icon = Icons.Default.Route)
                    NavStatColumn(value = speed, label = "Vitesse", icon = Icons.Default.Speed)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentStage != RideStage.COMPLETED) {
                    Button(
                        onClick = {
                            if (remainingDistance < 500) {
                                advanceStage()
                            } else {
                                showStageComplete = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                    ) {
                        Icon(
                            when (currentStage) {
                                RideStage.TO_PICKUP -> Icons.Default.Person
                                RideStage.TO_DESTINATION -> Icons.Default.Place
                                RideStage.TO_HOME -> Icons.Default.Home
                                RideStage.COMPLETED -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (currentStage) {
                                RideStage.TO_PICKUP -> "Client récupéré"
                                RideStage.TO_DESTINATION -> "Destination atteinte"
                                RideStage.TO_HOME -> "Course terminée"
                                RideStage.COMPLETED -> "Terminé"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val duration = ((System.currentTimeMillis() - rideStartTime) / 60000).toInt()
                            val completedRide = ride.copy(
                                distanceKm = (ride.distanceKm * 1000 + remainingDistance) / 1000.0,
                                durationMinutes = duration.coerceAtLeast(1),
                                status = com.drawtaxi.app.data.RideStatus.COMPLETED,
                                isPending = false
                            )
                            onComplete(completedRide)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green500)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sauvegarder & Terminer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${ride.sender}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Appeler")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${ride.sender}")
                                putExtra("sms_body", settings.arrivalMessageTemplate)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Message")
                    }
                }
            }
        }
    }

    if (showCallMenu) {
        ModalBottomSheet(onDismissRequest = { showCallMenu = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                ActionMenuItem(
                    icon = Icons.Default.Phone,
                    label = "Appeler le client",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${ride.sender}")
                        }
                        context.startActivity(intent)
                        showCallMenu = false
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.Sms,
                    label = "Envoyer un message",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${ride.sender}")
                            putExtra("sms_body", settings.arrivalMessageTemplate)
                        }
                        context.startActivity(intent)
                        showCallMenu = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Annuler la course")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showStageComplete) {
        AlertDialog(
            onDismissRequest = { showStageComplete = false },
            title = { Text("Étape terminée ?") },
            text = { Text("Confirmez-vous avoir atteint la destination ?") },
            confirmButton = {
                Button(
                    onClick = {
                        advanceStage()
                        showStageComplete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStageComplete = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private var _advanceStage: (() -> Unit)? = null

@Composable
private fun rememberAdvanceStage(): () -> Unit {
    var currentStage by remember { mutableStateOf(RideStage.TO_PICKUP) }
    var remainingDistance by remember { mutableStateOf(0.0) }
    var totalDistance by remember { mutableStateOf(0.0) }

    return remember {
        {
            currentStage = when (currentStage) {
                RideStage.TO_PICKUP -> {
                    totalDistance += remainingDistance
                    RideStage.TO_DESTINATION
                }
                RideStage.TO_DESTINATION -> {
                    totalDistance += remainingDistance
                    RideStage.TO_HOME
                }
                RideStage.TO_HOME -> {
                    totalDistance += remainingDistance
                    RideStage.COMPLETED
                }
                RideStage.COMPLETED -> RideStage.COMPLETED
            }
        }
    }
}

private fun advanceStage() {
    _advanceStage?.invoke()
}

@Composable
private fun StageProgressIndicator(currentStage: RideStage, modifier: Modifier = Modifier) {
    val stages = listOf(RideStage.TO_PICKUP, RideStage.TO_DESTINATION, RideStage.TO_HOME, RideStage.COMPLETED)
    val currentIndex = stages.indexOf(currentStage)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, _ ->
            val isActive = index <= currentIndex
            val isCurrent = index == currentIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = when {
                            isCurrent -> Color.White
                            isActive -> Color.White.copy(alpha = 0.5f)
                            else -> Slate600
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun ActionMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Slate700, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun drawRouteOnMap(
    map: MapView,
    route: OsmRoute,
    stage: RideStage,
    pickup: Location?,
    dest: Location?,
    home: Location?,
    ride: RideRequest
) {
    val existingRoutes = map.overlays.filterIsInstance<Polyline>().toList()
    existingRoutes.forEach { map.overlays.remove(it) }

    val existingMarkers = map.overlays.filterIsInstance<Marker>().toList()
    existingMarkers.forEach { map.overlays.remove(it) }

    val polyline = Polyline().apply {
        setPoints(route.geometry.map { GeoPoint(it.first, it.second) })
        outlinePaint.color = Color(0xFF6366F1).toArgb()
        outlinePaint.strokeWidth = 12f
        isGeodesic = true
    }
    map.overlays.add(0, polyline)

    when (stage) {
        RideStage.TO_PICKUP -> {
            pickup?.let { loc ->
                addMarker(map, GeoPoint(loc.latitude, loc.longitude), "Départ: ${ride.departure}", android.R.drawable.ic_menu_compass)
            }
        }
        RideStage.TO_DESTINATION -> {
            dest?.let { loc ->
                addMarker(map, GeoPoint(loc.latitude, loc.longitude), "Destination: ${ride.arrival}", android.R.drawable.ic_menu_compass)
            }
        }
        RideStage.TO_HOME -> {
            home?.let { loc ->
                addMarker(map, GeoPoint(loc.latitude, loc.longitude), "Retour: ${ride.homeAddress}", android.R.drawable.ic_menu_directions)
            }
        }
        RideStage.COMPLETED -> {}
    }

    map.invalidate()
}

private fun addMarker(map: MapView, point: GeoPoint, title: String, iconRes: Int) {
    val marker = Marker(map).apply {
        position = point
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        icon = map.context.getDrawable(iconRes)
    }
    map.overlays.add(marker)
}

private fun zoomToRoute(map: MapView?, route: OsmRoute, start: Location, end: Location) {
    map?.let { mv ->
        val bbox = BoundingBox.fromGeoPoints(
            listOf(
                GeoPoint(start.latitude, start.longitude),
                GeoPoint(end.latitude, end.longitude)
            )
        )
        mv.zoomToBoundingBox(bbox, true, 100)
        mv.invalidate()
    }
}

private fun updateMapPosition(map: MapView?, location: Location) {
    map?.let { mv ->
        mv.controller.setCenter(GeoPoint(location.latitude, location.longitude))
        mv.invalidate()
    }
}

private fun startActiveRideLocationUpdates(context: Context, client: FusedLocationProviderClient, onLocation: (Location) -> Unit) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
        setMinUpdateIntervalMillis(1000L)
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
