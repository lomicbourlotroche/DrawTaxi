package com.drawtaxi.app.logic.messaging

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.MessageChannel
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

data class WebFormRequest(
    val sender: String,
    val body: String,
    val departure: String = "",
    val arrival: String = "",
    val time: String = "",
    val date: String = "",
    val clientName: String = "",
    val clientEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(json: JSONObject): WebFormRequest {
            return WebFormRequest(
                sender = json.optString("sender", ""),
                body = json.optString("body", ""),
                departure = json.optString("departure", ""),
                arrival = json.optString("arrival", ""),
                time = json.optString("time", ""),
                date = json.optString("date", ""),
                clientName = json.optString("clientName", ""),
                clientEmail = json.optString("clientEmail", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("sender", sender)
            put("body", body)
            put("departure", departure)
            put("arrival", arrival)
            put("time", time)
            put("date", date)
            put("clientName", clientName)
            put("clientEmail", clientEmail)
            put("timestamp", timestamp)
        }
    }

    fun toRideRequest(): RideRequest {
        return RideRequest(
            id = RideRequest.createStableId(sender, body, timestamp),
            sender = sender,
            body = body,
            departure = departure,
            arrival = arrival,
            time = time,
            date = date,
            timestamp = timestamp,
            clientName = clientName,
            clientEmail = clientEmail,
            messageChannel = MessageChannel.WEB_FORM
        )
    }
}

object WebFormReceiver {

    private const val TAG = "WebFormReceiver"

    fun processWebFormRequest(context: Context, json: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = WebFormRequest.fromJson(json)
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                val repository = TaxiRepository(
                    database.rideDao(),
                    database.quoteDao(),
                    database.absenceDao(),
                    settingsManager
                )

                val ride = request.toRideRequest()
                repository.saveRide(ride)

                NotificationHelper.showNewRideNotification(
                    context,
                    ride.id,
                    ride.arrival,
                    ride.time
                )

                Log.d(TAG, "Course créée depuis formulaire web: ${ride.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement formulaire web: ${e.message}")
            }
        }
    }

    fun processWebFormRequest(context: Context, jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            processWebFormRequest(context, json)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing JSON: ${e.message}")
        }
    }

    fun createApiEndpoint(): String {
        return """
            API Endpoint pour le formulaire de contact du site web:
            
            POST /api/ride-request
            Content-Type: application/json
            
            {
                "sender": "+33612345678",
                "body": "Demande de course",
                "departure": "Paris",
                "arrival": "Lyon",
                "time": "14:30",
                "date": "15/03/2024",
                "clientName": "Jean Dupont",
                "clientEmail": "jean@example.com",
                "timestamp": 1710500000000
            }
            
            Pour connecter le site web:
            1. Créer un endpoint backend (Node.js, Python, PHP, etc.)
            2. Envoyer les données au format JSON ci-dessus
            3. Utiliser Firebase Cloud Messaging pour push vers l'app
            4. Ou utiliser un webhook avec polling
            
            Alternative: Utiliser Firebase Realtime Database ou Firestore
            pour synchroniser les demandes entre le site web et l'app mobile.
        """.trimIndent()
    }
}

class WebFormApiHandler {

    companion object {
        private const val TAG = "WebFormApiHandler"

        fun handleRequest(context: Context, method: String, path: String, body: String): String {
            return when {
                method == "POST" && path == "/api/ride-request" -> {
                    try {
                        val json = JSONObject(body)
                        WebFormReceiver.processWebFormRequest(context, json)
                        "{\"status\": \"success\", \"message\": \"Demande de course reçue\"}"
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur: ${e.message}")
                        "{\"status\": \"error\", \"message\": \"${e.message}\"}"
                    }
                }
                method == "GET" && path == "/api/health" -> {
                    "{\"status\": \"ok\", \"service\": \"DrawTaxi API\"}"
                }
                method == "GET" && path == "/api/docs" -> {
                    WebFormReceiver.createApiEndpoint()
                }
                else -> {
                    "{\"status\": \"error\", \"message\": \"Endpoint non trouvé\"}"
                }
            }
        }
    }
}
