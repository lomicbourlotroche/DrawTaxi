package com.drawtaxi.app.logic.pricing

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PriceEngineTest {

    @Test
    fun testBaseFareAndMinDistance() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12) // Midday (no night surcharge)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY) // Weekday (no Sunday surcharge)
            set(Calendar.DAY_OF_YEAR, 100) // Non-holiday
        }

        // Test with distance less than minimum distance (3.6 km)
        val result = PriceEngine.calculate(
            distanceKm = 2.0,
            dateTime = calendar,
            baseFare = 9.0,
            minDistanceKm = 3.6
        )

        assertEquals(9.00, result.basePrice, 0.001)
        assertEquals(0.00, result.distancePrice, 0.001)
        assertEquals(0.00, result.nightSurcharge, 0.001)
        assertEquals(0.00, result.sundaySurcharge, 0.001)
        assertEquals(9.90, result.totalTTC, 0.001) // 9.0 * 1.10 (TVA 10%)
    }

    @Test
    fun testDistanceSurcharge() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.DAY_OF_YEAR, 100)
        }

        // Test with distance more than minimum distance (10 km > 3.6 km)
        val result = PriceEngine.calculate(
            distanceKm = 10.0,
            dateTime = calendar,
            baseFare = 9.0,
            minDistanceKm = 3.6,
            pricePerKm = 2.50
        )

        assertEquals(9.00, result.basePrice, 0.001)
        assertEquals(16.00, result.distancePrice, 0.001) // (10.0 - 3.6) * 2.50 = 16.00
        assertEquals(25.00, result.subtotalHT, 0.001) // 9.0 + 16.0 = 25.0
        assertEquals(2.50, result.tvaAmount, 0.001) // 25.0 * 0.10
        assertEquals(27.50, result.totalTTC, 0.001)
    }

    @Test
    fun testNightSurcharge() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22) // Night (>= 20)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.DAY_OF_YEAR, 100)
        }

        val result = PriceEngine.calculate(
            distanceKm = 10.0,
            dateTime = calendar,
            baseFare = 9.0,
            minDistanceKm = 3.6,
            pricePerKm = 2.50,
            nightSurchargePercent = 0.15
        )

        assertTrue(result.isNight)
        assertFalse(result.isSunday)
        assertEquals(3.75, result.nightSurcharge, 0.001) // (9.0 + 16.0) * 0.15 = 3.75
        assertEquals(28.75, result.subtotalHT, 0.001) // 25.0 + 3.75 = 28.75
    }

    @Test
    fun testSundaySurcharge() {
        // May 24, 2026 is a Sunday
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 24)
            set(Calendar.HOUR_OF_DAY, 12)
        }

        val result = PriceEngine.calculate(
            distanceKm = 10.0,
            dateTime = calendar,
            baseFare = 9.0,
            minDistanceKm = 3.6,
            pricePerKm = 2.50,
            sundaySurchargePercent = 0.10
        )

        assertFalse(result.isNight)
        assertTrue(result.isSunday)
        assertEquals(2.50, result.sundaySurcharge, 0.001) // 25.0 * 0.10 = 2.50
        assertEquals(27.50, result.subtotalHT, 0.001) // 25.0 + 2.50 = 27.50
    }

    @Test
    fun testWaitTimeCalculation() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.DAY_OF_YEAR, 100)
        }

        val result = PriceEngine.calculate(
            distanceKm = 2.0,
            waitMinutes = 10,
            dateTime = calendar,
            baseFare = 9.0,
            euroPerMinute = 1.50
        )

        assertEquals(15.00, result.waitTimePrice, 0.001) // 10 min * 1.50 = 15.00
        assertEquals(24.00, result.subtotalHT, 0.001) // 9.0 base + 15.0 wait = 24.0
    }

    @Test
    fun testFormatQuoteMessage() {
        // May 24, 2026 is a Sunday
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 24)
            set(Calendar.HOUR_OF_DAY, 12)
        }

        val breakdown = PriceEngine.calculate(
            distanceKm = 10.0,
            dateTime = calendar,
            baseFare = 9.0,
            minDistanceKm = 3.6,
            pricePerKm = 2.50,
            sundaySurchargePercent = 0.10
        )

        val message = PriceEngine.formatQuoteMessage(
            price = breakdown,
            departure = "Paris",
            arrival = "Lyon",
            pricePerKm = 2.50,
            baseFare = 9.0
        )

        assertTrue(message.contains("Trajet : Paris → Lyon"))
        // estimatedDistance = (basePrice + distancePrice - baseFare) / pricePerKm
        //                   = (9.0 + 16.0 - 9.0) / 2.50 = 6.4 km
        // Note: locale-dependent decimal separator (dot or comma)
        assertTrue(message.contains("Distance : 6.4 km") || message.contains("Distance : 6,4 km"))
        assertTrue(message.contains("Majoration dimanche incluse"))
        assertTrue(message.contains("Total TTC : 30,25 €") || message.contains("Total TTC : 30.25 €"))
    }
}
