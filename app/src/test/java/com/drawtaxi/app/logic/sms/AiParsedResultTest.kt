package com.drawtaxi.app.logic.sms

import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideStatus
import org.junit.Assert.*
import org.junit.Test

class AiParsedResultTest {

    @Test
    fun testToRideRequestWithAllFields() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            date = "27/05/2026",
            clientName = "Dupont",
            clientFirstName = "Jean",
            phone = "+33612345678",
            email = "jean.dupont@email.com",
            nbPassengers = 2,
            price = 50.0,
            confidence = 0.85f
        )

        val ride = result.toRideRequest("+33612345678", 1000L)
        assertNotNull(ride)
        assertEquals("Paris", ride?.departure)
        assertEquals("Lyon", ride?.arrival)
        assertEquals("14h30", ride?.time)
        assertEquals("27/05/2026", ride?.date)
        assertEquals("Jean Dupont", ride?.clientName)
        assertEquals("jean.dupont@email.com", ride?.clientEmail)
        assertEquals(50.0, ride?.price ?: 0.0, 0.001)
        assertEquals(RideStatus.DRAFT, ride?.status)
    }

    @Test
    fun testToRideRequestWithMissingRequiredFieldsReturnsNull() {
        val result = AiParsedResult(
            departure = "",
            arrival = "",
            time = ""
        )

        val ride = result.toRideRequest("sender")
        assertNull(ride)
    }

    @Test
    fun testToRideRequestWithCancellation() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            isCancellation = true
        )

        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(RideStatus.CANCELLED, ride?.status)
    }

    @Test
    fun testToRideRequestWithConfirmation() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            isConfirmation = true
        )

        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(RideStatus.CONFIRMED, ride?.status)
    }

    @Test
    fun testToRideRequestWithPriceFromSettings() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            price = 0.0
        )

        val settings = AppSettings(
            pricePerKm = "2.00",
            basePrice = "10.00"
        )

        val ride = result.toRideRequest("sender", 1000L, settings)
        assertNotNull(ride)
        assertTrue(ride?.price ?: 0.0 > 0)
    }

    @Test
    fun testToRideRequestWithFullName() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            clientName = "Dupont"
        )

        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals("Dupont", ride?.clientName)
    }

    @Test
    fun testToRideRequestWithFirstAndLastName() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Lyon",
            time = "14h30",
            clientName = "Dupont",
            clientFirstName = "Jean"
        )

        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals("Jean Dupont", ride?.clientName)
    }

    @Test
    fun testToRideRequestDistanceParisToParis() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Gare de Lyon",
            time = "14h"
        )
        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(10.0, ride?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun testToRideRequestDistanceParisToOutside() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "Marseille",
            time = "14h"
        )
        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(25.0, ride?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun testToRideRequestDistanceOutsideToParis() {
        val result = AiParsedResult(
            departure = "CDG",
            arrival = "Versailles",
            time = "14h"
        )
        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(25.0, ride?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun testToRideRequestDistanceNonParis() {
        val result = AiParsedResult(
            departure = "Marseille",
            arrival = "Nice",
            time = "14h"
        )
        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertEquals(25.0, ride?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun testToRideRequestDurationCalculated() {
        val result = AiParsedResult(
            departure = "Marseille",
            arrival = "Nice",
            time = "14h"
        )
        val ride = result.toRideRequest("sender")
        assertNotNull(ride)
        assertTrue(ride?.durationMinutes ?: 0 > 0)
        assertEquals(30, ride?.durationMinutes)
    }

    @Test
    fun testMissingFieldsFromConstructor() {
        val result = AiParsedResult(
            departure = "Paris",
            arrival = "",
            time = "14h",
            missingFields = listOf("la destination")
        )

        assertTrue(result.missingFields.contains("la destination"))
        assertEquals(1, result.missingFields.size)
    }
}
