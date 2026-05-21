package com.drawtaxi.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.drawtaxi.app.TaxiApplication
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.AiParsedResult
import com.drawtaxi.app.logic.sms.ParsedSms
import com.drawtaxi.app.logic.sms.extractMissingFields
import com.drawtaxi.app.logic.sms.formatConfirmationMessage
import com.drawtaxi.app.logic.sms.formatMissingFieldsMessage
import com.drawtaxi.app.logic.sms.isTaxiRelated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val RECENT_RIDE_WINDOW_MS = 3600000L
        private const val DEDUP_WINDOW_MS = 30000L
        private const val MAX_DEDUP_CACHE = 200
        private val processedMessages = ConcurrentHashMap<String, Long>()
        private val smsProcessingDispatcher = java.util.concurrent.Executors
            .newSingleThreadExecutor()
            .asCoroutineDispatcher()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val dedupKey = "$sender|${fullBody.take(50)}|$timestamp"
        val lastProcessed = processedMessages[dedupKey]
        val now = System.currentTimeMillis()
        
        if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) {
            Log.d(TAG, "SMS dupliqué ignoré dans BroadcastReceiver")
            return
        }
        
        processedMessages[dedupKey] = now
        if (processedMessages.size > MAX_DEDUP_CACHE) {
            val oldestKeys = processedMessages.entries
                .sortedBy { it.value }
                .take(processedMessages.size - MAX_DEDUP_CACHE / 2)
            oldestKeys.forEach { processedMessages.remove(it.key) }
        }

        Log.d(TAG, "SMS reçu - De: $sender - Corps: ${fullBody.take(50)}...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permission RECEIVE_SMS non accordée")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                withTimeoutOrNull(15000L) {
                    val repository = (context.applicationContext as TaxiApplication).repository
                    val settings = repository.settings.first()

                    if (!settings.monitorSms) {
                        Log.d(TAG, "Surveillance SMS désactivée")
                        return@withTimeoutOrNull
                    }

                    if (!hasSmsPermission(context)) {
                        Log.e(TAG, "Permission SMS non accordée")
                        return@withTimeoutOrNull
                    }

                    // Utiliser l'IA pour parser le SMS
                    val aiResult = com.drawtaxi.app.logic.sms.AiSmsParser.parseWithAI(
                        context,
                        fullBody,
                        settings.aiEnabled
                    )
                    Log.d(TAG, "Résultat AI BroadcastReceiver: departure=${aiResult.departure}, arrival=${aiResult.arrival}, time=${aiResult.time}")

                    val existingRide = findRecentRideForSender(sender, repository)

                    if (aiResult.isCancellation) {
                        handleCancellation(context, sender, fullBody, repository)
                        return@withTimeoutOrNull
                    }

                    if (aiResult.isConfirmation && existingRide != null) {
                        handleConfirmation(context, sender, fullBody, existingRide, repository)
                        return@withTimeoutOrNull
                    }

                    if (aiResult.isModification && existingRide != null) {
                        handleModificationWithAI(context, sender, fullBody, aiResult, existingRide, repository, settings)
                        return@withTimeoutOrNull
                    }

                    processNewOrUpdatedRideWithAI(context, sender, fullBody, aiResult, timestamp, existingRide, repository, settings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement SMS: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun findRecentRideForSender(
        sender: String,
        repository: com.drawtaxi.app.data.TaxiRepository
    ): com.drawtaxi.app.data.RideRequest? {
        val pendingList = repository.pendingRides.first()
        return pendingList
            .filter { it.sender == sender }
            .filter { System.currentTimeMillis() - it.timestamp < RECENT_RIDE_WINDOW_MS }
            .maxByOrNull { it.timestamp }
    }

    private suspend fun handleCancellation(
        context: Context,
        sender: String,
        body: String,
        repository: com.drawtaxi.app.data.TaxiRepository
    ) {
        val pendingList = repository.pendingRides.first()
        val rideToDelete = pendingList
            .filter { it.sender == sender }
            .maxByOrNull { it.timestamp }

        if (rideToDelete != null) {
            repository.deleteRide(rideToDelete)
            Log.d(TAG, "Course supprimée: ${rideToDelete.id}")

            sendSmsReply(
                context,
                sender,
                "DrawTaxi : Votre course a été annulée. N'hésitez pas à nous recontacter si besoin."
            )

            NotificationHelper.showInfoNotification(
                context,
                "Course annulée",
                "Course supprimée: ${rideToDelete.departure} → ${rideToDelete.arrival}",
                null
            )
        } else {
            sendSmsReply(
                context,
                sender,
                "DrawTaxi : Nous n'avons pas de course en cours à votre nom. Bonne journée."
            )
        }
    }

    private suspend fun handleConfirmation(
        context: Context,
        sender: String,
        body: String,
        ride: com.drawtaxi.app.data.RideRequest,
        repository: com.drawtaxi.app.data.TaxiRepository
    ) {
        Log.d(TAG, "Confirmation reçue pour course: ${ride.id}")

        val updatedRide = ride.copy(
            body = ride.body + "\n--- CONFIRMATION ---\n$body"
        )

        repository.updateRide(updatedRide)

        sendSmsReply(
            context,
            sender,
            "DrawTaxi : Merci pour votre confirmation ! Votre chauffeur arrivera à l'heure prévue."
        )
    }

    private suspend fun handleModification(
        context: Context,
        sender: String,
        body: String,
        parsed: ParsedSms,
        existingRide: com.drawtaxi.app.data.RideRequest,
        repository: com.drawtaxi.app.data.TaxiRepository,
        settings: com.drawtaxi.app.data.AppSettings
    ) {
        Log.d(TAG, "Modification reçue pour course: ${existingRide.id}")

        val updatedDeparture = if (parsed.departure.isNotBlank() && 
            parsed.departure != existingRide.departure) {
            parsed.departure
        } else existingRide.departure

        val updatedArrival = if (parsed.arrival.isNotBlank() && 
            parsed.arrival != existingRide.arrival) {
            parsed.arrival
        } else existingRide.arrival

        val updatedTime = if (parsed.time.isNotBlank() && 
            parsed.time != existingRide.time) {
            parsed.time
        } else existingRide.time

        val updatedRide = existingRide.copy(
            departure = updatedDeparture,
            arrival = updatedArrival,
            time = updatedTime,
            body = existingRide.body + "\n--- MODIFICATION ---\n$body",
            timestamp = System.currentTimeMillis()
        )

        repository.updateRide(updatedRide)

        val missingFields = extractMissingFields(parsed, updatedRide)

        if (missingFields.isNotEmpty()) {
            val reply = formatMissingFieldsMessage(
                missingFields,
                settings.missingInfoTemplate
            )
            sendSmsReply(context, sender, reply)
        } else {
            sendSmsReply(
                context,
                sender,
                "DrawTaxi : Votre course a été mise à jour. À tout de suite !"
            )
        }
    }

    private suspend fun handleModificationWithAI(
        context: Context,
        sender: String,
        body: String,
        aiResult: AiParsedResult,
        existingRide: com.drawtaxi.app.data.RideRequest,
        repository: com.drawtaxi.app.data.TaxiRepository,
        settings: com.drawtaxi.app.data.AppSettings
    ) {
        Log.d(TAG, "Modification reçue (AI) pour course: ${existingRide.id}")

        val updatedDeparture = if (aiResult.departure.isNotBlank() && 
            aiResult.departure != existingRide.departure) {
            aiResult.departure
        } else existingRide.departure

        val updatedArrival = if (aiResult.arrival.isNotBlank() && 
            aiResult.arrival != existingRide.arrival) {
            aiResult.arrival
        } else existingRide.arrival

        val updatedTime = if (aiResult.time.isNotBlank() && 
            aiResult.time != existingRide.time) {
            aiResult.time
        } else existingRide.time

        val updatedRide = existingRide.copy(
            departure = updatedDeparture,
            arrival = updatedArrival,
            time = updatedTime,
            body = existingRide.body + "\n--- MODIFICATION ---\n$body",
            timestamp = System.currentTimeMillis()
        )

        repository.updateRide(updatedRide)

        if (aiResult.missingFields.isNotEmpty()) {
            val reply = formatMissingFieldsMessage(
                aiResult.missingFields,
                settings.missingInfoTemplate
            )
            sendSmsReply(context, sender, reply)
        } else {
            sendSmsReply(
                context,
                sender,
                "DrawTaxi : Votre course a été mise à jour. À tout de suite !"
            )
        }
    }

    private suspend fun processNewOrUpdatedRide(
        context: Context,
        sender: String,
        body: String,
        parsed: ParsedSms,
        timestamp: Long,
        existingRide: com.drawtaxi.app.data.RideRequest?,
        repository: com.drawtaxi.app.data.TaxiRepository,
        settings: com.drawtaxi.app.data.AppSettings
    ) {
        val hasValidInfo = parsed.departure.isNotBlank() ||
                          parsed.arrival.isNotBlank() ||
                          parsed.time.isNotBlank()

        if (!hasValidInfo && !isTaxiRelated(body)) {
            Log.d(TAG, "SMS ignoré (non taxi et sans info): $sender")
            return
        }

        val rideToSave: com.drawtaxi.app.data.RideRequest

        if (existingRide != null) {
            val mergedDeparture = mergeField(parsed.departure, existingRide.departure)
            val mergedArrival = mergeField(parsed.arrival, existingRide.arrival)
            val mergedTime = mergeField(parsed.time, existingRide.time)

            rideToSave = existingRide.copy(
                departure = mergedDeparture,
                arrival = mergedArrival,
                time = mergedTime,
                body = existingRide.body + "\n--- NOUVEAU MESSAGE ---\n$body",
                timestamp = System.currentTimeMillis()
            )

            repository.updateRide(rideToSave)
            Log.d(TAG, "Course mise à jour: ${rideToSave.id}")
        } else {
            val newRide = com.drawtaxi.app.data.RideRequest(
                id = com.drawtaxi.app.data.RideRequest.createStableId(sender, body, timestamp),
                sender = sender,
                body = body,
                departure = parsed.departure,
                arrival = parsed.arrival,
                time = parsed.time,
                date = parsed.date,
                distanceKm = 0.0,
                timestamp = timestamp
            )
            repository.saveRide(newRide)
            rideToSave = newRide
            Log.d(TAG, "Nouvelle course créée: ${rideToSave.id}")
        }

        val missingFields = parsed.missingFields.ifEmpty { extractMissingFields(parsed, rideToSave) }

        if (missingFields.isNotEmpty()) {
            val reply = formatMissingFieldsMessage(missingFields, settings.missingInfoTemplate)
            sendSmsReply(context, sender, reply)
            Log.d(TAG, "Demande infos manquantes envoyée: $missingFields")
        } else {
            val confirmationMsg = formatConfirmationMessage(settings, rideToSave)
            sendSmsReply(context, sender, confirmationMsg)
            Log.d(TAG, "Confirmation envoyée: ${rideToSave.departure} → ${rideToSave.arrival}")

            if (settings.enableNotifications) {
                NotificationHelper.showNewRideNotification(
                    context,
                    rideToSave.id,
                    rideToSave.arrival,
                    rideToSave.time
                )
            }
        }
    }

    private suspend fun processNewOrUpdatedRideWithAI(
        context: Context,
        sender: String,
        body: String,
        aiResult: AiParsedResult,
        timestamp: Long,
        existingRide: com.drawtaxi.app.data.RideRequest?,
        repository: com.drawtaxi.app.data.TaxiRepository,
        settings: com.drawtaxi.app.data.AppSettings
    ) {
        val hasValidInfo = aiResult.departure.isNotBlank() ||
                          aiResult.arrival.isNotBlank() ||
                          aiResult.time.isNotBlank()

        if (!hasValidInfo && !isTaxiRelated(body)) {
            Log.d(TAG, "SMS ignoré (non taxi et sans info): $sender")
            return
        }

        val rideToSave: com.drawtaxi.app.data.RideRequest

        if (existingRide != null) {
            val mergedDeparture = mergeField(aiResult.departure, existingRide.departure)
            val mergedArrival = mergeField(aiResult.arrival, existingRide.arrival)
            val mergedTime = mergeField(aiResult.time, existingRide.time)

            rideToSave = existingRide.copy(
                departure = mergedDeparture,
                arrival = mergedArrival,
                time = mergedTime,
                body = existingRide.body + "\n--- NOUVEAU MESSAGE ---\n$body",
                timestamp = System.currentTimeMillis()
            )

            repository.updateRide(rideToSave)
            Log.d(TAG, "Course mise à jour (AI): ${rideToSave.id}")
        } else {
            val ride = aiResult.toRideRequest(sender, timestamp, settings)
            if (ride == null) {
                Log.d(TAG, "AI n'a pas pu extraire d'informations de course")
                return
            }
            rideToSave = ride.copy(
                body = body,
                hasMissingInfo = aiResult.missingFields.isNotEmpty(),
                missingFieldsList = aiResult.missingFields.joinToString(",")
            )
            repository.saveRide(rideToSave)
            Log.d(TAG, "Nouvelle course créée (AI): ${rideToSave.id}")
        }

        val missingFields = aiResult.missingFields.ifEmpty { extractMissingFieldsAi(aiResult, rideToSave) }

        if (missingFields.isNotEmpty()) {
            val reply = formatMissingFieldsMessage(missingFields, settings.missingInfoTemplate)
            sendSmsReply(context, sender, reply)
            Log.d(TAG, "Demande infos manquantes envoyée: $missingFields")
        } else {
            val confirmationMsg = formatConfirmationMessage(settings, rideToSave)
            sendSmsReply(context, sender, confirmationMsg)
            Log.d(TAG, "Confirmation envoyée (AI): ${rideToSave.departure} → ${rideToSave.arrival}")

            if (settings.enableNotifications) {
                NotificationHelper.showNewRideNotification(
                    context,
                    rideToSave.id,
                    rideToSave.arrival,
                    rideToSave.time
                )
            }
        }
    }

    private fun extractMissingFieldsAi(aiResult: AiParsedResult, existing: com.drawtaxi.app.data.RideRequest? = null): List<String> {
        val missing = mutableListOf<String>()
        
        val departure = aiResult.departure.ifBlank { existing?.departure ?: "" }
        val arrival = aiResult.arrival.ifBlank { existing?.arrival ?: "" }
        val time = aiResult.time.ifBlank { existing?.time ?: "" }
        
        if (departure.isBlank() || departure == "Inconnu") {
            missing.add("le lieu de départ")
        }
        if (arrival.isBlank() || arrival == "Inconnu") {
            missing.add("la destination")
        }
        if (time.isBlank() || time == "Dès que possible" || time == "maintenant") {
            missing.add("l'heure")
        }
        
        return missing
    }

    private fun mergeField(newValue: String, existingValue: String): String {
        return when {
            newValue.isNotBlank() && existingValue.isBlank() -> newValue
            newValue.isNotBlank() && existingValue.isNotBlank() && newValue != existingValue -> newValue
            existingValue.isNotBlank() -> existingValue
            else -> newValue
        }
    }

    private fun sendSmsReply(context: Context, phoneNumber: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission SEND_SMS non accordée")
                return
            }

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d(TAG, "SMS envoyé à $phoneNumber: ${message.take(30)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoi SMS: ${e.message}", e)
        }
    }
}
