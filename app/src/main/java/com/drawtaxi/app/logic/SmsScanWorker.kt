package com.drawtaxi.app.logic

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository

class SmsScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsScanWorker"
        const val WORK_NAME = "periodic_sms_scan"

        fun getIntervalMinutes(context: Context): Int {
            val settingsManager = SettingsManager(context)
            var interval = 60
            kotlinx.coroutines.runBlocking {
                settingsManager.settingsFlow.collect { settings ->
                    interval = settings.smsScanIntervalMinutes
                    throw kotlinx.coroutines.CancellationException("Got value")
                }
            }
            return interval
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Periodic SMS scan started")

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
