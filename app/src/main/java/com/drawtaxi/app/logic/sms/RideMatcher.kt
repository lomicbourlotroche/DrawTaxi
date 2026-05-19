package com.drawtaxi.app.logic.sms

import com.drawtaxi.app.data.RideRequest

enum class RideMatchResult {
    NEW_RIDE,
    DUPLICATE,
    MODIFICATION,
    ADDITION,
    DELETION,
    CLARIFICATION
}

data class RideMatchInfo(
    val result: RideMatchResult,
    val matchedRide: RideRequest?,
    val confidence: Float,
    val reason: String
)

object RideMatcher {

    private val deleteKeywords = listOf(
        "annule", "annuler", "annulation", "supprime", "supprimer", "suppression",
        "efface", "effacer", "désist", "desist", "abandon",
        "ne vient pas", "ne peut pas", "cancel", "cancelled", "delete",
        "stop", "stopped", "stopper", "ne plus", "stoppé"
    )

    private val modifyKeywords = listOf(
        "modifie", "modifier", "change", "changer", "changement", "rectifie",
        "rectifier", "corrige", "corriger", "mauvais", "erreur", "faux",
        "invers", "swap", "different", "autre", "nouveau", "nouvelle",
        "mise à jour", "mise a jour", "update", "instead", "actually"
    )

    private val addKeywords = listOf(
        "ajoute", "ajouter", "rajoute", "rajouter", "ajout", "en plus",
        "aussi", "additional", "plus", "another", "second", "autre course",
        "une autre", "2eme", "deuxieme", "second", "deux"
    )

    private val clarificationKeywords = listOf(
        "c'est", "ces", "c'est quoi", "confirme", "confirmation",
        "merci", "ok", "okay", "compris", "entendu", "reçu", "recus",
        "bien reçu", "bien recu", "roger", "yes", "no", "oui", "non",
        "est ce", "est-ce", "peut etre", "peut-etre", "peut être"
    )

    fun matchSmsToRides(
        sender: String,
        body: String,
        pendingRides: List<RideRequest>
    ): RideMatchInfo {
        val lowerBody = body.lowercase()
        
        val sameSenderRides = pendingRides.filter { it.sender == sender }
        
        if (sameSenderRides.isEmpty()) {
            return RideMatchInfo(
                result = RideMatchResult.NEW_RIDE,
                matchedRide = null,
                confidence = 1.0f,
                reason = "Nouveau client"
            )
        }

        if (containsKeyword(lowerBody, deleteKeywords)) {
            val matchedRide = findBestMatch(sender, body, sameSenderRides)
            return RideMatchInfo(
                result = RideMatchResult.DELETION,
                matchedRide = matchedRide,
                confidence = if (matchedRide != null) 0.9f else 0.7f,
                reason = if (matchedRide != null) "Suppression demandée" else "Suppression détectée"
            )
        }

        if (containsKeyword(lowerBody, modifyKeywords)) {
            val matchedRide = findBestMatch(sender, body, sameSenderRides)
            return RideMatchInfo(
                result = RideMatchResult.MODIFICATION,
                matchedRide = matchedRide,
                confidence = if (matchedRide != null) 0.85f else 0.6f,
                reason = if (matchedRide != null) "Modification de la course" else "Modification détectée"
            )
        }

        if (containsKeyword(lowerBody, addKeywords)) {
            return RideMatchInfo(
                result = RideMatchResult.ADDITION,
                matchedRide = null,
                confidence = 0.9f,
                reason = "Demande de course supplémentaire"
            )
        }

        if (containsKeyword(lowerBody, clarificationKeywords) && !containsTaxiKeyword(lowerBody)) {
            val matchedRide = findBestMatch(sender, body, sameSenderRides)
            return RideMatchInfo(
                result = RideMatchResult.CLARIFICATION,
                matchedRide = matchedRide,
                confidence = if (matchedRide != null) 0.7f else 0.4f,
                reason = "Message de confirmation/clarification"
            )
        }

        val matchedRide = findBestMatch(sender, body, sameSenderRides)
        if (matchedRide != null && isDuplicate(matchedRide, body)) {
            return RideMatchInfo(
                result = RideMatchResult.DUPLICATE,
                matchedRide = matchedRide,
                confidence = 0.95f,
                reason = "Doublon détecté"
            )
        }

        val hasNewLocation = containsNewLocation(lowerBody, sameSenderRides)
        if (hasNewLocation) {
            return RideMatchInfo(
                result = RideMatchResult.NEW_RIDE,
                matchedRide = null,
                confidence = 0.8f,
                reason = "Nouvelle destination"
            )
        }

        return RideMatchInfo(
            result = RideMatchResult.NEW_RIDE,
            matchedRide = null,
            confidence = 0.6f,
            reason = "Course possiblement similaire"
        )
    }

