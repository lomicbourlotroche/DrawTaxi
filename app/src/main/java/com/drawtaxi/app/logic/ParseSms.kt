package com.drawtaxi.app.logic

import android.util.Log
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import java.util.*
import java.util.regex.Pattern

private const val TAG = "ParseSms"

enum class SmsField { DE, VERS, TIME, DATE, NB_PASSENGERS, PRICE, CONFIRM }
private data class SmsToken(val start: Int, val end: Int, val field: SmsField, val confidence: Float = 0.8f)

data class ParsedSms(
    val departure: String = "",
    val arrival: String = "",
    val time: String = "",
    val date: String = "",
    val nbPassengers: Int = 0,
    val price: Double = 0.0,
    val isConfirmation: Boolean = false,
    val isCancellation: Boolean = false,
    val isModification: Boolean = false,
    val confidence: Float = 0f,
    val missingFields: List<String> = emptyList()
)

private val taxiKeywords = listOf(
    "taxi", "course", "chauffeur", "vtc", "uber", "bolt", "heetch",
    "aller", "départ", "depart", "réservation", "reservation", "réserve",
    "reserve", "bonjour", "slt", "svp", "merci", "rdv", "rendez-vous",
    "transport", "trajet", "prise en charge", "aéroport", "gare"
)

private val departureKeywords = listOf(
    "depuis ", "de :", "de:", "dep :", "dep:", "adresse :", "adresse:",
    "départ :", "départ:", "depart :", "depart:", "enlèvement :", "enlèvement:",
    "prise en charge :", "prise en charge:", "depart de ", "dep. ", "-de ",
    "je suis à ", "je suis ", "出发", " starting from", " starting at",
    "mon adresse", "notre adresse", "à partir de ", "partant de ",
    "je pars de ", "pars de ", "au départ ", "point de départ ",
    "departing from", "from ", "from:", "depart from ", "pick up at ",
    "pickup at ", "address: ", "address :", "addr: ", "addr:"
)

private val arrivalKeywords = listOf(
    "vers ", "pour ", "aller à ", "destination :", "destination:",
    "arrivée :", "arrivée:", "arrivee :", "arrivee:", "dest :", "dest:", "à :", "-vers ",
    "destination", "chez ", "arriver", "rendez-vous", "rdv",
    "terminus", "gare", "l'a", "aeroport", "airport", "hotel",
    "restaurant", "hopital", "hôpital", "clinique", "to: ", "to ",
    "drop off at ", "dropoff at "
)

private val timeKeywords = listOf(
    " à ", " a ", "heure :", "heure:", " h ", "h ", "h:",
    "maintenant", "rdv :", "rdv:", "prévu à", "prevu a", "prevu ",
    "dès que", "des que",
    "immediatement", "immédiatement", "urgent", "vite", " asap ",
    "dans ", "cet après-midi", "cet aprèm", "cette après-midi", "cette après midi",
    "cet aprem", "cette aprem", "ce matin", "ce soir", "ce midi",
    "demain", "aujourd'hui", "aujourd hui", "aujourdhui",
    "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche",
    "at ", "time: ", "time :"
)

private val confirmationKeywords = listOf(
    "c'est", "ces", "c'est quoi", "confirme", "confirmation",
    "ok", "okay", "oki", "oué", "ouep", "ouéé", "compris", "entendu",
    "reçu", "recus", "bien reçu", "bien recu", "roger", "yes",
    "est ce", "est-ce", "peut etre", "peut-etre", "peut être",
    "d'accord", "dacc", "ok dac", "okdac", "nickel", "parfait",
    "super", "génial", "genial", "merci", "bonne journée", "bonne journee",
    "bonne soirée", "bonne soiree", "merci beaucoup", "merci bcp",
    "je confirme", "confirmé", "confirmee", "c'est noté", "c'est reglé", "c'est bon",
    "validé", "valide", "accepté", "accepte"
)

private val cancelKeywords = listOf(
    "annule", "annuler", "annulation", "annulez", "supprime", "supprimer",
    "suppression", "efface", "effacer", "désist", "desist", "abandon",
    "ne vient pas", "ne peut pas", "cancel", "cancelled",
    "delete", "stop", "stopped", "stopper", "ne plus", "stoppé", "stoppee",
    "plus besoin", "je n'ai plus", "pas la peine", "inutile",
    "c'est annulé", "annule", "c'est bon j'ai", "j'ai trouvé"
)

