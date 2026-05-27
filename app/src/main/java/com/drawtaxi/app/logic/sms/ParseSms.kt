package com.drawtaxi.app.logic.sms

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
    val clientFirstName: String = "",
    val clientLastName: String = "",
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
    "transport", "trajet", "prise en charge", "aéroport", "gare",
    "besoin d'un taxi", "besoin de taxi", "commander", "commande de taxi",
    "dépannage", "depannage", "transfert", "navette",
    "déplacement", "deplacement", "voyage", "itinéraire"
)

private val departureKeywords = listOf(
    "depuis ", "de :", "de:", "dep :", "dep:", "adresse :", "adresse:",
    "départ :", "départ:", "depart :", "depart:", "enlèvement :", "enlèvement:",
    "prise en charge :", "prise en charge:", "depart de ", "dep. ", "-de ",
    "je suis à ", "je suis au ", "je suis rue ", "je suis avenue ", "je suis boulevard ",
    "je suis place ", "je suis chemin ", "je suis route ", "je suis impasse ",
    "je suis allée ", "je suis quai ", "je suis cours ",
    "出发", " starting from", " starting at",
    "mon adresse", "notre adresse", "à partir de ", "partant de ",
    "je pars de ", "pars de ", "au départ ", "point de départ ",
    "departing from", "from ", "from:", "depart from ", "pick up at ",
    "pickup at ", "address: ", "address :", "addr: ", "addr:",
    "rdv à ", "rdv au ", "rdv rue ", "rdv avenue ", "rendez-vous à ",
    "prendre à ", "prendre au ", "prendre rue ", "prendre avenue ",
    "prendre place ", "prendre chemin ", "prendre route ", "prendre impasse ",
    "prendre allée ", "prendre quai ",
    "au niveau de ", "devant ", "à côté de ", "derrière ", "en face de ",
    "proche ", "près de ", "angle ", "carrefour "
)

private val arrivalKeywords = listOf(
    "vers ", "pour ", "aller à ", "aller au ", "destination :", "destination:",
    "arrivée :", "arrivée:", "arrivee :", "arrivee:", "dest :", "dest:", "à :", "-vers ",
    "direction ", "direction:", "direction :",
    "destination", "chez ", "arriver", "rendez-vous", "rdv",
    "terminus", "gare", "aéroport", "airport", "hôtel", "hotel",
    "restaurant", "hôpital", "hopital", "clinique", "to: ", "to ",
    "drop off at ", "dropoff at ", "descendre à ", "descendre au ",
    "jusqu'à ", "jusqu'au ", "récupérer à ", "récupérer au ",
    "aller chercher ", "amener à ", "amener au ", "conduire à ", "conduire au ",
    "emmener à ", "emmener au ", "se rendre à ", "se rendre au ",
    "déposer à ", "déposer au ", "pour aller ", "pour aller à ", "pour aller au ",
    "à destination de ", "en direction de ", "en route vers "
)

private val timeKeywords = listOf(
    " à ", " a ", "heure :", "heure:", " h ", "h ", "h:", "hh",
    "maintenant", "rdv :", "rdv:", "prévu à", "prevu a", "prevu ",
    "dès que", "des que", "des que possible",
    "immediatement", "immédiatement", "urgent", "vite", " asap ",
    "dans ", "cet après-midi", "cet aprèm", "cette après-midi", "cette après midi",
    "cet aprem", "cette aprem", "ce matin", "ce soir", "ce midi",
    "demain", "aujourd'hui", "aujourd hui", "aujourdhui",
    "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche",
    "lun ", "mar ", "mer ", "jeu ", "ven ", "sam ", "dim ",
    "lundi prochain", "mardi prochain", "mercredi prochain",
    "jeudi prochain", "vendredi prochain", "samedi prochain", "dimanche prochain",
    "at ", "time: ", "time :", "vers ", "aux alentours de ",
    "aux environs de ", "vers les ", "vers le ",
    "pour ", "pour ce", "pour ",
    "après ", "après ", "avant ", "entre ",
    "dans la matinée", "dans l'après-midi", "dans la soirée", "dans l'aprem",
    "en soirée", "en matinée", "début d'après-midi",
    "ce matin", "ce midi", "cet après-midi", "cette après-midi", "ce soir",
    "cette nuit", "très tôt", "tard",
    "réveil", "matin", "soir", "nuit",
    "lendemain"
)

