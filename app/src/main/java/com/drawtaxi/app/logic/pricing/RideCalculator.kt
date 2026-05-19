package com.drawtaxi.app.logic.pricing

import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import java.util.Calendar

data class RideCalcResult(
    val price: Double,
    val fuelCost: Double,
    val operatingCost: Double,
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
    val totalFuelCost: Double,
    val totalOpCost: Double,
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

    fun calculateFuelCost(distanceKm: Double, settings: AppSettings): Double {
        return distanceKm * settings.fuelCostPerKm
    }

    fun calculateOperatingCost(durationMinutes: Int, settings: AppSettings): Double {
        return (durationMinutes / 60.0) * settings.operatingCostPerHour
    }

    fun calculateProfitability(price: Double, fuelCost: Double, operatingCost: Double): Double {
        val totalCost = fuelCost + operatingCost
        if (totalCost == 0.0 || price == 0.0) return 0.0
        return ((price - totalCost) / price) * 100.0
    }

    fun calculateFull(
        distanceKm: Double,
        durationMinutes: Int,
        price: Double,
        settings: AppSettings
    ): RideCalcResult {
        val fuelCost = calculateFuelCost(distanceKm, settings)
        val operatingCost = calculateOperatingCost(durationMinutes, settings)
        val netProfit = price - fuelCost - operatingCost
        val profitability = calculateProfitability(price, fuelCost, operatingCost)
        val revenuePerKm = if (distanceKm > 0) price / distanceKm else 0.0
        val revenuePerHour = if (durationMinutes > 0) (price / durationMinutes) * 60.0 else 0.0

        return RideCalcResult(
            price = price,
            fuelCost = fuelCost,
            operatingCost = operatingCost,
            netProfit = netProfit,
            profitabilityPercent = profitability,
            revenuePerKm = revenuePerKm,
            revenuePerHour = revenuePerHour
        )
    }

    fun calculateForRide(ride: RideRequest, settings: AppSettings): RideCalcResult {
        return calculateFull(
            distanceKm = ride.distanceKm,
            durationMinutes = ride.durationMinutes,
            price = ride.price,
            settings = settings
        )
    }

    fun calculatePeriodStats(
        rides: List<RideRequest>,
        period: DashboardPeriod
    ): PeriodStats {
        val filtered = filterByPeriod(rides, period)
        val totalRevenue = filtered.sumOf { it.price }
        val totalRides = filtered.size
        val totalKm = filtered.sumOf { it.distanceKm }
        val totalFuelCost = filtered.sumOf { it.fuelCost }
        val totalOpCost = filtered.sumOf { it.operatingCost }
        val totalNetProfit = totalRevenue - totalFuelCost - totalOpCost
        val avgProfitability = if (totalRevenue > 0) (totalNetProfit / totalRevenue) * 100.0 else 0.0
        val avgPerRide = if (totalRides > 0) totalRevenue / totalRides else 0.0
        val avgPerKm = if (totalKm > 0) totalRevenue / totalKm else 0.0

        return PeriodStats(
            rides = filtered,
            totalRevenue = totalRevenue,
            totalRides = totalRides,
            totalKm = totalKm,
            totalFuelCost = totalFuelCost,
            totalOpCost = totalOpCost,
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

    fun calculateDailyBreakdown(rides: List<RideRequest>): List<DailyBreakdown> {
        return rides
            .groupBy { it.date.ifBlank { "Sans date" } }
            .map { (date, dayRides) ->
                val dayRevenue = dayRides.sumOf { it.price }
                val dayFuel = dayRides.sumOf { it.fuelCost }
                val dayOp = dayRides.sumOf { it.operatingCost }
                val dayNet = dayRevenue - dayFuel - dayOp
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
