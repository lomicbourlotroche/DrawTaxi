package com.drawtaxi.app.logic

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testDebugParseSms() {
        val body = "Taxi depuis l'adresse : 12 rue de la Paix pour l'Aéroport CDG heure : 8h00"
        val sender = "+33612345678"
        val parsed = parseSmsAdvanced(sender, body)
        println("DEPARTURE: '${parsed.departure}'")
        println("ARRIVAL: '${parsed.arrival}'")
        println("TIME: '${parsed.time}'")
    }

    @Test
    fun testParseStandardSms() {
        val body = "Taxi depuis Paris vers Lyon à 14h30"
        val sender = "+33612345678"
        val ride = parseSms(sender, body)

        assertNotNull(ride)
        assertEquals("Paris", ride?.departure)
        assertEquals("Lyon", ride?.arrival)
        assertEquals("14h30", ride?.time)
        assertEquals(sender, ride?.sender)
    }

    @Test
    fun testParseWithDifferentKeywords() {
        val body = "Taxi depuis l'adresse : 12 rue de la Paix pour l'Aéroport CDG heure : 8h00"
        val sender = "Client123"
        val ride = parseSms(sender, body)

        assertNotNull(ride)
        assertEquals("12 rue de la Paix", ride?.departure)
        assertEquals("L'Aéroport CDG", ride?.arrival)
        assertEquals("8h00", ride?.time)
    }

    @Test
    fun testParseIncompleteSms() {
        val body = "Bonjour je veux un taxi"
        val ride = parseSms("123", body)
        assertNotNull(ride)
        assertEquals("", ride?.departure)
        assertEquals("", ride?.arrival)
    }

    @Test
    fun testParseWithPartialInfo() {
        val body = "Taxi vers Monaco"
        val ride = parseSms("123", body)
        assertNotNull(ride)
        assertEquals("", ride?.departure)
        assertEquals("Monaco", ride?.arrival)
    }
}
