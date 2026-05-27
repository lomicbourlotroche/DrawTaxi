package com.drawtaxi.app.service.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drawtaxi.app.TaxiApplication
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.SmsScanner
import com.drawtaxi.app.service.foreground.SmsForegroundService
import kotlinx.coroutines.flow.first

/**
 * Worker responsable du scan périodique de la boîte de réception SMS.
 * Sert de sécurité si le [SmsForegroundService] ou le BroadcastReceiver échouent.
 */
class SmsScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsScanWorker"
        const val WORK_NAME = "periodic_sms_scan"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Periodic SMS scan started")

            // On ne scanne pas si le service de premier plan est déjà en train de le faire
            if (SmsForegroundService.isRunning) {
                Log.d(TAG, "SmsForegroundService already running, skipping periodic scan")
                return Result.success()
            }

            // Récupération propre du repository via l'application
            val app = applicationContext as? TaxiApplication
            val repository = app?.repository ?: return Result.failure()

            val settings = repository.settings.first()
            if (!settings.monitorSms) {
                Log.d(TAG, "SMS monitoring disabled, skipping scan")
                return Result.success()
            }

            // Scan des SMS de la dernière heure
            // Note: SmsScanner.scanLastHourSms utilise déjà Dispatchers.IO en interne via le ContentResolver
            val scannedRides = SmsScanner.scanLastHourSms(applicationContext)
            
            var newRidesCount = 0
            scannedRides.forEach { ride ->
                // saveRide retourne true s'il s'agit d'une nouvelle insertion (pas un doublon)
                if (repository.saveRide(ride)) {
                    newRidesCount++
                }
            }

            Log.d(TAG, "Periodic SMS scan completed: $newRidesCount new rides found out of ${scannedRides.size} scanned")

            if (newRidesCount > 0) {
                NotificationHelper.showInfoNotification(
                    applicationContext,
                    "Scan SMS périodique",
                    "$newRidesCount nouvelle(s) course(s) détectée(s)",
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
