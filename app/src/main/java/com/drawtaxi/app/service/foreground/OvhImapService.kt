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
import com.drawtaxi.app.data.local.SecureCredentialsManager
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.pricing.QuoteResponseHandler
import com.drawtaxi.app.logic.sms.AiSmsParser
import com.drawtaxi.app.logic.sms.AiParsedResult
import com.drawtaxi.app.logic.sms.RideMatcher
import com.drawtaxi.app.logic.sms.RideMatchResult
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import com.sun.mail.imap.IMAPFolder
import javax.mail.*
import javax.mail.Flags
import javax.mail.event.MessageCountAdapter
import javax.mail.event.MessageCountEvent
import javax.mail.internet.*
import javax.mail.search.FlagTerm

class OvhImapService : Service() {

    companion object {
        private const val TAG = "OvhImapService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "ovh_imap_channel"
        const val ACTION_SCAN_NOW = "com.drawtaxi.app.action.SCAN_NOW"

        // Backoff paramétrage : 1s → 2s → 4s → 8s → 16s → 32s → 64s → plafond 30 min
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30 * 60 * 1000L

        // Timeout OVH côté serveur : l'IDLE est déconnecté après ~29 minutes
        // On fixe notre timeout un peu en dessous pour anticiper la reconnexion
        private const val OVH_IDLE_TIMEOUT_MS = 28 * 60 * 1000L

        // Intervalle de vérification quand le service est désactivé
        private const val DISABLED_CHECK_MS = 60_000L

        @Volatile
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private lateinit var repository: TaxiRepository

    // Références vers la connexion IMAP active.
    // Permettent à onDestroy() d'interrompre immédiatement inbox.idle() en fermant le folder.
    @Volatile
    private var currentStore: Store? = null

    @Volatile
    private var currentInbox: Folder? = null

    private var consecutiveFailures = 0

    // ──────────────────────────────────────────────
    // Cycle de vie du Service
    // ──────────────────────────────────────────────

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
                    // API 34+ : SPECIAL_USE est le type approprié pour une surveillance réseau permanente
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29-33 : DATA_SYNC pour la synchronisation de données
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Impossible de démarrer en foreground: ${e.message}")
        }

        if (intent?.action == ACTION_SCAN_NOW) {
            Log.d(TAG, "Scan manuel demandé")
            serviceScope.launch { scanEmailsOnce() }
        } else {
            startMonitoring()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Arrêt du service en cours...")

        // 1. Désactiver la boucle de surveillance
        isMonitoring = false
        isRunning = false

        // 2. Interrompre immédiatement un éventuel inbox.idle() bloquant
        //    La fermeture du folder provoque une FolderClosedException dans le thread IDLE,
        //    ce qui débloque la boucle d'écoute et permet sa terminaison propre.
        try {
            currentInbox?.close(false)
        } catch (_: Exception) { }
        try {
            currentStore?.close()
        } catch (_: Exception) { }

        // 3. Annuler le scope coroutine (après avoir libéré les ressources réseau)
        serviceScope.cancel()

        super.onDestroy()
        Log.d(TAG, "Service détruit")
    }

    // ──────────────────────────────────────────────
    // Notification
    // ──────────────────────────────────────────────

    private fun createNotificationChannel() {
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

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DrawTaxi - Surveillance Email OVH")
            .setContentText("Surveillance temps réel activée")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DrawTaxi - Surveillance Email OVH")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) { }
    }