    private fun containsKeyword(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword ->
            if (keyword.endsWith(" ")) {
                text.contains(keyword)
            } else {
                text.contains(keyword) && (
                    text.indexOf(keyword) == 0 ||
                    text.indexOf(keyword) + keyword.length == text.length ||
                    text[text.indexOf(keyword) - 1].isWhitespace() ||
                    text[text.indexOf(keyword) + keyword.length].isWhitespace()
                )
            }
        }
    }

    private fun containsTaxiKeyword(text: String): Boolean {
        return text.contains("taxi") || 
               text.contains("course") || 
               text.contains("aller") ||
               text.contains("départ") ||
               text.contains("depart")
    }

    private fun findBestMatch(
        sender: String,
        body: String,
        sameSenderRides: List<RideRequest>
    ): RideRequest? {
        val lowerBody = body.lowercase()
        
        var bestMatch: RideRequest? = null
        var bestScore = 0f

        for (ride in sameSenderRides) {
            val score = calculateMatchScore(ride, lowerBody)
            if (score > bestScore && score > 0.3f) {
                bestScore = score
                bestMatch = ride
            }
        }

        return bestMatch
    }

    private fun calculateMatchScore(ride: RideRequest, newBody: String): Float {
        var score = 0f
        var totalWeight = 0f

        if (ride.departure.isNotBlank()) {
            totalWeight += 1f
            if (newBody.contains(ride.departure.lowercase())) {
                score += 1f
            }
        }

        if (ride.arrival.isNotBlank()) {
            totalWeight += 1f
            if (newBody.contains(ride.arrival.lowercase())) {
                score += 1f
            }
        }

        if (ride.time.isNotBlank()) {
            totalWeight += 0.5f
            if (newBody.contains(ride.time.lowercase())) {
                score += 0.5f
            }
        }

        if (ride.body.lowercase().split(" ").any { word -> 
            word.length > 3 && newBody.contains(word) 
        }) {
            score += 0.5f
            totalWeight += 0.5f
        }

        return if (totalWeight > 0f) score / totalWeight else 0f
    }

    private fun isDuplicate(ride: RideRequest, newBody: String): Boolean {
        val similarity = calculateSimilarity(ride.body.lowercase(), newBody.lowercase())
        return similarity > 0.7f
    }

    private fun calculateSimilarity(text1: String, text2: String): Float {
        if (text1 == text2) return 1f
        
        val words1 = text1.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val words2 = text2.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union.toFloat()
    }

    private fun containsNewLocation(
        newBody: String,
        existingRides: List<RideRequest>
    ): Boolean {
        val departureKeywords = listOf("depuis", "de ", "adresse", "depart", "départ")
        val arrivalKeywords = listOf("vers", "pour ", "destination", "arrivée")
        
        val hasDeparture = departureKeywords.any { newBody.contains(it) }
        val hasArrival = arrivalKeywords.any { newBody.contains(it) }
        
        if (!hasDeparture && !hasArrival) return false

        for (ride in existingRides) {
            if (hasDeparture && ride.departure.isNotBlank()) {
                if (!newBody.contains(ride.departure.lowercase())) {
                    return true
                }
            }
            if (hasArrival && ride.arrival.isNotBlank()) {
                if (!newBody.contains(ride.arrival.lowercase())) {
                    return true
                }
            }
        }

        return false
    }
}
