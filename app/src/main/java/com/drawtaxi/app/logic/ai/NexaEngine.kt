package com.drawtaxi.app.logic.ai

import android.content.Context
import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.bean.GenerationConfig
import com.nexa.sdk.bean.LlmCreateInput
import com.nexa.sdk.bean.LlmStreamResult
import com.nexa.sdk.bean.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * NexaEngine manages the lifecycle and inference of the local LLM using the Nexa SDK.
 * It provides thread-safe access to the model and handles automatic loading/unloading.
 */
object NexaEngine {

    private const val TAG = "NexaEngine"
    private const val DEFAULT_MAX_TOKENS = 256
    private const val DEFAULT_N_CTX = 512

    private val mutex = Mutex()
    private var llmWrapper: LlmWrapper? = null

    /**
     * Executes a full inference and returns the accumulated result as a String.
     * This method is thread-safe and will load the model if necessary.
     */
    suspend fun runInference(
        context: Context,
        prompt: String,
        timeoutMs: Long = 60_000L
    ): String? {
        if (prompt.isBlank()) return null
        val appContext = context.applicationContext

        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                mutex.withLock {
                    if (!ensureModelLoaded(appContext)) return@withTimeoutOrNull null

                    val llm = llmWrapper ?: return@withTimeoutOrNull null

                    try {
                        val result = StringBuilder()
                        llm.generateStreamFlow(prompt, GenerationConfig()).collect { streamResult ->
                            when (streamResult) {
                                is LlmStreamResult.Token -> {
                                    result.append(streamResult.text)
                                }
                                is LlmStreamResult.Completed -> {
                                    // Generation finished successfully
                                }
                                is LlmStreamResult.Error -> {
                                    Log.w(TAG, "Stream error during inference: ${streamResult.throwable.message}")
                                    throw streamResult.throwable
                                }
                            }
                        }
                        result.toString().ifBlank { null }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e(TAG, "Inference failed with exception: ${e.message}")
                        // If inference fails critically, it might be due to a corrupt state; unload to reset
                        unloadModelInternal()
                        null
                    }
                }
            }
        }
    }

    /**
     * Returns a Flow of tokens for the given prompt, allowing for real-time UI updates.
     * The mutex is held only during SDK collection (inside a child coroutine), not during
     * emission, so concurrent inference calls are not blocked.
     */
    fun generateStream(
        context: Context,
        prompt: String
    ): Flow<String> = channelFlow {
        if (prompt.isBlank()) return@channelFlow
        val appContext = context.applicationContext

        launch(kotlinx.coroutines.Dispatchers.IO) {
            mutex.withLock {
                if (!ensureModelLoaded(appContext)) return@launch
                val llm = llmWrapper ?: return@launch

                try {
                    llm.generateStreamFlow(prompt, GenerationConfig()).collect { streamResult ->
                        when (streamResult) {
                            is LlmStreamResult.Token -> send(streamResult.text)
                            is LlmStreamResult.Error -> throw streamResult.throwable
                            is LlmStreamResult.Completed -> { /* done */ }
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e(TAG, "Streaming inference failed", e)
                    unloadModelInternal()
                    throw e
                }
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    /**
     * Ensures the LLM model is loaded into memory.
     * Must be called within a mutex lock to ensure thread safety.
     */
    private suspend fun ensureModelLoaded(context: Context): Boolean {
        if (llmWrapper != null) return true

        val modelFile = LlamaModelManager.getModelFile(context)
        if (!LlamaModelManager.isModelAvailable(context)) {
            Log.w(TAG, "Model file not found or invalid: ${modelFile.absolutePath}")
            return false
        }

        Log.d(TAG, "Loading model into memory: ${modelFile.name} (${modelFile.length() / 1_000_000} MB)")

        return try {
            val result = LlmWrapper.builder()
                .llmCreateInput(
                    LlmCreateInput(
                        model_name = "",
                        model_path = modelFile.absolutePath,
                        config = ModelConfig(
                            max_tokens = DEFAULT_MAX_TOKENS,
                            nCtx = DEFAULT_N_CTX
                        ),
                        plugin_id = "cpu_gpu",
                        device_id = null
                    )
                )
                .build()

            llmWrapper = result.getOrNull()
            if (llmWrapper == null) {
                Log.e(TAG, "Failed to initialize LlmWrapper: ${result.exceptionOrNull()?.message}")
            } else {
                Log.i(TAG, "Model loaded successfully")
            }
            llmWrapper != null
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during model loading", e)
            false
        }
    }

    /**
     * Checks if the AI engine is ready to run inference immediately
     * (model loaded in memory). For UI display purposes.
     */
    fun isAvailable(context: Context): Boolean {
        return llmWrapper != null
    }

    /**
     * Checks if the model file exists on disk and can be loaded on demand.
     * Used internally to decide whether to attempt inference.
     */
    fun isModelDownloaded(context: Context): Boolean {
        return LlamaModelManager.isModelAvailable(context)
    }

    /**
     * Unloads the model and releases native resources.
     */
    fun unloadModel() {
        // Use a non-blocking check if possible, or just lock if we must ensure safety
        try {
            unloadModelInternal()
            Log.d(TAG, "Model unloaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Error during model unloading: ${e.message}")
        }
    }

    private fun unloadModelInternal() {
        llmWrapper?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing LlmWrapper", e)
            } finally {
                llmWrapper = null
            }
        }
    }
}
