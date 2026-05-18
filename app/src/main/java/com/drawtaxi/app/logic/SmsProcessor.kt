package com.drawtaxi.app.logic

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.TaxiRepository

object SmsProcessor {

    private const val TAG = "SmsProcessor"

    data class ProcessResult(
        val action: Action,
        val ride: RideRequest?,
        val notificationTitle: String? = null,
        val notificationBody: String? = null
    )

    enum class Action {
        NEW_RIDE,
        RIDE_UPDATED,
        RIDE_DELETED,
        DUPLICATE_IGNORED,
        NO_ACTION
    }

    suspend fun processSms(
        context: Context,
        repository: TaxiRepository,
        address: String,
        body: String,
        timestamp: Long
    ): ProcessResult {
        Log.d(TAG, "Traitement SMS de $address: ${body.take(50)}")

        try {
            val settings = repository.getSettingsSync()
            if (!settings.monitorSms) {
                Log.d(TAG, "Surveillance SMS désactivée, SMS ignoré")
                return ProcessResult(Action.NO_ACTION, null)
            }

            // Utiliser l'IA si disponible
            val aiResult = AiSmsParser.parseWithAI(context, body, settings.aiEnabled)
            Log.d(TAG, "Résultat AI: departure=${aiResult.departure}, arrival=${aiResult.arrival}, time=${aiResult.time}, confidence=${aiResult.confidence}")
            
            // Vérifier si c'est une réponse à un devis (confirmation/refus/modification)
            val isQuoteResponse = QuoteResponseHandler.handleResponse(
                context,
                repository,
                address,
                body,
                aiResult
            )
            if (isQuoteResponse) {
                Log.d(TAG, "Réponse à un devis traitée")
                return ProcessResult(Action.RIDE_UPDATED, null)
            }

            if (aiResult.isCancellation) {
                return handleCancellation(context, repository, address, body)
            }

            val pendingList = repository.getPendingRidesList()
            val matchInfo = RideMatcher.matchSmsToRides(address, body, pendingList)

            Log.d(TAG, "Analyse SMS: ${matchInfo.result} - ${matchInfo.reason}")

            return when (matchInfo.result) {
                RideMatchResult.DELETION -> {
                    handleDeletion(repository, matchInfo.matchedRide)
                }

                RideMatchResult.MODIFICATION -> {
                    handleModification(repository, matchInfo.matchedRide, address, body, timestamp)
                }

                RideMatchResult.ADDITION -> {
                    handleNewRideWithAI(context, repository, address, body, timestamp, aiResult, settings)
                }

                RideMatchResult.CLARIFICATION -> {
                    handleClarification(repository, matchInfo.matchedRide, address, body, timestamp)
                }

                RideMatchResult.DUPLICATE -> {
                    Log.d(TAG, "Doublon détecté, ignoré")
                    ProcessResult(Action.DUPLICATE_IGNORED, null)
                }

                RideMatchResult.NEW_RIDE -> {
                    handleNewRideWithAI(context, repository, address, body, timestamp, aiResult, settings)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur traitement SMS: ${e.message}", e)
            return ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleNewRideWithAI(
        context: Context,
        repository: TaxiRepository,
        address: String,
        body: String,
        timestamp: Long,
        aiResult: AiParsedResult,
        settings: com.drawtaxi.app.data.AppSettings
    ): ProcessResult {
        val ride = aiResult.toRideRequest(address, timestamp, settings)
        return if (ride != null) {
            val missingFields = aiResult.missingFields
            val hasMissingInfo = missingFields.isNotEmpty()
            val updatedRide = ride.copy(
                hasMissingInfo = hasMissingInfo,
                missingFieldsList = missingFields.joinToString(",")
            )
            repository.saveRide(updatedRide)
            Log.d(TAG, "Nouvelle course créée (AI): ${updatedRide.id} (infos manquantes: $hasMissingInfo)")
            ProcessResult(
                action = Action.NEW_RIDE,
                ride = updatedRide,
                notificationTitle = "Nouvelle course",
                notificationBody = "${updatedRide.departure} → ${updatedRide.arrival}"
            )
        } else {
            Log.d(TAG, "AI n'a pas pu extraire d'informations de course")
            ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleCancellation(
        context: Context,
        repository: TaxiRepository,
        address: String,
        body: String
    ): ProcessResult {
        val pendingList = repository.getPendingRidesList()
        val matchingRide = pendingList
            .filter { it.sender == address }
            .maxByOrNull { it.timestamp }

        return if (matchingRide != null) {
            repository.deleteRide(matchingRide)
            Log.d(TAG, "Course annulée via SMS: ${matchingRide.id}")
            ProcessResult(
                action = Action.RIDE_DELETED,
                ride = matchingRide,
                notificationTitle = "Course annulée",
                notificationBody = "Course supprimée: ${matchingRide.departure} → ${matchingRide.arrival}"
            )
        } else {
            Log.d(TAG, "Message d'annulation sans course correspondante, ignoré")
            ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleDeletion(
        repository: TaxiRepository,
        matchedRide: RideRequest?
    ): ProcessResult {
        return if (matchedRide != null) {
            repository.deleteRide(matchedRide)
            Log.d(TAG, "Course supprimée: ${matchedRide.id}")
            ProcessResult(
                action = Action.RIDE_DELETED,
                ride = matchedRide,
                notificationTitle = "Course annulée",
                notificationBody = "Course supprimée: ${matchedRide.departure} → ${matchedRide.arrival}"
            )
        } else {
            ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleModification(
        repository: TaxiRepository,
        matchedRide: RideRequest?,
        address: String,
        body: String,
        timestamp: Long
    ): ProcessResult {
        return if (matchedRide != null) {
            val parsedRide = parseSms(address, body, timestamp)
            if (parsedRide != null) {
                val updatedRide = matchedRide.copy(
                    departure = parsedRide.departure.ifBlank { matchedRide.departure },
                    arrival = parsedRide.arrival.ifBlank { matchedRide.arrival },
                    time = parsedRide.time.ifBlank { matchedRide.time },
                    body = matchedRide.body + "\n--- MODIFICATION ---\n" + body
                )
                repository.updateRide(updatedRide)
                Log.d(TAG, "Course modifiée: ${updatedRide.id}")
                ProcessResult(
                    action = Action.RIDE_UPDATED,
                    ride = updatedRide,
                    notificationTitle = "Course modifiée",
                    notificationBody = "${updatedRide.departure} → ${updatedRide.arrival}"
                )
            } else {
                ProcessResult(Action.NO_ACTION, null)
            }
        } else {
            ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleNewRide(
        repository: TaxiRepository,
        address: String,
        body: String,
        timestamp: Long
    ): ProcessResult {
        val parsedRide = parseSms(address, body, timestamp)
        return if (parsedRide != null) {
            val missingFields = parsedRide.missingFieldsList.split(",").filter { it.isNotBlank() }
            val hasMissingInfo = missingFields.isNotEmpty()
            val updatedRide = parsedRide.copy(
                hasMissingInfo = hasMissingInfo,
                missingFieldsList = missingFields.joinToString(",")
            )
            repository.saveRide(updatedRide)
            Log.d(TAG, "Nouvelle course créée: ${updatedRide.id} (infos manquantes: $hasMissingInfo)")
            ProcessResult(
                action = Action.NEW_RIDE,
                ride = updatedRide,
                notificationTitle = "Nouvelle course",
                notificationBody = "${updatedRide.departure} → ${updatedRide.arrival}"
            )
        } else {
            ProcessResult(Action.NO_ACTION, null)
        }
    }

    private suspend fun handleClarification(
        repository: TaxiRepository,
        matchedRide: RideRequest?,
        address: String,
        body: String,
        timestamp: Long
    ): ProcessResult {
        return if (matchedRide != null) {
            val parsedRide = parseSms(address, body, timestamp)
            val updatedRide = if (parsedRide != null && (parsedRide.departure.isNotBlank() ||
                parsedRide.arrival.isNotBlank() || parsedRide.time.isNotBlank())) {
                matchedRide.copy(
                    departure = parsedRide.departure.ifBlank { matchedRide.departure },
                    arrival = parsedRide.arrival.ifBlank { matchedRide.arrival },
                    time = parsedRide.time.ifBlank { matchedRide.time },
                    body = matchedRide.body + "\n--- REPONSE ---\n" + body
                )
            } else {
                matchedRide.copy(
                    body = matchedRide.body + "\n--- REPONSE ---\n" + body
                )
            }
            repository.updateRide(updatedRide)
            Log.d(TAG, "Réponse ajoutée à la course: ${matchedRide.id}")
            ProcessResult(
                action = Action.RIDE_UPDATED,
                ride = updatedRide
            )
        } else {
            ProcessResult(Action.NO_ACTION, null)
        }
    }
}
