package com.drawtaxi.app.logic.geocoding

object AddressNormalizer {

    private val abbreviations = mapOf(
        "r." to "rue", "av." to "avenue", "bd." to "boulevard", "bld." to "boulevard",
        "blvd." to "boulevard", "pl." to "place", "imp." to "impasse", "chem." to "chemin",
        "rte." to "route", "res." to "residence", "resid." to "residence",
        "app." to "appartement", "apt." to "appartement", "bat." to "batiment",
        "bt." to "batiment", "et." to "etage", "lieu-dit" to "lieu dit",
        "ld." to "lieu dit", "q." to "quartier", "quai." to "quai",
        "cours." to "cours", "allee." to "allee", "all." to "allee",
        "sq." to "square", "pass." to "passage", "pont." to "pont",
        "porte." to "porte", "parc." to "parc", "zone" to "zone",
        "zac." to "zac", "zi." to "zone industrielle", "za." to "zone artisanale"
    )

    private val accentCorrections = mapOf(
        "marechal" to "maréchal", "general" to "général",
        "republique" to "république", "liberation" to "libération",
        "prefecture" to "préfecture", "eglise" to "église",
        "ecole" to "école", "hopital" to "hôpital",
        "pharmacie" to "pharmacie", "mairie" to "mairie",
        "cimetiere" to "cimetière", "theatre" to "théâtre",
        "musee" to "musée", "stade" to "stade",
        "parking" to "parking", "centre commercial" to "centre commercial",
        "hyper" to "hyper", "super" to "super", "u." to "u",
        "leclerc" to "leclerc", "auchan" to "auchan", "carrefour" to "carrefour",
        "casino" to "casino", "intermarche" to "intermarché",
        "boulangerie" to "boulangerie", "boucherie" to "boucherie",
        "patisserie" to "pâtisserie", "restaurant" to "restaurant",
        "cafe" to "café", "hotel" to "hôtel",
        "gare" to "gare", "aeroport" to "aéroport"
    )

    private val noiseWords = listOf(
        "chez", "face a", "pres de", "a cote de", "en face de",
        "devant", "derriere", "a proximite de", "tout pres de",
        "juste devant", "juste derriere", "en sortie de",
        "a l'angle de", "au coin de", "au niveau de",
        "arrivee", "depart", "destination", "adresse",
        "je suis a", "je suis au", "je suis a l'",
        "rdv", "rendez-vous", "rejoindre", "venir a",
        "allez a", "aller a", "direction"
    )

    private val poiPatterns = listOf(
        "gare de {city}", "gare {city}", "gare",
        "aeroport de {city}", "aeroport {city}", "aeroport",
        "hotel de ville", "mairie de {city}", "mairie",
        "hopital de {city}", "hopital", "clinique de {city}", "clinique",
        "centre commercial de {city}", "centre commercial",
        "stade de {city}", "stade",
        "parking de {city}", "parking",
        "ecole de {city}", "ecole",
        "eglise de {city}", "eglise",
        "pharmacie de {city}", "pharmacie",
        "restaurant de {city}", "restaurant",
        "cafe de {city}", "cafe",
        "boulangerie de {city}", "boulangerie"
    )

    private val brestCities = listOf(
        "Brest", "Gouesnou", "Guipavas", "Le Relecq-Kerhuon",
        "Plougastel-Daoulas", "Plouzané", "Saint-Renan",
        "Landerneau", "Daoulas", "Plabennec", "Guilers",
        "Bohars", "Milizac", "Ploumoguer", "Locmaria-Plouzané",
        "Le Conquet", "Plougonvelin", "Saint-Renan",
        "Brest Metropole", "Brest Centre", "Brest Recouvrance",
        "Brest Lambézellec", "Brest Saint-Marc", "Brest Bellevue"
    )

