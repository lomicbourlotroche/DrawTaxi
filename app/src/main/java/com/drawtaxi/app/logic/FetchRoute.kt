package com.drawtaxi.app.logic

import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteInfo(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double = 0.0
)

suspend fun fetchRoute(start: GeoPoint, end: GeoPoint): RouteInfo {
    return withContext(Dispatchers.IO) {
        try {
            // OSRM Public Server (Demo only - respect usage policy)
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                    "?overview=full&geometries=geojson"

            android.util.Log.d("DrawTaxi", "FetchRoute: URL=$urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                parseOsrmResponse(response.toString())
            } else {
                android.util.Log.e("DrawTaxi", "FetchRoute ERROR: code=${connection.responseCode}")
                RouteInfo(emptyList(), 0.0)
            }
        } catch (e: Exception) {
            android.util.Log.e("DrawTaxi", "FetchRoute EXCEPTION: ${e.message}", e)
            RouteInfo(emptyList(), 0.0)
        }
    }
}

private fun parseOsrmResponse(jsonString: String): RouteInfo {
    val points = mutableListOf<GeoPoint>()
    var distance = 0.0
    var duration = 0.0
    try {
        val json = JSONObject(jsonString)
        val routes = json.getJSONArray("routes")
        if (routes.length() > 0) {
            val route = routes.getJSONObject(0)
            distance = route.getDouble("distance")
            duration = route.getDouble("duration")
            
            val geometry = route.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val lon = coord.getDouble(0)
                val lat = coord.getDouble(1)
                points.add(GeoPoint(lat, lon))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return RouteInfo(points, distance, duration)
}
