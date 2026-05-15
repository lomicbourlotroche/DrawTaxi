package com.drawtaxi.app.logic

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
import com.drawtaxi.app.TaxiApplication
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SmsForegroundService : Service() {

    companion object {
        private const val TAG = "SmsForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val POLLING_INTERVAL = 10000L
        private const val SMS_PROCESS_DELAY_MS = 1000L
        private const val DEDUP_WINDOW_MS = 60000L
        private const val MAX_PROCESSED_CACHE = 200

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
        Log.d(TAG, "Tâche retirée - redémarrage du service")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_SMS)
            .setContentTitle("DrawTaxi - Surveillance active")
            .setContentText("Dernière vérification: $currentTime")
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(true)
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

    private fun checkForNewSms() {
        serviceScope.launch {
            try {
                if (ContextCompat.checkSelfPermission(this@SmsForegroundService, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission READ_SMS non accordée pour le check")
                    return@launch
                }

                val uri = Uri.parse("content://sms/inbox")
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

                        Log.d(TAG, "SMS trouvé (ID: $id) - De: $address - ${body.take(30)}")

                        if (address.isNotBlank() && body.isNotBlank()) {
                            processedSmsCache[dedupKey] = now
                            if (processedSmsCache.size > MAX_PROCESSED_CACHE) {
                                val oldestKeys = processedSmsCache.entries
                                    .sortedBy { it.value }
                                    .take(processedSmsCache.size - MAX_PROCESSED_CACHE / 2)
                                oldestKeys.forEach { processedSmsCache.remove(it.key) }
                            }
                            
                            processSms(address, body, timestamp)
                        }
                    }
                    if (count > 0) {
                        Log.d(TAG, "$count SMS trouvé(s), ${processedSmsCache.size} en cache")
                        updateNotification()
                    }
                }

                lastCheckedTimestamp = System.currentTimeMillis()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException lecture SMS: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lecture SMS: ${e.message}", e)
            }
        }
    }

    private fun updateNotification() {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_SMS)
                .setContentTitle("DrawTaxi - Surveillance active")
                .setContentText("Dernière vérification: $currentTime")
                .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(true)
                .build()
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur mise à jour notification: ${e.message}")
        }
    }

    suspend fun processSms(address: String, body: String, timestamp: Long) {
        Log.d(TAG, "Traitement SMS de $address: ${body.take(50)}")

        try {
            val settings = repository.getSettingsSync()
            if (!settings.monitorSms) {
                Log.d(TAG, "Surveillance SMS désactivée, SMS ignoré")
                return
            }

            val isTaxi = AiSmsParser.isTaxiRelated(this, body, settings.aiEnabled)
            if (!isTaxi) {
                Log.d(TAG, "SMS non lié au taxi (IA), ignoré")
                return
            }

            val parsedDetails = parseSmsAdvanced(address, body, timestamp)
            
            if (parsedDetails.isCancellation) {
                handleCancellationSms(address, body)
                return
            }

            val pendingList = repository.getPendingRidesList()
            val matchInfo = RideMatcher.matchSmsToRides(address, body, pendingList)

            Log.d(TAG, "Analyse SMS: ${matchInfo.result} - ${matchInfo.reason}")

            when (matchInfo.result) {
                RideMatchResult.DELETION, RideMatchResult.MODIFICATION -> {
                    matchInfo.matchedRide?.let { ride ->
                        val parsedRide = parseSms(address, body, timestamp)
                        if (matchInfo.result == RideMatchResult.DELETION || parsedDetails.isCancellation) {
                            repository.deleteRide(ride)
                            Log.d(TAG, "Course supprimée: ${ride.id}")
                            NotificationHelper.showInfoNotification(
                                this@SmsForegroundService,
                                "Course annulée",
                                "Course supprimée: ${ride.departure} → ${ride.arrival}",
                                null
                            )
                        } else if (parsedRide != null) {
                            val updatedRide = ride.copy(
                                departure = if (parsedRide.departure.isNotBlank()) parsedRide.departure else ride.departure,
                                arrival = if (parsedRide.arrival.isNotBlank()) parsedRide.arrival else ride.arrival,
                                time = if (parsedRide.time.isNotBlank()) parsedRide.time else ride.time,
                                body = ride.body + "\n--- MODIFICATION ---\n" + body
                            )
                            repository.updateRide(updatedRide)
                            Log.d(TAG, "Course modifiée: ${updatedRide.id}")
                            NotificationHelper.showInfoNotification(
                                this@SmsForegroundService,
                                "Course modifiée",
                                "${updatedRide.departure} → ${updatedRide.arrival}",
                                ride.id
                            )
                        }
                    }
                }

                RideMatchResult.ADDITION -> {
                    val parsedRide = parseSms(address, body, timestamp)
                    if (parsedRide != null) {
                        repository.saveRide(parsedRide)
                        Log.d(TAG, "Nouvelle course ajoutée: ${parsedRide.id}")
                        NotificationHelper.showNewRideNotification(
                            this@SmsForegroundService,
                            parsedRide.id,
                            parsedRide.arrival,
                            parsedRide.time
                        )
                    }
                }

                RideMatchResult.CLARIFICATION -> {
                    matchInfo.matchedRide?.let { ride ->
                        val parsedRide = parseSms(address, body, timestamp)
                        if (parsedRide != null && (parsedRide.departure.isNotBlank() ||
                            parsedRide.arrival.isNotBlank() || parsedRide.time.isNotBlank())) {
                            val updatedRide = ride.copy(
                                departure = if (parsedRide.departure.isNotBlank()) parsedRide.departure else ride.departure,
                                arrival = if (parsedRide.arrival.isNotBlank()) parsedRide.arrival else ride.arrival,
                                time = if (parsedRide.time.isNotBlank()) parsedRide.time else ride.time,
                                body = ride.body + "\n--- REPONSE ---\n" + body
                            )
                            repository.updateRide(updatedRide)
                            Log.d(TAG, "Course mise à jour depuis réponse: ${ride.id}")
                        } else {
                            val updatedRide = ride.copy(
                                body = ride.body + "\n--- REPONSE ---\n" + body
                            )
                            repository.updateRide(updatedRide)
                            Log.d(TAG, "Réponse ajoutée à la course: ${ride.id}")
                        }
                    }
                }

                RideMatchResult.DUPLICATE -> {
                    Log.d(TAG, "Doublon détecté, ignoré")
                }

                RideMatchResult.NEW_RIDE -> {
                    if (parsedDetails.isCancellation) {
                        Log.d(TAG, "Message d'annulation sans course correspondante, ignoré")
                        return
                    }
                    val parsedRide = parseSms(address, body, timestamp)
                    if (parsedRide != null) {
                        val missingFields = parsedDetails.missingFields
                        val hasMissingInfo = missingFields.isNotEmpty()
                        val updatedRide = parsedRide.copy(
                            hasMissingInfo = hasMissingInfo,
                            missingFieldsList = missingFields.joinToString(",")
                        )
                        repository.saveRide(updatedRide)
                        Log.d(TAG, "Nouvelle course créée: ${updatedRide.id} (infos manquantes: $hasMissingInfo)")
                        NotificationHelper.showNewRideNotification(
                            this@SmsForegroundService,
                            updatedRide.id,
                            updatedRide.arrival,
                            updatedRide.time
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur traitement SMS: ${e.message}", e)
        }
    }

    private suspend fun handleCancellationSms(address: String, body: String) {
        val pendingList = repository.getPendingRidesList()
        val matchingRide = pendingList
            .filter { it.sender == address }
            .maxByOrNull { it.timestamp }
        
        if (matchingRide != null) {
            repository.deleteRide(matchingRide)
            Log.d(TAG, "Course annulée via SMS: ${matchingRide.id}")
            NotificationHelper.showInfoNotification(
                this@SmsForegroundService,
                "Course annulée",
                "Course supprimée: ${matchingRide.departure} → ${matchingRide.arrival}",
                null
            )
        }
    }

    fun scanNow() {
        Log.d(TAG, "Scan manuel déclenché")
        lastCheckedTimestamp = System.currentTimeMillis() - 3600000
        checkForNewSms()
    }
}
