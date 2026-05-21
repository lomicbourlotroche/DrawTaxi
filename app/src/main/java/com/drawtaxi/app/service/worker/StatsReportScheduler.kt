package com.drawtaxi.app.service.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.StatsReport
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.messaging.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsReportScheduler {

    companion object {
        private const val TAG = "StatsReportScheduler"
        const val ACTION_WEEKLY_REPORT = "com.drawtaxi.app.ACTION_WEEKLY_REPORT"
        const val ACTION_MONTHLY_REPORT = "com.drawtaxi.app.ACTION_MONTHLY_REPORT"

        fun scheduleWeeklyReport(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = getNextSundayAt2359()

            val weeklyIntent = Intent(context, StatsReportReceiver::class.java).apply {
                action = ACTION_WEEKLY_REPORT
            }
            val weeklyPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                weeklyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                weeklyPendingIntent
            )

            Log.d(TAG, "Rapport hebdomadaire programmé pour: ${calendar.time}")
        }

        fun scheduleMonthlyReport(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = getLastDayOfMonthAt2359()

            val monthlyIntent = Intent(context, StatsReportReceiver::class.java).apply {
                action = ACTION_MONTHLY_REPORT
            }
            val monthlyPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                monthlyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                monthlyPendingIntent
            )

            Log.d(TAG, "Rapport mensuel programmé pour: ${calendar.time}")
        }

        private fun getNextSundayAt2359(): Calendar {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            return calendar
        }

        private fun getLastDayOfMonthAt2359(): Calendar {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            return calendar
        }

        fun generateWeeklyStats(rides: List<RideRequest>): StatsReport? {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = calendar.timeInMillis

            val weekRides = rides.filter { it.timestamp >= weekAgo && !it.isPending }
            if (weekRides.isEmpty()) return null

            return StatsReport(
                id = "weekly_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
                type = "weekly",
                startDate = weekAgo,
                endDate = now,
                totalRides = weekRides.size,
                totalRevenue = weekRides.sumOf { it.price },
                totalKm = weekRides.sumOf { it.distanceKm },
                averagePrice = weekRides.map { it.price }.average(),
                averageDistance = weekRides.map { it.distanceKm }.average()
            )
        }

        fun generateMonthlyStats(rides: List<RideRequest>): StatsReport? {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            val monthAgo = calendar.timeInMillis

            val monthRides = rides.filter { it.timestamp >= monthAgo && !it.isPending }
            if (monthRides.isEmpty()) return null

            return StatsReport(
                id = "monthly_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
                type = "monthly",
                startDate = monthAgo,
                endDate = now,
                totalRides = monthRides.size,
                totalRevenue = monthRides.sumOf { it.price },
                totalKm = monthRides.sumOf { it.distanceKm },
                averagePrice = monthRides.map { it.price }.average(),
                averageDistance = monthRides.map { it.distanceKm }.average()
            )
        }

        fun formatReportMessage(report: StatsReport): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return buildString {
                appendLine("=== Rapport ${if (report.type == "weekly") "hebdomadaire" else "mensuel"} ===")
                appendLine("Du ${dateFormat.format(Date(report.startDate))} au ${dateFormat.format(Date(report.endDate))}")
                appendLine()
                appendLine("Courses: ${report.totalRides}")
                appendLine("Revenus: ${String.format("%.2f €", report.totalRevenue)}")
                appendLine("Distance: ${String.format("%.1f km", report.totalKm)}")
                appendLine()
                appendLine("Moyennes:")
                appendLine("  Panier moyen: ${String.format("%.2f €", report.averagePrice)}")
                appendLine("  Distance moyenne: ${String.format("%.1f km", report.averageDistance)}")
            }
        }

        fun formatReportAsCsv(report: StatsReport): String {
            return buildString {
                appendLine("Type,Date début,Date fin,Courses,Revenus (€),Distance (km),Panier moyen (€),Distance moyenne (km)")
                appendLine("${report.type},${report.startDate},${report.endDate},${report.totalRides},${String.format("%.2f", report.totalRevenue)},${String.format("%.1f", report.totalKm)},${String.format("%.2f", report.averagePrice)},${String.format("%.1f", report.averageDistance)}")
            }
        }
    }
}

class StatsReportReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StatsReportReceiver", "Rapport reçu: ${intent.action}")

        when (intent.action) {
            StatsReportScheduler.ACTION_WEEKLY_REPORT -> {
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    generateAndNotify(context, "weekly")
                }
            }
            StatsReportScheduler.ACTION_MONTHLY_REPORT -> {
                val calendar = Calendar.getInstance()
                val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (calendar.get(Calendar.DAY_OF_MONTH) == lastDayOfMonth) {
                    generateAndNotify(context, "monthly")
                }
            }
        }
    }

    private fun generateAndNotify(context: Context, type: String) {
        val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        receiverScope.launch {
            try {
                val database = com.drawtaxi.app.data.local.AppDatabase.getDatabase(context)
                val settingsManager = com.drawtaxi.app.data.local.SettingsManager(context)
                val repository = com.drawtaxi.app.data.TaxiRepository(
                    database.rideDao(),
                    database.quoteDao(),
                    database.absenceDao(),
                    settingsManager
                )

                val allRides = repository.getAllRides()
                val report = when (type) {
                    "weekly" -> StatsReportScheduler.generateWeeklyStats(allRides)
                    "monthly" -> StatsReportScheduler.generateMonthlyStats(allRides)
                    else -> null
                }

                report?.let {
                    val message = StatsReportScheduler.formatReportMessage(it)
                    NotificationHelper.showInfoNotification(
                        context,
                        "Rapport ${if (type == "weekly") "hebdomadaire" else "mensuel"}",
                        message,
                        null
                    )
                    Log.d("StatsReportReceiver", "Rapport $type généré: ${it.totalRides} courses, ${it.totalRevenue} €")
                }

                when (type) {
                    "weekly" -> StatsReportScheduler.scheduleWeeklyReport(context)
                    "monthly" -> StatsReportScheduler.scheduleMonthlyReport(context)
                }
            } catch (e: Exception) {
                Log.e("StatsReportReceiver", "Erreur génération rapport: ${e.message}")
            }
        }
    }
}
