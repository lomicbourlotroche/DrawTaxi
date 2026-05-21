package com.drawtaxi.app.service.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.SmsScanner
import com.drawtaxi.app.service.foreground.SmsForegroundService

import kotlinx.coroutines.flow.first

class SmsScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsScanWorker"
        const val WORK_NAME = "periodic_sms_scan"

        fun getIntervalMinutes(context: Context): Int {
            val settingsManager = SettingsManager(context)
            return kotlinx.coroutines.runBlocking {
                settingsManager.settingsFlow.first().smsScanIntervalMinutes
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Periodic SMS scan started")

            // Skip if foreground service is active (it handles scanning)
            if (SmsForegroundService.isRunning) {
                Log.d(TAG, "SmsForegroundService already running, skipping periodic scan")
                return Result.success()
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val settingsManager = SettingsManager(applicationContext)
            val repository = TaxiRepository(
                database.rideDao(),
                database.quoteDao(),
                database.absenceDao(),
                settingsManager
            )

            val settings = repository.getSettingsSync()
            if (!settings.monitorSms) {
                Log.d(TAG, "SMS monitoring disabled, skipping scan")
                return Result.success()
            }

            val scannedRides = SmsScanner.scanLastHourSms(applicationContext)
            scannedRides.forEach { ride ->
                repository.saveRide(ride)
            }

            Log.d(TAG, "Periodic SMS scan completed: ${scannedRides.size} rides found")

            if (scannedRides.isNotEmpty()) {
                NotificationHelper.showInfoNotification(
                    applicationContext,
                    "Scan SMS périodique",
                    "${scannedRides.size} nouvelle(s) course(s) détectée(s)",
                    null
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic SMS scan failed: ${e.message}", e)
            Result.retry()
        }
    }
}
