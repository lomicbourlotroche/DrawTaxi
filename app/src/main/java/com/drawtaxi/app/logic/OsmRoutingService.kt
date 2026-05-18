package com.drawtaxi.app.logic

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class RouteStep(
    val instruction: String,
    val distance: Double,
    val duration: Double,
    val maneuverType: String,
    val maneuverModifier: String = "",
    val name: String = ""
) {
    val displayInstruction: String
        get() {
            val direction = when (maneuverModifier) {
                "left" -> "à gauche"
                "right" -> "à droite"
                "slight left" -> "légèrement à gauche"
                "slight right" -> "légèrement à droite"
                "sharp left" -> "fortement à gauche"
                "sharp right" -> "fortement à droite"
                "straight" -> "tout droit"
                "uturn" -> "demi-tour"
                else -> ""
            }

            val action = when (maneuverType) {
                "turn" -> "Tournez $direction"
                "new name" -> if (name.isNotBlank()) "Continuez sur $name" else "Continuez tout droit"
                "depart" -> "Départ"
                "arrive" -> "Vous êtes arrivé"
                "roundabout" -> "Au rond-point, prenez la sortie $direction"
                "rotary" -> "Au rond-point, prenez la sortie $direction"
                "merge" -> "Fusionnez $direction"
                "fork" -> if (direction.contains("gauche")) "Bifurquez à gauche" else "Bifurquez à droite"
                "end of road" -> "En fin de route, tournez $direction"
                "notification" -> instruction
                else -> instruction
            }

            return if (name.isNotBlank() && maneuverType != "new name") {
                "$action sur $name"
            } else {
                action
            }
        }
}

data class OsmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: List<Pair<Double, Double>>,
    val steps: List<RouteStep>,
    val bbox: List<Double>? = null
)

object OsmRoutingService {

    private const val TAG = "OsmRouting"
    private const val OSRM_BASE_URL = "https://router.project-osrm.org"

    suspend fun getRoute(
        start: Location,
        end: Location
    ): OsmRoute? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$OSRM_BASE_URL/route/v1/driving/" +
                        "${start.longitude},${start.latitude};" +
                        "${end.longitude},${end.latitude}" +
                        "?overview=full&geometries=geojson&steps=true&annotations=true"

                Log.d(TAG, "OSRM request: $url")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    Log.e(TAG, "OSRM error: $responseCode")
                    connection.disconnect()
                    return@withContext null
                }

                val response = try {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    reader.readText()
                } finally {
                    connection.disconnect()
                }

                parseOsmResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "OSRM routing error: ${e.message}")
                null
            }
        }
    }

    private fun parseOsmResponse(response: String): OsmRoute? {
        return try {
            val json = JSONObject(response)

            if (json.optString("code") != "Ok") {
                Log.e(TAG, "OSRM error code: ${json.optString("code")}")
                return null
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return null

            val route = routes.getJSONObject(0)
            val distance = route.getDouble("distance")
            val duration = route.getDouble("duration")

            val geometry = parseGeometry(route.getJSONObject("geometry"))

            val legs = route.getJSONArray("legs")
            val steps = mutableListOf<RouteStep>()

            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                val legSteps = leg.getJSONArray("steps")

                for (j in 0 until legSteps.length()) {
                    val stepJson = legSteps.getJSONObject(j)
                    val maneuver = stepJson.getJSONObject("maneuver")

                    val step = RouteStep(
                        instruction = stepJson.optString("name", ""),
                        distance = stepJson.getDouble("distance"),
                        duration = stepJson.getDouble("duration"),
                        maneuverType = maneuver.getString("type"),
                        maneuverModifier = maneuver.optString("modifier", ""),
                        name = stepJson.optString("name", "")
                    )
                    steps.add(step)
                }
            }

            OsmRoute(
                distance = distance,
                duration = duration,
                geometry = geometry,
                steps = steps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            null
        }
    }

    private fun parseGeometry(geometryJson: JSONObject): List<Pair<Double, Double>> {
        val coordinates = geometryJson.getJSONArray("coordinates")
        val points = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            val lon = coord.getDouble(0)
            val lat = coord.getDouble(1)
            points.add(Pair(lat, lon))
        }

        return points
    }

    fun findNearestRoutePoint(
        location: Location,
        geometry: List<Pair<Double, Double>>
    ): Int {
        var minDistance = Double.MAX_VALUE
        var nearestIndex = 0

        geometry.forEachIndexed { index, point ->
            val dist = haversineDistance(
                location.latitude, location.longitude,
                point.first, point.second
            )
            if (dist < minDistance) {
                minDistance = dist
                nearestIndex = index
            }
        }

        return nearestIndex
    }

    fun calculateRemainingRoute(
        currentIndex: Int,
        geometry: List<Pair<Double, Double>>
    ): Double {
        if (currentIndex >= geometry.size - 1) return 0.0

        var remaining = 0.0
        for (i in currentIndex until geometry.size - 1) {
            remaining += haversineDistance(
                geometry[i].first, geometry[i].second,
                geometry[i + 1].first, geometry[i + 1].second
            )
        }
        return remaining
    }

    fun getCurrentStep(
        location: Location,
        geometry: List<Pair<Double, Double>>,
        steps: List<RouteStep>
    ): RouteStep? {
        val nearestIndex = findNearestRoutePoint(location, geometry)

        var accumulatedDistance = 0.0
        var currentStepIndex = 0

        for (i in 0 until nearestIndex) {
            val segmentDist = haversineDistance(
                geometry[i].first, geometry[i].second,
                geometry[i + 1].first, geometry[i + 1].second
            )
            accumulatedDistance += segmentDist

            while (currentStepIndex < steps.size - 1 &&
                accumulatedDistance > steps.take(currentStepIndex + 1).sumOf { it.distance }) {
                currentStepIndex++
            }
        }

        return steps.getOrNull(currentStepIndex)
    }

    fun getNextStep(
        currentStepIndex: Int,
        steps: List<RouteStep>
    ): RouteStep? {
        return steps.getOrNull(currentStepIndex + 1)
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
