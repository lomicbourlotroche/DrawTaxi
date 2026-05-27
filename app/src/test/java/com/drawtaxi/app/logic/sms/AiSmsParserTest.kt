package com.drawtaxi.app.logic.sms

import org.junit.Assert.*
import org.junit.Test

class AiSmsParserTest {

    // ── isTaxiRelatedRegex ──

    @Test
    fun testIsTaxiRelatedWithTaxiKeyword() {
        assertTrue(AiSmsParser.isTaxiRelatedRegex("Bonjour je voudrais un taxi"))
    }

    @Test
    fun testIsTaxiRelatedWithCourseKeyword() {
        assertTrue(AiSmsParser.isTaxiRelatedRegex("Je veux réserver une course"))
    }

    @Test
    fun testIsTaxiRelatedWithAeroportKeyword() {
        assertTrue(AiSmsParser.isTaxiRelatedRegex("Départ pour l'aéroport à 8h"))
    }

    @Test
    fun testIsTaxiRelatedWithNonTaxiMessage() {
        assertFalse(AiSmsParser.isTaxiRelatedRegex("Bonjour comment allez-vous ?"))
    }

    @Test
    fun testIsTaxiRelatedWithSpamMessage() {
        assertFalse(AiSmsParser.isTaxiRelatedRegex("Gagnez 1000€ par jour !"))
    }

    @Test
    fun testIsTaxiRelatedWithUberKeyword() {
        assertTrue(AiSmsParser.isTaxiRelatedRegex("Je commande un uber"))
    }

    @Test
    fun testIsTaxiRelatedWithGareKeyword() {
        assertTrue(AiSmsParser.isTaxiRelatedRegex("Direction la gare"))
    }

    @Test
    fun testIsTaxiRelatedEmptyString() {
        assertFalse(AiSmsParser.isTaxiRelatedRegex(""))
    }

    // ── extractJsonFromResponse ──

    @Test
    fun testExtractJsonFromCodeBlock() {
        val response = """Voici le résultat :
```json
{"departure": "Paris", "arrival": "Lyon", "time": "14h30"}
```
Merci"""
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNotNull(json)
        assertTrue(json?.contains("\"departure\"") ?: false)
        assertTrue(json?.contains("\"Paris\"") ?: false)
    }

    @Test
    fun testExtractJsonFromCodeBlockWithoutJsonLabel() {
        val response = """```
{"departure": "Paris", "arrival": "Lyon", "time": "14h30"}
```"""
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNotNull(json)
        assertTrue(json?.contains("\"departure\"") ?: false)
    }

    @Test
    fun testExtractJsonFromRawResponse() {
        val response = """{"departure": "Paris", "arrival": "Lyon", "time": "14h30"}"""
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNotNull(json)
        assertEquals(response, json)
    }

    @Test
    fun testExtractJsonWithTextBeforeAndAfter() {
        val response = """Voici les infos: {"departure": "Paris", "arrival": "Lyon", "time": "14h30"} Fin."""
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNotNull(json)
    }

    @Test
    fun testExtractJsonWithNoJsonReturnsNull() {
        val response = "Je n'ai pas trouvé d'information dans ce message."
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNull(json)
    }

    @Test
    fun testExtractJsonWithMalformedBrackets() {
        val response = """{"departure": "Paris", "arrival": "Lyon", "time": """
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNull(json)
    }

    @Test
    fun testExtractJsonWithNestedBraces() {
        val response = """{"departure": "Paris", "data": {"inner": "value"}, "time": "14h"}"""
        val json = AiSmsParser.extractJsonFromResponse(response)
        assertNotNull(json)
        assertTrue(json?.contains("\"time\"") ?: false)
    }

    // ── validatePhoneExtension ──

    @Test
    fun testValidatePhoneValidFrenchMobile() {
        assertEquals("0612345678", "0612345678".validatePhoneExtension())
    }

    @Test
    fun testValidatePhoneValidFrenchInternational() {
        assertEquals("+33612345678", "+33612345678".validatePhoneExtension())
    }

    @Test
    fun testValidatePhoneInvalidTooShort() {
        assertEquals("", "12345".validatePhoneExtension())
    }

    @Test
    fun testValidatePhoneInvalidWithLetters() {
        assertEquals("0612345678", "0612345678abc".validatePhoneExtension())
    }

    @Test
    fun testValidatePhoneEmpty() {
        assertEquals("", "".validatePhoneExtension())
    }

    // ── validateEmailExtension ──

    @Test
    fun testValidateEmailValid() {
        assertEquals("jean.dupont@email.com", "jean.dupont@email.com".validateEmailExtension())
    }

    @Test
    fun testValidateEmailInvalidNoAt() {
        assertEquals("", "jean.dupontemail.com".validateEmailExtension())
    }

    @Test
    fun testValidateEmailInvalidNoDomain() {
        assertEquals("", "jean@".validateEmailExtension())
    }

    @Test
    fun testValidateEmailEmpty() {
        assertEquals("", "".validateEmailExtension())
    }

    // ── cleanEmailBody ──

    @Test
    fun testCleanEmailBodyRemovesSignature() {
        val body = """Bonjour, je voudrais un taxi pour demain.
--
Signé: Jean"""
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertTrue(cleaned.startsWith("Bonjour"))
        assertFalse(cleaned.contains("Signé"))
    }

    @Test
    fun testCleanEmailBodyRemovesForwardHeader() {
        val body = """Réservation de taxi
Le 27/05/2026 à 14h30
De : Jean Dupont
À : Service Taxi
Objet : Course

Bonjour, je souhaite réserver une course."""
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertFalse(cleaned.contains("De :"))
        assertFalse(cleaned.contains("À :"))
        assertFalse(cleaned.contains("Objet :"))
    }

