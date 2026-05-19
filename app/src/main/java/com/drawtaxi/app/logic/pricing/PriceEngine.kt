package com.drawtaxi.app.logic.pricing

import java.util.Calendar

data class PriceBreakdown(
    val basePrice: Double,
    val distancePrice: Double,
    val nightSurcharge: Double,
    val sundaySurcharge: Double,
    val holidaySurcharge: Double,
    val waitTimePrice: Double,
    val subtotalHT: Double,
    val tvaTransport: Double,
    val tvaWaitTime: Double,
    val totalTTC: Double,
    val isNight: Boolean,
    val isSunday: Boolean,
    val isHoliday: Boolean,
    val waitMinutes: Int
) {
    val totalSurcharges: Double
        get() = nightSurcharge + sundaySurcharge + holidaySurcharge

    val totalTVA: Double
        get() = tvaTransport + tvaWaitTime

    val summary: String
        get() = buildString {
            appendLine("Base: ${String.format("%.2f €", basePrice)}")
            appendLine("Distance: ${String.format("%.2f €", distancePrice)}")
            if (isNight) appendLine("Majoration nuit: ${String.format("%.2f €", nightSurcharge)}")
            if (isSunday) appendLine("Majoration dimanche: ${String.format("%.2f €", sundaySurcharge)}")
            if (isHoliday) appendLine("Majoration férié: ${String.format("%.2f €", holidaySurcharge)}")
            if (waitMinutes > 0) appendLine("Attente ($waitMinutes min): ${String.format("%.2f €", waitTimePrice)}")
            appendLine("Sous-total HT: ${String.format("%.2f €", subtotalHT)}")
            appendLine("TVA transport (10%): ${String.format("%.2f €", tvaTransport)}")
            if (waitMinutes > 0) appendLine("TVA attente (20%): ${String.format("%.2f €", tvaWaitTime)}")
            appendLine("TOTAL TTC: ${String.format("%.2f €", totalTTC)}")
        }
}

object PriceEngine {

    private val frenchHolidays = intArrayOf(
        101, 501, 805, 1111, 2512
    )

    fun calculate(
        distanceKm: Double,
        waitMinutes: Int = 0,
        dateTime: Calendar = Calendar.getInstance(),
        pricePerKm: Double = 1.20,
        baseFare: Double = 2.60,
        nightSurchargePercent: Double = 0.15,
        sundaySurchargePercent: Double = 0.10,
        holidaySurchargePercent: Double = 0.15,
        euroPerMinute: Double = 1.00,
        nightStartHour: Int = 20,
        nightEndHour: Int = 7,
        tvaTransportRate: Double = 0.10,
        tvaWaitTimeRate: Double = 0.20
    ): PriceBreakdown {
        val hour = dateTime.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = dateTime.get(Calendar.DAY_OF_WEEK)
        val dayOfYear = dateTime.get(Calendar.DAY_OF_YEAR)
        val year = dateTime.get(Calendar.YEAR)

        val isNight = isNightTime(hour, nightStartHour, nightEndHour)
        val isSunday = dayOfWeek == Calendar.SUNDAY
        val isHoliday = isFrenchHoliday(dayOfYear, year)

        val base = baseFare
        val distanceCost = distanceKm * pricePerKm
        val transportHT = base + distanceCost

        var nightCost = 0.0
        var sundayCost = 0.0
        var holidayCost = 0.0

        if (isNight) {
            nightCost = transportHT * nightSurchargePercent
        }
        if (isSunday) {
            sundayCost = transportHT * sundaySurchargePercent
        }
        if (isHoliday) {
            holidayCost = transportHT * holidaySurchargePercent
        }

        val waitCost = waitMinutes * euroPerMinute

        val subtotalHT = transportHT + nightCost + sundayCost + holidayCost + waitCost

        val tvaTransport = (transportHT + nightCost + sundayCost + holidayCost) * tvaTransportRate
        val tvaWait = waitCost * tvaWaitTimeRate

        val totalTTC = subtotalHT + tvaTransport + tvaWait

        return PriceBreakdown(
            basePrice = base,
            distancePrice = distanceCost,
            nightSurcharge = nightCost,
            sundaySurcharge = sundayCost,
            holidaySurcharge = holidayCost,
            waitTimePrice = waitCost,
            subtotalHT = subtotalHT,
            tvaTransport = tvaTransport,
            tvaWaitTime = tvaWait,
            totalTTC = totalTTC,
            isNight = isNight,
            isSunday = isSunday,
            isHoliday = isHoliday,
            waitMinutes = waitMinutes
        )
    }

    private fun isNightTime(hour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }
    }

    private fun isFrenchHoliday(dayOfYear: Int, year: Int): Boolean {
        if (frenchHolidays.contains(dayOfYear)) return true

        val easter = calculateEasterSunday(year)
        val easterDayOfYear = easter.get(Calendar.DAY_OF_YEAR)

        val easterMonday = easterDayOfYear + 1
        val ascension = easterDayOfYear + 39
        val pentecostMonday = easterDayOfYear + 50

        return dayOfYear == easterMonday ||
               dayOfYear == ascension ||
               dayOfYear == pentecostMonday
    }

    private fun calculateEasterSunday(year: Int): Calendar {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
    }

    fun formatQuoteMessage(
        price: PriceBreakdown,
        departure: String,
        arrival: String,
        pricePerKm: Double = 1.20,
        baseFare: Double = 2.60
    ): String {
        val estimatedDistance = if (pricePerKm > 0) {
            (price.basePrice + price.distancePrice - baseFare) / pricePerKm
        } else {
            0.0
        }
        return buildString {
            appendLine("Bonjour, voici le devis pour votre course :")
            appendLine("")
            appendLine("Trajet : $departure → $arrival")
            appendLine("Distance : ${String.format("%.1f km", estimatedDistance)}")
            if (price.isNight) appendLine("⏰ Majoration nuit incluse")
            if (price.isSunday) appendLine("📅 Majoration dimanche incluse")
            if (price.isHoliday) appendLine("🎉 Majoration jour férié incluse")
            if (price.waitMinutes > 0) appendLine("⏱️ Temps d'attente : ${price.waitMinutes} min")
            appendLine("")
            appendLine("Total TTC : ${String.format("%.2f €", price.totalTTC)}")
            appendLine("")
            appendLine("Merci de confirmer votre acceptation en répondant OUI.")
        }
    }
}
