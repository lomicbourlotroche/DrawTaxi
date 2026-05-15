package com.drawtaxi.app.logic

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object LlamaModelManager {

    private const val TAG = "LlamaModelManager"
    private const val MODEL_FILENAME = "phi3-mini.gguf"
    private const val MODEL_URL = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        READY,
        ERROR,
        UNLOADED
    }

    private var _status = ModelStatus.NOT_DOWNLOADED
    private var _downloadProgress = 0f
    private var _lastUsedTime = 0L
    private val AUTO_UNLOAD_DELAY_MS = 10 * 60 * 1000L

    val status: ModelStatus get() = _status
    val downloadProgress: Float get() = _downloadProgress

    fun getModelFile(context: Context): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelAvailable(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > 100_000_000L
    }

    suspend fun downloadModel(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        if (isModelAvailable(context)) {
            _status = ModelStatus.READY
            return true
        }

        _status = ModelStatus.DOWNLOADING
        val outputFile = getModelFile(context)

        return try {
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true

            val totalSize = connection.contentLengthLong
            var downloadedBytes = 0L

            FileOutputStream(outputFile).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalSize > 0) {
                            _downloadProgress = downloadedBytes.toFloat() / totalSize
                            onProgress(_downloadProgress)
                        }
                    }
                }
            }

            if (outputFile.length() > 100_000_000L) {
                _status = ModelStatus.READY
                Log.i(TAG, "Model downloaded successfully: ${outputFile.length() / 1024 / 1024} MB")
                true
            } else {
                outputFile.delete()
                _status = ModelStatus.ERROR
                Log.e(TAG, "Downloaded file too small, deleting")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            outputFile.delete()
            _status = ModelStatus.ERROR
            false
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
        Log.i(TAG, "Model unloaded (auto-unload after inactivity)")
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        if (file.exists()) {
            file.delete()
            _status = ModelStatus.NOT_DOWNLOADED
            return true
        }
        return false
    }

    fun getDownloadedSize(context: Context): Long {
        val file = getModelFile(context)
        return if (file.exists()) file.length() else 0L
    }
}
