package com.drawtaxi.app.logic

import android.util.Log

/**
 * Parser pour les emails envoyés par le formulaire web de réservation
 * Format attendu :
 * - Nom complet
 * - Téléphone  
 * - Email
 * - Service (type de prestation)
 * - Date
 * - Adresse de départ
 * - Adresse de destination
 * - Heure
 * - Message (détails)
 */
object EmailFormParser {
    
    private const val TAG = "EmailFormParser"
    
    data class ParsedFormData(
        val name: String = "",
        val phone: String = "",
        val email: String = "",
        val service: String = "",
        val date: String = "",
        val departure: String = "",
        val destination: String = "",
        val time: String = "",
        val message: String = "",
        val passengerCount: Int = 1
    )
    
    /**
     * Parse un email contenant les données du formulaire web
     */
    fun parseEmailBody(body: String): ParsedFormData {
        Log.d(TAG, "Parsing email body: ${body.take(200)}...")
        
        return ParsedFormData(
            name = extractField(body, listOf(
                "Nom complet[\\s:]",
                "Nom[\\s:]",
                "Name[\\s:]"
            )),
            phone = extractField(body, listOf(
                "Téléphone[\\s:]",
                "Telephone[\\s:]",
                "Tél[\\s:]",
                "Phone[\\s:]",
                "T[èe]l[èe]phone"
            )),
            email = extractEmail(body),
            service = extractField(body, listOf(
                "Prestation[\\s:]",
                "Service[\\s:]",
                "Type de course[\\s:]"
            )),
            date = extractDate(body),
            departure = extractField(body, listOf(
                "Adresse de départ[\\s:]",
                "Départ[\\s:]",
                "Depart[\\s:]",
                "From[\\s:]",
                "Pickup[\\s:]"
            )),
            destination = extractField(body, listOf(
                "Adresse de destination[\\s:]",
                "Destination[\\s:]",
                "Arriv[èe]e[\\s:]",
                "To[\\s:]",
                "Dropoff[\\s:]"
            )),
            time = extractTime(body),
            message = extractField(body, listOf(
                "Détails du trajet[\\s:]",
                "Détails[\\s:]",
                "Message[\\s:]",
                "Commentaires[\\s:]",
                "Informations complémentaires[\\s:]"
            )),
            passengerCount = extractPassengerCount(body)
        )
    }
    
    /**
     * Extrait un champ avec différents patterns possibles
     */
    private fun extractField(body: String, patterns: List<String>): String {
        for (pattern in patterns) {
            // Pattern: Label: valeur ou Label : valeur (jusqu'à la fin de ligne ou double saut de ligne)
            val regexString = """$pattern\s*[:-]?\s*([^\n]+?)(?=\n\n|\n[A-Z]|\r\n\r\n|\$)"""
            val regex = Regex(regexString, RegexOption.IGNORE_CASE)
            val match = regex.find(body)
            if (match != null) {
                val value = match.groupValues[1].trim()
                if (value.isNotBlank() && !value.contains("Sélectionnez", ignoreCase = true)) {
                    return value
                }
            }
        }
        return ""
    }
    
    /**
     * Extrait l'email du corps ou de l'en-tête
     */
    private fun extractEmail(body: String): String {
        // Pattern standard email
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val match = emailRegex.find(body)
        return match?.value ?: ""
    }
    
    /**
     * Extrait la date au format dd/MM/yyyy ou yyyy-MM-dd
     */
    private fun extractDate(body: String): String {
        // Pattern pour date formatée (dd/MM/yyyy ou dd-MM-yyyy)
        val datePatterns = listOf(
            Regex("""(\d{2})[/-](\d{2})[/-](\d{4})"""),
            Regex("""(\d{4})[/-](\d{2})[/-](\d{2})""")
        )
        
        for (pattern in datePatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val groups = match.groupValues
                return if (groups[1].length == 4) {
                    // Format yyyy-MM-dd
                    "${groups[3]}/${groups[2]}/${groups[1]}"
                } else {
                    // Format dd/MM/yyyy déjà
                    "${groups[1]}/${groups[2]}/${groups[3]}"
                }
            }
        }
        
