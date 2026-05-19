package com.drawtaxi.app.logic.pricing

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.messaging.MessageSender
import com.drawtaxi.app.logic.sms.AiParsedResult

object QuoteResponseHandler {
    private const val TAG = "QuoteResponseHandler"

    suspend fun handleResponse(
        context: Context,
        repository: TaxiRepository,
        sender: String,
        body: String,
        aiParsedResult: AiParsedResult
    ): Boolean {
        if (!aiParsedResult.isConfirmation && !aiParsedResult.isCancellation && !aiParsedResult.isModification) {
            return false
        }

        // Chercher le devis correspondant (dernier devis envoyé à ce numéro)
        val pendingQuotes = repository.getPendingQuotesForSender(sender)
        if (pendingQuotes.isEmpty()) {
            Log.d(TAG, "Aucun devis en attente pour $sender")
            return false
        }

        val quote = pendingQuotes.first()
        val ride = repository.getRideById(quote.rideId)

        if (ride == null) {
            Log.d(TAG, "Course non trouvée pour le devis ${quote.id}")
            return false
        }

        return when {
            aiParsedResult.isConfirmation -> {
                Log.d(TAG, "Confirmation reçue pour la course ${ride.id}")
                // Mettre à jour le statut du devis
                repository.updateQuoteStatus(quote.id, com.drawtaxi.app.data.QuoteStatus.ACCEPTED)
                // Confirmer la course et la passer à isPending=false pour l'afficher dans l'accueil
                val confirmedRide = ride.copy(
                    status = RideStatus.CONFIRMED,
                    price = quote.price,
                    isPending = false
                )
                repository.updateRide(confirmedRide)
                // Envoyer une confirmation au client
                MessageSender.sendSms(
                    context,
                    sender,
                    "Votre course a été confirmée. Merci pour votre confiance !"
                )
                true
            }
            aiParsedResult.isCancellation -> {
                Log.d(TAG, "Refus reçu pour la course ${ride.id}")
                // Mettre à jour le statut du devis
                repository.updateQuoteStatus(quote.id, com.drawtaxi.app.data.QuoteStatus.REJECTED)
                // Supprimer la course
                repository.deleteRide(ride)
                // Envoyer un message au client
                MessageSender.sendSms(
                    context,
                    sender,
                    "Votre demande a été annulée. N'hésitez pas à nous recontacter."
                )
                true
            }
            aiParsedResult.isModification -> {
                Log.d(TAG, "Modification demandée pour la course ${ride.id}")
                // Mettre à jour les infos si présentes dans la réponse
                val updatedRide = ride.copy(
                    departure = aiParsedResult.departure.ifBlank { ride.departure },
                    arrival = aiParsedResult.arrival.ifBlank { ride.arrival },
                    time = aiParsedResult.time.ifBlank { ride.time },
                    date = aiParsedResult.date.ifBlank { ride.date }
                )
                repository.updateRide(updatedRide)
                true
            }
            else -> false
        }
    }
}