private val modifyKeywords = listOf(
    "modifie", "modifier", "modification", "change", "changer", "changement",
    "rectifie", "rectifier", "corrige", "corriger", "mauvais", "erreur",
    "faux", "invers", "swap", "different", "autre", "nouveau", "nouvelle",
    "mise à jour", "mise a jour", "update", "instead", "actually", "en fait",
    "plutot", "plutôt", "autrement", "à la place", "je voudrais", "je veux",
    "finalement", "en réalité", "en realite"
)

private val nbPassengersKeywords = listOf(
    "passager", "passagers", "personne", "personnes", "gens", "voyageur",
    "voyageurs", " occupants", " occupants", " personne", " personnes",
    "2 personnes", "3 personnes", "4 personnes", "1 personne", "1pers", "2pers", "3pers", "4pers",
    " à ", " personnes", "passagers", "personne"
)

private val priceKeywords = listOf(
    " euros", "euros", "€", "eur", " prix ", "cout", "coût", "tarif",
    "environ ", "env ", "approx", "approximativement"
)

private val timePattern = Pattern.compile(
    "(\\d{1,2})h(\\d{2})?|(\\d{1,2}):(\\d{2})|" +
    "(\\d{1,2})h(\\d{0,2})|" +
    "(dans|within|in) (\\d+) (min|minutes|heure|heures|h)|" +
    "(demain|aujourd'hui|aujourd hui|a midi|ce midi|ce soir|cet aprèm|cet après-midi|tomorrow|today)"
)

private val datePattern = Pattern.compile(
    "(demain|aujourd'hui|aujourd hui|tomorrow|today|" +
    "lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche|monday|tuesday|wednesday|thursday|friday|saturday|sunday)|" +
    "(\\d{1,2})/(\\d{1,2})|" +
    "(\\d{1,2}) (janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)"
)

fun parseSms(sender: String, body: String, timestamp: Long = System.currentTimeMillis(), settings: AppSettings? = null): RideRequest? {
    val parsed = parseSmsAdvanced(sender, body, timestamp)
    
    if (parsed.departure.isBlank() && parsed.arrival.isBlank() && parsed.time.isBlank()) {
        if (!isTaxiRelated(body)) {
            Log.d(TAG, "SMS ignoré (pas de mot taxi): $sender")
            return null
        }
        
        Log.d(TAG, "SMS taxi détecté mais sans infos de course: $sender")
        return null
    }
    
    val estimatedDistance = estimateDistance(parsed.departure, parsed.arrival)
    val estimatedPrice = settings?.let { calculateEstimatedPrice(estimatedDistance, it) } ?: 0.0
    val estimatedFuel = settings?.let { estimatedDistance * it.fuelCostPerKm } ?: 0.0
    val estimatedDuration = estimateDuration(estimatedDistance)
    val estimatedOpCost = settings?.let { (estimatedDuration / 60.0) * it.operatingCostPerHour } ?: 0.0
    val estimatedProfitability = RideRequest.calculateProfitability(estimatedPrice, estimatedFuel, estimatedOpCost)
    
    return RideRequest(
        id = RideRequest.createStableId(sender, body, timestamp),
        sender = sender,
        body = body,
        departure = parsed.departure,
        arrival = parsed.arrival,
        time = parsed.time,
        date = parsed.date,
        distanceKm = estimatedDistance,
        price = estimatedPrice,
        durationMinutes = estimatedDuration,
        fuelCost = estimatedFuel,
        operatingCost = estimatedOpCost,
        profitabilityPercent = estimatedProfitability,
        timestamp = timestamp,
        status = RideStatus.DRAFT
    )
}

