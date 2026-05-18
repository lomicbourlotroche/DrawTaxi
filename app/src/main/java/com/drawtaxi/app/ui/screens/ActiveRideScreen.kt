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
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.OsrmRoutingService
import com.drawtaxi.app.ui.components.NavigationMapView
import com.drawtaxi.app.ui.theme.*
import com.google.android.gms.location.*
import com.mapbox.mapboxsdk.Mapbox

// Étapes de navigation simplifiées
enum class NavigationStep(val label: String, val description: String) {
    TO_PICKUP("Aller chercher le client", "Navigation vers le point de départ"),
    TO_DESTINATION("Aller à destination", "Conduire le client"),
    CHOICE("Que faire ?", "Destination atteinte"),
    TO_HOME("Retour à domicile", "Navigation vers votre adresse"),
    COMPLETE("Terminer", "Récapitulatif de la course")
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
    var currentStep by remember { mutableStateOf(NavigationStep.TO_PICKUP) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var pickupLocation by remember { mutableStateOf<Location?>(null) }
    var destLocation by remember { mutableStateOf<Location?>(null) }
    var homeLocation by remember { mutableStateOf<Location?>(null) }
    var remainingDistance by remember { mutableStateOf(0.0) }
    var eta by remember { mutableStateOf("--") }
    var speed by remember { mutableStateOf("0 km/h") }
    var rideStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Géocodage des adresses
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(context, fusedLocationClient) { location ->
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

    // Calcul de l'itinéraire selon l'étape
    LaunchedEffect(currentStep, currentLocation, pickupLocation, destLocation, homeLocation) {
        val start = currentLocation
        val end = when (currentStep) {
            NavigationStep.TO_PICKUP -> pickupLocation
            NavigationStep.TO_DESTINATION -> destLocation
            NavigationStep.TO_HOME -> homeLocation
            else -> null
        }

        // Les distances et ETA seront calculés par le NavigationMapView via OSRM
        // On met juste à jour les stats de base
    }

    // Mise à jour position et stats
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            speed = if (loc.hasSpeed()) {
                String.format("%.0f km/h", loc.speed * 3.6)
            } else "0 km/h"
        }
    }

    // Initialiser Mapbox (MapLibre)
    LaunchedEffect(Unit) {
        Mapbox.getInstance(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Carte de navigation avec vrai itinéraire OSRM
        when (currentStep) {
            NavigationStep.TO_PICKUP -> {
                if (pickupLocation != null) {
                    NavigationMapView(
                        departure = "Ma position",
                        arrival = ride.departure,
                        brandColor = brandColor,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = brandColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calcul de l'itinéraire...", color = Slate700)
                        }
                    }
                }
            }
            NavigationStep.TO_DESTINATION -> {
                if (destLocation != null) {
                    NavigationMapView(
                        departure = ride.departure,
                        arrival = ride.arrival,
                        brandColor = brandColor,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = brandColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calcul de l'itinéraire...", color = Slate700)
                        }
                    }
                }
            }
            else -> {
                // Pour les autres étapes, afficher simplement la carte avec le dernier itinéraire
                NavigationMapView(
                    departure = ride.departure,
                    arrival = ride.arrival,
                    brandColor = brandColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Header overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = currentStep.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                FilledIconButton(
                    onClick = { showCancelDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Annuler", tint = Rose500)
                }
            }
        }

        // Overlay stats en bas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Carte d'info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(value = eta, label = "ETA")
                        StatItem(value = String.format("%.1f km", remainingDistance / 1000.0), label = "Distance")
                        StatItem(value = speed, label = "Vitesse")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bouton d'action principal selon l'étape
                    when (currentStep) {
                        NavigationStep.TO_PICKUP -> {
                            Button(
                                onClick = {
                                    totalDistance += remainingDistance
                                    currentStep = NavigationStep.TO_DESTINATION
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Client récupéré", fontWeight = FontWeight.Bold)
                            }
                        }
                        NavigationStep.TO_DESTINATION -> {
                            Button(
                                onClick = {
                                    totalDistance += remainingDistance
                                    currentStep = NavigationStep.CHOICE
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Destination atteinte", fontWeight = FontWeight.Bold)
                            }
                        }
                        NavigationStep.CHOICE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (homeLocation != null) {
                                    OutlinedButton(
                                        onClick = {
                                            currentStep = NavigationStep.TO_HOME
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rentrer", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = {
                                        currentStep = NavigationStep.COMPLETE
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terminer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        NavigationStep.TO_HOME -> {
                            Button(
                                onClick = {
                                    totalDistance += remainingDistance
                                    currentStep = NavigationStep.COMPLETE
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Green500)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terminer la course", fontWeight = FontWeight.Bold)
                            }
                        }
                        NavigationStep.COMPLETE -> {
                            val duration = ((System.currentTimeMillis() - rideStartTime) / 60000).toInt()
                            val finalDistance = (ride.distanceKm * 1000 + totalDistance) / 1000.0

                            Column {
                                Text(
                                    "Récapitulatif",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Distance totale:", color = Slate600)
                                    Text(String.format("%.1f km", finalDistance), fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Durée:", color = Slate600)
                                    Text("$duration min", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val completedRide = ride.copy(
                                            distanceKm = finalDistance,
                                            durationMinutes = duration.coerceAtLeast(1),
                                            status = RideStatus.COMPLETED,
                                            isPending = false
                                        )
                                        onComplete(completedRide)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sauvegarder & Fermer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annuler la course ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Oui, annuler")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Non")
                }
            }
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate700
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate500
        )
    }
}

private fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, onLocation: (Location) -> Unit) {
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
