package com.drawtaxi.app.logic.sms

import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import org.junit.Assert.*
import org.junit.Test

class RideMatcherTest {

    @Test
    fun testNewRideSenderNoPreviousRides() {
        val result = RideMatcher.matchSmsToRides(
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00",
            pendingRides = emptyList()
        )

        assertEquals(RideMatchResult.NEW_RIDE, result.result)
        assertNull(result.matchedRide)
        assertEquals("Nouveau client", result.reason)
    }

    @Test
    fun testDeletionMatch() {
        val existingRide = RideRequest(
            id = "1",
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00",
            departure = "Paris",
            arrival = "Orly",
            time = "12h00",
            status = RideStatus.CONFIRMED
        )

        val result = RideMatcher.matchSmsToRides(
            sender = "+33612345678",
            body = "Annuler ma course pour Orly svp",
            pendingRides = listOf(existingRide)
        )

        assertEquals(RideMatchResult.DELETION, result.result)
        assertEquals(existingRide, result.matchedRide)
    }

    @Test
    fun testModificationMatch() {
        val existingRide = RideRequest(
            id = "1",
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00",
            departure = "Paris",
            arrival = "Orly",
            time = "12h00",
            status = RideStatus.CONFIRMED
        )

        val result = RideMatcher.matchSmsToRides(
            sender = "+33612345678",
            body = "Bonjour, je voudrais modifier l'heure pour Orly, plutôt à 13h00",
            pendingRides = listOf(existingRide)
        )

        assertEquals(RideMatchResult.MODIFICATION, result.result)
        assertEquals(existingRide, result.matchedRide)
    }

    @Test
    fun testAdditionMatch() {
        val existingRide = RideRequest(
            id = "1",
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00",
            departure = "Paris",
            arrival = "Orly",
            time = "12h00",
            status = RideStatus.CONFIRMED
        )

        val result = RideMatcher.matchSmsToRides(
            sender = "+33612345678",
            body = "Pouvez-vous ajouter une seconde course pour demain matin ?",
            pendingRides = listOf(existingRide)
        )

        assertEquals(RideMatchResult.ADDITION, result.result)
        assertNull(result.matchedRide)
    }

    @Test
    fun testDuplicateDetection() {
        val existingRide = RideRequest(
            id = "1",
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00",
            departure = "Paris",
            arrival = "Orly",
            time = "12h00",
            status = RideStatus.DRAFT
        )

        val result = RideMatcher.matchSmsToRides(
            sender = "+33612345678",
            body = "Taxi de Paris vers Orly à 12h00", // Exact same message body
            pendingRides = listOf(existingRide)
        )

        assertEquals(RideMatchResult.DUPLICATE, result.result)
        assertEquals(existingRide, result.matchedRide)
    }
}