private fun estimateDistance(departure: String, arrival: String): Double {
    if (departure.isBlank() || arrival.isBlank()) return 0.0
    
    val parisKeywords = listOf("paris", "cdg", "orly", "gare du nord", "gare de lyon", "chatelet", "opera", "defence", "la défense", "montparnasse", "saint-lazare", "est", "austerlitz")
    val suburbKeywords = listOf("versailles", "saint-denis", "creteil", "nanterre", "bobigny", "evry", "cergy", "meaux", "pontoise", "melun", "fontainebleau")
    
    val depLower = departure.lowercase()
    val arrLower = arrival.lowercase()
    
    val depIsParis = parisKeywords.any { depLower.contains(it) }
    val arrIsParis = parisKeywords.any { arrLower.contains(it) }
    val depIsSuburb = suburbKeywords.any { depLower.contains(it) }
    val arrIsSuburb = suburbKeywords.any { arrLower.contains(it) }
    
    return when {
        depIsParis && arrIsParis -> 10.0
        (depIsParis && arrIsSuburb) || (depIsSuburb && arrIsParis) -> 25.0
        depIsSuburb && arrIsSuburb -> 20.0
        else -> 25.0
    }
}

private fun calculateEstimatedPrice(distanceKm: Double, settings: AppSettings): Double {
    val basePrice = settings.basePrice.toDoubleOrNull() ?: 2.60
    val perKm = settings.pricePerKm.toDoubleOrNull() ?: 1.20
    return basePrice + (distanceKm * perKm)
}

private fun estimateDuration(distanceKm: Double): Int {
    if (distanceKm <= 0) return 0
    val avgSpeedKmh = if (distanceKm < 10) 30.0 else 50.0
    return ((distanceKm / avgSpeedKmh) * 60).toInt()
}

fun parseSmsAdvanced(sender: String, body: String, timestamp: Long = System.currentTimeMillis()): ParsedSms {
    val lowerBody = body.lowercase()
    val allTokens = mutableListOf<SmsToken>()
    val contextAwareness = analyzeContext(body)
    
    for (kw in departureKeywords) {
        var idx = lowerBody.indexOf(kw)
        while (idx != -1) {
            val confidence = adjustConfidenceForContext(kw, contextAwareness)
            allTokens.add(SmsToken(idx, idx + kw.length, SmsField.DE, confidence))
            idx = lowerBody.indexOf(kw, idx + 1)
        }
    }
    
    for (kw in arrivalKeywords) {
        var idx = lowerBody.indexOf(kw)
        while (idx != -1) {
            val confidence = adjustConfidenceForContext(kw, contextAwareness)
            allTokens.add(SmsToken(idx, idx + kw.length, SmsField.VERS, confidence))
            idx = lowerBody.indexOf(kw, idx + 1)
        }
    }
    
    for (kw in timeKeywords) {
        var idx = lowerBody.indexOf(kw)
        while (idx != -1) {
            allTokens.add(SmsToken(idx, idx + kw.length, SmsField.TIME, 0.75f))
            idx = lowerBody.indexOf(kw, idx + 1)
        }
    }
    
    val sortedTokens = allTokens.sortedBy { it.start }
    
    val results = mutableMapOf<SmsField, String>()
    val confidenceMap = mutableMapOf<SmsField, Float>()
    
    for (i in sortedTokens.indices) {
        val token = sortedTokens[i]
        
        var nextTokenIdx = -1
        for (j in i + 1 until sortedTokens.size) {
            nextTokenIdx = j
            break
        }
        
        var endLimit = if (nextTokenIdx != -1) {
            sortedTokens[nextTokenIdx].start
        } else {
            lowerBody.length
        }
        
        if (token.field == SmsField.DE) {
            for (j in i + 1 until sortedTokens.size) {
                if (sortedTokens[j].field != SmsField.DE) {
                    endLimit = sortedTokens[j].start
                    break
                }
            }
        }
        
        if (token.end < endLimit && token.end < lowerBody.length) {
            var startPos = token.end
            if (token.field == SmsField.VERS && token.end - token.start <= 3 && token.end > 1) {
                val ch = body[token.end - 2]
                val isApostrophe = ch == '\'' || ch == '\u2018' || ch == '\u2019'
                if (isApostrophe && token.end - 3 >= 0 && body[token.end - 3].lowercaseChar() == 'l') {
                    startPos = token.end - 3
                }
            }
            val value = extractValue(body, startPos, endLimit)
            
            val minLength = when (token.field) {
                SmsField.DE, SmsField.VERS -> 3
                else -> 2
            }
            
            if (value.isNotEmpty() && value.length >= minLength) {
                if (results[token.field].isNullOrBlank()) {
                    results[token.field] = value
                    confidenceMap[token.field] = calculateFieldConfidence(token.field, value, token.confidence)
                }
            }
        }
    }
    
    var timeResult = results[SmsField.TIME] ?: ""
    if (timeResult.isBlank()) {
        timeResult = extractTimeFromBody(lowerBody)
    }
    
    val dateResult = extractDateFromBody(lowerBody)
    
    val departureRaw = results[SmsField.DE]
    val departureClean = departureRaw?.cleanLocation() ?: ""
    val departurePhrase = departureClean.takeFirstPhrase()
    val departure = departurePhrase.replaceFirstChar { it.uppercase() }
    val versRaw = results[SmsField.VERS]
    val arrival = versRaw?.cleanLocation()?.takeFirstPhrase()?.replaceFirstChar { it.uppercase() } ?: ""
    val isConfirmation = containsConfirmation(body.lowercase())
    val isCancellation = isCancellationMessage(body)
    val isModification = isModificationMessage(body)
    
    val missingFields = mutableListOf<String>()
    if (departure.isBlank()) missingFields.add("lieu de départ")
    if (arrival.isBlank()) missingFields.add("destination")
    if (timeResult.isBlank()) missingFields.add("heure")
    
    val confidence = calculateOverallConfidence(confidenceMap, contextAwareness)
    
    return ParsedSms(
        departure = departure,
        arrival = arrival,
        time = timeResult,
        date = dateResult,
        isConfirmation = isConfirmation,
        isCancellation = isCancellation,
        isModification = isModification,
        confidence = confidence,
        missingFields = missingFields
    )
}

