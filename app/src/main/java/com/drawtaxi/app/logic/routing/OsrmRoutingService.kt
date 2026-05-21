package com.drawtaxi.app.logic.routing

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OsrmRoutingService {

    private const val TAG = "OsrmRoutingService"
    private const val GOOGLE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json"
    var apiKey: String = "YOUR_GOOGLE_MAPS_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class RouteResult(
        val success: Boolean,
        val distance: Double,
        val duration: Double,
        val geometry: List<Pair<Double, Double>>,
        val legs: List<RouteLeg>,
        val errorMessage: String? = null
    )

    data class RouteLeg(
        val distance: Double,
        val duration: Double,
        val steps: List<RouteStep>
    )

    data class RouteStep(
        val instruction: String,
        val distance: Double,
        val duration: Double,
        val maneuver: String,
        val name: String
    )

    suspend fun calculateRoute(
        from: Location,
        to: Location
    ): RouteResult = withContext(Dispatchers.IO) {
        try {
            val origin = "${from.latitude},${from.longitude}"
            val destination = "${to.latitude},${to.longitude}"
            val url = "$GOOGLE_DIRECTIONS_URL?origin=$origin&destination=$destination&key=$apiKey&language=fr&units=metric"

            Log.d(TAG, "Requête Directions API : $url")

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur Directions API : ${response.code}")
                return@withContext RouteResult(
                    success = false, distance = 0.0, duration = 0.0,
                    geometry = emptyList(), legs = emptyList(),
                    errorMessage = "Erreur serveur : ${response.code}"
                )
            }

            val jsonResponse = response.body?.string() ?: ""
            val json = JSONObject(jsonResponse)

            val status = json.optString("status", "")
            if (status != "OK") {
                val errorMsg = json.optString("error_message", "Erreur inconnue")
                Log.e(TAG, "Erreur Directions API : $status - $errorMsg")
                return@withContext RouteResult(
                    success = false, distance = 0.0, duration = 0.0,
                    geometry = emptyList(), legs = emptyList(),
                    errorMessage = errorMsg
                )
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext RouteResult(
                    success = false, distance = 0.0, duration = 0.0,
                    geometry = emptyList(), legs = emptyList(),
                    errorMessage = "Aucun itinéraire trouvé"
                )
            }

            val route = routes.getJSONObject(0)

            val overviewPolyline = route.getJSONObject("overview_polyline")
            val encodedPoints = overviewPolyline.getString("points")
            val geometry = decodePolyline(encodedPoints)

            val legs = mutableListOf<RouteLeg>()
            val legsArray = route.getJSONArray("legs")
            var totalDistance = 0.0
            var totalDuration = 0.0

            for (i in 0 until legsArray.length()) {
                val legObj = legsArray.getJSONObject(i)
                val legDist = legObj.getJSONObject("distance").getDouble("value")
                val legDur = legObj.getJSONObject("duration").getDouble("value")
                totalDistance += legDist
                totalDuration += legDur

                val steps = mutableListOf<RouteStep>()
                val stepsArray = legObj.getJSONArray("steps")

                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    val stepDist = stepObj.getJSONObject("distance").getDouble("value")
                    val stepDur = stepObj.getJSONObject("duration").getDouble("value")
                    val htmlInstructions = stepObj.optString("html_instructions", "")
                    val plainInstruction = android.text.Html.fromHtml(htmlInstructions, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                    val maneuver = stepObj.getJSONObject("maneuver").optString("maneuver", "straight")

                    val name = stepObj.optString("street_name", "")
                        .ifBlank { stepObj.optString("name", "") }

                    steps.add(RouteStep(
                        instruction = plainInstruction,
                        distance = stepDist,
                        duration = stepDur,
                        maneuver = normalizeManeuver(maneuver),
                        name = name
                    ))
                }

                legs.add(RouteLeg(distance = legDist, duration = legDur, steps = steps))
            }

            Log.d(TAG, "Itinéraire calculé : ${totalDistance / 1000} km, ${totalDuration / 60} min, ${geometry.size} points")

            RouteResult(
                success = true, distance = totalDistance, duration = totalDuration,
                geometry = geometry, legs = legs
            )

        } catch (e: Exception) {
            Log.e(TAG, "Erreur calcul itinéraire", e)
            RouteResult(
                success = false, distance = 0.0, duration = 0.0,
                geometry = emptyList(), legs = emptyList(),
                errorMessage = "Erreur réseau : ${e.message}"
            )
        }
    }

    suspend fun calculateRouteFromCurrentPosition(
        currentLocation: Location,
        destinationAddress: String
    ): RouteResult {
        val destLocation = com.drawtaxi.app.logic.geocoding.GeocodingService.geocode(destinationAddress)
            ?: return RouteResult(
                success = false, distance = 0.0, duration = 0.0,
                geometry = emptyList(), legs = emptyList(),
                errorMessage = "Adresse de destination introuvable"
            )
        return calculateRoute(currentLocation, destLocation)
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
            meters >= 1000 -> String.format("%.1f km", meters / 1000)
            else -> String.format("%.0f m", meters)
        }
    }

    fun getManeuverInstruction(maneuver: String, name: String = ""): String {
        return when (maneuver.lowercase()) {
            "turn-left" -> "Tournez à gauche"
            "turn-right" -> "Tournez à droite"
            "turn-slight-left" -> "Tournez légèrement à gauche"
            "turn-slight-right" -> "Tournez légèrement à droite"
            "turn-sharp-left" -> "Tournez fortement à gauche"
            "turn-sharp-right" -> "Tournez fortement à droite"
            "straight" -> "Continuez tout droit"
            "uturn" -> "Faites demi-tour"
            "ramp-left" -> "Prenez la rampe à gauche"
            "ramp-right" -> "Prenez la rampe à droite"
            "merge" -> "Insérez-vous"
            "fork-left" -> "Bifurquez à gauche"
            "fork-right" -> "Bifurquez à droite"
            "roundabout-left" -> "Prenez le rond-point"
            "roundabout-right" -> "Prenez le rond-point"
            "roundabout" -> "Prenez le rond-point"
            "depart" -> "Départ"
            "arrive" -> "Arrivée"
            "end" -> "Arrivée"
            "continue" -> "Continuez"
            "turn" -> "Tournez"
            else -> "Continuez"
        } + if (name.isNotBlank()) " sur $name" else ""
    }

    private fun normalizeManeuver(googleManeuver: String): String {
        return when (googleManeuver) {
            "turn-slight-left" -> "turn"
            "turn-slight-right" -> "turn"
            "turn-sharp-left" -> "turn"
            "turn-sharp-right" -> "turn"
            "uturn-left", "uturn-right" -> "uturn"
            "ramp-left", "ramp-right" -> "on ramp"
            "fork-left", "fork-right" -> "fork"
            "roundabout-left", "roundabout-right" -> "roundabout"
            else -> googleManeuver
        }
    }

    private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            points.add(Pair(lat / 1e5, lng / 1e5))
        }

        return points
    }
}
