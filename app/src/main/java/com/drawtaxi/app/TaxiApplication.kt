package com.drawtaxi.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager

import android.app.NotificationChannel
import android.app.NotificationManager
import com.drawtaxi.app.logic.SmsForegroundService
import com.drawtaxi.app.logic.SmsProcessor
import com.drawtaxi.app.logic.SmsScanWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaxiApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val settingsManager by lazy { SettingsManager(this) }
    val repository by lazy {
        TaxiRepository(
            database.rideDao(),
            database.quoteDao(),
            database.absenceDao(),
            settingsManager
        )
    }

    companion object {
        private const val TAG = "TaxiApp"
        var isSmsServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        createNotificationChannel()
        
        startSmsServiceIfEnabled()
        schedulePeriodicSmsScan()
    }

    private fun schedulePeriodicSmsScan() {
        try {
            val workManager = WorkManager.getInstance(this)
            workManager.cancelUniqueWork(SmsScanWorker.WORK_NAME)

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val settings = repository.settings.first()
                    if (!settings.monitorSms) {
                        Log.d(TAG, "Scan périodique SMS désactivé (monitorSms = false)")
                        return@launch
                    }

                    val intervalMinutes = settings.smsScanIntervalMinutes.coerceIn(15, 1440)
                    Log.d(TAG, "Planification scan SMS périodique: toutes les $intervalMinutes minutes")

                    val workRequest = PeriodicWorkRequestBuilder<SmsScanWorker>(
                        intervalMinutes.toLong(), TimeUnit.MINUTES
                    )
                        .addTag("sms_scan")
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        SmsScanWorker.WORK_NAME,
                        androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur planification scan SMS: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur configuration scan périodique: ${e.message}")
        }
    }

    fun reschedulePeriodicScan() {
        schedulePeriodicSmsScan()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alerte Courses"
            val descriptionText = "Notifications pour les nouvelles courses reçues"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ride_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startSmsServiceIfEnabled() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.settings.first()
                if (!settings.monitorSms) {
                    Log.d(TAG, "Surveillance SMS désactivée dans les paramètres")
                    stopSmsService()
                    return@launch
                }

                if (ContextCompat.checkSelfPermission(this@TaxiApplication, Manifest.permission.READ_SMS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission READ_SMS non accordée")
                    stopSmsService()
                    return@launch
                }

                if (isSmsServiceRunning) {
                    Log.d(TAG, "SmsForegroundService déjà actif")
                    return@launch
                }

                startSmsForegroundService()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur démarrage service: ${e.message}")
            }
        }
    }

    private fun startSmsForegroundService() {
        Log.d(TAG, "Démarrage du SmsForegroundService")
        
        val serviceIntent = Intent(this, SmsForegroundService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopSmsService() {
        try {
            val serviceIntent = Intent(this, SmsForegroundService::class.java)
            stopService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur arrêt service: ${e.message}")
        }
        isSmsServiceRunning = false
        Log.d(TAG, "SmsForegroundService arrêté")
    }

    fun onNewSmsReceived(address: String, body: String, timestamp: Long) {
        Log.d(TAG, "SMS reçu de $address: ${body.take(50)}...")
        
        val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        appScope.launch {
            try {
                val result = SmsProcessor.processSms(
                    context = this@TaxiApplication,
                    repository = repository,
                    address = address,
                    body = body,
                    timestamp = timestamp
                )

                when (result.action) {
                    SmsProcessor.Action.NEW_RIDE -> {
                        com.drawtaxi.app.logic.NotificationHelper.showNewRideNotification(
                            this@TaxiApplication,
                            result.ride?.id ?: "",
                            result.ride?.arrival ?: "",
                            result.ride?.time ?: ""
                        )
                    }
                    SmsProcessor.Action.RIDE_UPDATED -> {
                        result.notificationTitle?.let { title ->
                            com.drawtaxi.app.logic.NotificationHelper.showInfoNotification(
                                this@TaxiApplication,
                                title,
                                result.notificationBody ?: "",
                                result.ride?.id
                            )
                        }
                    }
                    SmsProcessor.Action.RIDE_DELETED -> {
                        result.notificationTitle?.let { title ->
                            com.drawtaxi.app.logic.NotificationHelper.showInfoNotification(
                                this@TaxiApplication,
                                title,
                                result.notificationBody ?: "",
                                null
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement SMS: ${e.message}")
            }
        }
    }
}
