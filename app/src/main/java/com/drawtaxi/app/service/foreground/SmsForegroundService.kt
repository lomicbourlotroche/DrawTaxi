package com.drawtaxi.app.service.foreground



import android.Manifest

import android.app.Notification

import android.app.NotificationChannel

import android.app.NotificationManager

import android.app.PendingIntent

import android.app.Service

import android.content.ContentResolver

import android.content.Intent

import android.content.pm.PackageManager

import android.content.pm.ServiceInfo

import android.database.ContentObserver

import android.net.Uri

import android.os.Build

import android.os.Handler

import android.os.IBinder

import android.os.Looper

import android.provider.Telephony

import android.util.Log

import androidx.core.app.NotificationCompat

import androidx.core.app.ServiceCompat

import androidx.core.content.ContextCompat

import com.drawtaxi.app.MainActivity

import com.drawtaxi.app.R

import com.drawtaxi.app.TaxiApplication

import com.drawtaxi.app.data.AppSettings

import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.SmsProcessor

import com.drawtaxi.app.data.RideRequest

import com.drawtaxi.app.data.local.AppDatabase

import com.drawtaxi.app.data.local.SettingsManager

import kotlinx.coroutines.*

import java.text.SimpleDateFormat

import java.util.*

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.asCoroutineDispatcher



class SmsForegroundService : Service() {



    companion object {

        private const val TAG = "SmsForegroundService"

        private const val NOTIFICATION_ID = 1001

        private const val POLLING_INTERVAL = 10000L

        private const val SMS_PROCESS_DELAY_MS = 1000L

        private const val DEDUP_WINDOW_MS = 60000L

        private const val MAX_PROCESSED_CACHE = 200

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        const val ACTION_SCAN_NOW = "ACTION_SCAN_NOW"



        @Volatile

        var isRunning = false

            private set



        private var instance: SmsForegroundService? = null



        fun triggerScan() {

            instance?.scanNow() ?: Log.w(TAG, "Service non actif, scan impossible")

        }

    }



    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastCheckedTimestamp: Long = 0

    private var isScanning = false

    private var smsObserver: ContentObserver? = null

    private val processedSmsCache = ConcurrentHashMap<String, Long>()

    private val debounceHandler = Handler(Looper.getMainLooper())

    private var debounceRunnable: Runnable? = null

    private var totalSmsProcessed = 0



    private lateinit var repository: TaxiRepository



    override fun onCreate() {

        super.onCreate()

        isRunning = true

        instance = this



        val database = AppDatabase.getDatabase(this)

        val settingsManager = SettingsManager(this)

        repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)



        createNotificationChannel()

        lastCheckedTimestamp = System.currentTimeMillis()

        TaxiApplication.isSmsServiceRunning = true

        Log.d(TAG, "Service créé - Timestamp initial: $lastCheckedTimestamp")

    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_STOP_SERVICE -> {

                Log.d(TAG, "Arrêt demandé via notification")

                stopSelf()

                return START_NOT_STICKY

            }

