package com.drawtaxi.app.logic.geocoding

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GeocodingService {

    private const val TAG = "GeocodingService"
    private const val PHOTON_BASE = "https://photon.komoot.io/api"

    private val brestCities = listOf(
        "Brest", "Gouesnou", "Guipavas", "Le Relecq-Kerhuon",
        "Plougastel-Daoulas", "Plouzane", "Saint-Renan",
        "Landerneau", "Daoulas", "Plabennec", "Guilers"
    )

    suspend fun geocode(address: String, context: android.content.Context? = null): Location? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(15_000L) {
                Log.d(TAG, "Geocoding: $address")

                tryPhoton(address)?.let { return@withTimeoutOrNull it }

                for (city in brestCities) {
                    tryPhotonWithCity(address, city)?.let { return@withTimeoutOrNull it }
                }

                Log.d(TAG, "Photon geocoding failed for: $address")
                null
            }
        }
    }

    private fun tryPhoton(address: String): Location? {
        return try {
            val query = URLEncoder.encode(address, "UTF-8")
            val url = URL("$PHOTON_BASE?q=$query&limit=5&lang=fr&lat=48.39&lon=-4.49&zoom=10&location_bias_scale=0.8")
            parsePhotonResponse(fetchJson(url))
        } catch (e: Exception) {
            Log.w(TAG, "Photon failed: ${e.message}")
            null
        }
    }

    private fun tryPhotonWithCity(address: String, city: String): Location? {
        return try {
            val query = URLEncoder.encode("$address, $city", "UTF-8")
            val url = URL("$PHOTON_BASE?q=$query&limit=1&lang=fr&lat=48.39&lon=-4.49&zoom=12")
            parsePhotonResponse(fetchJson(url))
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchJson(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "DrawTaxi/1.0")
        try {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            return reader.readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePhotonResponse(response: String): Location? {
        return try {
            val json = JSONObject(response)
            val features = json.getJSONArray("features")
            if (features.length() > 0) {
                val feature = features.getJSONObject(0)
                val geometry = feature.getJSONObject("geometry")
                val coords = geometry.getJSONArray("coordinates")
                Location("photon").apply {
                    longitude = coords.getDouble(0)
                    latitude = coords.getDouble(1)
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