private data class ContextAnalysis(
    val hasGreeting: Boolean,
    val hasPoliteness: Boolean,
    val hasUrgency: Boolean,
    val hasNumbers: Boolean,
    val messageLength: Int,
    val hasLocationTerms: Boolean
)

private fun analyzeContext(body: String): ContextAnalysis {
    val lower = body.lowercase()
    return ContextAnalysis(
        hasGreeting = lower.contains("bonjour") || lower.contains("slt") || lower.contains("salut") || lower.contains("hello") || lower.contains("hi"),
        hasPoliteness = lower.contains("merci") || lower.contains("svp") || lower.contains("s'il vous plaît") || lower.contains("please"),
        hasUrgency = lower.contains("urgent") || lower.contains("vite") || lower.contains("asap") || lower.contains("maintenant") || lower.contains("immédiatement"),
        hasNumbers = body.any { it.isDigit() },
        messageLength = body.length,
        hasLocationTerms = lower.contains("adresse") || lower.contains("rue") || lower.contains("avenue") || lower.contains("boulevard") || lower.contains("place")
    )
}

private fun adjustConfidenceForContext(keyword: String, context: ContextAnalysis): Float {
    var baseConfidence = 0.8f
    
    if (context.hasGreeting && context.hasPoliteness) {
        baseConfidence += 0.1f
    }
    
    if (context.hasUrgency) {
        baseConfidence += 0.05f
    }
    
    if (context.messageLength > 20) {
        baseConfidence += 0.05f
    }
    
    return baseConfidence.coerceAtMost(1.0f)
}

fun isTaxiRelated(body: String): Boolean {
    val lowerBody = body.lowercase()
    val keywordMatches = taxiKeywords.count { lowerBody.contains(it) }
    return keywordMatches >= 1
}

private fun containsConfirmation(body: String): Boolean {
    return confirmationKeywords.any { body.contains(it) }
}

private fun extractValue(body: String, start: Int, end: Int): String {
    if (start >= body.length) return ""
    val actualEnd = minOf(end, body.length)
    return body.substring(start, actualEnd)
        .trim()
        .trimEnd('.', ',', '!', '?', ';', ':')
}

private val leadingPrepositionPattern = Regex(
    "^\\s*(depuis\\s+|de\\s+|du\\s+|du:\\s*|depart\\s+from\\s*|from\\s+|" +
    "departing\\s+from\\s*|au\\s+depart\\s+de\\s*|partant\\s+de\\s*|je\\s+pars\\s+de\\s*|" +
    "depart\\s+from\\s*|starting\\s+from\\s*|starting\\s+at\\s*|\\-de\\s*)",
    RegexOption.IGNORE_CASE
)

private val leadingLabelPattern = Regex(
    "^\\s*l?'?adresse\\s*:\\s*",
    RegexOption.IGNORE_CASE
)

