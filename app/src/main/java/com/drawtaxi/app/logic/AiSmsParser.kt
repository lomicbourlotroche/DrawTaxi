package com.drawtaxi.app.logic

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.RideRequest
import org.json.JSONObject

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
    fun toRideRequest(sender: String, timestamp: Long = System.currentTimeMillis(), settings: com.drawtaxi.app.data.AppSettings? = null): RideRequest? {
        if (departure.isBlank() && arrival.isBlank() && time.isBlank()) {
            return null
        }

        val estimatedDistance = if (departure.isNotBlank() && arrival.isNotBlank()) {
            val depLower = departure.lowercase()
            val arrLower = arrival.lowercase()
            val parisKeywords = listOf("paris", "cdg", "orly", "gare", "chatelet", "opera", "defense")
            val depIsParis = parisKeywords.any { depLower.contains(it) }
            val arrIsParis = parisKeywords.any { arrLower.contains(it) }
            when {
                depIsParis && arrIsParis -> 10.0
                depIsParis || arrIsParis -> 25.0
                else -> 25.0
            }
        } else 0.0

        val estimatedPrice = settings?.let {
            val basePrice = it.basePrice.toDoubleOrNull() ?: 2.60
            val perKm = it.pricePerKm.toDoubleOrNull() ?: 1.20
            basePrice + (estimatedDistance * perKm)
        } ?: 0.0

        val estimatedFuel = settings?.let { estimatedDistance * it.fuelCostPerKm } ?: 0.0
        val estimatedDuration = if (estimatedDistance > 0) ((estimatedDistance / if (estimatedDistance < 10) 30.0 else 50.0) * 60).toInt() else 0
        val estimatedOpCost = settings?.let { (estimatedDuration / 60.0) * it.operatingCostPerHour } ?: 0.0
        val estimatedProfitability = RideRequest.calculateProfitability(estimatedPrice, estimatedFuel, estimatedOpCost)

        return RideRequest(
            id = RideRequest.createStableId(sender, "$departure $arrival $time", timestamp),
            sender = phone.ifBlank { sender },
            body = "",
            departure = departure,
            arrival = arrival,
            time = time,
            date = date,
            distanceKm = estimatedDistance,
            price = if (price > 0) price else estimatedPrice,
            durationMinutes = estimatedDuration,
            fuelCost = estimatedFuel,
            operatingCost = estimatedOpCost,
            profitabilityPercent = estimatedProfitability,
            clientName = if (clientName.isNotBlank()) "$clientFirstName $clientName".trim() else "",
            clientEmail = email,
            timestamp = timestamp,
            status = com.drawtaxi.app.data.RideStatus.DRAFT
        )
    }
}

object AiSmsParser {

    private const val TAG = "AiSmsParser"
    private const val MAX_RETRIES = 3
    private const val TIMEOUT_SECONDS = 90

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
Si un champ n'est pas présent, utilise une chaîne vide ou 0.
""".trimIndent()

    suspend fun isTaxiRelated(context: Context, smsBody: String, aiEnabled: Boolean = true): Boolean {
        if (!aiEnabled) {
            return isTaxiRelatedRegex(smsBody)
        }

        LlamaModelManager.markUsed()

        if (!LlamaModelManager.isModelAvailable(context)) {
            return isTaxiRelatedRegex(smsBody)
        }

        for (attempt in 1..MAX_RETRIES) {
            try {
                val prompt = taxiClassificationPrompt.replace("{sms}", smsBody)
                val response = runInferenceWithTimeout(context, prompt, 30)
                if (response.isNullOrBlank()) continue

                val cleaned = response.trim().uppercase()
                return cleaned.contains("TAXI") && !cleaned.contains("NON_TAXI")
            } catch (e: Exception) {
                Log.e(TAG, "Classification AI error attempt $attempt: ${e.message}")
            }
        }

        return isTaxiRelatedRegex(smsBody)
    }

    private fun isTaxiRelatedRegex(body: String): Boolean {
        val lower = body.lowercase()
        val taxiKeywords = listOf(
            "taxi", "course", "chauffeur", "vtc", "uber", "bolt", "heetch",
            "aller", "départ", "depart", "réservation", "reservation",
            "transport", "trajet", "prise en charge", "aéroport", "gare"
        )
        return taxiKeywords.any { lower.contains(it) }
    }

    suspend fun parseWithAI(context: Context, smsBody: String, aiEnabled: Boolean = true): AiParsedResult {
        if (!aiEnabled) {
            Log.d(TAG, "AI disabled in settings, using regex fallback")
            return parseWithFallback(smsBody)
        }

        LlamaModelManager.markUsed()

        if (!LlamaModelManager.isModelAvailable(context)) {
            Log.d(TAG, "AI model not available, falling back to regex parser")
            return parseWithFallback(smsBody)
        }

        if (LlamaModelManager.shouldAutoUnload()) {
            LlamaModelManager.unload()
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
            if (jsonStr.isNullOrBlank()) return null

            val json = JSONObject(jsonStr)
            AiParsedResult(
                departure = json.optString("departure", ""),
                arrival = json.optString("arrival", ""),
                time = json.optString("time", ""),
                date = json.optString("date", ""),
                clientName = json.optString("clientName", ""),
                clientFirstName = json.optString("clientFirstName", ""),
                phone = json.optString("phone", "").validatePhone(),
                email = json.optString("email", "").validateEmail(),
                nbPassengers = json.optInt("nbPassengers", 1).coerceIn(1, 8),
                price = json.optDouble("price", 0.0),
                isConfirmation = json.optBoolean("isConfirmation", false),
                isCancellation = json.optBoolean("isCancellation", false),
                isModification = json.optBoolean("isModification", false),
                confidence = json.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f),
                aiReasoning = json.optString("reasoning", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            null
        }
    }

    private suspend fun runInferenceWithTimeout(context: Context, prompt: String, timeoutSeconds: Int): String? {
        return kotlinx.coroutines.withTimeoutOrNull(timeoutSeconds * 1000L) {
            runAiInferenceSync(context, prompt)
        }
    }

    private suspend fun runAiInferenceSync(context: Context, prompt: String): String? {
        return try {
            val modelFile = LlamaModelManager.getModelFile(context)

            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found")
                return null
            }

            Log.d(TAG, "Running inference with model: ${modelFile.name} (${modelFile.length() / 1024 / 1024} MB)")

            val result = runLlmInference(modelFile.absolutePath, prompt)
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "LLM native library not available: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            null
        }
    }

    private fun runLlmInference(modelPath: String, prompt: String): String? {
        try {
            val llmClass = Class.forName("com.drawtaxi.app.logic.LlmRunner")
            val runMethod = llmClass.getDeclaredMethod("run", String::class.java, String::class.java)
            return runMethod.invoke(null, modelPath, prompt) as? String
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "LlmRunner class not found - AI inference not available")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "LlmRunner execution failed: ${e.message}")
            return null
        }
    }

    private fun extractJsonFromResponse(response: String): String? {
        val jsonBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = jsonBlockRegex.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }

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
                        return response.substring(start, i + 1)
                    }
                }
            }
        }

        return null
    }

    private fun parseWithFallback(smsBody: String): AiParsedResult {
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

    private fun String.validatePhone(): String {
        val cleaned = replace(Regex("[^+0-9]"), "")
        return if (cleaned.matches(Regex("^\\+?33?[0-9]{9,10}$")) || cleaned.matches(Regex("^0[0-9]{9}$"))) {
            cleaned
        } else ""
    }

    private fun String.validateEmail(): String {
        return if (matches(Regex("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE))) {
            this
        } else ""
    }
}
