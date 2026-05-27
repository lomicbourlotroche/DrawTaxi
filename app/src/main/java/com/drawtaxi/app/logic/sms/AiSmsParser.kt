package com.drawtaxi.app.logic.sms

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.logic.ai.NexaEngine
import com.drawtaxi.app.logic.pricing.RideCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.text.Regex
import kotlin.text.RegexOption

data class AiParsedResult(
    val departure: String = "",
    val arrival: String = "",
    val time: String = "",
    val date: String = "",
    val clientName: String = "",
    val clientFirstName: String = "",
    val phone: String = "",
    val email: String = "",
    val nbPassengers: Int = 1,
    val price: Double = 0.0,
    val isConfirmation: Boolean = false,
    val isCancellation: Boolean = false,
    val isModification: Boolean = false,
    val confidence: Float = 0f,
    val missingFields: List<String> = emptyList(),
    val aiReasoning: String = ""
) {
    fun toRideRequest(
        sender: String,
        timestamp: Long = System.currentTimeMillis(),
        settings: AppSettings? = null,
        extraBody: String = "",
        messageChannel: com.drawtaxi.app.data.MessageChannel = com.drawtaxi.app.data.MessageChannel.SMS
    ): RideRequest? {
        if (departure.isBlank() && arrival.isBlank() && time.isBlank()) {
            return null
        }

        val estimatedDistance = estimateDistance()

        val finalPrice = if (price > 0) price else {
            settings?.let { RideCalculator.calculatePrice(estimatedDistance, it) } ?: 0.0
        }

        val estimatedDuration = estimateDuration(estimatedDistance)
        val distanceDomicileEst = estimatedDistance * 0.3
        val fuelCost = settings?.let { distanceDomicileEst * it.coutParKmDeplacement } ?: 0.0
        val profitability = RideRequest.calculateProfitability(finalPrice, fuelCost)

        val fullClientName = listOf(clientFirstName, clientName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val rideStatus = when {
            isCancellation -> RideStatus.CANCELLED
            isConfirmation -> RideStatus.CONFIRMED
            else -> RideStatus.DRAFT
        }

        return RideRequest(
            id = RideRequest.createStableId(sender, "$departure $arrival $time", timestamp),
            sender = phone.ifBlank { sender },
            body = extraBody,
            departure = departure,
            arrival = arrival,
            time = time,
            date = date,
            distanceKm = estimatedDistance,
            price = finalPrice,
            durationMinutes = estimatedDuration,
            fuelCost = fuelCost,
            operatingCost = 0.0,
            profitabilityPercent = profitability,
            clientName = fullClientName,
            clientFirstName = clientFirstName,
            clientLastName = clientName,
            clientPhone = phone,
            clientEmail = email,
            messageChannel = messageChannel,
            timestamp = timestamp,
            status = rideStatus
        )
    }

    internal fun estimateDistance(): Double {
        if (departure.isBlank() || arrival.isBlank()) return 0.0

        val depLower = departure.lowercase()
        val arrLower = arrival.lowercase()
        val parisKeywords = listOf("paris", "cdg", "orly", "gare", "chatelet", "opera", "defense")

        val depIsParis = parisKeywords.any { depLower.contains(it) }
        val arrIsParis = parisKeywords.any { arrLower.contains(it) }

        return when {
            depIsParis && arrIsParis -> 10.0
            depIsParis || arrIsParis -> 25.0
            else -> 25.0
        }
    }

    private fun estimateDuration(distanceKm: Double): Int {
        if (distanceKm <= 0) return 0
        val avgSpeed = if (distanceKm < 10) 30.0 else 50.0
        return ((distanceKm / avgSpeed) * 60).toInt()
    }
}

object AiSmsParser {

    private const val TAG = "AiSmsParser"
    private const val MAX_RETRIES = 3
    private const val TIMEOUT_SECONDS = 90

    private val REQUIRED_JSON_FIELDS = setOf("departure", "arrival", "time")

    private val taxiClassificationPrompt = """
Tu es un assistant qui classe les SMS. Réponds UNIQUEMENT par "TAXI" ou "NON_TAXI".

Un SMS est "TAXI" s'il concerne :
- Une réservation de taxi, VTC, Uber, Bolt, Heetch
- Une demande de transport, course, trajet
- Une confirmation, annulation ou modification de course
- Un rendez-vous pour un chauffeur

Un SMS est "NON_TAXI" s'il concerne :
- Un message personnel, familial, amical
- Une promotion, publicité, spam
- Un code de vérification, OTP
- Un message de banque, opérateur, administration
- Tout autre sujet non lié au transport

SMS à classer : "{sms}"
""".trimIndent()

    private val aiSystemPrompt = """
Tu es un assistant qui extrait des informations de SMS de réservation de taxi.
Réponds UNIQUEMENT en JSON valide avec cette structure exacte :
{
  "departure": "lieu de départ",
  "arrival": "destination",
  "time": "heure (format HHhMM ou HH:MM)",
  "date": "date (dd/MM/yyyy ou aujourd'hui/demain)",
  "clientName": "nom de famille",
  "clientFirstName": "prénom",
  "phone": "numéro de téléphone",
  "email": "email",
  "nbPassengers": 1,
  "price": 0.0,
  "isConfirmation": false,
  "isCancellation": false,
  "isModification": false,
  "confidence": 0.8,
  "reasoning": "brève explication de l'analyse"
}

EXEMPLES :
SMS : "Bonjour j'ai besoin d'un taxi pour aller de Paris à Orly demain à 8h. Jean Dupont 0612345678"
{
  "departure": "Paris",
  "arrival": "Orly",
  "time": "8h",
  "date": "demain",
  "clientName": "Dupont",
  "clientFirstName": "Jean",
  "phone": "0612345678",
  "email": "",
  "nbPassengers": 1,
  "price": 0.0,
  "isConfirmation": false,
  "isCancellation": false,
  "isModification": false,
  "confidence": 0.95,
  "reasoning": "Demande de taxi Paris vers Orly demain 8h avec nom et téléphone"
}

SMS : "Finalement annule la course pour demain merci"
{
  "departure": "",
  "arrival": "",
  "time": "",
  "date": "demain",
  "clientName": "",
  "clientFirstName": "",
  "phone": "",
  "email": "",
  "nbPassengers": 1,
  "price": 0.0,
  "isConfirmation": false,
  "isCancellation": true,
  "isModification": false,
  "confidence": 0.9,
  "reasoning": "Le mot annule indique une annulation de course"
}

SMS : "Je modifie l'adresse de départ ce sera la gare de l'Est"
{
  "departure": "Gare de l'Est",
  "arrival": "",
  "time": "",
  "date": "",
  "clientName": "",
  "clientFirstName": "",
  "phone": "",
  "email": "",
  "nbPassengers": 1,
  "price": 0.0,
  "isConfirmation": false,
  "isCancellation": false,
  "isModification": true,
  "confidence": 0.85,
  "reasoning": "Modification du lieu de départ vers la Gare de l'Est"
}

SMS : "Course pour 14h30 de la rue de Rivoli à l'aéroport CDG"
{
  "departure": "Rue de Rivoli",
  "arrival": "Aéroport CDG",
  "time": "14h30",
  "date": "",
  "clientName": "",
  "clientFirstName": "",
  "phone": "",
  "email": "",
  "nbPassengers": 1,
  "price": 0.0,
  "isConfirmation": false,
  "isCancellation": false,
  "isModification": false,
  "confidence": 0.9,
  "reasoning": "Course de Rue de Rivoli à CDG à 14h30"
}

RÈGLES IMPORTANTES :
- "isCancellation" et "isConfirmation" ne peuvent JAMAIS être tous les deux true
- "isCancellation" et "isModification" ne peuvent JAMAIS être tous les deux true
- "isConfirmation" et "isModification" ne peuvent JAMAIS être tous les deux true
- Si le message n'est PAS une demande de course taxi, mets confidence < 0.3 et laisse departure/arrival vides
- Format heure : "14h30" ou "14h" (PAS "14:30", PAS "14 heures 30")
- Format date : "aujourd'hui", "demain", ou "dd/MM/yyyy"
- Ne mets JAMAIS "non spécifié" ou "inconnu" ou "n/a" → mets une chaîne vide ""
- Les prix en euros : mets le nombre (35.0) PAS "35€" ni "35 euros"

Si un champ n'est pas présent dans le SMS, utilise une chaîne vide "" ou 0.
""".trimIndent()

    suspend fun isTaxiRelated(context: Context, smsBody: String, aiEnabled: Boolean = true): Boolean {
        if (!aiEnabled) {
            return isTaxiRelatedRegex(smsBody)
        }

        if (!NexaEngine.isModelDownloaded(context)) {
            return isTaxiRelatedRegex(smsBody)
        }

        for (attempt in 1..MAX_RETRIES) {
            try {
                val prompt = taxiClassificationPrompt.replace("{sms}", smsBody)
                val response = runInferenceWithTimeout(context, prompt, 30)
                if (response.isNullOrBlank()) continue
                val cleaned = response.trim().uppercase()
                if (cleaned.contains("TAXI") && !cleaned.contains("NON_TAXI")) {
                    return true
                }
                return false
            } catch (e: Exception) {
                Log.w(TAG, "AI attempt $attempt failed: ${e.message}")
                if (attempt == MAX_RETRIES) {
                    return isTaxiRelatedRegex(smsBody)
                }
                kotlinx.coroutines.delay(500)
            }
        }
        return isTaxiRelatedRegex(smsBody)
    }

    suspend fun parseWithAI(context: Context, smsBody: String, aiEnabled: Boolean = true): AiParsedResult {
        // Intercept and parse Formspree emails immediately using dedicated regex parser
        val formspreeResult = parseFormspreeEmail(smsBody)
        if (formspreeResult != null) {
            Log.d(TAG, "Parsed Formspree email successfully using regex")
            return formspreeResult
        }

        if (!aiEnabled) {
            Log.d(TAG, "AI disabled, using regex fallback")
            return parseWithFallback(smsBody)
        }

        if (!NexaEngine.isModelDownloaded(context)) {
            Log.d(TAG, "AI model not downloaded, falling back to regex parser")
            return parseWithFallback(smsBody)
        }

        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "AI parsing attempt $attempt/$MAX_RETRIES")

                val result = runAiInference(context, smsBody)
                if (result != null) {
                    Log.d(TAG, "AI parsing successful: ${result.aiReasoning}")
                    return result
                }

                Log.w(TAG, "AI parsing attempt $attempt returned null")
            } catch (e: Exception) {
                Log.e(TAG, "AI parsing error on attempt $attempt: ${e.message}")
            }
        }

        Log.w(TAG, "All AI attempts failed, falling back to regex parser")
        return parseWithFallback(smsBody)
    }

    private suspend fun runAiInference(context: Context, smsBody: String): AiParsedResult? {
        return try {
            val prompt = "$aiSystemPrompt\n\nSMS à analyser :\n\"$smsBody\""

            val response = runInferenceWithTimeout(context, prompt, TIMEOUT_SECONDS)
            if (response.isNullOrBlank()) return null

            val jsonStr = extractJsonFromResponse(response)
            if (jsonStr.isNullOrBlank()) {
                Log.w(TAG, "No JSON found in LLM response. Raw: ${response.take(200)}")
                return null
            }

            val json = JSONObject(jsonStr)

            val departure = sanitizeField(json.optString("departure", ""))
            val arrival = sanitizeField(json.optString("arrival", ""))
            val time = sanitizeField(json.optString("time", ""))
            val date = sanitizeField(json.optString("date", ""))
            val clientName = sanitizeField(json.optString("clientName", ""))
            val clientFirstName = sanitizeField(json.optString("clientFirstName", ""))
            val phone = json.optString("phone", "").validatePhone()
            val email = json.optString("email", "").validateEmail()
            val nbPassengers = json.optInt("nbPassengers", 1).coerceIn(1, 8)
            val price = json.optDouble("price", 0.0).coerceAtLeast(0.0)
            val isConfirmation = json.optBoolean("isConfirmation", false)
            val isCancellation = json.optBoolean("isCancellation", false)
            val isModification = json.optBoolean("isModification", false)
            val confidence = json.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f)

            // Validate required fields (at least one)
            val hasRequiredFields = listOf(departure, arrival, time).any { it.isNotBlank() }
            if (!hasRequiredFields) {
                Log.w(TAG, "LLM response missing required fields: $jsonStr")
                return null
            }

            val missingFields = mutableListOf<String>()
            if (departure.isBlank()) missingFields.add("le lieu de départ")
            if (arrival.isBlank()) missingFields.add("la destination")
            if (time.isBlank()) missingFields.add("l'heure")
            if (date.isBlank()) missingFields.add("la date")

            val result = AiParsedResult(
                departure = departure,
                arrival = arrival,
                time = time,
                date = date,
                clientName = clientName,
                clientFirstName = clientFirstName,
                phone = phone,
                email = email,
                nbPassengers = nbPassengers,
                price = price,
                isConfirmation = isConfirmation,
                isCancellation = isCancellation,
                isModification = isModification,
                confidence = confidence,
                missingFields = missingFields,
                aiReasoning = json.optString("reasoning", "")
            )

            return validateAiParsedResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            null
        }
    }

    private fun sanitizeField(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""
        val placeholders = setOf(
            "non spécifié", "non specifié", "non renseigné", "non renseigne",
            "inconnu", "unknown", "n/a", "n.a.", "non précisé", "non precise",
            "non communiqué", "non communique", "nc", "aucun", "none",
            "pas de", "indéfini", "indefini", "non", "null", "undefined"
        )
        if (trimmed.lowercase() in placeholders) return ""
        if (trimmed.length <= 1) return ""
        return trimmed
    }

    private fun validateAiParsedResult(result: AiParsedResult): AiParsedResult? {
        if (result.isConfirmation && result.isCancellation) {
            Log.w(TAG, "AI returned mutually exclusive flags: confirmation && cancellation")
            return null
        }
        if (result.isConfirmation && result.isModification) {
            Log.w(TAG, "AI returned mutually exclusive flags: confirmation && modification")
            return null
        }
        if (result.isCancellation && result.isModification) {
            Log.w(TAG, "AI returned mutually exclusive flags: cancellation && modification")
            return null
        }
        if (result.departure.length > 150 || result.arrival.length > 150) {
            Log.w(TAG, "AI returned unreasonably long field value")
            return null
        }
        if (result.price > 100_000) {
            Log.w(TAG, "AI returned unrealistic price: ${result.price}")
            return null
        }
        if (result.confidence < 0.3f && (result.departure.isNotBlank() || result.arrival.isNotBlank() || result.time.isNotBlank())) {
            Log.w(TAG, "AI returned fields but confidence too low (${result.confidence}), rejecting")
            return null
        }
        if (result.confidence < 0.15f && (result.isCancellation || result.isConfirmation || result.isModification)) {
            Log.w(TAG, "AI returned action flag but confidence too low (${result.confidence}), rejecting")
            return null
        }
        return result
    }

    private suspend fun runInferenceWithTimeout(context: Context, prompt: String, timeoutSeconds: Int): String? {
        return NexaEngine.runInference(context, prompt, timeoutMs = timeoutSeconds * 1000L)
    }

    fun isAiAvailable(context: Context): Boolean {
        return NexaEngine.isAvailable(context)
    }

    internal fun parseWithFallback(smsBody: String): AiParsedResult {
        Log.d(TAG, "Using regex fallback parser")

        val parsed = parseSmsAdvanced("", smsBody)
        val missingFields = mutableListOf<String>()

        if (parsed.departure.isBlank()) missingFields.add("lieu de départ")
        if (parsed.arrival.isBlank()) missingFields.add("destination")
        if (parsed.time.isBlank()) missingFields.add("heure")

        return AiParsedResult(
            departure = parsed.departure,
            arrival = parsed.arrival,
            time = parsed.time,
            date = parsed.date,
            isConfirmation = parsed.isConfirmation,
            isCancellation = parsed.isCancellation,
            isModification = parsed.isModification,
            confidence = parsed.confidence,
            missingFields = missingFields,
            aiReasoning = "Parsed with regex fallback (AI not available)"
        )
    }

    internal fun isTaxiRelatedRegex(body: String): Boolean {
        val lower = body.lowercase()
        val taxiKeywords = listOf(
            "taxi", "course", "chauffeur", "vtc", "uber", "bolt", "heetch",
            "aller", "départ", "depart", "réservation", "reservation",
            "transport", "trajet", "prise en charge", "aéroport", "gare"
        )
        return taxiKeywords.any { lower.contains(it) }
    }

    internal fun String.validatePhone(): String = this.validatePhoneExtension()

    internal fun String.validateEmail(): String = this.validateEmailExtension()

    fun isFormspreeEmail(body: String): Boolean {
        return (body.contains("Formspree", ignoreCase = true) || body.contains("formulaire réservation VTC", ignoreCase = true)) &&
                body.contains("Nom", ignoreCase = true) &&
                body.contains("Téléphone", ignoreCase = true)
    }

    fun parseFormspreeEmail(body: String): AiParsedResult? {
        if (!isFormspreeEmail(body)) return null

        try {
            val cleanedBody = cleanEmailBody(body)
            val nomRegex = Regex("(?im)^\\s*Nom\\s*(?::)?\\s*([^\\n\\r]+)")
            val telRegex = Regex("(?im)^\\s*(?:T\\u00e9l\\u00e9phone|Telephone|Tel)\\s*(?::)?\\s*([^\\n\\r]+)")
            val emailRegex = Regex("(?im)^\\s*E-?mail\\s*(?::)?\\s*([^\\n\\r]+)")
            val prestRegex = Regex("(?im)^\\s*Prestation\\s*(?::)?\\s*([^\\n\\r]+)")
            val dateRegex = Regex("(?im)^\\s*Date\\s*(?::)?\\s*([^\\n\\r]+)")
            val horaireRegex = Regex("(?im)^\\s*(?:Horaire|Heure|Time)\\s*(?::)?\\s*([^\\n\\r]+)")
            val depAddrRegex = Regex("(?im)^\\s*Adresse de d[ée]part\\s*(?::)?\\s*([^\\n\\r]+)")
            val destRegex = Regex("(?im)^\\s*(?:Adresse de destination|Destination|Arriv\\u00e9e|Arrivee)\\s*(?::)?\\s*([^\\n\\r]+)")
            val detailsRegex = Regex("(?im)^\\s*(?:D\u00e9tails|Details|Message)\\s*(?::)?\\s*([^\\n\\r]+)")

            val nom = nomRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val tel = telRegex.find(cleanedBody)?.groupValues?.get(1)?.trim()?.replace(Regex("[^+0-9]"), "") ?: ""
            val email = emailRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val dateVal = dateRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val horaire = horaireRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val adresseDepart = depAddrRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val dest = destRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""

            var firstName = ""
            var lastName = ""
            if (nom.isNotBlank()) {
                val parts = nom.split(Regex("\\s+"), 2)
                if (parts.size == 2) {
                    firstName = parts[0]
                    lastName = parts[1]
                } else {
                    lastName = nom
                }
            }

            val prest = prestRegex.find(cleanedBody)?.groupValues?.get(1)?.trim() ?: ""
            val rawDeparture = if (adresseDepart.isNotBlank()) adresseDepart else {
                if (prest.lowercase().startsWith("depuis")) {
                    prest.substring(6).trim()
                } else {
                    prest
                }
            }
            val departure = rawDeparture.trim().replaceFirstChar { it.uppercase() }
            val arrival = dest.trim()

            if (departure.isBlank() && arrival.isBlank() && horaire.isBlank()) {
                return null
            }

            val missingFields = mutableListOf<String>()
            if (departure.isBlank()) missingFields.add("lieu de départ")
            if (arrival.isBlank()) missingFields.add("destination")
            if (horaire.isBlank()) missingFields.add("heure")
            if (dateVal.isBlank()) missingFields.add("date")

            return AiParsedResult(
                departure = departure,
                arrival = arrival,
                time = horaire,
                date = dateVal,
                clientName = lastName,
                clientFirstName = firstName,
                phone = tel,
                email = email,
                nbPassengers = 1,
                price = 0.0,
                isConfirmation = false,
                isCancellation = false,
                isModification = false,
                confidence = 1.0f,
                missingFields = missingFields,
                aiReasoning = "Formspree reservation email regex parsing"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Formspree email", e)
            return null
        }
    }

    suspend fun parseEmail(context: Context, emailBody: String, aiEnabled: Boolean = true): AiParsedResult {
        Log.d(TAG, "Parsing email avec AI enabled: $aiEnabled")
        val cleanedBody = cleanEmailBody(emailBody)
        return parseWithAI(context, cleanedBody, aiEnabled)
    }

    internal fun cleanEmailBody(body: String): String {
        return body
            .replace(Regex("(?m)^--\\s*\\r?\\n[\\s\\S]*"), "")
            .replace(Regex("(?i)Envoyé depuis.*"), "")
            .replace(Regex("(?i)Sent from.*"), "")
            .replace(Regex("(?i)Le\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}.*"), "")
            .replace(Regex("(?im)^De\\s*:.*"), "")
            .replace(Regex("(?im)^À\\s*:.*"), "")
            .replace(Regex("(?im)^Objet\\s*:.*"), "")
            .replace(Regex("(?im)^Date\\s*:.*"), "")
            .replace(Regex("(?im)^Répondre à\\s*:.*"), "")
            .replace(Regex("(?im)^--------.*--------"), "")
            .replace(Regex("(?i)_______________"), "")
            .trim()
    }

    internal fun extractJsonFromResponse(response: String): String? {
        // Try markdown code blocks first
        val jsonBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = jsonBlockRegex.find(response)
        if (match != null) {
            val extracted = match.groupValues[1].trim()
            if (isValidJson(extracted)) return extracted
        }

        // Try finding the first { ... } pair with balanced braces
        var depth = 0
        var start = -1
        for (i in response.indices) {
            when (response[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        val candidate = response.substring(start, i + 1)
                        if (isValidJson(candidate)) return candidate
                        start = -1
                    }
                }
            }
        }

        return null
    }

    internal fun isValidJson(str: String): Boolean {
        return try {
            JSONObject(str)
            true
        } catch (e: Exception) {
            false
        }
    }
}

internal fun String.validatePhoneExtension(): String {
    val cleaned = replace(Regex("[^+0-9]"), "")
    return if (cleaned.matches(Regex("^\\+?33?[0-9]{9,10}$")) || cleaned.matches(Regex("^0[0-9]{9}$"))) {
        cleaned
    } else ""
}

internal fun String.validateEmailExtension(): String {
    return if (matches(Regex("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE))) {
        this
    } else ""
}
