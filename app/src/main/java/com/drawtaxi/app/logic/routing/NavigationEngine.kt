package com.drawtaxi.app.logic.routing

import java.util.Locale

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.navigation.core.location.engine.LocationEngine
import org.maplibre.navigation.core.location.engine.LocationEngineProvider
import org.maplibre.navigation.core.location.toAndroidLocation
import org.maplibre.navigation.core.models.DirectionsRoute
import org.maplibre.navigation.core.models.LegStep
import org.maplibre.navigation.core.models.ManeuverModifier
import org.maplibre.navigation.core.models.StepManeuver
import org.maplibre.navigation.core.navigation.MapLibreNavigation
import org.maplibre.navigation.core.navigation.MapLibreNavigationOptions
import org.maplibre.navigation.core.offroute.OffRouteListener
import org.maplibre.navigation.core.routeprogress.ProgressChangeListener
import org.maplibre.navigation.core.routeprogress.RouteProgress
import java.util.concurrent.TimeUnit

data class NavigationEngineState(
    val currentLocation: Location? = null,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val distanceRemaining: Double = 0.0,
    val durationRemaining: Double = 0.0,
    val currentInstruction: String = "",
    val nextInstruction: String = "",
    val nextInstructionDistance: String = "",
    val currentSpeed: String = "0 km/h",
    val etaText: String = "-- min",
    val distanceText: String = "-- km",
    val isNavigating: Boolean = false,
    val isCalculating: Boolean = false,
    val isRecalculating: Boolean = false,
    val error: String? = null
)