    fun normalize(address: String): List<String> {
        val variations = mutableSetOf<String>()

        variations.add(address.trim())

        val expanded = expandAbbreviations(address)
        variations.add(expanded)

        val corrected = correctAccents(address)
        variations.add(corrected)

        val cleaned = removeNoise(address)
        if (cleaned.isNotBlank()) variations.add(cleaned)

        val noNumber = removeHouseNumber(address)
        if (noNumber.isNotBlank()) variations.add(noNumber)

        val noDetails = removeInternalDetails(address)
        if (noDetails.isNotBlank()) variations.add(noDetails)

        val streetOnly = extractStreetOnly(address)
        if (streetOnly.isNotBlank()) variations.add(streetOnly)

        val streetCity = extractStreetAndCity(address)
        if (streetCity.isNotBlank()) variations.add(streetCity)

        for (city in brestCities) {
            variations.add("$address, $city")
            variations.add("$expanded, $city")
        }

        for (pattern in poiPatterns) {
            if (address.lowercase().contains(pattern.replace("{city}", "").trim())) {
                for (city in brestCities.take(3)) {
                    variations.add(pattern.replace("{city}", city))
                }
            }
        }

        val reversed = reverseWordOrder(address)
        if (reversed.isNotBlank()) variations.add(reversed)

        val simplified = simplifyAddress(address)
        if (simplified.isNotBlank()) variations.add(simplified)

        return variations.distinct().take(20)
    }

    private fun expandAbbreviations(address: String): String {
        var result = address
        for ((abbr, full) in abbreviations) {
            val pattern = Regex("\\b${Regex.escape(abbr)}\\b", RegexOption.IGNORE_CASE)
            result = pattern.replace(result, full)
        }
        return result
    }

    private fun correctAccents(address: String): String {
        var result = address
        for ((without, with) in accentCorrections) {
            val pattern = Regex("\\b${Regex.escape(without)}\\b", RegexOption.IGNORE_CASE)
            result = pattern.replace(result, with)
        }
        return result
    }

    private fun removeNoise(address: String): String {
        var result = address
        for (noise in noiseWords) {
            val pattern = Regex("\\b${Regex.escape(noise)}\\b", RegexOption.IGNORE_CASE)
            result = pattern.replace(result, "")
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun removeHouseNumber(address: String): String {
        return address.replace(Regex("^\\d+[\\s\\-]*"), "").trim()
    }

    private fun removeInternalDetails(address: String): String {
        val details = Regex(
            "\\b(appartement|apt|batiment|bat|etage|code postal|ce\\\\?dex|" +
            "boite postale|bp|cs|escalier|porte|couloir|" +
            "resi|residence)\\s*[:\\-]?\\s*\\S+",
            RegexOption.IGNORE_CASE
        )
        return details.replace(address, "").replace(Regex("\\s+"), " ").trim()
    }

    private fun extractStreetOnly(address: String): String {
        val streetTypes = listOf(
            "rue", "avenue", "boulevard", "impasse", "chemin",
            "route", "place", "allee", "square", "passage",
            "quai", "pont", "porte", "parc", "residence"
        )
        for (type in streetTypes) {
            val pattern = Regex("(?:^|,)\\s*($type\\s+[^,]+)", RegexOption.IGNORE_CASE)
            val match = pattern.find(address)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return ""
    }

    private fun extractStreetAndCity(address: String): String {
        val postalCodePattern = Regex("(\\d{5})\\s*([\\w\\s\\-]+)")
        val match = postalCodePattern.find(address)
        if (match != null) {
            val city = match.groupValues[2].trim()
            val street = removeHouseNumber(address).replace(Regex("\\d{5}.*"), "").trim()
            return if (street.isNotBlank()) "$street, $city" else city
        }
        return ""
    }

    private fun reverseWordOrder(address: String): String {
        val parts = address.split(",").map { it.trim() }
        if (parts.size >= 2) {
            return parts.reversed().joinToString(", ")
        }
        return ""
    }

    private fun simplifyAddress(address: String): String {
        return address
            .lowercase()
            .replace(Regex("[^a-z0-9\\s,\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