        // Chercher près du label "Date"
        val dateField = extractField(body, listOf("Date"))
        if (dateField.isNotBlank()) {
            // Essayer de parser
            val dateMatch = Regex("(\\d{2})[/-](\\d{2})[/-](\\d{4})").find(dateField)
            if (dateMatch != null) {
                return "${dateMatch.groupValues[1]}/${dateMatch.groupValues[2]}/${dateMatch.groupValues[3]}"
            }
        }
        
        return ""
    }
    
    /**
     * Extrait l'heure au format HH:mm ou HHhmm
     */
    private fun extractTime(body: String): String {
        // Pattern pour heure
        val timePatterns = listOf(
            Regex("""(\d{1,2})[h:](\d{2})"""),
            Regex("""Heure[\s:]+(\d{1,2})[h:](\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""Horaire[\s:]+(\d{1,2})[h:](\d{2})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in timePatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val hour = match.groupValues[1].padStart(2, '0')
                val minute = match.groupValues[2]
                return "${hour}h${minute}"
            }
        }
        
        // Chercher près du label "Heure"
        val timeField = extractField(body, listOf("Heure", "Horaire", "Time"))
        if (timeField.isNotBlank()) {
            val timeMatch = Regex("(\\d{1,2})[h:](\\d{2})").find(timeField)
            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].padStart(2, '0')
                return "${hour}h${timeMatch.groupValues[2]}"
            }
        }
        
        return ""
    }
    
    /**
     * Extrait le nombre de passagers du message
     */
    private fun extractPassengerCount(body: String): Int {
        // Chercher "X passagers" ou "X personnes"
        val patterns = listOf(
            Regex("""(\d+)\s*passager""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*personne""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*pax""", RegexOption.IGNORE_CASE),
            Regex("""nombre de passagers?\s*[:\-]?\s*(\d+)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 1
            }
        }
        
        return 1
    }
    
    /**
     * Détermine si le service est un aller ou un retour basé sur le type de prestation
     */
    fun isAirportArrival(service: String): Boolean {
        return service.contains("arrivée", ignoreCase = true) || 
               service.contains("arrivee", ignoreCase = true) ||
               service.contains("depuis", ignoreCase = true) ||
               service.contains("from", ignoreCase = true)
    }
    
    fun isAirportDeparture(service: String): Boolean {
        return service.contains("départ", ignoreCase = true) || 
               service.contains("depart", ignoreCase = true) ||
               service.contains("vers", ignoreCase = true) ||
               service.contains("to", ignoreCase = true)
    }
    
    /**
     * Convertit les données parsées en objet AiParsedResult pour créer une course
     */
    fun toAiParsedResult(parsed: ParsedFormData): AiParsedResult {
        // Déterminer départ et arrivée selon le type de service
        val (departure, arrival) = when {
            parsed.service.contains("aéroport", ignoreCase = true) || 
            parsed.service.contains("aeroport", ignoreCase = true) -> {
                if (isAirportArrival(parsed.service)) {
                    "Aéroport" to parsed.destination
                } else {
                    parsed.departure to "Aéroport"
                }
            }
            parsed.service.contains("gare", ignoreCase = true) -> {
                if (isAirportArrival(parsed.service)) {
                    "Gare" to parsed.destination
                } else {
                    parsed.departure to "Gare"
                }
            }
            else -> parsed.departure to parsed.destination
        }
        
        return AiParsedResult(
            departure = departure,
            arrival = arrival,
            time = parsed.time,
            date = parsed.date,
            clientName = parsed.name,
            clientFirstName = parsed.name.split(" ").firstOrNull() ?: "",
            phone = parsed.phone,
            email = parsed.email,
            nbPassengers = parsed.passengerCount,
            isConfirmation = false,
            isCancellation = false,
            isModification = false,
            confidence = 0.9f, // Haute confiance car format structuré
            aiReasoning = "Parsed from web form email"
        )
    }
}
