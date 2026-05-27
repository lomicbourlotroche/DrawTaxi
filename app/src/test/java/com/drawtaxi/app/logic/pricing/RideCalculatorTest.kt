package com.drawtaxi.app.logic.pricing

import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RideCalculatorTest {

    @Test
    fun testCalculatePriceSimple() {
        val settings = AppSettings(
            basePrice = "10.00",
            pricePerKm = "2.00"
        )
        val price = RideCalculator.calculatePrice(15.0, settings)
        assertEquals(40.00, price, 0.001) // 10.00 + 15.0 * 2.00 = 40.00
    }

    @Test
    fun testCalculateFull() {
        val settings = AppSettings(
            coutParKmDeplacement = 0.20
        )
        // distanceDomicileKm = 10.0, price = 50.0
        val result = RideCalculator.calculateFull(10.0, 50.0, settings)

        assertEquals(50.00, result.price, 0.001)
        assertEquals(2.00, result.coutDeplacement, 0.001) // 10.0 * 0.20 = 2.00
        assertEquals(48.00, result.netProfit, 0.001) // 50.00 - 2.00 = 48.00
        assertEquals(96.0, result.profitabilityPercent, 0.001) // (48.0 / 50.0) * 100 = 96%
    }

    @Test
    fun testCalculatePeriodStats() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L

        val rides = listOf(
            RideRequest(
                id = "1",
                sender = "Client1",
                body = "Course 1",
                distanceKm = 10.0,
                price = 30.0,
                isPending = false,
                timestamp = oneHourAgo,
                status = RideStatus.COMPLETED
            ),
            RideRequest(
                id = "2",
                sender = "Client2",
                body = "Course 2",
                distanceKm = 20.0,
                price = 50.0,
                isPending = false,
                timestamp = oneHourAgo,
                status = RideStatus.COMPLETED
            ),
            RideRequest(
                id = "3",
                sender = "Client3",
                body = "Course 3 (pending)",
                distanceKm = 30.0,
                price = 80.0,
                isPending = true, // Should be ignored in stats
                timestamp = oneHourAgo,
                status = RideStatus.DRAFT
            )
        )

        // Test stats for TODAY period
        val stats = RideCalculator.calculatePeriodStats(rides, DashboardPeriod.TODAY)

        assertEquals(2, stats.totalRides)
        assertEquals(80.0, stats.totalRevenue, 0.001) // 30.0 + 50.0 = 80.0
        assertEquals(30.0, stats.totalKm, 0.001) // 10.0 + 20.0 = 30.0
        assertEquals(40.0, stats.avgPerRide, 0.001) // 80.0 / 2 = 40.0
        assertEquals(2.666, stats.avgPerKm, 0.01) // 80.0 / 30.0 = 2.67
    }

    @Test
    fun testCalculateDailyBreakdown() {
        val now = System.currentTimeMillis()
        val rides = listOf(
            RideRequest(
                id = "1",
                sender = "Client1",
                body = "Course 1",
                distanceKm = 10.0,
                price = 30.0,
                date = "26/05/2026",
                isPending = false,
                timestamp = now
            ),
            RideRequest(
                id = "2",
                sender = "Client2",
                body = "Course 2",
                distanceKm = 20.0,
                price = 50.0,
                date = "26/05/2026",
                isPending = false,
                timestamp = now
            ),
            RideRequest(
                id = "3",
                sender = "Client3",
                body = "Course 3",
                distanceKm = 15.0,
                price = 45.0,
                date = "25/05/2026",
                isPending = false,
                timestamp = now - 86400_000L
            )
        )

        val breakdowns = RideCalculator.calculateDailyBreakdown(rides)

        assertEquals(2, breakdowns.size)

        val firstDay = breakdowns.find { it.date == "26/05/2026" }
        assertNotNull(firstDay)
        assertEquals(2, firstDay?.rideCount)
        assertEquals(80.0, firstDay?.totalRevenue ?: 0.0, 0.001)
        assertEquals(30.0, firstDay?.totalKm ?: 0.0, 0.001)

        val secondDay = breakdowns.find { it.date == "25/05/2026" }
        assertNotNull(secondDay)
        assertEquals(1, secondDay?.rideCount)
        assertEquals(45.0, secondDay?.totalRevenue ?: 0.0, 0.001)
        assertEquals(15.0, secondDay?.totalKm ?: 0.0, 0.001)
    }
}
