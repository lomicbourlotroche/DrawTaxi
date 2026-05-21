package com.drawtaxi.app.logic.routing

import android.location.Location
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteInfo(
    val points: List<Location>,
    val distanceMeters: Double,
    val durationSeconds: Double = 0.0
)

var fetchRouteApiKey: String = "YOUR_GOOGLE_MAPS_API_KEY"

suspend fun fetchRoute(start: Location, end: Location): RouteInfo {
    return withContext(Dispatchers.IO) {
        try {
            val origin = "${start.latitude},${start.longitude}"
            val destination = "${end.latitude},${end.longitude}"
            val urlString = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$origin&destination=$destination&key=$fetchRouteApiKey&units=metric"

            android.util.Log.d("DrawTaxi", "FetchRoute: URL=$urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            val result = try {
                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    parseDirectionsResponse(response.toString())
                } else {
                    android.util.Log.e("DrawTaxi", "FetchRoute ERROR: code=${connection.responseCode}")
                    RouteInfo(emptyList(), 0.0)
                }
            } finally {
                connection.disconnect()
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("DrawTaxi", "FetchRoute EXCEPTION: ${e.message}", e)
            RouteInfo(emptyList(), 0.0)
        }
    }
}

private fun parseDirectionsResponse(jsonString: String): RouteInfo {
    val points = mutableListOf<Location>()
    var distance = 0.0
    var duration = 0.0
    try {
        val json = JSONObject(jsonString)
        if (json.optString("status") != "OK") return RouteInfo(emptyList(), 0.0)

        val routes = json.getJSONArray("routes")
        if (routes.length() > 0) {
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")

            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                distance += leg.getJSONObject("distance").getDouble("value")
                duration += leg.getJSONObject("duration").getDouble("value")

                val steps = leg.getJSONArray("steps")
                for (j in 0 until steps.length()) {
                    val step = steps.getJSONObject(j)
                    val endLocation = step.getJSONObject("end_location")
                    val loc = Location("directions").apply {
                        latitude = endLocation.getDouble("lat")
                        longitude = endLocation.getDouble("lng")
                    }
                    points.add(loc)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return RouteInfo(points, distance, duration)
}