private val confirmationKeywords = listOf(
    "confirme", "confirmation",
    "ok", "okay", "oki", "oué", "ouep", "ouéé", "compris", "entendu",
    "reçu", "recus", "bien reçu", "bien recu", "roger", "yes",
    "d'accord", "dacc", "ok dac", "okdac", "nickel", "parfait",
    "super", "génial", "genial", "merci", "bonne journée", "bonne journee",
    "bonne soirée", "bonne soiree", "merci beaucoup", "merci bcp",
    "je confirme", "confirmé", "confirmee", "c'est noté", "c'est reglé", "c'est bon",
    "validé", "valide", "accepté", "accepte",
    "impeccable", "parfaitement", "top", "ça marche", "ca marche",
    "bon pour moi", "c'est bon pour moi", "très bien", "tres bien",
    "pour moi c'est bon", "tout bon"
)

private val cancelKeywords = listOf(
    "annule", "annuler", "annulation", "annulez", "supprime", "supprimer",
    "suppression", "efface", "effacer", "désist", "desist", "abandon",
    "ne vient pas", "ne peut pas", "cancel", "cancelled",
    "delete", "stop", "stopped", "stopper", "ne plus", "stoppé", "stoppee",
    "plus besoin", "je n'ai plus", "pas la peine", "inutile",
    "c'est annulé", "annule", "c'est bon j'ai", "j'ai trouvé",
    "je me désiste", "je me desiste", "pas nécessaire", "pas necessaire",
    "pas besoin finalement", "finalement non",
    "j'ai oublié", "j'ai oublie", "oubli", "plus tard",
    "report", "reporter", "remettre", "déprogrammer", "deprogrammer",
    "annulation de course", "supprimer la course",
    "finalement pas besoin", "non merci"
)

private val modifyKeywords = listOf(
    "modifie", "modifier", "modification", "change", "changer", "changement",
    "rectifie", "rectifier", "corrige", "corriger", "mauvais", "erreur",
    "faux", "invers", "swap", "different", "autre", "nouveau", "nouvelle",
    "mise à jour", "mise a jour", "update", "instead", "actually", "en fait",
    "plutot", "plutôt", "autrement", "à la place", "je voudrais", "je veux",
    "finalement", "en réalité", "en realite",
    "modif", "changement de plan", "changement de programme",
    "décaler", "decaler", "repousser", "avancer", "retarder",
    "modification d'adresse", "modification adresse",
    "changement d'adresse", "changement adresse",
    "changer l'heure", "changer le lieu",
    "je souhaite modifier", "on modifie",
    "finalement ce sera", "finalement c'est",
    "au lieu de", "a la place de"
)

private val nbPassengersKeywords = listOf(
    "passager", "passagers", "personne", "personnes", "gens", "voyageur",
    "voyageurs", " occupants", " occupants", " personne", " personnes",
    "2 personnes", "3 personnes", "4 personnes", "1 personne", "1pers", "2pers", "3pers", "4pers",
    " à ", " personnes", "passagers", "personne"
)

private val nameKeywords = listOf(
    "je m'appelle ", "je m'apelle ", "mon nom est ", "mon prénom est ",
    "c'est ", "moi c'est ", "ici ", "nom :", "nom:", "prénom :", "prenom:",
    "mr ", "mme ", "mlle ", "monsieur ", "madame ", "mademoiselle ",
    "my name is ", "this is ", "name: ", "name ",
    "appelle-moi ", "appelez-moi ", "client :", "client:",
    "de la part de ", "pour le compte de ",
    "réservé par ", "reserve par ", "au nom de "
)

private val priceKeywords = listOf(
    " euros", "euros", "€", "eur", " prix ", "cout", "coût", "tarif",
    "environ ", "env ", "approx", "approximativement",
    "prix", "forfait", "montant", "total", "facture",
    "règlement", "reglement", "payer", "régler", "regler",
    "estimation", "devis", "combien"
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
    val estimatedDuration = estimateDuration(estimatedDistance)
    val distanceDomicileEst = estimatedDistance * 0.3
    val coutDeplacement = settings?.let { distanceDomicileEst * it.coutParKmDeplacement } ?: 0.0
    val estimatedProfitability = RideRequest.calculateProfitability(estimatedPrice, coutDeplacement)
    
    return RideRequest(
        id = RideRequest.createStableId(sender, body, timestamp),
        sender = sender,
        body = body,
        departure = parsed.departure,
        arrival = parsed.arrival,
        time = parsed.time,
        date = parsed.date,
        clientName = parsed.clientLastName,
        clientFirstName = parsed.clientFirstName,
        distanceKm = estimatedDistance,
        price = estimatedPrice,
        durationMinutes = estimatedDuration,
        fuelCost = coutDeplacement,
        operatingCost = 0.0,
        profitabilityPercent = estimatedProfitability,
        timestamp = timestamp,
        status = RideStatus.DRAFT
    )
}

