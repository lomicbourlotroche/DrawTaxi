package com.drawtaxi.app.logic.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object LlamaModelManager {

    private const val TAG = "LlamaModelManager"
    private const val MODEL_FILENAME = "llama-3.2-3b-instruct-q4_k_m.gguf"
    private const val MODEL_URL = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf?download=true"
    private const val EXPECTED_SIZE_BYTES = 2_000_000_000L
    private const val MIN_VALID_SIZE = 1_800_000_000L
    private const val CHUNK_SIZE = 8192

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        READY,
        ERROR,
        UNLOADED,
        PAUSED
    }

    private val _status = MutableStateFlow(ModelStatus.NOT_DOWNLOADED)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var _downloadStartTime = 0L
    private var _downloadedBytes = 0L
    private val _isCancelled = AtomicBoolean(false)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getModelFile(context: Context): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelAvailable(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() >= MIN_VALID_SIZE
    }

    fun isAiAvailable(context: Context): Boolean {
        return isModelAvailable(context)
    }

    fun cancelDownload() {
        _isCancelled.set(true)
    }

    suspend fun downloadModel(
        context: Context,
        maxRetries: Int = 5,
        onProgress: (Float) -> Unit = {},
        onRetry: (attempt: Int, error: String) -> Unit = { _, _ -> },
        onStatusChange: (ModelStatus) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isModelAvailable(context)) {
            _status.value = ModelStatus.READY
            onStatusChange(ModelStatus.READY)
            return@withContext true
        }

        if (!isNetworkAvailable(context)) {
            _status.value = ModelStatus.ERROR
            onStatusChange(ModelStatus.ERROR)
            Log.e(TAG, "No internet connection")
            return@withContext false
        }

        val freeSpace = context.filesDir.freeSpace
        if (freeSpace < 3_000_000_000L) {
            _status.value = ModelStatus.ERROR
            onStatusChange(ModelStatus.ERROR)
            Log.e(TAG, "Insufficient disk space: ${freeSpace / 1_000_000} MB")
            return@withContext false
        }

        _isCancelled.set(false)
        _downloadStartTime = System.currentTimeMillis()
        _downloadedBytes = 0L
        _status.value = ModelStatus.DOWNLOADING
        onStatusChange(ModelStatus.DOWNLOADING)
        val outputFile = getModelFile(context)

        if (tryDownloadManager(context, outputFile, onProgress, onStatusChange)) {
            return@withContext true
        }

        if (_isCancelled.get()) {
            _status.value = ModelStatus.NOT_DOWNLOADED
            onStatusChange(ModelStatus.NOT_DOWNLOADED)
            return@withContext false
        }

        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            if (_isCancelled.get()) {
                _status.value = ModelStatus.NOT_DOWNLOADED
                onStatusChange(ModelStatus.NOT_DOWNLOADED)
                return@withContext false
            }

            try {
                Log.d(TAG, "Attempt $attempt/$maxRetries with OkHttp")
                val success = downloadWithOkHttp(outputFile, onProgress)

                if (success) {
                    _status.value = ModelStatus.READY
                    onStatusChange(ModelStatus.READY)
                    Log.i(TAG, "Model downloaded: ${outputFile.length() / 1_000_000} MB")
                    return@withContext true
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Timeout - connection too slow"
                    e.message?.contains("connect", ignoreCase = true) == true ->
                        "Connection error"
                    e.message?.contains("SSL", ignoreCase = true) == true ->
                        "SSL error"
                    e.message?.contains("space", ignoreCase = true) == true ->
                        "Insufficient disk space"
                    e.message?.contains("interrupted", ignoreCase = true) == true ->
                        "Download interrupted"
                    else -> e.message ?: "Unknown error"
                }

                Log.w(TAG, "Attempt $attempt failed: $errorMsg")
                onRetry(attempt, errorMsg)

                if (attempt < maxRetries) {
                    val delayMs = (attempt * 3000L).coerceAtMost(15000L)
                    delay(delayMs)
                }
            }
        }

        Log.e(TAG, "All $maxRetries attempts failed", lastException)
        _status.value = ModelStatus.ERROR
        onStatusChange(ModelStatus.ERROR)
        outputFile.delete()
        false
    }

    private suspend fun tryDownloadManager(
        context: Context,
        outputFile: File,
        onProgress: (Float) -> Unit,
        onStatusChange: (ModelStatus) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting DownloadManager...")

            val request = DownloadManager.Request(Uri.parse(MODEL_URL)).apply {
                setTitle("DrawTaxi AI Download")
                setDescription("Llama 3.2 3B (2GB)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(outputFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            var downloading = true
            var lastProgress = 0f

            while (downloading) {
                if (_isCancelled.get()) {
                    downloadManager.remove(downloadId)
                    outputFile.delete()
                    return@withContext false
                }

                delay(1000)

                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                _status.value = ModelStatus.READY
                                onStatusChange(ModelStatus.READY)
                                onProgress(1f)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (total > 0) {
                                    _downloadedBytes = downloaded
                                    val progress = downloaded.toFloat() / total
                                    if (progress - lastProgress > 0.01f) {
                                        lastProgress = progress
                                        _downloadProgress.value = progress
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                    } else {
                        downloading = false
                    }
                }
            }

            outputFile.exists() && outputFile.length() > MIN_VALID_SIZE
        } catch (e: Exception) {
            Log.w(TAG, "DownloadManager failed: ${e.message}")
            false
        }
    }

    private suspend fun downloadWithOkHttp(
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val existingSize = if (outputFile.exists()) outputFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(MODEL_URL)
            .header("User-Agent", "DrawTaxi/1.0 (Android)")

        if (existingSize > 0) {
            requestBuilder.header("Range", "bytes=$existingSize-")
            Log.d(TAG, "Resuming from ${existingSize / 1_000_000} MB")
        }

        val request = requestBuilder.build()
        var lastProgressUpdate = System.currentTimeMillis()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response")

            val contentLength = body.contentLength()
            val totalSize = if (response.code == 206 && existingSize > 0) {
                existingSize + contentLength
            } else {
                if (contentLength > 0) contentLength else EXPECTED_SIZE_BYTES
            }

            Log.d(TAG, "Total size: ${totalSize / 1_000_000} MB")

            val appendMode = response.code == 206

            RandomAccessFile(outputFile, "rw").use { raf ->
                if (appendMode) {
                    raf.seek(existingSize)
                } else {
                    raf.setLength(0)
                }

                body.byteStream().use { input ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int
                    var totalRead = existingSize

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (_isCancelled.get()) {
                            Log.d(TAG, "Download cancelled")
                            return@withContext false
                        }

                        raf.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        _downloadedBytes = totalRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 500) {
                            _downloadProgress.value = (totalRead.toFloat() / totalSize).coerceAtMost(1f)
                            onProgress(_downloadProgress.value)
                            lastProgressUpdate = now
                        }
                    }
                }
            }
        }

        val finalSize = outputFile.length()
        Log.d(TAG, "Downloaded: ${finalSize / 1_000_000} MB")

        finalSize > MIN_VALID_SIZE
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getConnectionType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return "Inconnu"

        val network = connectivityManager.activeNetwork ?: return "Aucune"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Inconnu"

        return when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Autre"
        }
    }

    fun estimateTimeRemaining(@Suppress("UNUSED_PARAMETER") context: Context, currentProgress: Float): String {
        if (currentProgress <= 0.01f) return "Calcul..."

        val elapsedMs = System.currentTimeMillis() - _downloadStartTime
        if (elapsedMs <= 0) return "Calcul..."

        val downloadedBytes = _downloadedBytes
        val totalBytes = EXPECTED_SIZE_BYTES
        val remainingBytes = totalBytes - downloadedBytes

        if (remainingBytes <= 0) return "Bientôt terminé"

        val bytesPerMs = downloadedBytes.toFloat() / elapsedMs
        if (bytesPerMs <= 0) return "Calcul..."

        val estimatedMs = (remainingBytes / bytesPerMs).toLong()
        val estimatedSeconds = (estimatedMs / 1000).toInt()

        return when {
            estimatedSeconds < 60 -> "~${estimatedSeconds}s"
            estimatedSeconds < 3600 -> "~${estimatedSeconds / 60}min"
            else -> "~${estimatedSeconds / 3600}h ${(estimatedSeconds % 3600) / 60}min"
        }
    }

    fun getDownloadSpeed(@Suppress("UNUSED_PARAMETER") context: Context): String {
        val elapsedMs = System.currentTimeMillis() - _downloadStartTime
        if (elapsedMs <= 0) return ""

        val bytesPerSec = (_downloadedBytes * 1000L) / (elapsedMs + 1)
        return when {
            bytesPerSec > 1_000_000 -> String.format("%.1f MB/s", bytesPerSec / 1_000_000.0)
            bytesPerSec > 1_000 -> String.format("%.1f KB/s", bytesPerSec / 1_000.0)
            else -> "${bytesPerSec} B/s"
        }
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        return if (file.exists()) {
            file.delete()
            _status.value = ModelStatus.NOT_DOWNLOADED
            _downloadProgress.value = 0f
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
}
