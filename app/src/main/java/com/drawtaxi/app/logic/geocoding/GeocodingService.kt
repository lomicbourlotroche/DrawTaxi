package com.drawtaxi.app.logic.geocoding

import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodingResult(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val confidence: Float
)

object GeocodingService {

    private const val TAG = "GeocodingService"
    private const val PHOTON_BASE = "https://photon.komoot.io/api"
    private const val NOMINATIM_BASE = "https://nominatim.openstreetmap.org/search"

    private val brestCities = listOf(
        "Brest", "Gouesnou", "Guipavas", "Le Relecq-Kerhuon",
        "Plougastel-Daoulas", "Plouzane", "Saint-Renan",
        "Landerneau", "Daoulas", "Plabennec", "Guilers"
    )

    suspend fun geocode(address: String, context: android.content.Context? = null): Location? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(15_000L) {
                log("Geocoding: $address")

                // 1. Essai direct avec les variations normalisées
                val variations = AddressNormalizer.normalize(address)
                log("Generated ${variations.size} variations")

                for ((index, variation) in variations.withIndex()) {
                    log("[$index] Trying: $variation")

                    var result: Location? = tryPhoton(variation)
                    if (result != null) {
                        log("Found via Photon: ${result.latitude}, ${result.longitude}")
                        return@withTimeoutOrNull result
                    }

                    if (index < 5) {
                        result = tryPhotonWithCity(variation)
                        if (result != null) {
                            log("Found via Photon+City: ${result.latitude}, ${result.longitude}")
                            return@withTimeoutOrNull result
                        }
                    }

                    if (index < 3) {
                        result = tryNominatim(variation)
                        if (result != null) {
                            log("Found via Nominatim: ${result.latitude}, ${result.longitude}")
                            return@withTimeoutOrNull result
                        }

                        result = tryNominatimSimple(variation)
                        if (result != null) {
                            log("Found via Nominatim Simple: ${result.latitude}, ${result.longitude}")
                            return@withTimeoutOrNull result
                        }
                    }
                }

                // 2. Fallback: Si l'adresse semble incomplète (ex: juste "Gare" ou "Aéroport")
                val incompleteResult = tryIncompleteAddress(address)
                if (incompleteResult != null) {
                    log("Found via incomplete address fallback: ${incompleteResult.latitude}, ${incompleteResult.longitude}")
                    return@withTimeoutOrNull incompleteResult
                }

                // 3. Fallback: Code postal
                val postalResult = tryPostalCodeMapping(address)
                if (postalResult != null) {
                    log("Found via postal code: ${postalResult.latitude}, ${postalResult.longitude}")
                    return@withTimeoutOrNull postalResult
                }

                // 4. Fallback ultime: Android Geocoder natif
                if (context != null) {
                    val rawResult = tryRawAddress(address, context)
                    if (rawResult != null) {
                        log("Found via raw address (Android Geocoder): ${rawResult.latitude}, ${rawResult.longitude}")
                        return@withTimeoutOrNull rawResult
                    }
                }

                log("All strategies failed for: $address")
                null
            }
        }
    }

    private suspend fun tryIncompleteAddress(address: String): Location? {
        return withContext(Dispatchers.IO) {
            val lower = address.lowercase()
            
            // Points d'intérêt communs à Brest (exemple)
            val poiLocations = mapOf(
                "gare" to Pair(48.3897, -4.4706), // Gare de Brest
                "aéroport" to Pair(48.4475, -4.4185), // Aéroport Brest Bretagne
                "cdg" to Pair(49.0097, 2.5479), // Charles de Gaulle
                "orly" to Pair(48.7233, 2.3794), // Orly
                "centre ville" to Pair(48.3904, -4.4860), // Centre Brest
                "hôpital" to Pair(48.3775, -4.4577), // Hôpital Cavale Blanche
                "hopital" to Pair(48.3775, -4.4577)
            )

            for ((poi, coords) in poiLocations) {
                if (lower.contains(poi)) {
                    log("Matched POI: $poi")
                    return@withContext Location("poi_fallback").apply {
                        latitude = coords.first
                        longitude = coords.second
                    }
                }
            }
            null
        }
    }

    private fun tryPhoton(address: String): Location? {
        return try {
            val query = URLEncoder.encode(address, "UTF-8")
            val url = URL("$PHOTON_BASE?q=$query&limit=1&lang=fr")
            val json = fetchJson(url)
            parsePhotonResponse(json)
        } catch (e: Exception) {
            Log.w(TAG, "Photon failed: ${e.message}")
            null
        }
    }

    private fun tryPhotonWithCity(address: String): Location? {
        return try {
            for (city in brestCities.take(5)) {
                val query = URLEncoder.encode("$address, $city", "UTF-8")
                val url = URL("$PHOTON_BASE?q=$query&limit=1&lang=fr&lat=48.39&lon=-4.49&zoom=12")
                val json = fetchJson(url)
                val result = parsePhotonResponse(json)
                if (result != null) return result
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryNominatim(address: String): Location? {
        return try {
            val query = URLEncoder.encode(address, "UTF-8")
            val url = URL("$NOMINATIM_BASE?q=$query&format=json&limit=1&countrycodes=fr")
            val json = fetchJson(url)
            parseNominatimResponse(json)
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim failed: ${e.message}")
            null
        }
    }

    private fun tryNominatimSimple(address: String): Location? {
        return try {
            val simplified = address
                .lowercase()
                .replace(Regex("[^a-z0-9\\s,\\-]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            val query = URLEncoder.encode(simplified, "UTF-8")
            val url = URL("$NOMINATIM_BASE?q=$query&format=json&limit=1")
            val json = fetchJson(url)
            parseNominatimResponse(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryPostalCodeMapping(address: String): Location? {
        return try {
            val postalPattern = Regex("(\\d{5})")
            val match = postalPattern.find(address)
            if (match != null) {
                val postalCode = match.groupValues[1]
                val query = URLEncoder.encode(postalCode, "UTF-8")
                val url = URL("$NOMINATIM_BASE?q=$query&format=json&limit=1&countrycodes=fr")
                val json = fetchJson(url)
                parseNominatimResponse(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryRawAddress(address: String, context: android.content.Context): Location? {
        return try {
            val geocoder = Geocoder(context)
            val results: List<Address>? = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                val addr = results[0]
                Location("geocoder").apply {
                    latitude = addr.latitude
                    longitude = addr.longitude
                }
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Raw address geocoding failed: ${e.message}")
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
            val response = reader.readText()
            reader.close()
            return response
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

    private fun parseNominatimResponse(response: String): Location? {
        return try {
            val json = JSONArray(response)
            if (json.length() > 0) {
                val result = json.getJSONObject(0)
                Location("nominatim").apply {
                    latitude = result.getDouble("lat")
                    longitude = result.getDouble("lon")
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }
}