    // ──────────────────────────────────────────────
    // Boucle principale IMAP IDLE (temps réel)
    // ──────────────────────────────────────────────

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        serviceScope.launch {
            imapIdleLoop()
        }
    }

    /**
     * Boucle d'écoute IMAP avec IDLE.
     *
     * Remplace l'ancien polling périodique par une connexion persistante :
     * - inbox.idle() bloque le thread jusqu'à réception d'un nouveau message
     *   (push du serveur, temps réel, zéro batterie gaspillée)
     * - En cas de déconnexion (timeout OVH ~29 min, perte réseau, etc.),
     *   la boucle se reconnecte automatiquement avec un backoff exponentiel.
     * - Le cycle de vie est thread-safe : onDestroy() ferme le folder,
     *   ce qui lève une FolderClosedException et débloque idle().
     */
    private suspend fun imapIdleLoop() {
        while (isMonitoring) {
            try {
                val settings = repository.getSettingsSync()

                if (!settings.ovhImapEnabled) {
                    updateNotification("Surveillance désactivée")
                    delay(DISABLED_CHECK_MS)
                    continue
                }

                val credentials = loadCredentials()
                    ?: run {
                        updateNotification("Identifiants OVH non configurés")
                        delay(DISABLED_CHECK_MS)
                        continue
                    }

                // Connexion IMAP persistante et boucle IDLE
                runIdleSession(settings, credentials.first, credentials.second)

                // Sortie propre de la session → réinitialiser le compteur d'échecs
                consecutiveFailures = 0
                updateNotification("Reconnexion programmée...")

            } catch (e: CancellationException) {
                throw e
            } catch (e: FolderClosedException) {
                // Fermeture volontaire (onDestroy) ou perte de connexion → reconnexion
                Log.i(TAG, "Session IMAP fermée, reconnexion...")
                handleReconnection()
            } catch (e: MessagingException) {
                Log.e(TAG, "Erreur protocole IMAP: ${e.message}")
                handleReconnection()
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Timeout socket IMAP")
                handleReconnection()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue: ${e.message}")
                handleReconnection()
            }
        }
    }

    /**
     * Établit une session IMAP persistante et entre dans la boucle d'écoute IDLE.
     *
     * La session reste ouverte tant que isMonitoring est vrai. Les appels
     * bloquants (connect, search, idle, setFlag) sont isolés dans des blocs
     * withContext(Dispatchers.IO) pour ne pas bloquer le thread coroutine.
     */
    private suspend fun runIdleSession(
        settings: com.drawtaxi.app.data.AppSettings,
        username: String,
        password: String
    ) {
        // Propriétés optimisées pour une connexion IMAP IDLE durable
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", settings.ovhImapServer)
            put("mail.imaps.port", settings.ovhImapPort.toString())
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "30000")
            put("mail.imaps.writetimeout", "15000")
            put("mail.imaps.ssl.trust", settings.ovhImapServer)
            // Activer le support IMAP IDLE dans JavaMail
            put("mail.imaps.idle", "true")
            // Limiter le fetch size pour éviter les OOM sur gros messages
            put("mail.imaps.fetchsize", "65536")
        }

        val session = Session.getInstance(props)

        // ── Connexion (bloquant, sur IO) ──
        val store: Store
        val inbox: Folder
        withContext(Dispatchers.IO) {
            val s = session.getStore("imaps")
            s.connect(settings.ovhImapServer, settings.ovhImapPort, username, password)
            store = s
            currentStore = s

            val f = s.getFolder(settings.ovhImapFolder)
            f.open(Folder.READ_WRITE)
            inbox = f
            currentInbox = f
        }

        updateNotification("Connecté - Surveillance temps réel")

        try {
                // ── Enregistrer un listener pour les nouveaux messages ──
            withContext(Dispatchers.IO) {
                inbox.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(e: MessageCountEvent) {
                        // Le simple fait de recevoir cet événement débloque idle()
                        // qui va alors retraiter les messages dans la boucle principale.
                    }
                })
            }

            // ── Boucle d'écoute IDLE ──
            // Le schéma est :
            //   1. Vérifier les messages non lus
            //   2. Attendre les nouveaux messages via IDLE (bloquant)
            //   3. Retour en 1.
            // Note : idle() n'existe que sur IMAPFolder (pas sur Folder)
            val imapFolder = inbox as IMAPFolder
            while (isMonitoring) {
                // Vérifier que le folder est toujours ouvert avant d'interagir
                if (!isFolderOpen(inbox)) break

                // Étape 1 : Traiter les messages non lus déjà présents
                processUnseenMessages(inbox)

                if (!isMonitoring) break

                // Étape 2 : Attendre passivement les nouveaux messages
                // idle() bloque jusqu'à :
                //   - Arrivée d'un nouveau message (messagesAdded event)
                //   - Timeout serveur (~29 min chez OVH)
                //   - Fermeture du folder via onDestroy()
                withContext(Dispatchers.IO) {
                    if (imapFolder.isOpen) {
                        imapFolder.idle()
                    }
                }

                if (!isMonitoring || !isFolderOpen(inbox)) break

                // Étape 3 : Traiter les messages arrivés pendant l'IDLE
                processUnseenMessages(inbox)
            }
        } finally {
            // Nettoyage des références AVANT fermeture pour éviter les races conditions
            currentInbox = null
            currentStore = null

            withContext(Dispatchers.IO) {
                closeQuietly(inbox)
                closeQuietly(store)
            }
        }
    }

    // ──────────────────────────────────────────────
    // Traitement des messages
    // ──────────────────────────────────────────────

    /**
     * Cherche et traite les messages non lus dans le dossier IMAP.
     * Marque chaque message comme lu après traitement réussi.
     */
    private suspend fun processUnseenMessages(inbox: Folder) {
        if (!isFolderOpen(inbox)) return

        val unseen = withContext(Dispatchers.IO) {
            try {
                inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            } catch (e: FolderClosedException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur recherche messages non lus", e)
                emptyArray<Message>()
            }
        }

        if (unseen.isEmpty()) return

        Log.d(TAG, "${unseen.size} nouveau(x) message(s) détecté(s)")

        var processedCount = 0
        var errorCount = 0
        for (msg in unseen) {
            try {
                processEmail(msg)
                processedCount++
            } catch (e: Exception) {
                errorCount++
                Log.e(TAG, "Erreur traitement message #${msg.messageNumber}: ${e.message}")
            }
            try {
                withContext(Dispatchers.IO) {
                    msg.setFlag(Flags.Flag.SEEN, true)
                }
            } catch (_: Exception) { }
        }
        if (errorCount > 0) {
            Log.w(TAG, "processUnseenMessages: $processedCount ok, $errorCount erreurs")
        }

        updateNotification("Dernière activité: ${currentTimeString()}")
    }

    /**
     * Vérifie si l'expéditeur est autorisé selon la whitelist configurée.
     * Si ovhAllowedSenders est vide, tous les expéditeurs sont acceptés.
     */
    private suspend fun isSenderAllowed(from: String): Boolean {
        val settings = repository.getSettingsSync()
        val allowed = settings.ovhAllowedSenders
        if (allowed.isBlank()) return true
        val senders = allowed.split(",").map { it.trim().lowercase() }
        val fromLower = from.lowercase()
        return senders.any { sender ->
            fromLower.contains(sender) || sender.contains(fromLower)
        }
    }

    /**
     * Traite un email avec le pipeline complet :
     *   1. Filtrage par expéditeur
     *   2. Parsing AI
     *   3. Détection réponse devis
     *   4. Détection annulation
     *   5. Matching avec courses existantes (modification, clarification, doublon, nouvelle course)
     *
     * Aligné sur le fonctionnement de SmsProcessor.processSms().
     */
    private suspend fun processEmail(message: Message) {
        val from = message.from?.firstOrNull()?.toString() ?: return
        val subject = message.subject ?: ""
        val body = extractEmailBody(message)

        Log.d(TAG, "Traitement email de: $from - Sujet: $subject")

        // 1. Filtrage par expéditeur
        if (!isSenderAllowed(from)) {
            Log.d(TAG, "Expéditeur non autorisé, ignoré: $from")
            return
        }

        val settings = repository.getSettingsSync()
        val aiResult = AiSmsParser.parseEmail(this, body, settings.aiEnabled)
        val timestamp = System.currentTimeMillis()

        // 2. Réponse à un devis (confirmation/refus/modification)
        val isQuoteResponse = QuoteResponseHandler.handleResponse(this, repository, from, body, aiResult)
        if (isQuoteResponse) {
            Log.d(TAG, "Réponse à un devis traitée depuis email: $from")
            return
        }

        // 3. Annulation
        if (aiResult.isCancellation) {
            val pendingList = repository.getPendingRidesList()
            val matchingRide = pendingList
                .filter { it.sender == from }
                .maxByOrNull { it.timestamp }
            if (matchingRide != null) {
                repository.deleteRide(matchingRide)
                Log.d(TAG, "Course annulée depuis email: ${matchingRide.id}")
                NotificationHelper.showInfoNotification(
                    this@OvhImapService,
                    "Course annulée",
                    "Course supprimée: ${matchingRide.departure} → ${matchingRide.arrival}"
                )
            } else {
                Log.d(TAG, "Annulation sans course correspondante, ignoré")
            }
            return
        }

        // 4. Matching avec courses existantes
        val pendingRides = repository.getPendingRidesList()
        val matchInfo = RideMatcher.matchSmsToRides(from, body, pendingRides)

        Log.d(TAG, "Analyse email: ${matchInfo.result} - ${matchInfo.reason}")

        when (matchInfo.result) {
            RideMatchResult.DELETION -> {
                matchInfo.matchedRide?.let { ride ->
                    repository.deleteRide(ride)
                    Log.d(TAG, "Course supprimée depuis email: ${ride.id}")
                }
            }
            RideMatchResult.MODIFICATION -> {
                val matched = matchInfo.matchedRide
                if (matched != null) {
                    val updated = matched.copy(
                        departure = aiResult.departure.takeIf { it.isNotBlank() } ?: matched.departure,
                        arrival = aiResult.arrival.takeIf { it.isNotBlank() } ?: matched.arrival,
                        time = aiResult.time.takeIf { it.isNotBlank() } ?: matched.time,
                        body = matched.body + "\n--- MODIFICATION EMAIL ---\n" + body
                    )
                    repository.updateRide(updated)
                    Log.d(TAG, "Course modifiée depuis email: ${updated.id}")
                    NotificationHelper.showInfoNotification(
                        this@OvhImapService,
                        "Course modifiée",
                        "${updated.departure} → ${updated.arrival}"
                    )
                }
            }
            RideMatchResult.ADDITION, RideMatchResult.NEW_RIDE -> {
                val ride = aiResult.toRideRequest(
                    sender = from,
                    timestamp = timestamp,
                    settings = settings,
                    extraBody = body,
                    messageChannel = com.drawtaxi.app.data.MessageChannel.EMAIL
                )
                if (ride != null) {
                    val hasMissingInfo = aiResult.missingFields.isNotEmpty()
                    val saved = ride.copy(
                        hasMissingInfo = hasMissingInfo,
                        missingFieldsList = aiResult.missingFields.joinToString(",")
                    )
                    repository.saveRide(saved)
                    Log.d(TAG, "Nouvelle course créée depuis email: ${saved.id}")
                    NotificationHelper.showNewRideNotification(
                        this,
                        saved.id,
                        saved.arrival,
                        saved.time
                    )
                }
            }
            RideMatchResult.CLARIFICATION -> {
                val matched = matchInfo.matchedRide
                if (matched != null) {
                    val hasAiInfo = aiResult.departure.isNotBlank() || aiResult.arrival.isNotBlank() || aiResult.time.isNotBlank()
                    val updated = if (hasAiInfo) {
                        matched.copy(
                            departure = aiResult.departure.takeIf { it.isNotBlank() } ?: matched.departure,
                            arrival = aiResult.arrival.takeIf { it.isNotBlank() } ?: matched.arrival,
                            time = aiResult.time.takeIf { it.isNotBlank() } ?: matched.time,
                            body = matched.body + "\n--- REPONSE EMAIL ---\n" + body
                        )
                    } else {
                        matched.copy(body = matched.body + "\n--- REPONSE EMAIL ---\n" + body)
                    }
                    repository.updateRide(updated)
                    Log.d(TAG, "Réponse ajoutée à la course: ${matched.id}")
                }
            }
            RideMatchResult.DUPLICATE -> {
                Log.d(TAG, "Doublon email ignoré de: $from")
            }
        }
    }

    /**
     * Scan unique des emails (hors boucle IDLE). Utilisé par ACTION_SCAN_NOW.
     */
    private suspend fun scanEmailsOnce() {
        try {
            val settings = repository.getSettingsSync()
            val credentials = loadCredentials() ?: run {
                updateNotification("Scan : identifiants manquants")
                return
            }
            val (username, password) = credentials

            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", settings.ovhImapServer)
                put("mail.imaps.port", settings.ovhImapPort.toString())
                put("mail.imaps.ssl.trust", settings.ovhImapServer)
                put("mail.imaps.connectiontimeout", "15000")
                put("mail.imaps.timeout", "30000")
            }

            val store = withContext(Dispatchers.IO) {
                val s = Session.getInstance(props).getStore("imaps")
                s.connect(settings.ovhImapServer, settings.ovhImapPort, username, password)
                s
            }

            var processedCount = 0
            var errorCount = 0
            try {
                val inbox = withContext(Dispatchers.IO) {
                    val f = store.getFolder(settings.ovhImapFolder)
                    f.open(Folder.READ_WRITE)
                    f
                }
                try {
                    val allMessages = withContext(Dispatchers.IO) {
                        (inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), true)) +
                         inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false)))
                            .distinctBy { it.messageNumber }
                    }
                    Log.d(TAG, "Scan manuel : ${allMessages.size} message(s) trouvé(s)")
                    for (msg in allMessages) {
                        try {
                            processEmail(msg)
                            processedCount++
                        } catch (e: Exception) {
                            errorCount++
                            Log.e(TAG, "Erreur traitement message #${msg.messageNumber}: ${e.message}")
                        }
                        try {
                            withContext(Dispatchers.IO) { msg.setFlag(Flags.Flag.SEEN, true) }
                        } catch (_: Exception) { }
                    }
                    updateNotification("Scan terminé : $processedCount traité(s), $errorCount erreur(s)")
                } finally {
                    withContext(Dispatchers.IO) { closeQuietly(inbox) }
                }
            } finally {
                withContext(Dispatchers.IO) { closeQuietly(store) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur scan manuel: ${e.message}")
            updateNotification("Erreur scan: ${e.message?.take(50)}")
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

    // ──────────────────────────────────────────────
    // Gestion de la résilience réseau
    // ──────────────────────────────────────────────

    /**
     * Calcule un backoff exponentiel : 1s → 2s → 4s → 8s → ... → plafond 30 min.
     * La progression est basée sur le nombre d'échecs consécutifs.
     * Le backoff est réinitialisé à 0 après une connexion réussie (dans imapIdleLoop).
     */
    private fun nextBackoffMs(): Long {
        val exponent = (consecutiveFailures - 1).coerceIn(0, 15)
        val backoff = INITIAL_BACKOFF_MS * (1L shl exponent)
        return backoff.coerceAtMost(MAX_BACKOFF_MS)
    }

    private suspend fun handleReconnection() {
        consecutiveFailures++
        val backoff = nextBackoffMs()
        val message = if (consecutiveFailures == 1) {
            "Reconnexion OVH..."
        } else {
            "Reconnexion OVH... (tentative #$consecutiveFailures, prochaine dans ${backoff / 1000}s)"
        }
        Log.w(TAG, message)
        updateNotification(message)
        delay(backoff)
    }

    // ──────────────────────────────────────────────
    // Utilitaires
    // ──────────────────────────────────────────────

    /**
     * Charge les identifiants OVH depuis SecureCredentialsManager avec fallback
     * vers les settings standards.
     */
    private suspend fun loadCredentials(): Pair<String, String>? {
        val settings = repository.getSettingsSync()
        val secureCreds = withContext(Dispatchers.IO) {
            SecureCredentialsManager(this@OvhImapService)
        }
        val username = secureCreds.ovhSmtpUsername.ifBlank { settings.ovhSmtpUsername }
        val password = secureCreds.ovhSmtpPassword.ifBlank { settings.ovhSmtpPassword }
        return if (username.isNotBlank() && password.isNotBlank()) {
            Pair(username, password)
        } else {
            null
        }
    }

    /**
     * Vérifie de manière robuste si le folder IMAP est toujours ouvert.
     * Attrape silencieusement les exceptions de connexion fermée.
     */
    private fun isFolderOpen(folder: Folder?): Boolean {
        return try {
            folder?.isOpen == true
        } catch (_: Exception) {
            false
        }
    }

    private fun closeQuietly(folder: Folder?) {
        try {
            if (folder?.isOpen == true) folder.close(false)
        } catch (_: Exception) { }
    }

    private fun closeQuietly(store: Store?) {
        try {
            if (store?.isConnected == true) store.close()
        } catch (_: Exception) { }
    }

    private fun currentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
