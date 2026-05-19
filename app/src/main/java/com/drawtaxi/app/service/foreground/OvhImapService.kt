package com.drawtaxi.app.service.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.drawtaxi.app.MainActivity
import com.drawtaxi.app.R
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.AiSmsParser
import kotlinx.coroutines.*
import java.util.Properties
import javax.mail.*
import javax.mail.Flags
import javax.mail.internet.*
import javax.mail.search.FlagTerm

class OvhImapService : Service() {

    companion object {
        private const val TAG = "OvhImapService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "ovh_imap_channel"
        
        @Volatile
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private lateinit var repository: TaxiRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service créé")
        
        val database = AppDatabase.getDatabase(this)
        val settingsManager = SettingsManager(this)
        repository = TaxiRepository(
            database.rideDao(), 
            database.quoteDao(), 
            database.absenceDao(), 
            settingsManager
        )
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service démarré")
        isRunning = true
        
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

        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isMonitoring = false
        serviceScope.cancel()
        Log.d(TAG, "Service détruit")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Surveillance Email OVH",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Surveillance des emails OVH pour détecter les demandes de course"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DrawTaxi - Surveillance Email OVH")
            .setContentText("En attente de nouveaux emails...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    checkEmails()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la vérification des emails", e)
                }
                
                // Attendre l'intervalle configuré
                val settings = repository.getSettingsSync()
                val intervalMs = (settings.ovhImapCheckInterval * 60 * 1000L).coerceAtLeast(60000L)
                delay(intervalMs)
            }
        }
    }

    private suspend fun checkEmails() {
        val settings = repository.getSettingsSync()
        
        if (!settings.ovhImapEnabled) {
            Log.d(TAG, "Surveillance IMAP désactivée")
            return
        }

        if (settings.ovhSmtpUsername.isBlank() || settings.ovhSmtpPassword.isBlank()) {
            Log.w(TAG, "Identifiants OVH non configurés")
            return
        }

        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", settings.ovhImapServer)
                put("mail.imaps.port", settings.ovhImapPort.toString())
                put("mail.imaps.connectiontimeout", "10000")
                put("mail.imaps.timeout", "10000")
            }

            val session = Session.getInstance(props)
            val store = session.getStore("imaps")
            
            store.connect(
                settings.ovhImapServer,
                settings.ovhImapPort,
                settings.ovhSmtpUsername,
                settings.ovhSmtpPassword
            )

            val inbox = store.getFolder(settings.ovhImapFolder)
            inbox.open(Folder.READ_WRITE)

            // Chercher les messages non lus
            val unseenMessages = inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            
            Log.d(TAG, "${unseenMessages.size} nouveaux messages trouvés")

            unseenMessages.forEach { message ->
                try {
                    processEmail(message)
                    // Marquer comme lu
                    message.setFlag(Flags.Flag.SEEN, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur traitement message", e)
                }
            }

            inbox.close(false)
            store.close()

            // Mettre à jour la notification
            updateNotification("Dernière vérification: ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date())}")

        } catch (e: AuthenticationFailedException) {
            Log.e(TAG, "Échec authentification IMAP", e)
            updateNotification("Erreur: Identifiants OVH incorrects")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur connexion IMAP", e)
            updateNotification("Erreur de connexion")
        }
    }

    private suspend fun processEmail(message: Message) {
        val from = message.from?.firstOrNull()?.toString() ?: return
        val subject = message.subject ?: ""
        val body = extractEmailBody(message)
        
        Log.d(TAG, "Traitement email de: $from - Sujet: $subject")

        // Utiliser l'IA pour parser l'email
        val settings = repository.getSettingsSync()
        val parsedResult = AiSmsParser.parseEmail(this, body, settings.aiEnabled)
        
        // Vérifier si c'est une demande de course
        if (parsedResult.departure.isNotBlank() || parsedResult.arrival.isNotBlank()) {
            val ride = parsedResult.toRideRequest(from, System.currentTimeMillis(), settings)
            
            // Sauvegarder la course si elle n'est pas null
            ride?.let { rideRequest ->
                repository.saveRide(rideRequest)
                
                Log.d(TAG, "Nouvelle course créée depuis email: ${rideRequest.id}")
                
                // Notification
                NotificationHelper.showNewRideNotification(
                    this,
                    rideRequest.id,
                    rideRequest.arrival,
                    rideRequest.time
                )
            }
        }
    }

    private fun extractEmailBody(message: Message): String {
        return try {
            when (val content = message.content) {
                is String -> content
                is Multipart -> {
                    val sb = StringBuilder()
                    for (i in 0 until content.count) {
                        val part = content.getBodyPart(i)
                        if (part.contentType?.startsWith("text/plain") == true) {
                            sb.append(part.content.toString())
                        }
                    }
                    sb.toString()
                }
                else -> content.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur extraction corps email", e)
            ""
        }
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DrawTaxi - Surveillance Email OVH")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }
}
