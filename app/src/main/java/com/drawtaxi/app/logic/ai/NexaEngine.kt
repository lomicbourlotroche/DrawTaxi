package com.drawtaxi.app.logic.ai

import android.content.Context
import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.bean.GenerationConfig
import com.nexa.sdk.bean.LlmCreateInput
import com.nexa.sdk.bean.LlmStreamResult
import com.nexa.sdk.bean.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

object NexaEngine {

    private const val TAG = "NexaEngine"
    private const val MODEL_FILENAME = "llama-3.2-3b-instruct-q4_k_m.gguf"
    private const val DEFAULT_MAX_TOKENS = 256
    private const val DEFAULT_N_CTX = 512

    private var llmWrapper: LlmWrapper? = null

    suspend fun runInference(
        context: Context,
        prompt: String,
        timeoutMs: Long = 60_000L
    ): String? {
        if (prompt.isBlank()) return null

        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                if (!ensureModelLoaded(context)) return@withTimeoutOrNull null

                val llm = llmWrapper ?: return@withTimeoutOrNull null

                try {
                    val result = StringBuilder()
                    llm.generateStreamFlow(prompt, GenerationConfig()).collect { streamResult ->
                        when (streamResult) {
                            is LlmStreamResult.Token -> {
                                result.append(streamResult.text)
                            }
                            is LlmStreamResult.Completed -> { }
                            is LlmStreamResult.Error -> {
                                Log.w(TAG, "Stream error: ${streamResult.throwable.message}")
                            }
                        }
                    }
                    result.toString().ifBlank { null }
                } catch (e: Exception) {
                    Log.e(TAG, "Inference failed: ${e.message}")
                    llmWrapper = null
                    null
                }
            }
        }
    }

    private suspend fun ensureModelLoaded(context: Context): Boolean {
        if (llmWrapper != null) return true

        val modelFile = File(context.filesDir, MODEL_FILENAME)
        if (!modelFile.exists() || !modelFile.canRead()) {
            Log.w(TAG, "Model not found: ${modelFile.absolutePath}")
            return false
        }

        Log.d(TAG, "Loading model: ${modelFile.absolutePath} (${modelFile.length() / 1_000_000} MB)")

        return withContext(Dispatchers.IO) {
            try {
                var loaded: LlmWrapper? = null
                LlmWrapper.builder()
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
                    .onSuccess { loaded = it }
                    .onFailure { Log.e(TAG, "Failed to load model: ${it.message}") }

                llmWrapper = loaded
                loaded != null
            } catch (e: Exception) {
                Log.e(TAG, "Model load error: ${e.message}")
                false
            }
        }
    }

    fun isAvailable(): Boolean {
        return llmWrapper != null
    }

    fun unloadModel() {
        try {
            llmWrapper = null
        } catch (e: Exception) {
            Log.w(TAG, "Error unloading model: ${e.message}")
        }
        Log.d(TAG, "Model unloaded")
    }
}
