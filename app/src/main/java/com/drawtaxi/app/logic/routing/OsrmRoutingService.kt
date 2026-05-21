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
    private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving"

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
            val origin = "${from.longitude},${from.latitude}"
            val destination = "${to.longitude},${to.latitude}"
            val url = "$OSRM_BASE_URL/$origin;$destination?overview=full&geometries=geojson&steps=true&language=fr"

            Log.d(TAG, "Requête OSRM : $url")

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur OSRM : ${response.code}")
                return@withContext RouteResult(
                    success = false, distance = 0.0, duration = 0.0,
                    geometry = emptyList(), legs = emptyList(),
                    errorMessage = "Erreur serveur : ${response.code}"
                )
            }

            val jsonResponse = response.body?.string() ?: ""
            val json = JSONObject(jsonResponse)

            val code = json.optString("code", "")
            if (code != "Ok") {
                val message = json.optString("message", "Erreur inconnue")
                Log.e(TAG, "Erreur OSRM : $code - $message")
                return@withContext RouteResult(
                    success = false, distance = 0.0, duration = 0.0,
                    geometry = emptyList(), legs = emptyList(),
                    errorMessage = message
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

            val geometry = route.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")
            val geomPoints = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                geomPoints.add(Pair(coord.getDouble(1), coord.getDouble(0)))
            }

            val legs = mutableListOf<RouteLeg>()
            val legsArray = route.getJSONArray("legs")
            var totalDistance = 0.0
            var totalDuration = 0.0

            for (i in 0 until legsArray.length()) {
                val legObj = legsArray.getJSONObject(i)
                val legDist = legObj.getDouble("distance")
                val legDur = legObj.getDouble("duration")
                totalDistance += legDist
                totalDuration += legDur

                val steps = mutableListOf<RouteStep>()
                val stepsArray = legObj.getJSONArray("steps")

                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    val stepDist = stepObj.getDouble("distance")
                    val stepDur = stepObj.getDouble("duration")
                    val rawInstruction = stepObj.optString("name", "")

                    val maneuver = stepObj.optString("maneuver", "straight").also { m ->
                        Log.d(TAG, "Step $j maneuver raw: '$m'")
                    }
                    val maneuverObj = stepObj.optJSONObject("maneuver")
                    val maneuverType = maneuverObj?.optString("type", "straight") ?: "straight"
                    val maneuverModifier = maneuverObj?.optString("modifier", "") ?: ""

                    val intersection = stepObj.optJSONArray("intersections")
                    val bearings = intersection?.optJSONObject(0)?.optJSONArray("bearings")
                    var maneuverStr = maneuverType
                    if (maneuverModifier.isNotBlank()) {
                        maneuverStr = "$maneuverType-$maneuverModifier"
                    }

                    steps.add(RouteStep(
                        instruction = rawInstruction,
                        distance = stepDist,
                        duration = stepDur,
                        maneuver = maneuverStr,
                        name = rawInstruction
                    ))
                }

                legs.add(RouteLeg(distance = legDist, duration = legDur, steps = steps))
            }

            Log.d(TAG, "Itinéraire calculé : ${totalDistance / 1000} km, ${totalDuration / 60} min, ${geomPoints.size} points")

            RouteResult(
                success = true, distance = totalDistance, duration = totalDuration,
                geometry = geomPoints, legs = legs
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
        return when {
            maneuver.contains("turn-left") -> "Tournez à gauche"
            maneuver.contains("turn-right") -> "Tournez à droite"
            maneuver.contains("turn-slight-left") -> "Tournez légèrement à gauche"
            maneuver.contains("turn-slight-right") -> "Tournez légèrement à droite"
            maneuver.contains("turn-sharp-left") -> "Tournez fortement à gauche"
            maneuver.contains("turn-sharp-right") -> "Tournez fortement à droite"
            maneuver.contains("straight") -> "Continuez tout droit"
            maneuver.contains("uturn") -> "Faites demi-tour"
            maneuver.contains("on ramp") || maneuver.contains("ramp") -> "Prenez la rampe"
            maneuver.contains("merge") -> "Insérez-vous"
            maneuver.contains("fork") -> "Bifurquez"
            maneuver.contains("roundabout") || maneuver.contains("rotary") -> "Prenez le rond-point"
            maneuver == "depart" -> "Départ"
            maneuver == "arrive" -> "Arrivée"
            maneuver == "end" -> "Arrivée"
            maneuver == "continue" || maneuver == "new name" -> "Continuez"
            maneuver == "turn" -> "Tournez"
            else -> "Continuez"
        } + if (name.isNotBlank()) " sur $name" else ""
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
