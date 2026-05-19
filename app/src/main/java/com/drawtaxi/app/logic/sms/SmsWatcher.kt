package com.drawtaxi.app.logic.sms

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.drawtaxi.app.TaxiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class SmsWatcher(
    private val contentResolver: ContentResolver,
    private val onNewSms: (address: String, body: String, timestamp: Long) -> Unit
) : android.database.ContentObserver(Handler(Looper.getMainLooper())) {

    companion object {
        private const val TAG = "SmsWatcher"
        private const val DEBOUNCE_DELAY_MS = 2000L
        private const val MAX_PROCESSED_CACHE_SIZE = 100
        private const val DEDUP_WINDOW_MS = 30000L
    }

    private var debounceJob: Job? = null
    private val watcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processedSmsCache = ConcurrentHashMap<String, Long>()
    private var lastCheckedId: Long = -1

    init {
        Log.d(TAG, "SmsWatcher initialisé avec debounce et déduplication")
    }

    override fun onChange(selfChange: Boolean) {
        if (selfChange) return
        
        Log.d(TAG, "Changement détecté dans les SMS - debounce activé")
        debounceJob?.cancel()
        debounceJob = watcherScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            checkForNewSms()
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (selfChange) return
        
        Log.d(TAG, "Changement détecté - URI: $uri")
        debounceJob?.cancel()
        debounceJob = watcherScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            checkForNewSms()
        }
    }

    private fun checkForNewSms() {
        watcherScope.launch {
            try {
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT,
                    Telephony.Sms.READ,
                    Telephony.Sms.SEEN
                )
                
                val cursor = contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC LIMIT 20"
                )

                cursor?.use {
                    val now = System.currentTimeMillis()
                    var newSmsCount = 0
                    
                    while (it.moveToNext()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                        val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                        val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        val dateSent = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT))

                        val timestamp = if (dateSent > 0) dateSent else date

                        if (id == lastCheckedId) break

                        val dedupKey = "$address|${body.take(50)}"
                        val lastProcessed = processedSmsCache[dedupKey]
                        
                        if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) {
                            Log.d(TAG, "SMS dupliqué ignoré: $dedupKey")
                            continue
                        }

                        if (address.isNotBlank() && body.isNotBlank() && timestamp > 0) {
                            processedSmsCache[dedupKey] = now
                            if (processedSmsCache.size > MAX_PROCESSED_CACHE_SIZE) {
                                evictOldCacheEntries()
                            }
                            
                            Log.d(TAG, "Nouveau SMS détecté - De: $address, Corps: ${body.take(50)}...")
                            onNewSms(address, body, timestamp)
                            newSmsCount++
                        }
                    }
                    
                    if (it.moveToFirst()) {
                        lastCheckedId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    }
                    
                    if (newSmsCount > 0) {
                        Log.d(TAG, "$newSmsCount nouveau(x) SMS traité(s)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lecture SMS: ${e.message}", e)
            }
        }
    }

    private fun evictOldCacheEntries() {
        val now = System.currentTimeMillis()
        val expiredKeys = processedSmsCache.filterValues { (now - it) > DEDUP_WINDOW_MS }.keys
        processedSmsCache.keys.removeAll(expiredKeys)
        
        if (processedSmsCache.size > MAX_PROCESSED_CACHE_SIZE) {
            val excessCount = processedSmsCache.size - (MAX_PROCESSED_CACHE_SIZE / 2)
            val oldestKeys = processedSmsCache.entries
                .sortedBy { it.value }
                .take(excessCount)
                .map { it.key }
            processedSmsCache.keys.removeAll(oldestKeys)
        }
    }

    fun start() {
        Log.d(TAG, "Démarrage de la surveillance SMS avec ContentObserver")
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            this
        )
        
        checkForNewSms()
    }

    fun stop() {
        Log.d(TAG, "Arrêt de la surveillance SMS")
        debounceJob?.cancel()
        watcherScope.cancel()
        try {
            contentResolver.unregisterContentObserver(this)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur arrêt watcher: ${e.message}")
        }
        processedSmsCache.clear()
    }
}
