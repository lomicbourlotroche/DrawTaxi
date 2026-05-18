package com.drawtaxi.app.logic

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service de calcul d'itinéraire via OSRM (Open Source Routing Machine)
 * Utilise l'API publique OSRM ou peut être auto-hébergé
 */
object OsrmRoutingService {
    
    private const val TAG = "OsrmRoutingService"
    
    // API publique OSRM (limitée à 100 req/min)
    private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/"
    
    // Alternative : MapTiler (clé API requise mais gratuite)
    // private const val MAPTILER_URL = "https://api.maptiler.com/routing/driving/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    data class RouteResult(
        val success: Boolean,
        val distance: Double, // en mètres
        val duration: Double, // en secondes
        val geometry: List<Pair<Double, Double>>, // Liste de points (lat, lon)
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
    
    /**
     * Calcule un itinéraire entre deux points
     * @param from Point de départ (Location avec latitude/longitude)
     * @param to Point d'arrivée (Location avec latitude/longitude)
     * @return RouteResult avec l'itinéraire complet ou une erreur
     */
    suspend fun calculateRoute(
        from: Location,
        to: Location
    ): RouteResult = withContext(Dispatchers.IO) {
        try {
            // Construire l'URL OSRM
            // Format : {lon1},{lat1};{lon2},{lat2}
            val coordinates = "${from.longitude},${from.latitude};${to.longitude},${to.latitude}"
            val url = "${OSRM_BASE_URL}${coordinates}?" +
                    "overview=full&" +          // Géométrie complète
                    "geometries=geojson&" +     // Format GeoJSON
                    "steps=true&" +             // Instructions étape par étape
                    "alternatives=false"        // Pas d'alternatives
            
            Log.d(TAG, "Requête OSRM : $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DrawTaxi/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur OSRM : ${response.code}")
                return@withContext RouteResult(
                    success = false,
                    distance = 0.0,
                    duration = 0.0,
                    geometry = emptyList(),
                    legs = emptyList(),
                    errorMessage = "Erreur serveur : ${response.code}"
                )
            }
            
            val jsonResponse = response.body?.string() ?: ""
            val json = JSONObject(jsonResponse)
            
            if (json.getString("code") != "Ok") {
                val message = json.optString("message", "Erreur inconnue")
                Log.e(TAG, "Erreur OSRM : $message")
                return@withContext RouteResult(
                    success = false,
                    distance = 0.0,
                    duration = 0.0,
                    geometry = emptyList(),
                    legs = emptyList(),
                    errorMessage = message
                )
            }
            
            // Parser la réponse
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext RouteResult(
                    success = false,
                    distance = 0.0,
                    duration = 0.0,
                    geometry = emptyList(),
                    legs = emptyList(),
                    errorMessage = "Aucun itinéraire trouvé"
                )
            }
            
            val route = routes.getJSONObject(0)
            val distance = route.getDouble("distance") // en mètres
            val duration = route.getDouble("duration") // en secondes
            
            // Parser la géométrie (points du trajet)
            val geometry = mutableListOf<Pair<Double, Double>>()
            val geometryObj = route.getJSONObject("geometry")
            val geometryCoordinates = geometryObj.getJSONArray("coordinates")
            
            for (i in 0 until geometryCoordinates.length()) {
                val point = geometryCoordinates.getJSONArray(i)
                val lon = point.getDouble(0)
                val lat = point.getDouble(1)
                geometry.add(Pair(lat, lon)) // Note : OSRM retourne [lon, lat], on convertit en [lat, lon]
            }
            
            // Parser les legs (segments)
            val legs = mutableListOf<RouteLeg>()
            val legsArray = route.getJSONArray("legs")
            
            for (i in 0 until legsArray.length()) {
                val legObj = legsArray.getJSONObject(i)
                val legDistance = legObj.getDouble("distance")
                val legDuration = legObj.getDouble("duration")
                
                // Parser les steps (instructions)
                val steps = mutableListOf<RouteStep>()
                val stepsArray = legObj.getJSONArray("steps")
                
                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    steps.add(
                        RouteStep(
                            instruction = stepObj.optString("name", "Continuer"),
                            distance = stepObj.getDouble("distance"),
                            duration = stepObj.getDouble("duration"),
                            maneuver = stepObj.getJSONObject("maneuver").optString("type", "straight"),
                            name = stepObj.optString("name", "")
                        )
                    )
                }
                
                legs.add(
                    RouteLeg(
                        distance = legDistance,
                        duration = legDuration,
                        steps = steps
                    )
                )
            }
            
            Log.d(TAG, "Itinéraire calculé : ${distance/1000} km, ${duration/60} min, ${geometry.size} points")
            
            RouteResult(
                success = true,
                distance = distance,
                duration = duration,
                geometry = geometry,
                legs = legs
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur calcul itinéraire", e)
            RouteResult(
                success = false,
                distance = 0.0,
                duration = 0.0,
                geometry = emptyList(),
                legs = emptyList(),
                errorMessage = "Erreur réseau : ${e.message}"
            )
        }
    }
    
    /**
     * Calcule l'itinéraire depuis la position actuelle vers une destination
     */
    suspend fun calculateRouteFromCurrentPosition(
        currentLocation: Location,
        destinationAddress: String
    ): RouteResult {
        val destLocation = com.drawtaxi.app.logic.GeocodingService.geocode(destinationAddress)
            ?: return RouteResult(
                success = false,
                distance = 0.0,
                duration = 0.0,
                geometry = emptyList(),
                legs = emptyList(),
                errorMessage = "Adresse de destination introuvable"
            )

        return calculateRoute(currentLocation, destLocation)
    }
    
    /**
     * Convertit une durée en secondes en format lisible
     */
    fun formatDuration(seconds: Double): String {
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes} min"
            else -> "< 1 min"
        }
    }
    
    /**
     * Convertit une distance en mètres en format lisible
     */
    fun formatDistance(meters: Double): String {
        return when {
            meters >= 1000 -> String.format("%.1f km", meters / 1000)
            else -> String.format("%.0f m", meters)
        }
    }
    
    /**
     * Retourne l'instruction textuelle pour un type de manœuvre
     */
    fun getManeuverInstruction(maneuver: String, name: String = ""): String {
        return when (maneuver.lowercase()) {
            "turn" -> "Tourner"
            "new name" -> "Continuer sur"
            "depart" -> "Départ"
            "arrive" -> "Arrivée"
            "merge" -> "S'insérer"
            "on ramp" -> "Prendre la rampe"
            "off ramp" -> "Sortir"
            "fork" -> "Prendre la bifurcation"
            "end of road" -> "Fin de route"
            "continue" -> "Continuer"
            "roundabout" -> "Prendre le rond-point"
            "rotary" -> "Prendre le giratoire"
            "roundabout turn" -> "Au rond-point"
            "notification" -> "Note"
            "exit roundabout" -> "Sortir du rond-point"
            "exit rotary" -> "Sortir du giratoire"
            else -> "Continuer"
        } + if (name.isNotBlank()) " $name" else ""
    }
}
