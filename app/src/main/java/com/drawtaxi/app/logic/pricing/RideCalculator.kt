package com.drawtaxi.app.logic.pricing

import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import java.util.Calendar

data class RideCalcResult(
    val price: Double,
    val coutDeplacement: Double,
    val netProfit: Double,
    val profitabilityPercent: Double,
    val revenuePerKm: Double,
    val revenuePerHour: Double
)

data class PeriodStats(
    val rides: List<RideRequest>,
    val totalRevenue: Double,
    val totalRides: Int,
    val totalKm: Double,
    val totalCoutDeplacement: Double,
    val totalNetProfit: Double,
    val avgProfitability: Double,
    val avgPerRide: Double,
    val avgPerKm: Double
)

object RideCalculator {

    fun calculatePrice(distanceKm: Double, settings: AppSettings): Double {
        val basePrice = settings.basePrice.toDoubleOrNull() ?: 2.60
        val perKm = settings.pricePerKm.toDoubleOrNull() ?: 1.20
        return basePrice + (distanceKm * perKm)
    }

    fun calculateFull(
        distanceDomicileKm: Double,
        price: Double,
        settings: AppSettings
    ): RideCalcResult {
        val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
        val netProfit = price - coutDeplacement
        val profitability = RideRequest.calculateProfitability(price, coutDeplacement)
        val revenuePerKm = if (distanceDomicileKm > 0) price / distanceDomicileKm else 0.0

        return RideCalcResult(
            price = price,
            coutDeplacement = coutDeplacement,
            netProfit = netProfit,
            profitabilityPercent = profitability,
            revenuePerKm = revenuePerKm,
            revenuePerHour = 0.0
        )
    }

    fun calculateForRide(ride: RideRequest, settings: AppSettings): RideCalcResult {
        val distanceDomicileKm = (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() && it > 0 }
            ?: ride.distanceKm * 0.3
        return calculateFull(
            distanceDomicileKm = distanceDomicileKm,
            price = ride.price,
            settings = settings
        )
    }

    fun calculatePeriodStats(
        rides: List<RideRequest>,
        period: DashboardPeriod,
        coutParKmDeplacement: Double = 0.10
    ): PeriodStats {
        val filtered = filterByPeriod(rides, period)
        val totalRevenue = filtered.sumOf { it.price }
        val totalRides = filtered.size
        val totalKm = filtered.sumOf { it.distanceKm }
        val totalCoutDeplacement = filtered.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * coutParKmDeplacement) }
        val totalNetProfit = totalRevenue - totalCoutDeplacement
        val avgProfitability = if (totalRevenue > 0) (totalNetProfit / totalRevenue) * 100.0 else 0.0
        val avgPerRide = if (totalRides > 0) totalRevenue / totalRides else 0.0
        val avgPerKm = if (totalKm > 0) totalRevenue / totalKm else 0.0

        return PeriodStats(
            rides = filtered,
            totalRevenue = totalRevenue,
            totalRides = totalRides,
            totalKm = totalKm,
            totalCoutDeplacement = totalCoutDeplacement,
            totalNetProfit = totalNetProfit,
            avgProfitability = avgProfitability,
            avgPerRide = avgPerRide,
            avgPerKm = avgPerKm
        )
    }

    fun filterByPeriod(rides: List<RideRequest>, period: DashboardPeriod): List<RideRequest> {
        val now = Calendar.getInstance()
        val calendar = now.clone() as Calendar

        val startOfPeriod = when (period) {
            DashboardPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DashboardPeriod.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DashboardPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DashboardPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DashboardPeriod.ALL -> 0L
        }

        return rides.filter { !it.isPending && it.timestamp >= startOfPeriod }
    }

    fun calculateDailyBreakdown(rides: List<RideRequest>, coutParKmDeplacement: Double = 0.10): List<DailyBreakdown> {
        return rides
            .groupBy { it.date.ifBlank { "Sans date" } }
            .map { (date, dayRides) ->
                val dayRevenue = dayRides.sumOf { it.price }
                val dayCoutDeplacement = dayRides.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * coutParKmDeplacement) }
                val dayNet = dayRevenue - dayCoutDeplacement
                val dayProfit = if (dayRevenue > 0) (dayNet / dayRevenue) * 100.0 else 0.0

                DailyBreakdown(
                    date = date,
                    rideCount = dayRides.size,
                    totalRevenue = dayRevenue,
                    totalKm = dayRides.sumOf { it.distanceKm },
                    netProfit = dayNet,
                    profitability = dayProfit
                )
            }
            .sortedByDescending { it.date }
    }
}

enum class DashboardPeriod(val label: String) {
    TODAY("Jour"),
    WEEK("Semaine"),
    MONTH("Mois"),
    YEAR("Année"),
    ALL("Tout")
}

data class DailyBreakdown(
    val date: String,
    val rideCount: Int,
    val totalRevenue: Double,
    val totalKm: Double,
    val netProfit: Double,
    val profitability: Double
)