            ACTION_SCAN_NOW -> {

                Log.d(TAG, "Scan manuel demandé via notification")

                scanNow()

                return START_STICKY

            }

        }



        Log.d(TAG, "Service démarré")



        try {

            ServiceCompat.startForeground(

                this,

                NOTIFICATION_ID,

                createNotification(),

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC

                } else {

                    0

                }

            )

        } catch (e: SecurityException) {

            Log.e(TAG, "Impossible de démarrer en foreground: ${e.message}")

        }



        startSmsWatcher()



        return START_STICKY

    }



    override fun onBind(intent: Intent?): IBinder? = null



    override fun onDestroy() {

        super.onDestroy()

        isRunning = false

        instance = null

        TaxiApplication.isSmsServiceRunning = false

        stopSmsWatcher()

        serviceScope.cancel()

        processedSmsCache.clear()

        debounceHandler.removeCallbacksAndMessages(null)

        Log.d(TAG, "Service détruit")

    }



    override fun onTaskRemoved(rootIntent: Intent?) {

        super.onTaskRemoved(rootIntent)

        val restartIntent = Intent(applicationContext, SmsForegroundService::class.java)
        applicationContext.startForegroundService(restartIntent)

        Log.d(TAG, "Tâche retirée - redémarrage du service")

    }



    private fun createNotificationChannel() {

            val channel = NotificationChannel(

                NotificationHelper.CHANNEL_ID_SMS,

                "Surveillance SMS",

                NotificationManager.IMPORTANCE_LOW

            ).apply {

                description = "Surveillance active des messages taxi"

                setShowBadge(false)

                enableLights(false)

                enableVibration(false)

            }



            val notificationManager = getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(channel)
    }



    private fun createNotification(): Notification {

        val pendingIntent = PendingIntent.getActivity(

            this,

            0,

            Intent(this, MainActivity::class.java),

            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        )



        val stopIntent = Intent(this, SmsForegroundService::class.java).apply {

            action = ACTION_STOP_SERVICE

        }

        val stopPendingIntent = PendingIntent.getService(

            this,

            1,

            stopIntent,

            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        )



        val scanIntent = Intent(this, SmsForegroundService::class.java).apply {

            action = ACTION_SCAN_NOW

        }

        val scanPendingIntent = PendingIntent.getService(

            this,

            2,

            scanIntent,

            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        )



        val smsCountText = if (totalSmsProcessed > 0) {

            "$totalSmsProcessed SMS analysé${if (totalSmsProcessed > 1) "s" else ""}"

        } else {

            "En attente de SMS..."

        }



        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_SMS)

            .setContentTitle("DrawTaxi - Surveillance SMS active")

            .setContentText(smsCountText)

            .setSmallIcon(R.drawable.ic_notification)

            .setOngoing(true)

            .setContentIntent(pendingIntent)

            .setPriority(NotificationCompat.PRIORITY_LOW)

            .setCategory(NotificationCompat.CATEGORY_SERVICE)

            .setShowWhen(true)

            .setUsesChronometer(false)

            .addAction(

                android.R.drawable.ic_menu_search,

                "Scanner",

                scanPendingIntent

            )

            .addAction(

                android.R.drawable.ic_menu_close_clear_cancel,

                "Arrêter",

                stopPendingIntent

            )

            .setStyle(

                NotificationCompat.BigTextStyle()

                    .bigText("DrawTaxi surveille vos SMS entrants pour détecter automatiquement les réservations de courses.\n\n$smsCountText")

            )

            .build()

    }



    private fun startSmsWatcher() {

        if (isScanning) return

        isScanning = true



        Log.d(TAG, "Démarrage de la surveillance SMS améliorée")



        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)

            == PackageManager.PERMISSION_GRANTED) {



            val handler = Handler(Looper.getMainLooper())

            smsObserver = object : ContentObserver(handler) {

                override fun onChange(selfChange: Boolean, uri: Uri?) {

                    super.onChange(selfChange, uri)

                    Log.d(TAG, "ContentObserver déclenché - URI: $uri")

                    if (!selfChange) {

                        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }

                        debounceRunnable = Runnable {

                            checkForNewSms()

                        }

                        debounceHandler.postDelayed(debounceRunnable!!, SMS_PROCESS_DELAY_MS)

                    }

                }

            }



            contentResolver.registerContentObserver(

                Uri.parse("content://sms"),

                true,

                smsObserver!!

            )



            Log.d(TAG, "ContentObserver enregistré sur content://sms avec debounce")

        } else {

            Log.w(TAG, "Permission READ_SMS non accordée - ContentObserver non enregistré")

        }



        checkForNewSmsImmediately()



        serviceScope.launch {

            while (isScanning) {

                delay(POLLING_INTERVAL)

                if (isScanning) {

                    checkForNewSms()

                }

            }

        }

    }



    private fun stopSmsWatcher() {

        isScanning = false

        smsObserver?.let { observer ->

            try {

                contentResolver.unregisterContentObserver(observer)

            } catch (e: Exception) {

                Log.e(TAG, "Erreur arrêt observer: ${e.message}")

            }

        }

        smsObserver = null

        debounceHandler.removeCallbacksAndMessages(null)

        Log.d(TAG, "Surveillance SMS arrêtée")

    }



    private fun checkForNewSmsImmediately() {

        lastCheckedTimestamp = System.currentTimeMillis() - 120000

        checkForNewSms()

    }



    private val smsProcessingDispatcher = java.util.concurrent.Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()



    private fun checkForNewSms() {
        serviceScope.launch {
            try {
                if (ContextCompat.checkSelfPermission(this@SmsForegroundService, Manifest.permission.READ_SMS)

                    != PackageManager.PERMISSION_GRANTED) {

                    Log.w(TAG, "Permission READ_SMS non accordée pour le check")

                    return@launch

                }



                // Vérifier inbox ET sent (pour les tests d'envoi)

                val uris = listOf(

                    Uri.parse("content://sms/inbox"),

                    Uri.parse("content://sms/sent")

                )



                for (uri in uris) {

                    processSmsUri(uri)

                }



                lastCheckedTimestamp = System.currentTimeMillis()

            } catch (e: SecurityException) {

                Log.e(TAG, "SecurityException lecture SMS: ${e.message}")

            } catch (e: Exception) {

                Log.e(TAG, "Erreur lecture SMS: ${e.message}", e)

            }

        }

    }



    private fun processSmsUri(uri: Uri) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.READ
        )

        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf((lastCheckedTimestamp - 5000).toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                var count = 0
                val now = System.currentTimeMillis()

                while (it.moveToNext()) {
                    count++
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    val dateSent = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT))

                    val timestamp = if (dateSent > 0) dateSent else date

                    val dedupKey = "$address|${body.take(50)}|$timestamp"
                    val lastProcessed = processedSmsCache[dedupKey]

                    if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) {
                        Log.d(TAG, "SMS dupliqué ignoré (cache): $dedupKey")
                        continue
                    }

                    Log.d(TAG, "SMS trouvé (ID: $id, uri: $uri) - De: $address - ${body.take(30)}")

                    if (address.isNotBlank() && body.isNotBlank()) {
                        processedSmsCache[dedupKey] = now
                        if (processedSmsCache.size > MAX_PROCESSED_CACHE) {
                            val oldestKeys = processedSmsCache.entries
                                .sortedBy { it.value }
                                .take(processedSmsCache.size - MAX_PROCESSED_CACHE / 2)
                            oldestKeys.forEach { processedSmsCache.remove(it.key) }
                        }

                        serviceScope.launch(smsProcessingDispatcher) {
                            processSmsInternal(address, body, timestamp)
                        }
                        totalSmsProcessed++
                    }
                }

                if (count > 0) {
                    Log.d(TAG, "$count SMS trouvé(s) sur $uri, ${processedSmsCache.size} en cache")
                    updateNotification()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture URI $uri: ${e.message}")
        }
    }



    private fun updateNotification() {

        try {

            val smsCountText = if (totalSmsProcessed > 0) {

                "$totalSmsProcessed SMS analysé${if (totalSmsProcessed > 1) "s" else ""}"

            } else {

                "En attente de SMS..."

            }

            

            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_SMS)

                .setContentTitle("DrawTaxi - Surveillance SMS active")

                .setContentText(smsCountText)

                .setSmallIcon(R.drawable.ic_notification)

                .setOngoing(true)

                .setContentIntent(PendingIntent.getActivity(

                    this, 0, Intent(this, MainActivity::class.java),

                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                ))

                .setPriority(NotificationCompat.PRIORITY_LOW)

                .setCategory(NotificationCompat.CATEGORY_SERVICE)

                .setShowWhen(true)

                .setOnlyAlertOnce(true)

                .build()

            

            val notificationManager = getSystemService(NotificationManager::class.java)

            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {

            Log.e(TAG, "Erreur mise à jour notification: ${e.message}")

        }

    }

    private suspend fun processSmsInternal(address: String, body: String, timestamp: Long) {
        val result = SmsProcessor.processSms(
            context = this@SmsForegroundService,

            repository = repository,

            address = address,

            body = body,

            timestamp = timestamp

        )



        when (result.action) {

            SmsProcessor.Action.NEW_RIDE -> {

                result.ride?.let { ride ->

                    NotificationHelper.showNewRideNotification(

                        this,

                        ride.id,

                        ride.arrival,

                        ride.time

                    )

                }

            }

            SmsProcessor.Action.RIDE_UPDATED, SmsProcessor.Action.RIDE_DELETED -> {

                result.notificationTitle?.let { title ->

                    NotificationHelper.showInfoNotification(

                        this,

                        title,

                        result.notificationBody ?: "",

                        result.ride?.id

                    )

                }

            }

            else -> {}

        }
    }



    fun scanNow() {
        Log.d(TAG, "Scan manuel déclenché")

        lastCheckedTimestamp = System.currentTimeMillis() - 3600000

        checkForNewSms()

    }

}

