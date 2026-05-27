package com.drawtaxi.app.logic.routing

import java.util.Locale
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.spatialk.geojson.Position
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

        val decoded = parseGeometry(route.geometry)
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
            val decoded = parseGeometry(newRoute.geometry)
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

        private fun fetchJson(url: String, method: String = "GET", body: String? = null): String? {
            var connection: HttpURLConnection? = null
            return try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "DrawTaxi/1.0")
                if (body != null) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.doOutput = true
                    connection.outputStream.write(body.toByteArray(Charset.forName("UTF-8")))
                }
                val code = connection.responseCode
                Log.d(TAG, "HTTP $code $method for ${url.take(100)}...")
                if (code in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    reader.readText()
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errBody = reader.readText()
                    Log.e(TAG, "HTTP error $code: ${errBody.take(200)}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "HTTP request failed: ${e.message}")
                null
            } finally {
                try { connection?.disconnect() } catch (_: Exception) { }
            }
        }

        suspend fun fetchRoute(
            fromLat: Double, fromLng: Double,
            toLat: Double, toLng: Double
        ): DirectionsRoute? = withContext(Dispatchers.IO) {
            val url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson&steps=true"
            try {
                Log.d(TAG, "OSRM route request (GET): $url")
                val body = fetchJson(url, "GET") ?: return@withContext null
                val root = JSONObject(body)
                val routes = root.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    Log.e(TAG, "OSRM: no routes in response")
                    return@withContext null
                }

                val routeObj = routes.getJSONObject(0)
                val result = DirectionsRoute.fromJson(routeObj.toString())
                if (result != null) {
                    Log.d(TAG, "OSRM route OK: ${result.distance.toInt()}m, ${result.duration.toInt()}s")
                }
                return@withContext result
            } catch (e: Exception) {
                Log.e(TAG, "OSRM route fetch failed: ${e.message}")
                null
            }
        }

        suspend fun fetchRouteGeometry(
            fromLat: Double, fromLng: Double,
            toLat: Double, toLng: Double
        ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
            val url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson&steps=false"
            try {
                Log.d(TAG, "OSRM geometry request (GET): $url")
                val body = fetchJson(url, "GET") ?: return@withContext emptyList()
                val root = JSONObject(body)
                val routes = root.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    Log.e(TAG, "OSRM geometry: no routes in response")
                    return@withContext emptyList()
                }

                val routeObj = routes.getJSONObject(0)
                val geometry = routeObj.getJSONObject("geometry")
                val coords = geometry.getJSONArray("coordinates")

                val points = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until coords.length()) {
                    val c = coords.getJSONArray(i)
                    points.add(Pair(c.getDouble(1), c.getDouble(0)))
                }
                Log.d(TAG, "OSRM geometry OK: ${points.size} points")
                points
            } catch (e: Exception) {
                Log.e(TAG, "OSRM geometry fetch failed: ${e.message}")
                emptyList()
            }
        }

        fun parseGeometry(geo: String): List<Pair<Double, Double>> {
            return try {
                val json = JSONObject(geo)
                val coordsArray = json.getJSONArray("coordinates")
                val points = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until coordsArray.length()) {
                    val coord = coordsArray.getJSONArray(i)
                    points.add(Pair(coord.getDouble(1), coord.getDouble(0)))
                }
                points
            } catch (e: Exception) {
                decodePolyline(geo)
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

        fun calculateZoom(points: List<Pair<Double, Double>>): Double {
            if (points.isEmpty()) return 13.0
            val lats = points.map { it.first }
            val lngs = points.map { it.second }
            val minLat = lats.minOrNull() ?: return 13.0
            val maxLat = lats.maxOrNull() ?: return 13.0
            val minLng = lngs.minOrNull() ?: return 13.0
            val maxLng = lngs.maxOrNull() ?: return 13.0

            val latSpan = maxLat - minLat
            val lngSpan = maxLng - minLng
            val maxSpan = maxOf(latSpan, lngSpan)
            if (maxSpan <= 0.0) return 15.0
            
            // log2(360.0 / (maxSpan / 0.6))
            val rawZoom = Math.log(360.0 / (maxSpan / 0.6)) / Math.log(2.0)
            return rawZoom.coerceIn(4.0, 16.0)
        }

        fun calculateZoomPositions(positions: List<Position>): Double {
            if (positions.isEmpty()) return 13.0
            val lats = positions.map { it.latitude }
            val lngs = positions.map { it.longitude }
            val minLat = lats.minOrNull() ?: return 13.0
            val maxLat = lats.maxOrNull() ?: return 13.0
            val minLng = lngs.minOrNull() ?: return 13.0
            val maxLng = lngs.maxOrNull() ?: return 13.0

            val latSpan = maxLat - minLat
            val lngSpan = maxLng - minLng
            val maxSpan = maxOf(latSpan, lngSpan)
            if (maxSpan <= 0.0) return 15.0
            
            // log2(360.0 / (maxSpan / 0.6))
            val rawZoom = Math.log(360.0 / (maxSpan / 0.6)) / Math.log(2.0)
            return rawZoom.coerceIn(4.0, 16.0)
        }
    }
}