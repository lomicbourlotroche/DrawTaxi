package com.drawtaxi.app.logic.routing

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RouteInfo(
    val points: List<Location>,
    val distanceMeters: Double,
    val durationSeconds: Double = 0.0
)

suspend fun fetchRoute(start: Location, end: Location): RouteInfo {
    return withContext(Dispatchers.IO) {
        try {
            val origin = "${start.longitude},${start.latitude}"
            val destination = "${end.longitude},${end.latitude}"
            val urlString = "https://router.project-osrm.org/route/v1/driving/$origin;$destination?overview=full&geometries=geojson&steps=true"

            android.util.Log.d("DrawTaxi", "OSRM Route: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            val result = try {
                if (connection.responseCode == 200) {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    parseOsrmResponse(response.toString())
                } else {
                    android.util.Log.e("DrawTaxi", "OSRM ERROR: code=${connection.responseCode}")
                    RouteInfo(emptyList(), 0.0)
                }
            } finally {
                connection.disconnect()
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("DrawTaxi", "OSRM EXCEPTION: ${e.message}", e)
            RouteInfo(emptyList(), 0.0)
        }
    }
}

private fun parseOsrmResponse(jsonString: String): RouteInfo {
    val points = mutableListOf<Location>()
    var distance = 0.0
    var duration = 0.0
    try {
        val json = JSONObject(jsonString)
        if (json.optString("code") != "Ok") return RouteInfo(emptyList(), 0.0)

        val routes = json.getJSONArray("routes")
        if (routes.length() > 0) {
            val route = routes.getJSONObject(0)
            distance = route.getDouble("distance")
            duration = route.getDouble("duration")

            val geometry = route.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val loc = Location("osrm").apply {
                    longitude = coord.getDouble(0)
                    latitude = coord.getDouble(1)
                }
                points.add(loc)
            }

            val legs = route.getJSONArray("legs")
            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                val steps = leg.getJSONArray("steps")
                for (j in 0 until steps.length()) {
                    val step = steps.getJSONObject(j)
                    val intersection = step.optJSONArray("intersections")
                    if (intersection != null && intersection.length() > 0) {
                        val loc = intersection.getJSONObject(0).getJSONArray("location")
                        val stepLoc = Location("osrm_step").apply {
                            longitude = loc.getDouble(0)
                            latitude = loc.getDouble(1)
                        }
                        points.add(stepLoc)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return RouteInfo(points, distance, duration)
}