    @Test
    fun testCleanEmailBodyRemovesEnvoyeDepuis() {
        val body = """Course pour demain 8h.
Envoyé depuis mon iPhone"""
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertEquals("Course pour demain 8h.", cleaned)
    }

    @Test
    fun testCleanEmailBodyShortBodyUnchanged() {
        val body = "Bonjour taxi pour 14h"
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertEquals(body, cleaned)
    }

    @Test
    fun testCleanEmailBodyRemovesSentFrom() {
        val body = """Réservation pour demain.
Sent from my Android"""
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertEquals("Réservation pour demain.", cleaned)
    }

    @Test
    fun testCleanEmailBodyRemovesSeparator() {
        val body = """Course pour l'aéroport.
_______________
Suite de la conversation"""
        val cleaned = AiSmsParser.cleanEmailBody(body)
        assertFalse(cleaned.contains("_______________"))
    }

    // ── parseWithFallback ──

    @Test
    fun testParseWithFallbackExtractsBasicInfo() {
        val result = AiSmsParser.parseWithFallback("Taxi depuis Paris vers Lyon à 14h30")
        assertEquals("Paris", result.departure)
        assertEquals("Lyon", result.arrival)
        assertEquals("14h30", result.time)
        assertTrue(result.aiReasoning.contains("regex fallback"))
    }

    @Test
    fun testParseWithFallbackNonTaxiMessage() {
        val result = AiSmsParser.parseWithFallback("Bonjour")
        assertNotNull(result)
        assertEquals("", result.departure)
        assertEquals("", result.arrival)
        assertEquals("", result.time)
    }

    @Test
    fun testParseWithFallbackIncompleteInfo() {
        val result = AiSmsParser.parseWithFallback("Taxi vers Monaco")
        assertEquals("Monaco", result.arrival)
        assertTrue(result.missingFields.isNotEmpty())
    }

    @Test
    fun testParseWithFallbackMissingFieldsWhenDepartureMissing() {
        val result = AiSmsParser.parseWithFallback("Taxi pour Paris à 14h")
        assertEquals("", result.departure)
        assertEquals("Paris", result.arrival)
        assertTrue(result.missingFields.any { it.contains("départ") || it.contains("depart") })
    }

    @Test
    fun testIsFormspreeEmail() {
        val formspreeBody = """
            Bonjour Jean,
            You've received a new form submission on formulaire réservation VTC.
            Nom bob leponge
            Téléphone 0664183172
            Prestation Depuis la gare
        """.trimIndent()
        assertTrue(AiSmsParser.isFormspreeEmail(formspreeBody))

        val normalBody = "Bonjour, je veux un taxi."
        assertFalse(AiSmsParser.isFormspreeEmail(normalBody))
    }

    @Test
    fun testParseFormspreeEmail() {
        val emailBody = """
-------- Courriel original --------
Objet: Réservation TY-ZEF - bob leponge - 27/05/2026
Date: 27.05.2026 14:12
De: Formspree <noreply@formspree.io>
À: contact@tyzefbrestvtc.fr
Répondre à: bob.leponge@plouf.fr

  You've received a new form submission. --

  New form submission on formulaire réservation VTC

  Someone just submitted a form on null. Here's what they had to say:

  Nom bob leponge

  Téléphone 0664183172

  Email bob.leponge@plouf.fr

  Prestation TY-ZEF Brest VTC

  Date 27/05/2026

  Horaire 18:12

  Adresse de départ Gare de Brest

  Adresse de destination Fort Montbarey, Route du Fort Montbarey, Brest

  Détails test

  Submitted 12:12 PM - 27 May 2026

  Mark as spam

   [1]

  You are receiving this because you confirmed this email address on Formspree.
        """.trimIndent()

        val result = AiSmsParser.parseFormspreeEmail(emailBody)
        assertNotNull(result)
        assertEquals("Gare de Brest", result?.departure)
        assertEquals("Fort Montbarey, Route du Fort Montbarey, Brest", result?.arrival)
        assertEquals("18:12", result?.time)
        assertEquals("27/05/2026", result?.date)
        assertEquals("leponge", result?.clientName)
        assertEquals("bob", result?.clientFirstName)
        assertEquals("0664183172", result?.phone)
        assertEquals("bob.leponge@plouf.fr", result?.email)
        assertEquals(1.0f, result?.confidence ?: 0f, 0.01f)
    }

    @Test
    fun testCleanEmailBodyDoesNotDestroyFormspreeContent() {
        val emailBody = """
-------- Courriel original --------
Objet: Réservation TY-ZEF - bob leponge - 27/05/2026
Date: 27.05.2026 14:12
De: Formspree <noreply@formspree.io>
À: contact@tyzefbrestvtc.fr
Répondre à: bob.leponge@plouf.fr

  You've received a new form submission. --

  New form submission on formulaire réservation VTC

  Someone just submitted a form on null. Here's what they had to say:

  Nom bob leponge
        """.trimIndent()

        val cleaned = AiSmsParser.cleanEmailBody(emailBody)
        assertTrue(cleaned.contains("Nom bob leponge"))
    }

    // ── isValidJson ──

    // isValidJson is a thin wrapper around JSONObject - behavior differs between
    // Android (strict) and JVM (lenient for unquoted keys, lone values).
    // Skipping environment-specific tests as they don't validate business logic.
}