private fun String.cleanLocation(): String {
    var text = this
        .replace(Regex("[.,!?;:\$€£]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    text = leadingPrepositionPattern.replace(text, "")
    text = leadingLabelPattern.replace(text, "")

    text = text.replace(Regex("^adresse\\s*:?\\s*", RegexOption.IGNORE_CASE), "")

    val lowerText = text.lowercase()
    if (lowerText.startsWith("l'adresse")) {
        text = text.substringAfter("l'adresse").trim()
        if (text.startsWith(":")) text = text.substringAfter(":").trim()
    } else if (lowerText.startsWith("adresse")) {
        text = text.substringAfter("adresse").trim()
        if (text.startsWith(":")) text = text.substringAfter(":").trim()
    }

    text = text.let { t ->
        val timePattern = Regex("\\d{1,2}h\\d{0,2}|\\d{1,2}:\\d{2}")
        val timeMatch = timePattern.find(t)
        if (timeMatch != null && timeMatch.range.first > 0) {
            t.substring(0, timeMatch.range.first).trim()
        } else {
            t
        }
    }

    return text
}

private val phraseSeparatorPattern = Regex(
    ",| et | ou | - | – | / |\\n| merci| svp| s'il| avant | après ",
    RegexOption.IGNORE_CASE
)

private val addressPrefixPattern = Regex(
    "^\\s*(depuis\\s+)?l?'?adresse\\s*:\\s*",
    RegexOption.IGNORE_CASE
)

private val contractionPrefixPattern = Regex(
    "^\\s*l'",
    RegexOption.IGNORE_CASE
)

private val genericPrepositionPattern = Regex(
    "^\\s*(depuis\\s+|de\\s+|du\\s+|du:\\s*|depart\\s+from\\s*|from\\s+|departing\\s+from\\s*|" +
    "au\\s+depart\\s+de\\s*|partant\\s+de\\s*|je\\s+pars\\s+de\\s*|depart\\s+from\\s*|" +
    "starting\\s+from\\s*|starting\\s+at\\s*|\\-de\\s*)",
    RegexOption.IGNORE_CASE
)

private val addressAfterPattern = Regex(
    "^\\s*(adresse|aeroport|aeroport| CDG|ORLY|airport|gare|hotel|restaurant|hôpital|hopital|clinique)\\s*:?\\s*",
    RegexOption.IGNORE_CASE
)

private fun String.takeFirstPhrase(): String {
    val match = phraseSeparatorPattern.find(this)
    if (match != null && match.range.first > 3) {
        return this.substring(0, match.range.first).trim()
    }
    val addrMatch = addressPrefixPattern.find(this)
    if (addrMatch != null) {
        val after = this.substring(addrMatch.range.last + 1).trim()
        if (after.isNotEmpty()) return after
    }
    val afterAddr = addressAfterPattern.find(this)
    if (afterAddr != null && afterAddr.range.first <= 1) {
        val after = this.substring(afterAddr.range.last + 1).trim()
        if (after.isNotEmpty()) return after
    }
    return this
}

private fun extractTimeFromBody(body: String): String {
    val lowerBody = body.lowercase()
    val times = mutableListOf<String>()
    
    val timeMarkers = listOf("h", ":", "min", "dans", "demain", "aujourd'hui", "midi", "soir", "après-midi", "cet aprèm")
    
    val hPattern = Regex("(\\d{1,2})h(\\d{2})")
    for (m in hPattern.findAll(lowerBody)) {
        val h = m.groupValues[1]
        val min = m.groupValues[2]
        if (min.isNotEmpty() && min.toIntOrNull() != null && min.toInt() < 60) {
            times.add("${h}h$min")
        }
    }
    
    val colonPattern = Regex("(\\d{1,2}):(\\d{2})")
    for (m in colonPattern.findAll(lowerBody)) {
        val h = m.groupValues[1]
        val min = m.groupValues[2]
        if (min.isNotEmpty() && min.toIntOrNull() != null && min.toInt() < 60) {
            times.add("$h:$min")
        }
    }
    
    val dansMinPattern = Regex("dans (\\d+) min")
    for (m in dansMinPattern.findAll(lowerBody)) {
        val num = m.groupValues[1]
        times.add("dans $num min")
    }
    
    if (lowerBody.contains("demain")) times.add("demain")
    if (lowerBody.contains("aujourd'hui") || lowerBody.contains("aujourd hui")) times.add("aujourd'hui")
    
    if (lowerBody.contains("cet apr") || lowerBody.contains("cet après") || lowerBody.contains("cet aprem")) times.add("après-midi")
    if (lowerBody.contains("ce midi") || lowerBody.contains("a midi")) times.add("12h")
    if (lowerBody.contains("ce soir")) times.add("soir")
    
    return times.firstOrNull() ?: ""
}

private fun extractDateFromBody(body: String): String {
    val lowerBody = body.lowercase()
    
    if (lowerBody.contains("aujourd'hui") || lowerBody.contains("aujourd hui")) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    if (lowerBody.contains("demain")) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(calendar.time)
    }
    
    val datePattern = Regex("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?")
    val match = datePattern.find(lowerBody)
    if (match != null) {
        val day = match.groupValues[1]
        val month = match.groupValues[2]
        val year = match.groupValues.getOrNull(3) ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString().takeLast(2)
        return "$day/$month/$year"
    }
    
    return ""
}

private fun calculateFieldConfidence(field: SmsField, value: String, tokenConfidence: Float = 0.8f): Float {
    val baseConfidence = when (field) {
        SmsField.DE, SmsField.VERS -> {
            if (value.length >= 5) 0.9f else if (value.length >= 3) 0.7f else 0.5f
        }
        SmsField.TIME -> 0.8f
        else -> 0.7f
    }
    return (baseConfidence * tokenConfidence).coerceAtMost(1.0f)
}

private fun calculateOverallConfidence(confidenceMap: Map<SmsField, Float>, context: ContextAnalysis): Float {
    if (confidenceMap.isEmpty()) return 0.3f
    var avg = confidenceMap.values.average().toFloat()
    
    if (context.hasGreeting && context.hasPoliteness) avg += 0.05f
    if (context.hasUrgency) avg += 0.05f
    if (context.messageLength > 50) avg += 0.05f
    
    return avg.coerceAtMost(1.0f)
}

fun isCancellationMessage(body: String): Boolean {
    val lowerBody = body.lowercase()
    return cancelKeywords.count { lowerBody.contains(it) } >= 1
}

fun isModificationMessage(body: String): Boolean {
    val lowerBody = body.lowercase()
    return modifyKeywords.count { lowerBody.contains(it) } >= 1
}

fun isConfirmationMessage(body: String): Boolean {
    return containsConfirmation(body.lowercase())
}

fun extractMissingFields(parsed: ParsedSms, existing: RideRequest? = null): List<String> {
    val missing = mutableListOf<String>()
    
    val departure = parsed.departure.ifBlank { existing?.departure ?: "" }
    val arrival = parsed.arrival.ifBlank { existing?.arrival ?: "" }
    val time = parsed.time.ifBlank { existing?.time ?: "" }
    
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

fun formatMissingFieldsMessage(missingFields: List<String>, template: String): String {
    if (missingFields.isEmpty()) return ""
    
    val fieldsText = when (missingFields.size) {
        1 -> missingFields[0]
        2 -> "${missingFields[0]} et ${missingFields[1]}"
        else -> {
            val allButLast = missingFields.dropLast(1).joinToString(", ")
            "$allButLast et ${missingFields.last()}"
        }
    }
    
    return template
        .replace("[FIELDS]", fieldsText)
        .replace("[-fields]", fieldsText)
        .replace("{{fields}}", fieldsText)
}

fun formatConfirmationMessage(@Suppress("UNUSED_PARAMETER") settings: AppSettings, ride: RideRequest): String {
    val parts = mutableListOf<String>()
    
    if (ride.departure.isNotBlank() && ride.arrival.isNotBlank()) {
        parts.add("Course de ${ride.departure} vers ${ride.arrival}")
    } else if (ride.arrival.isNotBlank()) {
        parts.add("Course vers ${ride.arrival}")
    }
    
    if (ride.time.isNotBlank()) {
        parts.add("à ${ride.time}")
    }
    
    if (parts.isEmpty()) {
        return "Bonjour, nous avons bien pris en compte votre demande."
    }
    
    return "Bonjour, c'est noté ! ${parts.joinToString(" ")}."
}