private fun estimateDistance(departure: String, arrival: String): Double {
    if (departure.isBlank() || arrival.isBlank()) return 0.0
    
    val parisKeywords = listOf("paris", "cdg", "orly", "gare du nord", "gare de lyon", "chatelet", "opera", "defence", "la défense", "montparnasse", "saint-lazare", "gare de l'est", "gare d'austerlitz", "austerlitz")
    val suburbKeywords = listOf("versailles", "saint-denis", "creteil", "creteil", "nanterre", "bobigny", "evry", "cergy", "meaux", "pontoise", "melun", "fontainebleau", "st-denis", "saint denis", "boulogne", "billancourt", "montreuil", "argenteuil", "colombes", "vitry", "aubervilliers", "asnieres", "la defense", "le bourget")
    
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
    
    // Resolve overlapping tokens (preferring higher confidence)
    val resolvedTokens = mutableListOf<SmsToken>()
    for (token in allTokens.sortedWith(compareBy({ it.start }, { -it.confidence }))) {
        val overlaps = resolvedTokens.any { accepted ->
            token.start < accepted.end && accepted.start < token.end
        }
        if (!overlaps) {
            resolvedTokens.add(token)
        }
    }
    val sortedTokens = resolvedTokens.sortedBy { it.start }
    
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
        
        if (token.field == SmsField.DE || token.field == SmsField.VERS) {
            for (j in i + 1 until sortedTokens.size) {
                if (sortedTokens[j].field != token.field) {
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
    
    // Extraction des noms
    val (firstName, lastName) = extractNamesFromBody(body)
    
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
        clientFirstName = firstName,
        clientLastName = lastName,
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
    
    // Format 14h30
    val hPattern = Regex("(\\d{1,2})h(\\d{0,2})")
    for (m in hPattern.findAll(lowerBody)) {
        val h = m.groupValues[1].toIntOrNull() ?: continue
        val min = m.groupValues[2].ifEmpty { "00" }
        if (h in 0..23 && min.toInt() in 0..59) {
            times.add("${h}h${min}")
        }
    }
    
    // Format 14:30
    val colonPattern = Regex("(\\d{1,2}):(\\d{2})")
    for (m in colonPattern.findAll(lowerBody)) {
        val h = m.groupValues[1].toIntOrNull() ?: continue
        val min = m.groupValues[2].toIntOrNull() ?: continue
        if (h in 0..23 && min in 0..59) {
            times.add("$h:$min")
        }
    }
    
    // Format "dans 30min" ou "dans 1h" ou "dans 30 minutes" ou "dans 1 heure"
    val dansPattern = Regex("dans\\s+(\\d+)\\s*(min|minutes|heure|heures|h)")
    for (m in dansPattern.findAll(lowerBody)) {
        val value = m.groupValues[1]
        val unit = m.groupValues[2]
        times.add("dans ${value}${if (unit.startsWith("h")) "h" else "min"}")
    }

    // Format "dans 1h30" (ex: "dans 1h30" → dans 1h30)
    val dansHrMinPattern = Regex("dans\\s+(\\d+)h(\\d{1,2})")
    for (m in dansHrMinPattern.findAll(lowerBody)) {
        val h = m.groupValues[1]
        val min = m.groupValues[2]
        times.add("dans ${h}h${min}")
    }

    // Format "pour 14h", "pour 14h30"
    val pourPattern = Regex("pour\\s+(\\d{1,2})h(\\d{0,2})")
    for (m in pourPattern.findAll(lowerBody)) {
        val h = m.groupValues[1].toIntOrNull() ?: continue
        val min = m.groupValues[2].ifEmpty { "00" }
        if (h in 0..23 && min.toInt() in 0..59) {
            times.add("${h}h${min}")
        }
    }

    // Mots-clés temporels
    if (lowerBody.contains("demain")) times.add("demain")
    if (lowerBody.contains("aujourd'hui") || lowerBody.contains("aujourd hui")) times.add("aujourd'hui")
    if (lowerBody.contains("ce soir")) times.add("ce soir")
    if (lowerBody.contains("ce matin")) times.add("ce matin")
    if (lowerBody.contains("ce midi") || lowerBody.contains("à midi")) times.add("12h")
    if (lowerBody.contains("cet après") || lowerBody.contains("cet aprem")) times.add("cet après-midi")
    if (lowerBody.contains("maintenant") || lowerBody.contains("immédiatement") || lowerBody.contains("urgent")) times.add("maintenant")
    
    return times.firstOrNull() ?: ""
}

private fun extractDateFromBody(body: String): String {
    val lowerBody = body.lowercase()
    val cal = java.util.Calendar.getInstance()
    
    // Aujourd'hui
    if (lowerBody.contains("aujourd'hui") || lowerBody.contains("aujourd hui") || lowerBody.contains("ce jour")) {
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }
    
    // Demain
    if (lowerBody.contains("demain")) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
    }
    
    // Après-demain
    if (lowerBody.contains("après-demain") || lowerBody.contains("apres-demain")) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
    }
    
    // Après-demain
    if (lowerBody.contains("après-demain") || lowerBody.contains("apres-demain") || lowerBody.contains("après demain") || lowerBody.contains("apres demain")) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
    }

    // Jours de la semaine (ex: "lundi", "mardi prochain", "lun", "mar")
    val days = mapOf(
        "lundi" to java.util.Calendar.MONDAY,
        "lun" to java.util.Calendar.MONDAY,
        "mardi" to java.util.Calendar.TUESDAY,
        "mar" to java.util.Calendar.TUESDAY,
        "mercredi" to java.util.Calendar.WEDNESDAY,
        "mer" to java.util.Calendar.WEDNESDAY,
        "jeudi" to java.util.Calendar.THURSDAY,
        "jeu" to java.util.Calendar.THURSDAY,
        "vendredi" to java.util.Calendar.FRIDAY,
        "ven" to java.util.Calendar.FRIDAY,
        "samedi" to java.util.Calendar.SATURDAY,
        "sam" to java.util.Calendar.SATURDAY,
        "dimanche" to java.util.Calendar.SUNDAY,
        "dim" to java.util.Calendar.SUNDAY
    )
    
    for ((dayName, dayConst) in days) {
        if (lowerBody.contains(dayName)) {
            val currentDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val targetDayOfWeek = when (dayConst) {
                java.util.Calendar.MONDAY -> java.util.Calendar.MONDAY
                java.util.Calendar.TUESDAY -> java.util.Calendar.TUESDAY
                java.util.Calendar.WEDNESDAY -> java.util.Calendar.WEDNESDAY
                java.util.Calendar.THURSDAY -> java.util.Calendar.THURSDAY
                java.util.Calendar.FRIDAY -> java.util.Calendar.FRIDAY
                java.util.Calendar.SATURDAY -> java.util.Calendar.SATURDAY
                java.util.Calendar.SUNDAY -> java.util.Calendar.SUNDAY
                else -> currentDayOfWeek
            }
            var daysToAdd = targetDayOfWeek - currentDayOfWeek
            if (daysToAdd <= 0) daysToAdd += 7
            if (lowerBody.contains("prochain")) {
                daysToAdd += 7
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
            return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
        }
    }
    
    // Format numérique JJ/MM/AAAA ou JJ/MM
    val datePattern = Regex("(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?")
    val match = datePattern.find(lowerBody)
    if (match != null) {
        val day = match.groupValues[1]
        val month = match.groupValues[2]
        val year = match.groupValues.getOrNull(3)?.let { 
            if (it.length == 2) "20$it" else it 
        } ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
        return "$day/$month/$year"
    }
    
    // Format texte "15 janvier", "15 jan"
    val months = mapOf(
        "janvier" to "01", "jan" to "01",
        "février" to "02", "fevrier" to "02", "fev" to "02",
        "mars" to "03",
        "avril" to "04", "avr" to "04",
        "mai" to "05",
        "juin" to "06",
        "juillet" to "07", "jul" to "07",
        "août" to "08", "aout" to "08",
        "septembre" to "09", "sep" to "09",
        "octobre" to "10", "oct" to "10",
        "novembre" to "11", "nov" to "11",
        "décembre" to "12", "decembre" to "12", "dec" to "12"
    )
    
    for ((monthName, monthNum) in months) {
        val pattern = Regex("(\\d{1,2})\\s+$monthName")
        val m = pattern.find(lowerBody)
        if (m != null) {
            val day = m.groupValues[1]
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            return "$day/$monthNum/$year"
        }
    }
    
    return ""
}

private fun extractNamesFromBody(body: String): Pair<String, String> {
    val lowerBody = body.lowercase()
    var firstName = ""
    var lastName = ""

    // Mot-clés explicites
    for (keyword in nameKeywords) {
        val idx = lowerBody.indexOf(keyword)
        if (idx != -1) {
            val start = idx + keyword.length
            val end = lowerBody.indexOfAny(charArrayOf('.', ',', '!', '?', ';'), start)
            val rawName = if (end != -1) body.substring(start, end) else body.substring(start)
            val cleanName = rawName.trim().replaceFirstChar { it.uppercase() }
            
            if (keyword.contains("nom") || keyword.contains("mr") || keyword.contains("mme")) {
                lastName = cleanName
            } else {
                firstName = cleanName
            }
            break
        }
    }

    // Détection après salutation (ex: "Bonjour Jean,")
    if (firstName.isBlank()) {
        val greetingPattern = Regex("(?:bonjour|slt|salut|hello|hi)\\s+([A-Z][a-z]+)")
        val match = greetingPattern.find(body)
        if (match != null) {
            firstName = match.groupValues[1]
        }
    }

    return Pair(firstName, lastName)
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
