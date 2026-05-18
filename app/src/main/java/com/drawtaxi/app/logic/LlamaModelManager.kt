package com.drawtaxi.app.logic

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

object LlamaModelManager {

    private const val TAG = "LlamaModelManager"
    private const val MODEL_FILENAME = "llama-3.2-3b-instruct-q4_k_m.gguf"
    private const val MODEL_URL = "https://huggingface.co/hugging-quants/Llama-3.2-3B-Instruct-Q4_K_M-GGUF/resolve/main/llama-3.2-3b-instruct-q4_k_m.gguf?download=true"
    private const val EXPECTED_SIZE_BYTES = 2_000_000_000L // ~2GB
    private const val MIN_VALID_SIZE = 1_800_000_000L // 1.8GB minimum
    private const val CHUNK_SIZE = 8192 // 8KB chunks

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        READY,
        ERROR,
        UNLOADED,
        PAUSED
    }

    private var _status = ModelStatus.NOT_DOWNLOADED
    private var _downloadProgress = 0f
    private var _lastUsedTime = 0L
    private var _downloadId: Long = -1
    private val AUTO_UNLOAD_DELAY_MS = 10 * 60 * 1000L

    // OkHttp client avec timeout augmenté
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    val status: ModelStatus get() = _status
    val downloadProgress: Float get() = _downloadProgress

    fun getModelFile(context: Context): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelAvailable(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > MIN_VALID_SIZE
    }

    /**
     * Télécharge le modèle avec gestion robuste des erreurs
     * Utilise OkHttp pour un téléchargement fiable des gros fichiers
     */
    suspend fun downloadModel(
        context: Context,
        maxRetries: Int = 5,
        onProgress: (Float) -> Unit = {},
        onRetry: (attempt: Int, error: String) -> Unit = { _, _ -> },
        onStatusChange: (ModelStatus) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isModelAvailable(context)) {
            _status = ModelStatus.READY
            onStatusChange(ModelStatus.READY)
            return@withContext true
        }

        // Vérifier la connexion
        if (!isNetworkAvailable(context)) {
            _status = ModelStatus.ERROR
            onStatusChange(ModelStatus.ERROR)
            Log.e(TAG, "Pas de connexion Internet")
            return@withContext false
        }

        // Vérifier l'espace disque (besoin de 3GB minimum)
        val freeSpace = context.filesDir.freeSpace
        if (freeSpace < 3_000_000_000L) {
            _status = ModelStatus.ERROR
            onStatusChange(ModelStatus.ERROR)
            Log.e(TAG, "Espace disque insuffisant: ${freeSpace / 1_000_000} MB")
            return@withContext false
        }

        _status = ModelStatus.DOWNLOADING
        onStatusChange(ModelStatus.DOWNLOADING)
        val outputFile = getModelFile(context)

        // Essayer d'abord avec DownloadManager (plus fiable pour gros fichiers)
        if (tryDownloadManager(context, outputFile, onProgress, onStatusChange)) {
            return@withContext true
        }

        // Fallback avec OkHttp
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "Tentative $attempt/$maxRetries avec OkHttp")
                
                val success = downloadWithOkHttp(outputFile, onProgress)
                
                if (success) {
                    _status = ModelStatus.READY
                    onStatusChange(ModelStatus.READY)
                    Log.i(TAG, "✅ Modèle téléchargé: ${outputFile.length() / 1_000_000} MB")
                    return@withContext true
                }

            } catch (e: Exception) {
                lastException = e
                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Délai dépassé - connexion trop lente"
                    e.message?.contains("connect", ignoreCase = true) == true ->
                        "Erreur de connexion"
                    e.message?.contains("SSL", ignoreCase = true) == true ->
                        "Erreur SSL"
                    e.message?.contains("space", ignoreCase = true) == true ->
                        "Espace disque insuffisant"
                    e.message?.contains("interrupted", ignoreCase = true) == true ->
                        "Téléchargement interrompu"
                    else -> e.message ?: "Erreur inconnue"
                }

                Log.w(TAG, "❌ Tentative $attempt échouée: $errorMsg")
                onRetry(attempt, errorMsg)

                if (attempt < maxRetries) {
                    val delayMs = (attempt * 3000L).coerceAtMost(15000L)
                    Log.d(TAG, "⏳ Attente ${delayMs}ms avant retry...")
                    delay(delayMs)
                }
            }
        }

        // Toutes les tentatives ont échoué
        Log.e(TAG, "❌ Échec définitif après $maxRetries tentatives", lastException)
        _status = ModelStatus.ERROR
        onStatusChange(ModelStatus.ERROR)
        outputFile.delete()
        false
    }

    /**
     * Essaie de télécharger avec DownloadManager (plus stable pour gros fichiers)
     */
    private suspend fun tryDownloadManager(
        context: Context,
        outputFile: File,
        onProgress: (Float) -> Unit,
        onStatusChange: (ModelStatus) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Tentative avec DownloadManager...")
            
            val request = DownloadManager.Request(Uri.parse(MODEL_URL)).apply {
                setTitle("Téléchargement IA DrawTaxi")
                setDescription("Llama 3.2 3B (2GB)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(outputFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            _downloadId = downloadManager.enqueue(request)

            // Attendre et surveiller la progression
            var downloading = true
            var lastProgress = 0f
            
            while (downloading) {
                delay(1000) // Vérifier chaque seconde
                
                val query = DownloadManager.Query().setFilterById(_downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                _status = ModelStatus.READY
                                onStatusChange(ModelStatus.READY)
                                onProgress(1f)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                Log.e(TAG, "DownloadManager failed: $reason")
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (total > 0) {
                                    val progress = downloaded.toFloat() / total
                                    if (progress - lastProgress > 0.01f) {
                                        lastProgress = progress
                                        _downloadProgress = progress
                                        onProgress(progress)
                                    }
                                }
                                Unit
                            }
                            else -> { /* Ignorer les autres statuts */ }
                        }
                    } else {
                        downloading = false
                    }
                }
            }

            // Vérifier si le fichier est valide
            if (outputFile.exists() && outputFile.length() > MIN_VALID_SIZE) {
                Log.i(TAG, "✅ Téléchargement DownloadManager réussi")
                true
            } else {
                Log.w(TAG, "❌ DownloadManager: fichier invalide")
                false
            }

        } catch (e: Exception) {
            Log.w(TAG, "DownloadManager a échoué: ${e.message}")
            false
        }
    }

    /**
     * Téléchargement avec OkHttp et support resume
     */
    private suspend fun downloadWithOkHttp(
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val existingSize = if (outputFile.exists()) outputFile.length() else 0L
        
        val requestBuilder = Request.Builder()
            .url(MODEL_URL)
            .header("User-Agent", "DrawTaxi/1.0 (Android)")
        
        // Reprendre depuis où on s'est arrêté
        if (existingSize > 0) {
            requestBuilder.header("Range", "bytes=$existingSize-")
            Log.d(TAG, "📥 Reprise depuis ${existingSize / 1_000_000} MB")
        }

        val request = requestBuilder.build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Réponse vide")
            
            // Calculer la taille totale
            val contentLength = body.contentLength()
            val totalSize = if (response.code == 206 && existingSize > 0) {
                existingSize + contentLength
            } else {
                if (contentLength > 0) contentLength else EXPECTED_SIZE_BYTES
            }

            Log.d(TAG, "📦 Taille totale: ${totalSize / 1_000_000} MB")

            // Mode append si on reprend
            val appendMode = response.code == 206
            
            RandomAccessFile(outputFile, "rw").use { raf ->
                if (appendMode) {
                    raf.seek(existingSize)
                } else {
                    raf.setLength(0) // Nouveau fichier
                }

                body.byteStream().use { input ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int
                    var totalRead = existingSize
                    var lastProgressUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        // Mettre à jour la progression toutes les 500ms
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 500) {
                            _downloadProgress = (totalRead.toFloat() / totalSize).coerceAtMost(1f)
                            onProgress(_downloadProgress)
                            lastProgressUpdate = now
                        }
                    }
                }
            }
        }

        // Vérification finale
        val finalSize = outputFile.length()
        Log.d(TAG, "✅ Téléchargé: ${finalSize / 1_000_000} MB")
        
        finalSize > MIN_VALID_SIZE
    }

    /**
     * Vérifie si une connexion Internet est disponible
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return false

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Retourne le type de connexion
     */
    fun getConnectionType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return "Inconnu"

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "Aucune"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Inconnu"

            when {
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Autre"
            }
        } else {
            @Suppress("DEPRECATION")
            when (connectivityManager.activeNetworkInfo?.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> "WiFi"
                android.net.ConnectivityManager.TYPE_MOBILE -> "Mobile"
                android.net.ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Inconnu"
            }
        }
    }

    /**
     * Estime le temps restant
     */
    fun estimateTimeRemaining(context: Context, currentProgress: Float): String {
        if (currentProgress <= 0.01f) return "Calcul..."
        
        val downloaded = getDownloadedSize(context)
        val total = EXPECTED_SIZE_BYTES
        val remaining = total - downloaded
        
        if (remaining <= 0) return "Bientôt terminé"
        
        // Estimation basée sur la vitesse moyenne
        val estimatedSeconds = (remaining.toFloat() / (downloaded.toFloat() / 30)).toInt()
        
        return when {
            estimatedSeconds < 60 -> "~${estimatedSeconds}s"
            estimatedSeconds < 3600 -> "~${estimatedSeconds / 60}min"
            else -> "~${estimatedSeconds / 3600}h ${(estimatedSeconds % 3600) / 60}min"
        }
    }

    fun markUsed() {
        _lastUsedTime = System.currentTimeMillis()
    }

    fun shouldAutoUnload(): Boolean {
        if (_status != ModelStatus.READY) return false
        if (_lastUsedTime == 0L) return false
        return System.currentTimeMillis() - _lastUsedTime > AUTO_UNLOAD_DELAY_MS
    }

    fun unload() {
        _status = ModelStatus.UNLOADED
        Log.i(TAG, "Modèle déchargé (inactivité)")
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        return if (file.exists()) {
            file.delete()
            _status = ModelStatus.NOT_DOWNLOADED
            true
        } else false
    }

    fun getDownloadedSize(context: Context): Long {
        val file = getModelFile(context)
        return if (file.exists()) file.length() else 0L
    }

    fun getModelSizeFormatted(context: Context): String {
        val size = getDownloadedSize(context)
        return when {
            size == 0L -> "Non téléchargé"
            size < 1_000_000 -> "${size / 1024} Ko"
            size < 1_000_000_000 -> "${size / 1_000_000} Mo"
            else -> String.format("%.2f Go", size / 1_000_000_000.0)
        }
    }

    fun getDownloadId(): Long = _downloadId
}