class NavigationEngine(
    context: Context,
    private val scope: CoroutineScope
) {
    private val locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context)
    private var maplibreNavigation: MapLibreNavigation? = null

    private var targetLat: Double = 0.0
    private var targetLng: Double = 0.0

    private val _state = MutableStateFlow(NavigationEngineState())
    val state: StateFlow<NavigationEngineState> = _state.asStateFlow()

    private var locationJob: kotlinx.coroutines.Job? = null

    init {
        locationJob = scope.launch {
            locationEngine.listenToLocation(
                LocationEngine.Request(
                    minIntervalMilliseconds = 1000,
                    maxIntervalMilliseconds = 2000,
                    accuracy = LocationEngine.Request.Accuracy.HIGH
                )
            ).collect { navLocation ->
                val android = navLocation.toAndroidLocation()
                val speed = if (android.hasSpeed()) {
                    String.format(Locale.getDefault(), "%.0f km/h", android.speed * 3.6)
                } else "0 km/h"
                _state.value = _state.value.copy(
                    currentLocation = android,
                    currentSpeed = speed
                )
            }
        }
    }

    fun startNavigation(route: DirectionsRoute, toLat: Double? = null, toLng: Double? = null) {
        stopNavigation()
        _state.value = _state.value.copy(isNavigating = true, isCalculating = false, error = null)

        if (toLat != null && toLng != null) {
            targetLat = toLat
            targetLng = toLng
        }

        val decoded = decodePolyline(route.geometry)
        _state.value = _state.value.copy(routePoints = decoded)

        maplibreNavigation = MapLibreNavigation(
            options = MapLibreNavigationOptions(
                defaultMilestonesEnabled = true,
                isDebugLoggingEnabled = false,
                snapToRoute = true,
                enableOffRouteDetection = true
            ),
            locationEngine = locationEngine
        )

        maplibreNavigation!!.addProgressChangeListener(
            ProgressChangeListener { location, routeProgress ->
                updateFromProgress(routeProgress)
            }
        )

        maplibreNavigation!!.addOffRouteListener(OffRouteListener { navLocation ->
            val androidLocation = navLocation.toAndroidLocation()
            Log.d(TAG, "Off-route detected at ${androidLocation.latitude}, ${androidLocation.longitude}")
            scope.launch {
                recalculateRoute(
                    fromLat = androidLocation.latitude,
                    fromLng = androidLocation.longitude,
                    toLat = targetLat,
                    toLng = targetLng
                )
            }
        })

        maplibreNavigation!!.startNavigation(route)
    }

    private suspend fun recalculateRoute(
        fromLat: Double, fromLng: Double,
        toLat: Double, toLng: Double
    ) {
        _state.value = _state.value.copy(isRecalculating = true)
        val newRoute = fetchRoute(fromLat = fromLat, fromLng = fromLng, toLat = toLat, toLng = toLng)
        if (newRoute != null) {
            maplibreNavigation?.stopNavigation()
            val decoded = decodePolyline(newRoute.geometry)
            _state.value = _state.value.copy(routePoints = decoded)
            maplibreNavigation?.startNavigation(newRoute)
        }
        _state.value = _state.value.copy(isRecalculating = false)
    }

    fun stopNavigation() {
        maplibreNavigation?.stopNavigation()
        maplibreNavigation?.onDestroy()
        maplibreNavigation = null
        _state.value = NavigationEngineState(
            currentLocation = _state.value.currentLocation,
            currentSpeed = _state.value.currentSpeed
        )
    }

    fun destroy() {
        stopNavigation()
        locationJob?.cancel()
    }

    private fun updateFromProgress(progress: RouteProgress) {
        val leg = progress.currentLeg
        val step = leg.steps.getOrNull(progress.stepIndex)
        val nextStep = leg.steps.getOrNull(progress.stepIndex + 1)

        _state.value = _state.value.copy(
            distanceRemaining = progress.distanceRemaining,
            durationRemaining = progress.durationRemaining,
            currentInstruction = buildInstruction(step),
            nextInstruction = if (nextStep != null) buildInstruction(nextStep) else "",
            nextInstructionDistance = if (nextStep != null) formatDistance(nextStep.distance) else "",
            etaText = formatDuration(progress.durationRemaining),
            distanceText = formatDistance(progress.distanceRemaining)
        )
    }

    companion object {
        private const val TAG = "NavigationEngine"
        private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving"

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        suspend fun fetchRoute(
            fromLat: Double, fromLng: Double,
            toLat: Double, toLng: Double
        ): DirectionsRoute? = withContext(Dispatchers.IO) {
            try {
                val origin = "$fromLng,$fromLat"
                val destination = "$toLng,$toLat"
                val url = "$OSRM_BASE_URL/$origin;$destination?overview=full&geometries=polyline&steps=true&language=fr&alternatives=false"
                Log.d(TAG, "OSRM request: $url")

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "OSRM error: ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                if (json.optString("code") != "Ok") {
                    Log.e(TAG, "OSRM bad response: ${json.optString("message")}")
                    return@withContext null
                }

                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return@withContext null

                val routeJson = routes.getJSONObject(0).toString()
                DirectionsRoute.fromJson(routeJson)
            } catch (e: Exception) {
                Log.e(TAG, "Route fetch failed", e)
                null
            }
        }

        suspend fun fetchRouteGeometry(
            fromLat: Double, fromLng: Double,
            toLat: Double, toLng: Double
        ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
            try {
                val route = fetchRoute(fromLat, fromLng, toLat, toLng) ?: return@withContext emptyList()
                decodePolyline(route.geometry)
            } catch (e: Exception) {
                Log.e(TAG, "Route geometry fetch failed", e)
                emptyList()
            }
        }

        fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
            val points = mutableListOf<Pair<Double, Double>>()
            var index = 0
            var lat = 0
            var lng = 0
            while (index < encoded.length) {
                var shift = 0
                var result = 0
                var b: Int
                do {
                    b = encoded[index++].code - 63
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) -(result shr 1) else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) -(result shr 1) else result shr 1
                lng += dlng

                points.add(Pair(lat / 1e5, lng / 1e5))
            }
            return points
        }

        fun buildInstruction(step: LegStep?): String {
            if (step == null) return "Continuez"
            val maneuver = step.maneuver
            val type = maneuver.type
            val modifier = maneuver.modifier
            val name = step.name ?: ""

            val base = when (type) {
                StepManeuver.Type.TURN -> "Tournez"
                StepManeuver.Type.NEW_NAME, StepManeuver.Type.CONTINUE -> "Continuez"
                StepManeuver.Type.DEPART -> "Départ"
                StepManeuver.Type.ARRIVE -> "Arrivée"
                StepManeuver.Type.MERGE -> "Insérez-vous"
                StepManeuver.Type.ON_RAMP -> "Prenez la rampe"
                StepManeuver.Type.OFF_RAMP -> "Prenez la sortie"
                StepManeuver.Type.FORK -> "Bifurquez"
                StepManeuver.Type.END_OF_ROAD -> "Fin de route"
                StepManeuver.Type.ROUNDABOUT, StepManeuver.Type.ROTARY -> "Prenez le rond-point"
                StepManeuver.Type.ROUNDABOUT_TURN -> "Prenez le rond-point"
                StepManeuver.Type.EXIT_ROUNDABOUT, StepManeuver.Type.EXIT_ROTARY -> "Sortez du rond-point"
                StepManeuver.Type.NOTIFICATION -> "Attention"
                StepManeuver.Type.USE_LANE -> "Suivez la voie"
                null -> "Continuez"
            }

            val direction = when (modifier) {
                ManeuverModifier.Type.LEFT -> "à gauche"
                ManeuverModifier.Type.RIGHT -> "à droite"
                ManeuverModifier.Type.SLIGHT_LEFT -> "légèrement à gauche"
                ManeuverModifier.Type.SLIGHT_RIGHT -> "légèrement à droite"
                ManeuverModifier.Type.SHARP_LEFT -> "fortement à gauche"
                ManeuverModifier.Type.SHARP_RIGHT -> "fortement à droite"
                ManeuverModifier.Type.STRAIGHT -> "tout droit"
                ManeuverModifier.Type.UTURN -> "demi-tour"
                else -> null
            }

            val suffix = if (name.isNotBlank()) " sur $name" else ""

            return when {
                type == StepManeuver.Type.TURN && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.ON_RAMP && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.OFF_RAMP && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.MERGE && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.FORK && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.ROUNDABOUT && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.ROTARY && direction != null -> "$base $direction$suffix"
                type == StepManeuver.Type.EXIT_ROUNDABOUT && direction != null -> "$base direction$suffix"
                type == StepManeuver.Type.EXIT_ROTARY && direction != null -> "$base $direction$suffix"
                else -> "$base$suffix"
            }
        }

        fun formatDuration(seconds: Double): String {
            val hours = (seconds / 3600).toInt()
            val minutes = ((seconds % 3600) / 60).toInt()
            return when {
                hours > 0 -> "${hours}h ${minutes}min"
                minutes > 0 -> "${minutes} min"
                else -> "< 1 min"
            }
        }

        fun formatDistance(meters: Double): String {
            return when {
                meters >= 1000 -> String.format(Locale.getDefault(), "%.1f km", meters / 1000)
                else -> String.format(Locale.getDefault(), "%.0f m", meters)
            }
        }
    }
}
